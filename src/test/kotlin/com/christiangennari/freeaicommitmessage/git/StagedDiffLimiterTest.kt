package com.christiangennari.freeaicommitmessage.git

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StagedDiffLimiterTest {

    @Test
    fun `test diff limiter bounds large diff`() {
        val largeDiff = "A".repeat(20000)
        val input = CommitInput(
            stagedDiff = largeDiff,
            statSummary = "10 files changed",
            fileList = (1..50).map { "file$it.kt" }
        )

        val limited = StagedDiffLimiter.limit(input, 5000)

        assertTrue(limited.stagedDiff.length <= 5000)
        assertTrue(limited.stagedDiff.contains("diff truncated by plugin"))
    }
}
