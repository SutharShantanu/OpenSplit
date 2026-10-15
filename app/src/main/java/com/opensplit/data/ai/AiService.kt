package com.opensplit.data.ai

import com.opensplit.domain.model.ExpenseItem
import com.opensplit.domain.model.SplitType
import com.opensplit.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Every place AI can act. Each can be switched off in Settings; all have offline fallbacks. */
enum class AiFeature(val label: String, val description: String) {
    CATEGORIZE("Smart categories", "Suggest a category while you type an expense"),
    RECEIPT_SCAN("Receipt scanning", "Read merchant, total and line items from a photo"),
    NATURAL_LANGUAGE("Describe an expense", "Turn a sentence into a filled-in expense"),
    INSIGHTS("Spending insights", "Short written takeaways on the Insights tab"),
    SPLIT_SUGGEST("Split suggestions", "Recommend equal, itemized or other splits"),
    ICON_SUGGEST("Group icons", "Pick a group avatar from its name")
}

data class ParsedReceipt(
    val merchant: String?,
    val total: Double?,
    val currency: String?,
    val dateIso: String?,
    val items: List<ExpenseItem>,
    val lowConfidence: Boolean
)

data class ParsedExpense(
    val description: String,
    val amount: Double?,
    val category: String?,
    val paidByName: String?,
    val participantNames: List<String>,
    val splitType: SplitType?,
    val source: Source
) {
    enum class Source { AI, LOCAL }
}

/**
 * Single entry point for every AI feature. Resolves the active provider from preferences,
 * enforces the master and per-feature switches, counts usage, and never throws: a null
 * result means "use the fallback", and callers always have one.
 */
class AiService(private val prefs: UserPreferencesRepository) {

    /** Fully resolved config for the active provider, or null when AI is off or has no key. */
    suspend fun activeConfig(): AiConfig? {
        if (!prefs.aiEnabledFlow.first()) return null
        val id = prefs.aiProviderFlow.first()
        return configFor(id)
    }

    suspend fun configFor(providerId: String, keyOverride: String? = null, modelOverride: String? = null, baseUrlOverride: String? = null): AiConfig? {
        val info = AI_PROVIDERS_CATALOG.firstOrNull { it.id == providerId } ?: return null
        val key = keyOverride ?: prefs.providerApiKeysFlow.first()[providerId].orEmpty()
        if (key.isBlank() && providerId != "custom") return null
        val model = modelOverride ?: prefs.providerModelsFlow.first()[providerId]
            ?: if (providerId == "custom") prefs.customModelFlow.first() else info.defaultModel
        val baseUrl = baseUrlOverride ?: if (providerId == "custom") prefs.customEndpointFlow.first() else info.defaultBaseUrl
        return AiConfig(providerId, key, model, baseUrl)
    }

    /** Emits true while [feature] can run: master on, feature on, active provider has a key. */
    fun availability(feature: AiFeature): Flow<Boolean> = combine(
        prefs.aiEnabledFlow, prefs.aiDisabledFeaturesFlow, prefs.aiProviderFlow, prefs.providerApiKeysFlow
    ) { enabled, disabled, provider, keys ->
        enabled && feature.name !in disabled && (provider == "custom" || !keys[provider].isNullOrBlank())
    }

    suspend fun isAvailable(feature: AiFeature): Boolean = availability(feature).first()

