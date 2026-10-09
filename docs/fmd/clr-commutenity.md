# Data and User Protection Register: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md)

## 0. Scope

This register covers a local hackathon demo on Android phones. The privacy story is local processing: questions, voice, and camera images never leave the phone.

Two exposures remain:
- **Community contributions.** Route suggestions, votes, and notes sync through an authenticated backend function under an anonymous Auth identity.
- **Team data collection in public spaces.** Signboard photos.

## 1. Processing Inventory

| Activity | Data | Where | Retention | Notes |
|---|---|---|---|---|
| Trip question | Typed text | Phone memory | Session only | Never uploaded |
| Voice (T4) | Audio and transcript | Phone memory | Discarded after transcription | Never stored or uploaded |
| Signboard check (T3) | Camera frame | Phone memory | Discarded after the verdict | Never stored or uploaded |
| Local events (PRD §5.6) | Latencies and flags | Phone | Until cleared | No question text |
| Contributions | Suggested legs (pack IDs), note, vote, anonymous Auth UID, timestamp | Phone (Room) and Supabase sync backend | Event duration | No user-facing account, name, or location trace. Raw UIDs and votes are not exposed to readers. Notes are free text and could contain personal data, so the UI warns against it. |
| Team photo collection | Photos of public vehicles | Team machines; blurred photos only in the repo | Event duration plus the repo | Faces and plates blurred before commit |
| Team preference labels | Rankings per scenario per rater | Repo | Permanent | Raters shown as R1–R4, not by name |

No location tracking, payment data, or ad tracking.

## 2. Regulatory Awareness

The Philippine Data Privacy Act (RA 10173) covers processing of personal information. To stay clear of it we:
- keep answer-path data on the phone
- design contributions to carry no personal information (random UUID, structured legs, a short note with a warning)
- minimise and blur photos

No legal conclusion is drawn beyond this.

## 3. Protection Bars

| Flag | Present? | Protection | Status |
|---|---|---|---|
| Answer-path data leaving the phone | No | No network calls on the ask, answer, alternatives, scan, or voice paths | QA-01 pending |
| Contribution data leaving the phone | Yes | Anonymous Auth identity; notes ≤ 280 characters with a no-personal-info hint; authenticated Edge Function validates writes and rate-limits server-side; readers receive aggregates only | QA-10, AI-07 pending |
| People captured incidentally in photos | Yes (collection) | Frame on the signboard; blur faces and plates; keep raw photos out of the public repo | Pending |
| Children's or sensitive data | No | — | N/A |

## 4. Terms Readiness

There are no terms and no privacy policy because nothing is distributed. The README states what runs locally and what needs the internet (model download, contribution sync), as the [submission rules](JUDGING.md#submission) require.

## 5. IP, Provenance and Disclosure

| Item | Treatment |
|---|---|
| Pre-existing CommuteNity work | **Disclose** in the README. Reused: the product idea, personas, the route → segment → stop data shape, the route-picker and community-contribution concepts, and the "Jeepney Gold" visual direction. No code reused, including the earlier Compose scaffold. The repo is fresh ([D3](state.md#5-decisions)). |
| Team-generated and mock data | Disclose that the preferences, contributions, and eval sets were created by the team during the event. List which pack records and labels are `mock` (counts per class). |
| Model weights | Record each license (e.g., Apache-2.0, Gemma terms, Llama license) and comply with attribution |
| Runtimes and libraries | Record licenses (MediaPipe, llama.cpp, ONNX Runtime, ML Kit, whisper.cpp, as used) |
| OpenStreetMap coordinates | ODbL attribution: "© OpenStreetMap contributors" |
| Fare data | Cite source and date |
| Fonts and icons | Bundled; licenses recorded |
| AI dev tools | Listed in the README disclosures |
| Name | "CommuteNity" is not trademark-cleared |

## 6. Platform Distribution

None. The APK is built from the public repo. There is no Play Store listing.

## 7. Posture and Revisit Register

| ID | Topic | Treatment now | Revisit trigger | Safe fallback |
|---|---|---|---|---|
| CLR-P1 | Collecting photos in public spaces | Minimise and blur; raw photos stay off the public repo | Publishing any dataset | Keep raw data off the repo |
| CLR-P2 | Contribution data | Anonymous UUID; no accounts | Real users, accounts, or moderation | Local-only contributions |
| CLR-P3 | Telemetry | Nothing leaves the phone except contributions | Any analytics proposal | On-device logs only |
| CLR-P4 | Full social layer (parked) | Not built | Un-parking it | — |
