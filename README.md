<div align="center">

**English** · [简体中文](README.zh-CN.md)

# Shot You

### Pick your shots — let AI keep the best one.

*You took fifty near-identical photos. Shot You finds the duplicates, understands them,
and fuses the best of each into one flawless shot.*

[![Android CI](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml)
[![Release](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

</div>

---

## The idea

You snap a burst — same person, same place, slightly different pose, eyes half-closed in
three of them. Normally you scroll, pinch-zoom, agonise, and keep one mediocre frame.

**Shot You** does the thinking for you:

1. **Select a big batch** of photos (tap, or press-and-drag to sweep-select) and hit
   **Classify with AI**.
2. Classification runs **in the background** (a sliding window over a Vision-Language
   Model, processed concurrently) and lands in the **Queue** as a *session* — you can keep
   using your phone; progress shows in the status bar.
3. When it's **ready**, open the session to **curate**: each group shows its category and a
   "worth generating?" suggestion (so redundant shots are pre-unchecked to save cost). Tap
   to zoom, edit the prompt, check the ones you want, **Start**.
4. Each chosen group generates **three candidates** — *conservative · balanced · bold* —
   the references are actually fed to the image model (`images/edits`).
5. **Swipe** through the candidates, keep any, ask for changes to iterate, then optionally
   **move the originals to the recycle bin**.
6. A **usage dashboard** tracks every call (with your own per-million pricing), and a
   **pausable** queue keeps everything unobtrusive.

---

## Features

| | |
|---|---|
| **Full album access** | Modern Photo Picker + media permissions, incl. Android 14 *partial* access; press-and-drag sweep selection; already-processed photos are marked. |
| **Background classification** | Sliding-window VLM clustering (configurable batch size) run **concurrently with retry** — large selections never freeze the UI. Returns a category + "worth generating?" hint per group. |
| **Staged queue** | Sessions move through *Classifying → Ready for review → Generating*, visible in the Queue with live status-bar progress; **pause / resume**; auto-retry. |
| **Curate before you spend** | Review groups, zoom in, edit the prompt, uncheck redundant ones (pre-unchecked from the VLM's suggestion) before generating. |
| **3 candidates** | Each group yields *conservative / balanced / bold* versions with distinct prompts; references are fed via `images/edits`; output aspect ratio matches the source. Swipe to pick, ask-for-changes to iterate. |
| **Per-category styles** | Map each category (people / scenery / food / …) to a style preset; plus Realistic / Beautify / Cinematic / Fresh / Artistic + intensity. Prompts tuned for "beautiful but believable". |
| **Originals cleanup** | Move the rest to the system **recycle bin** (recoverable), or keep one. |
| **Bring-your-own AI** | Any **OpenAI-compatible** endpoint with a **custom API host** (official / proxy / local). Per-million token pricing for the cost dashboard. Keys stored **on-device**, no backend. |
| **Keep-alive** | Foreground-service generation + status-bar progress; optional **root** Doze-whitelist (low battery impact). |
| **Bilingual** | In-app switch between English / 简体中文 / system; prompts follow your language. |
| **Modern UI** | Jetpack Compose, **Material 3**, a curated teal palette, pinch-to-zoom, edge-to-edge, dark mode. |

---

## Architecture

Clean, modular MVVM — a single-module app organised by layer:

```
ui/         Compose screens + ViewModels (Library, Groups, Generate, Batch,
            Result, Queue, Templates, Usage, Settings) + navigation + theme
domain/     models, repository interfaces, AI contracts, PromptComposer, SessionStore
data/
  local/    Room (templates, jobs, usage)
  remote/   OpenAI-compatible client, DTOs, AiProviderFactory
  repository/  repository implementations
  settings/ DataStore-backed settings
work/       WorkManager generation worker
di/         Hilt modules
```

- **Hilt** for DI, **Room** + **DataStore** for persistence, **WorkManager** for the
  queue, **Retrofit/OkHttp** + **kotlinx.serialization** for the network layer, and
  **Coil** for images.
- AI is fully abstracted behind `VlmProvider` / `LlmProvider` / `ImageGenProvider` and an
  `AiProviderFactory`, so adding a provider is a single class.

> Fun fact: the entire app was built in parallel — the foundation and contracts first,
> then five features developed simultaneously in isolated git worktrees and merged, with
> GitHub Actions as the build gate. See the commit history for the full build log.

---

## Getting started

### Install
Grab the latest **`shot-you-debug.apk`** from the
[Releases](https://github.com/jiangmuran/shot-you/releases) page (or from the
[CI artifacts](https://github.com/jiangmuran/shot-you/actions)) and install it.

### Configure the endpoint
Open **Settings** and fill in:

- **API Host** — defaults to `https://api.openai.com/v1`; change it to any OpenAI-compatible
  endpoint (official, a proxy, or a local server).
- **API Key** — stored on-device only, never uploaded.
- **Models** — set the VLM (grouping), LLM (prompt optimization) and image models
  separately (defaults: `gpt-4o`, `gpt-4o-mini`, `gpt-image-2`).

Any service implementing the OpenAI `chat/completions` and `images/edits` APIs will work.

> The VLM model must support image input (vision). On a proxy/relay host, make sure the VLM
> model name you set is a vision-capable one, or classification can't actually see the photos.

### Use it
**Library** → select photos (tap or press-and-drag) → **Classify with AI** (runs in the
background) → **Queue**: open the session when it's *Ready* → **curate** (uncheck redundant
groups, zoom, tweak prompts) → **Start** → swipe the three candidates per group, keep / ask
for changes, then move the leftover originals to the recycle bin.

---

## Build from source

```bash
git clone https://github.com/jiangmuran/shot-you.git
cd shot-you
./gradlew assembleDebug # → app/build/outputs/apk/debug/app-debug.apk
```

Requires **JDK 17** and the Android SDK (**compileSdk 35**). CI builds a debug APK on
every push.

---

## Roadmap

- [x] Background classification sessions with a staged, pausable queue + status-bar progress
- [x] Per-category style rules; 3 candidates (conservative/balanced/bold); move-to-recycle-bin
- [ ] Side-by-side before/after compare on the result screen
- [ ] On-device pre-clustering (perceptual hash) to cut VLM cost on huge batches
- [ ] Inpainting / targeted edits ("just fix the eyes")

Contributions welcome.

## License

[MIT](LICENSE) © 2026 jiangmuran
