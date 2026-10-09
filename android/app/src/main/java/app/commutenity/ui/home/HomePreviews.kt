package app.commutenity.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.commutenity.data.sample.SampleTripSource
import app.commutenity.domain.Field
import app.commutenity.domain.HomeEvent
import app.commutenity.domain.HomeState
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Sheet
import app.commutenity.domain.reduce
import app.commutenity.ui.theme.CommuteNityTheme

private val source = SampleTripSource()

private fun place(field: Field, query: String) =
    source.search(field, query).filterIsInstance<SearchRow.PlaceRow>().first().place

private fun withOrigin() = reduce(HomeState(), HomeEvent.UseMyLocation, source)

private fun withTrip() = reduce(
    reduce(withOrigin(), HomeEvent.Focus(Field.B), source),
    HomeEvent.Pick(place(Field.B, "Dela Rosa")),
    source,
)

private fun outside() = reduce(
    reduce(withOrigin(), HomeEvent.Focus(Field.B), source),
    HomeEvent.Pick(place(Field.B, "Outside")),
    source,
)

private fun searching() = reduce(HomeState(), HomeEvent.Focus(Field.A), source).let {
    reduce(it, HomeEvent.Query("Ayala"), source)
}

@Preview(name = "T0-01 empty", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewEmpty() = Frame { MapHomeScreen(HomeState(), source, {}, designStatusBar = true) }

@Preview(name = "T0-01 a-set", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewASet() = Frame { MapHomeScreen(withOrigin(), source, {}, designStatusBar = true) }

@Preview(name = "T0-01 ab-set", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewAbSet() = Frame {
    MapHomeScreen(withTrip().copy(sheet = Sheet.Peek), source, {}, designStatusBar = true)
}

@Preview(name = "T0-01 ab-set dark", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewAbSetDark() = Frame(dark = true) {
    MapHomeScreen(withTrip().copy(sheet = Sheet.Peek), source, {}, designStatusBar = true)
}

@Preview(name = "T0-02 place search", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewSearch() = Frame { MapHomeScreen(searching(), source, {}, designStatusBar = true) }

@Preview(name = "T0-03 half", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewHalf() = Frame { MapHomeScreen(withTrip(), source, {}, designStatusBar = true) }

@Preview(name = "T0-03 half dark", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewHalfDark() = Frame(dark = true) { MapHomeScreen(withTrip(), source, {}, designStatusBar = true) }

@Preview(name = "T0-04 not in data", widthDp = 412, heightDp = 915)
@Composable
private fun PreviewNotice() = Frame { MapHomeScreen(outside(), source, {}, designStatusBar = true) }

@Composable
private fun Frame(dark: Boolean = false, content: @Composable () -> Unit) {
    CommuteNityTheme(darkTheme = dark, content = content)
}
