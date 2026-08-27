package com.christiangennari.freeaicommitmessage.prompt

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.GenerationOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommitPromptBuilderTest {

    @Test
    fun `test system prompt with standard options`() {
        val options = GenerationOptions()
        val systemPrompt = CommitPromptBuilder.buildSystemPrompt(options)

        assertTrue(systemPrompt.contains("Conventional Commits format"))
        assertTrue(systemPrompt.contains("imperative present tense"))
    }

    @Test
    fun `test system prompt with Gitmoji and custom language`() {
        val options = GenerationOptions(promptLanguage = "Swedish", useGitmoji = true)
        val systemPrompt = CommitPromptBuilder.buildSystemPrompt(options)

        assertTrue(systemPrompt.contains("Gitmoji"))
        assertTrue(systemPrompt.contains("Swedish"))
    }

    @Test
    fun `test user prompt with additional context and diff`() {
        val input = CommitInput(
            stagedDiff = "diff --git a/file.kt b/file.kt\n+val x = 1",
            statSummary = "1 file changed, 1 insertion(+)",
            fileList = listOf("M\tfile.kt"),
            additionalContext = "WIP: add constant"
        )
        val userPrompt = CommitPromptBuilder.buildUserPrompt(input)

        assertTrue(userPrompt.contains("<additional-context>"))
        assertTrue(userPrompt.contains("WIP: add constant"))
        assertTrue(userPrompt.contains("<staged-diff>"))
        assertTrue(userPrompt.contains("val x = 1"))
    }
}
