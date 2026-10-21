package com.opensplit.data.repository

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Await a Firestore write for a short grace period only. Firestore applies writes to the local
 * cache immediately and syncs them when the backend is reachable, so blocking the UI past that
 * point only ever hangs it (no network, backend not provisioned, captive portal, ...).
 * Returns null when the server ack hasn't arrived yet; the write is still queued.
 */
suspend fun <T> Task<T>.awaitWrite(graceMs: Long = 2500): T? = withTimeoutOrNull(graceMs) { await() }

/**
 * Fire-and-forget for writes the UI must not wait on: Firestore applies them to the local cache
 * synchronously and syncs later, so the data is already visible. Failures are only logged.
 */
fun <T> Task<T>.enqueue(tag: String = "Firestore"): Task<T> =
    addOnFailureListener { android.util.Log.w(tag, "queued write failed", it) }
