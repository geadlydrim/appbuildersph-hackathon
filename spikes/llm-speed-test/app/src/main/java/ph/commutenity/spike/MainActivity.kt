// PROTOTYPE: throwaway harness for "Which LLM and runtime pass the speed test on the demo phone?".
// Not the product. Run it with ../../run.sh; results land in <external files>/results/*.json.
package ph.commutenity.spike

import android.app.Activity
import android.app.ActivityManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.ceil

private const val TAG = "SPIKE"

/**
 * Hand-labelled test question. Accepted strings are compared lowercase and trimmed.
 * Places are test inputs only (Makati, D20/D30); no route facts are claimed.
 */
private data class Question(
    val id: String,
    val kind: String,
    val text: String,
    val intent: String,
    val origin: Set<String>?,
    val destination: Set<String>?,
    val preference: String?,
    val vehicleText: Set<String>? = null,
)

private val QUESTIONS = listOf(
    Question("q1", "pure Tagalog (hero pair)", "Paano pumunta sa Dela Rosa, Pio del Pilar mula sa Ayala Center?",
        "trip", setOf("ayala center"), setOf("dela rosa, pio del pilar", "dela rosa"), null),
    Question("q2", "Taglish + pa- prefix + fastest", "Pa-Greenbelt ako galing Guadalupe, ano pinakamabilis?",
        "trip", setOf("guadalupe"), setOf("greenbelt"), "fastest"),
    Question("q3", "English, cheapest", "What's the cheapest way from Pio del Pilar to Ayala Triangle?",
        "trip", setOf("pio del pilar"), setOf("ayala triangle"), "cheapest"),
    Question("q4", "typo + abbreviation + fewest transfers", "frm RCBC pa-Pio del Pilar, ung walang lipat sana",
        "trip", setOf("rcbc"), setOf("pio del pilar"), "fewest_transfers"),
    Question("q5", "correct-vehicle check (D27)", "Ito ba yung tamang jeep? Nakasulat sa karatula: PASAY - GUADALUPE",
        "vehicle_check", null, null, null, setOf("pasay - guadalupe")),
    Question("q6", "non-trip", "Uulan ba mamaya?",
        "other", null, null, null),
    // Added before the hybrid parser was run, as unseen checks against overfitting the rules.
    Question("q7", "English landmark, no preference", "How do I get to Makati Med from Ayala Center?",
        "trip", setOf("ayala center"), setOf("makati med"), null),
    Question("q8", "Tagalog, destination only", "Paano pumunta sa Poblacion?",
        "trip", null, setOf("poblacion"), null),
    Question("q9", "Taglish cheapest, papuntang", "Pinakamura papuntang Buendia galing Washington SyCip Park?",
        "trip", setOf("washington sycip park"), setOf("buendia"), "cheapest"),
    Question("q10", "English vehicle check", "Is this the right bus? It says 'AYALA - FTI'",
        "vehicle_check", null, null, null, setOf("ayala - fti")),
)

/** Ticket rule (≥ 4/5 exact origin and destination) scaled to the question count: ≥ 80 %, rounded up. */
private val OD_PASS = kotlin.math.ceil(0.8 * QUESTIONS.size).toInt()

private const val PARSER_SYSTEM = """You turn Metro Manila commute questions (English, Tagalog, or Taglish) into JSON.
intent: "trip" when the rider asks how to get from one place to another; "vehicle_check" when the rider asks if a jeep, bus, or van is the right one (copy its signboard text to vehicle_text); otherwise "other".
Copy place names as the rider wrote them; use null when missing.
preference: "cheapest" (mura, tipid), "fastest" (mabilis), "fewest_transfers" (walang/konting lipat), else null. Reply with JSON only.
Q: Paano pumunta sa Cubao galing Fairview, yung mura lang?
A: {"intent":"trip","origin":"Fairview","destination":"Cubao","preference":"cheapest","vehicle_text":null}
Q: Tama ba 'tong bus? QUIAPO - CUBAO nakalagay
A: {"intent":"vehicle_check","origin":null,"destination":null,"preference":null,"vehicle_text":"QUIAPO - CUBAO"}
Q: Saan masarap kumain?
A: {"intent":"other","origin":null,"destination":null,"preference":null,"vehicle_text":null}"""

