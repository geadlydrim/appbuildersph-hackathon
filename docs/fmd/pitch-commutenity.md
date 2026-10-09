# CommuteNity: Five-Minute Demo Pitch & Presentation Script

**Project:** CommuteNity  
**Date:** 2026-10-09  
**Version:** 0.3  
**Owner:** Presenter (P4 - Jrabara101)  
**Status:** Ready for Rehearsal  
**Last reconciled:** 2026-10-09 (Aligned with Decisions D1–D19)  
**Source brief:** [Idea](idea-commutenity.md) · **Product plan:** [PRD](prd-commutenity.md) · **Rules:** [JUDGING](JUDGING.md)

---

## 1. Story & Narrative Arc

* **The Reality:** Google Maps tells you to walk 4 kilometers along a highway. Waze only knows private cars. You’re at a chaotic jeepney terminal with zero data signal, three jeeps slowing down, and ten seconds to decide.
* **The Problem:** In Metro Manila, commute knowledge is tribal—it lives entirely in commuters' heads, and you need it right when the cloud fails.
* **The Innovation:** CommuteNity puts tribal commute intelligence directly onto your device. 
* **The Proof:** Airplane mode on stage. A natural Taglish question answered with exact legs, fares, and "para" spots in seconds with zero network calls.
* **The Rule of Grounding:** Code decides facts; AI parses and phrases. No hallucinated fares or imaginary routes.

---

## 2. Timed Presentation Scenes (5:00 Total / 300 Seconds)

The demo phone (POCO X6 Pro) is mirrored live on the venue display via `scrcpy` over USB.

