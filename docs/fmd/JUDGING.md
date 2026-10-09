# Mechanics & Judging: AppBuildersPH Hackathon 2026

Source: official participant briefing (Oct 9, 2026). This file is the single reference for hackathon constraints. When a project decision conflicts with this file, this file wins.

## Theme: Local AI

Useful AI experiences where **meaningful AI computation happens on the user's device**, rather than depending entirely on cloud inference.

> This is different from building an AI product for a local audience.

**Challenge:** Build an AI product that remains genuinely useful when the cloud disappears.

**Ask:** Create a working product that uses AI running locally on a user's device to solve a real problem. Show why running AI locally creates an experience that would be difficult, expensive, slow, private, or impossible with a cloud-only approach.

### What counts as Local AI

- Local LLMs, local vision models, local speech recognition
- Local embeddings and RAG, local AI agents, local image generation
- Edge AI, offline AI, privacy-preserving AI
- Hybrid local + cloud systems
- AI on PCs, laptops, phones, or edge hardware

Cloud services may be used, but meaningful AI functionality must run locally.

**Product categories (any):** productivity, developer tools, accessibility, education, finance, gaming, disaster response, healthcare, creative tools, enterprise tools, computer vision, personal assistants, privacy tools, local agents.

## Rules


| Required                                                                       | Allowed                                   |
| ------------------------------------------------------------------------------ | ----------------------------------------- |
| Substantially built during the hackathon                                       | Existing open-source models and libraries |
| A meaningful part of AI inference executes locally                             | AI-assisted development                   |
| A working product, demonstrated                                                | Devin                                     |
| Models, APIs, frameworks, and major tools disclosed                            | Cloud APIs as **secondary** components    |
| Core Local AI functionality works without depending entirely on a cloud AI API |                                           |


**Results can be disputed (disqualification)** if a team is proven to have violated the rules, including:

- A pre-existing project
- External help from people outside the hackathon
- Fake benchmarks

**Reusing old code:** allowed only if the project is still substantially built during the hackathon. **All existing code and assets must be disclosed.**

### Example tools

Ollama, LM Studio, llama.cpp, MLX, ONNX, PyTorch, TensorFlow, WebGPU, Core ML, AMD ROCm, DirectML, Hugging Face. You can also use any open-source LLMs, vision models, and speech models. No specific model, framework, OS, or hardware is required. Mobile apps and hardware projects are allowed.

## Judging criteria


| Weight  | Criterion                  | Question                                                                 |
| ------- | -------------------------- | ------------------------------------------------------------------------ |
| **25%** | Problem &amp; Usefulness   | Does it solve a genuine problem for a clear target user?                 |
| **25%** | Local AI Implementation    | Is local inference fundamental, and does it give a meaningful advantage? |
| 20%     | Technical Execution        | Does it actually work, reliably enough for a live demonstration?         |
| 15%     | Innovation                 | Is it meaningfully different? Does Local AI enable something new?        |
| 15%     | Product &amp; Demo Quality | Is the UX usable, and is the live demonstration convincing?              |


Half the score is usefulness and how real the Local AI is.

## Submission

- **Deadline: 10:00 AM, Oct 10, 2026. No extensions.**
- **Where:** [https://cerebralvalley.ai/e/appbuildersph-hackathon-2026](https://cerebralvalley.ai/e/appbuildersph-hackathon-2026)
- **One submission per team, no edits or resubmits.** Check everything before you submit.
- **Code freeze at the deadline.** Judges review the repository as it was at 10:00 AM. Commits after the deadline don't count.
- **The GitHub repo must be public** by the deadline.
- **Live deployment is not required**, as long as the repo has instructions for judges to recreate the project.

### Checklist


| The project                             | The proof                                                                               | The disclosures             |
| --------------------------------------- | --------------------------------------------------------------------------------------- | --------------------------- |
| Project name                            | Demo video (\~1 min)                                                                    | Models used                 |
| Short description                       | X / LinkedIn video post URL (required; tag Devin / Cognition, include `#AppBuildersPH`) | Technologies and frameworks |
| Team members (names from official list) | What runs locally                                                                       | APIs and cloud services     |
| Public GitHub repository                | What requires internet                                                                  | Existing code and assets    |
|                                         |                                                                                         | AI development tools        |


**Every submission must answer:** *Why does this product benefit from running AI locally?*

## Demo Day pitch

- **Format:** 5 min pitch and live demo, then 3 min judge Q&amp;A (8 min per team).
- **Prioritize showing a working product** over a large number of slides.
- **Finalists:** 10–15 teams, announced 1:00 PM on Oct 10.
- **Attendance:** finalists must be **on site**. Someone from the team must pitch and answer Q&amp;A in person; remote pitching is not allowed.
- **Venue:** Wi-Fi, power, HDMI and USB-C are available. **Bring your own laptop to demo on.**
- **Judges:** 10, announced on Demo Day.
- **People's Choice:** audience members scan a QR code to vote.

## Implications for this project

- **Local inference is the core, not decoration.** The airplane-mode demo must work end to end.
- **Track for disclosure as we go:** every model, framework, API, cloud service, AI dev tool, and any reused code/assets (e.g. ideas or data model carried over from CommuteNity).
- **The README must include setup instructions** so judges can recreate the project without a live deployment.
- **Plan the ~1-min demo video and the X/LinkedIn post before 10:00 AM Oct 10.**
- **Mock data is allowed** (owner-reported organizer allowance; it isn't in the written briefing). Mix it with real data only if it's labelled `mock`, marked in the app, and disclosed. Never base a benchmark claim on mock data alone (see "fake benchmarks" under Rules).
- **The live demo runs on our Android phone, mirrored to the venue display** (e.g., scrcpy over USB to a laptop, then HDMI). Pre-download the models, bring a spare phone, and don't rely on venue Wi-Fi.