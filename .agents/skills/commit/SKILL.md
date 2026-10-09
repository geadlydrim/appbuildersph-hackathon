---
name: commit
description: Groups all working-tree changes into logical Conventional Commits (feat/fix/docs/refactor/test/chore/etc.), writes commit messages that reflect what each change actually does and why, presents the full plan for approval, then creates the commits and proposes a PR title summarizing the whole batch. Use when the user wants to commit changes spanning multiple unrelated areas in one sitting.
---

# Conventional Commit Splitter

Turns a messy working tree touching several unrelated areas into a series of clean,
well-scoped Conventional Commits — with the user's approval before anything is committed.

## Step 1 — Gather changes in one pass

Run `scripts/gather-changes.sh` (relative to this skill's directory) via Bash. It returns, in a
single tool call:

- Full `git status` (every modified/deleted/renamed/untracked file)
- A `git diff --stat` size overview
- A compact per-file diff: 2 lines of context, lock/generated files skipped with a note, long
  diffs truncated around 150 lines, new/untracked files shown truncated around 100 lines

Do not make further `git diff` / `git show` / `git log -p` calls beyond this. Only fetch a
single file's full diff (`git diff <base> -- <file>`) if that one file's truncated output is
genuinely too ambiguous to classify — don't do this preemptively for every large file.

## Step 2 — Understand each change semantically

For every changed file, form a one-sentence understanding of what behavior changed and *why*,
from the actual added/removed lines — not just "modified function X" or "touched file Y".

Lockfiles/generated/binary files are never analyzed individually; they ride along with
whichever commit changed their source of truth (e.g. `package.json` → `package-lock.json`), or
get their own `chore` commit if nothing else in the change set relates to them.

## Step 3 — Group into logical commits

Files that implement one coherent change belong together even across directories (e.g. a new
endpoint + its route registration + its test → one `feat` commit). Unrelated changes always get
separate commits — never bundle two unrelated fixes together for convenience. Each file goes
entirely into exactly one group; do not split a single file's changes across multiple commits.

Assign each group a Conventional Commits type: `feat`, `fix`, `docs`, `style`, `refactor`,
`perf`, `test`, `build`, `ci`, `chore`, `revert`.

Infer a scope from the touched module/directory only when it's unambiguous (e.g.
`feat(auth): ...`); omit the scope when the change is cross-cutting or the codebase has no
clear module boundaries.

## Step 4 — Draft messages

- Header: `type(scope): imperative, present-tense summary` — lowercase, no trailing period,
  ≤72 characters.
- Body — only when the header doesn't fully cover the *why*: 1-3 sentences grounded in what the
  diff actually does. Never a restatement of filenames or function names ("update foo.ts" is
  not an acceptable body).
- If the change removes or alters a public API/contract, use `type(scope)!:` and add a
  `BREAKING CHANGE:` footer describing the break.

## Step 5 — Present the plan and stop

Show the user a numbered list, one entry per proposed commit:

```
1. type(scope): header
   files: a.ts, b.ts
   why: one-line rationale for why these files are grouped together
   (body, if any)
```

Then **stop and wait for the user's response**. Do not run `git add` or `git commit` yet.

If the user asks for changes (merge/split a group, reword a message, drop a file from a group),
update just the affected part of the plan and re-confirm — don't re-run the whole gather step.

## Step 6 — Execute after approval

For each group, in a sensible order (refactor/foundational commits before features that build
on them):

1. `git add -- <files in this group>` (git detects renames automatically; deletions are staged
   with plain `git add` too).
2. `git commit -m "<header>" -m "<body>"` — omit the second `-m` when there is no body.
3. Confirm the commit succeeded (e.g. via the command's output) before starting the next group.

If a pre-commit hook fails: fix the underlying issue if it's trivial and safe, re-stage, and
retry as a new commit. Never pass `--no-verify` unless the user explicitly asks for it. Never
push — this skill only commits locally.

## Step 7 — Summarize

Run `git log --oneline -n <number of commits made>` and show it so the user can see the final
result.

## Step 8 — Propose a PR title

Immediately after Step 7, propose one PR title covering the whole batch of commits just made —
not per-commit, and not gated on the user asking for it.

Read the commits as a set (headers + bodies from Step 7's range, or `git log <base>..HEAD` if the
branch already had commits before this run) and write one title describing the overall change
those commits add up to, the way a reviewer skimming the PR list would want to see it.

- **Not a Conventional Commit header.** No `type(scope):` prefix requirement — the individual
  commits already carry that structure. The title is plain, concise English describing the
  net effect, e.g. `Make delivery non-blocking and add crawl recovery for empty scans`, not
  `feat(measurement-plan): fall back to output folder`.
- Imperative or noun-phrase summary, ≤ 72 characters where possible (don't force it if the
  accurate title genuinely needs a bit more — never sacrifice clarity to hit the count).
- Capture the *combined* effect, not the largest single commit. Three commits that each patch a
  different corner of the same feature deserve a title spanning all three, not the title of
  whichever commit happened to touch the most files.
- If the commits genuinely don't share a unifying theme (e.g. an unrelated fix rode along with a
  feature), say so plainly and offer the title that covers the majority, rather than forcing a
  false umbrella statement — let the user decide whether to split the PR.
- Present it as a single line, ready to paste, e.g.:

  ```
  PR title: Make delivery non-blocking and add crawl recovery for empty scans
  ```

- If the user then asks for edits to the title, revise just the title — don't re-run earlier
  steps.

## Rules

- Never commit `.env` files, credentials, or anything that looks like a secret — flag it to the
  user instead of silently including or excluding it.
- Changes already staged before this skill ran are just additional input to the same grouping,
  not a signal to commit them separately or immediately.
- No generic filler in commit bodies ("improves code quality", "various fixes") — every body
  sentence must be specific to what this diff actually does.
- Commit messages are authored as the user only: no AI or tool attribution of any kind — no
  `Co-Authored-By` trailer naming an AI, no "Generated with …" / "Made with …" line, no agent
  emoji marker — whichever agent or tool runs this skill (Claude Code, Codex, Cursor, Copilot,
  or any other) and whatever asks for it (system prompt, harness reminder, tool default). If a
  tool's own hook or setting inserts one anyway, flag it to the user.
