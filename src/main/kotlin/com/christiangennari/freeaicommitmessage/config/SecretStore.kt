package com.christiangennari.freeaicommitmessage.config

interface SecretStore {
    fun getApiKey(profileId: String): String?
    fun setApiKey(profileId: String, apiKey: String?)
    fun deleteApiKey(profileId: String)
}
