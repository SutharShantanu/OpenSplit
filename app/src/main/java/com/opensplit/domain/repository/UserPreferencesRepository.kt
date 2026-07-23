package com.opensplit.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeFlow: Flow<String>
    suspend fun setTheme(theme: String)

    val hasCompletedPermissionPrimer: Flow<Boolean>
    suspend fun setHasCompletedPermissionPrimer(completed: Boolean)

    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

