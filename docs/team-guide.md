# Team Guide: How We Work on CommuteNity

Read this once before you start. It takes about 10 minutes.

## 1. The big picture (plain English)

**What we're building.** CommuteNity is an Android app for **Makati City**. You set where you are (point A) and where you're going (point B) on a map. It picks the best trip: which jeep or train, where to say "para", and how much it costs, and draws it on real roads. It works **with no signal**, because the map, the commute data, and the road shapes are all stored on the phone and everything is computed there. When the phone is online it refreshes that data. While you ride, GPS tells you whether you're still on route and buzzes before your stop. Riders can also share their own trips and vote on what worked, and ask and answer questions that give small, bounded evidence for a trip (rider Q&A, saved on the phone only). You can also ask in words, for example "How to get from V.A. Rufino St to Pio del Pilar?": a language model that runs on the phone reads the question and sets A and B. That is part of the MVP. Asking by voice comes later, if there's time. We are **not** building turn-by-turn navigation, a signboard camera scanner, or spoken answers.

**Deadline.** The code freeze is **10:00 AM, Oct 10**. Whatever is on GitHub at 10:00 AM is what the judges see.

**How we work: decide first, then build. Planning is over: building is now.** We don't jump straight into code. A few decisions, made badly, would waste the whole night: which phone, which area, which map. Those are decided (see `docs/fmd/state.md`; the latest re-scope is D20–D29: Makati only, map-first). So we work in two phases:

1. **Planning (finished).** We answered a short list of open questions. Each question was a GitHub issue called a **ticket**, all hanging off one parent issue called the **map**. The planning sections below (§4) are kept for reference and for any ticket still open.
2. **Building (now, until the freeze).** Decisions become build tasks, also GitHub issues, and we build in fixed stages (**tiers**). Each tier must fully work before the next one starts, so we always have a working demo.

**Any ticket still open takes the default answer written at the bottom of the ticket.** We build anyway.

**The docs are the source of truth.** Everything we've decided is written in `docs/fmd/`. Start with `docs/fmd/state.md`: it says where we are, what's decided, and what's still open. `docs/fmd/` beats code, branches, PR text, and chat (D35). If code and docs disagree, the code changes, or a decision updates the doc first. Only the owner (`geadlydrim`) merges changes to decisions in `state.md`; you propose a change by issue or PR.

**No fixed roles (D35).** Work is broken into GitHub issues as we go. Anyone claims any issue by assigning themselves, and nobody is limited to one area. Checkpoints, tier order, and cut rules stay.

## 2. Words you'll see

