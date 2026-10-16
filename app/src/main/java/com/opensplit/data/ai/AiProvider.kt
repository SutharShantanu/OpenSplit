package com.opensplit.data.ai

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Resolved settings for one call: which backend, with which key, model and base URL. */
data class AiConfig(val providerId: String, val apiKey: String, val model: String, val baseUrl: String)

/** One prompt, optionally with an image; the answer is expected as plain text (usually JSON). */
data class AiRequest(
    val prompt: String,
    val imageBytes: ByteArray? = null,
    val imageMime: String = "image/jpeg",
    val maxTokens: Int = 1024
)

class AiException(message: String, val status: Int? = null) : Exception(message)

/** One answer plus the tokens the provider billed for it (null when the API doesn't report usage). */
data class AiReply(val text: String, val tokens: Int?)

/**
 * A chat-completion backend. [buildBody] and [extractText] are pure so the wire format can be
 * unit-tested; [complete] does the HTTP round trip.
 */
abstract class AiProvider {
    abstract fun url(config: AiConfig): String
    abstract fun headers(config: AiConfig): Map<String, String>
    abstract fun buildBody(config: AiConfig, request: AiRequest): JSONObject
    abstract fun extractText(body: JSONObject): String
    /** Billed tokens for one reply, from the provider's usage block. */
    abstract fun extractTokens(body: JSONObject): Int?
    /** Endpoint that lists the models this key can use. */
    abstract fun modelsUrl(config: AiConfig): String
    /** Model ids from a [modelsUrl] response, in display order. Pure, so unit-testable. */
    abstract fun parseModels(body: JSONObject): List<String>

