package com.christiangennari.freeaicommitmessage.git

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExactDiffBudgetTest {

    @Test
    fun `test limited prompt payload strictly respects character budget`() {
        val hugeDiff = "A".repeat(50000)
        val hugeStat = "B".repeat(10000)
        val hugeFiles = (1..500).map { "file$it.kt" }

        val input = CommitInput(
            stagedDiff = hugeDiff,
            statSummary = hugeStat,
            fileList = hugeFiles,
            additionalContext = "C".repeat(5000)
        )

        val budget = 4000
        val limited = StagedDiffLimiter.limit(input, budget)

        val totalChars = limited.statSummary.length + limited.stagedDiff.length
        assertTrue(totalChars <= budget, "Expected totalChars ($totalChars) <= budget ($budget)")
    }
}
