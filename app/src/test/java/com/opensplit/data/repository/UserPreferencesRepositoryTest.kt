package com.opensplit.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserPreferencesRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = UserPreferencesRepositoryImpl(context)
    }

    @Test
    fun testThemePreferenceWriteAndRead() = runBlocking {
        repository.setTheme("dark")
        val savedTheme = repository.themeFlow.first()
        assertEquals("dark", savedTheme)

        repository.setTheme("light")
        val updatedTheme = repository.themeFlow.first()
        assertEquals("light", updatedTheme)
    }

    @Test
    fun testPermissionPrimerPreferenceWriteAndRead() = runBlocking {
        repository.setHasCompletedPermissionPrimer(true)
        val completed = repository.hasCompletedPermissionPrimer.first()
        assertEquals(true, completed)
    }

    @Test
    fun settingsBackupRoundTripSkipsSecrets() = runBlocking {
        repository.setTheme("dark")
        repository.setGroupMuted("g1", true)
        val json = repository.exportSettings()
        // API keys live in DataStore on older installs; they must never be exported.
        val withSecret = SettingsBackup.encode(
            androidx.datastore.preferences.core.mutablePreferencesOf(
                androidx.datastore.preferences.core.stringPreferencesKey("openai_api_key") to "sk-secret"
            )
        )
        org.junit.Assert.assertFalse(withSecret.contains("sk-secret"))

        repository.setTheme("light")
        repository.setGroupMuted("g1", false)
        val restored = repository.importSettings(json)
        org.junit.Assert.assertTrue(restored >= 2)
        assertEquals("dark", repository.themeFlow.first())
        assertEquals(setOf("g1"), repository.mutedGroupIdsFlow.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun settingsImportRejectsForeignJson(): Unit = runBlocking {
        repository.importSettings("""{"format":"something-else","values":{}}""")
    }
}
