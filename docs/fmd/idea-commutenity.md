# Idea Brief: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**Context:** AppBuildersPH Hackathon 2026, theme **Local AI** ([JUDGING](JUDGING.md))

## 1. The Spark

**One-line pitch:** CommuteNity is an Android commute assistant for Metro Manila that runs its AI on the phone. Ask in Taglish and it picks the most efficient route: which jeep, train, or UV to take, where to say "para", and roughly what it costs. It works with no signal or load. When you want other options, it shows alternatives from the routing algorithm and from routes other riders have shared.

**Problem:** commuting in Metro Manila depends on knowledge that nobody wrote down. Jeepney, UV Express, and tricycle routes mostly live in the heads of regular riders. Fares change, and signboards are hard to read on a moving jeep. The rider who most needs help often can't get it online when it matters:

- **No signal where it matters:** underground and elevated stations, tunnels, packed terminals.
- **Data costs money:** many riders are on prepaid load, and cloud AI queries or map downloads use it up.
- **Decisions take seconds:** the jeep slows down, you read the sign, you decide. A round trip to the cloud loses the jeep.
- **Trip patterns are private:** home, work, and school are where you go every day. Asking a cloud API about each trip leaks them.

The best route also isn't always the "official" one. Riders know shortcuts and cheaper combinations, and those should improve what the app suggests.

**Insight (why local, why now):** small LLMs, embedding models, OCR, and speech models now run on mid-range Android phones. A curated commute pack is a few megabytes. Together they make an assistant that never needs the network when you're actually traveling. It still gets smarter from community contributions whenever the phone is online. That fits the challenge exactly: *"remains genuinely useful when the cloud disappears."*

**Riskiest assumption:** a small on-phone model can turn a messy Taglish question into the right origin and destination, and the pack plus contributions produce a route that riders would actually agree is the best one. This has to hold reliably enough for a live demo ([SCRUTINY §4](scrutiny-commutenity.md#4-assumption-stress-test)).

## 2. Who It's For

**Primary user: the New Arrival.** A first-year student from the province, an OFW returnee, or a provincial transplant. They have a thin mental map of the city, a prepaid SIM and a mid-range Android phone, and they need to get somewhere unfamiliar.

**Secondary users:**
- The **Occasional Commuter**, who drives most days, takes transit when forced, and has no patience for jargon.
- The **Daily Rider**, who knows their own corridor but not this trip. They are also the most likely contributor: they suggest the shortcut and vote on what worked.

**Their moment of pain:** at a terminal or station with no signal, unsure which jeep to take, or whether the one slowing down is the right one.

**Success in their words** (intended, not an interview quote): "I asked in my own words. It picked the route locals actually take, told me where to say *para*, and didn't need data."

## 3. Scope & Cut Line

The full tiering is in [MVP scope](mvp-scope.md).

| Capability | Core to the one thing? |
|---|---|
| Curated commute pack on the phone | Yes |
| Offline Taglish question → auto-picked most efficient route, with a grounded answer | Yes |
| Alternatives on request (algorithm and community) | Yes, T1 |
| Suggest a route or vote "this worked"; sync when online | Yes, T1 |
| On-device ranker trained on preferences and votes | T2 upgrade; never blocks |
| Signboard check | T3 |
| Voice question | T4 |
| Full social (feed, comments, profiles, accounts) | Parked |
| Browser app | After the event |

**If we only ship one thing:** the phone is in airplane mode and someone types a Taglish trip question for a covered corridor. The app automatically picks the best route and shows the legs, the boarding and alighting points, and the fares. Everything comes from the pack on the phone, and every answer is produced by on-device AI.

**Explicitly out of scope:** accounts, social feed, comments, live GPS navigation, real-time vehicle locations, ride-hailing, payments, nationwide coverage, iOS.

## 4. Success & Judging Criteria

Success means:
- The offline demo works live on the phone.
- Every fact in an answer traces back to a pack or contribution record.
- The route pick is explainable ("fewest transfers, ₱X, about N min").
- We can show why local beats cloud: no signal, no data cost, decisions in seconds, and trips that stay private.

The rubric mapping is in [PITCH §4](pitch-commutenity.md#4-judging-map).

## 5. Concept Visuals

None yet. Interaction and visual direction are in [DSD](dsd-commutenity.md).

## 6. Open Questions

Tracked in [state §4](state.md#4-open-assumptions) and resolved through wayfinder.
