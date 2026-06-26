<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="0" height="0" alt="" />

# 📸 Shot You

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

## ✨ The idea

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

## 🧩 Features

| | |
|---|---|
| 🖼️ **Full album access** | Modern Photo Picker + media permissions, incl. Android 14 *partial* (user-selected) access, with graceful fallbacks down to Android 8. |
| 🧠 **Smart grouping** | VLM clusters visually-similar shots and returns a title + reason + reference picks per group. |
| 🎯 **Reference selection** | The best frames of each group are pre-selected and fully editable. |
| ✍️ **Prompt fusion** | Saved template library (with built-ins) + one-tap **LLM prompt optimization**; quick-insert chips for hair / expression / pose / position. |
| 🌅 **Generation & regenerate** | One fused image from references + prompt; regenerate until you're happy; auto-saved to your gallery; share sheet. |
| 🛰️ **Background queue** | WorkManager-driven job queue with configurable **concurrency**, **request pacing**, retries and Wi-Fi-only mode — fully unobtrusive. |
| 📊 **Usage dashboard** | Call counts by provider & operation, token totals, image count and estimated cost. |
| 🔌 **Bring-your-own AI** | Mix & match providers per role (VLM / LLM / image). **Gemini** and **OpenAI** built in. Keys stored **on-device**, no backend. |
| 🎨 **Modern UI** | Jetpack Compose, **Material 3**, dynamic color, edge-to-edge, dark mode. |

---

## 🏗️ Architecture

Clean, modular MVVM — a single-module app organised by layer:

```
ui/            Compose screens + ViewModels  (Library · Groups · Generate ·
               Result · Queue · Templates · Usage · Settings) + navigation + theme
domain/        Models · repository interfaces · AI provider contracts · SessionStore
data/
  local/       Room (templates · jobs · usage)
  remote/ai/   Gemini + OpenAI clients, DTOs, AiProviderFactory
  repository/  Repository implementations
  settings/    DataStore-backed settings
work/          WorkManager generation worker
di/            Hilt modules
```

- **Hilt** for DI · **Room** + **DataStore** for persistence · **WorkManager** for the
  queue · **Retrofit/OkHttp** + **kotlinx.serialization** for the network layer ·
  **Coil** for images.
- AI is fully abstracted behind `VlmProvider` / `LlmProvider` / `ImageGenProvider` and an
  `AiProviderFactory`, so adding a provider is a single class.

> 🛠️ Fun fact: the entire app was built in parallel — the foundation and contracts first,
> then five features developed simultaneously in isolated git worktrees and merged, with
> GitHub Actions as the build gate. See the commit history for the full build log.

---

## 🚀 Getting started

### Install
Grab the latest **`shot-you-debug.apk`** from the
[Releases](https://github.com/jiangmuran/shot-you/releases) page (or from the
[CI artifacts](https://github.com/jiangmuran/shot-you/actions)) and install it.

### Add your API key
Open **Settings** and paste a key for the provider(s) you want. Keys never leave your
device.

| Provider | Get a key | Default models (editable) |
|---|---|---|
| **Google Gemini** | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) | `gemini-2.5-flash` (VLM/LLM) · `gemini-2.5-flash-image` (image) |
| **OpenAI** | [platform.openai.com/api-keys](https://platform.openai.com/api-keys) | `gpt-4o` (VLM) · `gpt-4o-mini` (LLM) · `gpt-image-1` (image) |

You can assign a different provider to each role (e.g. Gemini for grouping, OpenAI for
images).

### Use it
**Library** → multi-select photos → **Group with AI** → open a group → pick a template /
write & optimize a prompt → **Generate** → watch it in **Queue** → open the **Result** →
regenerate if you like.

---

## 🔨 Build from source

```bash
git clone https://github.com/jiangmuran/shot-you.git
cd shot-you
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
```

Requires **JDK 17** and the Android SDK (**compileSdk 35**). CI builds a debug APK on
every push.

---

## 🗺️ Roadmap

- [ ] Side-by-side before/after compare on the result screen
- [ ] On-device pre-clustering (perceptual hash) to cut VLM cost on huge batches
- [ ] Inpainting / targeted edits ("just fix the eyes")
- [ ] Multiple candidates per generation, pick-the-best
- [ ] More providers (Stability, local models)

Contributions welcome.

## 📄 License

[MIT](LICENSE) © 2026 jiangmuran
