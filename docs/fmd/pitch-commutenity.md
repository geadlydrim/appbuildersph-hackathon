# CommuteNity: Five-Minute Demo Pitch & Presentation Script

**Project:** CommuteNity  
**Date:** 2026-10-09  
**Version:** 0.3  
**Owner:** Presenter (P4 - Jrabara101)  
**Status:** Ready for Rehearsal (Makati-only, Map-First Scope D20–D28)  
**Last reconciled:** 2026-10-09  
**Source brief:** [Idea](idea-commutenity.md) · **Product plan:** [PRD](prd-commutenity.md) · **Rules:** [JUDGING](JUDGING.md)

---

## 1. Story & Narrative Arc

Open on a New Arrival in Makati: no signal, a map, two points to connect, and unfamiliar streets. 
Turn on Airplane Mode live. Tap point A and point B on the offline map—the phone draws the best trip along real road lines, calculating exact fares, minutes, and where to shout "para".
Show what fellow riders suggest and vote offline. 
Start the trip: watch the phone track live progress offline and trigger the "Para na!" alert before the stop.
Then ask in Taglish or voice, check the vehicle signboard text, and close on why this only works locally.
Every stop, fare, and road shape comes from our verified local pack. Code decides facts; AI parses and phrases. No hallucinated fares or imaginary routes.

---

## 2. Timed Presentation Scenes (5:00 Total / 300 Seconds)

The demo phone (POCO X6 Pro) is mirrored live on the venue display via `scrcpy` over USB.  
The hero trip is the Makati pair chosen under A13.

