package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.prompt.InvalidCommitMessageException
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
        val attempts = if (isPrimaryFreeCloud(profile)) {
            mutableListOf(
                profile,
                profile.copy(endpoint = BuiltInProfiles.FREE_FALLBACK_ENDPOINT)
            ).apply {
                if (options.autoRetryInvalidOutput) add(profile)
            }
        } else {
            listOf(profile, profile)
        }

        var lastResult: ProviderResult? = null
        for (index in attempts.indices) {
            val result = generateOnce(attempts[index], apiKey, input, options, indicator)
            if (result !is ProviderResult.Error || !result.retryable) return result

            val invalidOutput = result.message == InvalidCommitMessageException.MESSAGE
            val mayRetry = (!invalidOutput || options.autoRetryInvalidOutput) && (invalidOutput || index == 0)
            if (!mayRetry || index == attempts.lastIndex) return result
            lastResult = result
        }

        return lastResult ?: ProviderResult.Error(InvalidCommitMessageException.MESSAGE, retryable = true)
    }

    private fun isPrimaryFreeCloud(profile: ProviderProfile): Boolean {
        return profile.kind == ProviderKind.FREE_CLOUD &&
            profile.endpoint.trimEnd('/') == BuiltInProfiles.FREE_PRIMARY_ENDPOINT
    }

    private fun generateOnce(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator?
    ): ProviderResult {
        return when (profile.kind) {
            ProviderKind.GEMINI -> geminiProvider.generate(profile, apiKey, input, options, indicator)
            ProviderKind.FREE_CLOUD, ProviderKind.OPENAI_COMPATIBLE, ProviderKind.OLLAMA -> openAiProvider.generate(profile, apiKey, input, options, indicator)
            ProviderKind.ANTHROPIC -> anthropicProvider.generate(profile, apiKey, input, options, indicator)
        }
    }
}