| Time | On Screen | Presenter Script & Live Actions | Stagecraft & Fallback Notes |
|---|---|---|---|
| **0:00–0:35**<br>*(35s)* | Photo/visual of a crowded Manila jeepney terminal | *"Google Maps tells you to walk 4 kilometers. Waze only knows private cars. You just arrived in Manila, you're at a crowded terminal, you have zero data signal, three jeeps are slowing down, and you have ten seconds to pick. Commute knowledge in Manila lives entirely in people's heads—and you need it exactly when the cloud is nowhere to be found."* | Strong hook. Resonates with every Manila commuter and the judging panel. |
| **0:35–0:50**<br>*(15s)* | Swipe down Android quick settings live; turn on **Airplane Mode** | *"Everything you are about to see runs 100% on the silicon inside this phone."*<br>*(Point to the Offline Badge on the app header).*<br>*"No WiFi, no cellular data, no cloud API."* | **Critical judging moment:** Visually proves Local AI compliance immediately before running any query. |
| **0:50–1:55**<br>*(65s)* | Type or speak Taglish query:<br>**"Paano pumunta sa Recto galing Malanday?"**<br><br>Hero Route Card appears with legs, exact fares, minutes, and "para" cues. | **While query processes (3–8s):**<br>*"Notice there's no loading spinner reaching out to AWS. The phone is tokenizing Taglish, extracting destination entities, and matching our local commute pack right now on the NPU."*<br><br>**When Best Route Card appears:**<br>*"Here is the optimal route: the Malanday–Recto e-jeep straight to Avenida. Exact fare: ₱35. Alight cue: 'Para sa Doroteo Jose'. It tells you what signboard to read and where to shout 'para'.*<br>*And here is our golden rule: The language model understood my Taglish, but it did NOT invent the route. Code decides facts; models parse and phrase. Zero hallucinated fares."* | **Hero Corridor:** Valenzuela–Recto ([D15](state.md#5-decisions)).<br>**Dead-Air Strategy:** Narrate the on-device NPU/CPU processing while inference runs. Never pause in silence. |
| **1:55–2:45**<br>*(50s)* | Tap **"View Alternatives"**.<br>Bottom sheet opens showing algorithmic vs community-submitted routes. | *"What if you want other options? Commuters know shortcuts algorithms miss. Here's an alternative: taking the jeep to LRT-1 Monumento, riding to Doroteo Jose, and walking. A fellow rider contributed this route."*<br><br>*(Tap 👍 'Worked for me' vote while still offline):*<br>*"I just voted 'Worked for me'. That vote is stored locally in Room database. When I regain signal hours later, it syncs silently in the background. Local-first community."* | Demonstrates **T1 (Alternatives + Community Layer)**.<br>Emphasizes local-first storage and asynchronous sync. |
| **2:45–3:25**<br>*(40s)* | **The On-Device Ranker** *(if T2 shipped)*<br><br>OR **Deterministic Baseline** *(if T2 cut)* | **If T2 shipped:**<br>*"How do we order these routes? We trained a lightweight pairwise ranker directly on commuter preferences. It runs entirely on the device. On held-out validation trips, it agrees with experienced riders over our hand-crafted heuristics."*<br><br>**If T2 cut:**<br>*"Our baseline ranks lexicographically: fewest transfers first, then travel minutes, with community upvotes breaking ties. Every rider preference stays completely transparent."* | **Honesty check:** Disclose that event training data is team-generated ([D13](state.md#5-decisions)). Never claim fake benchmarks. |
| **3:25–3:55**<br>*(30s)* | **Signboard OCR Check** *(if T3 shipped)*<br><br>OR **Graceful Fallback / Out-of-Coverage** | **If T3 shipped:**<br>Point phone camera at printed signboard ("RECTO / MONUMENTO"). Instant green banner: *"Ride this jeep"*. Switch to wrong signboard ("CUBAO"): red banner: *"Wrong jeep"*. *"Camera frames are processed on-device via ML Kit; your camera feed never touches a server."*<br><br>**If T3 skipped:**<br>Show out-of-coverage query: app clearly admits coverage boundaries without hallucinating. | **Cut Rule:** If demo time is running long, skip signboard first to guarantee full 60s for "Why Local" and closing. |
| **3:55–4:35**<br>*(40s)* | Slide / UI: **The 4 Reasons Why Local Matters** | *"Why can't this be a cloud app? Four hard facts:*<br>1. **No Signal:** Terminals, basements, and LRT stations are connectivity dead zones.<br>2. **No Data / Load:** Commuters shouldn't spend ₱50 prepaid load just to know a ₱15 jeep fare.<br>3. **Curb-Speed Latency:** Jeeps don't wait 10 seconds for a server round-trip.<br>4. **Absolute Privacy:** Your daily transit habits and location traces never leave your pocket."* | **The Cloud Fails at the Curb.** Directly satisfies Hackathon Theme: why Local AI creates an experience impossible with cloud AI. |
| **4:35–5:00**<br>*(25s)* | Final Summary Screen & GitHub QR | *"CommuteNity is built for the everyday Filipino commuter. Local language models, local embeddings, local ranker, 100% offline facts. We're Keanu, Pablo, Jeff, and John. Scan to check our open-source repo. Thank you!"* | Clear, confident finish. Direct call-to-action for the audience People's Choice vote. |

---

## 3. Demo Preparation, Hardware & Fallback Matrix

### Equipment & Stage Setup
* **Primary Phone:** POCO X6 Pro (MediaTek Dimensity 8300-Ultra, HyperOS) with pre-warmed models and pre-compiled commute pack.
* **Mirroring:** `scrcpy` over high-quality USB-C cable to laptop (`scrcpy --max-fps=60 --video-bit-rate=16M --stay-awake`).
* **Display Settings:** Phone screen timeout set to **10 minutes**, Do Not Disturb enabled, brightness locked at 85%, app set to high-contrast theme.
* **Backup Phone:** Teammate's Android phone with the exact same APK and local pack preloaded.
* **Printed Props:** Two laminated/printed jeepney signboards ("RECTO" and "CUBAO") for T3 OCR demo.

### Live Fallback Rules
| If this happens on stage... | Do this immediately: | Say this to the judges: |
|---|---|---|
| **On-device phrasing takes > 8 seconds** | The template answer card renders deterministically beneath the input. | *"Our deterministic fallback surfaced the exact route instantly while the LLM phrasing polishes in the background."* |
| **`scrcpy` mirroring disconnects** | Reconnect USB once; if failed, switch to laptop showing the 60s pre-recorded video backup. | *"Switching to our pre-recorded screen capture—recorded live on the exact same POCO X6 Pro in airplane mode earlier today."* *(Never claim a recording is live).* |
| **Sync demo fails** | Show the unsynced badge indicator in the Room UI. | *"Because we are local-first, the vote is preserved safely on the device and will retry sync when network connectivity returns."* |

---

## 4. Judging Rubric Alignment

| Criterion | Weight | How CommuteNity Proves It in 5 Minutes |
|---|---:|---|
| **Problem & Usefulness** | **25%** | Solves the universal Metro Manila commuting pain point: unmapped informal transit, missing signage, confusing fares, and zero signal at critical moments. |
| **Local AI Implementation** | **25%** | Demonstrated in live **Airplane Mode**. Core inference (LLM parsing, vector embedding lookup, route ranking, OCR) runs entirely on the device hardware without network fallbacks. |
| **Technical Execution** | **20%** | Stable Jetpack Compose app, robust deterministic graph traversal over local commute pack, zero hallucinations, fast local response. |
| **Innovation** | **15%** | First on-device commute assistant combining offline Taglish intent parsing with a learned commuter-preference ranker and local-first crowd contributions. |
| **Product & Demo Quality** | **15%** | Highly polished UI with "Jeepney Gold" aesthetic, honest offline states, transparent reason chips, and reliable live execution. |

---

## 5. Judge Q&A Defenses (Anticipated 3-Minute Questions)

| Question | Winning Response |
|---|---|
| **Why not just call Claude or GPT-4o mini over cellular?** | *"Because when you're standing at a terminal in Monumento or under an MRT station, you have 0 bars. Even with signal, prepaid data costs money—commuters shouldn't spend load to check a fare. Plus, cloud models routinely hallucinate non-existent jeep routes and outdated fares."* |
| **How do you guarantee the model doesn't hallucinate a route?** | *"Our architecture strictly decouples facts from language. The LLM only extracts entities ('Recto', 'Malanday') into structured JSON. Our deterministic Kotlin routing engine computes the valid legs, stops, and LTFRB fares from the local pack. The model only phrases the result."* |
| **How big is the app and model download?** | *"The one-time download on Wi-Fi is ~700 MB to 1 GB (quantized int4 Gemma3-1B / Qwen via LiteRT-LM, plus our 2 MB commute pack). Once downloaded, it never consumes a single byte of data."* |
| **Isn't your community training data biased or fake?** | *"We disclose transparently in our README and app that hackathon training pairs are team-generated across four riders. However, the schema, pairwise ranking pipeline, and Room-to-Supabase Edge Function sync are built to ingest real commuter submissions post-launch."* |
| **Why focus only on Android?** | *"Android holds over 86% market share in the Philippines, and practically 100% of the mass transit commuter demographic. Building native Kotlin with LiteRT gives us direct access to phone NPUs and hardware acceleration."* |
| **How does your project handle bad actors submitting bogus routes?** | *"Every submitted leg must reference existing stops in the verified pack. Fares and travel times are always calculated by code, not by the contributor. Furthermore, upvotes are rate-limited per device ID and bounded so spam cannot hijack ranking."* |
| **Did you build on prior code?** | *"No code was carried over; all Kotlin, scripts, and configurations were built during this hackathon. We openly disclose that the high-level CommuteNity concept and route-stop data schema originated from an earlier ideation, per rule disclosure requirements."* |

---

## 6. Official Submission Video Script (~60 Seconds)
*Required for Cerebral Valley submission & social post (#AppBuildersPH tagging Devin/Cognition).*

* **0:00–0:10 (The Hook):**  
  *(Camera on presenter at street/terminal, or screen showing Manila transit):*  
  *"In Metro Manila, commute routes live in people's heads. When you need directions at a terminal, you have no data signal, no signs, and no time."*
* **0:10–0:20 (The Proof):**  
  *(Screen capture showing phone swipe down):*  
  *"This is CommuteNity. We're turning on Airplane Mode. Zero internet."*
* **0:20–0:40 (The Core Query):**  
  *(Typing Taglish):* *"Paano pumunta sa Recto galing Malanday?"*  
  *(Instant route card pops up):*  
  *"Our on-device model understands Taglish, matches our offline transit pack, and gives the exact e-jeep leg, the ₱35 fare, and where to shout 'para'."*
* **0:40–0:50 (Community & Ranker):**  
  *(Opening alternatives):*  
  *"Need shortcuts? Here are alternative routes submitted by fellow riders, re-ranked by an on-device preference model. We vote offline, syncing only when we're back online."*
* **0:50–1:00 (The Punchline):**  
  *"No cloud APIs. No tracking. No signal required. CommuteNity brings Local AI to the commuters who need it most."*

---

## 7. Submission Answer Draft

**Prompt: Why does this product benefit from running AI locally?**

> Commuters in Metro Manila need transit guidance at the exact moments when cloud connectivity is unavailable or unaffordable: crowded terminals, underground underpasses, or on prepaid SIMs with no active data. CommuteNity runs its language model, entity embeddings, route ranker, and OCR locally on Android hardware using LiteRT and ML Kit against an offline commute pack. It delivers sub-second answers with zero network latency, costs zero cellular data, eliminates hallucinated fares through deterministic verification, and protects commuter privacy by ensuring personal movement patterns and camera feeds never leave the phone.
