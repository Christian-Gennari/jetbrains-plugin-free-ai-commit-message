package com.christiangennari.freeaicommitmessage.config

import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.provider.BuiltInProfiles
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class FreeAiCommitMessageSettings {
    var activeProfileId: String = BuiltInProfiles.FREE_CLOUD.id
    var maxDiffCharacters: Int = 12000
    var requestTimeoutMs: Long = 120000L
    var defaultTemperature: Double = 0.2
    var promptLanguage: String = "English"
    var useGitmoji: Boolean = false
    var autoRetryInvalidOutput: Boolean = true

    @Tag("customProfiles")
    @XCollection(style = XCollection.Style.v2)
    var customProfiles: MutableList<ProviderProfile> = mutableListOf()

    fun getOptions(): GenerationOptions {
        return GenerationOptions(
            maxDiffCharacters = maxDiffCharacters,
            requestTimeoutMs = requestTimeoutMs,
            defaultTemperature = defaultTemperature,
            promptLanguage = promptLanguage,
            useGitmoji = useGitmoji,
            autoRetryInvalidOutput = autoRetryInvalidOutput
        )
    }
}
