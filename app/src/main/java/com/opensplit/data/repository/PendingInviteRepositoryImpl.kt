package com.opensplit.data.repository

import com.opensplit.domain.model.PendingInvite
import com.opensplit.domain.repository.PendingInviteRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PendingInviteRepositoryImpl(
    private val firestore: FirebaseFirestore
) : PendingInviteRepository {

    private fun invitesRef(groupId: String) =
        firestore.collection("groups").document(groupId).collection("pendingInvites")

    override fun getPendingInvites(groupId: String): Flow<List<PendingInvite>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = invitesRef(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val invites = snapshot?.documents?.mapNotNull {
                it.toObject(PendingInvite::class.java)?.copy(id = it.id)
            } ?: emptyList()
            trySend(invites.sortedByDescending { it.createdAt })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun addInvite(invite: PendingInvite): Result<String> {
        return try {
            val docRef = invitesRef(invite.groupId).document()
            docRef.set(invite.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeInvite(groupId: String, inviteId: String): Result<Unit> {
        return try {
            invitesRef(groupId).document(inviteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
