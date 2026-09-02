<div align="center">

# Free AI Commit Message — JetBrains Plugin

**Generate clean, conventional Git commit messages directly in JetBrains Rider, IntelliJ IDEA, PyCharm, WebStorm, and all IntelliJ Platform IDEs.**

Works out of the box with zero configuration and no API keys required, or connect directly with your own personal API keys to leverage generous free daily quotas from **Gemini (1,500 req/day)**, **Groq (14,400 req/day)**, **GitHub Models**, and **OpenRouter**, or run **100% offline with local Ollama**.

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains%20Marketplace-Install-blue?logo=jetbrains)](https://plugins.jetbrains.com/)
[![CI](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml/badge.svg)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE.txt)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-black?logo=github)](https://github.com/Christian-Gennari/jetbrains-plugin-free-ai-commit-message)

</div>

> **Using Visual Studio Code?** Check out the sibling extension: [Free AI Commit Message for VS Code](https://marketplace.visualstudio.com/items?itemName=christiangennari.free-ai-commit-message) ([GitHub Repository](https://github.com/Christian-Gennari/vscode-extension-free-ai-commit-message)).

---

## Quick Start (Zero Setup Required)

1. **Select or Stage your Git changes:**
   - Open the **Commit Tool Window** (`Alt+0` / `Cmd+0`).
   - Works automatically with both **Default Changelists** (unstaged modified files) and the **Git Staging Area** (`git diff --cached`).
2. **Generate your Commit Message:**
   - Click the **Sparkle icon** (`✨`) in the Commit message toolbar, or press **`Ctrl+Alt+G`** (`Cmd+Alt+G` on macOS).
   - Your conventional commit message is generated in the background and placed directly into the commit message box.

*(Optional)* **Want higher rate limits or custom models?**
- Open **Settings / Preferences** (`Ctrl+Alt+S` / `Cmd+,`) -> **Tools -> Free AI Commit Message**.
- Switch to Gemini, Groq, Ollama, etc. and enter your personal API key (stored securely in OS Keychain via JetBrains `PasswordSafe`).

---

## Provider Profiles & Recommendations

| Rank | Provider Profile | Default Model | Setup Required | Speed & Limits | Key Source |
| :---: | :--- | :--- | :---: | :--- | :--- |
| **#1** | **Free (No Setup Required)** *(Default)* | `free` | **None (Zero Setup)** | Instant quick-start, daily free pool | Built-in |
| **#2** | **Google Gemini** | `gemini-3.5-flash-lite` | Free API Key | **1,500 req/day** (30 RPM) | [Google AI Studio](https://aistudio.google.com/app/apikey) |
| **#3** | **Groq Cloud** | `openai/gpt-oss-120b` | Free API Key | **14,400 req/day** (30 RPM, ~300ms latency) | [Groq Console](https://console.groq.com/keys) |
| **#4** | **Local Ollama** | `qwen2.5-coder:3b` | Local Server | **Unlimited** (100% offline, zero data leaves machine) | [Ollama](https://ollama.com) |
| **#5** | **OpenRouter Free** | `openrouter/free` | Free API Key | Free community tier | [OpenRouter Keys](https://openrouter.ai/keys) |
| **#6** | **GitHub Models** | `gpt-4o-mini` | GitHub PAT | **150 req/day** (15 RPM) | [GitHub PAT Tokens](https://github.com/settings/tokens) |
| — | **DeepSeek** | `deepseek-chat` | BYOK | High reasoning, low cost | [DeepSeek Platform](https://platform.deepseek.com) |
| — | **OpenAI** | `gpt-4o-mini` | BYOK | Direct OpenAI API | [OpenAI Platform](https://platform.openai.com) |
| — | **Anthropic Claude** | `claude-3-5-haiku` | BYOK | Direct Anthropic API | [Anthropic Console](https://console.anthropic.com) |

> **Recommendation:** Keep **Free (No Setup Required)** for immediate commits right after installing. For heavy day-to-day use with large repositories, switch to your own free **Google Gemini** or **Groq Cloud** key for up to 14,400 requests/day.

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
| **Provider Profile** | `Free (No Setup Required)` | Active AI provider preset (`free`, `gemini`, `groq`, `ollama`, `openrouter`, `github-models`, `deepseek`, `openai`, `anthropic`). |
| **API Key** | *(secure)* | Stored in OS Keychain via JetBrains `PasswordSafe`. Zero plaintext tokens in config files. Leave blank for Free preset or local Ollama. |
| **Max Diff Characters** | `12000` | Safety character budget for staged diffs (1,000 – 100,000). |
| **Request Timeout (ms)** | `120000` | HTTP request timeout in milliseconds (5,000 – 600,000). |
| **Temperature** | `0.2` | Model sampling temperature (0.0 to 2.0). |
| **Commit Language** | `English` | Output language (e.g. English, Swedish, German, Spanish, French). |
| **Prefix with Gitmoji** | `false` | Prefix conventional commit types with Gitmoji icons (e.g. `:sparkles: feat: ...`, `:bug: fix: ...`). |

---

## Security & Privacy

- **Zero-Setup Quick Start:** The default `free` profile connects securely over HTTPS to the proxy with server-side prompt isolation, input diff truncation, and zero tracking.
- **Encrypted Secret Storage:** Personal API keys are stored exclusively in your operating system's native keychain (macOS Keychain, Windows Credential Manager, Linux Secret Service / KWallet) via JetBrains `PasswordSafe`. They are never written to XML configuration files or logs.
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
- **DataGrip**
- **Android Studio**

---

## License

MIT © [Christian Gennari](https://github.com/Christian-Gennari)
