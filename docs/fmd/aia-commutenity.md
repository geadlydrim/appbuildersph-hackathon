# AI Assurance Dossier: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Project owner
**Status:** Draft. Updated for the Makati-only, map-first scope. OCR is dropped ([D26](state.md#5-decisions)). The MVP includes the PRD-F8 on-device LLM ([D31](state.md#5-decisions)).
**Last reconciled:** 2026-10-09
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md) · **QAD:** [QA plan](qad-commutenity.md) · **CLR:** [Protection register](clr-commutenity.md)

## 0. Scope

AI and AI-adjacent logic ship in these features:

| Feature | Role | Model? |
|---|---|---|
| PRD-F1 | Place search over pack places: alias and fuzzy matching first; embeddings only if [A4](state.md#4-open-assumptions) keeps them | Maybe (embedding) |
| PRD-F2 | Best trip: deterministic candidates and the D16 lexicographic order | No |
| PRD-F7 | Tracking: map-matching, on/off-route, and the para alert, all deterministic geometry on GPS fixes | No |
| PRD-F8 | The LLM extracts places and the vehicle text from a free-form question and phrases a grounded answer; the correct-vehicle verdict comes from a deterministic text matcher ([D27](state.md#5-decisions)) | Yes (MVP per [D31](state.md#5-decisions); T3 label) |
| PRD-F9 | Speech-to-text: on-device Whisper via whisper.cpp ([D26](state.md#5-decisions)) | Yes (T3 voice, optional) |
| PRD-F10 | Learned ranker ([D11](state.md#5-decisions), [D18](state.md#5-decisions)) | Yes (T4) |

The MVP (T0 + T1 + T2 + PRD-F8) includes one model: the F8 on-device LLM ([D31](state.md#5-decisions)). The T0 to T2 path itself needs no model. If the LLM speed test fails, the fallbacks stay on-device: a smaller model, then llama.cpp, and last the rule-based parser plus on-device embedding place search; never a cloud model. Voice (T3) and the ranker (T4) are optional, are cut before F8, and merge into the demo build only in tier order.

All inference runs on the phone. No model creates facts or makes a verdict on its own. The pack and deterministic code own routes, fares, minutes, road shapes, the tracking status, and the "ride this / wrong" decision. Models only parse, order, phrase, and transcribe.

This is an assurance plan, not an audit that has been run.

## 1. Model and System Card

| Field | Value |
|---|---|
| Intended use | Help a rider pick and understand a commute inside Makati City, offline: build a trip on a map, see it on real roads, and get on-route status and a para alert while riding |
| Intended user | New Arrival, Occasional Commuter, Daily Rider |
| Models | Open-weight LLM (PRD-F8, MVP per [D31](state.md#5-decisions)), speech-to-text (Whisper via whisper.cpp, T3 voice, optional), ranker (T4, optional), and a place embedding model only if A4 keeps it. **No OCR model and no TTS model.** Exact LLM IDs remain pending [A4](state.md#4-open-assumptions); the ranker plan is [D18](state.md#5-decisions). |
| Data provenance | Curated Makati pack with road shapes precomputed from OpenStreetMap through an open routing engine ([A15](state.md#4-open-assumptions)), plus team-generated preferences, contributions, and eval sets ([data plan](data-commutenity.md)) |
| Model-visible data | The user's question or transcript; structured candidates and features; typed or spoken signboard or route-name text. **GPS fixes and map coordinates never enter a model's context.** |
| Human oversight | The rider sees the reason for the pick, alternatives with their sources, the transcript before it is used, and the text the verdict was matched against. The app says "not in my data" or "Not sure, check the signboard" instead of guessing. |
| Prohibited uses | Safety-critical navigation, promises about real-time arrival, turn-by-turn navigation, acting as an official fare authority (fares are estimates), and relying on the para alert as the only cue for where to alight |
| Performance | Not measured. Targets in [QAD §6](qad-commutenity.md#6-release-criteria). |

**Limitations:**
- Part of the pack and of the training data is `mock` (synthetic). It is marked in the app, and any metric measured on mock data says nothing about real-world accuracy.
- Fares and routes change.
- Travel minutes are typical estimates made by the team.
- Coverage is Makati City only. A trip that starts or ends outside it gets "not in my data".
- Road shapes come from a routing engine's driving or foot profile. A real jeepney may take a different path, so the drawn line is an approximation of the route, not a survey.
- GPS accuracy drops between high-rises and under MRT-3 or EDSA, so on-route status and the para alert can be late, early, or wrong ([A16](state.md#4-open-assumptions)).
- The correct-vehicle check compares text the rider types or says. It cannot see the vehicle.
- Whisper's accuracy on Taglish is unmeasured.
- The ranker reflects four raters' preferences.
- A fluent answer is not necessarily a correct one.

## 2. Risk Register

| ID | Risk | Severity | Mitigation | Eval | Status |
|---|---|---|---|---|---|
| AIA-R1 | Invented route, stop, fare, or minutes | High | Facts come from the generator and pack; composer fact-check; template fallback | AI-01, AI-02 | Specified |
| AIA-R2 | **Correct-vehicle false "Yes, ride this"**: the rider boards the wrong vehicle because a wrong or misread text matched | High | The verdict comes only from a deterministic fuzzy match against the active trip's legs (`routes.signboards[]`, route name). The LLM only extracts the text and never decides. A conservative threshold, and ambiguous or unmatched text gives "Not sure, check the signboard". The matched text is shown with the verdict. | AI-05 (0 false "ride this"), QA-13 | Specified |
| AIA-R3 | Poisoned or spam contributions push a bad route to the top | Medium | Legs must be valid against the pack; facts always computed from the pack; vote influence bounded; rate limit per device | AI-07 | Specified |
| AIA-R4 | Ranker overfits to the team's preferences, or the claim is inflated | Medium (rules risk) | Pair-level held-out split; ship rule; disclose rater count and data origin | AI-06 | Specified |
| AIA-R5 | Wrong place resolution in search, or in LLM extraction of A and B | Medium | Ambiguity picker; confidence threshold; the rider can always re-place the pin on the map | AI-03, QA-01, QA-12 | Specified |
| AIA-R6 | Prompt injection through the question or a contribution note | Low (the model has no tools) | Schema-validated parser output; notes shown as plain text and never sent to the LLM | AI-04, AI-07 | Specified |
| AIA-R7 | Stale fares or minutes shown as current | Medium | "Data as of" date; estimate labels; the refresh time is shown | QA-02, QA-04 | Specified |
| AIA-R8 | **Wrong para alert or false off-route.** GPS drift in an urban canyon (Ayala CBD high-rises, under MRT-3 or EDSA) makes the alert fire early, late, or never, or flags a rider who is on route. The rider may alight at the wrong place or distrust the app. | High (physical-world guidance) | Off-route needs more than 100 m from the polyline for at least 30 s, and the alert fires about 300 m before the alight stop (proposed, config, tuned under [A16](state.md#4-open-assumptions)). The alert is one-shot. A "GPS signal lost" state shows instead of a stale position. The trip card always lists the para point and its landmark, so the alert is never the only cue. If the thresholds can't be tuned in time, the demo uses a labelled simulated route. | QA-10, QA-11 (mock GPS); A16 field test | Specified |
| AIA-R9 | **Location privacy.** GPS fixes are personal data. A leak, a log line, or a crash report would expose the rider's movements. | High | The GPS track is processed on the phone only, is never uploaded, and is discarded when the trip ends. It is not written to logs or events, and not put in a model's context. The one online exception is optional: when the phone is online and "use my location" sets Point A, that single point goes to our server with the foot-route request ([SDD §5](sdd-commutenity.md#5-security-and-privacy)), and the rider is told. The foreground-service notification shows that tracking is on, with a Stop action. See [CLR §1](clr-commutenity.md#1-processing-inventory). | QA-11; network inspector during a mock-GPS trip | Specified |
| AIA-R10 | Whisper mis-transcribes a place or vehicle text (T3 voice) | Medium | The transcript is shown for confirmation before use; typed input is always available; the transcript goes through the same resolver and matcher as typed text | QA-14, AI-03, AI-05 | Specified |
| AIA-R11 | A road shape is wrong, or the drawn route doesn't match the real path, so the rider sees a line where the vehicle doesn't go | Medium | Pack validator checks each shape against its stops and rejects straight-line shapes. The hero trip is checked against `collected` or `known` knowledge. Mock shapes are marked. | QA-16, QA-17 | Specified |

## 3. Audit Trail

| Stage | Evidence | State |
|---|---|---|
| Scoping | IDEA, MVP scope | Documented |
| Mapping | SDD §1, §3, §8 | Documented |
| Artifact collection | Model IDs and licenses; data sources; map and routing-engine terms; rater count | Pending |
| Testing | QAD §7, QA-10, QA-11, QA-13 | Not run |

## 4. Safety Bars

| Flag | Present? | Bar | Met? |
|---|---|---|---|
| Consequential decision about a person | No | — | N/A |
| Physical-world guidance (which vehicle to board, where to alight) | Yes | Facts only from the pack; 0 false "Yes, ride this" verdicts on eval (AI-05); the para alert is never the only cue; a tracking failure shows a visible state | Pending |
| Untrusted input reaches a model or the ranker | Yes (questions, contributions) | No tools; validated output; bounded vote influence | Pending |
| Personal data in model context | Only the user's own question or transcript, kept on the device | Nothing on the answer path leaves the device; GPS is never in a model's context | Pending |
| Location data | Yes (tracking, "use my location") | The track is processed on the device only, never uploaded, and discarded after the trip. The only online exception is a "use my location" Point A in an optional foot-route request, disclosed to the rider. Tracking is disclosed in the notification. | Pending |

**If rehearsal produces an invented fact or a false "Yes, ride this":** disable that path (template-only answers, baseline scorer, or voice off) and fix it before re-tagging. F8 itself is not removed: it reverts to template phrasing and the deterministic matcher ([D31](state.md#5-decisions)).

**If rehearsal produces a wrong para alert or a false off-route:** widen or tune the thresholds. If it still misfires, demo tracking with a recorded mock-GPS route, labelled as simulated, and say so.

## 5. Regulatory Awareness

No AI regulatory tier or certification is claimed. This is a hackathon demo with no deployment. Location handling is covered in [CLR §2](clr-commutenity.md#2-regulatory-awareness).
