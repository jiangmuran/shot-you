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

1. **Select a big batch** of photos from your gallery.
2. A **Vision-Language Model** clusters the near-duplicates (burst / same subject / same
   scene) and explains *why* each group belongs together.
3. It nominates the **strongest reference frames** from each cluster.
4. You add a **template prompt** — pick one from your library, or write a line and let an
   LLM **refine** it. Hair, expression, pose, position… all adjustable.
5. An **image model fuses** the references + prompt into one polished photo.
6. Not perfect? **Regenerate.** Everything runs through a **background queue** so the app
   stays buttery, and a **usage dashboard** tracks every call.

---

## Features

| | |
|---|---|
| **Full album access** | Modern Photo Picker + media permissions, incl. Android 14 *partial* (user-selected) access, with graceful fallbacks down to Android 8. |
| **Smart grouping** | VLM clusters visually-similar shots and returns a title + reason + reference picks per group. |
| **Reference selection** | The best frames of each group are pre-selected and fully editable. |
| **Prompt fusion** | Saved template library (with built-ins) + one-tap **LLM prompt optimization**; quick-insert chips for hair / expression / pose / position. |
| **Generation & regenerate** | One fused image from references + prompt; regenerate until you're happy; auto-saved to your gallery; share sheet. |
| **Background queue** | WorkManager-driven job queue with configurable **concurrency**, **request pacing**, retries and Wi-Fi-only mode — fully unobtrusive. |
| **Usage dashboard** | Call counts by provider & operation, token totals, image count and estimated cost. |
| **Style & intensity** | Realistic / Beautify / Cinematic / Fresh / Artistic presets + an intensity slider; configurable defaults. Prompts tuned for "beautiful but believable" — not the plastic AI look. |
| **Bring-your-own AI** | Any **OpenAI-compatible** endpoint with a **custom API host** (official / proxy / local). Per-million token pricing for the cost dashboard. Keys stored **on-device**, no backend. |
| **Bilingual** | In-app switch between English / 简体中文 / system. |
| **Modern UI** | Jetpack Compose, **Material 3**, dynamic color, edge-to-edge, dark mode. |

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

### Use it
**Library** → multi-select photos (drag to select) → **Group with AI** → review the groups
→ open one → pick a template / write & optimize a prompt, choose a style & intensity →
**Generate** (2-3 candidates) → swipe to pick the best, ask for changes, then tidy up the
originals.

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

- [ ] Fully automatic pipeline: classify everything, per-category style rules, one tap to start
- [ ] Foreground-service queue with live status-bar progress, pause / resume
- [ ] Side-by-side before/after compare on the result screen
- [ ] On-device pre-clustering (perceptual hash) to cut VLM cost on huge batches
- [ ] Inpainting / targeted edits ("just fix the eyes")

Contributions welcome.

## License

[MIT](LICENSE) © 2026 jiangmuran
