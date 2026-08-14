package com.opensplit.data.ai

import com.opensplit.BuildConfig
import com.opensplit.ui.components.GroupAvatarPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI-powered Group Avatar Suggester using Gemini REST API with local keyword fallback.
 */
object GeminiGroupIconSuggester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-2.0-flash"

    /** Returns true when a usable Gemini API key is present. */
    fun isConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Instant local keyword match for zero latency UI feedback.
     */
    fun getLocalHeuristicSuggestion(groupName: String): String? {
        val lower = groupName.lowercase().trim()
        if (lower.isBlank()) return null

        val keywords = mapOf(
            "travel" to listOf("trip", "tour", "flight", "fly", "vacation", "holiday", "travel", "goa", "paris", "tokyo", "bali", "hawaii", "plane", "resort", "roadtrip", "trek", "hike"),
            "home" to listOf("home", "flat", "apartment", "house", "room", "rent", "roommate", "household", "stay", "villa", "pg", "suite"),
            "food" to listOf("food", "pizza", "lunch", "dinner", "restaurant", "eat", "burger", "cafe", "snack", "biryani", "swiggy", "zomato", "dining", "breakfast", "brunch", "sushi", "taco"),
            "party" to listOf("party", "club", "celebration", "bar", "drinks", "pub", "weekend", "fest", "nightout", "bachelor", "bachelorette", "cheers"),
            "work" to listOf("office", "work", "project", "team", "colleague", "corp", "startup", "company", "meeting", "hackathon", "sync"),
            "school" to listOf("school", "college", "university", "study", "class", "tuition", "batch", "exam", "grad", "campus", "hostel"),
            "beach" to listOf("beach", "sea", "ocean", "surf", "island", "coast", "goa", "miami", "maldives"),
            "gaming" to listOf("game", "gaming", "ps5", "xbox", "steam", "esports", "lan", "arcade", "valorant", "dota"),
            "car" to listOf("car", "cab", "uber", "ola", "drive", "road", "taxi", "ride", "fuel", "petrol", "gas", "commute"),
            "movie" to listOf("movie", "cinema", "film", "show", "netflix", "theater", "popcorn", "imax", "binge"),
            "sports" to listOf("sports", "football", "soccer", "cricket", "match", "gym", "turf", "badminton", "tennis", "nba", "ipl"),
            "pets" to listOf("pet", "dog", "cat", "puppy", "kitten", "vet", "doggo"),
            "music" to listOf("music", "concert", "gig", "band", "song", "spotify", "festival", "show"),
            "books" to listOf("book", "lib", "read", "library", "novel", "reading"),
            "coffee" to listOf("coffee", "chai", "tea", "starbucks", "boba", "espresso", "latte"),
            "shopping" to listOf("shopping", "mall", "clothes", "groceries", "market", "store", "target", "amazon", "mart"),
            "money" to listOf("money", "fund", "investment", "finance", "bank", "savings", "budget", "cash", "crypto"),
            "birthday" to listOf("birthday", "bday", "cake", "anniversary", "gift"),
            "fitness" to listOf("fitness", "workout", "gym", "crossfit", "training", "run", "marathon", "yoga"),
            "world" to listOf("world", "global", "international", "europe", "asia", "usa", "overseas")
        )

        for ((key, words) in keywords) {
            if (words.any { lower.contains(it) }) {
                return key
            }
        }
        return null
    }

    /**
     * Suggests the best avatar key for the given group name using Gemini 2.0 Flash,
     * falling back to local heuristics if unavailable or on error.
     */
    suspend fun suggestAvatarKey(groupName: String): String? = withContext(Dispatchers.IO) {
        val trimmed = groupName.trim()
        if (trimmed.isEmpty()) return@withContext null

        val local = getLocalHeuristicSuggestion(trimmed)
        if (!isConfigured()) return@withContext local

        val validKeys = GroupAvatarPresets.all.map { it.key }
        val keysString = validKeys.joinToString(", ")

        try {
            val prompt = "You are an AI assistant in an expense-sharing app. " +
                "Given the group name \"$trimmed\", choose the SINGLE best matching avatar category key from this exact list:\n" +
                "[$keysString]\n\n" +
                "Respond ONLY with the exact key string from the list (e.g. \"travel\"). Do not include quotes, markdown, or any extra text."

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
                if (!response.isSuccessful) return@withContext local
                val bodyString = response.body?.string() ?: return@withContext local
                val rawText = JSONObject(bodyString)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                    .getString("text")
                val key = rawText.trim().lowercase().removeSurrounding("\"").removeSurrounding("'")
                if (validKeys.contains(key)) key else local
            }
        } catch (e: Exception) {
            local
        }
    }
}
