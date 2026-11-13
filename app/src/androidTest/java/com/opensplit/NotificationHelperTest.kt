package com.opensplit

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opensplit.domain.model.NotificationType
import com.opensplit.service.NotificationHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Posts real notifications on the device and checks channels, stacking and replacement. */
@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nm = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.createNotificationChannels(context)
        nm.cancelAll()
    }

    @After
    fun tearDown() = nm.cancelAll()

    private fun active() = run { Thread.sleep(500); nm.activeNotifications.toList() }

    @Test
    fun eachTypeHasItsOwnChannel() {
        val ids = nm.notificationChannels.map { it.id }.toSet()
        NotificationType.entries.forEach { assertTrue(it.name, NotificationHelper.channelFor(it) in ids) }
    }

    @Test
    fun sameGroupStacksUnderOneSummary() {
        NotificationHelper.show(context, NotificationType.EXPENSE_ADDED, "A added Pizza", "₹500", groupId = "g1", groupName = "Goa", key = "e1")
        NotificationHelper.show(context, NotificationType.EXPENSE_ADDED, "B added Cab", "₹300", groupId = "g1", groupName = "Goa", key = "e2")
        val posted = active()
        assertEquals(3, posted.size)
        assertTrue(posted.all { it.notification.group == "group:g1" })
        assertEquals(1, posted.count { it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0 })
    }

    @Test
    fun sameKeyReplacesInsteadOfAdding() {
        NotificationHelper.show(context, NotificationType.EXPENSE_EDITED, "Edited", "v1", key = "e1")
        NotificationHelper.show(context, NotificationType.EXPENSE_EDITED, "Edited", "v2", key = "e1")
        val posted = active()
        assertEquals(1, posted.size)
        assertEquals("v2", posted[0].notification.extras.getCharSequence("android.text").toString())
    }
}
