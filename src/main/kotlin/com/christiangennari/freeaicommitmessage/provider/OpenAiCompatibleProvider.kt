package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.domain.Validation
import com.christiangennari.freeaicommitmessage.prompt.CommitPromptBuilder
import com.christiangennari.freeaicommitmessage.prompt.ConventionalCommitSanitizer
import com.christiangennari.freeaicommitmessage.prompt.InvalidCommitMessageException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import kotlinx.serialization.SerialName
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

class OpenAiCompatibleProvider(private val httpClient: HttpClient = HttpClient.newBuilder().build()) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double? = null
    )

    @Serializable
    private data class Choice(
        val message: Message? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    private data class ChatCompletionResponse(
        val choices: List<Choice>? = null
    )

    override fun generate(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator?
    ): ProviderResult {
        if (profile.kind.requiresApiKey && apiKey.isNullOrBlank()) {
            return ProviderResult.Error("API key is required for ${profile.name}.", 401)
        }

        val systemPrompt = CommitPromptBuilder.buildSystemPrompt(options)
        val userPrompt = CommitPromptBuilder.buildUserPrompt(input)
        val temp = Validation.clampTemperature(profile.temperature ?: options.defaultTemperature)
        val timeout = Validation.clampTimeoutMs(options.requestTimeoutMs)

        var endpoint = profile.endpoint.trimEnd('/')
        if (!endpoint.endsWith("/chat/completions")) {
            endpoint = if (endpoint.endsWith("/v1")) "$endpoint/chat/completions" else "$endpoint/v1/chat/completions"
        }

        val requestBody = ChatCompletionRequest(
            model = profile.model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = temp
        )

        val requestJson = json.encodeToString(ChatCompletionRequest.serializer(), requestBody)

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(timeout))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))

        if (profile.kind.requiresApiKey && !apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        requestBuilder.header("Accept", "application/json")
        requestBuilder.header("User-Agent", CLIENT_USER_AGENT)

        val future = httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        return try {
            val response = awaitResponse(future, indicator, timeout)
            if (response.statusCode() in 200..299) {
                val completionResp = json.decodeFromString(ChatCompletionResponse.serializer(), response.body())
                val choice = completionResp.choices?.firstOrNull()
                if (choice?.finishReason == "length") {
                    throw InvalidCommitMessageException()
                }
                val rawText = choice?.message?.content
                if (rawText.isNullOrBlank()) {
                    ProviderResult.Error("Provider returned an empty response.", response.statusCode(), retryable = true)
                } else {
                    ProviderResult.Success(ConventionalCommitSanitizer.sanitize(rawText))
                }
            } else {
                val statusMsg = when (response.statusCode()) {
                    401, 403 -> "Authentication failed for ${profile.name}. Please check your API key."
                    429 -> "Rate limit exceeded for ${profile.name}."
                    in 500..599 -> "${profile.name} service error (HTTP ${response.statusCode()})."
                    else -> "${profile.name} returned HTTP ${response.statusCode()}."
                }
                ProviderResult.Error(
                    statusMsg,
                    response.statusCode(),
                    retryable = response.statusCode() == 408 || response.statusCode() == 429 || response.statusCode() in 500..599
                )
            }
        } catch (e: InvalidCommitMessageException) {
            ProviderResult.Error(e.message ?: InvalidCommitMessageException.MESSAGE, retryable = true)
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
                is HttpTimeoutException, is TimeoutException -> ProviderResult.Error("${profile.name} request timed out.", retryable = true)
                is IOException -> ProviderResult.Error("Could not connect to ${profile.name}.", retryable = true)
                else -> ProviderResult.Error("Network error calling ${profile.name}.")
            }
        } catch (e: HttpTimeoutException) {
            future.cancel(true)
            ProviderResult.Error("${profile.name} request timed out.", retryable = true)
        } catch (e: TimeoutException) {
            future.cancel(true)
            ProviderResult.Error("${profile.name} request timed out.", retryable = true)
        } catch (e: IOException) {
            ProviderResult.Error("Could not connect to ${profile.name}.", retryable = true)
        } catch (e: Exception) {
            ProviderResult.Error("Unexpected error calling ${profile.name}.")
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

    private companion object {
        const val CLIENT_USER_AGENT = "Free-AI-Commit-Message/1.0"
    }
}
