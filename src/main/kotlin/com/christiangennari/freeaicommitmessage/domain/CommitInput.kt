package com.christiangennari.freeaicommitmessage.domain

data class CommitInput(
    val stagedDiff: String,
    val statSummary: String,
    val fileList: List<String>,
    val additionalContext: String = ""
)
