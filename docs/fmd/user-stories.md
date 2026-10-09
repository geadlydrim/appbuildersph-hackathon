# User Stories

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Status:** Draft. Re-scoped to Makati, map-first ([D20](state.md#5-decisions)–[D29](state.md#5-decisions)). US-11 and US-12 are release-critical for the submission and part of the MVP ([D31](state.md#5-decisions)); US-13 (voice) stays optional.

Format: *As a [persona], I want [capability] so that [outcome].* Acceptance criteria use Given/When/Then.

Related docs:
- Personas: [IDEA §2](idea-commutenity.md#2-who-its-for)
- Tiers: [MVP scope](mvp-scope.md#tiers)
- Feature IDs: [PRD §3](prd-commutenity.md#3-features-and-priorities)
- Test cases (`QA-*`, `AI-*`): [QAD §7](qad-commutenity.md#7-ai-evaluation) and the QAD scenarios

"Offline" in acceptance criteria means the demo phone is in airplane mode and the app makes no outbound network requests. GPS still works offline. Numeric thresholds marked *config* come from [D25](state.md#5-decisions), start as the proposed values, and are tuned in testing ([A16](state.md#4-open-assumptions)).

The v0.2 stories (question-first, including the OCR signboard story) are retired and the IDs reused with the meanings below. The older CommuteNity stories (US-100+ feed and auth; US-001–016 catalog) stay retired. The route-picker and contribution ideas come back in their thin form ([D2](state.md#5-decisions), [D21](state.md#5-decisions)).

---

## T0: Walking skeleton

### US-01: Pick A and B on the map, offline
**PRD-F1** · QA-01

> As a **New Arrival** in Makati, I want to set where I am and where I'm going on a map so that I get a trip without typing a question, signal, or load.

- Given the Makati map and pack are on the phone and the phone is offline, when I open the app, then the map renders with OSM attribution visible and makes no network requests.
- Given the map, when I tap, then the first tap sets **point A** and the next sets **point B**. I can drag either pin to move it.
- Given I type in the search box, when the phone is offline, then I see matching pack places and aliases, and choosing one places the pin on its stop. Given several places match, I pick; the app doesn't guess.
- Given I tap "use my location", when GPS has a fix (offline works), then A is set to my position. Given permission is denied or there is no fix yet, then I see why and A stays unset.
- Given the phone is online, when I search, then the server geocoder may add results beyond pack places. Offline, search covers pack places only.
- Given both pins are set, when I confirm, then the best trip is computed ([US-02](#us-02-see-the-best-trip-on-real-roads)). A pin outside Makati goes to [US-03](#us-03-know-when-its-not-in-my-data).

### US-02: See the best trip on real roads
**PRD-F2, PRD-F3** · QA-02, QA-17, QA-19

> As an **Occasional Commuter**, I want the trip drawn on real roads with its fare, time, distance, walk time, para point, and a one-line reason so that I trust the pick and don't miss my stop.

- Given pins A and B in Makati with a route between them, when the trip is computed offline, then I see the best trip: ordered legs, each with mode, where to board, where to alight (the para point), fare, and minutes. It appears within the [latency target](sdd-commutenity.md#7-non-functional-targets).
- The trip card also shows total fare, total minutes, distance, and walk time, plus the pick reason built from the scored features, for example "fewest transfers · ₱X · ~N min" ([D16](state.md#5-decisions)).
- Given the trip is displayed, then every transit leg is drawn on its stored road shape (rail follows track geometry). No straight line is drawn except the offline first/last-mile walk, which is dashed and labelled "walk ~N m" ([D22](state.md#5-decisions)).
- Given the phone is online, when the first or last mile starts at a tapped point, then the walking route is fetched through our server and cached. The cached route is used offline; without one, the dashed labelled line is shown.
- Given a leg has no fare or minutes in the pack, when the trip renders, then that value reads "unknown" and the total reads "unknown" too. No number is invented and no partial total is shown.
- Every fare, stop, minute, and distance shown matches a pack record ([AI-01](qad-commutenity.md#7-ai-evaluation)).
- Given any shown fact is a `mock` record (stop, fare, minutes, or shape), when the trip renders, then that value carries a "sample data" marker, and unmarked values are never `mock` ([D13](state.md#5-decisions)).

### US-03: Know when it's not in my data
**PRD-F2** · QA-03

> As a **Daily Rider** off the covered area, I want the app to admit when it doesn't know so that I don't trust a made-up trip.

- Given A or B is outside Makati, when I confirm, then I see "not in my data" and that only Makati is covered. No trip is produced.
- Given A and B are inside Makati but the pack has no route between them, when I confirm, then I see "no route in my data". No trip is invented.
- Given I asked an off-topic question in words (T3), when it is processed, then the app redirects me to trip questions and sets no pin.

## T1: Online refresh + community

### US-04: Refresh when online, then work offline
**PRD-F4** · QA-04, QA-09

> As a **New Arrival** on prepaid load, I want the app to fetch the latest data once on Wi-Fi so that later trips cost no data.

- Given the phone is online, when a refresh runs, then the app fetches and caches the latest pack (with road shapes), the Makati map file, community suggestions, and vote aggregates, and shows what is stored: version, size, and when it was last refreshed ([D24](state.md#5-decisions)).
- Given a refresh completed, when I switch to airplane mode and reopen the app, then US-01 and US-02 work with the refreshed data and no network requests.
- Given a download is interrupted, when I reopen the app, then it resumes or retries, keeps the last good pack and map, and doesn't get stuck in a broken state.
- Given a refresh fails or the phone is offline, then I see its status, the app keeps using the cached data, and the UI is never blocked.

### US-05: See alternatives
**PRD-F5** · QA-05

> As a **Daily Rider**, I want to tap "other trips" so that I can choose a cheaper, less crowded, or more familiar option than the auto pick.

- Given a best trip, when I tap "Show alternatives", then I see up to N other candidates. Each is ordered by [D16](state.md#5-decisions), shows its reason line, is labelled **Algorithm** or **Community** (with its vote count), and can be drawn on the map.
- Given no alternatives exist, when I tap, then I see "no other trips yet. Suggest one?".
- This works offline using everything refreshed and synced so far.

### US-06: Suggest a trip
**PRD-F6** · QA-06

> As a **Daily Rider** who knows a shortcut, I want to submit my trip for a pair of points so that other riders see it as an alternative.

- Given an A–B pair, when I build a trip from pack stops and routes leg by leg, with an optional note, and submit it, then it's saved on the phone right away, even offline, and marked "not yet synced".
- A submission that references unknown stops or breaks leg continuity is rejected with the reason.

### US-07: Vote
**PRD-F6** · QA-07

> As a **rider** who just took a trip, I want to mark it worked or didn't so that good trips rise for everyone.

- Given any shown trip (algorithm or community), when I vote up or down, then my vote is stored locally. Voting again the same way clears it.
- My vote influences ordering only through synced aggregate counts (the bounded tie-break in D16) and the ranker's features. It is never treated as fact for fares or stops.

### US-08: Sync between phones
**PRD-F6** · QA-08, QA-09

> As a **rider**, I want my contributions to upload and others' to download automatically when I have signal so that offline use never waits on the network.

- Given unsynced contributions and connectivity, when the app syncs, then they upload and are marked synced. Contributions from others download into the local overlay.
- Given phone A suggests a trip and syncs, when phone B syncs, then B shows that trip as a Community alternative for the same pair.
- Given sync fails, then nothing is lost. The app retries later and never blocks the map, the trip, or tracking.

### US-15: Read a sample question, and add one without logging in
**PRD-F12** · Mock only. Not in the current MVP cut. No login.

> As a **New Arrival**, I want to see how a question and an answer would look, with a place name on them, so that "doon sa kanto" can point at a place later.

- Every question and answer on this screen is mock data. The screen says "Sample data. Walang account at walang login." There is no account, no login, and no sync of other riders.
- Given the sample threads, when I open "Mga tanong", then I can read a question, its place name, and its answers. Each one is marked Sample.
- Given I type a question or an answer, when the text is empty, then it is not saved. When it has text, then it stays on this phone for the session and is still marked Sample.
- Given I vote up or down, voting the same way again clears it. A vote never changes a fare, a stop, or a pack route.
- A map reference is a place name. It is labeled sample, not verified. Missing map tiles do not hide the name.

## T2: In-trip tracking

### US-09: Am I still on the right route?
**PRD-F7** · QA-10

> As a **New Arrival** on a moving jeep, I want the app to tell me whether I'm still on my trip so that I notice a wrong turn or wrong vehicle early.

- Given an active trip and a GPS fix, when the phone is offline, then the app snaps the fix to the trip's polyline and shows the current leg and the distance and stops remaining to the para point ([D25](state.md#5-decisions)).
- Given my fix stays within the on-route distance of the polyline, then the status reads **On route**.
- Given my fix is more than the off-route distance (*config*, proposed 100 m) from the polyline for at least the off-route time (*config*, proposed 30 s), then the status changes to **Off route** with a visible banner.
- Given I return within the on-route distance, then the status goes back to **On route**.
- Tracking runs in a foreground service, so it continues with the screen off. It uses GPS and the stored shapes only, with no network.
- Given the demo uses a recorded or Android mock-location GPS track, then the screen labels it as simulated.

### US-10: When should I para?
**PRD-F7** · QA-11

> As a **New Arrival**, I want an alert before my stop so that I don't miss it or have to watch the map.

- Given an active trip, when I get within the para-alert distance (*config*, proposed ~300 m) of the alight stop, then the app vibrates, shows a heads-up notification, and shows an on-screen banner. The alert fires once per alight stop.
- Given the screen is off or the app is in the background, then the alert still fires.
- Given GPS is lost, then the screen says so and shows the last known position. It does not report **Off route** or fire an alert from a stale fix.
- Given GPS recovers after the alert threshold was passed but before the stop, then the alert fires once on recovery (proposed behaviour).
- No spoken (TTS) alert is planned ([D26](state.md#5-decisions)).

## T3: Ask in words + voice

US-11 and US-12 keep their T3 label but are **release-critical (MVP) per [D31](state.md#5-decisions)**: PRD-F8 merges behind its feature flag as soon as T0 is demo-safe, tagged `demo-safe-f8` when both pass. US-13 (voice) is optional and is cut before them.

### US-11: Ask "How to get from X to Y?" in words
**PRD-F8** · QA-12
*Release-critical (MVP), [D31](state.md#5-decisions).*

> As a **New Arrival** who'd rather type than tap, I want to ask in my own words so that the app sets A and B for me.

- Given the phone is offline, when I type "paano pumunta sa X galing Y" or "How to get from X to Y?" in Taglish or English, then the on-device LLM extracts the two places, and the app resolves them against pack places and sets pins A and B ([AI-03](qad-commutenity.md#7-ai-evaluation)).
- Given a place is ambiguous, when I ask, then I pick from the matching places. The app doesn't guess.
- Given a place isn't in the pack, then I see "not in my data" ([US-03](#us-03-know-when-its-not-in-my-data)).
- The LLM never chooses the trip; the normal US-02 computation does. Any phrasing it adds is grounded in the computed trip ([A3](state.md#4-open-assumptions)).

### US-12: Is this the correct vehicle?
**PRD-F8** · QA-13
*Release-critical (MVP), [D31](state.md#5-decisions).*

> As a **New Arrival** at a terminal, I want to type or say what's written on the jeep so that I know in seconds whether it's the right one.

- Given an active trip, when I type or say the signboard text or route name, then the app matches it against the trip's legs (`routes.signboards[]` and route name) with a deterministic fuzzy match ([D27](state.md#5-decisions)).
- The result is one of three: "Yes, ride this" (it matches a leg), "No, look for '<signboard>'", or "Not sure, check the signboard" (ambiguous or no clear match).
- Given a free-form question, the LLM only extracts the text to match. It never decides the verdict.
- On held-out signboard and route-name texts there are 0 false "ride this" verdicts ([AI-05](qad-commutenity.md#7-ai-evaluation)).
- Inference runs on the phone. There is no camera or image input.

### US-13: Ask by voice
**PRD-F9** · QA-14

> As an **Occasional Commuter** holding a handrail, I want to speak my question so that I don't have to type.

- Given microphone permission and an offline phone, when I speak a Taglish question, then on-device Whisper transcribes it and the transcript appears for me to confirm before it follows the US-11 or US-12 flow ([D26](state.md#5-decisions)).
- Given microphone permission is denied, then I see how to enable it and typing still works.
- Given silence or unintelligible audio, then I see "didn't catch that, try again". No guessed question is run.
- Audio is processed on the phone. It is never stored or sent.

## T4: Trained ranker

### US-14: Ranker learned from riders
**PRD-F10** · QA-15

> As the **team**, we want the trip order learned from preferences and votes so that it matches what riders actually choose better than our hand-set order does.

- Given the held-out `known` preference labels ([data plan §6](data-commutenity.md#6-evaluation-sets)), when we compare the trained ranker with the D16 deterministic order, then the ranker ships only if it is better on the agreed metric ([D18](state.md#5-decisions)).
- It runs on the phone, offline, and the Python and Kotlin implementations produce identical feature vectors and scores on a fixture. Its training data (team-generated, disclosed as such), method, and real results go in the README. No fabricated benchmarks ([JUDGING](JUDGING.md#rules)).
