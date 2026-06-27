<div align="center">

**English** · [简体中文](README.zh-CN.md)

# Shot You

### Shoot freely. Let AI keep the one shot worth keeping.

You take fifty almost-identical photos and keep one mediocre frame.
**Shot You** understands the whole burst, groups what belongs together, and fuses the
best of each group into a single, believable, beautiful photograph — entirely on a
bring-your-own-key, on-device pipeline.

[![Android CI](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/build.yml)
[![Release](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml/badge.svg)](https://github.com/jiangmuran/shot-you/actions/workflows/release.yml)
[![Latest release](https://img.shields.io/github/v/release/jiangmuran/shot-you?sort=semver)](https://github.com/jiangmuran/shot-you/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk 26](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)

[Download APK](https://github.com/jiangmuran/shot-you/releases/latest) ·
[Report a bug](https://github.com/jiangmuran/shot-you/issues) ·
[简体中文文档](README.zh-CN.md)

</div>

---

## The problem

Phones make it trivial to take twenty shots of the same moment. They make it miserable to
choose. You pinch-zoom across near-duplicates, someone always blinked, the good pose has
the bad background, and you settle. Every photo you took is useful raw material — but
nobody has the patience to combine them.

## What Shot You does

```
  Your gallery            On device              Background (your AI key)            You decide
 ────────────         ────────────────         ───────────────────────────       ─────────────────
  pick / drag-     →   downscale + batch    →   VLM clusters near-dupes,      →    curate: keep,
  select a pile        (sliding window,         tags a category, suggests          uncheck redundant,
  of photos            never blocks the UI)     which are worth generating         zoom, tweak prompt
                                                          │
                                                          ▼
                                            for each kept group, generate
                                            3 candidates (conservative /      →    swipe, pick,
                                            balanced / bold) — the real            ask for changes,
                                            reference photos are fed in            send leftovers
                                            via images/edits                       to the recycle bin
```

Classification and generation run as **background sessions** in a queue you can watch from
the status bar, pause, and resume — keep shooting while it works.

---

## Highlights

| | |
|---|---|
| **Never blocks** | Classification is a sliding window over the VLM, run **concurrently with retry**. Hundreds of photos, no frozen UI; progress lives in a staged queue and the status bar. |
| **Spend deliberately** | The VLM tags each group with a category and a *worth-generating?* hint, so redundant clusters are flagged before you pay for a single generation. Curate, zoom, and edit prompts first. |
| **Grounded generation** | Your actual reference photos are sent to the image model via `images/edits` — results reflect the real subject, not a hallucination. Output aspect ratio matches the source. |
| **Three takes, your call** | Every group yields **conservative / balanced / bold** candidates with genuinely different prompts. Swipe to compare, keep any, or *ask for changes* to iterate. |
| **Beautiful but believable** | Prompts are engineered by a portrait-photographer persona: real composition, lighting and skin texture — tuned to avoid the plastic "AI look". |
| **Per-category styles** | Map people → portrait, scenery → travel, and so on; plus Realistic / Beautify / Cinematic / Fresh / Artistic presets with an intensity slider. |
| **Bring-your-own AI** | Any **OpenAI-compatible** endpoint with a **custom API host** (official, a relay, or a local server). Keys live on-device; no backend. |
| **Real cost tracking** | A usage dashboard with your own per-million-token and per-image pricing — calls, tokens, images and estimated spend. |
| **Stays alive, gently** | Foreground-service generation with a status-bar progress notification; optional **root** Doze-whitelist with negligible battery cost. |
| **Yours, safely** | Originals go to the system **recycle bin** (recoverable), never hard-deleted. Full album access incl. Android 14 partial selection. |
| **Polished** | Jetpack Compose + Material 3, a curated palette, pinch-to-zoom, edge-to-edge, dark mode, and in-app English / 简体中文 with prompts that follow your language. |

---

## How it works

1. **Select** photos in the Library — tap, or press-and-drag to sweep-select. Hit
   **Classify with AI**. Already-processed photos are marked.
2. Classification runs **in the background**; the run appears in the **Queue** as a session
   moving through *Classifying → Ready for review → Generating*, with live status-bar progress.
3. Open a **Ready** session to **curate**: each group shows its category, a thumbnail strip,
   a skip suggestion, and an editable prompt. Zoom in, uncheck what you don't want, **Start**.
4. Each kept group generates **three candidates**. Open it to **swipe** through them, keep
   what you like, or *ask for changes* to spin a refined candidate from the current one.
5. Finally, **move the leftover originals to the recycle bin** — or keep one.

---

## Architecture

A single-module, clean-MVVM app organised by layer:

```
ui/         Compose screens + ViewModels (Library, Queue, Groups/curation, Batch,
            Result, Templates, Usage, Settings) + navigation + theme + components
domain/     models, repository interfaces, AI contracts, PromptComposer, sessions
data/
  local/    Room (templates, jobs, usage, sessions) + migrations
  remote/   OpenAI-compatible client (chat vision, images/edits), AiProviderFactory
  repository/  repository implementations
  settings/ DataStore-backed settings
work/       WorkManager: ClassificationWorker + GenerationWorker (foreground)
di/         Hilt modules
```

- **Hilt** DI, **Room** + **DataStore** persistence, **WorkManager** for background sessions,
  **Retrofit/OkHttp** + **kotlinx.serialization** networking, **Coil** images.
- The AI layer is fully abstracted behind `VlmProvider` / `LlmProvider` / `ImageGenProvider`
  and an `AiProviderFactory` — adding a provider is one class.
- A **sliding window with union-find merge** keeps large selections under the model's
  per-request image limit without ever splitting a group across two requests.

> **Built in the open, by a fleet of agents.** The foundation and contracts were laid first,
> then features were developed in parallel across isolated git worktrees and merged behind a
> GitHub Actions build gate, with adversarial review passes hunting bugs between releases.
> The commit history is the build log.

---

## Get started

### Install
Grab the latest **APK** from [Releases](https://github.com/jiangmuran/shot-you/releases/latest)
(or the [CI artifacts](https://github.com/jiangmuran/shot-you/actions)) and install it.

### Point it at your AI
Open **Settings** and set:

- **API Host** — defaults to `https://api.openai.com/v1`; change it to any OpenAI-compatible
  endpoint (official, a relay, or a local server).
- **API Key** — stored on-device only.
- **Models** — VLM (classification), LLM (prompt optimization), and image, set separately
  (defaults `gpt-4o`, `gpt-4o-mini`, `gpt-image-2`).
- Optional: **pricing** (per 1M tokens / per image), **per-category styles**, queue
  concurrency and pacing, keep-alive, language.

> The VLM model must support **image input (vision)**. On a relay host, make sure the VLM
> model name maps to a vision-capable model — otherwise classification can't see your photos
> and will describe them incorrectly. Any service implementing OpenAI `chat/completions` and
> `images/edits` works.

---

## Build from source

```bash
git clone https://github.com/jiangmuran/shot-you.git
cd shot-you
./gradlew assembleDebug    # -> app/build/outputs/apk/debug/app-debug.apk
```

Requires **JDK 17** and the Android SDK (**compileSdk 35**). Every push is built by CI; every
tag publishes a signed APK.

---

## Roadmap

- [x] Background classification sessions; staged, pausable queue with status-bar progress
- [x] Curate-before-you-spend; three candidates (conservative/balanced/bold); recycle bin
- [x] Per-category style rules; OpenAI-compatible custom host; bilingual; usage + pricing
- [ ] Side-by-side before/after compare on the result screen
- [ ] On-device perceptual-hash pre-clustering to cut VLM cost on huge batches
- [ ] Inpainting / targeted edits ("just fix the eyes")

Issues and pull requests welcome.

## License

[MIT](LICENSE) © 2026 jiangmuran
