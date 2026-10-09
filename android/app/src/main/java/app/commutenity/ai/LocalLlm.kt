package app.commutenity.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "LocalLlm"

sealed interface LlmStatus {
    data object Loading : LlmStatus
    data object Ready : LlmStatus
    data class Failed(val code: String, val message: String) : LlmStatus
}

data class ParseResult(val output: ParserOutput, val rawLlm: String, val millis: Long)

/**
 * On-device question parser. Code classifies the question (see [HybridParser]); the LLM only copies
 * place names or signboard text out of it.
 *
 * LiteRT-LM is not documented as thread-safe, so every engine and conversation call runs on one
 * dedicated thread. [engine] and [ready] are only touched from that thread.
 */
@OptIn(ExperimentalApi::class)
class LocalLlm(context: Context, private val modelPath: String = AiConfig.MODEL_PATH) {
    private val cacheDir = context.applicationContext.cacheDir.path
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "local-llm") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val llmScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _status = MutableStateFlow<LlmStatus>(LlmStatus.Loading)
    val status: StateFlow<LlmStatus> = _status.asStateFlow()

    private val started = AtomicBoolean(false)
    private val parseLock = Mutex()

    private var engine: Engine? = null
    /** The conversation whose system prompt is already prefilled, waiting for the next question. */
    private var ready: Conversation? = null

    /** Loads the engine on a background thread and pre-creates the first conversation. Idempotent. */
    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        scope.launch(dispatcher) { load() }
    }

    private fun load() {
        if (!File(modelPath).canRead()) {
            Log.e(TAG, "Model not readable: $modelPath")
            _status.value = LlmStatus.Failed("MODEL_LOAD_FAILED", "AI model file not found on this phone.")
            return
        }
        try {
            val t0 = System.nanoTime()
            val e = Engine(EngineConfig(modelPath = modelPath, backend = Backend.GPU(), cacheDir = cacheDir))
            e.initialize()
            engine = e
            ready = newConversation(e)
            Log.i(TAG, "Loaded in ${(System.nanoTime() - t0) / 1_000_000} ms")
            _status.value = LlmStatus.Ready
        } catch (t: Throwable) {
            Log.e(TAG, "Model load failed", t)
            _status.value = LlmStatus.Failed("MODEL_LOAD_FAILED", "The AI model could not be loaded on this phone.")
        }
    }

    private fun newConversation(e: Engine): Conversation = e.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(ParserCues.SYSTEM_PROMPT),
            samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
            maxOutputToken = AiConfig.MAX_OUTPUT_TOKENS,
            enableResponseFormat = true,
            prefillPrefaceOnInit = true,
        ),
    )

    /** Must run on [dispatcher]. */
    private fun precreate() {
        val e = engine ?: return
        if (ready != null) return
        try {
            ready = newConversation(e)
        } catch (t: Throwable) {
            // parse() creates one on demand if this failed.
            Log.w(TAG, "Pre-creating conversation failed", t)
        }
    }

    /**
     * Suspends until the model is loaded (throws [IllegalStateException] if it failed). Calls are
     * serialized. millis covers send + decode + assemble only, not conversation creation.
     */
    suspend fun parse(question: String): ParseResult {
        val state = status.first { it !is LlmStatus.Loading }
        if (state is LlmStatus.Failed) throw IllegalStateException(state.message)
        return parseLock.withLock {
            withContext(dispatcher) {
                val e = checkNotNull(engine) { "AI model is not loaded." }
                val conv = ready ?: newConversation(e)
                ready = null
                try {
                    val t0 = System.nanoTime()
                    val fields = HybridParser.fieldsFor(question)
                    val raw = conv.sendMessage(
                        question,
                        responseFormat = ResponseFormat.json(HybridParser.schemaFor(fields)),
                    ).toString()
                    val output = HybridParser.assemble(question, extract(raw, fields))
                    ParseResult(output, raw, (System.nanoTime() - t0) / 1_000_000)
                } finally {
                    conv.close()
                    llmScope.launch { precreate() }
                }
            }
        }
    }

    /** Bad model output yields all-null fields; it never throws. */
    private fun extract(raw: String, fields: List<String>): Map<String, String?> {
        val json = try {
            JSONObject(raw.trim())
        } catch (_: JSONException) {
            return fields.associateWith { null }
        }
        return fields.associateWith { k -> if (json.isNull(k)) null else json.opt(k) as? String }
    }

    fun close() {
        executor.execute {
            runCatching { ready?.close() }
            ready = null
            runCatching { engine?.close() }
            engine = null
        }
        executor.shutdown()
    }
}
