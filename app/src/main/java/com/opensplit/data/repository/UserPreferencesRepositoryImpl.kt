package com.opensplit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opensplit.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val PERMISSION_PRIMER_KEY = booleanPreferencesKey("has_completed_permission_primer")

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
}

