package com.opensplit.data.ai

import com.opensplit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns a spending summary into a few short, plain-language insights using the Gemini REST API.
 *
 * Same contract as [GeminiReceiptParser]: the key comes from BuildConfig.GEMINI_API_KEY and any
 * missing-key/failure case returns null so the screen degrades to charts-only.
 */
object GeminiInsightsGenerator {

    private val client = OkHttpClient()
    private const val MODEL = "gemini-2.0-flash"

    /** True when a usable API key is configured — lets the UI hide the AI entry point entirely. */
    fun isConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * @param summary a compact, pre-computed description of the user's spending (categories,
     *   monthly trend, totals). Only aggregates are sent — never raw expense descriptions.
     * @return short insight lines, or null if unavailable.
     */
    suspend fun generateInsights(summary: String): List<String>? =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext null
            try {
                val prompt = buildString {
                    append("You are a personal finance assistant inside an expense-splitting app. ")
                    append("Given this spending summary, write 3 to 4 short insights (max 18 words each) ")
                    append("that are specific, useful, and reference the actual numbers. ")
                    append("Cover spending concentration, trends over time, and one practical suggestion. ")
                    append("Respond ONLY with a JSON array of strings. No markdown fences, no extra text.\n\n")
                    append(summary)
                }
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", prompt))
                            )
                        )
                    )
                }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
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
                    (0 until array.length())
                        .mapNotNull { i -> array.optString(i).takeIf { it.isNotBlank() } }
                        .ifEmpty { null }
                }
            } catch (e: Exception) {
                null
            }
        }
}
