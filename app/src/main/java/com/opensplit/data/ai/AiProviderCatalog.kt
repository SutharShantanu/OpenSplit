package com.opensplit.data.ai

/** A supported AI backend. Keys and chosen model are stored per provider id in preferences. */
data class AiProviderInfo(
    val id: String,
    val name: String,
    val description: String,
    val defaultModel: String,
    val models: List<String>,
    val defaultBaseUrl: String,
    val helpUrl: String,
    /** Cheapest model used by "Test key"; falls back to [defaultModel]. */
    val testModel: String = defaultModel
)

val AI_PROVIDERS_CATALOG = listOf(
    AiProviderInfo(
        id = "anthropic",
        name = "Anthropic Claude",
        description = "Strong document understanding for receipts and natural-language expenses.",
        defaultModel = "claude-opus-5-5",
        models = listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5"),
        defaultBaseUrl = "https://api.anthropic.com/v1",
        helpUrl = "https://console.anthropic.com/settings/keys",
        testModel = "claude-haiku-5-5"
    ),
    AiProviderInfo(
        id = "openai",
        name = "OpenAI",
        description = "GPT models for parsing, categorisation and insights.",
        defaultModel = "gpt-4o-mini",
        models = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1"),
        defaultBaseUrl = "https://api.openai.com/v1",
        helpUrl = "https://platform.openai.com/api-keys"
    ),
    AiProviderInfo(
        id = "gemini",
        name = "Google Gemini",
        description = "Fast multimodal models with a generous free tier.",
        defaultModel = "gemini-2.5-flash",
        models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite", "gemini-3-flash-preview", "gemini-3-pro-preview"),
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        helpUrl = "https://aistudio.google.com/app/apikey"
    ),
    AiProviderInfo(
        id = "groq",
        name = "Groq",
        description = "Very fast inference for open-weight models (OpenAI-compatible).",
        defaultModel = "llama-3.3-70b-versatile",
        models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        helpUrl = "https://console.groq.com/keys",
        testModel = "llama-3.1-8b-instant"
    ),
    AiProviderInfo(
        id = "mistral",
        name = "Mistral AI",
        description = "European open-weight and specialised models (OpenAI-compatible).",
        defaultModel = "mistral-small-latest",
        models = listOf("mistral-small-latest", "mistral-large-latest"),
        defaultBaseUrl = "https://api.mistral.ai/v1",
        helpUrl = "https://console.mistral.ai/api-keys"
    ),
    AiProviderInfo(
        id = "deepseek",
        name = "DeepSeek",
        description = "Efficient reasoning models (OpenAI-compatible).",
        defaultModel = "deepseek-chat",
        models = listOf("deepseek-chat", "deepseek-reasoner"),
        defaultBaseUrl = "https://api.deepseek.com",
        helpUrl = "https://platform.deepseek.com/api_keys"
    ),
    AiProviderInfo(
        id = "custom",
        name = "Custom / Local (Ollama, OpenRouter)",
        description = "Any OpenAI-compatible endpoint, including self-hosted models.",
        defaultModel = "llama3:latest",
        models = listOf("llama3:latest", "mistral:latest", "qwen2.5:latest"),
        defaultBaseUrl = "http://localhost:11434/v1",
        helpUrl = "https://ollama.com"
    )
)

fun providerInfo(id: String): AiProviderInfo = AI_PROVIDERS_CATALOG.first { it.id == id }
