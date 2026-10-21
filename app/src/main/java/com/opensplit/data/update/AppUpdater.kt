package com.opensplit.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.opensplit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * In-app updates straight from GitHub Releases.
 *
 * Checks `releases/latest`, compares its tag with [BuildConfig.VERSION_NAME], downloads the APK
 * with [DownloadManager] and hands it to the system package installer. No Play Services needed.
 */
class AppUpdater(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        /** [progress] is 0..1, or null while the total size is still unknown. */
        data class Downloading(val release: Release, val progress: Float?) : State
        data class Downloaded(val release: Release, val apk: Uri) : State
        data class Failed(val message: String) : State
    }

    data class Release(
        val tag: String,
        val version: List<Int>,
        val notes: String,
        val pageUrl: String,
        val apkUrl: String?,
        val apkBytes: Long
    )

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var autoChecked = false

    /** Once per process, and never for local builds (versionCode 1 has no release to compare). */
    suspend fun checkOnLaunch() {
        if (autoChecked || BuildConfig.VERSION_CODE <= 1) return
        autoChecked = true
        check()
    }

    suspend fun check(): State = withContext(Dispatchers.IO) {
        _state.value = State.Checking
        val next = runCatching { fetchLatest() }.fold(
            onSuccess = { release ->
                if (compareVersions(release.version, parseVersion(BuildConfig.VERSION_NAME)) > 0) {
                    State.Available(release)
                } else {
                    State.UpToDate
                }
            },
            onFailure = { e ->
                Log.w(TAG, "update check failed", e)
                State.Failed("Couldn't check for updates")
            }
        )
        _state.value = next
        next
    }

    fun dismiss() {
        _state.value = State.Idle
    }

    /** Downloads the APK, tracking progress in [state], then opens the installer. */
    suspend fun downloadAndInstall(release: Release) {
        val url = release.apkUrl ?: run {
            // No APK asset on the release: fall back to the release page in the browser.
            open(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
            return
        }
        val dm = context.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("OpenSplit ${release.tag}")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "OpenSplit-${release.tag}.apk")
        val id = dm.enqueue(request)
        _state.value = State.Downloading(release, null)

        // ponytail: 400 ms polling instead of a BroadcastReceiver; gives progress for free.
        while (true) {
            delay(400)
            val cursor = dm.query(DownloadManager.Query().setFilterById(id)) ?: break
            val done = cursor.use { c ->
                if (!c.moveToFirst()) return@use true
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val soFar = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val apk = dm.getUriForDownloadedFile(id)
                        if (apk == null) {
                            _state.value = State.Failed("Download finished but the file is missing")
                        } else {
                            _state.value = State.Downloaded(release, apk)
                            install(apk)
                        }
                        true
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        _state.value = State.Failed("Download failed (code $reason)")
                        true
                    }
                    else -> {
                        _state.value = State.Downloading(
                            release,
                            if (total > 0) (soFar.toFloat() / total).coerceIn(0f, 1f) else null
                        )
                        false
                    }
                }
            }
            if (done) break
        }
    }

    /** Opens the system installer for an already downloaded APK (e.g. "Install" tapped again). */
    fun install(apk: Uri) {
        open(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apk, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    private fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w(TAG, "no activity for $intent", it) }
    }

    private fun fetchLatest(): Release {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
        val tag = json.getString("tag_name")
        var apkUrl: String? = null
        var apkBytes = 0L
        val assets = json.optJSONArray("assets")
        for (i in 0 until (assets?.length() ?: 0)) {
            val asset = assets!!.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                apkBytes = asset.optLong("size")
                break
            }
        }
        return Release(
            tag = tag,
            version = parseVersion(tag),
            notes = plainNotes(json.optString("body")),
            pageUrl = json.optString("html_url"),
            apkUrl = apkUrl,
            apkBytes = apkBytes
        )
    }

    companion object {
        private const val TAG = "AppUpdater"
        private const val APK_MIME = "application/vnd.android.package-archive"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/SutharShantanu/OpenSplit/releases/latest"

        /** "v1.0.31" -> [1, 0, 31]; "1.0" -> [1, 0]. */
        fun parseVersion(text: String): List<Int> =
            Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()

        /** Lexicographic compare with missing components treated as 0. */
        fun compareVersions(a: List<Int>, b: List<Int>): Int {
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }

        /**
         * Release bodies are markdown with our asset table on top and the generated
         * "What's Changed" list below. Keep only that list, minus markdown noise.
         */
        fun plainNotes(body: String): String {
            val changes = body.substringAfter("## What's Changed", body)
            return changes.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("|") && !it.startsWith("```") && !it.startsWith("#") }
                .map { line ->
                    line.removePrefix("* ").removePrefix("- ")
                        .replace(Regex("\\s+by @\\S+ in https?://\\S+$"), "")
                        .replace("**", "").replace("`", "")
                }
                .take(12)
                .joinToString("\n") { "• $it" }
        }
    }
}
