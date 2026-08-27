package com.christiangennari.freeaicommitmessage.config

import com.christiangennari.freeaicommitmessage.domain.ProviderKind
import com.christiangennari.freeaicommitmessage.domain.ProviderProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SecretStorageSecurityTest {

    class FakeSecretStore : SecretStore {
        private val secrets = mutableMapOf<String, String>()

        override fun getApiKey(profileId: String): String? = secrets[profileId]

        override fun setApiKey(profileId: String, apiKey: String?) {
            if (apiKey.isNullOrBlank()) {
                secrets.remove(profileId)
            } else {
                secrets[profileId] = apiKey
            }
        }

        override fun deleteApiKey(profileId: String) {
            secrets.remove(profileId)
        }
    }

    @Test
    fun `test secret store CRUD and blank deletion`() {
        val store = FakeSecretStore()

        assertNull(store.getApiKey("gemini"))
        store.setApiKey("gemini", "secret-key-123")
        assertEquals("secret-key-123", store.getApiKey("gemini"))

        store.setApiKey("gemini", "")
        assertNull(store.getApiKey("gemini"))
    }

    @Test
    fun `test ProviderProfile data class has no credential fields`() {
        val profile = ProviderProfile(
            id = "custom-openai",
            name = "Custom OpenAI",
            kind = ProviderKind.OPENAI_COMPATIBLE,
            endpoint = "https://custom.ai/v1",
            model = "gpt-4o"
        )

        val str = profile.toString()
        assertFalse(str.contains("key", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
        assertFalse(str.contains("password", ignoreCase = true))
    }
}
