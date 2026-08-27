package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.intellij.openapi.progress.ProgressIndicator

class AiProviderEngine(
    private val geminiProvider: AiProvider = GeminiProvider(),
    private val openAiProvider: AiProvider = OpenAiCompatibleProvider(),
    private val anthropicProvider: AiProvider = AnthropicProvider()
) {

    fun generate(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator? = null
    ): ProviderResult {
        return when (profile.kind) {
            ProviderKind.GEMINI -> geminiProvider.generate(profile, apiKey, input, options, indicator)
            ProviderKind.FREE_CLOUD, ProviderKind.OPENAI_COMPATIBLE, ProviderKind.OLLAMA -> openAiProvider.generate(profile, apiKey, input, options, indicator)
            ProviderKind.ANTHROPIC -> anthropicProvider.generate(profile, apiKey, input, options, indicator)
        }
    }
}
