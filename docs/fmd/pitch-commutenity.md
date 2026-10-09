# CommuteNity: Five-Minute Demo

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Presenter (P4)
**Status:** Draft
**Last reconciled:** 2026-10-09
**Source brief:** [Idea](idea-commutenity.md) · **Product plan:** [PRD](prd-commutenity.md)

## 1. Story

Open on a New Arrival at a terminal: no signal, three jeeps, one chance. Show the phone in airplane mode picking the route locals actually take. Then show where that knowledge comes from: other riders, and a ranker trained on what riders prefer. Close on why this only works locally.

This script describes the intended demo. Present only what works at the freeze, and label any unfinished tier.

## 2. Timed Scenes

The phone is mirrored on the venue display.

| Time | On screen | Script and action |
|---|---|---|
| 0:00–0:35 | Photo of a jeepney terminal | "You just moved to Manila. You're at a terminal, no signal, three jeeps slowing down. Which one? Commute knowledge here lives in people's heads, and you need it exactly when the cloud isn't there." |
| 0:35–0:50 | Turn on airplane mode, live | "Everything you'll see runs on this phone." Point to the offline badge. |
| 0:50–2:00 | Ask a Taglish question | "Paano pumunta sa ⟨destination⟩ galing ⟨origin⟩?" The best-route card appears: legs, fares, minutes, "para sa ⟨landmark⟩", and a reason chip. "An on-device model understood my Taglish. Every stop and fare comes from our verified pack, not from the model's imagination." |
| 2:00–2:50 | Show alternatives | "Want other options?" Open the sheet: Algorithm and Community routes with vote counts. "Riders know shortcuts. Here's one a rider shared." Vote 👍 offline; it saves on the phone and syncs later. |
| 2:50–3:30 | The trained ranker (if T2 shipped) | "We trained a ranker on which routes riders prefer, and it runs on the phone. On held-out trips it agrees with riders X% of the time, versus Y% for our hand-tuned rules." Use real numbers, and name the data as team-generated. |
| 3:30–4:00 | Signboard (if T3 shipped), or skip | Scan a signboard photo: "ride this", then a wrong one: "wrong jeep". "Your camera image never leaves the phone." |
| 4:00–4:40 | Why local | Four reasons: no signal, no load, seconds to decide, private trips. "A cloud version fails at exactly the moment you need it. The community part uses the network only when you have it." |
| 4:40–5:00 | Close | What runs on the phone, and what's next: more corridors and real riders' contributions. |

**Total:** 300 seconds. Rehearse with real latency. If time runs short, cut the 3:30 scene first.

## 3. Demo Preparation and Fallback

- **Preparation:**
  - Models pre-downloaded on the demo phone and a spare phone.
  - Mirroring tested at the 12:15 PM AV check.
  - Second phone already synced with a community suggestion.
  - Rehearsed questions: 3 covered and 1 out-of-coverage.
  - Printed signboard photos.
- **Fallback:**
  - If phrasing is slow, the template answer still shows.
  - If sync fails, say "it syncs when online" and show the local unsynced marker.
  - If mirroring fails, use the recorded clip, **labelled as recorded**.
  - Never present a recording as live.

## 4. Judging Map

| Criterion | Weight | Proof in the demo |
|---|---:|---|
| Problem & Usefulness | 25% | Terminal story; New Arrival; correct, explained picks; community alternatives |
| Local AI Implementation | 25% | Airplane mode on stage; on-phone LLM, embeddings, ranker, OCR, and speech; the four "why local" reasons |
| Technical Execution | 20% | Works live; deterministic routing explains why it's reliable |
| Innovation | 15% | An on-device ranker learned from rider preferences, plus local-first community knowledge |
| Product & Demo Quality | 15% | One clean flow on a real phone; honest states |

Rules source: [JUDGING](JUDGING.md#judging-criteria).

## 5. Readiness

**Gate: not passed.** No product exists yet. Readiness follows [QAD §6](qad-commutenity.md#6-release-criteria).

## 6. Questions

| Likely question | Answer |
|---|---|
| Why not a cloud model? | No signal at the moment of use, data costs money, latency at the curb, and privacy. The answer path makes zero network calls. |
| What if the model hallucinates a fare? | It can't add one. Fares come from the pack, and phrasing is fact-checked against it. |
| Isn't community data from your own team? | Yes. During the event all contributions and preference labels are team-generated, and we disclose that. The pipeline is built for real riders. |
| Is this real data? | The demo corridors use data we collected or already knew. The wider coverage is sample data, marked in the app and listed in the README. |
| Can bad actors push wrong routes? | Suggestions must be valid pack legs, fares are recomputed from the pack, vote influence is bounded, and there's a rate limit per device. |
| How big is the download? | ⟨measured⟩ once on Wi-Fi. |
| How accurate is it? | ⟨real eval numbers and sample sizes⟩ |
| Did you build on an existing project? | The idea, data shape, and route-picker concept come from our earlier CommuteNity concept, which we disclose. All code was written during the hackathon. |
| Why Android? | It's a travel app, so it's used on the phone. Android dominates in the Philippines *(verify before saying)*. A browser version comes later. |

## 7. Submission Answer Draft

**Why does this product benefit from running AI locally?**

Commuters need help exactly where connectivity fails: underground stations, packed terminals, a prepaid SIM with no load left. They need it within seconds, before the jeep leaves. CommuteNity runs its language model, place embeddings, route ranker, OCR, and speech models on the phone against a local commute pack. It answers instantly with no signal, costs nothing per question, and never sends your daily routes, voice, or camera images to a server. Community route suggestions sync only when you're back online, and the ranker that orders them runs on the device.
