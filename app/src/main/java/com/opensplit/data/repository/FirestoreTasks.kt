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
