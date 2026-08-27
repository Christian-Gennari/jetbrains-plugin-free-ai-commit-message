# Free AI Commit Message — JetBrains Plugin

[![CI](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml/badge.svg)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.txt)

Generate clean, descriptive conventional Git commit messages directly in your JetBrains IDE Commit Tool Window (Rider, IntelliJ IDEA, PyCharm, WebStorm, etc.) using free tier AI models or Bring-Your-Own-Key.

---

## Features

- **100% Free AI Defaults:**
  - **Google Gemini:** `gemini-3.5-flash-lite` (Free Generative Language API)
  - **Groq LPU:** `openai/gpt-oss-120b` (Ultra-fast free inference)
  - **Local Ollama:** `qwen2.5-coder:3b` (100% private, offline, no API key needed)
  - **OpenRouter Free Tier:** `cohere/north-mini-code:free`
  - **GitHub Models:** `gpt-4o-mini` (Free tier with personal access token)
- **BYOK Providers:** DeepSeek, OpenAI, Anthropic Claude.
- **Hardware-Backed Secret Storage:** API keys are stored exclusively in JetBrains `PasswordSafe` (OS Keychain / Credential Manager / Secret Service). Zero plaintext tokens in config files.
- **Commit Toolbar Integration:** One-click generation via the sparkle icon in the Commit Tool Window message toolbar.
- **Context-Aware:** Existing draft text in the commit box is incorporated as additional guidance.
- **Strict Diff Budgeting:** Automatically calculates and bounds diff metadata to keep requests lean and fast.
- **Fully Cancellable:** Background generation with non-blocking EDT dispatch and timeout protection.

---

## Installation

### From JetBrains Marketplace
Search for **Free AI Commit Message** in `Settings / Preferences -> Plugins -> Marketplace` and click **Install**.

### Manual Installation
1. Download the latest `jetbrains-plugin-free-ai-commit-message-x.y.z.zip` from [Releases](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/releases).
2. In your IDE, go to `Settings / Preferences -> Plugins -> ⚙️ -> Install Plugin from Disk...`.
3. Select the downloaded `.zip` file and restart the IDE.

---

## Configuration

Go to `Settings / Preferences -> Tools -> Free AI Commit Message`:

| Setting | Default | Description |
|---|---|---|
| **Active Profile** | Google Gemini | Active AI provider profile |
| **API Key** | *(secure)* | Stored in OS Keychain via JetBrains `PasswordSafe` |
| **Max Diff Characters** | `12000` | Maximum character budget for staged diffs |
| **Request Timeout** | `120000` ms | Maximum HTTP request timeout in milliseconds |
| **Temperature** | `0.2` | Model sampling temperature (0.0 – 2.0) |
| **Prompt Language** | English | Output language for commit messages |
| **Use Gitmoji** | `false` | Prefix commit messages with standard Gitmoji |

---

## Privacy Notice

- **Local Ollama:** 100% private and offline. No diffs, code, or metadata leave your machine.
- **Cloud Providers:** When using Gemini, Groq, OpenRouter, GitHub Models, OpenAI, or Claude, your staged Git diff is sent directly to the respective API endpoint.

---

## License

[MIT License](LICENSE.txt) © 2026 Christian Gennari
