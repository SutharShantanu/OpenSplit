package com.example.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PermissionStep(
    val permission: String?,
    val title: String,
    val description: String,
    val minSdk: Int = 0
) {
    NOTIFICATIONS(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
        title = "Enable Notifications",
        description = "OpenSplit uses notifications to let you know when someone adds an expense or settles up.",
        minSdk = Build.VERSION_CODES.TIRAMISU
    ),
    CONTACTS(
        permission = Manifest.permission.READ_CONTACTS,
        title = "Connect Contacts",
        description = "Access your contacts to quickly invite and add friends to your groups."
    ),
    CAMERA(
        permission = Manifest.permission.CAMERA,
        title = "Enable Camera",
        description = "Use your camera to scan receipts and extract amounts automatically."
    );

    fun isApplicable(): Boolean {
        return Build.VERSION.SDK_INT >= minSdk && permission != null
    }

    fun isGranted(context: Context): Boolean {
        val perm = permission ?: return true
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

class PermissionPrimerViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val hasCompletedPrimer: StateFlow<Boolean> = userPreferencesRepository.hasCompletedPermissionPrimer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getPendingSteps(context: Context): List<PermissionStep> {
        return PermissionStep.values().filter { step ->
            step.isApplicable() && !step.isGranted(context)
        }
    }

    fun markPrimerCompleted() {
        viewModelScope.launch {
            userPreferencesRepository.setHasCompletedPermissionPrimer(true)
        }
    }
}
