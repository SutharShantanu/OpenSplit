package com.opensplit.data.ai

import com.opensplit.domain.model.SplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiParsingTest {

    @Test
    fun `fences and prose around json are stripped`() {
        assertEquals("""{"a":1}""", AiService.stripFences("```json\n{\"a\":1}\n```"))
        assertEquals("""["x","y"]""", AiService.stripFences("Sure! Here you go: [\"x\",\"y\"] hope that helps"))
    }

    @Test
    fun `receipt json maps items and confidence`() {
        val r = AiService.parseReceiptJson(
            """{"merchant":"Cafe Noir","total":42.5,"currency":"INR","date":"2026-10-01",
               "items":[{"name":"Latte","price":12.5},{"name":"","price":1}],"confidence":"low"}"""
        )!!
        assertEquals("Cafe Noir", r.merchant)
        assertEquals(42.5, r.total!!, 0.0)
        assertEquals("INR", r.currency)
        assertEquals("2026-10-01", r.dateIso)
        assertEquals(1, r.items.size)
        assertTrue(r.lowConfidence)
        assertNull(AiService.parseReceiptJson("not json"))
    }

    @Test
    fun `expense json resolves members by first name and validates category`() {
        val members = listOf("Rahul Sharma", "Priya Singh", "Shantanu")
        val p = AiService.parseExpenseJson(
            """{"description":"Dinner","amount":1500,"category":"food","paidBy":"rahul",
               "participants":["Priya","nobody"],"splitType":"equal"}""", members
        )!!
        assertEquals("Dinner", p.description)
        assertEquals(1500.0, p.amount!!, 0.0)
        assertEquals("Food", p.category)
        assertEquals("Rahul Sharma", p.paidByName)
        assertEquals(listOf("Priya Singh"), p.participantNames)
        assertEquals(SplitType.EQUAL, p.splitType)
        assertEquals(ParsedExpense.Source.AI, p.source)
    }

    @Test
    fun `offline heuristics categorise and parse a sentence`() {
        assertEquals("Transport", AiHeuristics.categoryFor("Uber to airport"))
        assertEquals("Other", AiHeuristics.categoryFor("zzz"))
        val p = AiHeuristics.parseExpenseText("Rahul paid 1,200 for dinner with Priya and me", listOf("Rahul Sharma", "Priya Singh", "Shantanu"))!!
        assertEquals(1200.0, p.amount!!, 0.0)
        assertEquals("Rahul Sharma", p.paidByName)
        assertEquals(listOf("Rahul Sharma", "Priya Singh"), p.participantNames)
        assertEquals("Food", p.category)
        assertEquals("Dinner", p.description)
        assertEquals(ParsedExpense.Source.LOCAL, p.source)
        assertNull(AiHeuristics.parseExpenseText("   ", emptyList()))
    }
}
