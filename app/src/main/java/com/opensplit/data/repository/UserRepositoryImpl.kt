package com.opensplit.data.repository

import com.opensplit.domain.model.User
import com.opensplit.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UserRepository {
    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(uid: String): User? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserIfNotFound(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                val user = User(
                    uid = uid,
                    displayName = displayName ?: "User",
                    email = email ?: "",
                    photoUrl = photoUrl
                )
                usersCollection.document(uid).set(user).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCurrency(currency: String): Result<Unit> {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("No user"))
        return try {
            usersCollection.document(uid).update("currency", currency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).update("displayName", user.displayName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSeenActivity(
        uid: String,
        timestamp: com.google.firebase.Timestamp
    ): Result<Unit> {
        return try {
            usersCollection.document(uid).update("lastSeenActivityTimestamp", timestamp).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override fun searchUsersByEmail(query: String): Flow<List<User>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = usersCollection
            .orderBy("email")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }
}
