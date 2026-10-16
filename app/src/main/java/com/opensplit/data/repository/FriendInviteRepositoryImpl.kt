package com.opensplit.data.repository

import com.opensplit.domain.model.FriendInvite
import com.opensplit.domain.repository.FriendInviteRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendInviteRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val groupRepository: com.opensplit.domain.repository.GroupRepository? = null
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

    override suspend fun sendInvite(inviterUid: String, email: String, inviterName: String): Result<String> {
        val trimmed = email.trim().lowercase()
        if (inviterUid.isBlank() || trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing inviter or email"))
        }
        return try {
            val docRef = invitesCollection.document()
            val invite = FriendInvite(id = docRef.id, inviterUid = inviterUid, email = trimmed, inviterName = inviterName)
            docRef.set(invite).awaitWrite()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> {
        return try {
            invitesCollection.document(inviteId).delete().awaitWrite()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIncomingInvites(email: String): Flow<List<FriendInvite>> = callbackFlow {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) { trySend(emptyList()); awaitClose {}; return@callbackFlow }
        val reg = invitesCollection.whereEqualTo("email", normalized).whereEqualTo("status", "pending")
            .addSnapshotListener { snap, e ->
                if (e != null) { trySend(emptyList()); return@addSnapshotListener }
                val now = com.google.firebase.Timestamp.now()
                trySend(snap?.documents?.mapNotNull { it.toObject(FriendInvite::class.java)?.copy(id = it.id) }
                    ?.filter { it.expiresAt > now } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun respond(inviteId: String, accept: Boolean): Result<Unit> = try {
        invitesCollection.document(inviteId).update("status", if (accept) "accepted" else "declined").awaitWrite()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private val friendLinks = firestore.collection("friendLinks")

    override suspend fun getOrCreateFriendLink(uid: String, displayName: String): Result<com.opensplit.domain.model.FriendLink> = try {
        val now = com.google.firebase.Timestamp.now()
        val existing = kotlinx.coroutines.withTimeoutOrNull(2500) { friendLinks.whereEqualTo("uid", uid).get().await().documents }
            ?.firstOrNull { (it.getTimestamp("expiresAt") ?: now).seconds > now.seconds + 86400 }
        val expires = existing?.getTimestamp("expiresAt") ?: com.google.firebase.Timestamp(now.seconds + 30L * 86400, 0)
        val token = existing?.id ?: run {
            val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val rnd = java.security.SecureRandom()
            val t = (1..20).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
            friendLinks.document(t).set(
                mapOf("uid" to uid, "name" to displayName, "createdAt" to now, "expiresAt" to expires)
            ).awaitWrite()
            t
        }
        Result.success(com.opensplit.domain.model.FriendLink(com.opensplit.domain.model.InviteLink.FRIEND_BASE_URL + token, token, expires))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun acceptFriendLink(token: String, me: String, currency: String): Result<String> = try {
        val doc = friendLinks.document(token).get().await()
        val owner = doc.getString("uid") ?: throw IllegalStateException("This friend link is invalid or expired.")
        val expires = doc.getTimestamp("expiresAt")
        if (expires != null && expires.seconds <= com.google.firebase.Timestamp.now().seconds) throw IllegalStateException("This friend link has expired. Ask for a fresh one.")
        if (owner == me) throw IllegalStateException("That's your own link.")
        groupRepository?.getOrCreateDirectGroup(me, owner, currency)?.getOrThrow()
        Result.success(owner)
    } catch (e: Exception) { Result.failure(e) }
}
