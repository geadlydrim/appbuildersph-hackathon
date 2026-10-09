# Idea Brief: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**Context:** AppBuildersPH Hackathon 2026, theme **Local AI** ([JUDGING](JUDGING.md))

## 1. The Spark

**One-line pitch:** CommuteNity is an Android commute assistant for **Makati City** that runs on the phone. Pick where you are and where you're going on a map, and it picks the most efficient trip: which jeep, train, or UV to take, where to say "para", and roughly what it costs. It draws the trip on real roads, works with no signal or load, and alerts you before your stop. When you want other options, it shows alternatives from the routing algorithm and from trips other riders have shared.

**Problem:** commuting in Metro Manila depends on knowledge that nobody wrote down. Jeepney, UV Express, and tricycle routes mostly live in the heads of regular riders. Fares change, and signboards are hard to read on a moving jeep. The rider who most needs help often can't get it online when it matters:

- **No signal where it matters:** underground and elevated stations, tunnels, packed terminals.
- **Data costs money:** many riders are on prepaid load, and cloud AI queries or map downloads use it up.
- **Decisions take seconds:** the jeep slows down, you read the sign, you decide. A round trip to the cloud loses the jeep.
- **Trip patterns are private:** home, work, and school are where you go every day. Asking a cloud API about each trip leaks them.

The best route also isn't always the "official" one. Riders know shortcuts and cheaper combinations, and those should improve what the app suggests.

**Insight (why local, why now):** a Makati vector-tile extract and a curated commute pack, with road shapes precomputed ahead of time, are small enough to live on a mid-range Android phone. Trip computation and GPS tracking need no network, and small LLM and speech models now run on-device too. Together they make an assistant that never needs the network when you're actually traveling. It still refreshes its data and learns from community contributions whenever the phone is online. That fits the challenge exactly: *"remains genuinely useful when the cloud disappears."*

**Riskiest assumption:** the offline map renders on the demo phone, the precomputed road shapes make a trip that looks right on real roads, the pack plus contributions produce a trip that riders would actually agree is the best one, and the on-device LLM (PRD-F8, release-critical per [D31](state.md#5-decisions)) runs fast enough on the demo phone to turn a typed question into point A and B. This has to hold reliably enough for a live demo ([SCRUTINY §4](scrutiny-commutenity.md#4-assumption-stress-test), [A13–A16](state.md#4-open-assumptions)).

## 2. Who It's For

**Primary user: the New Arrival.** A first-year student from the province, an OFW returnee, or a provincial transplant. They have a thin mental map of the city, a prepaid SIM and a mid-range Android phone, and they need to get somewhere unfamiliar in Makati.

**Secondary users:**
- The **Occasional Commuter**, who drives most days, takes transit when forced, and has no patience for jargon.
- The **Daily Rider**, who knows their own corridor but not this trip. They are also the most likely contributor: they suggest the shortcut and vote on what worked.

**Their moment of pain:** at a terminal or station with no signal, unsure which jeep to take, whether the one slowing down is the right one, or when to get off.

**Success in their words** (intended, not an interview quote): "I put my pin on where I'm going. It picked the way locals go, showed it on the road, buzzed me before my stop, and didn't need data."

## 3. Scope & Cut Line

The full tiering is in [MVP scope](mvp-scope.md).

| Capability | Core to the one thing? |
|---|---|
| Makati commute pack with road shapes, and an offline Makati map | Yes, T0 |
| Map trip builder: A/B pins, best trip computed on the phone and drawn on real roads | Yes, T0 |
| Online refresh of pack, map, and community data | T1 |
| Alternatives on request (algorithm and community) | T1 |
| Suggest a trip or vote "this worked"; sync when online | T1 |
| In-trip tracking: on-route status and the para alert (offline GPS) | T2 |
| Ask in words and the correct-vehicle text check, through an on-device LLM (PRD-F8) | Release-critical, in the MVP ([D31](state.md#5-decisions)); T3 label |
| Voice (speech-to-text) | T3 voice, optional; cut before F8 |
| On-device ranker trained on preferences and votes | T4 upgrade; never blocks |
| Full social (feed, comments, profiles, accounts) | Parked |
| OCR signboard scan, text-to-speech, coverage beyond Makati | Parked / Won't this event |
| Browser app | After the event |

**If we only ship one thing:** the phone is in airplane mode and someone sets two pins on the Makati map. The app automatically picks the best trip and draws it on real roads, with the legs, the boarding and alighting points, the fares, the walk time, and where to say "para". Everything comes from the pack and map on the phone.

**The MVP adds one more thing:** the rider can also ask in words, for example "How to get from V.A. Rufino St to Pio del Pilar?" (the hero pair, [D30](state.md#5-decisions), amended by [D37](state.md#5-decisions)), and an on-device LLM sets the two pins. This is what makes the submission run meaningful local AI inference ([D31](state.md#5-decisions)).

**Explicitly out of scope:** accounts, social feed, comments, turn-by-turn navigation, real-time vehicle locations, ride-hailing, payments, coverage beyond Makati, OCR signboard scanning, text-to-speech, iOS.

## 4. Success & Judging Criteria

Success means:
- The offline demo works live on the phone, in airplane mode.
- Every fact in an answer traces back to a pack or contribution record, and the drawn route follows a stored road shape.
- The trip pick is explainable ("fewest transfers, ₱X, about N min").
- During a trip the app tells the rider whether they're still on route and raises the para alert at the threshold, with no data.
- We can show why local beats cloud: no signal, no data cost, decisions in seconds, and trips that stay private.

The rubric mapping is in [PITCH §4](pitch-commutenity.md#4-judging-map).

## 5. Concept Visuals

None yet. Interaction and visual direction are in [DSD](dsd-commutenity.md).

## 6. Open Questions

Tracked in [state §4](state.md#4-open-assumptions) and resolved through wayfinder.