private const val PARSER_SCHEMA = """{"type":"object","properties":{
"intent":{"type":"string","enum":["trip","vehicle_check","other"]},
"origin":{"type":["string","null"]},
"destination":{"type":["string","null"]},
"preference":{"enum":["cheapest","fastest","fewest_transfers",null]},
"vehicle_text":{"type":["string","null"]}},
"required":["intent","origin","destination","preference","vehicle_text"],"additionalProperties":false}"""

// ---- Hybrid parser: code classifies, the LLM only copies spans out of the question. ----

private const val HYBRID_SYSTEM = """You copy place names and signboard text out of Metro Manila commute questions (English, Tagalog, or Taglish). Reply with JSON only.
Trip question: origin is where the rider starts (after "galing", "mula sa", "from"); destination is where they are going (after "sa", "papunta", "pa-", "to"). Use null for a place the question does not name.
Vehicle question: vehicle_text is the signboard or route text the rider read.
Q: Paano pumunta sa Cubao galing Fairview?
A: {"origin":"Fairview","destination":"Cubao"}
Q: Saan masarap kumain?
A: {"origin":null,"destination":null}
Q: Tama ba 'tong bus? QUIAPO - CUBAO nakalagay
A: {"vehicle_text":"QUIAPO - CUBAO"}"""

private val IGNORE = setOf(RegexOption.IGNORE_CASE)

private val VEHICLE_CUES = Regex(
    """\b(tama ba|tamang|ito ba|eto ba|right (jeep|jeepney|bus|van)|correct (jeep|jeepney|bus|van|vehicle)|karatula|signboard|nakasulat|nakalagay|it says)\b""",
    IGNORE,
)

/** First match wins, so the more specific transfer cue goes first. */
private val PREFERENCE_CUES = listOf(
    "fewest_transfers" to Regex("""\b(walang lipat|walang transfer|konting lipat|isang sakay|diretso|direct|no transfers?|fewest transfers?|less transfers?)\b""", IGNORE),
    "cheapest" to Regex("""\b(pinakamura|mura|tipid|cheap|cheapest|cheaper|least expensive)\b""", IGNORE),
    "fastest" to Regex("""\b(pinakamabilis|mabilis|bilis|fastest|quickest|fast)\b""", IGNORE),
)

private fun preferenceOf(q: String): String? = PREFERENCE_CUES.firstOrNull { it.second.containsMatchIn(q) }?.first

// Enum-of-spans constraints were tried first and failed: the decoder closed every string after its
// first word ("Ayala" is itself a valid span). So the schema allows free strings and code checks the copy.
private val FREE_STRING = JSONObject().put("type", JSONArray().put("string").put("null"))

private fun fieldSchema(fields: List<String>): String {
    val props = JSONObject().apply { fields.forEach { put(it, FREE_STRING) } }
    return JSONObject()
        .put("type", "object").put("properties", props)
        .put("required", JSONArray(fields)).put("additionalProperties", false)
        .toString()
}

private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/**
 * Keeps an extracted value only if the rider actually wrote it: edge punctuation and a "pa-" prefix are
 * stripped, then the value must appear word-aligned in the question. Anything else becomes null, so an
 * invented place can never reach place search.
 */
private fun copied(value: String?, question: String): String? {
    if (value == null) return null
    var v = value.trim { !it.isLetterOrDigit() }
    if (v.length > 3 && v.startsWith("pa-", ignoreCase = true)) v = v.substring(3)
    val nv = norm(v)
    if (nv.isEmpty()) return null
    return v.takeIf { " ${norm(question)} ".contains(" $nv ") }
}

/** Runs the hybrid parser and returns (SDD §4 JSON text, raw LLM text). */
private fun hybridParse(conv: com.google.ai.edge.litertlm.Conversation, q: String): Pair<String, String> {
    val vehicle = VEHICLE_CUES.containsMatchIn(q)
    val fields = if (vehicle) listOf("vehicle_text") else listOf("origin", "destination")
    val raw = conv.sendMessage(q, responseFormat = ResponseFormat.json(fieldSchema(fields))).toString()
    val llm = JSONObject(raw.trim())
    fun field(k: String): String? = copied(if (llm.isNull(k)) null else llm.getString(k), q)
    val out = JSONObject()
    if (vehicle) {
        out.put("intent", "vehicle_check").put("origin", JSONObject.NULL).put("destination", JSONObject.NULL)
            .put("preference", JSONObject.NULL).put("vehicle_text", field("vehicle_text") ?: JSONObject.NULL)
    } else {
        val destination = field("destination")
        // A trip cannot start where it ends; the model echoes the one place it found into both fields.
        val origin = field("origin")?.takeUnless { destination != null && norm(it) == norm(destination) }
        val isTrip = origin != null || destination != null
        out.put("intent", if (isTrip) "trip" else "other")
            .put("origin", origin ?: JSONObject.NULL).put("destination", destination ?: JSONObject.NULL)
            .put("preference", if (isTrip) preferenceOf(q) ?: JSONObject.NULL else JSONObject.NULL)
            .put("vehicle_text", JSONObject.NULL)
    }
    return out.toString() to raw
}

private const val PHRASE_SYSTEM = """You are CommuteNity, a Metro Manila commute helper. Turn the trip facts into a short, friendly Taglish answer of 2 to 3 sentences.
Use only the given facts. Never add stops, routes, fares, or times. Say where to board, the signboard to look for, and where to say "para"."""

// FIXTURE for the phrasing test only. The fare and minutes are placeholders, not real data.
private const val PHRASE_INPUT = """{"origin":"Malanday","destination":"Recto","best_trip":{"legs":[{"mode":"jeepney","board":"Malanday","signboard":"RECTO","alight":"Recto","fare_php":20,"minutes":55}],"transfers":0,"total_fare_php":20,"total_minutes":55},"reason":"fewest transfers: one direct ride"}"""
private val PHRASE_ALLOWED_NUMBERS = setOf("20", "55", "0", "1")

private data class RunConfig(
    val model: String,
    val backend: String,
    val constrained: Boolean,
    val iters: Int,
    val warmup: Int,
    val phrase: Boolean,
    val thinkOff: Boolean,
    val threads: Int,
    /** Create the conversation (system prompt prefilled) before the question arrives; time only the send. */
    val prewarm: Boolean,
    /** Keyword rules + span-constrained LLM extraction instead of the one-shot parser. */
    val hybrid: Boolean,
) {
    val tag get() = "${model.substringBeforeLast('.')}-$backend-${if (constrained) "json" else "free"}" +
        (if (threads > 0) "-t$threads" else "") + (if (prewarm) "-prewarm" else "") + (if (hybrid) "-hybrid" else "")
}

