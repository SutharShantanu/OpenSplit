package com.example.data.repository

import com.example.domain.model.Settlement
import com.example.domain.repository.SettlementRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class SettlementRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: com.example.domain.repository.ActivityRepository
) : SettlementRepository {

    override fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("groups").document(groupId)
            .collection("settlements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val settlements = snapshot?.documents?.mapNotNull { it.toObject(Settlement::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(settlements.sortedByDescending { it.date })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addSettlement(groupId: String, settlement: Settlement): Result<String> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)
                .collection("settlements")
                .add(settlement).await()

            activityRepository.logActivity(
                groupId,
                com.example.domain.model.Activity(
                    type = com.example.domain.model.ActivityType.SETTLEMENT_ADDED,
                    actorUid = settlement.fromUid,
                    message = "logged a settlement",
                    timestamp = settlement.date
                )
            )

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
