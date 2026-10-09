# LLM speed test: results (PROTOTYPE, not the product)

Ticket: [Which LLM and runtime pass the speed test on the demo phone?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/8). Run 2026-10-09 21:45 to 2026-10-10 00:50 (+08) by geadlydrim with an agent.

## Setup

| Item | Value |
|---|---|
| Phone | Xiaomi POCO X6 5G, model `23122PCD1G`, codename `garnet`, SoC `SM7435` (Snapdragon 7s Gen 2), 7,301 MB RAM reported, Android SDK 35 |
| Runtime | LiteRT-LM `com.google.ai.edge.litertlm:litertlm-android:0.18.0`, release-like build (not debuggable) |
| Sampler | topK 1, temperature 0, max 64 output tokens; JSON-schema constrained decoding (`ResponseFormat.json`) |
| Timing | Per question: 2 discarded warm-ups, then 20 timed runs (screens: 1 warm-up, 1 run). The clock covers send, decode, parse, and contract validation. With `prewarm`, the conversation (system prompt prefilled, `prefillPrefaceOnInit`) is created before the clock starts. Its creation time is recorded separately. |
| Pass rule | Worst per-question p95 ≤ 5 s; every question valid JSON in every run; ≥ 80% of questions exact (intent, origin, destination, vehicle_text). |

## Decision

**Gemma 4 E2B (`gemma-4-E2B-it.litertlm`, 2,588,147,712 bytes, Apache-2.0, ungated) on LiteRT-LM 0.18.0, GPU backend, with the hybrid parser.** The LLM parses only; answers use the template (the owner judged the LLM phrasing not natural).

Full run (`results/v3-hybrid/gemma-4-E2B-it-gpu-json-prewarm-hybrid-FULL.json`), 10 Makati questions × 20 runs:

| Metric | Value |
|---|---|
| Worst per-question p95 | 2,842 ms (q9) |
| Overall median | 2,102 ms |
| Valid JSON | 10/10 questions, 20/20 runs each |
| Exact intent + origin + destination + vehicle_text | 10/10 (raw LLM output alone was also 10/10; the copy check never fired) |
| Preference correct | 10/10 |
| Decode / prefill | ~8 tok/s / 39–84 tok/s (prompt is only the question, 14–31 tokens) |
| Cold engine load | 16.9 s (other E2B loads on this phone: 23.2 s, 29.0 s) |
| Conversation pre-creation | 4.2–8.5 s median per question |
| PSS after run | 631 MB |
| Thermal status | 0 before and after |
| Phrasing (not shipped) | median 5.3 s; no numbers outside the facts; omitted fare and minutes; owner verdict: not natural |

## Hybrid parser (what passed)

1. Code: Taglish/English keyword cues decide `vehicle_check` and `preference`.
2. LLM: fills only `{origin, destination}` or `{vehicle_text}` (free strings under a JSON schema).
3. Code: keeps a value only if it appears word-aligned in the question after trimming edge punctuation and a `pa-` prefix; otherwise null. If origin equals destination, origin becomes null. Intent is `trip` if any place survives, else `other`.
4. Code assembles the SDD §4 parser JSON (`intent`, `origin`, `destination`, `preference`, `vehicle_text`).

## Everything that was tried

| Run | Contract | Result |
|---|---|---|
| Qwen3-0.6B int4 `nothink` (347 MB), CPU/GPU | v1 (4 fields, Valenzuela questions) | Fail: every answer `intent: other`; 8–15 s CPU, 6–7 s GPU |
| Gemma3-1B int4 (584 MB), CPU / GPU, conversation created inside the clock | v1 | Fail on speed: 10–13 s CPU, 8–11 s GPU; 4/5 exact |
| Gemma3-1B int4, GPU, prewarm, 20 runs | v1 | Pass: worst p95 4.2 s, 5/5 valid, 4/5 exact; preference 3/5 |
| Gemma3-1B int4, GPU, prewarm, screen | v2 (5 fields, SDD §4 after the Makati re-scope) | Fail: 3/6 exact, up to 5.7 s; q5 vehicle check read as a trip; invented "FCB" |
| Gemma3-1B hybrid with enum-of-spans constraint, screen | v2 + 4 unseen questions | Fail: 0/10, every string closed after its first word ("Ayala") |
| Gemma3-1B hybrid with copy check, screen | v2 | Pass: worst 4.2 s, 9/10 exact. Raw LLM invented "Cubao" twice (from the few-shot); the copy check nulled both |
| Gemma 4 E2B **GPU build** (`gemma-4-E2B-it-gpu.litertlm`, 2.0 GB), screen | v2 | Fail: malformed output that ignored the JSON constraint, with thinking on or off |
| Gemma 4 E2B generic, one-shot (no rules), screen | v2 | Pass: 10/10, worst 4.5 s (too close to 5 s for a 20-run p95) |
| **Gemma 4 E2B generic, hybrid, 20 runs** | v2 | **Pass (above)** |

Not run: Qwen2.5-0.5B (the ticket's "Qwen2.5-0.5B or Qwen3-0.6B" slot was filled by Qwen3-0.6B) and llama.cpp (not needed once LiteRT-LM passed).

## Caveats

- 10 hand-written questions, written by the same person who wrote the rules; questions q7 to q10 were added before the hybrid ran, as unseen checks. The 30-question eval set (QAD) is still needed.
- Cold load and conversation creation exceed what a rider will wait for, so the app must load the engine at startup and keep one conversation pre-created.
- The 2.6 GB model is above the SDD's 1.5 GB download target; pre-install it on the demo phone.
- The phrasing fixture uses placeholder fares and minutes, not real data.

## Run it

```sh
./gradlew :app:installRelease
sh run.sh ~/commutenity-models/gemma-4-E2B-it.litertlm gpu true 20 --ez prewarm true --ez hybrid true
```
