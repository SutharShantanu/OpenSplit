package com.opensplit.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeFlow: Flow<String>
    suspend fun setTheme(theme: String)

    val hasCompletedPermissionPrimer: Flow<Boolean>
    suspend fun setHasCompletedPermissionPrimer(completed: Boolean)

    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    val pinnedGroupIdsFlow: Flow<Set<String>>
    suspend fun togglePinnedGroup(groupId: String)

    val openAiApiKeyFlow: Flow<String>
    suspend fun setOpenAiApiKey(key: String)

    val geminiApiKeyFlow: Flow<String>
    suspend fun setGeminiApiKey(key: String)

    val aiProviderFlow: Flow<String>
    suspend fun setAiProvider(provider: String)
}
