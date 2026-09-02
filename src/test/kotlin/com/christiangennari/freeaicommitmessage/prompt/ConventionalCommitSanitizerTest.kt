package com.christiangennari.freeaicommitmessage.prompt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `accepts Gitmoji-prefixed conventional commit messages`() {
        val unicode = ConventionalCommitSanitizer.sanitize("✨ feat: add output validation")
        assertEquals("✨ feat: add output validation", unicode.fullMessage)

        val alias = ConventionalCommitSanitizer.sanitize(":sparkles: feat: add output validation")
        assertEquals(":sparkles: feat: add output validation", alias.fullMessage)
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

    @Test
    fun `rejects reasoning output instead of inventing a fallback subject`() {
        assertThrows(InvalidCommitMessageException::class.java) {
            ConventionalCommitSanitizer.sanitize("<think>internal reasoning</think>\n\nfix: reject leaked reasoning")
        }
    }

    @Test
    fun `rejects explanatory preambles`() {
        assertThrows(InvalidCommitMessageException::class.java) {
            ConventionalCommitSanitizer.sanitize("Here is the commit message:\n\nfix: update proxy")
        }
    }

    @Test
    fun `rejects planning prose after a valid subject`() {
        assertThrows(InvalidCommitMessageException::class.java) {
            ConventionalCommitSanitizer.sanitize("feat: add endpoint\n\nAnalysis: the change introduces a route")
        }
    }

    @Test
    fun `rejects an invalid subject instead of inventing a fallback`() {
        assertThrows(InvalidCommitMessageException::class.java) {
            ConventionalCommitSanitizer.sanitize("not a conventional commit")
        }
    }
}
