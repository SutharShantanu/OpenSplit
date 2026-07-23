package com.opensplit.data.repository

import com.opensplit.domain.model.Activity
import com.opensplit.domain.repository.ActivityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class ActivityRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ActivityRepository {

    override fun getActivityForGroup(groupId: String): Flow<List<Activity>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .collection("activity")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val activities = snapshot?.documents?.mapNotNull { it.toObject(Activity::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(activities)
            }
        awaitClose { listener.remove() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getActivityForUser(userId: String, groupIds: List<String>): Flow<List<Activity>> {
        if (groupIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val groupActivityFlows = groupIds.map { groupId -> getActivityForGroup(groupId) }
        return kotlinx.coroutines.flow.combine(groupActivityFlows) { arrays ->
            arrays.flatMap { it }.sortedByDescending { it.timestamp.seconds }
        }
    }

    override suspend fun logActivity(groupId: String, activity: Activity): Result<String> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)
                .collection("activity")
                .add(activity).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
