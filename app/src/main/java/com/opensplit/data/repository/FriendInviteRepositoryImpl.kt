package com.opensplit.data.repository

import com.opensplit.domain.model.FriendInvite
import com.opensplit.domain.repository.FriendInviteRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendInviteRepositoryImpl(
    private val firestore: FirebaseFirestore
) : FriendInviteRepository {

    private val invitesCollection = firestore.collection("invites")

    override fun getInvites(inviterUid: String): Flow<List<FriendInvite>> = callbackFlow {
        if (inviterUid.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = invitesCollection
            .whereEqualTo("inviterUid", inviterUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invites = snapshot?.documents?.mapNotNull {
                    it.toObject(FriendInvite::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(invites.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendInvite(inviterUid: String, email: String): Result<String> {
        val trimmed = email.trim()
        if (inviterUid.isBlank() || trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing inviter or email"))
        }
        return try {
            val docRef = invitesCollection.document()
            val invite = FriendInvite(id = docRef.id, inviterUid = inviterUid, email = trimmed)
            docRef.set(invite).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> {
        return try {
            invitesCollection.document(inviteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
