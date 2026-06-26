<div align="center">

# Shot You 📸

**Pick your shots — let AI keep the best one.**

A modern, beautiful Android app that uses Vision-Language Models to group near-duplicate
photos (burst shots, same person / same place / different pose), then fuses the best of
each group into one near-perfect generated image.

</div>

---

## What it does

1. **Full album access** — pick a big batch of photos from your gallery (modern Android
   Photo Picker + Media permissions, including Android 14 partial access).
2. **Smart grouping** — a VLM clusters visually-similar shots (same subject / scene /
   burst) and explains *why* they belong together.
3. **Reference selection** — the model chooses a few of the strongest frames from each
   group as references.
4. **Prompt fusion** — add a template prompt (saved from your library, or write one and
   have an LLM refine it). Hair, expression, pose, position and more are all editable.
5. **Generation** — an image model produces one polished photo from the references +
   prompt. Not happy? Regenerate.
6. **Background queue** — generations run in a managed queue (WorkManager) so it stays
   seamless; configurable concurrency and request pacing.
7. **Usage dashboard** — see your call counts and token/credit usage per provider.

## Tech

- **Kotlin + Jetpack Compose** (Material 3, dynamic color, edge-to-edge)
- **Hilt** DI · **Room** · **DataStore** · **WorkManager** · **Coil** · **Retrofit/OkHttp**
- **Pluggable AI providers** — bring-your-own API key, mix & match a VLM, an LLM for
  prompt optimization, and an image-generation model (Gemini / OpenAI built in).
- Keys stored on-device; the app talks to providers directly (no backend required).

## Build

CI builds a debug APK on every push (see the **Android CI** workflow / Actions artifacts).
Locally:

```bash
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (compileSdk 35).

## Status

🚧 Under active development. See commit history for the build log.

## License

MIT — see [LICENSE](LICENSE).