    /** Runs one request on the active provider. Null when unavailable or on any failure. */
    private suspend fun ask(feature: AiFeature, request: AiRequest): String? {
        if (!isAvailable(feature)) return null
        val config = activeConfig() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { AiProvider.forProvider(config.providerId).complete(AiProvider.httpClient, config, request) }
                .onSuccess { prefs.incrementAiUsage(config.providerId) }
                .getOrNull()
        }
    }

    /** One cheap round trip to prove a key works. Returns the model's reply or the error text. */
    suspend fun testKey(providerId: String, key: String, baseUrl: String?, model: String?): Result<String> {
        val info = AI_PROVIDERS_CATALOG.firstOrNull { it.id == providerId }
            ?: return Result.failure(AiException("Unknown provider"))
        val config = AiConfig(providerId, key, model ?: info.testModel, baseUrl ?: info.defaultBaseUrl)
        return withContext(Dispatchers.IO) {
            runCatching {
                AiProvider.forProvider(providerId).complete(
                    AiProvider.httpClient, config, AiRequest("Reply with the single word OK.", maxTokens = 16)
                ).trim()
            }
        }
    }

    suspend fun suggestCategory(description: String): String? {
        val text = ask(
            AiFeature.CATEGORIZE,
            AiRequest(
                "Classify this shared expense into exactly one category from this list: " +
                    "${CATEGORIES.joinToString(", ")}.\nExpense: \"$description\"\n" +
                    "Respond with the category name only.",
                maxTokens = 16
            )
        ) ?: return null
        val cleaned = stripFences(text).trim().trim('"', '.')
        return CATEGORIES.firstOrNull { it.equals(cleaned, ignoreCase = true) }
    }

    suspend fun suggestSplit(description: String, category: String): SplitType? {
        val text = ask(
            AiFeature.SPLIT_SUGGEST,
            AiRequest(
                "For the shared expense \"$description\" (category $category) choose the best split method " +
                    "from EQUAL, EXACT, PERCENTAGE, SHARES, ITEMIZED. Respond with the word only.",
                maxTokens = 8
            )
        ) ?: return null
        return runCatching { SplitType.valueOf(stripFences(text).trim().uppercase()) }.getOrNull()
    }

    suspend fun suggestAvatarKey(groupName: String, validKeys: List<String>): String? {
        val text = ask(
            AiFeature.ICON_SUGGEST,
            AiRequest(
                "Group name: \"$groupName\". Choose the single best avatar key from: ${validKeys.joinToString(", ")}. " +
                    "Respond with the key only.",
                maxTokens = 8
            )
        ) ?: return null
        val key = stripFences(text).trim().lowercase().trim('"', '\'')
        return key.takeIf { it in validKeys }
    }

    suspend fun insights(summary: String): List<String>? {
        val text = ask(
            AiFeature.INSIGHTS,
            AiRequest(
                "You are a personal finance assistant inside an expense-splitting app. Given this spending " +
                    "summary, write 3 to 4 short insights (max 18 words each) that reference the actual numbers: " +
                    "spending concentration, trend over time, one practical suggestion. " +
                    "Respond ONLY with a JSON array of strings.\n\n$summary",
                maxTokens = 400
            )
        ) ?: return null
        return parseStringArray(text)
    }

    suspend fun parseReceipt(imageBytes: ByteArray, mime: String = "image/jpeg"): ParsedReceipt? {
        val text = ask(
            AiFeature.RECEIPT_SCAN,
            AiRequest(
                "Read this receipt. Respond ONLY with JSON: {\"merchant\": string|null, \"total\": number|null, " +
                    "\"currency\": ISO-4217 string|null, \"date\": \"YYYY-MM-DD\"|null, " +
                    "\"items\": [{\"name\": string, \"price\": number}], \"confidence\": \"high\"|\"low\"}. " +
                    "Prices are per line including quantity. Omit tax and tip lines from items.",
                imageBytes = imageBytes, imageMime = mime, maxTokens = 1500
            )
        ) ?: return null
        return parseReceiptJson(text)
    }

    suspend fun parseExpenseText(text: String, memberNames: List<String>, currency: String): ParsedExpense? {
        val raw = ask(
            AiFeature.NATURAL_LANGUAGE,
            AiRequest(
                "Extract an expense from this sentence for an expense-splitting app. Group members: " +
                    "${memberNames.joinToString(", ")}. Default currency: $currency. Categories: ${CATEGORIES.joinToString(", ")}.\n" +
                    "Sentence: \"$text\"\n" +
                    "Respond ONLY with JSON: {\"description\": string, \"amount\": number|null, \"category\": string|null, " +
                    "\"paidBy\": member name|null, \"participants\": [member names]|[], " +
                    "\"splitType\": \"EQUAL\"|\"EXACT\"|\"PERCENTAGE\"|\"SHARES\"|\"ITEMIZED\"|null}. " +
                    "Use null when unsure. Participants empty means everyone.",
                maxTokens = 300
            )
        ) ?: return null
        return parseExpenseJson(raw, memberNames)
    }

    companion object {
        val CATEGORIES = listOf(
            "Food", "Groceries", "Coffee", "Drinks", "Transport", "Bills", "Shopping", "Rent", "Health",
            "Fitness", "Travel", "Entertainment", "Subscriptions", "Education", "Gifts", "Pets", "Other"
        )

        /** Models often wrap JSON in ``` fences or add prose; keep only the JSON payload. */
        fun stripFences(text: String): String {
            var t = text.trim()
            if (t.startsWith("```")) {
                t = t.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
                t = t.substringBeforeLast("```").trim()
            }
            val objStart = t.indexOf('{'); val arrStart = t.indexOf('[')
            val start = listOf(objStart, arrStart).filter { it >= 0 }.minOrNull() ?: return t
            val end = if (start == objStart) t.lastIndexOf('}') else t.lastIndexOf(']')
            return if (end > start) t.substring(start, end + 1) else t
        }

        fun parseStringArray(text: String): List<String>? = runCatching {
            val arr = JSONArray(stripFences(text))
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }.ifEmpty { null }
        }.getOrNull()

        fun parseReceiptJson(text: String): ParsedReceipt? = runCatching {
            val obj = JSONObject(stripFences(text))
            val arr = obj.optJSONArray("items") ?: JSONArray()
            val items = (0 until arr.length()).mapNotNull { i ->
                val it = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = it.optString("name").ifBlank { return@mapNotNull null }
                ExpenseItem(id = UUID.randomUUID().toString(), name = name, price = it.optDouble("price", 0.0))
            }
            ParsedReceipt(
                merchant = obj.optString("merchant").takeIf { it.isNotBlank() && it != "null" },
                total = obj.optDouble("total").takeIf { !it.isNaN() },
                currency = obj.optString("currency").takeIf { it.length == 3 },
                dateIso = obj.optString("date").takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) },
                items = items,
                lowConfidence = obj.optString("confidence").equals("low", ignoreCase = true)
            )
        }.getOrNull()

        fun parseExpenseJson(text: String, memberNames: List<String>): ParsedExpense? = runCatching {
            val obj = JSONObject(stripFences(text))
            fun member(name: String?): String? = name?.takeIf { it.isNotBlank() && it != "null" }?.let { n ->
                memberNames.firstOrNull { it.equals(n, ignoreCase = true) }
                    ?: memberNames.firstOrNull { it.substringBefore(' ').equals(n.substringBefore(' '), ignoreCase = true) }
            }
            val parts = obj.optJSONArray("participants") ?: JSONArray()
            ParsedExpense(
                description = obj.optString("description").ifBlank { "Expense" },
                amount = obj.optDouble("amount").takeIf { !it.isNaN() && it > 0 },
                category = obj.optString("category").takeIf { c -> CATEGORIES.any { it.equals(c, true) } }
                    ?.let { c -> CATEGORIES.first { it.equals(c, true) } },
                paidByName = member(obj.optString("paidBy")),
                participantNames = (0 until parts.length()).mapNotNull { member(parts.optString(it)) },
                splitType = runCatching { SplitType.valueOf(obj.optString("splitType").uppercase()) }.getOrNull(),
                source = ParsedExpense.Source.AI
            )
        }.getOrNull()
    }
}
