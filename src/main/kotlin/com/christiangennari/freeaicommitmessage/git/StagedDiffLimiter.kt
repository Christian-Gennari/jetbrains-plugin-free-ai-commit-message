package com.christiangennari.freeaicommitmessage.git

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.christiangennari.freeaicommitmessage.domain.Validation

object StagedDiffLimiter {

    private const val TRUNCATION_MARKER = "\n\n...[diff truncated by plugin]"

    fun limit(input: CommitInput, maxChars: Int): CommitInput {
        val totalBudget = Validation.clampMaxDiffCharacters(maxChars)

        // 1. Additional context budget (max 15% of total budget)
        val contextBudget = (totalBudget * 0.15).toInt()
        val boundedContext = input.additionalContext.take(contextBudget)

        // 2. Summary budget (max 20% of remaining)
        val summary = buildString {
            if (input.statSummary.isNotBlank()) {
                append("Summary:\n").append(input.statSummary.trim()).append("\n\n")
            }
            if (input.fileList.isNotEmpty()) {
                append("Files:\n").append(input.fileList.joinToString("\n")).append("\n\n")
            }
        }
        val summaryBudget = minOf(summary.length, (totalBudget * 0.20).toInt())
        val boundedSummary = summary.take(summaryBudget)

        // 3. Diff budget is the remainder
        val usedBudget = boundedContext.length + boundedSummary.length
        val remainingBudget = (totalBudget - usedBudget).coerceAtLeast(0)

        val boundedDiff = when {
            input.stagedDiff.length <= remainingBudget -> input.stagedDiff
            remainingBudget <= TRUNCATION_MARKER.length -> TRUNCATION_MARKER.take(remainingBudget)
            else -> input.stagedDiff.take(remainingBudget - TRUNCATION_MARKER.length) + TRUNCATION_MARKER
        }

        return input.copy(
            stagedDiff = boundedDiff,
            statSummary = boundedSummary,
            additionalContext = boundedContext
        )
    }
}
