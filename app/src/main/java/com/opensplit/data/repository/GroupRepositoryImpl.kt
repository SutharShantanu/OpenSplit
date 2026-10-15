package com.opensplit.data.repository

import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class GroupRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: ActivityRepository
) : GroupRepository {
    
    private val groupsCollection = firestore.collection("groups")

    override fun getGroupsForUser(userId: String): Flow<List<Group>> {
        val firestoreFlow = callbackFlow<List<Group>> {
            val listener = groupsCollection
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val groups = snapshot?.documents?.mapNotNull { it.toObject(Group::class.java) } ?: emptyList()
                    trySend(groups)
                }
            awaitClose { listener.remove() }
        }.onStart { emit(emptyList<Group>()) }

        return combine(firestoreFlow, com.opensplit.data.local.InMemoryDataStore.groups) { remote, local ->
            val userLocal = local.filter { it.memberIds.contains(userId) }
            (remote + userLocal).distinctBy { it.id }
        }
    }

    override suspend fun getGroup(groupId: String): Group? {
        val local = com.opensplit.data.local.InMemoryDataStore.groups.value.find { it.id == groupId }
        if (local != null) return local
        return try {
            kotlinx.coroutines.withTimeoutOrNull(1000) {
                groupsCollection.document(groupId).get().await().toObject(Group::class.java)
            } ?: local
        } catch (e: Exception) {
            local
        }
    }

    override suspend fun createGroup(group: Group): Result<String> {
        return try {
            val docRef = groupsCollection.document()
            val newGroup = group.copy(id = docRef.id)
            docRef.set(newGroup).awaitWrite()
            activityRepository.logActivity(
                docRef.id,
                Activity(
                    type = ActivityType.GROUP_CREATED,
                    actorUid = group.createdBy,
                    message = "created group '${group.name}'"
                )
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGroup(group: Group): Result<Unit> {
        if (group.id.isBlank()) return Result.failure(IllegalArgumentException("Group ID cannot be blank"))
        return try {
            groupsCollection.document(group.id).set(group).awaitWrite()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        if (groupId.isBlank()) return Result.failure(IllegalArgumentException("Group ID cannot be blank"))
        return try {
            groupsCollection.document(groupId).delete().awaitWrite()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrCreateDirectGroup(me: String, other: String, currency: String): Result<Group> = try {
        val existing = groupsCollection.whereEqualTo("isDirect", true).whereArrayContains("memberIds", me).get().await()
            .documents.mapNotNull { it.toObject(Group::class.java) }
            .firstOrNull { other in it.memberIds }
        if (existing != null) Result.success(existing) else {
            val group = Group(name = "Direct", createdBy = me, memberIds = listOf(me, other), currency = currency, isDirect = true)
            createGroup(group).map { id -> group.copy(id = id) }
        }
    } catch (e: Exception) { Result.failure(e) }
}