class MainActivity : Activity() {
    private lateinit var out: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(24, 24, 24, 24)
        }
        setContentView(ScrollView(this).apply { addView(out) })

        val model = intent.getStringExtra("model")
        if (model == null) {
            log("No model extra. Launch with run.sh.")
            return
        }
        val cfg = RunConfig(
            model = model,
            backend = intent.getStringExtra("backend") ?: "cpu",
            constrained = intent.getBooleanExtra("constrained", true),
            iters = intent.getIntExtra("iters", 20),
            warmup = intent.getIntExtra("warmup", 2),
            phrase = intent.getBooleanExtra("phrase", true),
            thinkOff = intent.getBooleanExtra("thinkoff", false),
            threads = intent.getIntExtra("threads", 0),
            prewarm = intent.getBooleanExtra("prewarm", false),
            hybrid = intent.getBooleanExtra("hybrid", false),
        )
        thread(name = "spike") {
            // App-created dir: a dir made by `adb shell mkdir` is not writable by the app (EACCES).
            val runsDir = File(getExternalFilesDir(null), "runs").apply { mkdirs() }
            try {
                val result = run(cfg)
                val file = File(runsDir, "${cfg.tag}.json")
                file.writeText(result.toString(2))
                log("SPIKE_DONE ${file.name}")
            } catch (t: Throwable) {
                log("SPIKE_ERROR ${Log.getStackTraceString(t)}")
                runCatching { File(runsDir, "${cfg.tag}.error.txt").writeText(Log.getStackTraceString(t)) }
            }
        }
    }

    private fun log(line: String) {
        Log.i(TAG, line)
        runOnUiThread { out.append(line + "\n") }
    }

    @OptIn(ExperimentalApi::class)
    private fun run(cfg: RunConfig): JSONObject {
        // Pushed by run.sh to /data/local/tmp/llm (the documented LiteRT path). The app cannot see
        // files that `adb push` writes into its own Android/data dir.
        val modelFile = File("/data/local/tmp/llm", cfg.model)
        require(modelFile.canRead()) { "Model not readable: ${modelFile.path}" }
        val result = JSONObject()
        result.put("config", JSONObject().apply {
            put("model", cfg.model); put("model_bytes", modelFile.length()); put("backend", cfg.backend)
            put("constrained", cfg.constrained); put("iters", cfg.iters); put("warmup", cfg.warmup)
            put("think_off", cfg.thinkOff); put("threads", cfg.threads); put("max_output_tokens", 64)
            put("runtime", "LiteRT-LM litertlm-android 0.18.0")
        })
        result.put("device", deviceInfo())
        log("${cfg.tag} on ${Build.MANUFACTURER} ${Build.MODEL} / ${Build.SOC_MODEL}")

        ExperimentalFlags.enableBenchmark = true
        val backend = when (cfg.backend) {
            "gpu" -> Backend.GPU()
            else -> Backend.CPU(threadCount = cfg.threads.takeIf { it > 0 })
        }
        val engine = Engine(EngineConfig(modelPath = modelFile.path, backend = backend, cacheDir = cacheDir.path))
        val loadMs = timeMs { engine.initialize() }
        result.put("load_ms", loadMs)
        log("load: ${"%.0f".format(loadMs)} ms")

        val sampler = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
        val thinking = if (cfg.thinkOff) ThinkingConfig(enableThinking = false) else null
        val parserConfig = ConversationConfig(
            systemInstruction = Contents.of(if (cfg.hybrid) HYBRID_SYSTEM else PARSER_SYSTEM),
            samplerConfig = sampler,
            maxOutputToken = 64,
            thinkingConfig = thinking,
            enableResponseFormat = cfg.constrained || cfg.hybrid,
            prefillPrefaceOnInit = cfg.prewarm,
        )
        val format = if (cfg.constrained) ResponseFormat.json(PARSER_SCHEMA) else null

        val questions = JSONArray()
        var valid = 0
        var odExact = 0
        var allLatencies = mutableListOf<Double>()
        for (q in QUESTIONS) {
            val latencies = mutableListOf<Double>()
            val createMs = mutableListOf<Double>()
            val benches = JSONArray()
            val outputs = linkedSetOf<String>()
            var validRuns = 0
            var firstText = ""
            var firstRaw = ""
            repeat(cfg.warmup + cfg.iters) { i ->
                val text: String
                val ms: Double
                val tc = System.nanoTime()
                engine.createConversation(parserConfig).use { conv ->
                    val created = System.nanoTime()
                    val t0 = if (cfg.prewarm) created else tc
                    val raw: String
                    if (cfg.hybrid) {
                        val (assembled, llmText) = hybridParse(conv, q.text)
                        text = assembled; raw = llmText
                    } else {
                        text = conv.sendMessage(q.text, responseFormat = format).toString(); raw = text
                    }
                    val ok = parse(text) != null // validation is inside the timed region
                    ms = (System.nanoTime() - t0) / 1e6
                    if (i >= cfg.warmup) {
                        latencies += ms
                        createMs += (created - tc) / 1e6
                        if (ok) validRuns++
                        if (firstRaw.isEmpty()) firstRaw = raw
                        outputs += text
                        if (firstText.isEmpty()) firstText = text
                        runCatching { conv.getBenchmarkInfo() }.getOrNull()?.let { b ->
                            benches.put(JSONObject().apply {
                                put("ttft_s", num(b.timeToFirstTokenInSecond))
                                put("prefill_tokens", b.lastPrefillTokenCount)
                                put("decode_tokens", b.lastDecodeTokenCount)
                                put("prefill_tps", num(b.lastPrefillTokensPerSecond))
                                put("decode_tps", num(b.lastDecodeTokensPerSecond))
                            })
                        }
                    }
                }
            }
            val parsed = parse(firstText)
            val score = score(q, parsed)
            if (validRuns == cfg.iters) valid++
            if (score.getBoolean("od_exact")) odExact++
            allLatencies += latencies
            val med = percentile(latencies, 0.5)
            val p95 = percentile(latencies, 0.95)
            questions.put(JSONObject().apply {
                put("id", q.id); put("kind", q.kind); put("question", q.text)
                put("output", firstText); put("llm_raw", firstRaw); put("distinct_outputs", outputs.size)
                put("valid_runs", validRuns); put("score", score)
                put("median_ms", med); put("p95_ms", p95)
                put("latencies_ms", JSONArray(latencies))
                put("create_conversation_median_ms", percentile(createMs, 0.5))
                put("bench_first", benches.optJSONObject(0))
                put("decode_tps_median", num(percentile(benchList(benches, "decode_tps"), 0.5)))
                put("prefill_tps_median", num(percentile(benchList(benches, "prefill_tps"), 0.5)))
            })
            log("${q.id} med ${"%.0f".format(med)} p95 ${"%.0f".format(p95)} ms (create ${"%.0f".format(percentile(createMs, 0.5))}) valid $validRuns/${cfg.iters} od=${score.getBoolean("od_exact")} pref=${score.getBoolean("preference_ok")} :: $firstText")
        }
        val worstP95 = (0 until questions.length()).maxOf { questions.getJSONObject(it).getDouble("p95_ms") }
        val pass = worstP95 <= 5000 && valid == QUESTIONS.size && odExact >= OD_PASS
        result.put("questions", questions)
        result.put("summary", JSONObject().apply {
            put("worst_question_p95_ms", worstP95)
            put("overall_median_ms", percentile(allLatencies, 0.5))
            put("valid_json_questions", valid)
            put("od_exact_questions", odExact)
            put("pass", pass)
        })
        log("SUMMARY worst p95 ${"%.0f".format(worstP95)} ms, valid $valid/${QUESTIONS.size}, exact $odExact/${QUESTIONS.size} (need $OD_PASS), PASS=$pass")

        if (cfg.phrase) result.put("phrasing", phrase(engine, sampler, thinking))

        result.put("thermal_after", thermal())
        result.put("pss_kb_after", Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss)
        engine.close()
        return result
    }

    private fun phrase(engine: Engine, sampler: SamplerConfig, thinking: ThinkingConfig?): JSONObject {
        val config = ConversationConfig(
            systemInstruction = Contents.of(PHRASE_SYSTEM),
            samplerConfig = sampler,
            maxOutputToken = 128,
            thinkingConfig = thinking,
        )
        val latencies = mutableListOf<Double>()
        var text = ""
        repeat(4) { i ->
            val t0 = System.nanoTime()
            engine.createConversation(config).use { conv ->
                text = conv.sendMessage(PHRASE_INPUT).toString().trim()
            }
            if (i > 0) latencies += (System.nanoTime() - t0) / 1e6
        }
        val numbers = Regex("\\d+").findAll(text).map { it.value }.toSet()
        val unknown = numbers - PHRASE_ALLOWED_NUMBERS
        log("PHRASE med ${"%.0f".format(percentile(latencies, 0.5))} ms, unknown numbers $unknown :: $text")
        return JSONObject().apply {
            put("input", PHRASE_INPUT); put("output", text)
            put("median_ms", percentile(latencies, 0.5)); put("latencies_ms", JSONArray(latencies))
            put("numbers_not_in_facts", JSONArray(unknown.toList()))
        }
    }

    private fun deviceInfo() = JSONObject().apply {
        val mem = ActivityManager.MemoryInfo().also { (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
        put("manufacturer", Build.MANUFACTURER); put("model", Build.MODEL); put("device", Build.DEVICE)
        put("soc_manufacturer", Build.SOC_MANUFACTURER); put("soc_model", Build.SOC_MODEL)
        put("sdk", Build.VERSION.SDK_INT); put("total_ram_mb", mem.totalMem / (1024 * 1024))
        put("avail_ram_mb", mem.availMem / (1024 * 1024)); put("cpus", Runtime.getRuntime().availableProcessors())
        put("thermal_before", thermal())
    }

    private fun thermal() = JSONObject().apply {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        put("status", pm.currentThermalStatus)
        put("headroom_10s", num(pm.getThermalHeadroom(10).toDouble()))
    }
}

private inline fun timeMs(block: () -> Unit): Double {
    val t0 = System.nanoTime()
    block()
    return (System.nanoTime() - t0) / 1e6
}

/** Strict SDD §4 parser contract check. Returns null when the output is not valid. */
private fun parse(text: String): JSONObject? = runCatching {
    val o = JSONObject(text.trim())
    val keys = o.keys().asSequence().toSet()
    if (keys != setOf("intent", "origin", "destination", "preference", "vehicle_text")) return null
    if (o.get("intent") !in setOf("trip", "vehicle_check", "other")) return null
    if (!(o.isNull("vehicle_text") || o.get("vehicle_text") is String)) return null
    for (k in listOf("origin", "destination")) if (!(o.isNull(k) || o.get(k) is String)) return null
    if (!(o.isNull("preference") || o.get("preference") in setOf("cheapest", "fastest", "fewest_transfers"))) return null
    o
}.getOrNull()

private fun score(q: Question, parsed: JSONObject?): JSONObject {
    fun place(k: String) = parsed?.takeUnless { it.isNull(k) }?.getString(k)?.trim()?.lowercase()
    fun matches(actual: String?, accepted: Set<String>?) = if (accepted == null) actual == null else actual in accepted
    val originOk = parsed != null && matches(place("origin"), q.origin)
    val destOk = parsed != null && matches(place("destination"), q.destination)
    val pref = parsed?.takeUnless { it.isNull("preference") }?.getString("preference")
    val intentOk = parsed?.optString("intent") == q.intent
    val vehicleOk = parsed != null && matches(place("vehicle_text"), q.vehicleText)
    return JSONObject().apply {
        put("valid", parsed != null)
        put("intent_ok", intentOk)
        put("origin_ok", originOk); put("destination_ok", destOk); put("vehicle_text_ok", vehicleOk)
        // Stricter than the ticket's "origin and destination": intent and vehicle_text must match too.
        put("od_exact", intentOk && originOk && destOk && vehicleOk)
        put("preference_ok", parsed != null && pref == q.preference)
    }
}

private fun benchList(a: JSONArray, key: String) =
    (0 until a.length()).map { a.getJSONObject(it).optDouble(key) }.filter { it.isFinite() }

/** org.json rejects NaN and Infinity. */
private fun num(v: Double): Any = if (v.isFinite()) v else JSONObject.NULL

/** Nearest-rank percentile. */
private fun percentile(values: List<Double>, p: Double): Double {
    if (values.isEmpty()) return Double.NaN
    val sorted = values.sorted()
    return sorted[(ceil(p * sorted.size).toInt() - 1).coerceIn(0, sorted.size - 1)]
}
