package com.opensplit.util

import com.google.firebase.firestore.FirebaseFirestore
import com.opensplit.data.local.InMemoryDataStore

object MockDataSeeder {

    suspend fun seedMockData(firestore: FirebaseFirestore, currentUid: String): Result<Unit> {
        return try {
            InMemoryDataStore.seedForUser(currentUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
