package com.christiangennari.freeaicommitmessage.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderKind(val displayName: String, val defaultEndpoint: String, val requiresApiKey: Boolean) {
    FREE_CLOUD("Free (No Setup Required)", "https://commit.cgennari.com/v1", false),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com", true),
    OPENAI_COMPATIBLE("OpenAI-Compatible", "https://api.groq.com/openai/v1", true),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/v1", true),
    OLLAMA("Local Ollama", "http://localhost:11434/v1", false)
}
