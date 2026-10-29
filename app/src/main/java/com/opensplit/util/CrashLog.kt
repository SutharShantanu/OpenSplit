package com.opensplit.util

import android.content.Context
import android.os.Build
import com.opensplit.BuildConfig

/**
 * Keeps the last uncaught crash so the next launch can offer to copy it for a bug report.
 * Chains to the previous handler, so the system crash dialog / Play vitals still see it.
 */
object CrashLog {
    private const val PREFS = "crash_log"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                // commit(): the process dies right after this returns.
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY, report(thread, error)).commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pending(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    fun report(thread: Thread, error: Throwable): String = buildString {
        appendLine("OpenSplit ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Time: ${java.util.Date()}")
        appendLine("Thread: ${thread.name}")
        appendLine()
        append(error.stackTraceToString().take(12_000))
    }
}
