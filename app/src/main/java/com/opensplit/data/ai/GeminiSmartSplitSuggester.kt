package com.opensplit.data.ai

import com.opensplit.BuildConfig
import com.opensplit.domain.model.SplitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmartSplitSuggestion(
    val category: String,
    val splitType: SplitType,
    val reason: String
)

/**
 * AI assistant that predicts category and optimal split type (EQUAL, EXACT, PERCENTAGE, SHARES, ITEMIZED)
 * based on expense description and total amount.
 */
object GeminiSmartSplitSuggester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-2.0-flash"

    fun isConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /** Instant local fallback heuristic */
    fun getLocalSuggestion(description: String): SmartSplitSuggestion {
        val lower = description.lowercase().trim()
        return when {
            lower.contains("pizza") || lower.contains("dinner") || lower.contains("food") || lower.contains("lunch") || lower.contains("swiggy") || lower.contains("zomato") ->
                SmartSplitSuggestion("Food & Drinks", SplitType.EQUAL, "Dining and food expenses are usually split equally.")
            lower.contains("rent") || lower.contains("stay") || lower.contains("villa") || lower.contains("hotel") || lower.contains("airbnb") ->
                SmartSplitSuggestion("Rent", SplitType.EQUAL, "Shared stay & rent is divided equally among members.")
            lower.contains("uber") || lower.contains("cab") || lower.contains("fuel") || lower.contains("flight") || lower.contains("trip") ->
                SmartSplitSuggestion("Travel", SplitType.EQUAL, "Commute and travel costs are split equally.")
            lower.contains("grocery") || lower.contains("market") || lower.contains("blinkit") || lower.contains("zepto") ->
                SmartSplitSuggestion("Groceries", SplitType.ITEMIZED, "Groceries can be split itemized based on who bought what.")
            lower.contains("wifi") || lower.contains("electricity") || lower.contains("utility") || lower.contains("bill") ->
                SmartSplitSuggestion("Utilities", SplitType.EQUAL, "Utility bills are shared equally.")
            else ->
                SmartSplitSuggestion("General", SplitType.EQUAL, "Equal split works for most shared group expenses.")
        }
    }

    suspend fun suggestSplit(description: String, amount: Double): SmartSplitSuggestion = withContext(Dispatchers.IO) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return@withContext getLocalSuggestion("general")

        val local = getLocalSuggestion(trimmed)
        if (!isConfigured()) return@withContext local

        try {
            val prompt = "You are an AI financial assistant in an expense-splitting app. " +
                "Given the expense description \"$trimmed\" and total amount $amount, predict the best category and split type.\n" +
                "Available categories: [\"Food & Drinks\", \"Rent\", \"Travel\", \"Entertainment\", \"Groceries\", \"Utilities\", \"General\"]\n" +
                "Available split types: [\"EQUAL\", \"EXACT\", \"PERCENTAGE\", \"SHARES\", \"ITEMIZED\"]\n\n" +
                "Respond ONLY with a JSON object: {\"category\": \"...\", \"splitType\": \"...\", \"reason\": \"...\"}. No extra text or markdown."

            val payload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext local
                val body = response.body?.string() ?: return@withContext local
                val text = JSONObject(body)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                    .getString("text").trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(text)
                val cat = json.optString("category", local.category)
                val stStr = json.optString("splitType", local.splitType.name).uppercase()
                val st = runCatching { SplitType.valueOf(stStr) }.getOrDefault(local.splitType)
                val reason = json.optString("reason", local.reason)
                SmartSplitSuggestion(cat, st, reason)
            }
        } catch (e: Exception) {
            local
        }
    }
}
