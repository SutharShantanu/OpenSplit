package com.opensplit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opensplit.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val PERMISSION_PRIMER_KEY = booleanPreferencesKey("has_completed_permission_primer")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    private val PINNED_GROUPS_KEY = stringSetPreferencesKey("pinned_group_ids")

    override val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    override suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    override val hasCompletedPermissionPrimer: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PERMISSION_PRIMER_KEY] ?: false
        }

    override suspend fun setHasCompletedPermissionPrimer(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERMISSION_PRIMER_KEY] = completed
        }
    }

    override val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    override val pinnedGroupIdsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PINNED_GROUPS_KEY] ?: emptySet()
        }

    override suspend fun togglePinnedGroup(groupId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PINNED_GROUPS_KEY] ?: emptySet()
            preferences[PINNED_GROUPS_KEY] = if (groupId in current) current - groupId else current + groupId
        }
    }
}

