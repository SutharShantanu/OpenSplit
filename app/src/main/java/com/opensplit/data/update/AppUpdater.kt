package com.opensplit.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.opensplit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * In-app updates straight from GitHub Releases.
 *
 * Checks `releases/latest`, compares its tag with [BuildConfig.VERSION_NAME], downloads the APK
 * into the app cache, verifies its SHA-256 and installs it through PackageInstaller. No Play Services needed.
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
        data class Downloading(val release: Release, val progress: Float?, val bytes: Long = 0, val totalBytes: Long = 0) : State
        /** APK downloaded and verified; [needsPermission] until "install unknown apps" is allowed. */
        data class Downloaded(val release: Release, val apk: java.io.File, val needsPermission: Boolean = false) : State
        /** Handed to PackageInstaller; [progress] 0..1 covers copying the APK in and Android installing it. */
        data class Installing(val release: Release, val progress: Float = 0f) : State
        data class Failed(val message: String) : State
    }

    data class Release(
        val tag: String,
        val version: List<Int>,
        val notes: String,
        val pageUrl: String,
        val apkUrl: String?,
        val apkBytes: Long,
        /** `checksums.txt` asset (sha256sum format) published by release.yml; null on old releases. */
        val checksumsUrl: String? = null
    )

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var autoChecked = false

    /** Copies the APK into the installer session off the main thread. */
    private val installScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    private val prefs by lazy { context.getSharedPreferences("app_updater", Context.MODE_PRIVATE) }

    private val _whatsNew = MutableStateFlow<Release?>(null)
    /** Notes of the version just installed, shown once after an update. */
    val whatsNew: StateFlow<Release?> = _whatsNew.asStateFlow()

    /**
     * First launch after an update: fetch this version's release notes once. A fresh install only
     * records the version, so new users aren't greeted with a changelog.
     */
    suspend fun checkWhatsNew() = withContext(Dispatchers.IO) {
        if (BuildConfig.VERSION_CODE <= 1) return@withContext
        val lastSeen = prefs.getString(KEY_LAST_SEEN, null)
        if (lastSeen == BuildConfig.VERSION_NAME) return@withContext
        prefs.edit().putString(KEY_LAST_SEEN, BuildConfig.VERSION_NAME).apply()
        if (lastSeen == null) return@withContext
        runCatching { fetchRelease("$RELEASES_URL/tags/v${BuildConfig.VERSION_NAME}").withCommitNotes("v$lastSeen") }
            .onSuccess { if (it.notes.isNotBlank()) _whatsNew.value = it }
            .onFailure { Log.w(TAG, "what's new fetch failed", it) }
    }

    fun dismissWhatsNew() {
        _whatsNew.value = null
    }

    /** Once per process, and never for local builds (versionCode 1 has no release to compare). */
    suspend fun checkOnLaunch() {
        // Just updated and showing "What's new": don't stack an update sheet on top.
        if (autoChecked || BuildConfig.VERSION_CODE <= 1 || _whatsNew.value != null) return
        autoChecked = true
        check()
    }

    suspend fun check(): State = withContext(Dispatchers.IO) {
        _state.value = State.Checking
        val next = runCatching { fetchRelease("$RELEASES_URL/latest") }.fold(
            onSuccess = { release ->
                if (compareVersions(release.version, parseVersion(BuildConfig.VERSION_NAME)) > 0) {
                    State.Available(release.withCommitNotes("v${BuildConfig.VERSION_NAME}"))
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

    /** Downloads the APK into the app cache with progress, verifies it, then installs in-app. */
    suspend fun downloadAndInstall(release: Release) {
        val url = release.apkUrl ?: run {
            // No APK asset on the release: fall back to the release page in the browser.
            open(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
            return
        }
        _state.value = State.Downloading(release, null)
        val apk = runCatching { download(url, release) }.getOrElse {
            Log.w(TAG, "download failed", it)
            _state.value = State.Failed("Download failed: check your connection")
            return
        }
        if (!verifyChecksum(release, Uri.fromFile(apk))) {
            apk.delete()
            _state.value = State.Failed("Downloaded file failed its integrity check")
            return
        }
        _state.value = State.Downloaded(release, apk)
        install()
    }

    private suspend fun download(url: String, release: Release): java.io.File = withContext(Dispatchers.IO) {
        val dir = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the newest download
        val file = java.io.File(dir, "OpenSplit-${release.tag}.apk")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkBytes
            body.byteStream().use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    var lastEmit = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (read - lastEmit > 256 * 1024 || read == total) {
                            lastEmit = read
                            _state.value = State.Downloading(
                                release,
                                if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else null,
                                read,
                                total
                            )
                        }
                    }
                }
            }
        }
        file
    }

    /**
     * Installs the downloaded APK through a PackageInstaller session (in-app: Android shows only its
     * own confirmation, and on 12+ updates of an app we installed can skip it). Call again after the
     * user allows "install unknown apps" for OpenSplit.
     */
    fun install() {
        val ready = _state.value as? State.Downloaded ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = ready.copy(needsPermission = true)
            return
        }
        _state.value = State.Installing(ready.release)
        installScope.launch {
            runCatching {
                val installer = context.packageManager.packageInstaller
                val params = android.content.pm.PackageInstaller.SessionParams(
                    android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setAppPackageName(context.packageName)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
                val sessionId = installer.createSession(params)
                // Android reports staging + install progress (0..1) for the session; only ever move forward.
                fun report(p: Float) {
                    val cur = _state.value as? State.Installing ?: return
                    if (p > cur.progress) _state.value = cur.copy(progress = p.coerceAtMost(1f))
                }
                installer.registerSessionCallback(object : android.content.pm.PackageInstaller.SessionCallback() {
                    override fun onCreated(id: Int) = Unit
                    override fun onBadgingChanged(id: Int) = Unit
                    override fun onActiveChanged(id: Int, active: Boolean) = Unit
                    override fun onProgressChanged(id: Int, progress: Float) { if (id == sessionId) report(progress) }
                    override fun onFinished(id: Int, success: Boolean) {
                        if (id != sessionId) return
                        if (success) report(1f)
                        installer.unregisterSessionCallback(this)
                    }
                }, android.os.Handler(android.os.Looper.getMainLooper()))

                installer.openSession(sessionId).use { session ->
                    val total = ready.apk.length().coerceAtLeast(1)
                    session.openWrite("base.apk", 0, total).use { out ->
                        ready.apk.inputStream().use { input ->
                            val buf = ByteArray(64 * 1024)
                            var copied = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                copied += n
                                // Copying the APK in is staging; Android turns this into session progress.
                                session.setStagingProgress(copied.toFloat() / total)
                            }
                        }
                        session.fsync(out)
                    }
                    val callback = android.app.PendingIntent.getBroadcast(
                        context, sessionId,
                        Intent(context, UpdateInstallReceiver::class.java),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                    )
                    session.commit(callback.intentSender)
                }
            }.onFailure {
                Log.w(TAG, "install failed", it)
                _state.value = State.Failed("Couldn't start the installer")
            }
        }
    }

    /** Opens the "install unknown apps" switch for OpenSplit. */
    fun openInstallPermissionSettings() {
        open(
            Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
        )
    }

    /** Called by [UpdateInstallReceiver] when the session ends without installing. */
    internal fun onInstallFailed(message: String) {
        _state.value = State.Failed(message)
    }

    private fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w(TAG, "no activity for $intent", it) }
    }

    /**
     * Releases built from direct pushes have no "What's Changed" list (GitHub only adds it for
     * merged PRs), so fall back to the commit subjects between [fromTag] and this release.
     */
    private fun Release.withCommitNotes(fromTag: String): Release {
        if (notes.isNotBlank()) return this
        val commits = runCatching {
            val request = Request.Builder()
                .url("$REPO_API_URL/compare/$fromTag...$tag")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { r ->
                if (!r.isSuccessful) error("compare HTTP ${r.code}")
                JSONObject(r.body?.string().orEmpty()).getJSONArray("commits")
            }
        }.onFailure { Log.w(TAG, "commit notes unavailable", it) }.getOrNull() ?: return this
        val subjects = (0 until commits.length()).map {
            commits.getJSONObject(it).getJSONObject("commit").getString("message")
        }
        return copy(notes = commitNotes(subjects))
    }

    /**
     * Compares the APK's SHA-256 with the release's checksums.txt. Releases without that asset
     * predate it and are accepted; a checksum we can't fetch or that differs is a failure.
     */
    private suspend fun verifyChecksum(release: Release, apk: Uri): Boolean = withContext(Dispatchers.IO) {
        val url = release.checksumsUrl ?: return@withContext true
        runCatching {
            val listing = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("checksums HTTP ${r.code}")
                r.body?.string().orEmpty()
            }
            val apkName = release.apkUrl?.substringAfterLast('/') ?: return@runCatching false
            val expected = expectedSha256(listing, apkName) ?: return@runCatching false
            val actual = context.contentResolver.openInputStream(apk)!!.use(::sha256Hex)
            expected.equals(actual, ignoreCase = true)
        }.onFailure { Log.w(TAG, "checksum verify failed", it) }.getOrDefault(false)
    }

    private fun fetchRelease(apiUrl: String): Release {
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .build()
        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
        val tag = json.getString("tag_name")
        var apkUrl: String? = null
        var apkBytes = 0L
        var checksumsUrl: String? = null
        val assets = json.optJSONArray("assets")
        for (i in 0 until (assets?.length() ?: 0)) {
            val asset = assets!!.getJSONObject(i)
            val name = asset.getString("name")
            if (apkUrl == null && name.endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                apkBytes = asset.optLong("size")
            } else if (name == "checksums.txt") {
                checksumsUrl = asset.getString("browser_download_url")
            }
        }
        return Release(
            tag = tag,
            version = parseVersion(tag),
            notes = plainNotes(json.optString("body")),
            pageUrl = json.optString("html_url"),
            apkUrl = apkUrl,
            apkBytes = apkBytes,
            checksumsUrl = checksumsUrl
        )
    }

    companion object {
        private const val TAG = "AppUpdater"
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val KEY_LAST_SEEN = "last_seen_version"
        const val REPO_API_URL = "https://api.github.com/repos/SutharShantanu/OpenSplit"
        const val RELEASES_URL = "$REPO_API_URL/releases"

        /** Commit messages (oldest first, as GitHub returns them) to newest-first bullets. */
        fun commitNotes(messages: List<String>): String =
            messages.asReversed()
                .map { it.lineSequence().first().trim() }
                .filter { it.isNotBlank() && !it.startsWith("Merge ") }
                .distinct()
                .take(12)
                .joinToString("\n") { "• $it" }

        /** Finds `<hex>  <file>` for [fileName] in sha256sum output. */
        fun expectedSha256(listing: String, fileName: String): String? =
            listing.lineSequence()
                .map { it.trim().split(Regex("\\s+"), limit = 2) }
                .firstOrNull { it.size == 2 && it[1].trimStart('*') == fileName }
                ?.get(0)

        fun sha256Hex(input: java.io.InputStream): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

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
            // Without a change list the body is only our asset table and install commands.
            if ("## What's Changed" !in body) return ""
            val changes = body.substringAfter("## What's Changed")
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
