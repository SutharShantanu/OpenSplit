package com.opensplit.ui

import com.google.firebase.Timestamp
import com.opensplit.data.ai.AiService
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.model.SettlementMethod
import com.opensplit.ui.viewmodel.SettleUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSuggestionHelpersTest {
    @Test
    fun localHostsAreLoopbackPrivateOrMdns() {
        listOf("localhost", "127.0.0.1", "10.0.0.5", "192.168.1.20", "172.16.0.1", "172.31.9.9", "ollama.local")
            .forEach { assertTrue(it, AiService.isLocalHost(it)) }
        listOf(null, "api.openai.com", "172.32.0.1", "8.8.8.8", "my10.0.example.com")
            .forEach { assertFalse(it.toString(), AiService.isLocalHost(it)) }
    }

    private fun paid(from: String, method: SettlementMethod, secondsAgo: Long) =
        Settlement(fromUid = from, toUid = "x", amount = 1.0, method = method, date = Timestamp(1_800_000_000 - secondsAgo, 0))

    @Test
    fun usualMethodIsMostUsedThenMostRecent() {
        val me = "me"
        assertNull(SettleUpViewModel.usualMethod(emptyList(), me))
        assertNull(SettleUpViewModel.usualMethod(listOf(paid("other", SettlementMethod.CASH, 1)), me))
        assertEquals(
            SettlementMethod.CASH,
            SettleUpViewModel.usualMethod(listOf(paid(me, SettlementMethod.UPI, 10), paid(me, SettlementMethod.CASH, 5), paid(me, SettlementMethod.CASH, 1)), me)
        )
        // A tie goes to the method used most recently.
        assertEquals(
            SettlementMethod.BANK_TRANSFER,
            SettleUpViewModel.usualMethod(listOf(paid(me, SettlementMethod.UPI, 10), paid(me, SettlementMethod.BANK_TRANSFER, 1)), me)
        )
    }
}
