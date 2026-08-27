package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.domain.Validation
import com.christiangennari.freeaicommitmessage.prompt.CommitPromptBuilder
import com.christiangennari.freeaicommitmessage.prompt.ConventionalCommitSanitizer
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AnthropicProvider(private val httpClient: HttpClient = HttpClient.newBuilder().build()) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class AnthropicRequest(
        val model: String,
        val max_tokens: Int = 1024,
        val system: String,
        val messages: List<Message>,
        val temperature: Double? = null
    )

    @Serializable
    private data class ContentBlock(val type: String? = null, val text: String? = null)

    @Serializable
    private data class AnthropicResponse(
        val content: List<ContentBlock>? = null
    )

    override fun generate(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator?
    ): ProviderResult {
        if (apiKey.isNullOrBlank()) {
            return ProviderResult.Error("API key is required for Anthropic Claude.", 401)
        }

        val systemPrompt = CommitPromptBuilder.buildSystemPrompt(options)
        val userPrompt = CommitPromptBuilder.buildUserPrompt(input)
        val temp = Validation.clampTemperature(profile.temperature ?: options.defaultTemperature)
        val timeout = Validation.clampTimeoutMs(options.requestTimeoutMs)

        var endpoint = profile.endpoint.trimEnd('/')
        if (!endpoint.endsWith("/messages")) {
            endpoint = if (endpoint.endsWith("/v1")) "$endpoint/messages" else "$endpoint/v1/messages"
        }

        val requestBody = AnthropicRequest(
            model = profile.model.ifBlank { "claude-3-5-haiku-20241022" },
            system = systemPrompt,
            messages = listOf(Message(role = "user", content = userPrompt)),
            temperature = temp
        )

        val requestJson = json.encodeToString(AnthropicRequest.serializer(), requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .timeout(Duration.ofMillis(timeout))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build()

        val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

        return try {
            val response = awaitResponse(future, indicator, timeout)
            if (response.statusCode() in 200..299) {
                val anthropicResp = json.decodeFromString(AnthropicResponse.serializer(), response.body())
                val rawText = anthropicResp.content
                    ?.filter { it.type == "text" }
                    ?.joinToString("\n") { it.text ?: "" }
                    ?.trim()
                if (rawText.isNullOrBlank()) {
                    ProviderResult.Error("Claude returned an empty response.", response.statusCode())
                } else {
                    ProviderResult.Success(ConventionalCommitSanitizer.sanitize(rawText))
                }
            } else {
                val statusMsg = when (response.statusCode()) {
                    401, 403 -> "Authentication failed for Anthropic Claude. Please check your API key."
                    429 -> "Anthropic Claude rate limit exceeded."
                    in 500..599 -> "Anthropic Claude service error."
                    else -> "Anthropic Claude returned HTTP ${response.statusCode()}."
                }
                ProviderResult.Error(statusMsg, response.statusCode())
            }
        } catch (e: ProcessCanceledException) {
            future.cancel(true)
            ProviderResult.Cancelled
        } catch (e: CancellationException) {
            future.cancel(true)
            ProviderResult.Cancelled
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            ProviderResult.Cancelled
        } catch (e: ExecutionException) {
            future.cancel(true)
            when (e.cause) {
                is ProcessCanceledException, is CancellationException -> ProviderResult.Cancelled
                is InterruptedException -> {
                    Thread.currentThread().interrupt()
                    ProviderResult.Cancelled
                }
                is HttpTimeoutException, is TimeoutException -> ProviderResult.Error("Anthropic Claude request timed out.")
                is IOException -> ProviderResult.Error("Could not connect to Anthropic Claude.")
                else -> ProviderResult.Error("Network error calling Anthropic Claude.")
            }
        } catch (e: HttpTimeoutException) {
            future.cancel(true)
            ProviderResult.Error("Anthropic Claude request timed out.")
        } catch (e: TimeoutException) {
            future.cancel(true)
            ProviderResult.Error("Anthropic Claude request timed out.")
        } catch (e: IOException) {
            ProviderResult.Error("Could not connect to Anthropic Claude.")
        } catch (e: Exception) {
            ProviderResult.Error("Unexpected error calling Anthropic Claude.")
        }
    }

    private fun <T> awaitResponse(
        future: java.util.concurrent.CompletableFuture<T>,
        indicator: ProgressIndicator?,
        timeoutMs: Long
    ): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            if (indicator != null && indicator.isCanceled) {
                future.cancel(true)
                throw ProcessCanceledException()
            }
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) {
                future.cancel(true)
                throw TimeoutException("Request timed out")
            }
            try {
                return future.get(minOf(remaining, 100L), TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Poll check for indicator cancellation
            }
        }
    }
}
