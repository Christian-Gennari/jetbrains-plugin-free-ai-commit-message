package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile

object BuiltInProfiles {
    val FREE_CLOUD = ProviderProfile(
        id = "free",
        name = "Free (No Setup Required)",
        kind = ProviderKind.FREE_CLOUD,
        endpoint = "https://commit.cgennari.com/v1",
        model = "free",
        isBuiltIn = true
    )

    val GEMINI = ProviderProfile(
        id = "gemini",
        name = "Google Gemini (gemini-3.5-flash-lite)",
        kind = ProviderKind.GEMINI,
        endpoint = "https://generativelanguage.googleapis.com",
        model = "gemini-3.5-flash-lite",
        isBuiltIn = true
    )

    val GROQ = ProviderProfile(
        id = "groq",
        name = "Groq LPU (gpt-oss-120b)",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        endpoint = "https://api.groq.com/openai/v1",
        model = "openai/gpt-oss-120b",
        isBuiltIn = true
    )

    val OLLAMA = ProviderProfile(
        id = "ollama",
        name = "Local Ollama (qwen2.5-coder:3b)",
        kind = ProviderKind.OLLAMA,
        endpoint = "http://localhost:11434/v1",
        model = "qwen2.5-coder:3b",
        isBuiltIn = true
    )

    val OPENROUTER = ProviderProfile(
        id = "openrouter",
        name = "OpenRouter Free (openrouter/free)",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        endpoint = "https://openrouter.ai/api/v1",
        model = "openrouter/free",
        isBuiltIn = true
    )

    val GITHUB_MODELS = ProviderProfile(
        id = "github-models",
        name = "GitHub Models (gpt-4o-mini)",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        endpoint = "https://models.inference.ai.azure.com",
        model = "gpt-4o-mini",
        isBuiltIn = true
    )

    val DEEPSEEK = ProviderProfile(
        id = "deepseek",
        name = "DeepSeek (deepseek-chat)",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        endpoint = "https://api.deepseek.com/v1",
        model = "deepseek-chat",
        isBuiltIn = true
    )

    val OPENAI = ProviderProfile(
        id = "openai",
        name = "OpenAI (gpt-4o-mini)",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        endpoint = "https://api.openai.com/v1",
        model = "gpt-4o-mini",
        isBuiltIn = true
    )

    val ANTHROPIC = ProviderProfile(
        id = "anthropic",
        name = "Anthropic Claude (claude-3-5-haiku)",
        kind = ProviderKind.ANTHROPIC,
        endpoint = "https://api.anthropic.com/v1",
        model = "claude-3-5-haiku-20241022",
        isBuiltIn = true
    )

    val ALL_PRESETS = listOf(
        FREE_CLOUD,
        GEMINI,
        GROQ,
        OLLAMA,
        OPENROUTER,
        GITHUB_MODELS,
        DEEPSEEK,
        OPENAI,
        ANTHROPIC
    )

    fun findById(id: String): ProviderProfile? {
        return ALL_PRESETS.firstOrNull { it.id == id }
    }
}
