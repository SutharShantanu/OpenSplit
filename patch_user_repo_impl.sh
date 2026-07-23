sed -i '/override fun searchUsersByEmail/i \
    override suspend fun updateCurrency(currency: String): Result<Unit> {\
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("No user"))\
        return try {\
            usersCollection.document(uid).update("currency", currency).await()\
            Result.success(Unit)\
        } catch (e: Exception) {\
            Result.failure(e)\
        }\
    }\
\
    override suspend fun updateUser(user: User): Result<Unit> {\
        return try {\
            usersCollection.document(user.uid).update("displayName", user.displayName).await()\
            Result.success(Unit)\
        } catch (e: Exception) {\
            Result.failure(e)\
        }\
    }' app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt
