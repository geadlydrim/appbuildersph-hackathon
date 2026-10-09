package app.commutenity.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0D5636),
    onPrimary = Color(0xFFFCF9F1),
    background = Color(0xFFE3F1DF),
    surface = Color(0xFFFCF9F1),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFCF9F1),
    onPrimary = Color(0xFF0D5636),
    background = Color(0xFF0F291C),
    surface = Color(0xFF122E21),
)

@Composable
fun CommuteNityTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme) {
        CompositionLocalProvider(LocalCommuteColors provides colors, content = content)
    }
}
