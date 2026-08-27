package com.christiangennari.freeaicommitmessage.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object FreeAiNotifications {
    private const val GROUP_ID = "Free AI Commit Message"

    fun showInfo(project: Project?, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun showWarning(project: Project?, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }

    fun showError(project: Project?, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }
}
