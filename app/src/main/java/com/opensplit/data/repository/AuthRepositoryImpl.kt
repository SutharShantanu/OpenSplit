package com.opensplit.data.repository

import com.opensplit.domain.repository.AuthRepository
import com.opensplit.domain.repository.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val _demoUserFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override val currentUser: com.google.firebase.auth.FirebaseUser? get() = auth.currentUser
    
    override fun getAuthState(): Flow<AuthState> {
        val firebaseAuthFlow = callbackFlow<AuthState> {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    trySend(AuthState.LoggedIn(user.uid))
                } else {
                    trySend(AuthState.LoggedOut)
                }
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

        return kotlinx.coroutines.flow.combine(firebaseAuthFlow, _demoUserFlow) { firebaseState, demoUid ->
            if (demoUid != null) {
                AuthState.LoggedIn(demoUid)
            } else {
                firebaseState
            }
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid ?: _demoUserFlow.value
    }
    
    override suspend fun signInAsDemo(
        uid: String,
        displayName: String,
        email: String
    ): Result<String> {
        _demoUserFlow.value = uid
        return Result.success(uid)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // Deep-links the reset link back into the app (see the App Link intent-filter in
            // AndroidManifest.xml) so "set new password" happens in our own UI via
            // confirmPasswordReset rather than on Firebase's hosted page.
            val actionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
                .setUrl("https://${com.opensplit.BuildConfig.FIREBASE_AUTH_ACTION_HOST}/reset")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(com.opensplit.BuildConfig.APPLICATION_ID, false, null)
                .build()
            auth.sendPasswordResetEmail(email, actionCodeSettings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Result<Unit> {
        return try {
            auth.confirmPasswordReset(oobCode, newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(displayName: String): Result<Unit> {
        return try {
            val updateRequest = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            auth.currentUser?.updateProfile(updateRequest)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reauthenticateWithEmail(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user or email"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                user.reauthenticate(credential).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        _demoUserFlow.value = null
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Ignore sign-out failures
        }
    }
}
