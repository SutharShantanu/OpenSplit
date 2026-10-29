package com.opensplit.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Real on-disk numbers for the Device Storage settings, instead of placeholder figures. */
object StorageStats {
    suspend fun cacheBytes(context: Context): Long = withContext(Dispatchers.IO) {
        listOfNotNull(context.cacheDir, context.externalCacheDir, context.codeCacheDir).sumOf(::sizeOf)
    }

    /** Everything under the app's data dir except caches: Firestore offline DB, prefs, files. */
    suspend fun appDataBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val skip = setOf(context.cacheDir, context.codeCacheDir).map { it.canonicalFile }
        context.dataDir.listFiles().orEmpty()
            .filter { it.canonicalFile !in skip }
            .sumOf(::sizeOf)
    }

    /** Deletes cache contents (not the dirs) and returns bytes freed. */
    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val before = cacheBytes(context)
        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
        (before - cacheBytes(context)).coerceAtLeast(0)
    }

    fun formatMb(bytes: Long): String = String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)

    private fun sizeOf(file: File): Long = file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
