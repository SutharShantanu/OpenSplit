package com.opensplit.data.repository

import com.opensplit.domain.model.User
import com.opensplit.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UserRepository {
    private val usersCollection = firestore.collection("users")
    private val localUsers = kotlinx.coroutines.flow.MutableStateFlow<Map<String, User>>(
        mapOf(
            "user_elena_demo" to User(
                uid = "user_elena_demo",
                displayName = "Elena Rodriguez",
                email = "elena.rodriguez@example.com"
            )
        )
    )

    override suspend fun getUser(uid: String): User? {
        if (uid.isBlank()) return null
        val cached = localUsers.value[uid]
        if (cached != null) return cached

        val localMatch = com.opensplit.data.local.InMemoryDataStore.friends.value.find { it.uid == uid }
        if (localMatch != null) return localMatch

        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserFlow(uid: String): Flow<User?> {
        if (uid.isBlank()) {
            return localUsers.map { local ->
                local[uid] ?: com.opensplit.data.local.InMemoryDataStore.friends.value.find { it.uid == uid }
            }
        }
        val firestoreFlow = callbackFlow<User?> {
            if (uid.isBlank()) {
                trySend(null)
                awaitClose {}
                return@callbackFlow
            }
            val listener = try {
                usersCollection.document(uid).addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        val current = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (current != null && current.uid == uid) {
                            trySend(User(uid = uid, displayName = current.displayName ?: "User", email = current.email ?: ""))
                        } else {
                            trySend(null)
                        }
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(User::class.java))
                }
            } catch (e: Exception) {
                trySend(null)
                awaitClose {}
                return@callbackFlow
            }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(firestoreFlow, localUsers) { remote, local ->
            local[uid] ?: remote ?: com.opensplit.data.local.InMemoryDataStore.friends.value.find { it.uid == uid }
        }
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        if (user.uid.isBlank()) return Result.success(Unit)
        localUsers.value = localUsers.value + (user.uid to user)
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun createUserIfNotFound(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        if (uid.isBlank()) return Result.success(Unit)
        val user = User(
            uid = uid,
            displayName = displayName ?: "User",
            email = email ?: "",
            photoUrl = photoUrl
        )
        localUsers.value = localUsers.value + (uid to user)
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                usersCollection.document(uid).set(user).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun updateCurrency(currency: String): Result<Unit> {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("No user"))
        if (uid.isBlank()) return Result.failure(Exception("No user"))
        return try {
            usersCollection.document(uid).update("currency", currency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        if (user.uid.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
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
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("UID cannot be blank"))
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
