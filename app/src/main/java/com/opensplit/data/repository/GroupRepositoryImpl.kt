package com.opensplit.data.repository

import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        }

        return kotlinx.coroutines.flow.combine(firestoreFlow, com.opensplit.data.local.InMemoryDataStore.groups) { remote, local ->
            val userLocal = local.filter { it.memberIds.contains(userId) }
            (remote + userLocal).distinctBy { it.id }
        }
    }

    override suspend fun getGroup(groupId: String): Group? {
        if (groupId.isBlank()) return com.opensplit.data.local.InMemoryDataStore.groups.value.find { it.id == groupId }
        return try {
            groupsCollection.document(groupId).get().await().toObject(Group::class.java)
        } catch (e: Exception) {
            com.opensplit.data.local.InMemoryDataStore.groups.value.find { it.id == groupId }
        }
    }

    override suspend fun createGroup(group: Group): Result<String> {
        return try {
            val docRef = groupsCollection.document()
            val newGroup = group.copy(id = docRef.id)
            docRef.set(newGroup).await()
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
            groupsCollection.document(group.id).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        if (groupId.isBlank()) return Result.failure(IllegalArgumentException("Group ID cannot be blank"))
        return try {
            groupsCollection.document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