| Time | On Screen | Presenter Script & Live Actions | Stagecraft & Fallback Notes |
|---|---|---|---|
| **0:00–0:30**<br>*(30s)* | Photo/visual of a crowded Makati street or terminal | *"Google Maps tells you to walk 4 kilometers along an expressway. Waze only knows private cars. You just arrived in Makati, you have zero data signal, two unfamiliar places to connect, and ten seconds to decide which jeep to ride. Commute knowledge in Manila lives entirely in people's heads—and you need it exactly when the cloud disappears."* | Strong, relatable hook. Establishes the real commute pain point in Makati. |
| **0:30–0:45**<br>*(15s)* | Swipe down Android quick settings live; turn on **Airplane Mode** | *"Everything you are about to see runs 100% on this phone. The map itself is a vector file stored on the device."*<br>*(Point to the Offline Badge on the app header).*<br>*"No Wi-Fi, no cellular data, no cloud API."* | **Critical judging moment:** Visually proves Local AI compliance immediately before running any query. |
| **0:45–1:45**<br>*(60s)* | **The Offline Map Trip Builder (T0)**<br>Tap Point A, tap Point B (or search a pack place).<br><br>The best trip appears drawn on real roads with legs, fares, minutes, and alight cue.<br><br>Tap outside Makati: *"Not in my data"*. | *"Watch the map. I tap Point A and Point B. Instantly, the best route appears, drawn on real roads—not straight lines. We precomputed road geometries into our local pack, and the phone ranks the route locally. Exact fare: ₱15. Alight cue: 'Para sa landmark'.*<br><br>*(Tap point outside Makati)*: *"Notice what happens if I tap outside Makati: it cleanly says 'Not in my data'. Code decides facts from our pack; zero hallucinated routes."* | **T0 Walking Skeleton:** MapLibre rendering local PMTiles file offline.<br>Shows deterministic routing and precomputed road shapes ([D22](state.md#5-decisions), [D23](state.md#5-decisions)). |
| **1:45–2:25**<br>*(40s)* | **Alternatives & Community (T1)**<br>Tap "View Alternatives". Bottom sheet opens with Algorithm & Community trips. | *"Want other options? Commuters know shortcuts algorithms miss. Here is an alternative route shared by a fellow rider, drawn on the map.*<br><br>*(Tap 👍 'Worked for me' vote while offline):*<br>*"I just voted 'Worked for me'. That vote is stored locally in Room database. When I get signal hours later, it syncs silently in the background. Local-first community."* | Demonstrates **T1 (Alternatives + Community Layer)**.<br>Local-first SQLite/Room storage and asynchronous background sync. |
| **2:25–3:25**<br>*(60s)* | **Start Trip: Tracking & Para Alert (T2)**<br>Tap "Start Trip". Follows location along the polyline.<br>Alert triggers: vibration, notification, banner **"Para na! ⟨stop⟩"**. | *"Now we tap 'Start Trip'. GPS works completely without data. Our on-device foreground service matches each GPS fix to the route polyline in real time, tracking progress and distance to the alight point.*<br><br>*(Alert fires):*<br>*"And there it is: vibration, notification, and banner: 'Para na!'. You never miss your stop. Your GPS track is processed on-device and discarded immediately when the trip ends."*<br><br>*(If replayed via mock track:)* *"Note: On stage, this route replay uses a simulated GPX track."* | **T2 In-Trip Tracking ([D25](state.md#5-decisions)):** Offline GPS map-matching to polyline.<br>**Honesty rule:** Disclose if using simulated/mock GPX replay on stage. |
| **3:25–4:05**<br>*(40s)* | **Ask in Words & Vehicle Check (T3)**<br>Type or speak Taglish query.<br>Vehicle check: text match on signboard. | *"Prefer words over pins? We type or say: 'Paano pumunta sa Ayala galing Guadalupe?' The on-device model parses the Taglish and sets the pins automatically.*<br><br>*Next: 'Is this the correct vehicle?' We enter the signboard text: 'GUADALUPE'. The phone matches it against our trip's legs: 'Yes, ride this.' Enter a wrong sign: 'No, look for AYALA'. Strict code decides the verdict; the model cannot guess."* | **T3 Natural Language & STT ([D26](state.md#5-decisions), [D27](state.md#5-decisions)):** whisper.cpp STT + deterministic text match (OCR dropped). Skip any sub-part not shipped. |
| **4:05–4:20**<br>*(15s)* | **The On-Device Ranker (T4)**<br>*(Only if shipped)* | *"How do we order community routes? We trained a lightweight pairwise ranker directly on commuter preferences running locally on the phone. On held-out validation trips, it agrees with experienced riders over hand-crafted rules."*<br><br>*(If T4 cut: skip this scene and spend time on Why Local).* | **Honesty check:** Disclose that event training data is team-generated ([D13](state.md#5-decisions)). Never claim fake benchmarks. |
| **4:20–4:45**<br>*(25s)* | Slide / UI: **The 4 Reasons Why Local Matters** | *"Why can't this be a cloud app? Four hard facts:*<br>1. **No Signal:** Terminals, underpasses, and CBD canyons are dead zones.<br>2. **No Data / Load:** Commuters shouldn't spend ₱50 prepaid load just to check a ₱15 fare.<br>3. **Curb-Speed Latency:** Jeeps don't wait 10 seconds for a server round-trip.<br>4. **Absolute Privacy:** Your GPS track and commute habits never leave your pocket."* | **The Cloud Fails at the Curb.** Directly satisfies Hackathon Theme: why Local AI creates an experience impossible with cloud AI. |
| **4:45–5:00**<br>*(15s)* | Final Screen: Team & GitHub QR | *"CommuteNity puts transit intelligence where it belongs: on the commuter's phone. 100% offline map, tracking, and local AI. We're Keanu, Pablo, Jeff, and John. Scan to explore our open-source repo. Thank you!"* | Clear, confident finish. Direct call-to-action for the audience People's Choice vote. |

**Total:** 300 seconds. Rehearse with real latency.  
**Cut order if time runs short:** The ranker scene (4:05), then voice, then the rest of T3. **Never cut the airplane-mode, map trip builder, or tracking scenes.**

---

## 3. Demo Preparation, Hardware & Fallback Matrix

### Equipment & Stage Setup
* **Primary Phone:** POCO X6 Pro (MediaTek Dimensity 8300-Ultra, HyperOS) with pre-warmed models, offline PMTiles map, and pre-compiled Makati commute pack.
* **Mirroring:** `scrcpy` over high-quality USB-C cable to laptop (`scrcpy --max-fps=60 --video-bit-rate=16M --stay-awake`).
* **Display Settings:** Phone screen timeout set to **10 minutes**, Do Not Disturb enabled, brightness locked at 85%, app set to high-contrast theme.
* **Backup Phone:** Teammate's Android phone with the exact same APK, offline map, and local pack preloaded.
* **Rehearsed Points:** Hero A/B points in Makati known and practiced; one out-of-Makati point ready to demonstrate graceful bounds.
* **Mock Location App:** Mock-location app configured with the hero-trip GPX track loaded ([QAD §2](qad-commutenity.md#2-data-and-environment)); location and notification permissions granted.

### Live Fallback Rules
| If this happens on stage... | Do this immediately: | Say this to the judges: |
|---|---|---|
| **Live GPS fix is unavailable or jumps inside venue** | Switch to the loaded mock-location GPX replay. | *"We are running this on a simulated GPX track recorded earlier on this exact route."* *(Honesty rule: never present simulation as live GPS).* |
| **On-device phrasing / LLM is slow (> 8s)** | The template answer card renders deterministically beneath the input. | *"Our deterministic fallback surfaced the exact route instantly while the LLM phrasing polishes in the background."* |
| **`scrcpy` mirroring disconnects** | Reconnect USB once; if failed, switch to laptop showing the pre-recorded video backup. | *"Switching to our pre-recorded screen capture—recorded live on the exact same POCO X6 Pro in airplane mode earlier today."* *(Never claim a recording is live).* |
| **Sync demo fails** | Show the unsynced badge indicator in the Room UI. | *"Because we are local-first, the vote is preserved safely on the device and will retry sync when network connectivity returns."* |

---

## 4. Judging Rubric Alignment

| Criterion | Weight | How CommuteNity Proves It in 5 Minutes |
|---|---:|---|
| **Problem & Usefulness** | **25%** | Solves the universal Metro Manila commuting pain point: unmapped informal transit, missing signage, confusing fares, and dead zones at critical moments. The Makati story, a best trip on real roads, and a reliable "para" alert. |
| **Local AI Implementation** | **25%** | Demonstrated in live **Airplane Mode**. Offline vector map (PMTiles), on-device GPS map-matching polyline tracking, and on-phone LLM / STT / Ranker. **Claim only what shipped.** |
| **Technical Execution** | **20%** | Stable Jetpack Compose app, robust deterministic graph traversal over local commute pack, precomputed road polylines, reliable background GPS service, honest simulated states. |
| **Innovation** | **15%** | A map-first trip builder that works completely offline, plus tracking with a "para" alert, local-first community knowledge, and an on-device preference ranker. |
| **Product & Demo Quality** | **15%** | Highly polished UI with "Jeepney Gold" aesthetic, honest offline states, transparent reason chips, and reliable live execution. |

---

## 5. Judge Q&A Defenses (Anticipated 3-Minute Questions)

| Question | Winning Response |
|---|---|
| **Why not just call Claude or GPT-4o mini over cellular?** | *"Because when you're standing at a terminal or under an MRT station, you have 0 bars. Even with signal, prepaid data costs money—commuters shouldn't spend load to check a fare. Plus, cloud models routinely hallucinate non-existent jeep routes and outdated fares."* |
| **Does tracking need data?** | *"No. GPS operates entirely without data. The phone matches each GPS fix to the active trip's stored road polyline and calculates on-route status and the para alert locally in airplane mode."* |
| **Where does location data go?** | *"Your GPS track goes nowhere. It is processed solely in the foreground service on the device, never uploaded to any server, and immediately discarded when the trip ends."* |
| **How do you guarantee the model doesn't hallucinate a route?** | *"Our architecture strictly decouples facts from language. The LLM only extracts entities into structured JSON. Our deterministic Kotlin routing engine computes the valid legs, stops, and LTFRB fares from the local pack. The model only phrases the result."* |
| **Can it say 'ride this' wrongly?** | *"The language model never decides the vehicle check. A strict text match against the active trip's route names and signboards determines the verdict. If unsure, it advises: 'Not sure, check the signboard'."* |
| **Does it read signboards with the camera?** | *"No. Camera OCR was dropped for this event to focus on high-reliability core transit needs. The rider types or speaks the signboard text, and deterministic matching validates it."* |
| **Are those lines real roads?** | *"Yes. We precomputed each route's road line at data-build time from OpenStreetMap data using an open routing engine and stored the polylines directly in the pack. The phone renders stored shapes without needing an on-device routing engine."* |
| **How does the map work offline?** | *"A Makati vector-tile extract (Protomaps PMTiles) is bundled directly on the phone and rendered offline by MapLibre Native Android. OpenStreetMap attribution is displayed on the map."* |
| **Why only Makati?** | *"Accuracy over coverage. We'd rather get one city's trips and road shapes 100% verified, reliable, and working on real roads than claim broad coverage with broken routes."* |
| **Isn't your community data from your own team?** | *"Yes. We disclose transparently in our README and app that hackathon contributions and preference labels are team-generated across four riders. However, the schema, Room database, and Supabase Edge Function sync are built to ingest real commuter submissions post-launch."* |
| **How big is the app and download?** | *"The commute pack and offline PMTiles map extract together are only a few megabytes. With the quantized int4 model, the one-time Wi-Fi download is ~700 MB – 1 GB, and it never uses cellular data again."* |
| **Why focus only on Android?** | *"Android holds over 86% market share in the Philippines, and practically 100% of the mass transit commuter demographic. Building native Kotlin with LiteRT gives us direct access to phone hardware acceleration."* |
| **Did you build on prior code?** | *"No code was carried over; all Kotlin, scripts, and configurations were built during this hackathon. We openly disclose that the high-level CommuteNity concept, pin-builder ideas, and route-stop data schema originated from an earlier ideation, per rule disclosure requirements."* |

---

## 6. Official Submission Video Script (~60 Seconds)
*Required for Cerebral Valley submission & social post (#AppBuildersPH tagging Devin/Cognition).*

* **0:00–0:10 (The Hook):**  
  *(Visual of Makati street or terminal):*  
  *"In Metro Manila, commute routes live in people's heads. When you need directions at a terminal, you have zero data signal, no signs, and no time."*
* **0:10–0:20 (The Proof: Airplane Mode):**  
  *(Screen capture showing phone swipe down and toggling Airplane Mode):*  
  *"This is CommuteNity. We're in Airplane Mode. Vector map, transit pack, and AI—100% offline."*
* **0:20–0:35 (Map Trip Builder):**  
  *(Tapping Point A and Point B on the offline Makati map):*  
  *"Tap Point A, tap Point B. CommuteNity instantly draws the best route along real road shapes with exact LTFRB fares and alight cues."*
* **0:35–0:48 (Tracking & The Para Alert):**  
  *(Tapping 'Start Trip', GPS snaps to route, notification banner pops up):*  
  *"Hit 'Start Trip'. Completely offline GPS tracks your progress and alerts you: 'Para na!' before your stop. Your location data never leaves your phone."*
* **0:48–1:00 (The Punchline):**  
  *"No cloud APIs. No tracking. No signal required. CommuteNity brings Local AI to the commuters who need it most."*

---

## 7. Submission Answer Draft

**Prompt: Why does this product benefit from running AI locally?**

> Commuters in Metro Manila need transit guidance at the exact moments when cloud connectivity is unavailable or unaffordable: crowded terminals, underground underpasses, or on prepaid SIMs with no active data. CommuteNity puts the whole trip on the phone: an offline Makati vector map (PMTiles), a local commute pack with precomputed road shapes, deterministic route ranking, offline GPS tracking with a "para" alert, and on-device language and speech models. It answers instantly with no signal, costs zero cellular data, eliminates hallucinated fares through deterministic verification, and protects commuter privacy by ensuring personal GPS tracks and daily movement patterns never leave the device.
