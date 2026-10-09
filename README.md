# CommuteNity

An Android commute assistant for Makati City that works **with no internet**. Ask "Paano pumunta sa Dela Rosa galing V.A. Rufino?" by typing (Taglish or English) or by voice (English). An AI model running **on the phone** reads the question, and the app shows the trip on an offline map: which jeep or bus, the fare, the minutes, and where to say "para". You can also ask "Is this the right jeep?" with the signboard text you see.

AppBuildersPH Hackathon 2026, theme Local AI.

## What runs on the phone (no internet needed)

| Part | How |
|---|---|
| Understanding the question | **Gemma 4 E2B** (`gemma-4-E2B-it.litertlm`, Apache-2.0) on **LiteRT-LM 0.18.0**, GPU, fully on-device. Code picks the intent and preference; the model only copies place names and signboard text out of the question; code drops anything the rider didn't write. |
| Voice | Android's built-in **on-device** speech recognizer, English. |
| Places, trips, fares, minutes | Deterministic Kotlin over the bundled commute data (`data/pack/`). The AI never decides a route, fare, or time. |
| "Is this the right jeep?" | Deterministic match against the stored signboards. |
| Map | **MapLibre Android 11.13.5** with a bundled Makati **PMTiles** extract (Protomaps basemap, OpenStreetMap data). |

## Build and install

Requirements: Android Studio (or the Android SDK) and a phone with Android 12+ (arm64). The demo phone is a POCO X6 5G.

```sh
cd android
./gradlew :app:assembleDebug          # Windows: gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The commute data (`data/pack/hero-trip.source.json`) is copied into the app automatically at build time.

## Put the AI model on the phone (one time)

The model is 2.6 GB, so it is not inside the APK. Download it once, then copy it to the phone over USB:

1. Download `gemma-4-E2B-it.litertlm` (2,588,147,712 bytes) from Hugging Face: <https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm> (ungated, Apache-2.0).
2. Copy it to the path the app reads (`AiConfig.MODEL_PATH`):

   ```sh
   adb shell mkdir -p /data/local/tmp/llm
   adb push gemma-4-E2B-it.litertlm /data/local/tmp/llm/gemma-4-E2B-it.litertlm
   ```
3. Open the app. The ask box says "AI loading…" for about 15–30 s, then "On-device AI ready". After that everything works in airplane mode.

Without the model the app still works, but asking in words falls back to simple name matching ("AI unavailable; simple matching").

## Try it

1. Turn on **airplane mode**.
2. Tap **Ask…**, type `Paano pumunta sa Dela Rosa galing V.A. Rufino?` and tap **Find**. Or tap the mic and say "How do I get from V.A. Rufino to Dela Rosa Street?".
3. The trip appears: the ₱12 jeep (cheapest), walk, ride along Gil Puyat Ave, and the "para" point, drawn on the map.
4. Ask `Tama ba tong jeep? Buendia LRT nakalagay` → "Yes, ride this".
5. You can also tap the map to set A and B.

## Data and sources

- **Hero trip** (V.A. Rufino St → Dela Rosa St, Pio del Pilar): jeep ₱12 and city bus ₱15, about 7 min on board, signboard "LRT" / "Buendia - LRT". Recorded from the team's route knowledge (`known`, from memory), not checked on a ride during the event. Stop locations from owner plus codes and OpenStreetMap (Nominatim, Overpass).
- **Road line** for the ride: OSRM demo server, driving profile, fetched once and stored (`data/pack/cache/`). OpenStreetMap data, ODbL.
- **Map**: Protomaps basemap extract of Makati, © OpenStreetMap contributors (ODbL). Attribution is shown on the map.
- Rider Q&A on the Questions screen is **sample data**, marked as such in the app.

## Project docs

Decisions and plans live in [`docs/fmd/`](docs/fmd/) — start with [`docs/fmd/state.md`](docs/fmd/state.md).
