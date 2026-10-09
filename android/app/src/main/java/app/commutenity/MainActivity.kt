package app.commutenity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.commutenity.data.sample.SampleTripSource
import app.commutenity.domain.HomeEvent
import app.commutenity.domain.HomeState
import app.commutenity.domain.reduce
import app.commutenity.ui.home.MapHomeScreen
import app.commutenity.ui.theme.CommuteNityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val source = SampleTripSource()
        setContent {
            CommuteNityTheme {
                var state by remember { mutableStateOf(HomeState()) }
                MapHomeScreen(
                    state = state,
                    source = source,
                    onEvent = { event: HomeEvent -> state = reduce(state, event, source) },
                )
            }
        }
    }
}
