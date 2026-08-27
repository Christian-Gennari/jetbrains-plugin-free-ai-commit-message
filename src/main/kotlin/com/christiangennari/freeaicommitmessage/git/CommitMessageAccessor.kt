package com.christiangennari.freeaicommitmessage.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.CommitMessage

object CommitMessageAccessor {

    fun getCommitMessageComponent(e: AnActionEvent): CommitMessage? {
        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (commitMessage is CommitMessage) {
            return commitMessage
        }
        return null
    }

    fun getCommitMessageText(e: AnActionEvent): String {
        val component = getCommitMessageComponent(e) ?: return ""
        return component.text.trim()
    }

    fun setCommitMessageText(e: AnActionEvent, message: String) {
        val component = getCommitMessageComponent(e) ?: return
        component.text = message
    }
}
