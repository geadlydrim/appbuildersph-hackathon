package app.commutenity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import app.commutenity.voice.OnDeviceSpeech
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.commutenity.data.sample.SampleQuestions

import app.commutenity.domain.HomeEvent
import app.commutenity.domain.HomeState
import app.commutenity.domain.QaEvent
import app.commutenity.domain.reduce
import app.commutenity.domain.QaRoute
import app.commutenity.domain.QaTrip
import app.commutenity.domain.TripResult
import app.commutenity.domain.canOpenTrip
import app.commutenity.domain.evidenceFor
import app.commutenity.domain.orderingVotes
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.QaCandidate
import app.commutenity.domain.QaState
import app.commutenity.domain.Trip
import app.commutenity.domain.TripPreference
import app.commutenity.domain.TripSource
import app.commutenity.domain.Field
import app.commutenity.domain.SearchRow
import app.commutenity.data.qa.RiderQaLoader
import android.content.Context
import android.util.Log
import app.commutenity.domain.reduceQa
import app.commutenity.ui.home.HomeMenuDrawer
import app.commutenity.ui.home.MapHomeScreen
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.commutenity.ui.qa.QaScreen
import app.commutenity.ui.theme.CommuteNityTheme
import android.content.pm.ApplicationInfo
import androidx.lifecycle.lifecycleScope
import app.commutenity.ai.LlmSelfTest
import app.commutenity.ai.LlmStatus
import app.commutenity.ai.LocalLlm
import app.commutenity.data.pack.PackTripSource
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    private val llm by lazy { LocalLlm(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val voteContext = VoteContext()
        val source: TripSource = VoteAwareSource(
            PackTripSource.fromAssets(applicationContext) { key ->
                voteContext.questions().orderingVotes(key, heroPair = voteContext.heroPair)
            },
            voteContext,
        )
        val initialQuestions = loadQuestions(applicationContext)
        llm.start(lifecycleScope)
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent.getBooleanExtra("llm_selftest", false)) {
            LlmSelfTest.run(llm, lifecycleScope)
        }
        setContent {
            CommuteNityTheme {
                var state by remember { mutableStateOf(HomeState()) }
                var questions by remember { mutableStateOf(initialQuestions) }
                voteContext.questions = { questions }
                val context = LocalContext.current
                val speech = remember { OnDeviceSpeech(context) }
                DisposableEffect(Unit) { onDispose { speech.destroy() } }
                val voiceAvailable = remember { OnDeviceSpeech.isAvailable(context) }
                // Recognizer callbacks arrive later; `state` is a delegate so this always reduces the latest value.
                fun dispatch(event: HomeEvent) {
                    state = reduce(state, event, source)
                }
                fun startVoice() {
                    dispatch(HomeEvent.VoiceStart)
                    speech.start(
                        onPartial = { dispatch(HomeEvent.VoicePartial(it)) },
                        onResult = { dispatch(HomeEvent.VoiceResult(it)) },
                        onError = { dispatch(HomeEvent.VoiceError(it)) },
                    )
                }
                val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        startVoice()
                    } else {
                        dispatch(HomeEvent.VoiceError("Microphone permission is off. You can still type your question."))
                    }
                }
                BackHandler(enabled = questions.open || state.asking || state.activeField != null) {
                    when {
                        questions.threadId != null -> questions = reduceQa(questions, QaEvent.BackToList)
                        questions.asking -> questions = reduceQa(questions, QaEvent.CancelDraft)
                        questions.open -> questions = reduceQa(questions, QaEvent.Close)
                        state.asking -> {
                            if (state.listening) speech.cancel()
                            state = reduce(state, HomeEvent.CloseAsk, source)
                        }
                        state.activeField != null -> state = reduce(state, HomeEvent.DismissSearch, source)
                    }
                }
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val llmStatus by llm.status.collectAsState()
                val aiStatusText = when (llmStatus) {
                    LlmStatus.Loading -> "AI loading… (about 30 s after opening)"
                    LlmStatus.Ready -> "On-device AI ready"
                    is LlmStatus.Failed -> "AI unavailable; simple matching"
                }
                val origin = state.origin
                val destination = state.destination
                val shownTrip = if (canOpenTrip(state) && origin != null && destination != null) {
                    (source.resolve(origin, destination, state.preference) as? TripResult.Ready)?.let {
                        QaTrip(it.trip.key, QaRoute(origin.id, destination.id, origin.name, destination.name))
                    }
                } else {
                    null
                }
                fun openCandidates(): List<QaCandidate> =
                    if (origin != null && destination != null) {
                        source.candidates(origin, destination, state.preference).map { QaCandidate(it.key, tripLabel(it)) }
                    } else {
                        emptyList()
                    }
                Box(Modifier.fillMaxSize()) {
                    HomeMenuDrawer(
                        drawerState = drawerState,
                        onOpenQuestions = { questions = reduceQa(questions, QaEvent.Open(shownTrip, candidates = openCandidates())) },
                    ) {
                        MapHomeScreen(
                            state = state,
                            source = source,
                            aiStatus = aiStatusText,
                            onEvent = { event: HomeEvent ->
                                if (state.listening && (event == HomeEvent.CloseAsk || event == HomeEvent.SubmitAsk)) speech.cancel()
                                if (event != HomeEvent.SubmitAsk) {
                                    dispatch(event)
                                } else {
                                    val draft = state.askDraft
                                    when {
                                        draft.isBlank() -> dispatch(event)
                                        state.thinking -> Unit
                                        llmStatus is LlmStatus.Ready -> {
                                            dispatch(HomeEvent.AskThinking)
                                            lifecycleScope.launch {
                                                try {
                                                    val result = llm.parse(draft)
                                                    dispatch(HomeEvent.AskParsed(result.output))
                                                } catch (e: Exception) {
                                                    dispatch(HomeEvent.AskFailed("The AI couldn't read that. Try again or pick on the map."))
                                                }
                                            }
                                        }
                                        llmStatus is LlmStatus.Loading ->
                                            dispatch(HomeEvent.AskFailed("AI is still loading (about 30 s after opening). Try again in a moment."))
                                        else -> dispatch(event)
                                    }
                                }
                            },
                            onOpenQuestions = {
                                questions = reduceQa(questions, QaEvent.Open(shownTrip, onlyTrip = true, candidates = openCandidates()))
                            },
                            workedCount = shownTrip?.let { questions.evidenceFor(it.key) } ?: 0,
                            onMenu = { scope.launch { drawerState.open() } },
                            onMic = if (voiceAvailable) {
                                {
                                    if (state.listening) {
                                        speech.stop()
                                        dispatch(HomeEvent.VoiceStop)
                                    } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startVoice()
                                    } else {
                                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                    if (questions.open) {
                        QaScreen(
                            state = questions,
                            onEvent = { event: QaEvent -> questions = reduceQa(questions, event) },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) llm.close()
        super.onDestroy()
    }
}

/** What the trip finder may read when it asks for votes: the latest Q&A state and whether the pair being resolved is the hero pair. */
private class VoteContext {
    var questions: () -> QaState = { QaState() }
    var heroPair: Boolean = false
}

/**
 * Marks each resolve/candidates call with its own pair before the finder asks for votes, so the hero rule (D13)
 * follows the pair being resolved, even while a reduction is still computing the next state.
 */
private class VoteAwareSource(private val inner: TripSource, private val context: VoteContext) : TripSource by inner {
    override fun resolve(origin: Place, destination: Place, preference: TripPreference): TripResult {
        context.heroPair = isHeroPair(origin, destination)
        return inner.resolve(origin, destination, preference)
    }

    override fun candidates(origin: Place, destination: Place, preference: TripPreference): List<Trip> {
        context.heroPair = isHeroPair(origin, destination)
        return inner.candidates(origin, destination, preference)
    }

    private fun isHeroPair(origin: Place, destination: Place) =
        origin.id == PackTripSource.HERO_ORIGIN_ID && destination.id == PackTripSource.HERO_DESTINATION_ID
}

/** Short label for an answer chip: ride kind, fare and time straight from the trip. */
private fun tripLabel(trip: Trip): String {
    val mode = trip.legs.filterIsInstance<Leg.Ride>().firstOrNull()?.mode?.lowercase().orEmpty()
    val kind = when {
        mode.startsWith("jeep") -> "Jeep"
        mode.startsWith("bus") -> "Bus"
        else -> "Ride"
    }
    return "$kind ${trip.fare} · ${trip.minutes}"
}

/** Rider answers need the same candidate keys the finder produces, so load them against a vote-free source. */
private fun loadQuestions(context: Context): QaState = try {
    val plain = PackTripSource.fromAssets(context)
    val places = plain.search(Field.B, "").filterIsInstance<SearchRow.PlaceRow>().associate { it.place.id to it.place }
    RiderQaLoader.fromAssets(context) { fromId, toId ->
        val from = places[fromId]
        val to = places[toId]
        if (from == null || to == null) emptySet() else plain.candidates(from, to).map { it.key }.toSet()
    }
} catch (e: Exception) {
    Log.w("MainActivity", "Rider answers did not load; using sample questions", e)
    SampleQuestions.initial()
}
