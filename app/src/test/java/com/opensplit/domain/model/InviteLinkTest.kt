package com.opensplit.domain.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteLinkTest {
    private val base = InviteLink(token = "abcDEF1234567890", groupId = "g", groupName = "Goa")

    @Test
    fun `token parses from url path or bare token`() {
        assertEquals("abcDEF1234567890", InviteLink.tokenFrom("https://opensplit-fe83f.firebaseapp.com/join/abcDEF1234567890?x=1"))
        assertEquals("abcDEF1234567890", InviteLink.tokenFrom("  abcDEF1234567890 "))
        assertNull(InviteLink.tokenFrom("not a token!"))
        assertNull(InviteLink.tokenFrom(""))
    }

    @Test
    fun `validation order expired then used up then member`() {
        val now = 1_000_000L
        assertNull(base.validate(now, "u", emptyList()))
        assertEquals(JoinError.EXPIRED, base.copy(expiresAt = Timestamp(now - 1, 0)).validate(now, "u", emptyList()))
        assertNull(base.copy(expiresAt = Timestamp(now + 60, 0)).validate(now, "u", emptyList()))
        assertEquals(JoinError.USED_UP, base.copy(maxUses = 1, uses = 1).validate(now, "u", emptyList()))
        assertNull(base.copy(maxUses = 0, uses = 99).validate(now, "u", emptyList()))
        assertEquals(JoinError.ALREADY_MEMBER, base.validate(now, "u", listOf("u")))
        assertEquals(JoinError.NOT_FOUND, base.copy(revoked = true).validate(now, "u", emptyList()))
    }
}
