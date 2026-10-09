# Data and User Protection Register: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Project owner
**Status:** Draft. Updated for the Makati-only, map-first scope, with in-trip GPS tracking ([D25](state.md#5-decisions)) and OCR dropped ([D26](state.md#5-decisions)).
**Last reconciled:** 2026-10-10
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md)

## 0. Scope

This register covers a local hackathon demo on Android phones. The privacy story is local processing: questions, voice, the active trip, and the GPS track never leave the phone.

Three exposures remain:
- **Community contributions.** Route suggestions, votes, and notes sync through an authenticated backend function under an anonymous Auth identity.
- **Optional online requests.** Only when the phone is online and only when needed, two requests leave the phone through our server with the anonymous credential ([SDD §5](sdd-commutenity.md#5-security-and-privacy)): the first/last-mile foot-route request, which sends the pin and stop coordinates (and the current location when "use my location" sets Point A), and online geocoding, which sends the typed search text. Both are optional and fail soft offline. The GPS track is never sent.
- **Team data collection in public spaces.** Signboard text, written down by hand. No photos are collected.

## 1. Processing Inventory

| Activity | Data | Where | Retention | Notes |
|---|---|---|---|---|
| Map pins (Point A / Point B) | Two coordinates the rider tapped, dragged, or searched | Phone memory | Session only | Not uploaded on the offline path |
| Trip question (T3) | Typed text | Phone memory | Session only | Never uploaded |
| Voice (T3) | Audio and transcript | Phone memory | Discarded after transcription | Never stored or uploaded |
| Correct-vehicle check (T3) | Signboard or route-name text, typed or spoken | Phone memory | Discarded after the verdict | Never uploaded |
| **GPS track (tracking, T2)** | Location fixes read by the foreground service while a trip is active. One fix when the rider taps "use my location". | Phone memory only | **The track is processed on the device, never uploaded, discarded when the trip ends** | Not written to logs or local events, not in model context, and not in crash reports. The foreground-service notification shows that tracking is on. The one exception is the Point A fix below. |
| Refresh (T1) | Requests for the latest commute pack, map pack, community suggestions, and vote aggregates | Phone and our server (Supabase Storage plus the sync Edge Function) | The files are cached until the next refresh | Requests carry no location and no question text. The server sees the network address and the anonymous Auth identity, as any request does. |
| Online requests (T0/T1) | (1) The pin and stop coordinates of a first/last-mile foot route. When "use my location" sets Point A, **that current location is one of them**. (2) The typed search text, for online geocoding. | Phone and our server; the routing engine if the server proxies one ([A15](state.md#4-open-assumptions)) | Foot routes are cached on the phone, local only. Server-side log retention is **not yet decided**. | Only when online and only when needed; never on the offline answer path or the tracking path. Both fail soft: offline or denied, the app draws the dashed "walk ~N m" line and uses pack places. Same anonymous credential and no service-role rule as sync. |
| Local events (PRD §5.6) | Latencies and flags | Phone | Until cleared | No question text and no coordinates |
| Contributions | Suggested legs (pack IDs), note, vote, anonymous Auth UID, timestamp | Phone (Room) and Supabase sync backend | Event duration | No user-facing account, name, or location trace. Raw UIDs and votes are not exposed to readers. Notes are free text and could contain personal data, so the UI warns against it. |
| Rider Q&A (T1, first cut, [D34](state.md#5-decisions)) | The rider's own questions and answers, and the key of a trip an answer is tied to | Phone only (memory or Room) | Until cleared or the app is reset | Never synced and never sent anywhere. No account, name, or profile. The bundled mock threads are hand-written samples marked Sample. The UI warns against typing personal data. |
| Team signboard text collection | Signboard and route-name text noted from public vehicles. No photos. | Team notes, then the pack and eval set in the repo | Event duration plus the repo | Text only; nothing that identifies a person or a plate |
| Team GPX test tracks | Recorded or hand-traced tracks along the hero trip | Repo (test fixtures) | Event duration plus the repo | A recorded track is a teammate's movement. Trim the start and end away from private places before committing. |
| Team preference labels | Rankings per scenario per rater | Repo | Permanent | Raters shown as R1–R4, not by name |

No payment data and no ad tracking. Location is used only for the active trip and for "use my location".

### 1.1 Location permission rationale

- **When asked:** at the first "use my location" tap or the first "Start trip", never at app launch. Notification permission (Android 13 and later) is asked at "Start trip" too.
- **Rider-facing text (proposed):** "CommuteNity uses your location during a trip to show whether you're still on route and to alert you before your stop. Your location track stays on this phone and is deleted when the trip ends." Next to "use my location": "When you're online, this point is sent to our server to fetch a walking route. Offline, nothing is sent."
- **If denied:** pins by tap, drag, and search still work, and the best trip is unchanged. Tracking and "use my location" show a clear unavailable message.
- **Foreground-service notification:** shows for the whole trip, says that CommuteNity is tracking the trip, and has a Stop action. It is the disclosure that tracking is on.
- **Permission set:** foreground location only, with a foreground service of location type. No background-location permission is planned. Whoever builds tracking confirms the final set at build and records it here.

## 2. Regulatory Awareness

The Philippine Data Privacy Act (RA 10173) covers processing of personal information. A location trace of a person's movements is personal information. To stay clear of it we:
- keep answer-path data and location on the phone
- never upload the GPS track, and discard it when the trip ends
- design contributions to carry no personal information (random UUID, structured legs, a short note with a warning)
- keep online requests to the foot-route endpoints and typed search text, disclose that "use my location" as Point A sends that one point when online, and decide server log retention before the demo
- keep signboard collection to text, with no photos of people or plates

No legal conclusion is drawn beyond this.

## 3. Protection Bars

| Flag | Present? | Protection | Status |
|---|---|---|---|
| Answer-path data leaving the phone | No | No network calls on the pins, best trip, alternatives, tracking, ask, or voice paths | QA-02, QA-10, QA-11 pending |
| GPS track leaving the phone | No | Fixes are never uploaded, never logged, never in model context, and discarded after the trip. Checked with a network inspector during a mock-GPS trip. | QA-10, QA-11 pending |
| Tracking without the rider knowing | No | Permission asked in context; foreground notification shown for the whole trip; a Stop action | QA-11, QA-18 pending |
| Online request data leaving the phone | Yes (optional, only when online) | Only the foot-route endpoints (including a "use my location" Point A) and typed search text; not the track; no account; fail soft; the README says so; server log retention to decide | Pending |
| Contribution data leaving the phone | Yes | Anonymous Auth identity; notes ≤ 280 characters with a no-personal-info hint; authenticated Edge Function validates writes and rate-limits server-side; readers receive aggregates only | QA-08, AI-07 pending |
| People captured incidentally in collection | No | Signboard text only; no photos | N/A |
| Children's or sensitive data | No | — | N/A |

## 4. Terms Readiness

There are no terms and no privacy policy because nothing is distributed. The README states what runs locally and what needs the internet (refresh of the pack, map pack, and community data; online place search and foot routes, which can carry a "use my location" Point A; the one-time model download for ask in words, which is in the MVP per [D31](state.md#5-decisions); contribution sync), as the [submission rules](JUDGING.md#submission) require. It also states that the GPS track stays on the phone.

## 5. IP, Provenance and Disclosure

| Item | Treatment |
|---|---|
| Pre-existing CommuteNity work | **Disclose** in the README. Reused: the product idea, personas, the route → segment → stop data shape, the community-contribution concept, the "Jeepney Gold" visual direction, and from CommuteNity-Web the *ideas* for the A/B pin trip builder, location search, and Philippine place labels ([D21](state.md#5-decisions)). No code reused, including the earlier Compose scaffold and the web app. The repo is fresh ([D3](state.md#5-decisions)). |
| Team-generated and mock data | Disclose that the preferences, contributions, and eval sets were created by the team during the event. List which pack records, shapes, and labels are `mock` (counts per class). |
| Model weights | Record each license (e.g., Apache-2.0, Gemma terms, Llama license, Whisper's license) and comply with attribution. Applies if T3 or T4 ships. |
| Runtimes and libraries | Record licenses (MapLibre Native Android, PMTiles support, MediaPipe, llama.cpp, ONNX Runtime, whisper.cpp, as used). ML Kit is no longer used. |
| **OpenStreetMap data and map attribution** | The map pack, the stop coordinates, and the road shapes all derive from OpenStreetMap, which is under the ODbL. Show "© OpenStreetMap contributors" visibly on the map screen, not behind a toggle. Protomaps documents its basemap as an ODbL Produced Work that needs OpenStreetMap attribution ([Protomaps basemap downloads](https://docs.protomaps.com/basemaps/downloads), read 2026-10-09). **To verify:** any separate Protomaps attribution request, the terms of the basemap style, fonts (glyphs), and sprites we bundle, and whether the stored road shapes count as a derivative database under the ODbL (the pack is published in the public repo). |
| **Map tiles source** | The map pack ([data plan §2.2](data-commutenity.md#22-offline-map-pack)) is a Makati extract from a Protomaps build (the `pmtiles extract` command; [A14](state.md#4-open-assumptions)), bundled or downloaded once from our own storage. Never fetch tiles from `tile.openstreetmap.org`: its policy forbids offline use and bulk download ([OSMF tile usage policy](https://operations.osmfoundation.org/policies/tiles/), read 2026-10-09). Protomaps asks users to host a copy rather than hotlink its downloads. |
| **Routing engine for road shapes** | Shapes are precomputed at data-build time through OSRM or GraphHopper ([A15](state.md#4-open-assumptions), [data plan §3.2](data-commutenity.md#32-road-shape-pipeline-a15)). The OSRM wiki says its public demo server is for reasonable, non-commercial use, at most 1 request per second, with no uptime guarantee ([OSRM demo server](https://github.com/Project-OSRM/osrm-backend/wiki/Demo-server), read 2026-10-09). **To verify before pack v0:** that this use fits the FOSSGIS usage policy, which profiles the demo server serves, GraphHopper's usage terms, and the rail geometry source. A self-hosted engine avoids third-party terms but still carries ODbL. The runtime foot-route lookup must not lean on a public demo server. Record the engine, version, and date in the README. |
| Fare data | Cite source and date |
| Fonts and icons | Bundled; licenses recorded |
| AI dev tools | Listed in the README disclosures |
| Name | "CommuteNity" is not trademark-cleared |

## 6. Platform Distribution

None. The APK is built from the public repo. There is no Play Store listing.

## 7. Posture and Revisit Register

| ID | Topic | Treatment now | Revisit trigger | Safe fallback |
|---|---|---|---|---|
| CLR-P1 | Collecting data in public spaces | Signboard text only, by hand. No photos, so nothing to blur. (Supersedes the photo collection plan, [D26](state.md#5-decisions).) | Any photo or video collection, or publishing a dataset | Keep raw data off the repo |
| CLR-P2 | Contribution data | Anonymous UUID; no accounts | Real users, accounts, or moderation | Local-only contributions |
| CLR-P3 | Telemetry | Nothing leaves the phone except contributions, refresh requests, and optional online lookups | Any analytics proposal | On-device logs only |
| CLR-P4 | Full social layer (parked) | Not built. Exception: rider Q&A as bounded trip evidence, saved on the phone only and never synced ([D34](state.md#5-decisions)) | Un-parking it | — |
| CLR-P5 | Location and tracking | On device only, never uploaded, discarded after the trip; notification discloses tracking | Any proposal to upload, store, or share a trace, or to track in the background | Turn tracking off and show the trip card only |
| CLR-P6 | Online requests (geocoding, foot routes) | Optional, only when online and needed. The foot-route request carries the pin and stop coordinates, including a "use my location" Point A; geocoding carries the typed text. The GPS track is never sent. | Server logs retained, a third-party routing or geocoding service added, or the track or any trace sent | Dashed "walk ~N m" line and pack-place search only |
| CLR-P7 | Map and routing-engine terms | OSM attribution on the map; own-hosted copy of the map pack; no OSM tile server; engine terms checked before pack v0 | A14 or A15 resolves, or a term is found to forbid our use | Switch to a provider that allows offline use, or a self-hosted engine |
