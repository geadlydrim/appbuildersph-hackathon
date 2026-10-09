# CommuteNity: Five-Minute Demo Pitch & Presentation Script

**Project:** CommuteNity  
**Date:** 2026-10-09  
**Version:** 0.4 (Enhanced Problem → Solution → Demo → Ask Framework)  
**Owner:** Presenter (P4 - Jrabara101)  
**Status:** Pitch-Ready  
**Last reconciled:** 2026-10-09 (Makati-Only, Map-First Scope D20–D28)  
**Source brief:** [Idea](idea-commutenity.md) · **Product plan:** [PRD](prd-commutenity.md) · **Rules:** [JUDGING](JUDGING.md)

---

## 1. Executive Narrative: Problem → Solution → Demo → Ask

### 🔴 The Problem: The Pain of Four Scarcities
In Metro Manila, commuting is an exhausting daily survival test for 14 million people. In Makati—the financial capital—informal public transit (jeepneys, UVs, e-jeeps) is completely unmapped. Google Maps tells you to walk 4 kilometers in the midday heat, and Waze only cares about private cars. Commuters face **four brutal scarcities** at the curb:
1. **Signal Scarcity:** Concrete high-rises, underpasses, and crowded terminals are mobile dead zones.
2. **Data / Load Scarcity:** Millions of Filipinos rely on prepaid SIMs without active data. Nobody should have to burn ₱50 in prepaid load just to look up a ₱15 jeepney fare.
3. **Time Scarcity:** A jeep slows down for *five seconds*. If you hesitate at the curb, it’s gone, leaving you stranded in the rain.
4. **Information Scarcity:** Commute knowledge is purely *tribal*—it lives exclusively in locals' heads.

> **The Insight:** Commuters need transit intelligence at the exact moment the cloud completely abandons them.

### 🟢 The Solution: Commute Intelligence Compressed On-Device
CommuteNity is the first native, on-device commute assistant built specifically for informal transit in Makati. We compressed the city’s tribal commute brain directly into phone hardware: an offline vector map, precomputed real-road polylines, verified LTFRB fares, local GPS map-matching, and on-device language models. When the internet disappears, CommuteNity is 100% functional.

### 🟡 The Demo: Uncompromising Offline Proof
We flip **Airplane Mode live on stage**. Zero cellular data, zero Wi-Fi. 
- We tap Point A to Point B: the phone instantly draws the optimal trip along real roads, calculating fares and alight cues.
- We show community shortcuts and cast an offline vote stored in local Room storage.
- We start in-trip tracking: offline GPS snaps to the road, alerting the rider with a haptic **"Para na!"** banner right before their stop.
- We demonstrate grounded AI: Code computes facts; AI parses intent. Zero hallucinations.

### 🔵 The Ask: Bringing AI Where It Matters Most
Big tech builds AI for users with 5G fiber and air-conditioned cars. We built Local AI for the everyday Filipino commuter standing in the rain with zero load. Back the project solving an authentic, daily struggle for 14 million commuters.

---

## 2. Timed Presentation Scenes (5:00 Total / 300 Seconds)

The demo phone (POCO X6 Pro) is mirrored live on the venue display via `scrcpy` over USB.  
The hero trip is the Makati pair chosen under A13.

