package com.christiangennari.freeaicommitmessage.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BuiltInProfilesTest {

    @Test
    fun `test built-in presets presence`() {
        val presets = BuiltInProfiles.ALL_PRESETS
        assertEquals(9, presets.size)

        assertNotNull(BuiltInProfiles.findById("free"))
        assertNotNull(BuiltInProfiles.findById("gemini"))
        assertNotNull(BuiltInProfiles.findById("groq"))
        assertNotNull(BuiltInProfiles.findById("ollama"))
        assertNotNull(BuiltInProfiles.findById("openrouter"))
        assertNotNull(BuiltInProfiles.findById("github-models"))
        assertNotNull(BuiltInProfiles.findById("deepseek"))
        assertNotNull(BuiltInProfiles.findById("openai"))
        assertNotNull(BuiltInProfiles.findById("anthropic"))
    }
}
