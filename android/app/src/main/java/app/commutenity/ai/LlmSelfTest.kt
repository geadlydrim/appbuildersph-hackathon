package app.commutenity.ai

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug-only on-device check of [LocalLlm]: runs the spike's ten questions (1 warm-up + 5 timed each)
 * and logs one line per question under the `LlmSelfTest` tag. Started from MainActivity only in
 * debuggable builds launched with the `llm_selftest` boolean extra.
 */
object LlmSelfTest {
    private const val TAG = "LlmSelfTest"
    private const val WARMUP = 1
    private const val TIMED = 5

    // Verbatim from the spike's QUESTIONS list (q1–q10).
    private val QUESTIONS = listOf(
        "q1" to "Paano pumunta sa Dela Rosa, Pio del Pilar mula sa Ayala Center?",
        "q2" to "Pa-Greenbelt ako galing Guadalupe, ano pinakamabilis?",
        "q3" to "What's the cheapest way from Pio del Pilar to Ayala Triangle?",
        "q4" to "frm RCBC pa-Pio del Pilar, ung walang lipat sana",
        "q5" to "Ito ba yung tamang jeep? Nakasulat sa karatula: PASAY - GUADALUPE",
        "q6" to "Uulan ba mamaya?",
        "q7" to "How do I get to Makati Med from Ayala Center?",
        "q8" to "Paano pumunta sa Poblacion?",
        "q9" to "Pinakamura papuntang Buendia galing Washington SyCip Park?",
        "q10" to "Is this the right bus? It says 'AYALA - FTI'",
    )

    fun run(llm: LocalLlm, scope: CoroutineScope) {
        val startedAt = SystemClock.elapsedRealtime()
        scope.launch {
            val status = llm.status.first { it !is LlmStatus.Loading }
            Log.i(TAG, "SELFTEST_STATUS $status after ${SystemClock.elapsedRealtime() - startedAt} ms")
            if (status is LlmStatus.Failed) return@launch
            for ((id, question) in QUESTIONS) {
                repeat(WARMUP) { llm.parse(question) }
                val millis = ArrayList<Long>(TIMED)
                var last: ParseResult? = null
                repeat(TIMED) {
                    val result = llm.parse(question)
                    millis += result.millis
                    last = result
                }
                val sorted = millis.sorted()
                Log.i(TAG, "$id median=${sorted[sorted.size / 2]} max=${sorted.last()} :: ${last?.output?.toJson()}")
            }
            Log.i(TAG, "SELFTEST_DONE")
        }
    }
}
