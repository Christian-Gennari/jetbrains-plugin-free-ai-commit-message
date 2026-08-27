package com.christiangennari.freeaicommitmessage.action

import com.christiangennari.freeaicommitmessage.FreeAiCommitMessageBundle
import com.christiangennari.freeaicommitmessage.config.FreeAiCommitMessageSettingsService
import com.christiangennari.freeaicommitmessage.domain.Validation
import com.christiangennari.freeaicommitmessage.git.CommitMessageAccessor
import com.christiangennari.freeaicommitmessage.git.IntellijStagedChangesReader
import com.christiangennari.freeaicommitmessage.git.StagedChangesReader
import com.christiangennari.freeaicommitmessage.git.StagedChangesResult
import com.christiangennari.freeaicommitmessage.git.StagedDiffLimiter
import com.christiangennari.freeaicommitmessage.notification.FreeAiNotifications
import com.christiangennari.freeaicommitmessage.provider.AiProviderEngine
import com.christiangennari.freeaicommitmessage.provider.ProviderResult
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys

class GenerateCommitMessageAction(
    private val stagedReader: StagedChangesReader = IntellijStagedChangesReader(),
    private val providerEngine: AiProviderEngine = AiProviderEngine()
) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasCommitControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) != null
        e.presentation.isEnabled = project != null && !project.isDisposed && hasCommitControl
        e.presentation.text = FreeAiCommitMessageBundle.message("action.GenerateCommitMessage.text")
        e.presentation.description = FreeAiCommitMessageBundle.message("action.GenerateCommitMessage.description")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (project.isDisposed) return

        val settingsService = FreeAiCommitMessageSettingsService.instance
        val settings = settingsService.state
        val profile = settingsService.getActiveProfile()
        val options = settings.getOptions()

        var apiKey = settingsService.secretStore.getApiKey(profile.id)

        // If key is required and missing, prompt user interactively
        if (profile.kind.requiresApiKey && apiKey.isNullOrBlank()) {
            val enteredKey = Messages.showPasswordDialog(
                project,
                "Enter API key for ${profile.name}:",
                "API Key Required",
                Messages.getQuestionIcon()
            )
            if (enteredKey.isNullOrBlank()) {
                FreeAiNotifications.showWarning(project, "API key is missing for ${profile.name}. Please configure it in settings.")
                return
            }
            apiKey = enteredKey.trim()
            settingsService.secretStore.setApiKey(profile.id, apiKey)
        }

        val existingContext = CommitMessageAccessor.getCommitMessageText(e)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating AI Commit Message", true) {
            override fun run(indicator: ProgressIndicator) {
                if (indicator.isCanceled || project.isDisposed) return

                indicator.isIndeterminate = true
                indicator.text = "Reading staged Git changes..."

                val readResult = stagedReader.readStagedChanges(project, existingContext, indicator)

                if (indicator.isCanceled || project.isDisposed) return

                when (readResult) {
                    is StagedChangesResult.NoRepository -> {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                FreeAiNotifications.showWarning(project, FreeAiCommitMessageBundle.message("notification.no.repository"))
                            }
                        }
                        return
                    }
                    is StagedChangesResult.NoStagedChanges -> {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                FreeAiNotifications.showWarning(project, FreeAiCommitMessageBundle.message("notification.no.staged.changes"))
                            }
                        }
                        return
                    }
                    is StagedChangesResult.Error -> {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                FreeAiNotifications.showError(project, FreeAiCommitMessageBundle.message("notification.error", readResult.message))
                            }
                        }
                        return
                    }
                    is StagedChangesResult.Success -> {
                        val boundedInput = StagedDiffLimiter.limit(
                            readResult.input,
                            Validation.clampMaxDiffCharacters(options.maxDiffCharacters)
                        )

                        if (indicator.isCanceled || project.isDisposed) return

                        indicator.text = "Generating commit message with ${profile.name}..."
                        val result = providerEngine.generate(profile, apiKey, boundedInput, options, indicator)

                        if (indicator.isCanceled || project.isDisposed) return

                        ApplicationManager.getApplication().invokeLater {
                            if (project.isDisposed) return@invokeLater

                            when (result) {
                                is ProviderResult.Success -> {
                                    val currentText = CommitMessageAccessor.getCommitMessageText(e)
                                    // If user edited text while generation was running, prevent silent clobber
                                    if (currentText.isNotBlank() && currentText != existingContext) {
                                        val replace = Messages.showYesNoDialog(
                                            project,
                                            "Commit message was modified during generation. Overwrite with AI message?\n\nGenerated:\n${result.message.fullMessage}",
                                            "Commit Message Modified",
                                            "Overwrite",
                                            "Keep Current",
                                            Messages.getQuestionIcon()
                                        ) == Messages.YES

                                        if (replace) {
                                            CommitMessageAccessor.setCommitMessageText(e, result.message.fullMessage)
                                            FreeAiNotifications.showInfo(project, FreeAiCommitMessageBundle.message("notification.success"))
                                        } else {
                                            FreeAiNotifications.showInfo(project, FreeAiCommitMessageBundle.message("notification.stale.editor"))
                                        }
                                    } else {
                                        CommitMessageAccessor.setCommitMessageText(e, result.message.fullMessage)
                                        FreeAiNotifications.showInfo(project, FreeAiCommitMessageBundle.message("notification.success"))
                                    }
                                }
                                is ProviderResult.Error -> {
                                    FreeAiNotifications.showError(project, FreeAiCommitMessageBundle.message("notification.error", result.message))
                                }
                                is ProviderResult.Cancelled -> {
                                    FreeAiNotifications.showInfo(project, FreeAiCommitMessageBundle.message("notification.cancelled"))
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}
