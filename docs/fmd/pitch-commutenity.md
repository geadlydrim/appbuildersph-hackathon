# CommuteNity: Five-Minute Demo

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.3
**Owner:** Presenter (P4)
**Status:** Draft. Rewritten for the Makati-only, map-first scope ([D20](state.md#5-decisions)–[D28](state.md#5-decisions)).
**Last reconciled:** 2026-10-09
**Source brief:** [Idea](idea-commutenity.md) · **Product plan:** [PRD](prd-commutenity.md)

## 1. Story

Open on a New Arrival in Makati: no signal, a map, and two places to connect. Turn on airplane mode. Tap point A and point B, and the phone draws the best trip on real roads, with fares, minutes, and where to say "para". Show what other riders suggest. Start the trip and watch the phone follow along and tell the rider when to say "para". Then ask in words, check a vehicle, and close on why this only works locally.

This script describes the intended demo. Present only what works at the freeze, and label any unfinished tier or any simulated part. Say plainly what runs on the phone.

## 2. Timed Scenes

The phone is mirrored on the venue display. The hero trip is the Makati pair chosen under [A13](state.md#4-open-assumptions).

| Time | On screen | Script and action |
|---|---|---|
| 0:00–0:30 | Photo of a Makati street or terminal | "You just moved to Manila. You're in Makati, no signal, two places to connect, and strangers to ask. Commute knowledge here lives in people's heads, and you need it exactly when the cloud isn't there." |
| 0:30–0:45 | Turn on airplane mode, live | "Everything you'll see runs on this phone. The map is a file on it." Point to the offline badge. |
| 0:45–1:45 | The map. Tap point A, tap point B (or drag, or search a place). | The best trip appears, drawn on real roads: legs, fares, minutes, distance, walk time, "para sa ⟨landmark⟩", and a reason chip. "We worked out the road lines beforehand and stored them in the pack. The phone does the ranking. Every stop and fare comes from our verified pack." Tap a point outside Makati: "Not in my data." |
| 1:45–2:25 | Show alternatives (T1) | "Want other options?" Open the sheet: Algorithm and Community trips with vote counts, drawn on the map. "Riders know shortcuts. Here's one a rider shared." Vote 👍 offline; it saves on the phone and syncs later. "When there's signal, the phone refreshes its pack and map. When there isn't, it still works." |
| 2:25–3:25 | Start trip: tracking and the para alert (T2) | Tap "Start trip". The phone follows the position along the trip: on route, the current leg, distance to the para point. Then the alert: vibration, a notification, and a banner "Para na! ⟨stop⟩". **If the route is replayed from a mock-location track, say "This is a simulated route".** "GPS works without data. The phone matches it to the stored road line. Your GPS track never leaves the phone and is gone when the trip ends." |
| 3:25–4:05 | Ask in words, check the vehicle, voice (T3) | Type or say "Paano pumunta sa ⟨destination⟩ galing ⟨origin⟩?" and the pins are set. Then "Is this the correct vehicle?" with the signboard text: "No, look for '⟨signboard⟩'", then a matching one: "Yes, ride this." Voice: show the transcript for confirmation. "The on-device model reads the question. A strict text match decides the answer. The model can't say 'ride this' on its own." Skip any part that isn't shipped. |
| 4:05–4:20 | The trained ranker (T4, only if shipped) | "We trained a ranker on which trips riders prefer. On held-out trips it agrees with riders X% of the time, versus Y% for our hand-tuned rules." Use real numbers, and name the data as team-generated. |
| 4:20–4:45 | Why local | Four reasons: no signal, no load, seconds to decide, private trips. "A cloud version fails at exactly the moment you need it. GPS tracking needs no data, and your track stays on the phone. The network is used only to refresh, to share community suggestions, and, if you ask, to fetch a walking route or search a place." |
| 4:45–5:00 | Close | What runs on the phone, and what's next: more areas beyond Makati and real riders' contributions. |

**Total:** 300 seconds. Rehearse with real latency. Cut order if time runs short: the ranker scene (4:05), then voice, then the rest of the T3 scene. Don't cut the airplane-mode, trip, or tracking scenes.

## 3. Demo Preparation and Fallback

- **Preparation:**
  - Commute pack and map pack refreshed on Wi-Fi beforehand, then tested in airplane mode on the demo phone and a spare phone.
  - Hero A/B points known and rehearsed; one out-of-Makati point ready.
  - Mock-location app selected and the hero-trip GPX track loaded ([QAD §2](qad-commutenity.md#2-data-and-environment)); location and notification permissions granted; the screen-off case tried once.
  - Mirroring tested at the 12:15 PM AV check.
  - Second phone already synced with a community suggestion.
  - If T3 ships: models pre-downloaded, 3 covered questions and 1 out-of-coverage rehearsed, and the signboard texts for the vehicle check written down.
- **Fallback:**
  - If tracking isn't passing, **demo with the recorded mock-GPS route and say "simulated route"**. The same applies if the live track misbehaves on stage.
  - If refresh or sync fails, say "it refreshes and syncs when online", use the bundled pack, and show the local unsynced marker.
  - If phrasing is slow, the template answer still shows.
  - If mirroring fails, use the recorded clip, **labelled as recorded**.
  - Never present a recording or a simulated route as live.

## 4. Judging Map

| Criterion | Weight | Proof in the demo |
|---|---:|---|
| Problem & Usefulness | 25% | The Makati story; New Arrival; a best trip on real roads, explained; a para alert; community alternatives |
| Local AI Implementation | 25% | Airplane mode on stage; offline map and on-phone tracking; whichever of the on-device LLM, speech model, and ranker shipped; the four "why local" reasons. **Claim only what shipped.** The MVP alone has no learned model ([SCRUTINY FC-18](scrutiny-commutenity.md#2-claim--reference-audit)). |
| Technical Execution | 20% | Works live; deterministic routing and map-matching explain why it's reliable; the simulated route is labelled |
| Innovation | 15% | A map-first trip builder that works offline, plus tracking with a para alert, plus local-first community knowledge, plus an on-device ranker if shipped |
| Product & Demo Quality | 15% | One clean flow on a real phone; honest states |

Rules source: [JUDGING](JUDGING.md#judging-criteria).

## 5. Readiness

**Gate: not passed.** No product exists yet. Readiness follows [QAD §6](qad-commutenity.md#6-release-criteria).

## 6. Questions

| Likely question | Answer |
|---|---|
| Why not a cloud model? | No signal at the moment of use, data costs money, latency at the curb, and privacy. The offline path makes zero network calls. |
| **Does tracking need data?** | No. GPS works without data. The phone matches each fix to the trip's stored road line and tells you if you're on route and when to say para. Everything runs on the phone, in airplane mode. |
| **Where does location go?** | Your GPS track goes nowhere. It's processed on the phone only, never uploaded, and discarded when the trip ends. A notification shows while tracking is on. One optional exception: if you tap "use my location" for point A while online, that single point is sent to our server to fetch a walking route. Offline, nothing is sent. |
| What if the model hallucinates a fare? | It can't add one. Fares come from the pack, and phrasing is fact-checked against it. |
| Can it say "ride this" wrongly? | The language model never decides. A strict text match against your trip's legs does, and it says "Not sure, check the signboard" when unsure. We report false "ride this" on held-out texts: ⟨real number⟩. |
| Does it read signboards with the camera? | No. You type or say the signboard text, and the app matches it. |
| Are those lines real roads? | We computed each route's road line beforehand from OpenStreetMap with ⟨routing engine, to confirm⟩ and stored it in the pack. It's an approximation of the vehicle's path, and a validator rejects straight-line shapes. Unverified shapes are marked as sample data. |
| How does the map work offline? | A Makati map file from OpenStreetMap data (Protomaps) loaded on the phone, drawn by MapLibre. OpenStreetMap attribution is on the map. |
| Isn't community data from your own team? | Yes. During the event all contributions and preference labels are team-generated, and we disclose that. The pipeline is built for real riders. |
| Is this real data? | The Makati hero trip uses data we collected or already knew. The wider coverage is sample data, marked in the app and listed in the README. |
| Why only Makati? | Accuracy over coverage. We'd rather get one city's trips right and drawn on real roads. |
| Can bad actors push wrong routes? | Suggestions must be valid pack legs, fares are recomputed from the pack, vote influence is bounded, and there's a rate limit per device. |
| How big is the download? | Pack ⟨measured⟩ and map pack ⟨measured⟩, once on Wi-Fi. |
| How accurate is it? | ⟨real eval numbers and sample sizes; tracking tested on N mock tracks and M real trips⟩ |
| Did you build on an existing project? | The idea, data shape, and the pin-builder concepts from our earlier CommuteNity work, which we disclose. All code was written during the hackathon. |
| Why Android? | It's a travel app, so it's used on the phone. Android dominates in the Philippines *(verify before saying)*. A browser version comes later. |

## 7. Submission Answer Draft

**Why does this product benefit from running AI locally?**

Commuters need help exactly where connectivity fails: underground stations, packed terminals, a prepaid SIM with no load left. They need it within seconds, before the jeep leaves. CommuteNity puts the whole trip on the phone: an offline Makati map, a local commute pack, trip ranking, GPS tracking with a "para" alert, and ⟨if shipped: a language model for questions in Taglish and English, on-device speech-to-text, and a route ranker trained on rider preferences⟩. It answers instantly with no signal, costs nothing per question, and never sends your questions, voice, or GPS track to a server. The track is processed on the phone and discarded when the trip ends. The phone refreshes its pack and map, fetches optional walking routes, and syncs community route suggestions, only when you're back online.
