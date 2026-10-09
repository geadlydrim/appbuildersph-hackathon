# CommuteNity: Pitch & Presentation Script

**Project:** CommuteNity  
**Date:** 2026-10-09  
**Version:** 0.5 (Shortened: 3:50 Spoken Pitch + 1:00 Demo Video)  
**Presenter:** Jrabara101 (not a fixed role; [D35](state.md#5-decisions))  
**Total Presentation:** 4:50 (Within 5:00 limit; 10s buffer) + 3:00 Judge Q&A  
**Phone Setup:** POCO X6 5G mirrored via `scrcpy` over USB to projector  
**Hero Trip ([D30](state.md#5-decisions)):** Ayala Center (Station Rd, San Lorenzo) → Dela Rosa St, Pio del Pilar, Makati  
**Release Floor ([D31](state.md#5-decisions)):** On-device LLM (PRD-F8 ask in words) is release-critical MVP  

---

## 1. Executive Narrative: Problem → Solution → Demo → Ask

* **The Problem (Pain & Scarcity):** 14 million commuters face an unmapped informal transit system. At the curb, they face four scarcities: **zero signal** in underpasses, **zero prepaid load** for data, **zero seconds** before a jeep leaves, and **zero public signs**. Google Maps fails; the cloud abandons commuters when they need it most.
* **The Solution (Local AI):** CommuteNity compresses Makati's transit brain into on-device silicon: an offline vector map (PMTiles), precomputed real road lines, offline GPS map-matching, and an on-device LLM.
* **The Demo (Proof):** 100% Airplane Mode. Sub-second A-to-B route generation on real roads, offline community shortcuts, and in-trip GPS with a haptic **"Para na!"** alert.
* **The Grounding Rule:** Code decides facts (fares, routes, stops); AI parses intent. Zero hallucinations.
* **The Ask:** Back the team bringing Local AI where it matters most: to the everyday Filipino commuter standing in the rain with zero load.

---

## 2. Timed Presentation Scenes (3:50 Pitch + 1:00 Video = 4:50 Total)

| Time | Phase | On Screen | Physical Action (What You Do) | Spoken Script (What You Say) |
|---|---|---|---|---|
| **0:00–0:45**<br>*(45s)* | **Problem & Scarcity** | Slide: Crowded Makati terminal / Ayala rush hour | Stand center stage. Make direct eye contact with judges. | *"Every single day, 14 million Filipinos face the most exhausting commute in Southeast Asia. Here in Makati, Google Maps is useless—it tells you to walk 4 kilometers in the heat, and Waze only knows private cars.<br><br>At the terminal, commuters face four brutal scarcities: **zero signal** in underpasses, **zero prepaid load** to spend on data, **zero seconds** before the jeep leaves you behind, and **zero public signs**. Commute knowledge lives in locals' heads—and the cloud fails right when you need it at the curb."* |
| **0:45–1:25**<br>*(40s)* | **Solution & Architecture** | Slide: CommuteNity on-device architecture & Offline badge | Hold up the demo phone (POCO X6 5G) in hand. | *"That is why we built CommuteNity. We compressed Makati's tribal transit intelligence directly into the silicon of this phone.<br><br>The entire vector map, precomputed road shapes, and language model live on the device. And our golden rule: **Code decides facts; AI parses intent.** Fares and routes come strictly from our verified local pack. Zero hallucinated routes, zero imaginary fares."* |
| **1:25–2:10**<br>*(45s)* | **Feature Walkthrough** | Slide: Feature breakdown (Offline Map, Community, GPS Tracking) | Point to the three core features on the slide. | *"CommuteNity delivers three core breakthroughs:<br>1. **The Map Trip Builder:** Tap Point A to Point B—it plots real road paths, not straight lines, with exact LTFRB fares.<br>2. **Local-First Community:** Riders share shortcut routes. You vote 'Worked for me' completely offline, saved in local Room storage to sync later.<br>3. **In-Trip GPS Tracking:** An offline foreground service snaps your location to the route and buzzes your pocket with a 'Para na!' alert before your stop."* |
| **2:10–2:55**<br>*(45s)* | **Why Local Matters** | Slide: 'The Cloud Fails at the Curb' (4 Pillars) | Count 4 points on fingers. | *"Why can't this just be a cloud app? Four hard realities:<br>1. **No Signal:** Basements and CBD canyons are dead zones.<br>2. **No Data:** Commuters shouldn't spend ₱50 of prepaid load to check a ₱15 fare.<br>3. **Curb Latency:** Jeeps don't wait 10 seconds for a server round-trip.<br>4. **Absolute Privacy:** Your daily transit tracks never leave your pocket."* |
| **2:55–3:30**<br>*(35s)* | **The Ask & Transition** | Slide: Team photo & GitHub repo QR code | Gesture toward screen, then transition to video player. | *"Big tech builds AI for Silicon Valley high-speed fiber. We built CommuteNity for the commuter standing in the rain at a Makati terminal with zero load.<br><br>Don't take our word for it. Watch CommuteNity running live on this phone in 100% Airplane Mode."* |
| **3:30–3:50**<br>*(20s)* | **Video Intro & Setup** | Transition screen / Video player ready | Click play on the 60-second Demo Video. Step to the side. | *"Here is our one-minute live screen capture—recorded entirely on this POCO X6 5G with zero internet."* |
| **3:50–4:50**<br>*(60s)* | **1-Minute Demo Video** | **1-MINUTE DEMO VIDEO PLAYS** *(See Section 3 for video breakdown)* | Stand beside display; let video audio play (or deliver tight live narration). | *(Video audio plays: Airplane mode on $\rightarrow$ Ayala Center to Dela Rosa St $\rightarrow$ Real road route $\rightarrow$ Tracking $\rightarrow$ 'Para na!' alert $\rightarrow$ Vehicle text check).* |
| **4:50–4:50+**<br>*(10s)* | **Closing Punchline** | Final Slide: CommuteNity Logo & People's Choice Vote | Step forward, bow/nod, open for Q&A. | *"Zero cloud APIs. Zero tracking. 100% on-device. We are CommuteNity—vote for us in People's Choice. Thank you!"* |

---

## 3. The 1-Minute Demo Video Breakdown (Plays at 3:50–4:50)

*Also serves as your official video post for X/LinkedIn (#AppBuildersPH tagging Devin/Cognition).*

* **0:00–0:10 (Airplane Mode):** Swipe down quick settings on phone; toggle **Airplane Mode**. Show the "Offline" badge clearly.  
  *Audio/Caption:* *"Airplane Mode is ON. Zero Wi-Fi, zero cellular data."*
* **0:10–0:25 (Map Trip Builder - T0):** Tap Point A (**Ayala Center**) and Point B (**Dela Rosa St, Pio del Pilar**).  
  *Audio/Caption:* *"Sub-second response: plots the best route along real road shapes with exact ₱15 fare and alight cue."*
* **0:25–0:35 (Community Shortcuts - T1):** Open "View Alternatives". Tap 👍 *"Worked for me"*.  
  *Audio/Caption:* *"Community shortcuts saved locally in Room database; syncs when online."*
* **0:35–0:48 (In-Trip Tracking & 'Para' Alert - T2):** Tap *"Start Trip"*. GPS snaps to polyline. Notification and banner pop up: **"Para na! ⟨stop⟩"**.  
  *Audio/Caption:* *"Offline GPS tracks progress and alerts you right before your stop. Your location never leaves your phone."*
* **0:48–1:00 (On-Device LLM - D31 MVP):** Type *"Paano pumunta sa Dela Rosa St. galing Ayala Center?"* Route pins set automatically. Signboard check validates vehicle text.  
  *Audio/Caption:* *"On-device LLM understands Taglish. Code decides facts. CommuteNity brings Local AI to the commuters who need it most."*

---

## 4. Demo Preparation & Fallback Matrix

### Equipment Setup
* **Primary Phone:** POCO X6 5G (Snapdragon 7s Gen 2) with offline PMTiles map, commute pack, and the Gemma 4 E2B model preloaded.
* **Mirroring / Video:** Laptop HDMI to projector. Video file queued in VLC/QuickTime in full-screen.
* **Settings:** Phone timeout set to 10 min, Do Not Disturb ON, brightness 85%.

### Fallback Rules
| Issue on Stage | Action | Say to Judges |
|---|---|---|
| **Video doesn't play audio** | Narrate live using the cues in Section 3. | *"I'll narrate our live screen capture directly."* |
| **Projector / Mirroring lag** | Focus on your spoken delivery; switch to the laptop backup. | *"Switching to our local backup screen."* |
| **Question about mock GPS** | Disclose transparently. | *"The live demo replay uses a verified GPX track recorded on this exact Makati route."* |

---

## 5. Judge Q&A Defenses (3 Minutes)

| Question | Winning 10-Second Response |
|---|---|
| **Why not call Claude or GPT-4o mini over cellular?** | *"Underpasses and terminals have zero bars. Even with signal, prepaid data costs money—commuters shouldn't spend load to check a fare. Plus, cloud models hallucinate non-existent jeep routes."* |
| **Does GPS tracking consume data?** | *"Zero data. GPS operates purely via satellite hardware. Our on-device foreground service matches fixes to the trip polyline and triggers the 'Para na!' alert completely in airplane mode."* |
| **Where does location data go?** | *"Nowhere. Location fixes are processed in the on-device foreground service and discarded immediately when the trip ends. Zero tracking."* |
| **How do you prevent route hallucinations?** | *"Code decides facts; AI parses intent. The LLM only extracts structured origin/destination JSON. Deterministic Kotlin code computes valid routes and LTFRB fares from our local pack."* |
| **Why focus only on Makati?** | *"Accuracy over coverage. We'd rather get one city's transit network 100% verified, road-accurate, and reliable than offer broad coverage filled with broken routes."* |
| **How big is the download?** | *"The language model, Gemma 4 E2B, is 2.6 GB. It downloads once over Wi-Fi and never uses cellular data again. The pack and offline map come on top of that."* |

---

## 6. Official Submission Answer Draft

**Prompt: Why does this product benefit from running AI locally?**

> Commuters in Metro Manila need transit guidance at the exact moments when cloud connectivity is unavailable or unaffordable: crowded terminals, underground underpasses, or on prepaid SIMs with no active data. CommuteNity puts the whole trip on the phone: an offline Makati vector map (PMTiles), a local commute pack with precomputed road shapes, deterministic route ranking, offline GPS tracking with a "para" alert, and an on-device language model. It answers instantly with no signal, costs zero cellular data, eliminates hallucinated fares through deterministic verification, and protects commuter privacy by ensuring personal GPS tracks and daily movement patterns never leave the device.
