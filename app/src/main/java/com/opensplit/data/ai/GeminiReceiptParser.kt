package com.opensplit.data.ai

import android.util.Base64
import com.opensplit.BuildConfig
import com.opensplit.domain.model.ExpenseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Parses a receipt image into line items using the Gemini REST API.
 *
 * The API key comes from BuildConfig.GEMINI_API_KEY (injected by the Secrets Gradle
 * plugin from .env). Returns null when no key is configured or on any failure, so the
 * caller can degrade gracefully — the rest of the app works without it.
 */
object GeminiReceiptParser {

    private val client = OkHttpClient()
    private const val MODEL = "gemini-2.0-flash"

    suspend fun parseReceipt(imageBytes: ByteArray, mimeType: String = "image/jpeg"): List<ExpenseItem>? =
        withContext(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY") return@withContext null
            try {
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val prompt = "Extract the line items from this receipt. Respond ONLY with a JSON " +
                    "array of objects, each having \"name\" (string) and \"price\" (number). " +
                    "No markdown fences, no extra text."
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray()
                                    .put(JSONObject().put("text", prompt))
                                    .put(
                                        JSONObject().put(
                                            "inline_data",
                                            JSONObject()
                                                .put("mime_type", mimeType)
                                                .put("data", base64)
                                        )
                                    )
                            )
                        )
                    )
                }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyString = response.body?.string() ?: return@withContext null
                    val text = JSONObject(bodyString)
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                        .getString("text")
                    val cleaned = text.trim()
                        .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val array = JSONArray(cleaned)
                    (0 until array.length()).mapNotNull { i ->
                        val obj = array.optJSONObject(i) ?: return@mapNotNull null
                        val name = obj.optString("name").ifBlank { return@mapNotNull null }
                        val price = obj.optDouble("price", 0.0)
                        ExpenseItem(id = UUID.randomUUID().toString(), name = name, price = price, assignedUids = emptyList())
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
}