    /** Live model list for this key; never a hardcoded catalogue. */
    fun listModels(client: OkHttpClient, config: AiConfig): List<String> {
        val builder = Request.Builder().url(modelsUrl(config)).get()
        headers(config).forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AiException(errorMessage(text, response.code), response.code)
            return parseModels(JSONObject(text))
        }
    }

    fun complete(client: OkHttpClient, config: AiConfig, request: AiRequest): AiReply {
        val builder = Request.Builder().url(url(config))
            .post(buildBody(config, request).toString().toRequestBody(JSON))
        headers(config).forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiException(errorMessage(text, response.code), response.code)
            }
            val json = JSONObject(text)
            return AiReply(extractText(json), extractTokens(json))
        }
    }

    private fun errorMessage(body: String, code: Int): String = runCatching {
        val err = JSONObject(body).optJSONObject("error")
        err?.optString("message")?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: "HTTP $code"

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
        fun forProvider(id: String): AiProvider = when (id) {
            "gemini" -> GeminiProvider
            "anthropic" -> AnthropicProvider
            else -> OpenAiCompatibleProvider
        }
        fun base64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

/** Gemini `generateContent`; the key travels in a header, never in the URL. */
object GeminiProvider : AiProvider() {
    override fun url(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/models/${config.model}:generateContent"
    override fun headers(config: AiConfig) = mapOf("x-goog-api-key" to config.apiKey)
    override fun buildBody(config: AiConfig, request: AiRequest): JSONObject {
        val parts = JSONArray().put(JSONObject().put("text", request.prompt))
        request.imageBytes?.let {
            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", request.imageMime).put("data", base64(it))))
        }
        return JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("maxOutputTokens", request.maxTokens))
    }
    override fun extractText(body: JSONObject): String {
        // Thinking models can return a candidate with no parts when the output budget is spent.
        val candidate = body.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw AiException(body.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotBlank() }?.let { "Blocked: $it" } ?: "No candidates in response")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        for (i in 0 until (parts?.length() ?: 0)) {
            val text = parts!!.getJSONObject(i).optString("text")
            if (text.isNotBlank()) return text
        }
        throw AiException("Empty reply (finishReason=${candidate.optString("finishReason").ifBlank { "unknown" }})")
    }
    override fun extractTokens(body: JSONObject): Int? = body.optJSONObject("usageMetadata")?.optInt("totalTokenCount")?.takeIf { it > 0 }
    override fun modelsUrl(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/models?pageSize=200"
    /** Only models that can answer `generateContent`; embedding/image-only models are skipped. */
    override fun parseModels(body: JSONObject): List<String> {
        val models = body.optJSONArray("models") ?: JSONArray()
        return (0 until models.length()).map { models.getJSONObject(it) }
            .filter { m -> val methods = m.optJSONArray("supportedGenerationMethods"); methods == null || (0 until methods.length()).any { methods.getString(it) == "generateContent" } }
            .map { it.getString("name").removePrefix("models/") }
    }
}

/** OpenAI `chat/completions`; also serves Groq, Mistral, DeepSeek and custom endpoints. */
object OpenAiCompatibleProvider : AiProvider() {
    override fun url(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/chat/completions"
    override fun headers(config: AiConfig) =
        if (config.apiKey.isBlank()) emptyMap() else mapOf("Authorization" to "Bearer ${config.apiKey}")
    override fun buildBody(config: AiConfig, request: AiRequest): JSONObject {
        val content: Any = if (request.imageBytes == null) request.prompt else JSONArray()
            .put(JSONObject().put("type", "text").put("text", request.prompt))
            .put(
                JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", "data:${request.imageMime};base64,${base64(request.imageBytes)}")
                )
            )
        return JSONObject()
            .put("model", config.model)
            .put("max_tokens", request.maxTokens)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
    }
    override fun extractText(body: JSONObject): String = body
        .getJSONArray("choices").getJSONObject(0)
        .getJSONObject("message").getString("content")
    override fun extractTokens(body: JSONObject): Int? = body.optJSONObject("usage")?.optInt("total_tokens")?.takeIf { it > 0 }
    override fun modelsUrl(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/models"
    override fun parseModels(body: JSONObject): List<String> = idsOf(body.optJSONArray("data")).sorted()
}

/** `{ "data": [ { "id": ... } ] }`, the shape OpenAI, Groq, Mistral, DeepSeek, Ollama and Anthropic share. */
internal fun idsOf(data: JSONArray?): List<String> =
    if (data == null) emptyList() else (0 until data.length()).mapNotNull { data.getJSONObject(it).optString("id").takeIf { id -> id.isNotBlank() } }

/** Anthropic Messages API. Image blocks go before the text block, per the API docs. */
object AnthropicProvider : AiProvider() {
    override fun url(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/messages"
    override fun headers(config: AiConfig) = mapOf(
        "x-api-key" to config.apiKey,
        "anthropic-version" to "2023-06-01"
    )
    override fun buildBody(config: AiConfig, request: AiRequest): JSONObject {
        val content = JSONArray()
        request.imageBytes?.let {
            content.put(
                JSONObject().put("type", "image").put(
                    "source",
                    JSONObject().put("type", "base64").put("media_type", request.imageMime).put("data", base64(it))
                )
            )
        }
        content.put(JSONObject().put("type", "text").put("text", request.prompt))
        return JSONObject()
            .put("model", config.model)
            .put("max_tokens", request.maxTokens)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
    }
    override fun extractText(body: JSONObject): String {
        if (body.optString("stop_reason") == "refusal") throw AiException("The model declined this request.")
        val content = body.getJSONArray("content")
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            if (block.optString("type") == "text") return block.getString("text")
        }
        throw AiException("No text in response")
    }
    override fun extractTokens(body: JSONObject): Int? = body.optJSONObject("usage")?.let { it.optInt("input_tokens") + it.optInt("output_tokens") }?.takeIf { it > 0 }
    override fun modelsUrl(config: AiConfig) = "${config.baseUrl.trimEnd('/')}/models?limit=1000"
    override fun parseModels(body: JSONObject): List<String> = idsOf(body.optJSONArray("data"))
}
