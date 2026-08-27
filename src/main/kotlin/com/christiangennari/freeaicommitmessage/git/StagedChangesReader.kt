package com.christiangennari.freeaicommitmessage.git

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

sealed class StagedChangesResult {
    data class Success(val input: CommitInput) : StagedChangesResult()
    object NoRepository : StagedChangesResult()
    object NoStagedChanges : StagedChangesResult()
    data class Error(val message: String) : StagedChangesResult()
}

interface StagedChangesReader {
    fun readStagedChanges(
        project: Project,
        additionalContext: String = "",
        indicator: ProgressIndicator? = null
    ): StagedChangesResult
}
