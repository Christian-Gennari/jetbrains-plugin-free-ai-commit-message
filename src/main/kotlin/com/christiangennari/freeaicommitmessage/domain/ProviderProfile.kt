package com.christiangennari.freeaicommitmessage.domain

import kotlinx.serialization.Serializable

@Serializable
data class ProviderProfile(
    val id: String = "",
    val name: String = "",
    val kind: ProviderKind = ProviderKind.GEMINI,
    val endpoint: String = "",
    val model: String = "",
    val isBuiltIn: Boolean = false,
    val temperature: Double? = null
)
