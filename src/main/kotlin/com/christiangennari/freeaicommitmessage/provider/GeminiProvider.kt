package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.domain.Validation
import com.christiangennari.freeaicommitmessage.prompt.CommitPromptBuilder
import com.christiangennari.freeaicommitmessage.prompt.ConventionalCommitSanitizer
import com.christiangennari.freeaicommitmessage.prompt.InvalidCommitMessageException
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

class GeminiProvider(private val httpClient: HttpClient = HttpClient.newBuilder().build()) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ContentPart(val text: String)

    @Serializable
    private data class Content(val role: String? = null, val parts: List<ContentPart>)

    @Serializable
    private data class GenerationConfig(val temperature: Double? = null)

    @Serializable
    private data class GeminiRequest(
        val systemInstruction: Content? = null,
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null
    )

    @Serializable
    private data class Candidate(val content: Content? = null)

    @Serializable
    private data class GeminiResponse(
        val candidates: List<Candidate>? = null
    )

    override fun generate(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator?
    ): ProviderResult {
        if (apiKey.isNullOrBlank()) {
            return ProviderResult.Error("API key is required for Google Gemini.", 401)
        }

        val systemPrompt = CommitPromptBuilder.buildSystemPrompt(options)
        val userPrompt = CommitPromptBuilder.buildUserPrompt(input)
        val temp = Validation.clampTemperature(profile.temperature ?: options.defaultTemperature)
        val timeout = Validation.clampTimeoutMs(options.requestTimeoutMs)

        val model = profile.model.ifBlank { "gemini-3.5-flash-lite" }
        val baseUrl = profile.endpoint.trimEnd('/')
        val url = "$baseUrl/v1beta/models/$model:generateContent"

        val requestBody = GeminiRequest(
            systemInstruction = Content(parts = listOf(ContentPart(systemPrompt))),
            contents = listOf(Content(role = "user", parts = listOf(ContentPart(userPrompt)))),
            generationConfig = GenerationConfig(temperature = temp)
        )

        val requestJson = json.encodeToString(GeminiRequest.serializer(), requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .timeout(Duration.ofMillis(timeout))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build()

        val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

        return try {
            val response = awaitResponse(future, indicator, timeout)
            if (response.statusCode() in 200..299) {
                val geminiResp = json.decodeFromString(GeminiResponse.serializer(), response.body())
                val rawText = geminiResp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText.isNullOrBlank()) {
                    ProviderResult.Error("Gemini returned an empty response.", response.statusCode())
                } else {
                    ProviderResult.Success(ConventionalCommitSanitizer.sanitize(rawText))
                }
            } else {
                val statusMsg = when (response.statusCode()) {
                    400 -> "Invalid request or model not available on this API key."
                    401, 403 -> "Invalid Gemini API key or quota exceeded."
                    429 -> "Gemini rate limit exceeded. Please retry in a moment."
                    in 500..599 -> "Gemini service temporarily unavailable."
                    else -> "Gemini API returned HTTP ${response.statusCode()}."
                }
                ProviderResult.Error(statusMsg, response.statusCode())
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
                is HttpTimeoutException, is TimeoutException -> ProviderResult.Error("Gemini request timed out.")
                is IOException -> ProviderResult.Error("Could not connect to Gemini API.")
                else -> ProviderResult.Error("Network error calling Gemini API.")
            }
        } catch (e: HttpTimeoutException) {
            future.cancel(true)
            ProviderResult.Error("Gemini request timed out after ${timeout}ms.")
        } catch (e: TimeoutException) {
            future.cancel(true)
            ProviderResult.Error("Gemini request timed out.")
        } catch (e: IOException) {
            ProviderResult.Error("Could not connect to Gemini API.")
        } catch (e: Exception) {
            ProviderResult.Error("Unexpected error calling Gemini API.")
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
