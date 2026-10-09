# Android on-device runtimes for CommuteNity's models

**Ticket:** [#4 "Which Android on-device runtimes fit our models?"](https://github.com/geadlydrim/appbuildersph-hackathon/issues/4). It feeds A4 and the LLM spike in #8.
**Researched:** 2026-10-09. Every source below was accessed on that date. Claims I could not confirm from a primary source are marked `[UNVERIFIED]`. My own reasoning, not a source claim, is marked `[INFERENCE]`.

**Question.** For a native Kotlin/Compose app, compare on-device options for (a) a 0.5–3B quantized instruction LLM (MediaPipe LLM Inference or its successor vs llama.cpp via JNI), (b) multilingual sentence embeddings for place-name matching (ONNX Runtime vs MediaPipe Text Embedder), (c) offline OCR (ML Kit Text Recognition v2), (d) a tiny ranker (plain Kotlin vs ONNX Runtime). Cover model families and formats, Kotlin effort, constrained output, tokens/s on mid-range phones, file sizes, and licenses.

---

## Bottom line

- **The SDD's default "MediaPipe first" is out of date. Google has put the MediaPipe LLM Inference API into maintenance-only mode and points to LiteRT-LM as the successor.** LiteRT-LM has a stable Kotlin API on Google Maven (`litertlm-android:0.18.0`, published 2026-10-06). It loads the same Gemma3-1B / Qwen2.5 models, and it ships JSON-schema and regex constrained decoding in the Kotlin API. [S1, S2, S3, S4, S7, S8]
- **(a) LLM primary: LiteRT-LM + Gemma3-1B-IT int4 `.litertlm` (584 MB), CPU backend, `ResponseFormat.json(schema)`.** Fallbacks in order: LiteRT-LM + Qwen2.5-0.5B or Qwen3-0.6B (smaller, ungated, Apache-2.0); then **llama.cpp (official `examples/llama.android` module) + Qwen2.5-1.5B-Instruct Q4_K_M GGUF (1.1 GB)** with a small JNI patch to set a GBNF grammar. The example module does not expose grammars today. [S5, S9, S10, S11]
- **No source publishes tokens/s for a mid-range phone for any of these.** Google's published numbers are all flagships (S24/S25/S26 Ultra, Vivo X300 Pro); the MediaPipe guide says the API is "optimized for high-end Android devices". Published S24 Ultra CPU decode is about 30–34 tok/s for Gemma3-1B, Qwen2.5-0.5B, and Qwen2.5-1.5B. By my arithmetic (300 prompt tokens, 40 output tokens) that is about 2.5–3 s on a flagship, so a phone more than about 1.7× slower than that busts the 5 s budget unless the prompt is trimmed. Only a measurement on the demo phone settles it. [S1, S3; arithmetic in §2.4]
- **(b) Embeddings primary: LiteRT-LM `EmbeddingEngine` + EmbeddingGemma 2 Text 270M (165 MB, Apache-2.0, ungated, 100+ languages per its model card).** It adds no second runtime if (a) uses LiteRT-LM. Fallbacks: MediaPipe Text Embedder + `embedding_gemma.task` (184 MB, Gemma license); then ONNX Runtime + `multilingual-e5-small` int8 (118 MB, MIT, Tagalog listed) at the cost of writing a tokenizer. Keep deterministic alias matching first, as the SDD already specifies; embeddings are only the third tier, and alias vectors can be precomputed. [S13, S14, S15, S16]
- **(c) OCR primary: ML Kit Text Recognition v2, Latin script, bundled (`com.google.mlkit:text-recognition:16.0.1`).** It is on-device and works offline from first launch (about 4 MB per script per architecture). The unbundled Play Services variant is about 260 KB but downloads the model on first use. Tagalog/Filipino is written in Latin script, so Latin is the right library `[INFERENCE]`; accuracy on painted jeepney signboards is unverified (FC-9). [S17]
- **(d) Ranker primary: plain Kotlin** (logistic weights as JSON; GBDT as a JSON tree dump walked in Kotlin `[INFERENCE]`). Fallback: export the GBDT to ONNX and run it with ONNX Runtime Android, but only if ORT is already in the app. ORT's prebuilt Android AAR is 55 MB (all ABIs) for a job a dot product does. [S15, S18]

---

## 1. Status check: is MediaPipe LLM Inference superseded?

Yes. Both the overview and the Android guide open with a notice that the MediaPipe LLM Inference API (Android, iOS, Web) is in **maintenance-only mode**, and tell developers to migrate to **LiteRT-LM**. [S1, S2]

| Item | MediaPipe LLM Inference (`tasks-genai`) | LiteRT-LM |
|---|---|---|
| Status | Maintenance-only; "new features and optimizations will be focused on LiteRT-LM" [S2] | Google's "production-ready" LLM orchestration layer; Kotlin API listed "Stable" [S3] |
| Latest Maven release | `com.google.mediapipe:tasks-genai:0.10.35`, 2026-04-27 (the guide's quickstart still pins `0.10.27`) [S1, S8] | `com.google.ai.edge.litertlm:litertlm-android:0.18.0`, 2026-10-06; six releases between 2026-08-04 (0.15.0) and 2026-10-06 [S5, S8] |
| AAR / native size | AAR 42.4 MB (measured via HTTP header) [S8] | AAR 20.9 MB; `arm64-v8a` .so 22.2 MB and `x86_64` .so 26.4 MB (inspected from the AAR); `minSdk 24` [S8] |
| Model formats | `.task` and `.litertlm` [S2] | `.litertlm` (`.task` bundles: whether `Engine` loads them is `[UNVERIFIED]`) [S4, S9] |
| Backends | CPU/GPU [S2] | CPU, GPU, NPU [S3, S4] |
| Structured output | None in the documented option table (modelPath, maxTokens, topK, temperature, randomSeed, loraPath, listeners) [S1] | JSON Schema and regex via LLGuidance, in Kotlin `ResponseFormat` [S6, S7] |
| License | Apache-2.0 (repo) [S8] | Apache-2.0 (repo) [S5] |

The Google AI Edge Gallery app (Apache-2.0) runs on LiteRT-LM and shows TTFT and decode speed for models you import (`.litertlm` or `.task`). It is a free no-code smoke test for the demo phone before any spike code is written. [S1, S3]

---

## 2. (a) Small instruction LLM

### 2.1 Runtime comparison

| | **LiteRT-LM (Kotlin)** | **llama.cpp via JNI** | MediaPipe LLM Inference (legacy) |
|---|---|---|---|
| Model files | `.litertlm` from the [LiteRT Community](https://huggingface.co/litert-community); own PyTorch models via LiteRT Torch. Broad support: "Gemma, Llama, Phi-4, Qwen and more" [S3] | `.gguf`. Any architecture llama.cpp supports, with first-party GGUFs for Qwen2.5 (Qwen org) and Gemma 3 (`ggml-org`) [S11] | `.task` / `.litertlm`; Gemma-3 1B, Gemma-3n, Gemma-2 2B documented [S2] |
| Integration | One Gradle line; `Engine` → `Conversation` → `sendMessage` or `Flow`; coroutines-friendly. Roughly tens of lines `[INFERENCE]` [S4] | Import the official `examples/llama.android` `lib` module (`com.arm.aichat`): NDK `29.0.13113456`, CMake `3.31.6`, `minSdk 33` in the sample, ABIs `arm64-v8a` + `x86_64`, builds llama.cpp from source in Gradle. API: `loadModel`, `setSystemPrompt`, `sendUserPrompt(): Flow<String>`, `bench` [S10] | One Gradle line; `LlmInference.createFromOptions` + `generateResponse` [S1] |
| Constrained output | `ConversationConfig(enableResponseFormat = true)` + `sendMessage(message, responseFormat = ResponseFormat.json(schema))`; also `ResponseFormat.regex(pattern)`. Backed by LLGuidance. Present in the shipped 0.18.0 AAR (I confirmed the `ResponseFormat` classes in `classes.jar`). Not mentioned in the Android dev-doc page, so treat the API as young. [S6, S7, S8] | GBNF grammars and JSON-Schema→GBNF are first-class in llama.cpp (`grammars/README.md`, `common/json-schema-to-grammar.h`; `common_params_sampling.grammar`). **The Android example's JNI creates its sampler with only `temp` set (0.3), so you must patch `ai_chat.cpp` to pass a grammar.** Small change, but yours to write. [S10] | None documented |
| Prompt handling | Chat templating and system instruction handled in `ConversationConfig` [S4] | Chat template via `common_chat_templates`; system prompt processed once and kept in KV; context shift implemented (and marked TODO to improve) [S10] | Raw prompt string [S1] |
| Quirks | `engine.initialize()` "can take up to 10 seconds"; set `cacheDir` to speed up the 2nd load; GPU backend needs two `<uses-native-library>` manifest lines [S4] | Example hard-codes `n_ctx = 8192`, 2–4 threads, global singleton state; `ggml_backend_load_all_from_path` loads CPU variants at runtime [S10] | "Optimized for high-end Android devices, such as Pixel 8 and Samsung S23 or later, and does not reliably support device emulators" [S1] |
| License (runtime) | Apache-2.0 [S5] | MIT [S10] | Apache-2.0 [S8] |
| Published tok/s | Flagships only, see §2.3 | **None found in official docs.** The Snapdragon backend README shows sample logs, but they look illustrative, so I did not use them. `[UNVERIFIED]` for all mid-range llama.cpp numbers [S10] | Same as LiteRT-LM (model-card numbers) |

### 2.2 Models: formats, sizes, licenses

| Model | Format / file | Size | License / access |
|---|---|---|---|
| Gemma3-1B-IT, LiteRT | `gemma3-1b-it-int4.litertlm` (also `…multi-prefill-seq_q4_ekv4096.litertlm`) | **584 MB**. `.task` int4 555 MB; q8 `.task` ~1.05 GB | Gemma Terms of Use; **gated on Hugging Face** (`auto`: needs HF login and license acceptance). The app's first-run downloader cannot fetch it anonymously. [S9, S12] |
| Qwen2.5-0.5B-Instruct, LiteRT | `.task` q8 (547 MB); `.tflite` q8 (513–544 MB). No `.litertlm` in the repo. | 547 MB | Apache-2.0, ungated [S9] |
| Qwen2.5-1.5B-Instruct, LiteRT | `…q8_ekv4096.litertlm` | 1,598 MB | Apache-2.0, ungated [S9] |
| Qwen3-0.6B, LiteRT | `Qwen3-0.6B.litertlm` (614 MB); `qwen3_0_6b_mixed_int4.litertlm` (498 MB); `litert-community/Qwen3-0.6B-int4` has `qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm` (347 MB) | 347–614 MB | Apache-2.0, ungated. A reasoning model: use `ThinkingConfig(enableThinking=false)` or the `nothink` file. [S4, S9] |
| Llama-3.2-1B, LiteRT | `llama3_2_1b_mixed_int4_gpu.litertlm` (name suggests a GPU build) | 964 MB | Llama 3.2 community license; gated (`auto`). No published benchmark in the LiteRT-LM overview. [S9] |
| FunctionGemma 270M (mobile-actions fine-tune) | `mobile_actions_q8_ekv1024.litertlm` | 289 MB | Gemma Terms; gated (`auto`). A function-calling fine-tune, not a general parser. [S3, S9] |
| Gemma-4-E2B-it | `gemma-4-E2B-it.litertlm` | 2,588 MB | Apache-2.0, ungated. 2.6 GB plus 1.7 GB peak RSS on a flagship is heavy for a mid-range phone. [S3, S9] |
| Qwen2.5-0.5B-Instruct GGUF (Qwen org) | Q4_0 / Q4_K_M / Q5_K_M / Q8_0 | 429 / **491** / 522 / 676 MB | Apache-2.0 [S11] |
| Qwen2.5-1.5B-Instruct GGUF (Qwen org) | Q4_0 / Q4_K_M / Q5_K_M / Q8_0 | 1,066 / **1,117** / 1,286 / 1,895 MB | Apache-2.0 [S11] |
| Qwen2.5-3B-Instruct GGUF (Qwen org) | Q4_K_M | 2,105 MB | Hugging Face license field `other`; license name `qwen-research`. **Not Apache-2.0**; check its terms before using it. Avoid for the hackathon. [S11] |
| Gemma-3-1B-it GGUF (`ggml-org`) | Q4_K_M / Q8_0 | **806** / 1,069 MB | Gemma Terms; ungated (the QAT `google/…-qat-q4_0-gguf` is manually gated) [S11, S12] |
| Llama-3.2-1B-Instruct GGUF | Q4_K_M 808 MB, Q4_0 773 MB (from `unsloth/…`, a third party; Meta's own repo is gated) | 773–808 MB | Llama 3.2 community license [S11] |

Gemma Terms of Use (modified 2026-04-01) allow redistribution if you include the use restrictions, give recipients the terms, and ship a Notice file. Outputs are not claimed by Google. Disclose this in the README's model list. [S12]

### 2.3 Published speed (the only numbers available)

From the LiteRT-LM overview ("Last updated 2026-10-06"). The page does not say which quantization variant each row measured. Model "Size (MB)" in the table (e.g., Gemma3-1B = 1,005 MB) differs from the int4 file above (584 MB), so the benchmark variant is `[UNVERIFIED]`. [S3]

| Model | Device | CPU prefill / decode (tok/s) | GPU prefill / decode (tok/s) |
|---|---|---|---|
| Gemma3-1B (1,005 MB) | Samsung S24 Ultra | 177 / 33 | 1,191 / 24 |
| Qwen2.5-0.5B (521 MB) | Samsung S24 Ultra | 251 / 30 | — |
| Qwen2.5-1.5B (1,598 MB) | Samsung S25 Ultra | 298 / 34 | 1,668 / 31 |
| Qwen3-0.6B (586 MB) | Vivo X300 Pro | 165 / 9 | 580 / 21 |
| FunctionGemma (289 MB) | Samsung S25 Ultra | 2,238 / 154 | — |
| Gemma-4-E2B (2,583 MB) | Samsung S26 Ultra | 557 / 47 (1.8 s TTFT, 1,733 MB peak CPU mem) | 3,808 / 52 |

Model-card figures for Qwen2.5-0.5B (S24 Ultra, CPU, XNNPACK, 4 threads, KV 1280): dynamic-int8 gives 250.7 prefill / 30.0 decode tok/s, TTFT 2.31 s, RSS 1,363 MB, model 521 MB. [S9]

Observations:
- On the S24 Ultra, **Gemma3-1B decodes faster on CPU (33) than on GPU (24)**. Start the spike on CPU. [S3]
- **No mid-range data point exists in any official source.** The MediaPipe guide explicitly targets Pixel 8 / S23 or later. [S1]

### 2.4 What a 5 s parse budget implies `[INFERENCE]`

Latency ≈ `prompt_tokens / prefill_rate + output_tokens / decode_rate`, ignoring tokenization and sampler overhead. The parser JSON in SDD §4 has only four short fields, so output can be about 40 tokens. With a ~300-token prompt (system prompt plus 2 few-shot examples plus the question):

| Setup (S24/S25 Ultra CPU, published rates) | Estimated time |
|---|---|
| Gemma3-1B (177 / 33) | 300/177 + 40/33 ≈ **2.9 s** |
| Qwen2.5-0.5B (251 / 30) | 300/251 + 40/30 ≈ **2.5 s** |
| Qwen2.5-1.5B (298 / 34) | 300/298 + 40/34 ≈ **2.2 s** |

These are best cases on flagship CPUs. **A phone about 1.7× slower than an S24 Ultra on the Gemma3-1B row already breaks 5 s.** Levers, in order of cost: trim the prompt to ≤150 tokens; cap output at `maxOutputToken ≈ 64`; constrain the output with an enum schema so no tokens are wasted on prose; keep the system prompt prefilled and reused (llama.cpp does this natively via `setSystemPrompt`; reuse across `createConversation` calls in LiteRT-LM is `[UNVERIFIED]`); drop to Qwen2.5-0.5B.

---

## 3. (b) Multilingual embeddings for place-name matching

| | **LiteRT-LM `EmbeddingEngine`** | **MediaPipe Text Embedder** (`tasks-text:1.1.0`, 2026-10-06) | **ONNX Runtime Android** (`onnxruntime-android:1.31.0`, 2026-10-09) |
|---|---|---|---|
| Models | EmbeddingGemma 2 Text 270M `.litertlm`: **165 MB** (also 388 MB text+vision, 485 MB omnimodal). Card: "supports over 100 languages", MRL output sizes 128/256/512/768 dims, input sizes 128–8192 tokens. [S14] | `embedding_gemma.task` (EmbeddingGemma 300M, int4+int8, **183.8 MB** by HTTP header, max sequence 512) or Universal Sentence Encoder (**6.1 MB** `.tflite`; language coverage not stated in the docs, `[UNVERIFIED]`). [S13] | Any ONNX model you convert or download. See candidates below. [S15, S16] |
| Speed (published) | 27.1 ms CPU / 25.9 ms GPU on S26 Ultra, 128-token signature; 334 MB CPU memory [S14] | ~200 ms (EmbeddingGemma 300M) and ~10 ms (USE) on S26 Ultra CPU, 4 threads [S13] | No official number for these models on Android |
| Tokenization | Built in | Built in ("the task handles … tokenization") [S13] | **Not included.** You must run or port the model's tokenizer (XLM-R SentencePiece for e5/MiniLM). The `onnxruntime-extensions-android` artifact's latest Maven release is 0.13.0 (2024-10-31). Either way, this is the biggest effort item. [S15, S16] |
| Kotlin effort | Same dependency and API style as the LLM; `EmbeddingEngine(EmbeddingEngineConfig(...)).computeEmbedding(...)` [S14] | `TextEmbedder.createFromOptions` / `embed` / `cosineSimilarity`, plus `TextFormatContext` for EmbeddingGemma prompts [S13] | `OrtEnvironment` / `OrtSession` / `OnnxTensor`; Java/Kotlin bindings in `onnxruntime-android`. AAR is **55 MB** (arm64-v8a .so 34 MB; armeabi-v7a, x86, x86_64 also bundled; inspected). [S15] |
| License | Model card: Apache-2.0; ungated [S14] | EmbeddingGemma: Gemma Terms; the file hosts on a public Google bucket [S12, S13] | ORT: MIT [S15]. Models vary, see below |

**ONNX candidates** (all from Hugging Face model repos; sizes via API):

| Model | ONNX file | Size | License | Tagalog? |
|---|---|---|---|---|
| `intfloat/multilingual-e5-small` | `onnx/model_qint8_avx512_vnni.onnx` (official, but quantized for x86 AVX512-VNNI, so test on ARM); `model_O4.onnx` 235 MB; `model.onnx` 470 MB | 118 / 235 / 470 MB | MIT | `tl` is in the card's language list; "supports 100 languages from xlm-roberta, but low-resource languages may see performance degradation". Needs `query: ` / `passage: ` prefixes. [S16] |
| `Xenova/multilingual-e5-small` | `onnx/model_int8.onnx` / `model_quantized.onnx` | 118 MB | none stated | same base model [S16] |
| `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` | `onnx/model_qint8_arm64.onnx` (official ARM64 quantization) | **118 MB** | Apache-2.0 | **Not in its language list** (50+ languages: id, ms, vi, th… but no `tl`/`fil`) [S16] |
| `onnx-community/embeddinggemma-300m-ONNX` | `model_q4.onnx` (+`_data`) / `model_q4f16.onnx` | 197 MB / 175 MB | Gemma Terms | 100+ languages per Google [S12, S16] |

**Reading `[INFERENCE]`:**
- Place names are short and a pack has hundreds to low thousands of aliases. Do not pay 118–184 MB and a tokenizer port to find "Cubao" for "Kubao". The SDD's order is right: exact alias → normalized alias → embeddings. Add a cheap character-trigram / edit-distance step before embeddings.
- Precompute alias vectors once (laptop or first run) and ship or cache them in the pack. Then runtime embeds one query string per question. At the published ~27 ms per call on a flagship, first-run embedding of 1,000 aliases would be about 27 s, so shipping vectors computed with the same model file is better. (Whether vectors from desktop and phone match is `[UNVERIFIED]`; check cosine ≈ 1 in the spike.)
- EmbeddingGemma 2 is new (the v0.18.0 release notes call it shipped on 2026-10-06). Treat it as the riskiest primary choice; the fallbacks are real.

---

## 4. (c) Offline OCR: ML Kit Text Recognition v2

| Fact | Detail | Source |
|---|---|---|
| On-device / offline | "ML Kit's processing happens on-device … It also works while offline and can be used for processing images and text that need to remain on the device." | S17 |
| Scripts | Latin, Chinese, Devanagari, Japanese, Korean. A separate library per script. No Filipino-specific model. Tagalog uses Latin script. | S17 |
| Bundled | `com.google.mlkit:text-recognition:16.0.1`. Model statically linked at build time. **About 4 MB per script per architecture.** Model available immediately. | S17 |
| Unbundled | `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1`. Model downloaded via Google Play services. About 260 KB per script per architecture. "Might have to wait for model to download before first use." If you do not enable install-time download, it downloads the first time you run the scanner, and requests made before download completes return no results. | S17 |
| Performance | "Real-time on most devices for Latin script library, slower for others." No per-device latency numbers published. | S17 |
| API | `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`, `InputImage.fromBitmap/fromMediaImage`, then `process(image)`. Min API 21. Result: blocks → lines → elements → symbols, with bounding boxes, confidence, language. | S17 |
| Libraries last updated | Maven: `text-recognition` 16.0.1 and `play-services-mlkit-text-recognition` 19.0.1 (the page lists these as current). Docs page last updated 2024-07-10. | S8, S17 |

**Recommendation `[INFERENCE]`:** bundle the Latin model. The demo is in airplane mode and must not depend on Google Play services downloading anything. The 4 MB cost is negligible next to the 0.5–1.6 GB LLM. Painted jeepney lettering is not typical printed text, so accuracy is unverified; the SDD's matcher already needs a conservative threshold and an "unreadable" verdict (AIA-R2). PaddleOCR via ONNX (an SDD candidate) was not researched here and is not worth starting before ML Kit fails on real signboard photos.

---

## 5. (d) Tiny ranker: plain Kotlin vs ONNX Runtime

| | Plain Kotlin | ONNX Runtime Android |
|---|---|---|
| Logistic regression | `sigmoid(w·x + b)`: a dozen lines. Weights as JSON, as the data plan already specifies. Parity test (QA-11) compares two float implementations of the same formula. | Convertible with `sklearn-onnx` (Apache-2.0); ORT's own Java test set includes a scikit-learn logistic regression model (`lr_mnist_scikit.onnx`). [S15, S18] |
| GBDT | Dump trees to JSON (LightGBM/XGBoost/sklearn), walk them in Kotlin. Fine for tens to low hundreds of shallow trees. `[INFERENCE]` | `sklearn-onnx` handles sklearn models; `onnxmltools` converts LightGBM/XGBoost. The prebuilt Android package "contain[s] the full ONNX Runtime feature and operator set". [S15, S18] |
| Cost | Zero dependencies | 55 MB AAR (all ABIs; arm64 .so 34 MB); a custom minimal build cut ORT 1.18's AAR from 24.4 MB to 7.5 MB in ORT's own table, but needs the ORT-format conversion and a custom build, so not an overnight task. [S15] |
| Risk | You own the tree-walker | Output types for sklearn classifiers are `OnnxSequence`/`OnnxMap` and need handling [S15] |

**Recommendation:** plain Kotlin. The ranker scores a few dozen candidates within a 300 ms budget. Use ORT only if the embedding fallback already brought it in.

---

## 6. Recommended stack

| Role | Primary | Fallback 1 | Fallback 2 |
|---|---|---|---|
| (a) Parser/phrasing LLM | LiteRT-LM `litertlm-android:0.18.0` (pin the version) + **Gemma3-1B-IT int4 `.litertlm`** (584 MB), CPU, JSON-schema `ResponseFormat`, `maxOutputToken ≈ 64` | Same runtime + **Qwen2.5-0.5B-Instruct** (547 MB `.task`; or Qwen3-0.6B int4 `.litertlm`, 347 MB, thinking off). Ungated, so the first-run downloader works. | **llama.cpp** (`examples/llama.android` `lib`, patched for GBNF) + **Qwen2.5-1.5B-Instruct Q4_K_M** (1,117 MB) or Qwen2.5-0.5B Q4_K_M (491 MB) or Gemma-3-1B Q4_K_M (806 MB, `ggml-org`). Last resort: MediaPipe `tasks-genai:0.10.35` (maintenance-only, no constrained decoding, so prompt-plus-validate plus the SDD's one retry), then the SDD's rule-based parser. |
| (b) Place embeddings | LiteRT-LM `EmbeddingEngine` + EmbeddingGemma 2 Text 270M (165 MB), alias vectors precomputed; alias/fuzzy match first | MediaPipe `tasks-text:1.1.0` + `embedding_gemma.task` (184 MB) | ONNX Runtime + `multilingual-e5-small` int8 (118 MB) + own tokenizer |
| (c) OCR | ML Kit Text Recognition v2 Latin, **bundled** `16.0.1` | ML Kit unbundled (Play services; needs prior download) | PaddleOCR via ONNX (not researched) |
| (d) Ranker | Plain Kotlin (JSON weights / JSON trees) | ONNX Runtime Android | — |

Hackathon rules: `docs/fmd/JUDGING.md` lists llama.cpp and ONNX by name and says "No specific model, framework, OS, or hardware is required", so LiteRT-LM is allowed. Disclose every model and framework used in the README. [S19]

---

## 7. Spike plan for #8 (LLM speed on the demo phone)

**Why this changes the default:** the ticket's default was "MediaPipe first, then llama.cpp". MediaPipe LLM Inference is now maintenance-only, so the first runtime to try is LiteRT-LM. Use MediaPipe `tasks-genai` only if LiteRT-LM's `Engine` will not load on the demo phone.

### 7.1 Order of attempts (stop at the first one that passes; ≈30 min each)

0. **10-minute smoke test, no code:** install Google AI Edge Gallery (Play Store), import the model file, and read the TTFT and decode tok/s it shows. Do this for Gemma3-1B first to confirm the phone can run it at all. [S1, S3]
1. **LiteRT-LM + Gemma3-1B-IT int4 `.litertlm` (584 MB), CPU backend.** Needs the HF login and license click-through (gated); push with `adb push`. Then repeat on GPU for comparison.
2. **LiteRT-LM + Qwen2.5-0.5B-Instruct** (`.task`, 547 MB; if `Engine` rejects `.task`, use Qwen3-0.6B `nothink` int4 `.litertlm`, 347 MB). This is the speed floor.
3. **LiteRT-LM + Qwen2.5-1.5B-Instruct `q8_ekv4096.litertlm` (1,598 MB)** only if 1–2 parse badly and the phone has the RAM. This is the accuracy ceiling.
4. **llama.cpp**, in parallel on a second person's laptop since the NDK build is the slow part: import `examples/llama.android`, patch `ai_chat.cpp` so `common_params_sampling.grammar` is set to a GBNF (or build one from the JSON schema with `json_schema_to_grammar`), set `temp=0`, and load Qwen2.5-1.5B Q4_K_M (1,117 MB), Qwen2.5-0.5B Q4_K_M (491 MB), and Gemma-3-1B Q4_K_M (806 MB).

### 7.2 The five test questions

Use the parser JSON from SDD §4 (`intent`, `origin`, `destination`, `preference`). Include: pure Tagalog, Taglish with a landmark alias ("pa-SM North galing Cubao"), an English "cheapest" question, a typo or abbreviation ("Ayala to BGC"), and one non-trip question that must return `intent: "other"`. No vendor published Taglish quality numbers for any candidate, so these five are the only evidence `[UNVERIFIED]`.

### 7.3 How to measure "≤ 5 s warm parse"

Set up once:
- **Hardware:** demo phone, battery above 50%, screen on, airplane mode, no other apps; note `Build.MODEL`, `Build.SOC_MODEL`, total RAM. Let it cool between model trials.
- **Build:** release-like (not debuggable) bare app, one Activity, model path pushed with `adb`. Keep tokenization and JSON decoding inside the timed region.
- **Config:** CPU first; fixed `SamplerConfig` (temperature ≈ 0) so runs are comparable; prompt ≤ 150 tokens system + the question; `maxOutputToken` ≈ 64.

Per model, record these (the report #8 asks for):

| Metric | How to measure |
|---|---|
| Model ID, quantization, file size | From the file name and `ls -l` |
| Cold load | Wall time of `Engine(config).initialize()` on the first launch after a force-stop; then the 2nd launch (LiteRT-LM `cacheDir` set) [S4] |
| Time to first token and prefill tok/s | Stream with `sendMessageAsync(...): Flow<Message>`; timestamp the first emission (`System.nanoTime()`); prefill tok/s = prompt tokens / TTFT (count prompt tokens from the tokenizer or the runtime's reporting) |
| Decode tok/s | output tokens / (t_last − t_first) |
| **Per-question latency** | `System.nanoTime()` before `sendMessage(...)` and after the response is **parsed and validated** (kotlinx.serialization decode + enum check). Same bracketing for llama.cpp around `sendUserPrompt(...).collect`. |
| JSON validity | Count of responses that decode and pass schema validation, with constrained decoding ON. Also run once with it OFF to show what the constraint buys. |
| Correctness | Exact origin/destination/preference match against hand-labelled answers for the five questions |
| Memory | `adb shell dumpsys meminfo <pkg>` peak PSS after a run |
| Thermal drift | `adb shell dumpsys thermalservice` before and after; run 20 back-to-back iterations and watch the median drift |

Run protocol: **2 discarded warm-ups, then 20 timed iterations per question** (the SDD's QAD measures 20 timed runs). Report median and p95 per question. llama.cpp also has a built-in `bench(pp, tg, pl, nr)` JNI call for raw prefill/decode tok/s. [S10, S19]

**Pass rule:** p95 per-question latency ≤ 5 s, 5/5 valid JSON, and ≥ 4/5 exact origin/destination. Pick the smallest model that passes (matching #8's default). If none pass in 1–4, go to the SDD's fallback (rule-based parser plus embedding resolver) and keep the LLM for phrasing only, which has no 5 s budget because the template renders first.

### 7.4 Minimal harness sketch (from the documented API; check names against the 0.18.0 sources)

```kotlin
// build.gradle.kts: implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
val engine = Engine(EngineConfig(modelPath = path, backend = Backend.CPU(), cacheDir = ctx.cacheDir.path))
val tLoad = measureNanoTime { engine.initialize() }          // cold-load metric
val conv = engine.createConversation(
    ConversationConfig(
        systemInstruction = Contents.of(SYSTEM_PROMPT),
        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
        maxOutputToken = 64,
        enableResponseFormat = true,                          // LLGuidance constraint provider
    )
)
val t0 = System.nanoTime()
val reply = conv.sendMessage(Message.user(question), responseFormat = ResponseFormat.json(PARSER_SCHEMA))
val parsed = Json.decodeFromString<ParserOut>(reply.toString()) // validate here, then stop the clock
val ms = (System.nanoTime() - t0) / 1e6
```

`[UNVERIFIED]`: `Backend.CPU()` default arguments and `SamplerConfig` field types are taken from the docs and the Config source but not compiled here. Whether `responseFormat` is accepted by the async `Flow` overload is also unverified; use the synchronous call for the total-latency number.

---

## 8. Open items left for the spike (not resolvable from docs)

- Mid-range phone speed for every candidate, and the demo phone's actual SoC/RAM (A12).
- Taglish parsing accuracy for Gemma3-1B, Qwen2.5, Qwen3, Llama 3.2 at ≤1.6 GB.
- Whether LiteRT-LM `Engine` loads `.task` bundles (needed for the Qwen2.5-0.5B file).
- Latency overhead of LLGuidance constrained decoding on a phone CPU.
- Whether desktop-computed EmbeddingGemma 2 alias vectors match phone-computed ones.
- ML Kit accuracy on painted jeepney signboards (FC-9).

---

## Sources (all accessed 2026-10-09)

- **S1** MediaPipe LLM Inference guide for Android (maintenance-only banner, quickstart `tasks-genai:0.10.27`, config options, "optimized for high-end Android devices"): https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android
- **S2** MediaPipe LLM Inference overview (maintenance-only banner, models Gemma-3n / Gemma-3 1B / Gemma-2 2B, `.task`/`.litertlm`): https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference
- **S3** LiteRT-LM overview (benchmarks, supported models, backends; last updated 2026-10-06): https://developers.google.com/edge/litert-lm/overview
- **S4** LiteRT-LM Kotlin / Android guide (Gradle artifact, `Engine`, `Conversation`, tools, thinking; last updated 2026-09-04): https://developers.google.com/edge/litert-lm/android
- **S5** LiteRT-LM repository, README and license (Apache-2.0; v0.18.0 released 2026-10-06): https://github.com/google-ai-edge/LiteRT-LM and https://github.com/google-ai-edge/LiteRT-LM/releases
- **S6** LiteRT-LM constrained decoding (LLGuidance: regex, JSON Schema, Lark; C++ API doc): https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/cpp/constrained-decoding.md
- **S7** LiteRT-LM Kotlin sources at tag v0.18.0: https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/ResponseFormat.kt , `Conversation.kt` and `Config.kt` in the same directory (`enableResponseFormat`, `sendMessage(..., responseFormat)`)
- **S8** Maven metadata and artifacts: https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/maven-metadata.xml (latest 0.18.0), https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-genai/maven-metadata.xml (0.10.35), https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-text/maven-metadata.xml (1.1.0), https://dl.google.com/dl/android/maven2/com/google/mlkit/text-recognition/maven-metadata.xml (16.0.1), https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-mlkit-text-recognition/maven-metadata.xml (19.0.1); AAR contents inspected from https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/0.18.0/litertlm-android-0.18.0.aar ; MediaPipe repo license: https://github.com/google-ai-edge/mediapipe
- **S9** LiteRT Community model repos on Hugging Face (file lists, sizes, license, gating via the HF API): https://huggingface.co/litert-community/Gemma3-1B-IT , https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct (model card with S24 Ultra table), https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct , https://huggingface.co/litert-community/Qwen3-0.6B , https://huggingface.co/litert-community/Qwen3-0.6B-int4 , https://huggingface.co/litert-community/Llama-3.2-1B , https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions , https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- **S10** llama.cpp Android: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md ; example module https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android (`lib/build.gradle.kts`, `lib/src/main/cpp/ai_chat.cpp`, `lib/src/main/java/com/arm/aichat/InferenceEngine.kt`); GBNF https://github.com/ggml-org/llama.cpp/blob/master/grammars/README.md ; `common/common.h` (`common_params_sampling.grammar`) and `common/json-schema-to-grammar.h` in the same repo; license MIT (repo license file)
- **S11** GGUF repos on Hugging Face: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF , https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF , https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF , https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF , https://huggingface.co/google/gemma-3-1b-it-qat-q4_0-gguf , https://huggingface.co/unsloth/Llama-3.2-1B-Instruct-GGUF , https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct
- **S12** Gemma Terms of Use (modified 2026-04-01): https://ai.google.dev/gemma/terms
- **S13** MediaPipe Text Embedder overview and Android guide (models, benchmarks on S26 Ultra, `TextFormatContext`): https://developers.google.com/edge/mediapipe/solutions/text/text_embedder and https://developers.google.com/edge/mediapipe/solutions/text/text_embedder/android ; model files https://storage.googleapis.com/mediapipe-models/text_embedder/embedding_gemma/int4int8/latest/embedding_gemma.task and https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite
- **S14** LiteRT-LM embedding models and EmbeddingGemma 2 Text 270M card: https://developers.google.com/edge/litert-lm/embedding_models and https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm
- **S15** ONNX Runtime: Android install https://onnxruntime.ai/docs/install/ ; mobile flow and binary-size table https://onnxruntime.ai/docs/tutorials/mobile/ ; Java API https://onnxruntime.ai/docs/get-started/with-java.html ; Maven https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/maven-metadata.xml and https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-extensions-android/maven-metadata.xml ; AAR contents inspected from onnxruntime-android-1.31.0.aar; license MIT https://github.com/microsoft/onnxruntime
- **S16** Embedding model repos on Hugging Face: https://huggingface.co/intfloat/multilingual-e5-small , https://huggingface.co/Xenova/multilingual-e5-small , https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 , https://huggingface.co/onnx-community/embeddinggemma-300m-ONNX
- **S17** ML Kit Text Recognition v2: https://developers.google.com/ml-kit/vision/text-recognition/v2 and https://developers.google.com/ml-kit/vision/text-recognition/v2/android ; ML Kit home (on-device / offline statement): https://developers.google.com/ml-kit
- **S18** sklearn-onnx (Apache-2.0; `onnxmltools` for LightGBM/XGBoost): https://onnx.ai/sklearn-onnx/
- **S19** Repo docs: `docs/fmd/sdd-commutenity.md` §4 (parser contract), §7 (≤5 s warm), §8 (candidate list); `docs/fmd/build-commutenity.md` (CP1); `docs/fmd/JUDGING.md` (allowed frameworks); ticket [#8](https://github.com/geadlydrim/appbuildersph-hackathon/issues/8)
