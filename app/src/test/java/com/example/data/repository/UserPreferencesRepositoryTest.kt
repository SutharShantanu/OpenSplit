package com.example.data.repository

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
}
