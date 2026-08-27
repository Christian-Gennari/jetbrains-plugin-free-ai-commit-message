package com.christiangennari.freeaicommitmessage.config

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

class PasswordSafeSecretStore : SecretStore {

    private fun createAttributes(profileId: String): CredentialAttributes {
        val serviceName = generateServiceName("FreeAiCommitMessage", profileId)
        return CredentialAttributes(serviceName)
    }

    override fun getApiKey(profileId: String): String? {
        val attributes = createAttributes(profileId)
        return PasswordSafe.instance.getPassword(attributes)
    }

    override fun setApiKey(profileId: String, apiKey: String?) {
        val attributes = createAttributes(profileId)
        if (apiKey.isNullOrBlank()) {
            PasswordSafe.instance.setPassword(attributes, null)
        } else {
            PasswordSafe.instance.setPassword(attributes, apiKey.trim())
        }
    }

    override fun deleteApiKey(profileId: String) {
        setApiKey(profileId, null)
    }
}
