package com.christiangennari.freeaicommitmessage.prompt

import com.christiangennari.freeaicommitmessage.domain.CommitMessage

class InvalidCommitMessageException : IllegalArgumentException(MESSAGE) {
    companion object {
        const val MESSAGE = "Provider returned invalid commit message."
    }
}

object ConventionalCommitSanitizer {
    private val conventionalCommitSubject = Regex("^[a-z][a-z0-9-]*(?:\\([^\\r\\n)]+\\))?!?: [^\\r\\n]+$")
    private val reasoningMarker = Regex("</?(?:think|analysis|reasoning)\\b", RegexOption.IGNORE_CASE)
    private val planningProseMarker = Regex(
        "(?im)^\\s*(?:analysis|reasoning|thinking process|here(?:'s| is)\\s+(?:the\\s+)?commit message|the commit message is)\\s*[:\\-]"
    )

    fun sanitize(raw: String): CommitMessage {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (reasoningMarker.containsMatchIn(text) || planningProseMarker.containsMatchIn(text)) {
            throw InvalidCommitMessageException()
        }

        // Strip surrounding Markdown code fences if present
        if (text.startsWith("```") && text.lines().lastOrNull()?.trim() == "```") {
            val lines = text.lines()
            text = lines.subList(1, lines.size - 1).joinToString("\n").trim()
        }

        // Strip surrounding quotes
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'")) ||
            (text.startsWith("`") && text.endsWith("`"))) {
            if (text.length >= 2) {
                text = text.substring(1, text.length - 1).trim()
            }
        }

        val lines = text.lines().map { it.trimEnd() }
        val subjectIndex = lines.indexOfFirst { it.isNotBlank() }
        if (subjectIndex < 0) throw InvalidCommitMessageException()

        val subject = lines[subjectIndex].trim()
        if (!conventionalCommitSubject.matches(subject)) {
            throw InvalidCommitMessageException()
        }
        val bodyLines = if (subjectIndex < lines.size - 1) {
            lines.subList(subjectIndex + 1, lines.size).dropWhile { it.isBlank() }
        } else {
            emptyList()
        }

        val body = if (bodyLines.isNotEmpty()) bodyLines.joinToString("\n").trim() else null

        return CommitMessage(
            subject = subject,
            body = if (body.isNullOrBlank()) null else body
        )
    }
}
