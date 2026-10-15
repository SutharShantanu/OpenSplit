package com.opensplit.data.ai

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiProviderTest {
    private val img = byteArrayOf(1, 2, 3)

    @Test
    fun `anthropic request puts image before text and parses first text block`() {
        val cfg = AiConfig("anthropic", "k", "claude-opus-5-5", "https://api.anthropic.com/v1")
        val body = AnthropicProvider.buildBody(cfg, AiRequest("hi", img, "image/png", 99))
        val content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("image", content.getJSONObject(0).getString("type"))
        assertEquals("image/png", content.getJSONObject(0).getJSONObject("source").getString("media_type"))
        assertEquals("text", content.getJSONObject(1).getString("type"))
        assertEquals(99, body.getInt("max_tokens"))
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicProvider.url(cfg))
        assertEquals("2023-06-01", AnthropicProvider.headers(cfg)["anthropic-version"])
        val reply = JSONObject("""{"content":[{"type":"thinking","thinking":""},{"type":"text","text":"OK"}],"stop_reason":"end_turn"}""")
        assertEquals("OK", AnthropicProvider.extractText(reply))
    }

    @Test(expected = AiException::class)
    fun `anthropic refusal surfaces as an error`() {
        AnthropicProvider.extractText(JSONObject("""{"content":[],"stop_reason":"refusal"}"""))
    }

    @Test
    fun `openai compatible request uses data url for images and bearer auth`() {
        val cfg = AiConfig("openai", "k", "gpt-4o-mini", "https://api.openai.com/v1/")
        val body = OpenAiCompatibleProvider.buildBody(cfg, AiRequest("hi", img))
        val content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertTrue(content.getJSONObject(1).getJSONObject("image_url").getString("url").startsWith("data:image/jpeg;base64,"))
        assertEquals("https://api.openai.com/v1/chat/completions", OpenAiCompatibleProvider.url(cfg))
        assertEquals("Bearer k", OpenAiCompatibleProvider.headers(cfg)["Authorization"])
        assertTrue(OpenAiCompatibleProvider.headers(cfg.copy(apiKey = "")).isEmpty())
        val reply = JSONObject("""{"choices":[{"message":{"role":"assistant","content":"Food"}}]}""")
        assertEquals("Food", OpenAiCompatibleProvider.extractText(reply))
    }

    @Test
    fun `gemini keeps the key out of the url`() {
        val cfg = AiConfig("gemini", "secret", "gemini-2.0-flash", "https://generativelanguage.googleapis.com/v1beta")
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent", GeminiProvider.url(cfg))
        assertEquals("secret", GeminiProvider.headers(cfg)["x-goog-api-key"])
        val parts = GeminiProvider.buildBody(cfg, AiRequest("hi", img)).getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertEquals("hi", parts.getJSONObject(0).getString("text"))
        assertEquals("image/jpeg", parts.getJSONObject(1).getJSONObject("inline_data").getString("mime_type"))
    }

    @Test
    fun `provider registry routes openai-compatible ids together`() {
        assertEquals(OpenAiCompatibleProvider, AiProvider.forProvider("groq"))
        assertEquals(OpenAiCompatibleProvider, AiProvider.forProvider("custom"))
        assertEquals(AnthropicProvider, AiProvider.forProvider("anthropic"))
        assertEquals(GeminiProvider, AiProvider.forProvider("gemini"))
    }
}
