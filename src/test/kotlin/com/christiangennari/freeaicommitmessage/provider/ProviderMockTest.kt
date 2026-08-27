package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import javax.net.ssl.SSLSession

class ProviderMockTest {

    class MockHttpResponse(
        private val statusCode: Int,
        private val body: String,
        private val headersMap: Map<String, List<String>> = emptyMap()
    ) : HttpResponse<String> {
        override fun statusCode(): Int = statusCode
        override fun body(): String = body
        override fun request(): HttpRequest = throw UnsupportedOperationException()
        override fun previousResponse(): java.util.Optional<HttpResponse<String>> = java.util.Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(headersMap) { _, _ -> true }
        override fun sslSession(): java.util.Optional<SSLSession> = java.util.Optional.empty()
        override fun uri(): java.net.URI = java.net.URI.create("https://mock.api")
        override fun version(): HttpClient.Version = HttpClient.Version.HTTP_2
    }

    class MockHttpClient(private val responseProvider: (HttpRequest) -> HttpResponse<String>) : HttpClient() {
        var lastCapturedRequest: HttpRequest? = null

        override fun <T : Any?> sendAsync(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>?
        ): CompletableFuture<HttpResponse<T>> {
            lastCapturedRequest = request
            val resp = responseProvider(request) as HttpResponse<T>
            return CompletableFuture.completedFuture(resp)
        }

        override fun <T : Any?> sendAsync(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>?,
            pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?
        ): CompletableFuture<HttpResponse<T>> = throw UnsupportedOperationException()

        override fun <T : Any?> send(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>?
        ): HttpResponse<T> = throw UnsupportedOperationException()

        override fun cookieHandler(): java.util.Optional<java.net.CookieHandler> = java.util.Optional.empty()
        override fun connectTimeout(): java.util.Optional<java.time.Duration> = java.util.Optional.empty()
        override fun followRedirects(): Redirect = Redirect.NEVER
        override fun proxy(): java.util.Optional<java.net.ProxySelector> = java.util.Optional.empty()
        override fun sslContext(): javax.net.ssl.SSLContext = javax.net.ssl.SSLContext.getDefault()
        override fun sslParameters(): javax.net.ssl.SSLParameters = javax.net.ssl.SSLParameters()
        override fun authenticator(): java.util.Optional<java.net.Authenticator> = java.util.Optional.empty()
        override fun version(): Version = Version.HTTP_2
        override fun executor(): java.util.Optional<java.util.concurrent.Executor> = java.util.Optional.empty()
    }

    @Test
    fun `test gemini provider sets x-goog-api-key header and parses candidate`() {
        val mockClient = MockHttpClient { req ->
            val jsonResponse = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    { "text": "feat(api): add auth endpoint" }
                                ]
                            }
                        }
                    ]
                }
            """.trimIndent()
            MockHttpResponse(200, jsonResponse)
        }

        val provider = GeminiProvider(mockClient)
        val profile = BuiltInProfiles.GEMINI
        val input = CommitInput("staged diff", "", emptyList())
        val options = GenerationOptions()

        val result = provider.generate(profile, "test-gemini-key", input, options)

        assertTrue(result is ProviderResult.Success)
        assertEquals("feat(api): add auth endpoint", (result as ProviderResult.Success).message.subject)

        // Verify key was passed in header, NOT in URI query
        val req = mockClient.lastCapturedRequest!!
        assertEquals("test-gemini-key", req.headers().firstValue("x-goog-api-key").orElse(null))
        assertTrue(req.uri().query.isNullOrBlank())
    }

    @Test
    fun `test openai-compatible provider sets Authorization header`() {
        val mockClient = MockHttpClient { req ->
            val jsonResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "fix(core): resolve null pointer"
                            }
                        }
                    ]
                }
            """.trimIndent()
            MockHttpResponse(200, jsonResponse)
        }

        val provider = OpenAiCompatibleProvider(mockClient)
        val profile = BuiltInProfiles.GROQ
        val input = CommitInput("diff", "", emptyList())

        val result = provider.generate(profile, "groq-key-123", input, GenerationOptions())

        assertTrue(result is ProviderResult.Success)
        assertEquals("fix(core): resolve null pointer", (result as ProviderResult.Success).message.subject)

        val req = mockClient.lastCapturedRequest!!
        assertEquals("Bearer groq-key-123", req.headers().firstValue("Authorization").orElse(null))
    }

    @Test
    fun `test anthropic provider sets x-api-key header and parses content blocks`() {
        val mockClient = MockHttpClient { req ->
            val jsonResponse = """
                {
                    "content": [
                        {
                            "type": "text",
                            "text": "refactor(git): optimize diff parser"
                        }
                    ]
                }
            """.trimIndent()
            MockHttpResponse(200, jsonResponse)
        }

        val provider = AnthropicProvider(mockClient)
        val profile = BuiltInProfiles.ANTHROPIC
        val input = CommitInput("diff", "", emptyList())

        val result = provider.generate(profile, "sk-ant-test", input, GenerationOptions())

        assertTrue(result is ProviderResult.Success)
        assertEquals("refactor(git): optimize diff parser", (result as ProviderResult.Success).message.subject)

        val req = mockClient.lastCapturedRequest!!
        assertEquals("sk-ant-test", req.headers().firstValue("x-api-key").orElse(null))
        assertEquals("2023-06-01", req.headers().firstValue("anthropic-version").orElse(null))
    }

    @Test
    fun `test 401 status code mapping without leaking error body`() {
        val mockClient = MockHttpClient {
            MockHttpResponse(401, "{\"error\": {\"message\": \"internal secret token leaked\"}}")
        }

        val provider = GeminiProvider(mockClient)
        val result = provider.generate(BuiltInProfiles.GEMINI, "invalid-key", CommitInput("diff", "", emptyList()), GenerationOptions())

        assertTrue(result is ProviderResult.Error)
        val error = result as ProviderResult.Error
        assertEquals(401, error.statusCode)
        assertTrue(error.message.contains("Invalid Gemini API key"))
        assertTrue(!error.message.contains("internal secret token"))
    }

    @Test
    fun `test free cloud provider generates commit message with zero api key required`() {
        val mockClient = MockHttpClient { req ->
            val jsonResponse = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "feat: zero setup commit message"
                            }
                        }
                    ]
                }
            """.trimIndent()
            MockHttpResponse(200, jsonResponse)
        }

        val provider = OpenAiCompatibleProvider(mockClient)
        val profile = BuiltInProfiles.FREE_CLOUD
        val input = CommitInput("diff", "", emptyList())

        val result = provider.generate(profile, null, input, GenerationOptions())

        assertTrue(result is ProviderResult.Success)
        assertEquals("feat: zero setup commit message", (result as ProviderResult.Success).message.subject)
    }
}
