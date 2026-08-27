package com.christiangennari.freeaicommitmessage.prompt

import com.christiangennari.freeaicommitmessage.domain.CommitMessage

object ConventionalCommitSanitizer {

    fun sanitize(raw: String): CommitMessage {
        var text = raw.trim()

        // Strip surrounding Markdown code fences if present
        if (text.startsWith("```")) {
            val lines = text.lines()
            val startIdx = if (lines.first().startsWith("```")) 1 else 0
            val endIdx = if (lines.last().trim() == "```") lines.size - 1 else lines.size
            text = lines.subList(startIdx, endIdx).joinToString("\n").trim()
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
        val subject = lines.firstOrNull { it.isNotBlank() } ?: "chore: update files"
        val subjectIndex = lines.indexOfFirst { it.isNotBlank() }
        val bodyLines = if (subjectIndex >= 0 && subjectIndex < lines.size - 1) {
            lines.subList(subjectIndex + 1, lines.size).dropWhile { it.isBlank() }
        } else {
            emptyList()
        }

        val body = if (bodyLines.isNotEmpty()) bodyLines.joinToString("\n").trim() else null

        return CommitMessage(
            subject = subject.trim(),
            body = if (body.isNullOrBlank()) null else body
        )
    }
}
