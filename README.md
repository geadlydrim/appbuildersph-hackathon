# CommuteNity

A commute assistant for Filipino commuters that works **with no internet**. Ask "Paano pumunta sa Dela Rosa galing V.A. Rufino?" by typing (Taglish or English) or by voice (English). An AI model running **on the phone** reads the question, and the app shows the trip on an offline map: which jeep or bus to ride, the fare, the minutes, and where to say "para". At the stop, ask "Is this the right jeep?" with the signboard text you see.

CommuteNity is meant for commuters anywhere in the Philippines. **For this hackathon we built it for one city first, Makati**, so every route in the demo could be drawn on the real road and every fare checked against the fare table. A wide, half-right map is worse than a narrow, correct one. How it grows to other cities is under [From Makati to more cities](#from-makati-to-more-cities).

AppBuildersPH Hackathon 2026, theme Local AI.

## Team

- Keanu Agustin (GitHub `geadlydrim`)
- John Rainier Valencia (GitHub `pablo-pica`)
- Jefferson Tuparan (GitHub `storms23`)
- Johnrick Rabara (GitHub `Jrabara101`)

## Why run the AI locally?

Commuters need directions exactly where the cloud fails them: underpasses, crowded terminals, and prepaid SIMs with no load. On the phone, the question is understood with **no signal and no data cost**, and **nothing the rider types or says leaves the phone**. The AI only reads the question; the route, fare, minutes, and the "right jeep?" verdict come from stored commute data through plain code, so the answer can't be made up.

## What works in this build

Verified on the demo phone (POCO X6 5G, Snapdragon 7s Gen 2) in **airplane mode**, 2026-10-10:

- **Offline map of Makati** (the pilot city): pan and zoom. Tap to set A then B. Hold a pin, then drag it to move it.
- **Ask by text:** Taglish or English, read by the on-device AI. "Paano pumunta sa Dela Rosa galing V.A. Rufino?" sets A and B and shows the trip.
- **Ask by voice:** English, on-device speech recognition; same path as typing.
- **The real trip** from V.A. Rufino St to Dela Rosa St, Pio del Pilar, drawn along the road on Gil Puyat Ave:
  - The card shows: jeep **₱14**, **16 min** in all (7 min on board), 0 transfers, labelled "Cheapest", signboard "LRT" / "Buendia - LRT".
  - Steps: walk ~540 m, ride, walk ~100 m, then say "para" at Gil Puyat Ave near Osmeña Hwy / PNR.
  - A short answer sentence in English or Taglish, built from those facts.
- **"Is this the right jeep?":** e.g. "Tama ba tong jeep? Buendia LRT nakalagay" gives "Yes, ride this"; "PASAY GUADALUPE" gives "No, look for "LRT" or "Buendia - LRT"".
- **Rider Q&A** (Questions screen): **sample data**, marked in the app. "2 riders say this works · sample" shows on the jeep card. Sample answers never change the order of the demo trip.
- **EDSA Carousel** stops in Makati (Guadalupe, Buendia, Ayala / One Ayala), both directions, with the fare from the air-con bus rule (₱18 within Makati). "Guadalupe to Glorietta" rides the Carousel.
- **Door-to-door ranking:** trips are compared including the walk to the first stop and from the last stop, so the app doesn't send you 700 m to a farther stop for a ride that's only faster on paper.
- **More Makati routes and places from OpenStreetMap:**
  - 118 bus routes, 295 named stops and 339 Makati places (malls, barangays, stations, schools, parks and more), so a trip like "Power Plant Mall to Glorietta" works too.
  - These trips are marked **"OSM · unverified"**: fares show as "Fare unknown", and minutes are estimates shown as "~N min (est.)" (120 m per minute, calibrated on the hero ride).
  - Short hops suggest walking.

Measured on that phone (on-device AI):
- The AI is ready about 16 s after opening the app.
- Parsing a question: median 2.10 s, worst p95 2.84 s.
- 10/10 parsed correctly on 10 Makati test questions × 20 runs.

### Not built yet (planned in [`docs/fmd/`](docs/fmd/))

- In-trip GPS tracking and the "Para na!" alert.
- "Use my location": a stand-in that sets V.A. Rufino St and says so on screen. No GPS is used.
- Online refresh, community trip suggestions and votes, and sync.
- An alternatives list (the ₱15 bus is computed as the runner-up but not shown).
- The learned ranker.
- A 30-question AI evaluation.

## From Makati to more cities

Makati is the first city, not the limit. Everything city-specific is **data**, so a new city is a new data pack, not new app logic:

- **Routes, stops and places:** `data/pack/osm_import.py` builds a city's bus routes, stops and named places from OpenStreetMap. OSM already has bus routes elsewhere: Manila 172 (167 bus, 5 jeepney) and Pasig 64, counted on 2026-10-10.
- **Jeepney routes** are thin in OSM everywhere we checked, so they come from people who ride them, the way the team wrote the Makati demo trip, and later from riders' suggestions in the app.
- **Fares** are rules in a table (base fare plus per-km, per vehicle type), not hard-coded numbers.
- **Offline map:** one small map file per city, cut from the same open basemap (Makati's is 3.3 MB).
- **The on-device AI doesn't change per city:** it only copies place names out of the question, and the city's pack supplies the places. Other languages, like Bisaya, need their own keyword lists; voice is English only for now.

What's still needed to go beyond Makati: the city bounds and messages are hard-coded to Makati in this build, so the first step is making the city a setting.

## What runs locally, what needs internet

| Runs on the phone, no internet | Needs internet (one time, before using the app) |
|---|---|
| Understanding the question (Gemma 4 E2B on LiteRT-LM, GPU) | Downloading the 2.6 GB model from Hugging Face |
| English speech-to-text (Android on-device recognizer) | Getting the English offline speech pack, if the phone doesn't have it |
| Place search, trip finding and ranking, fares, minutes, answer sentence, "right jeep?" check (Kotlin) | Building the app (Gradle downloads libraries) |
| Map display (MapLibre + bundled PMTiles map) | Building the data: stop and place lookups and OSM routes (Nominatim, Overpass), and the hero road line (OSRM), done once by the team and stored in the repo |
| Rider Q&A (sample data bundled; your own answers stay on the phone) | — |

The app makes no network requests at runtime.

## Build and install

Requirements: Android Studio (or the Android SDK with JDK 17+) and a phone with Android 12+ (arm64).

```sh
cd android
./gradlew :app:assembleDebug          # Windows: gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The commute data (`data/pack/hero-trip.source.json`) and the sample Q&A (`data/mock/rider-qa.json`) are copied into the app at build time. A pre-built APK is attached to the [`demo-safe-f8` release](https://github.com/geadlydrim/appbuildersph-hackathon/releases/tag/demo-safe-f8). It was built before the pin-drag fix, so build from `master` for the latest.

Run the unit tests: `./gradlew :app:testDebugUnitTest`.

## Put the AI model on the phone (one time)

The model is 2.6 GB, so it is not inside the APK.

1. Download `gemma-4-E2B-it.litertlm` (2,588,147,712 bytes) from <https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm> (ungated, Apache-2.0).
2. Copy it to the path the app reads (`AiConfig.MODEL_PATH`):

   ```sh
   adb shell mkdir -p /data/local/tmp/llm
   adb push gemma-4-E2B-it.litertlm /data/local/tmp/llm/gemma-4-E2B-it.litertlm
   ```
3. Open the app. The ask box says "AI loading…", then "On-device AI ready". After that everything works in airplane mode.

Without the model the app still runs, but asking in words falls back to simple name matching ("AI unavailable; simple matching").

## Try it

1. Turn on **airplane mode** and open the app; wait for "On-device AI ready".
2. Tap **Ask…**, type `Paano pumunta sa Dela Rosa galing V.A. Rufino?`, tap **Find**. Or tap the mic and say "How do I get from V.A. Rufino to Dela Rosa Street?".
3. Read the trip card and the answer sentence; tap **Taglish** to switch language.
4. Ask `Tama ba tong jeep? Buendia LRT nakalagay`.
5. Hold the B pin and drag it somewhere else; the trip recomputes.

## Disclosures

### Models

- **Gemma 4 E2B**, instruction-tuned (`gemma-4-E2B-it.litertlm`, from `litert-community/gemma-4-E2B-it-litert-lm` on Hugging Face), Apache-2.0. It runs fully on the phone and only parses the question. Its phrasing is off; answer text is a template.
- **Android's built-in on-device speech recognizer** (English) for voice.
- Tested and **not used**: Gemma3-1B int4 and Qwen3-0.6B int4 (LiteRT-LM). Results are on the [`prototype/llm-speed-test`](https://github.com/geadlydrim/appbuildersph-hackathon/blob/prototype/llm-speed-test/spikes/llm-speed-test/RESULTS.md) branch.

### Technologies and frameworks

Kotlin 2.4.21, Jetpack Compose (BOM 2024.10.01), Android Gradle Plugin 8.13.2, Gradle 8.14.3, **LiteRT-LM** `litertlm-android` 0.18.0, **MapLibre Android** 11.13.5, PMTiles, AndroidX Activity Compose, org.json (tests), JUnit 4. Data scripts: Python 3 (`data/pack/shapes.py`).

### APIs and cloud services

- **No cloud API at runtime.** No cloud AI is used at all.
- **Build-time only**, to make the stored data:
  - OSRM public demo server: the road line for the hero ride, fetched once.
  - OpenStreetMap **Nominatim** and **Overpass**: stop and place lookups, and the OSM route and place import (`data/pack/osm_import.py`), run once.
- **Hugging Face:** model download.
- **GitHub:** repo, issues, releases.

### Data and sources

- **Hero trip** (`data/pack/hero-trip.source.json`): **team-generated** from the team's own route knowledge (`known`). The jeep fare is ₱14, the traditional-jeep minimum from the team's fare table (first 4 km). The ₱15 bus fare and about 7 min on board are from memory and were not checked on a ride during the event. Stop points come from team-supplied plus codes and OpenStreetMap.
- **Fare table** (team-supplied, 2026-10-10): air-conditioned bus ₱18 for the first 5 km + ₱2.98/km; ordinary bus ₱15 + ₱2.49/km; traditional jeep ₱14 for the first 4 km + ₱2.00/km; modern jeep ₱17 + ₱2.40/km. Used for the hero jeep and the EDSA Carousel; fares are worked out per ride from its distance.
- **EDSA Carousel in Makati** (`data/pack/carousel-makati.source.json`, built by `data/pack/carousel.py`): the operating stops Guadalupe, Buendia and Ayala (One Ayala), both directions, from [Wikipedia's EDSA Carousel stops table](https://en.wikipedia.org/wiki/EDSA_Carousel). Road line from OpenStreetMap. Fare by the air-con bus rule (₱18 within Makati); minutes are estimates.
- **Road line:** OSRM driving route over OpenStreetMap data (ODbL), stored in `data/pack/`.
- **OSM routes and places** (`data/pack/osm-makati.source.json`, built by `data/pack/osm_import.py` from Overpass): bus routes, stops and route geometry, plus places inside Makati's OSM boundary. © OpenStreetMap contributors (ODbL). Not verified by the team: OSM fares are not used ("Fare unknown"), and minutes are estimates (120 m/min). **Sakay.ph data is not used:** its Terms of Service forbid extracting or reusing it.
- **Map:** Makati extract of the Protomaps basemap (`makati-20261009-z14.pmtiles`), © OpenStreetMap contributors (ODbL), with attribution shown on the map. Map fonts and icons are from [protomaps/basemaps-assets](https://github.com/protomaps/basemaps-assets): Noto Sans glyphs under the SIL Open Font License, and icons derived from MIT-licensed tangrams/icons. The style is based on [protomaps/basemaps](https://github.com/protomaps/basemaps).
- **Rider Q&A** (`data/mock/rider-qa.json`): **mock, hand-written by the team**. Every record is tagged `source_class: mock` and shown as "sample" in the app.

### Existing code and assets

- **No code was reused.** Everything in this repo was written during the hackathon.
- **Ideas reused from the team's earlier CommuteNity project:** the idea, personas, the route and stop data shape, the route-picker concept, and the visual direction ([CLR §5](docs/fmd/clr-commutenity.md#5-ip-provenance-and-disclosure)).
- **UI font:** Plus Jakarta Sans.
- **App logo** (`res/drawable-nodpi/logo.png`). <!-- TODO(owner): state where the logo came from (made by the team? generated? reused?) before submitting. -->

### AI development tools

- **Claude**, an AI coding agent, used through the omp harness / Claude Code. It helped with planning docs, code, reviews, and on-phone testing.
- **Cursor.**

## Project docs

Decisions, plans, and status live in [`docs/fmd/`](docs/fmd/). Start with [`docs/fmd/state.md`](docs/fmd/state.md).
