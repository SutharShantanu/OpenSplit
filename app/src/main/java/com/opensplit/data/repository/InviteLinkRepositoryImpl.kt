package com.opensplit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.InviteLink
import com.opensplit.domain.model.JoinError
import com.opensplit.domain.model.validate
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.InviteLinkRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class JoinException(val error: JoinError) : Exception(error.message)

class InviteLinkRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: ActivityRepository
) : InviteLinkRepository {
    private val links = firestore.collection("inviteLinks")

    override suspend fun create(group: Group, createdBy: String, expiresInSeconds: Long?, maxUses: Int): Result<InviteLink> = try {
        val token = newToken()
        val link = InviteLink(
            token = token, groupId = group.id, groupName = group.name, createdBy = createdBy,
            expiresAt = expiresInSeconds?.let { Timestamp(Timestamp.now().seconds + it, 0) },
            maxUses = maxUses
        )
        links.document(token).set(link).awaitWrite()
        Result.success(link)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun get(token: String): InviteLink? =
        runCatching { links.document(token).get().await().toObject(InviteLink::class.java)?.copy(token = token) }.getOrNull()

    override fun linksForGroup(groupId: String): Flow<List<InviteLink>> = callbackFlow {
        val reg = links.whereEqualTo("groupId", groupId).whereEqualTo("revoked", false)
            .addSnapshotListener { snap, e ->
                if (e != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(InviteLink::class.java)?.copy(token = it.id) }
                    ?.sortedByDescending { it.createdAt } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun revoke(token: String): Result<Unit> =
        runCatching { links.document(token).update("revoked", true).awaitWrite(); Unit }

    override suspend fun join(token: String, uid: String): Result<String> = try {
        val link = get(token) ?: throw JoinException(JoinError.NOT_FOUND)
        val groupRef = firestore.collection("groups").document(link.groupId)
        // The group is not readable before joining (rules), so validate against the link only,
        // then let the rules reject the write if the member list already contains the joiner.
        link.validate(Timestamp.now().seconds, uid, emptyList())?.let { throw JoinException(it) }
        groupRef.update(mapOf("memberIds" to FieldValue.arrayUnion(uid), "lastJoinToken" to token)).awaitWrite()
        links.document(token).update("uses", FieldValue.increment(1)).awaitWrite()
        activityRepository.logActivity(link.groupId, Activity(type = ActivityType.MEMBER_ADDED, actorUid = uid, message = "joined via invite link"))
        Result.success(link.groupId)
    } catch (e: Exception) { Result.failure(e) }

    private fun newToken(): String {
        val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        return (1..20).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
    }
}
