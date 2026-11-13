package com.opensplit.data.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // real org.json
class DetectedPaymentStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun store() = DetectedPaymentStore(java.io.File(tmp.root, "detected.json"))
    private fun pay(amount: Double, who: String? = null, ref: String? = null, source: String = "GPay") =
        ParsedPayment(amount, Direction.DEBIT, who, ref, source)

    @Test fun bankAndUpiAppNotificationsMergeIntoOne() {
        val s = store()
        assertNotNull(s.record(pay(1200.0, "Rahul Kumar", source = "GPay"), now = 1_000))
        // The bank notifies a minute later with the reference and a truncated name.
        assertNull(s.record(pay(1200.0, "RAHUL K", ref = "612345678901", source = "HDFC Bank"), now = 61_000))
        val only = s.items.value.single()
        assertEquals("Rahul Kumar", only.counterparty)
        assertEquals("612345678901", only.reference)
    }

    @Test fun sameReferenceMergesEvenLater() {
        val s = store()
        s.record(pay(500.0, ref = "999999"), now = 0)
        assertNull(s.record(pay(500.0, ref = "999999"), now = 60 * 60 * 1000))
        assertEquals(1, s.items.value.size)
    }

    @Test fun differentPaymentsStaySeparate() {
        val s = store()
        s.record(pay(300.0, "Rahul"), now = 0)
        s.record(pay(300.0, "Priya"), now = 10_000)                       // same amount, other payee
        s.record(pay(300.0, "Rahul"), now = DetectedPaymentStore.MERGE_WINDOW_MS + 1) // same payee, much later
        s.record(pay(301.0, "Rahul"), now = 20_000)                       // other amount
        assertEquals(4, s.items.value.size)
    }

    @Test fun keepsOnlyParsedFieldsAcrossRestarts() {
        val file = java.io.File(tmp.root, "detected.json")
        val s = DetectedPaymentStore(file)
        val p = s.record(pay(42.5, "Chai Point", "123456"), now = 5)!!
        s.setStatus(p.id, DetectedPayment.Status.IGNORED)
        val reloaded = DetectedPaymentStore(file).items.value.single()
        assertEquals(p.copy(status = DetectedPayment.Status.IGNORED), reloaded)
        assertEquals(setOf("id", "amount", "direction", "counterparty", "reference", "source", "detectedAt", "status"),
            org.json.JSONArray(file.readText()).getJSONObject(0).keys().asSequence().toSet())
    }
}
