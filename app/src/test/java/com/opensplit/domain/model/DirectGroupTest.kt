package com.opensplit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectGroupTest {
    @Test
    fun `direct groups stay out of the visible list but keep both members`() {
        val direct = Group(id = "d", name = "Direct", memberIds = listOf("a", "b"), isDirect = true)
        val trip = Group(id = "t", name = "Trip", memberIds = listOf("a", "b", "c"))
        val visible = listOf(direct, trip).filterNot { it.isDirect }
        assertEquals(listOf(trip), visible)
        assertTrue(direct.memberIds.containsAll(listOf("a", "b")))
        assertEquals("pending", FriendInvite().status)
    }
}
