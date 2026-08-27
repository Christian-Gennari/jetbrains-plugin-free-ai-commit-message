package com.christiangennari.freeaicommitmessage.domain

data class CommitMessage(
    val subject: String,
    val body: String? = null
) {
    val fullMessage: String
        get() = if (body.isNullOrBlank()) subject else "$subject\n\n$body"
}
