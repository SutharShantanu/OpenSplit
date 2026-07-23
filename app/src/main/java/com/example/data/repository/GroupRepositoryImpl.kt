package com.example.data.repository

import com.example.domain.model.Group
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.repository.ActivityRepository
import com.example.domain.repository.GroupRepository
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

    override fun getGroupsForUser(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = groupsCollection
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { it.toObject(Group::class.java) } ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getGroup(groupId: String): Group? {
        return try {
            groupsCollection.document(groupId).get().await().toObject(Group::class.java)
        } catch (e: Exception) {
            null
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
        return try {
            groupsCollection.document(group.id).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
