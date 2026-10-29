package com.opensplit.data.repository

import com.opensplit.domain.model.Settlement
import com.opensplit.domain.repository.SettlementRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class SettlementRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: com.opensplit.domain.repository.ActivityRepository
) : SettlementRepository {

    override fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>> {
        val firestoreFlow = callbackFlow<List<Settlement>> {
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
                    val settlements = snapshot?.documents?.mapNotNull { 
                        it.toObject(Settlement::class.java)?.copy(id = it.id, groupId = groupId) 
                    } ?: emptyList()
                    trySend(settlements)
                }
            awaitClose { listener.remove() }
        }.onStart { emit(emptyList<Settlement>()) }

        return firestoreFlow.map { it.sortedByDescending { s -> s.date } }
    }

    override suspend fun addSettlement(groupId: String, settlement: Settlement): Result<String> {
        return try {
            val docId = if (settlement.id.isNotBlank()) settlement.id else firestore.collection("groups").document(groupId).collection("settlements").document().id
            val settlementWithId = settlement.copy(id = docId, groupId = groupId)
            val docRef = firestore.collection("groups").document(groupId)
                .collection("settlements").document(docId)
            // The caller logs the activity with payer/payee names; logging here duplicated it.
            docRef.set(settlementWithId).awaitWrite()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
