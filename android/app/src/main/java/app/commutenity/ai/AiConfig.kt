package app.commutenity.ai

object AiConfig {
    /** Pushed with `adb push` to the documented LiteRT path; the app cannot see files pushed into its own Android/data dir. */
    const val MODEL_PATH = "/data/local/tmp/llm/gemma-4-E2B-it.litertlm"
    const val MAX_OUTPUT_TOKENS = 64
}
