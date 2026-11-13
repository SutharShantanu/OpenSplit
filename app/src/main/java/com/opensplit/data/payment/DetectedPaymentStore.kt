package com.opensplit.data.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

data class DetectedPayment(
    val id: Long,
    val amount: Double,
    val direction: Direction,
    val counterparty: String?,
    val reference: String?,
    val source: String,
    val detectedAt: Long,
    val status: Status = Status.PENDING
) {
    enum class Status { PENDING, ADDED, IGNORED }
}

/**
 * Payments detected on this device, as parsed fields only (never message text). Kept in a
 * small JSON file in no-backup storage: it never leaves the phone and is not cloud-backed-up.
 */
class DetectedPaymentStore(private val file: File) {

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<DetectedPayment>> = _items.asStateFlow()

    private val _detected = MutableSharedFlow<DetectedPayment>(extraBufferCapacity = 4)
    /** Each newly stored payment, for the in-app banner. */
    val detected: SharedFlow<DetectedPayment> = _detected.asSharedFlow()

    fun get(id: Long): DetectedPayment? = _items.value.firstOrNull { it.id == id }

    /**
     * Stores [p] unless it is the same payment seen from another app (a bank and a UPI app
     * often both notify). Returns the new record, or null when it was merged into an existing one.
     */
    @Synchronized
    fun record(p: ParsedPayment, now: Long = System.currentTimeMillis()): DetectedPayment? {
        val list = _items.value
        val match = list.firstOrNull { it.reference != null && it.reference == p.reference }
            ?: list.firstOrNull {
                it.direction == p.direction && abs(it.amount - p.amount) < 0.005 &&
                    now - it.detectedAt in 0..MERGE_WINDOW_MS && compatible(it.counterparty, p.counterparty)
            }
        if (match != null) {
            // Keep what the first sighting lacked, e.g. the bank's reference or the UPI app's payee name.
            save(list.map { if (it.id == match.id) it.copy(counterparty = it.counterparty ?: p.counterparty, reference = it.reference ?: p.reference) else it })
            return null
        }
        val new = DetectedPayment((list.maxOfOrNull { it.id } ?: 0) + 1, p.amount, p.direction, p.counterparty, p.reference, p.source, now)
        // ponytail: newest 200 only; enough to review, keeps the file tiny.
        save((listOf(new) + list).take(200))
        _detected.tryEmit(new)
        return new
    }

    @Synchronized
    fun setStatus(id: Long, status: DetectedPayment.Status) =
        save(_items.value.map { if (it.id == id) it.copy(status = status) else it })

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(list: List<DetectedPayment>) {
        _items.value = list
        val json = JSONArray()
        list.forEach {
            json.put(JSONObject().apply {
                put("id", it.id); put("amount", it.amount); put("direction", it.direction.name)
                put("counterparty", it.counterparty ?: JSONObject.NULL); put("reference", it.reference ?: JSONObject.NULL)
                put("source", it.source); put("detectedAt", it.detectedAt); put("status", it.status.name)
            })
        }
        runCatching { file.parentFile?.mkdirs(); file.writeText(json.toString()) }
    }

    private fun load(): List<DetectedPayment> = runCatching {
        val json = JSONArray(file.readText())
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            DetectedPayment(
                o.getLong("id"), o.getDouble("amount"), Direction.valueOf(o.getString("direction")),
                o.optString("counterparty").takeIf { !o.isNull("counterparty") }, o.optString("reference").takeIf { !o.isNull("reference") },
                o.getString("source"), o.getLong("detectedAt"), DetectedPayment.Status.valueOf(o.getString("status"))
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val MERGE_WINDOW_MS = 3 * 60 * 1000L

        /** "RAHUL K" and "Rahul Kumar" can be the same payee; unknown on either side is compatible. */
        fun compatible(a: String?, b: String?): Boolean {
            if (a == null || b == null) return true
            val x = a.uppercase().take(5); val y = b.uppercase().take(5)
            return a.uppercase().startsWith(y) || b.uppercase().startsWith(x)
        }
    }
}
