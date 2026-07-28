package com.opensplit.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.opensplit.domain.repository.StorageRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepositoryImpl(
    private val storage: FirebaseStorage
) : StorageRepository {

    override suspend fun uploadReceipt(groupId: String, localUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("receipts/$groupId/${UUID.randomUUID()}")
            ref.putFile(localUri).await()
            val url = ref.downloadUrl.await()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
