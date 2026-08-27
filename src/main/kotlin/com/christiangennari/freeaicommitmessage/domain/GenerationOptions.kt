package com.christiangennari.freeaicommitmessage.domain

import kotlinx.serialization.Serializable

@Serializable
data class GenerationOptions(
    val maxDiffCharacters: Int = 12000,
    val requestTimeoutMs: Long = 120000L,
    val defaultTemperature: Double = 0.2,
    val promptLanguage: String = "English",
    val useGitmoji: Boolean = false
)
