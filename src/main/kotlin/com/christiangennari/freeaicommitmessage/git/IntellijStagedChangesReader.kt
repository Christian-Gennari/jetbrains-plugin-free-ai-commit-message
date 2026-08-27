package com.christiangennari.freeaicommitmessage.git

import com.christiangennari.freeaicommitmessage.domain.CommitInput
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class IntellijStagedChangesReader : StagedChangesReader {

    override fun readStagedChanges(
        project: Project,
        additionalContext: String,
        indicator: ProgressIndicator?
    ): StagedChangesResult {
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val repositories = repositoryManager.repositories
        if (repositories.isEmpty()) {
            return StagedChangesResult.NoRepository
        }

        val allDiffs = mutableListOf<String>()
        val allStats = mutableListOf<String>()
        val allFiles = mutableListOf<String>()

        for (repo in repositories) {
            indicator?.checkCanceled()

            // 1. Staged diff
            val diffHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters("--cached", "--no-color", "--no-ext-diff")
            }
            val diffResult = Git.getInstance().runCommand(diffHandler)
            if (diffResult.success() && diffResult.outputAsJoinedString.isNotBlank()) {
                val prefix = if (repositories.size > 1) "# Repository: ${repo.root.name}\n" else ""
                allDiffs.add(prefix + diffResult.outputAsJoinedString)
            }

            indicator?.checkCanceled()

            // 2. Staged stats
            val statHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters("--cached", "--stat", "--no-color")
            }
            val statResult = Git.getInstance().runCommand(statHandler)
            if (statResult.success() && statResult.outputAsJoinedString.isNotBlank()) {
                val prefix = if (repositories.size > 1) "[${repo.root.name}] " else ""
                allStats.add(prefix + statResult.outputAsJoinedString)
            }

            indicator?.checkCanceled()

            // 3. Staged files
            val nameStatusHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters("--cached", "--name-status")
            }
            val nameStatusResult = Git.getInstance().runCommand(nameStatusHandler)
            if (nameStatusResult.success()) {
                val files = nameStatusResult.output.filter { it.isNotBlank() }
                if (repositories.size > 1) {
                    allFiles.addAll(files.map { "${repo.root.name}/$it" })
                } else {
                    allFiles.addAll(files)
                }
            }
        }

        if (allDiffs.isEmpty() && allFiles.isEmpty()) {
            return StagedChangesResult.NoStagedChanges
        }

        return StagedChangesResult.Success(
            CommitInput(
                stagedDiff = allDiffs.joinToString("\n\n"),
                statSummary = allStats.joinToString("\n"),
                fileList = allFiles.distinct(),
                additionalContext = additionalContext.trim()
            )
        )
    }
}
