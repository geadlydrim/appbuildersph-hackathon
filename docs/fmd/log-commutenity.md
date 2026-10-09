# Build Session Log (LOG)

**Project:** CommuteNity
**Project slug:** `commutenity`
**Session started:** 2026-10-09

Append-only. One row per meaningful action or decision. Never edit past rows.

---

## 1. Action log

| # | Date | Trigger / action | Docs written / updated | Verdict |
|---|---|---|---|---|
| 1 | 2026-10-09 | Drafted the Local AI proposal from the original CommuteNity docs and the hackathon briefing | proposal (since merged into IDEA) | — |
| 2 | 2026-10-09 | Owner decision: park the community layer; use an app-provided commute pack | proposal → IDEA, MVP scope | D2 |
| 3 | 2026-10-09 | Transcribed the official briefing PDF; removed the screenshots and PDF | JUDGING | — |
| 4 | 2026-10-09 | Set up agent skills: GitHub tracker on the public repo `geadlydrim/appbuildersph-hackathon`, default triage labels, `wayfinder:*` labels, single-context domain docs | AGENTS.md, docs/agents/* | D6 |
| 5 | 2026-10-09 | Owner decisions: fresh start, docs first, working product before ambition, data collection and training planned. Restructured docs into the full suite modelled on the reference format. Removed the reference folder and the superseded CommuteNity docs (product-vision, plan, backend-decision, data-model, design-baseline-prompt, proposal); their surviving content was merged into IDEA, the data plan, and DSD. | All docs | D3, D4, D5; A1–A8 opened |
| 6 | 2026-10-09 | Owner decisions: mobile first as native Android (Kotlin/Compose), browser second after the event; name CommuteNity; team of 4; auto route picker with alternatives from the algorithm and community; thin community layer (suggest and vote, local-first sync) back in scope; learned on-device route ranker as the training target; build order T0 pick → T1 alternatives + community → T2 ranker → T3 signboard → T4 voice. Docs revised to v0.2. | All docs, AGENTS.md | D2 amended; D8–D12; A1, A2, A5, A7 closed; A9–A12 opened |
| 7 | 2026-10-09 | Owner decision: mixed data is allowed (owner-reported organizer allowance). Every record is classed `collected`, `known`, or `mock`. Mock fills coverage and training gaps, is marked in the app and README, never drives the demo hero question or the ranker ship-rule eval, and metrics are reported per class. | state, data plan, mvp-scope, stories, PRD, SDD, DSD, QAD, AIA, CLR, VALIDATION, SCRUTINY, PITCH, BUILD, JUDGING, AGENTS.md | D13; A6, A10, A11 amended |
| 8 | 2026-10-09 | Moved the project doc suite into `docs/fmd/` for a cleaner layout. `docs/agents/` stays where it is because the skills expect that path. Paths updated in AGENTS.md, the index, and BUILD. | AGENTS.md, index, BUILD | — |
| 9 | 2026-10-09 | Charted the wayfinder map: issue #1 with 9 tickets, cutoff 7:00 PM. Added GLOSSARY.md (route = one line, trip = door-to-door). Research resolved the runtime question (LiteRT-LM replaces MediaPipe as the primary LLM runtime) and the fare-source question (rail fares `collected`, jeepney and bus `known` until the LTFRB guides are saved). Results are on the `research/*` branches. | state A4, SDD §8, BUILD §3, data plan §3 | A4 narrowed |
| 10 | 2026-10-09 | Owner decision: planning cutoff moved from 7:00 PM to 9:00 PM. Build checkpoints shifted about 1 h later (T0 ~1 AM, T1 ~4 AM, T2 ~6 AM, T3/T4 6–8 AM) and cut rules tightened to match. Feature freeze (8:00 AM) and code freeze (10:00 AM) unchanged. | team guide, BUILD §1, map #1, ticket defaults | — |
| 11 | 2026-10-09 | Closed the fourth-teammate ticket: the team is geadlydrim, pablo-pica, storms23, and Jrabara101 (invites pending for the last two). Role assignment split into a new ticket at the top of the map. | state D12, A8; team guide | D12 amended |
| 12 | 2026-10-09 | Closed the demo-phone ticket: the owner's POCO X6 Pro (Dimensity 8300-Ultra). The LLM speed-test ticket is now unblocked. | state D14 (A12 closed), team guide | D14 |

---

## 2. Friction

| # | Area | What happened | Fix |
|---|---|---|---|
| 1 | Rules | The briefing PDF lists the reveal at both 1:00 and 2:00 PM, and the Tutorials Dojo award layout is ambiguous | Noted in JUDGING; confirm in Telegram if it matters |
