<div align="center">

# Free AI Commit Message — JetBrains Plugin

**Generate clean, conventional Git commit messages directly in JetBrains Rider, IntelliJ IDEA, PyCharm, WebStorm, and all IntelliJ Platform IDEs.**

Works out-of-the-box with **Gemini (1,500 req/day free)**, **Groq (14,400 req/day free)**, **GitHub Models**, **OpenRouter**, and **100% offline local Ollama**, as well as DeepSeek, OpenAI, and Anthropic Claude.

[![Release](https://img.shields.io/github/v/release/Christian-Gennari/jetbrains-plugin-free-ai-commit-message?logo=github&label=Release)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/releases)
[![CI](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml/badge.svg)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE.txt)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-black?logo=github)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message)

</div>

> **Using Visual Studio Code?** Check out the sibling extension: [Free AI Commit Message for VS Code](https://marketplace.visualstudio.com/items?itemName=christiangennari.free-ai-commit-message) ([GitHub Repository](https://github.com/Christian-Gennari/vscode-extension-free-ai-commit-message)).

---

## Quick Start

1. **Select or Stage your Git changes:**
   - Open the **Commit Tool Window** (`Alt+0` / `Cmd+0`).
   - Works automatically with both **Default Changelists** (unstaged modified files) and the **Git Staging Area** (`git diff --cached`).
2. **Set your API Key:**
   - Open **Settings / Preferences** (`Ctrl+Alt+S` / `Cmd+,`)
   - Navigate to **Tools -> Free AI Commit Message**
   - Select your active provider and enter your API key *(saved securely in OS Keychain via JetBrains `PasswordSafe`)*.
3. **Generate your Commit Message:**
   - Click the **Sparkle icon** (`✨`) in the Commit message toolbar, or press **`Ctrl+Alt+G`** (`Cmd+Alt+G` on macOS).
   - The conventional commit message is generated in the background and placed directly in the commit message box.

---

## Provider Recommendations

Providers ranked by speed, reliability, and free daily quota:

| Rank | Provider | Default Model | Speed & Characteristics | Free Daily Quota | Key Source |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **#1** | **Google Gemini** *(Default)* | `gemini-3.5-flash-lite` | Highest reliability & quality | **1,500 req/day** (30 RPM) | [Google AI Studio](https://aistudio.google.com/app/apikey) |
| **#2** | **Groq Cloud** | `openai/gpt-oss-120b` | Fastest (~300ms LPU latency) | **14,400 req/day** (30 RPM) | [Groq Console](https://console.groq.com/keys) |
| **#3** | **Ollama** | `qwen2.5-coder:3b` | 100% offline, zero data leaves machine | **Unlimited** (Local) | [Ollama](https://ollama.com) |
| **#4** | **OpenRouter Free** | `cohere/north-mini-code:free` | Dedicated free code model | Free community tier | [OpenRouter Keys](https://openrouter.ai/keys) |
| **#5** | **GitHub Models** | `gpt-4o-mini` | Stable OpenAI endpoint via GitHub PAT | **150 req/day** (15 RPM) | [GitHub PAT Tokens](https://github.com/settings/tokens) |
| — | **DeepSeek** | `deepseek-chat` | High reasoning, low cost | BYOK / Pay-as-you-go | [DeepSeek Platform](https://platform.deepseek.com) |
| — | **OpenAI** | `gpt-4o-mini` | Direct OpenAI API | BYOK / Pay-as-you-go | [OpenAI Platform](https://platform.openai.com) |
| — | **Anthropic Claude** | `claude-3-5-haiku` | Direct Anthropic API | BYOK / Pay-as-you-go | [Anthropic Console](https://console.anthropic.com) |

> **Recommendation:** Start with **Google Gemini** for the best balance of speed and reliability, or switch to **Groq Cloud** for near-instant LPU completions. For offline development, install [Ollama](https://ollama.com) and run `ollama run qwen2.5-coder:3b` with no API key required.

---

## Installation

### From JetBrains Marketplace
1. Open your IDE Settings (`Ctrl+Alt+S` / `Cmd+,`) and select **Plugins**.
2. Search for **Free AI Commit Message** in the **Marketplace** tab.
3. Click **Install** and restart the IDE if prompted.

### Manual Installation (.zip)
1. Download the latest `jetbrains-plugin-free-ai-commit-message-x.y.z.zip` from [Releases](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/releases).
2. In your IDE, go to `Settings -> Plugins -> ⚙️ (Gear icon) -> Install Plugin from Disk...`.
3. Select the downloaded `.zip` file and click **OK**.

---

## Configuration

Open **Settings / Preferences** (`Ctrl+Alt+S` / `Cmd+,`) -> **Tools** -> **Free AI Commit Message**:

| Setting | Default | Description |
| :--- | :--- | :--- |
| **Provider Profile** | `Google Gemini` | Active AI provider preset (`gemini`, `groq`, `ollama`, `openrouter`, `github-models`, `deepseek`, `openai`, `anthropic`). |
| **API Key** | *(secure)* | Stored in OS Keychain via JetBrains `PasswordSafe`. Zero plaintext tokens in config files. |
| **Max Diff Characters** | `12000` | Safety character budget for staged diffs (1,000 – 100,000). |
| **Request Timeout (ms)** | `120000` | HTTP request timeout in milliseconds (5,000 – 600,000). |
| **Temperature** | `0.2` | Model sampling temperature (0.0 to 2.0). |
| **Commit Language** | `English` | Output language (e.g. English, Swedish, German, Spanish, French). |
| **Prefix with Gitmoji** | `false` | Prefix conventional commit types with Gitmoji icons (e.g. `:sparkles: feat: ...`, `:bug: fix: ...`). |

---

## Security & Privacy

- **Encrypted Secret Storage:** API keys are stored exclusively in your operating system's native keychain (macOS Keychain, Windows Credential Manager, Linux Secret Service / KWallet) via JetBrains `PasswordSafe`. They are never written to XML configuration files or logs.
- **Direct Client-to-API:** Diff data travels directly from your IDE to the chosen provider API over TLS. No intermediate telemetry, tracking, or middleman proxy servers are used.
- **Local Isolation with Ollama:** When complete privacy is required, select the **Ollama** profile (`http://localhost:11434/v1`). Zero code or metadata leaves your local workstation.
- **Prompt Isolation:** Staged diffs, file lists, and user notes are strictly delimited and marked as untrusted input to defend against prompt injection.

---

## Supported IDEs

Compatible with **JetBrains IDE version 2024.1+ (Build 241+)**:
- **JetBrains Rider** (.NET / C# / F# / Unity / Unreal)
- **IntelliJ IDEA** (Ultimate & Community)
- **PyCharm** (Professional & Community)
- **WebStorm**
- **GoLand**
- **CLion**
- **PhpStorm**
- **RubyMine**
- **Android Studio** (2024.1+ based)

---

## License

[MIT License](LICENSE.txt) © 2026 Christian Gennari
