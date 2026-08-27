package com.christiangennari.freeaicommitmessage.prompt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConventionalCommitSanitizerTest {

    @Test
    fun `test sanitize plain commit subject`() {
        val raw = "feat(auth): add login endpoint"
        val message = ConventionalCommitSanitizer.sanitize(raw)

        assertEquals("feat(auth): add login endpoint", message.subject)
        assertNull(message.body)
    }

    @Test
    fun `test sanitize markdown code fences`() {
        val raw = "```\nfix(parser): handle empty strings\n\nResolve edge-case when input length is 0.\n```"
        val message = ConventionalCommitSanitizer.sanitize(raw)

        assertEquals("fix(parser): handle empty strings", message.subject)
        assertEquals("Resolve edge-case when input length is 0.", message.body)
    }

    @Test
    fun `test sanitize quoted message`() {
        val raw = "\"refactor(core): optimize memory usage\""
        val message = ConventionalCommitSanitizer.sanitize(raw)

        assertEquals("refactor(core): optimize memory usage", message.subject)
        assertNull(message.body)
    }

    @Test
    fun `test sanitize backtick wrapped message`() {
        val raw = "`docs(readme): update install instructions`"
        val message = ConventionalCommitSanitizer.sanitize(raw)

        assertEquals("docs(readme): update install instructions", message.subject)
    }
}
