package com.christiangennari.freeaicommitmessage.config

import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import com.christiangennari.freeaicommitmessage.provider.BuiltInProfiles
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "FreeAiCommitMessageSettings",
    storages = [Storage("freeAiCommitMessage.xml")]
)
class FreeAiCommitMessageSettingsService : PersistentStateComponent<FreeAiCommitMessageSettings> {

    private var myState = FreeAiCommitMessageSettings()
    val secretStore: SecretStore = PasswordSafeSecretStore()

    override fun getState(): FreeAiCommitMessageSettings {
        return myState
    }

    override fun loadState(state: FreeAiCommitMessageSettings) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getAllProfiles(): List<ProviderProfile> {
        return BuiltInProfiles.ALL_PRESETS + myState.customProfiles
    }

    fun getActiveProfile(): ProviderProfile {
        val found = getAllProfiles().firstOrNull { it.id == myState.activeProfileId }
        return found ?: BuiltInProfiles.GEMINI
    }

    companion object {
        val instance: FreeAiCommitMessageSettingsService
            get() = ApplicationManager.getApplication().getService(FreeAiCommitMessageSettingsService::class.java)
    }
}
