package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.domain.repository.AuthRepository
import com.opensplit.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithEmail(email: String, pass: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                // User doc check or create just in case? Usually not needed for simple sign in
                _uiState.value = AuthUiState.Success()
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, pass)
            if (result.isSuccess) {
                val uid = result.getOrNull()!!
                // Create user profile document
                val userResult = userRepository.createUserIfNotFound(uid, displayName, email, null)
                if (userResult.isSuccess) {
                    _uiState.value = AuthUiState.Success("Account created successfully")
                } else {
                    _uiState.value = AuthUiState.Error("Failed to initialize user profile")
                }
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String, displayName: String?, email: String?, photoUrl: String?) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val uid = result.getOrNull()!!
                val userResult = userRepository.createUserIfNotFound(uid, displayName, email, photoUrl)
                if (userResult.isSuccess) {
                    _uiState.value = AuthUiState.Success()
                } else {
                    _uiState.value = AuthUiState.Error("Failed to initialize user profile")
                }
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Google Sign-In failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success("Password reset email sent")
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
