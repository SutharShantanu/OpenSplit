sed -i '/override suspend fun deleteAccount/i \
    override suspend fun updateProfile(displayName: String): Result<Unit> {\
        return try {\
            val updateRequest = com.google.firebase.auth.UserProfileChangeRequest.Builder()\
                .setDisplayName(displayName)\
                .build()\
            auth.currentUser?.updateProfile(updateRequest)?.await()\
            Result.success(Unit)\
        } catch (e: Exception) {\
            Result.failure(e)\
        }\
    }\
\
    override suspend fun reauthenticateWithEmail(password: String): Result<Unit> {\
        return try {\
            val user = auth.currentUser\
            if (user != null && user.email != null) {\
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)\
                user.reauthenticate(credential).await()\
                Result.success(Unit)\
            } else {\
                Result.failure(Exception("No user or email"))\
            }\
        } catch (e: Exception) {\
            Result.failure(e)\
        }\
    }' app/src/main/java/com/example/data/repository/AuthRepositoryImpl.kt
