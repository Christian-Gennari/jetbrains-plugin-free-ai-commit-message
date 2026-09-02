package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.CommitMessage
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.prompt.InvalidCommitMessageException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiProviderEngineTest {

    @Test
    fun `retries one invalid provider result`() {
        var calls = 0
        val provider = object : AiProvider {
            override fun generate(
                profile: ProviderProfile,
                apiKey: String?,
                input: CommitInput,
                options: GenerationOptions,
                indicator: com.intellij.openapi.progress.ProgressIndicator?
            ): ProviderResult {
                calls += 1
                return if (calls == 1) {
                    ProviderResult.Error(InvalidCommitMessageException.MESSAGE, retryable = true)
                } else {
                    ProviderResult.Success(CommitMessage("fix: use validated output"))
                }
            }
        }

        val engine = AiProviderEngine(openAiProvider = provider)
        val result = engine.generate(
            ProviderProfile("test", "Test", ProviderKind.OPENAI_COMPATIBLE, "https://example.com/v1", "test-model"),
            "test-key",
            CommitInput("diff", "", emptyList()),
            GenerationOptions()
        )

        assertTrue(result is ProviderResult.Success)
        assertEquals("fix: use validated output", (result as ProviderResult.Success).message.subject)
        assertEquals(2, calls)
    }

    @Test
    fun `retries invalid output from both free endpoints before succeeding`() {
        var calls = 0
        val provider = object : AiProvider {
            override fun generate(
                profile: ProviderProfile,
                apiKey: String?,
                input: CommitInput,
                options: GenerationOptions,
                indicator: com.intellij.openapi.progress.ProgressIndicator?
            ): ProviderResult {
                calls += 1
                return if (calls < 3) {
                    ProviderResult.Error(InvalidCommitMessageException.MESSAGE, retryable = true)
                } else {
                    ProviderResult.Success(CommitMessage("fix: recover after automatic retry"))
                }
            }
        }

        val engine = AiProviderEngine(openAiProvider = provider)
        val result = engine.generate(
            BuiltInProfiles.FREE_CLOUD,
            null,
            CommitInput("diff", "", emptyList()),
            GenerationOptions()
        )

        assertTrue(result is ProviderResult.Success)
        assertEquals(3, calls)
    }

    @Test
    fun `does not retry invalid output when automatic retry is disabled`() {
        var calls = 0
        val provider = object : AiProvider {
            override fun generate(
                profile: ProviderProfile,
                apiKey: String?,
                input: CommitInput,
                options: GenerationOptions,
                indicator: com.intellij.openapi.progress.ProgressIndicator?
            ): ProviderResult {
                calls += 1
                return ProviderResult.Error(InvalidCommitMessageException.MESSAGE, retryable = true)
            }
        }

        val engine = AiProviderEngine(openAiProvider = provider)
        val result = engine.generate(
            ProviderProfile("test", "Test", ProviderKind.OPENAI_COMPATIBLE, "https://example.com/v1", "test-model"),
            "test-key",
            CommitInput("diff", "", emptyList()),
            GenerationOptions(autoRetryInvalidOutput = false)
        )

        assertTrue(result is ProviderResult.Error)
        assertEquals(1, calls)
    }
}
