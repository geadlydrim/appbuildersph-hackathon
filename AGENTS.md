# AGENTS.md

Native Android (Kotlin/Compose) on-device AI commute assistant for Metro Manila (AppBuildersPH Hackathon 2026, theme Local AI). Deadline and code freeze: 10:00 AM, Oct 10, 2026.

## Project docs

Read `docs/fmd/state.md` first: position, open assumptions (A*), decisions (D*). `docs/fmd/index.md` lists every doc and reading path. `docs/fmd/JUDGING.md` holds the hackathon rules; it wins over any project doc. Humans start with `docs/team-guide.md`.

Standing rules:

- **Docs first.** No product code until `docs/fmd/state.md` marks the decisions for the walking skeleton resolved.
- **Working product before ambition.** Build tiers T0→T4 in order (`docs/fmd/mvp-scope.md`). Data collection, ranker training, and sync never block the T0/T1 baseline.
- **Local inference is the core.** The answer path makes zero network requests. No cloud model answers, ranks, or reads anything. The network is used only for model download and contribution sync.
- **Code decides facts; models parse, rank, and phrase.** Routes, stops, fares, minutes, and signboard verdicts come from the commute pack via deterministic code (`docs/fmd/sdd-commutenity.md`). Community suggestions must be pack-valid; they only add and order candidates.
- **Never fabricate** benchmarks, fares, minutes, or model versions. Mock data is allowed, but only when tagged `source_class: mock`, marked in the UI, and disclosed. Never pass it off as real. Record real numbers and disclose models, data sources, tools, team-generated data, and reused CommuteNity ideas in the README.
- **Community is the thin layer only:** suggest a route and vote, with local-first sync. Don't build feeds, comments, profiles, or accounts.

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
