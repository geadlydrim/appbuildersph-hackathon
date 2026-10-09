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
import app.commutenity.data.sample.SampleTripSource
import app.commutenity.domain.HomeEvent
import app.commutenity.domain.HomeState
import app.commutenity.domain.QaEvent
import app.commutenity.domain.reduce
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
import app.commutenity.ai.LocalLlm

class MainActivity : ComponentActivity() {
    private val llm by lazy { LocalLlm(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val source = SampleTripSource()
        llm.start(lifecycleScope)
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent.getBooleanExtra("llm_selftest", false)) {
            LlmSelfTest.run(llm, lifecycleScope)
        }
        setContent {
            CommuteNityTheme {
                var state by remember { mutableStateOf(HomeState()) }
                var questions by remember { mutableStateOf(SampleQuestions.initial()) }
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
                val openQuestions = { questions = reduceQa(questions, QaEvent.Open) }
                Box(Modifier.fillMaxSize()) {
                    HomeMenuDrawer(drawerState = drawerState, onOpenQuestions = openQuestions) {
                        MapHomeScreen(
                            state = state,
                            source = source,
                            onEvent = { event: HomeEvent ->
                                if (state.listening && (event == HomeEvent.CloseAsk || event == HomeEvent.SubmitAsk)) speech.cancel()
                                dispatch(event)
                            },
                            onOpenQuestions = openQuestions,
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
