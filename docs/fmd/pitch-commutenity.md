# CommuteNity: Pitch & Presentation Script

**Project:** CommuteNity  
**Date:** 2026-10-10  
**Version:** 0.6 (Revised to what is built: 3:50 Spoken Pitch + 1:00 Demo Video)  
**Presenter:** Jrabara101 (not a fixed role; [D35](state.md#5-decisions))  
**Total Presentation:** 4:50 (Within 5:00 limit; 10s buffer) + 3:00 Judge Q&A  
**Phone Setup:** POCO X6 5G mirrored via `scrcpy` over USB to projector  
**Hero Trip ([D37](state.md#5-decisions), amends D30):** V.A. Rufino St → Dela Rosa St, Pio del Pilar, Makati  
**Release Floor ([D31](state.md#5-decisions)):** On-device LLM (PRD-F8 ask in words) is release-critical MVP  

---

## 1. Executive Narrative: Problem → Solution → Demo → Ask

* **The Problem (Pain & Scarcity):** 14 million commuters face an unmapped informal transit system. At the curb, they face four scarcities: **zero signal** in underpasses, **zero prepaid load** for data, **zero seconds** before a jeep leaves, and **zero public signs**. Google Maps fails; the cloud abandons commuters when they need it most.
* **The Solution (Local AI):** CommuteNity puts Makati's transit help on the phone: an offline map (bundled PMTiles) with a real road line, and an on-device AI (Gemma 4 E2B) that understands Taglish questions typed in, and English questions spoken.
* **The Demo (Proof):** 100% Airplane Mode. Ask in words and get a real trip: **₱14 jeep, 16 minutes, 0 transfers, "Cheapest", signboard "LRT"**, the walk, the ride on Gil Puyat Ave, and the "para" point, drawn along the road. Then the **right-jeep check** ("Tama ba tong jeep?") and rider Q&A evidence (sample data, marked in the app).
* **The Grounding Rule:** Code decides facts (fares, routes, stops); AI only reads the question. The AI never invents a fare or a route.
* **The Ask:** Back the team bringing Local AI where it matters most: to the everyday Filipino commuter standing in the rain with zero load.

---

## 2. Timed Presentation Scenes (3:50 Pitch + 1:00 Video = 4:50 Total)

| Time | Phase | On Screen | Physical Action (What You Do) | Spoken Script (What You Say) |
|---|---|---|---|---|
| **0:00–0:45**<br>*(45s)* | **Problem & Scarcity** | Slide: Crowded Makati terminal / Ayala rush hour | Stand center stage. Make direct eye contact with judges. | *"Every single day, 14 million Filipinos face the most exhausting commute in Southeast Asia. Here in Makati, Google Maps is useless—it tells you to walk 4 kilometers in the heat, and Waze only knows private cars.<br><br>At the terminal, commuters face four brutal scarcities: **zero signal** in underpasses, **zero prepaid load** to spend on data, **zero seconds** before the jeep leaves you behind, and **zero public signs**. Commute knowledge lives in locals' heads—and the cloud fails right when you need it at the curb."* |
| **0:45–1:25**<br>*(40s)* | **Solution & Architecture** | Slide: CommuteNity on-device architecture & Offline badge | Hold up the demo phone (POCO X6 5G) in hand. | *"That is why we built CommuteNity. We put Makati's transit help directly on this phone.<br><br>The offline map, the road line, and the language model all live on the device. And our golden rule: **Code decides facts; AI reads the question.** Fares and routes come from our local pack, never from the AI. The AI never makes up a route or a fare."* |
| **1:25–2:10**<br>*(45s)* | **Feature Walkthrough** | Slide: Feature breakdown (Offline Map + Real Trip, On-Device AI, Right-Jeep Check + Rider Q&A) | Point to the three core features on the slide. | *"CommuteNity delivers three things, all working in airplane mode:<br>1. **Offline Map + Real Trip:** A Makati map that needs no signal. Tap A and B, or just ask, and you get the fare, the minutes, the signboard to look for, and where to say 'para', drawn along the real road.<br>2. **On-Device AI Ask:** Type in Taglish or speak in English. The AI runs on the phone and only reads your question; our code decides the trip.<br>3. **Right-Jeep Check + Rider Q&A:** Type what the jeep's signboard says and get 'Yes, ride this' or 'No, look for…'. Rider answers back it up. In this build they are sample data, and the app says so."* |
| **2:10–2:55**<br>*(45s)* | **Why Local Matters** | Slide: 'The Cloud Fails at the Curb' (4 Pillars) | Count 4 points on fingers. | *"Why can't this just be a cloud app? Four hard realities:<br>1. **No Signal:** Basements and CBD canyons are dead zones.<br>2. **No Data:** Commuters shouldn't spend ₱50 of prepaid load to check a ₱15 fare.<br>3. **Curb Latency:** Jeeps don't wait for a server round-trip.<br>4. **Privacy:** No cloud AI. Your questions stay on your phone."* |
| **2:55–3:30**<br>*(35s)* | **The Ask & Transition** | Slide: Team photo & GitHub repo QR code | Gesture toward screen, then transition to video player. | *"Big tech builds AI for Silicon Valley high-speed fiber. We built CommuteNity for the commuter standing in the rain at a Makati terminal with zero load.<br><br>Don't take our word for it. Watch CommuteNity running live on this phone in 100% Airplane Mode."* |
| **3:30–3:50**<br>*(20s)* | **Video Intro & Setup** | Transition screen / Video player ready | Click play on the 60-second Demo Video. Step to the side. | *"Here is our one-minute live screen capture—recorded entirely on this POCO X6 5G with zero internet."* |
| **3:50–4:50**<br>*(60s)* | **1-Minute Demo Video** | **1-MINUTE DEMO VIDEO PLAYS** *(See Section 3 for video breakdown)* | Stand beside display; let video audio play (or deliver tight live narration). | *(Video audio plays: Airplane mode on $\rightarrow$ offline map, AI ready $\rightarrow$ typed Taglish question $\rightarrow$ trip card and road line $\rightarrow$ spoken English question $\rightarrow$ right-jeep check $\rightarrow$ rider Q&A (sample) $\rightarrow$ drag a pin, trip recomputes).* |
| **4:50–4:50+**<br>*(10s)* | **Closing Punchline** | Final Slide: CommuteNity Logo & People's Choice Vote | Step forward, bow/nod, open for Q&A. | *"No cloud AI. Works in airplane mode. 100% on-device. We are CommuteNity—vote for us in People's Choice. Thank you!"* |

---

## 3. The 1-Minute Demo Video Breakdown (Plays at 3:50–4:50)

*Also serves as your official video post for X/LinkedIn (#AppBuildersPH tagging Devin/Cognition).*  
*Everything below is built and was checked on the demo phone in airplane mode. Airplane Mode stays visible in the status bar the whole time.*

1. **0:00–0:10 (Offline map, AI loading → ready):** Airplane Mode icon visible. App opens on the offline Makati map; the AI status goes from loading to ready. *(The AI takes ~16 s after opening the app; open it beforehand or trim the wait in the edit.)*  
   *Caption:* *"Airplane Mode is ON. Offline Makati map. The AI runs on this phone."*
2. **0:10–0:22 (Ask by text, Taglish):** Type *"Paano pumunta sa Dela Rosa galing V.A. Rufino?"* Pins A and B are placed; the trip card shows **₱14, 16 min, 0 transfers, "Cheapest", signboard "LRT"**; the line is drawn on Gil Puyat Ave.  
   *Caption:* *"Ask in Taglish. Pins, fare, minutes, signboard, and the road line."*
3. **0:22–0:28 (Answer sentence, Taglish toggle):** Read the answer sentence on the trip card, then toggle English ↔ Taglish.  
   *Caption:* *"The answer comes from the trip's own facts. Not made up by the AI."*
4. **0:28–0:38 (Ask by voice, English):** Tap the mic and say *"How do I get from V.A. Rufino to Dela Rosa Street?"* Same trip appears.  
   *Caption:* *"Speak in English. Phone's own on-device speech recognizer, same trip."*
5. **0:38–0:46 (Right-jeep check):** Type *"Tama ba tong jeep? Buendia LRT nakalagay"* The app answers **"Yes, ride this"**.  
   *Caption:* *"Is this the right jeep? Checked against the pack's signboards."*
6. **0:46–0:53 (Rider Q&A, sample):** Point to **"2 riders say this works · sample"** on the jeep card; tap through to the Questions screen.  
   *Caption:* *"Rider answers back it up. Sample data, marked in the app."*
7. **0:53–1:00 (Drag a pin):** Hold pin B and drag it; the trip recomputes.  
   *Caption:* *"Move a pin and the trip updates. No internet. No cloud AI. CommuteNity."*

---

## 4. Demo Preparation & Fallback Matrix

### Equipment Setup
* **Primary Phone:** POCO X6 5G (Snapdragon 7s Gen 2) with the bundled offline map and the Gemma 4 E2B model pre-installed (model installed via the README steps before the pitch).
* **Rollback APK:** GitHub release `demo-safe-f8`. Note: it predates the pin-drag fix, so holding and dragging a pin is not in that APK.
* **Mirroring / Video:** Laptop HDMI to projector. Video file queued in VLC/QuickTime in full-screen.
* **Settings:** Phone timeout set to 10 min, Do Not Disturb ON, brightness 85%.

### Fallback Rules
| Issue on Stage | Action | Say to Judges |
|---|---|---|
| **Video doesn't play audio** | Narrate live using the cues in Section 3. | *"I'll narrate our live screen capture directly."* |
| **Projector / Mirroring lag** | Focus on your spoken delivery; switch to the laptop backup. | *"Switching to our local backup screen."* |
| **AI still loading** | Wait about 16 s after opening the app, or open the app before going on stage. Tap A and B on the map meanwhile. | *"The AI is warming up on the phone. It loads once per app start, with no internet."* |
| **Live app fails** | Install rollback APK `demo-safe-f8` (no pin-drag), or play the video. | *"Switching to our recorded capture of the same phone."* |

---

## 5. Judge Q&A Defenses (3 Minutes)

| Question | Winning 10-Second Response |
|---|---|
| **Why not call Claude or GPT-4o mini over cellular?** | *"Underpasses and terminals have zero bars. Even with signal, prepaid data costs money—commuters shouldn't spend load to check a fare. Plus, cloud models hallucinate non-existent jeep routes."* |
| **Do you track GPS? Where does location data go?** | *"Not in this build. This build uses no location at all. In-trip GPS tracking with the 'Para na!' alert is designed and planned (T2): on-device map-matching, no network. It is not built yet. The 'Use my location' button is a demo stand-in that sets V.A. Rufino St."* |
| **How do you prevent route hallucinations?** | *"Code decides facts; AI reads the question. The AI only copies place names and signboard text, and our code drops anything the rider didn't write. Deterministic Kotlin code picks the trip, the fare, and the answer sentence from our local pack."* |
| **Why is the fare from memory?** | *"The hero trip data is our own route knowledge. It is marked `known` in the pack, we haven't checked it on a ride yet, and we disclose that."* |
| **Is the community data real?** | *"No. The rider Q&A is sample data. It is marked 'sample' in the app and we disclose it. Sample answers never reorder the hero trip."* |
| **How fast is the AI?** | *"Measured on this phone: median 2.10 s, worst p95 2.84 s to read a question. It got 10 out of 10 exact on our 10 Makati test questions, 20 runs each. After app open it needs about 16 s to be ready."* |
| **Why focus only on Makati?** | *"Accuracy over coverage. We'd rather get one city's transit network 100% verified, road-accurate, and reliable than offer broad coverage filled with broken routes."* |
| **How big is the download?** | *"The language model, Gemma 4 E2B, is 2.6 GB. It downloads once over Wi-Fi and never uses cellular data again. The offline map is only about 3.3 MB."* |

---

## 6. Official Submission Answer Draft

**Prompt: Why does this product benefit from running AI locally?**

> Commuters in Metro Manila need transit guidance at the exact moments when cloud connectivity is unavailable or unaffordable: crowded terminals, underground underpasses, or on prepaid SIMs with no active data. CommuteNity puts the whole thing on the phone: an offline Makati map (PMTiles) with a real road line, a local commute pack, and an on-device language model (Gemma 4 E2B on LiteRT-LM) that understands Taglish questions typed in and English questions spoken. The AI reads a question in about 2 seconds (measured median 2.10 s on the demo phone) and the app answers with a real trip (fare, minutes, signboard, and where to say "para") with no signal and zero cellular data. It also checks "is this the right jeep?" against the pack's signboards. Our code, not the AI, decides every fare and route, so the AI cannot invent them. Because there is no cloud AI, the commuter's questions never leave the phone. Rider answers are sample data in this build and are marked as such.
