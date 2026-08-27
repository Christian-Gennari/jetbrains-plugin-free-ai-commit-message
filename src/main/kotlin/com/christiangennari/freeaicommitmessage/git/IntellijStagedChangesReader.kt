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

        // 1. Try reading staged changes first (--cached)
        var (allDiffs, allStats, allFiles) = collectDiffs(project, repositories, staged = true, indicator = indicator)

        // 2. If no staged changes found, fallback to working tree (unstaged) changes
        // This supports JetBrains default changelist mode where users do not use explicit staging
        if (allDiffs.isEmpty() && allFiles.isEmpty()) {
            val unstaged = collectDiffs(project, repositories, staged = false, indicator = indicator)
            allDiffs = unstaged.first
            allStats = unstaged.second
            allFiles = unstaged.third
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

    private fun collectDiffs(
        project: Project,
        repositories: List<GitRepository>,
        staged: Boolean,
        indicator: ProgressIndicator?
    ): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        val allDiffs = mutableListOf<String>()
        val allStats = mutableListOf<String>()
        val allFiles = mutableListOf<String>()

        val baseDiffArgs = if (staged) listOf("--cached", "--no-color", "--no-ext-diff") else listOf("--no-color", "--no-ext-diff")
        val statArgs = if (staged) listOf("--cached", "--stat", "--no-color") else listOf("--stat", "--no-color")
        val nameStatusArgs = if (staged) listOf("--cached", "--name-status") else listOf("--name-status")

        for (repo in repositories) {
            indicator?.checkCanceled()

            // 1. Diff
            val diffHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters(baseDiffArgs)
            }
            val diffResult = Git.getInstance().runCommand(diffHandler)
            if (diffResult.success() && diffResult.outputAsJoinedString.isNotBlank()) {
                val prefix = if (repositories.size > 1) "# Repository: ${repo.root.name}\n" else ""
                allDiffs.add(prefix + diffResult.outputAsJoinedString)
            }

            indicator?.checkCanceled()

            // 2. Stats
            val statHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters(statArgs)
            }
            val statResult = Git.getInstance().runCommand(statHandler)
            if (statResult.success() && statResult.outputAsJoinedString.isNotBlank()) {
                val prefix = if (repositories.size > 1) "[${repo.root.name}] " else ""
                allStats.add(prefix + statResult.outputAsJoinedString)
            }

            indicator?.checkCanceled()

            // 3. Files
            val nameStatusHandler = GitLineHandler(project, repo.root, GitCommand.DIFF).apply {
                addParameters(nameStatusArgs)
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

            // 4. If unstaged, also include untracked files
            if (!staged) {
                indicator?.checkCanceled()
                val statusHandler = GitLineHandler(project, repo.root, GitCommand.STATUS).apply {
                    addParameters("--porcelain", "--untracked-files=all")
                }
                val statusResult = Git.getInstance().runCommand(statusHandler)
                if (statusResult.success()) {
                    val untracked = statusResult.output
                        .filter { it.startsWith("?? ") }
                        .map { it.removePrefix("?? ").trim() }
                        .filter { it.isNotBlank() }
                    if (repositories.size > 1) {
                        allFiles.addAll(untracked.map { "${repo.root.name}/$it" })
                    } else {
                        allFiles.addAll(untracked)
                    }
                    if (untracked.isNotEmpty() && allDiffs.isEmpty()) {
                        allStats.add("Untracked new files:\n" + untracked.joinToString("\n") { "  + $it" })
                    }
                }
            }
        }

        return Triple(allDiffs, allStats, allFiles)
    }
}
