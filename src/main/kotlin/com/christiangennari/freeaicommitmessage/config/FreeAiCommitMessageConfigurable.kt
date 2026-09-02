package com.christiangennari.freeaicommitmessage.config

import com.christiangennari.freeaicommitmessage.domain.Validation
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class FreeAiCommitMessageConfigurable : Configurable {

    private val settingsService = FreeAiCommitMessageSettingsService.instance
    private val settings = settingsService.state

    private val profileCombo = ComboBox<String>()
    private val apiKeyField = JBPasswordField()
    private val maxDiffCharsField = JBTextField()
    private val timeoutMsField = JBTextField()
    private val temperatureField = JBTextField()
    private val languageField = JBTextField()
    private val gitmojiCheckbox = JBCheckBox("Prefix commit messages with Gitmoji (:sparkles:, :bug:, etc.)")
    private val autoRetryInvalidOutputCheckbox = JBCheckBox("Automatically retry invalid commit output")

    private val profilesList = settingsService.getAllProfiles()
    private val originalKeys = mutableMapOf<String, String>()

    override fun getDisplayName(): String = "Free AI Commit Message"

    override fun createComponent(): JComponent {
        profileCombo.removeAllItems()
        profilesList.forEach { profile ->
            profileCombo.addItem(profile.name)
            originalKeys[profile.id] = settingsService.secretStore.getApiKey(profile.id) ?: ""
        }

        val activeProfile = settingsService.getActiveProfile()
        val activeIndex = profilesList.indexOfFirst { it.id == activeProfile.id }.coerceAtLeast(0)
        profileCombo.selectedIndex = activeIndex

        profileCombo.addActionListener {
            val selectedIdx = profileCombo.selectedIndex
            val profile = profilesList.getOrNull(selectedIdx) ?: return@addActionListener
            apiKeyField.text = originalKeys[profile.id] ?: ""
            apiKeyField.isEnabled = profile.kind.requiresApiKey
        }

        apiKeyField.text = originalKeys[activeProfile.id] ?: ""
        apiKeyField.isEnabled = activeProfile.kind.requiresApiKey
        maxDiffCharsField.text = settings.maxDiffCharacters.toString()
        timeoutMsField.text = settings.requestTimeoutMs.toString()
        temperatureField.text = settings.defaultTemperature.toString()
        languageField.text = settings.promptLanguage
        gitmojiCheckbox.isSelected = settings.useGitmoji
        autoRetryInvalidOutputCheckbox.isSelected = settings.autoRetryInvalidOutput

        return panel {
            group("Active AI Provider") {
                row("Provider Profile:") {
                    cell(profileCombo)
                        .comment("Select the active AI model or preset")
                }
                row("API Key:") {
                    cell(apiKeyField)
                        .comment("Stored securely in OS Keychain via PasswordSafe. Leave blank for local Ollama.")
                }
            }

            group("Generation Settings") {
                row("Max Diff Characters:") {
                    cell(maxDiffCharsField)
                        .comment("Character budget for staged diffs (1000 - 100000)")
                }
                row("Request Timeout (ms):") {
                    cell(timeoutMsField)
                        .comment("Network timeout in milliseconds (5000 - 600000)")
                }
                row("Temperature:") {
                    cell(temperatureField)
                        .comment("Sampling temperature (0.0 - 2.0)")
                }
                row("Commit Language:") {
                    cell(languageField)
                        .comment("e.g. English, Swedish, German, Spanish")
                }
                row {
                    cell(gitmojiCheckbox)
                }
                row {
                    cell(autoRetryInvalidOutputCheckbox)
                        .comment("Retry invalid or badly formatted provider output automatically")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val selectedIdx = profileCombo.selectedIndex
        val selectedProfile = profilesList.getOrNull(selectedIdx) ?: return false

        val keyModified = String(apiKeyField.password) != (originalKeys[selectedProfile.id] ?: "")
        val profileModified = selectedProfile.id != settings.activeProfileId
        val maxDiffModified = maxDiffCharsField.text.toIntOrNull() != settings.maxDiffCharacters
        val timeoutModified = timeoutMsField.text.toLongOrNull() != settings.requestTimeoutMs
        val tempModified = temperatureField.text.toDoubleOrNull() != settings.defaultTemperature
        val langModified = languageField.text.trim() != settings.promptLanguage
        val gitmojiModified = gitmojiCheckbox.isSelected != settings.useGitmoji
        val autoRetryModified = autoRetryInvalidOutputCheckbox.isSelected != settings.autoRetryInvalidOutput

        return keyModified || profileModified || maxDiffModified || timeoutModified || tempModified || langModified || gitmojiModified || autoRetryModified
    }

    override fun apply() {
        val selectedIdx = profileCombo.selectedIndex
        val selectedProfile = profilesList.getOrNull(selectedIdx) ?: return

        settings.activeProfileId = selectedProfile.id
        settings.maxDiffCharacters = Validation.clampMaxDiffCharacters(maxDiffCharsField.text.toIntOrNull() ?: 12000)
        settings.requestTimeoutMs = Validation.clampTimeoutMs(timeoutMsField.text.toLongOrNull() ?: 120000L)
        settings.defaultTemperature = Validation.clampTemperature(temperatureField.text.toDoubleOrNull() ?: 0.2)
        settings.promptLanguage = languageField.text.trim().ifBlank { "English" }
        settings.useGitmoji = gitmojiCheckbox.isSelected
        settings.autoRetryInvalidOutput = autoRetryInvalidOutputCheckbox.isSelected

        val key = String(apiKeyField.password).trim()
        originalKeys[selectedProfile.id] = key
        settingsService.secretStore.setApiKey(selectedProfile.id, if (key.isBlank()) null else key)
    }

    override fun reset() {
        val activeProfile = settingsService.getActiveProfile()
        val activeIndex = profilesList.indexOfFirst { it.id == activeProfile.id }.coerceAtLeast(0)
        profileCombo.selectedIndex = activeIndex
        apiKeyField.text = originalKeys[activeProfile.id] ?: ""
        apiKeyField.isEnabled = activeProfile.kind.requiresApiKey
        maxDiffCharsField.text = settings.maxDiffCharacters.toString()
        timeoutMsField.text = settings.requestTimeoutMs.toString()
        temperatureField.text = settings.defaultTemperature.toString()
        languageField.text = settings.promptLanguage
        gitmojiCheckbox.isSelected = settings.useGitmoji
        autoRetryInvalidOutputCheckbox.isSelected = settings.autoRetryInvalidOutput
    }
}