| Word | Meaning |
|---|---|
| **Map** | GitHub issue [#1](https://github.com/geadlydrim/appbuildersph-hackathon/issues/1). The list of all planning tickets and every decision made so far. |
| **Ticket** | One open question, as a GitHub issue under the map |
| **Ready** | A ticket that's open, nobody is assigned to it, and it isn't waiting on another ticket |
| **Blocked** | A ticket that depends on another one. GitHub shows "Blocked by …" on it. |
| **Claim** | Assigning yourself to a ticket, so nobody else works on it at the same time |
| **Ticket type** | *grilling* = a discussion, where an AI asks questions and the humans decide. *task* = a checklist people do by hand. *prototype* = throwaway test code. *research* = an AI reads docs and reports back. |
| **Tier** | One build stage. T0 = set A and B on the offline map and get the best trip drawn on real roads. T1 = online refresh, alternatives, and community (suggest, vote, sync, and rider Q&A, which is the first thing cut). T2 = in-trip tracking (on-route status and the para alert). T3 = ask in words and voice. T4 = trained ranker. The MVP is T0 + T1 + T2 + ask in words (the on-device LLM, PRD-F8, kept in the MVP by D31). Voice and the ranker are optional. |
| **Source class** | Where a piece of data came from. `collected` = we verified it today. `known` = we know it from experience. `mock` = made up to fill gaps, and always labelled "sample data" in the app. |
| **Route vs trip** | A **route** is one line (one jeepney route, or MRT-3). A **trip** is your whole door-to-door plan. Full list: [`GLOSSARY.md`](../GLOSSARY.md). |

## 3. One-time setup

**Accept the GitHub invite.** Check your email or <https://github.com/notifications>. Tell the project owner (`geadlydrim`) your GitHub username if you haven't been invited yet.

**Install the tools.** You need:
- Git: <https://git-scm.com>
- GitHub CLI: <https://cli.github.com>
- Android Studio, for the build phase

**Sign in and clone:**

```sh
gh auth login                      # choose GitHub.com → HTTPS → log in with browser
git clone https://github.com/geadlydrim/appbuildersph-hackathon.git
cd appbuildersph-hackathon
```

**Read, in this order:**
1. [`docs/fmd/state.md`](fmd/state.md): where we are
2. [`GLOSSARY.md`](../GLOSSARY.md): the words we use
3. [Map #1](https://github.com/geadlydrim/appbuildersph-hackathon/issues/1): the open questions

**If you use an AI coding agent:**
- The repo ships its own skills in `.agents/skills/`: wayfinder, grilling, research, and others.
- Open the repo in your agent and **start a new session** after pulling. Most agents only load skills at startup.
- omp reads `.agents/skills/` automatically. If you use Claude Code or Cursor, tell the project owner so we can make the skills visible to your tool.
- The agent reads [`AGENTS.md`](../AGENTS.md) for the project rules.

## 4. Planning phase: what to do (finished; kept for reference)

### The loop

```mermaid
flowchart LR
    A[Look at the map] --> B[Pick a ready ticket]
    B --> C[Claim it]
    C --> D[Answer it]
    D --> E[Post the answer, close it]
    E --> F[Tell the owner]
    F --> A
```

### Step by step

**1. See what's open and unclaimed:**

```sh
gh issue list --state open --search "no:assignee -label:wayfinder:map"
```

Then open the map in the browser and check which of those say **Blocked by**. Skip any that do.

```sh
gh issue view 1 --web
```

**2. Read the ticket:**

```sh
gh issue view <number>
```

**3. Claim it.** Do this first, before any work:

```sh
gh issue edit <number> --add-assignee @me
```

**4. Answer it.** Pick one of these:
- **With an AI agent.** In your agent, run:
  ```
  /skill:wayfinder https://github.com/geadlydrim/appbuildersph-hackathon/issues/<number>
  ```
  The agent asks you questions and you make the decisions. It records the answer, closes the ticket, and updates the docs.
- **Without an agent.** Talk it through with the team, then post the decision yourself:
  ```sh
  gh issue comment <number> --body "Decision: … Why: …"
  gh issue close <number>
  ```
  Then tell the project owner (`geadlydrim`). They add the one-line summary to the map and merge the decision into `state.md`, so four people aren't editing the same file at once (D35).

**5. Repeat.** Closing a ticket can unblock others. Check the list again.

### Who does what first

| Ticket | Who |
|---|---|
| ~~[Who owns each workstream?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/11)~~ | **Superseded (D35):** no fixed role owners. Anyone claims any issue. |
| ~~[Which phone presents the demo?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/3)~~ | **Decided:** the owner's POCO X6 5G (Snapdragon 7s Gen 2). It was first recorded as the X6 Pro by mistake. |
| ~~[Which corridors does the pack cover?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/6)~~ | **Superseded:** Makati City only (D20). The hero trip pair is decided (D30, amended by D37): **V.A. Rufino St → Dela Rosa Street, Pio del Pilar**. Still open (A13): at least two genuinely different candidate trips for that pair, listed and verified by the team before pack v0. |
| [Which sync backend?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/9) | Whoever claims it |
| [Ranker data format](https://github.com/geadlydrim/appbuildersph-hackathon/issues/10) | Whoever claims it |
| ~~[LLM speed test on the demo phone](https://github.com/geadlydrim/appbuildersph-hackathon/issues/8)~~ | **Decided (D32):** Gemma 4 E2B on LiteRT-LM, GPU, with the hybrid parser: about 2–3 s per question, 10/10 correct on the test set. The LLM only parses; answers use the template. The 2.6 GB model is pre-installed on the demo phone. [Results](https://github.com/geadlydrim/appbuildersph-hackathon/blob/prototype/llm-speed-test/spikes/llm-speed-test/RESULTS.md). |
| [Travel minutes and "most efficient"](https://github.com/geadlydrim/appbuildersph-hackathon/issues/7) | Whoever claims it. Decided in D16; minutes now need to be recorded for the Makati hero trip. |

### Useful prep while you wait

None of this needs a decision first:
- Install Android Studio and set up your phone for USB debugging.
- Install `scrcpy` on the laptop that will present.
- Make a Hugging Face account and accept the Gemma model license. It's needed to download that model.
- Save the LTFRB jeepney and bus fare guides from a normal browser into the repo. The website blocks scripts, so this has to be done by hand.
- Type the LRT-1, LRT-2, and MRT-3 fare tables into a sheet. Each fact goes in with its source.

## 5. Building phase: what to do (now → 10:00 AM)

**Getting started.** The decisions are made. The decisions are broken into build issues, one per task, as we go. Anyone claims any issue; nobody is limited to one area (D35). Then:

**1. Pick your next build issue and claim it:**

```sh
gh issue list --state open --assignee @me
gh issue edit <number> --add-assignee @me
```

**2. Work on a short-lived branch:**

```sh
git switch master && git pull
git switch -c <number>-short-name
# … work …
git add -A
git commit -m "feat(routing): pick best trip by transfers then minutes"
git push -u origin HEAD
gh pr create --fill
```

**3. Merge fast.** One teammate glances at the PR, then:

```sh
gh pr merge --squash --delete-branch
```

**4. Close the loop.** When a tier fully works on the demo phone in airplane mode, whoever finished the tier's last issue tags it and saves the APK. That tag is our rollback point if something breaks later.

```sh
git tag demo-safe-t0 && git push origin demo-safe-t0
```

### Checkpoints

The table is the plan. For what is built right now, see [`docs/fmd/state.md`](fmd/state.md) §1.

| Time | Goal |
|---|---|
| ~11:30 PM | **CP1:** LLM speed test result on the demo phone, and the map spike (the Makati map renders offline on the POCO) |
| In parallel | **CP2:** first version of the Makati commute pack with road shapes, and the trip-finder module |
| ~1:30 AM | **T0:** set A and B offline, get the best trip drawn on real roads |
| ~3:30 AM | **T1:** online refresh, alternatives, suggest a trip, vote, sync. Rider Q&A (bounded trip evidence, D34) is part of T1 and the first thing cut. |
| ~4:30 AM | **Ask in words (CP-F8):** the on-device LLM turns "How to get from X to Y?" into A and B, and checks the vehicle text. It merges once T0 works, whatever T1 is doing. Tag `demo-safe-f8`. |
| ~5:30 AM | **T2:** in-trip tracking with the para alert. T0 + T1 + T2 + ask in words is the demo-ready MVP. |
| 5:30–8:00 AM | Voice and T4 trained ranker, both optional, only if the ranker beats the simple ordering. They are cut before ask in words. |
| **8:00 AM** | **Feature freeze.** Fixes only. |
| 8:00–9:30 AM | README, disclosures, 1-minute video, X/LinkedIn post |
| **10:00 AM** | **Code freeze and submission** |

**If we fall behind,** we cut features. We don't stay up arguing. The cut rules are in [`docs/fmd/build-commutenity.md`](fmd/build-commutenity.md#1-build-sequence). If time runs short at any point, rider Q&A is cut first, before tracking and before ask in words (D34). For example, if T0 isn't working by 2:30 AM, voice and the ranker are dropped, but ask in words stays (D31). Other cuts: if refresh and sync aren't working by 4:00 AM, we demo with the bundled data and local contributions and say so; if T2 isn't passing by 6:30 AM, we demo tracking with a recorded mock-GPS route, labelled as simulated; if ask in words isn't passing by 6:30 AM, we ship the smallest model that gives valid JSON with template wording, plus the text matcher, and never drop it; if the ranker hasn't beaten the simple ordering by 7:00 AM, we ship the simple ordering.

## 6. Rules everyone follows

- **The AI runs on the phone.** No cloud AI answers questions. The demo runs in airplane mode.
- **Facts come from our data, not the AI.** Routes, stops, fares, and times come from the commute pack. The AI only understands the question and words the answer.
- **Docs beat code (D35).** Fix the code, or get a decision updated first. Only the owner merges decision changes in `state.md`.
- **Mock data is fine, as long as it's labelled.** Tag it `mock`, show it as "sample data" in the app, and list it in the README. Never present it as real.
- **Never fake numbers.** No made-up benchmarks, fares, or model versions. Write down what you actually measured.
- **Finish a tier before starting the next.** A working small app beats a broken big one.
- **Never ship without on-device AI inference.** Ask in words (the on-device LLM) is release-critical (D31). If the speed test fails, use a smaller model or another runtime on the phone, never a cloud model.
- **Write down what we use.** Every model, library, data source, and AI tool goes in the README. The judges require it.

## 7. Where things are

| Need | Go to |
|---|---|
| What's decided and what's open | [`docs/fmd/state.md`](fmd/state.md) |
| Every doc, by topic | [`docs/fmd/index.md`](fmd/index.md) |
| Hackathon rules and scoring | [`docs/fmd/JUDGING.md`](fmd/JUDGING.md) |
| Open questions | [Map #1](https://github.com/geadlydrim/appbuildersph-hackathon/issues/1) |
| Rules for AI agents | [`AGENTS.md`](../AGENTS.md) |
| Stuck or unsure | Ask in the team chat, or comment on the ticket. For hackathon rules, use the official Telegram group. |
