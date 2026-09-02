package com.christiangennari.freeaicommitmessage.provider

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.CommitMessage
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.intellij.openapi.progress.ProgressIndicator

sealed class ProviderResult {
    data class Success(val message: CommitMessage) : ProviderResult()
    data class Error(val message: String, val statusCode: Int? = null, val retryable: Boolean = false) : ProviderResult()
    object Cancelled : ProviderResult()
}

interface AiProvider {
    fun generate(
        profile: ProviderProfile,
        apiKey: String?,
        input: CommitInput,
        options: GenerationOptions,
        indicator: ProgressIndicator? = null
    ): ProviderResult
}
