# AGENTS.md

Native Android (Kotlin/Compose) on-device, map-first commute assistant for Makati City (AppBuildersPH Hackathon 2026, theme Local AI). Deadline and code freeze: 10:00 AM, Oct 10, 2026.

## Project docs

Read `docs/fmd/state.md` first: position, open assumptions (A*), decisions (D*). `docs/fmd/index.md` lists every doc and reading path. `docs/fmd/JUDGING.md` holds the hackathon rules; it wins over any project doc. Humans start with `docs/team-guide.md`.

Standing rules:

- **Docs are the spec; building is now.** Planning is over (Makati re-scope, D20–D29). Build from `docs/fmd/prd-commutenity.md`, `user-stories.md`, and `sdd-commutenity.md`. A change that contradicts a decision in `state.md` needs a new decision row, not a silent deviation.
- **Working product before ambition.** Build tiers T0→T4 in order (`docs/fmd/mvp-scope.md`): T0 walking skeleton, T1 online refresh + community, T2 in-trip tracking, T3 ask in words + voice, T4 ranker. MVP = T0+T1+T2. Later tiers never block earlier ones.
- **Map-first, Makati only.** The primary input is the map trip builder: point A and point B. Cover Makati City only; nothing beyond it (D20, D21).
- **Road shapes are precomputed.** Route each pack route through an open routing engine at data-build time and store the polyline per segment. The phone draws stored shapes and runs no routing engine. The offline map is a Makati PMTiles extract. **Never bulk-download tiles** from tile.openstreetmap.org; show OSM attribution (D22, D23).
- **Local compute is the core.** Trip computation, ranking, tracking, and AI make zero network requests and work in airplane mode. The network is used only for refresh (pack, map, community, cached foot routes) and contribution sync (D24).
- **Tracking is offline GPS + map-matching.** A foreground service snaps GPS fixes to the active trip's polyline: on-route / off-route status and the para alert. It is not turn-by-turn navigation (D25).
- **Voice is speech-to-text only.** OCR signboard scanning and TTS are out. The correct-vehicle check is a deterministic text match; the LLM only extracts text (D26, D27).
- **Code decides facts; models parse and phrase.** Routes, stops, fares, minutes, shapes, and the correct-vehicle verdict come from the commute pack via deterministic code (`docs/fmd/sdd-commutenity.md`). Community suggestions must be pack-valid; they only add and order candidates.
- **Never fabricate** benchmarks, fares, minutes, or model versions. Mock data is allowed, but only when tagged `source_class: mock`, marked in the UI, and disclosed. Never pass it off as real. Record real numbers and disclose models, data sources, tools, team-generated data, and reused CommuteNity ideas in the README.
- **CommuteNity-Web is ideas only.** No code reused (D3). Don't bring back its feed, comments, profiles, accounts, or straight-line route drawing.
- **Community is the thin layer only:** suggest a trip and vote, with local-first sync. Don't build feeds, comments, profiles, or accounts.
- **Figma follows the docs.** The Figma file is updated from `docs/fmd/figma-brief-commutenity.md` and `docs/fmd/dsd-commutenity.md` via a Figma MCP agent. Docs are canonical; never change a doc to match Figma without a decision (D29).

## Agent skills

### Issue tracker

GitHub Issues on `geadlydrim/appbuildersph-hackathon` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: root `GLOSSARY.md` + `docs/adr/`. See `docs/agents/domain.md`.

### Repo skills

These skills are vendored in `.agents/skills/` so every teammate's agent uses the same versions:

| Skill | Use it for |
|---|---|
| `wayfinder` | Working the decision map (issue #1) |
| `grilling` + `domain-modeling` | Grilling tickets; updating `GLOSSARY.md` and ADRs |
| `research` | Research tickets (findings go on `research/<name>` branches) |
| `prototype` | The throwaway LLM spike |
| `to-tickets` | Turning decisions into build issues |
| `triage` | Labelling incoming issues |
| `commit` | Splitting work into Conventional Commits (run its script with `sh`, not `bash`, on Windows) |

The repo copy of `domain-modeling` writes `GLOSSARY.md` / `GLOSSARY-MAP.md` instead of upstream's `CONTEXT.md`, to match `docs/agents/domain.md`. When a skill and a `docs/agents/*.md` file disagree, the docs file wins.