| Time & Phase | On Screen | Presenter Script & Live Actions *(Leading with Pain & Scarcity)* | Stagecraft & Fallback Notes |
|---|---|---|---|
| **0:00–0:30**<br>*(30s)*<br><br>**[PROBLEM & SCARCITY]** | Photo/visual of a crowded Makati street or terminal | *"Every single day, 14 million Filipinos face the most exhausting commute in Southeast Asia. Here in Makati, Google Maps is useless—it tells you to walk 4 kilometers in the heat, and Waze only knows private cars.*<br><br>*Standing at a terminal, you face four brutal scarcities: **zero signal** in underpasses, **zero prepaid load** to spend on data, **zero seconds** before the jeep leaves you behind, and **zero public signs**. Commute knowledge in Manila lives entirely in people's heads—and you need it right when the cloud fails."* | **Lead with raw pain and scarcity.** Resonates instantly with judges and audience. Sets up high stakes. |
| **0:30–0:45**<br>*(15s)*<br><br>**[SOLUTION & AIRPLANE FLIP]** | Swipe down Android quick settings live; turn on **Airplane Mode** | *"That is why we built CommuteNity. We took Manila's tribal transit intelligence and compressed it directly into the silicon of this phone.*<br><br>*(Swipe down, tap Airplane Mode live):*<br>*Watch the screen: Airplane Mode is ON. No Wi-Fi, no data, no server. Everything you will see from this second onwards runs 100% offline."* | **The Proof Point:** Visually proves Local AI compliance on stage before running any feature. |
| **0:45–1:45**<br>*(60s)*<br><br>**[DEMO: MAP & GROUNDING]** | **The Offline Map Trip Builder (T0)**<br>Tap Point A, tap Point B (or search a pack place).<br><br>The best trip appears drawn on real roads with legs, fares, minutes, and alight cue.<br><br>Tap outside Makati: *"Not in my data"*. | *"Look at the map. The entire city map is an offline vector file on this phone. I tap Point A, I tap Point B.*<br><br>*Instantly—sub-second, with zero network latency—it plots the optimal route along real streets. Not fake straight lines, real roads. Exact fare: ₱15. Alight cue: 'Para sa landmark'.*<br><br>*(Tap point outside Makati)*: *And if I tap outside our verified boundary, it says 'Not in my data'. Here is our core principle: Code decides facts from our pack; AI parses and phrases. Zero hallucinated routes, zero imaginary fares."* | **T0 Walking Skeleton:** MapLibre rendering local PMTiles file offline.<br>Proves deterministic routing, precomputed road shapes ([D22](state.md#5-decisions), [D23](state.md#5-decisions)), and anti-hallucination. |
| **1:45–2:25**<br>*(40s)*<br><br>**[DEMO: COMMUNITY]** | **Alternatives & Community (T1)**<br>Tap "View Alternatives". Bottom sheet opens with Algorithm & Community trips. | *"Algorithms only know official routes, but commuters know real street shortcuts. In 'View Alternatives', here is a faster route discovered by a fellow rider, drawn on the map.*<br><br>*(Tap 👍 'Worked for me' vote while offline):*<br>*I just voted 'Worked for me'. That vote didn't fail because I'm offline—it saved instantly in our local Room database. When I get signal hours later, it syncs silently. Local-first community intelligence."* | Demonstrates **T1 (Alternatives + Community Layer)**.<br>Emphasizes local-first SQLite/Room storage and asynchronous background sync. |
| **2:25–3:25**<br>*(60s)*<br><br>**[DEMO: TRACKING & 'PARA' ALERT]** | **Start Trip: Tracking & Para Alert (T2)**<br>Tap "Start Trip". Follows location along the polyline.<br>Alert triggers: vibration, notification, banner **"Para na! ⟨stop⟩"**. | *"Now imagine this: You are packed inside a dark, crowded jeepney. It's pouring rain outside, the windows are fogged up, and you can't see the street signs. You tap 'Start Trip'.*<br><br>*GPS works without cellular data. Our on-device service matches your position to the road line in real time.*<br><br>*(Alert triggers with vibration/banner):*<br>*And right before the stop—'Para na!' The phone alerts you exactly when to shout. You never miss your stop, and your personal GPS track never leaves your device."* | **T2 In-Trip Tracking ([D25](state.md#5-decisions)):** Offline GPS map-matching to polyline.<br>**Honesty rule:** If running via mock-location GPX on stage, disclose: *"This route replay uses a simulated track."* |
| **3:25–4:05**<br>*(40s)*<br><br>**[DEMO: LANGUAGE & VEHICLE]** | **Ask in Words & Vehicle Check (T3)**<br>Type or speak Taglish query.<br>Vehicle check: text match on signboard. | *"What if you don't want to use pins? We speak or type Taglish: 'Paano pumunta sa Ayala galing Guadalupe?' Our on-device language model extracts the destination and pins it.*<br><br>*Next: You see three jeeps idling. You ask: 'Is this the correct vehicle?' and enter the signboard: 'GUADALUPE'. Instant confirmation: 'Yes, ride this.' Enter the wrong sign: 'No, look for AYALA'. Strict deterministic code decides the check—AI never guesses."* | **T3 Natural Language & STT ([D26](state.md#5-decisions), [D27](state.md#5-decisions)):** whisper.cpp STT + deterministic text match. Skip any sub-part not shipped. |
| **4:05–4:20**<br>*(15s)*<br><br>**[DEMO: RANKER]** | **The On-Device Ranker (T4)**<br>*(Only if shipped)* | *"To rank multiple community routes, we trained a lightweight ranker directly on commuter preferences. It runs on the phone, learning how real riders trade off transfers versus walking minutes."*<br><br>*(If T4 cut: skip directly to Why Local).* | **Honesty check:** Disclose that event training data is team-generated ([D13](state.md#5-decisions)). Never claim fake benchmarks. |
| **4:20–4:45**<br>*(25s)*<br><br>**[WHY LOCAL: SCARCITY RECAP]** | Slide / UI: **The 4 Reasons Why Local Matters** | *"Why can't this be a cloud app? Because **the cloud fails at the curb**.*<br>1. *When cellular drops in Ayala underpasses, cloud AI is dead.*<br>2. *When you have no prepaid load, cloud queries cost money you don't have.*<br>3. *When a jeep pauses for 5 seconds, cloud latency is too slow.*<br>4. *And your daily transit routes shouldn't be tracked on a remote corporate server."* | **The Cloud Fails at the Curb.** Directly satisfies Hackathon Theme: why Local AI creates an experience impossible with cloud AI. |
| **4:45–5:00**<br>*(15s)*<br><br>**[THE ASK & CLOSE]** | Final Screen: Team & GitHub QR | *"Big tech builds AI for Silicon Valley high-speed fiber. We built CommuteNity for the everyday commuter standing in the rain at a Makati terminal with zero load.*<br><br>*100% offline map, tracking, and local AI. We're Keanu, Pablo, Jeff, and John. Scan the QR code to check our open-source repo, and vote for CommuteNity. Thank you!"* | **Memorable Ask:** High-emotion finish. Directly requests the People's Choice vote. |

**Total:** 300 seconds (5:00). Rehearse with real latency.  
**Cut order if running short:** Cut T4 ranker (4:05), then voice, then T3 vehicle check. **Never cut the airplane-mode, map trip builder, or tracking scenes.**

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

* **0:00–0:12 (The Pain & Scarcity):**  
  *(Visual of crowded Makati street or terminal):*  
  *"In Metro Manila, 14 million commuters navigate an unmapped transit system. At the terminal, you have four scarcities: zero signal, zero prepaid load, zero signs, and five seconds to catch your jeep."*
* **0:12–0:22 (The Solution & Proof):**  
  *(Screen capture showing phone swipe down and toggling Airplane Mode):*  
  *"This is CommuteNity. We're turning on Airplane Mode. Entire vector map, transit pack, and AI—100% on the device."*
* **0:22–0:36 (The Map Demo):**  
  *(Tapping Point A and Point B on the offline Makati map):*  
  *"Tap Point A, tap Point B. CommuteNity instantly plots the best route along real road shapes with exact fares and alight cues. Zero hallucinations."*
* **0:36–0:48 (Tracking & The Para Alert):**  
  *(Tapping 'Start Trip', GPS snaps to route, notification banner pops up):*  
  *"Hit 'Start Trip'. Completely offline GPS tracks your progress and alerts you: 'Para na!' right before your stop. Your location data never leaves your phone."*
* **0:48–1:00 (The Ask & Punchline):**  
  *"No cloud APIs. No tracking. No signal required. CommuteNity brings Local AI to the commuters who need it most."*

---

## 7. Submission Answer Draft

**Prompt: Why does this product benefit from running AI locally?**

> Commuters in Metro Manila need transit guidance at the exact moments when cloud connectivity is unavailable or unaffordable: crowded terminals, underground underpasses, or on prepaid SIMs with no active data. CommuteNity puts the whole trip on the phone: an offline Makati vector map (PMTiles), a local commute pack with precomputed road shapes, deterministic route ranking, offline GPS tracking with a "para" alert, and on-device language and speech models. It answers instantly with no signal, costs zero cellular data, eliminates hallucinated fares through deterministic verification, and protects commuter privacy by ensuring personal GPS tracks and daily movement patterns never leave the device.
