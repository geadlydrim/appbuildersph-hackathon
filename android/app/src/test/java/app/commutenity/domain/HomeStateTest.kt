package app.commutenity.domain

import app.commutenity.data.sample.SampleTripSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private val source = SampleTripSource()

    @Test
    fun ayalaSearchMatchesTheResultsFrame() {
        val rows = source.search(Field.A, "Ayala")
        assertTrue(rows[0] is SearchRow.UseMyLocation)
        val place = (rows[1] as SearchRow.PlaceRow).place
        assertEquals("Ayala Center", place.name)
        assertEquals("Station Rd, San Lorenzo", place.area)
        assertEquals(2, rows.size)
    }

    @Test
    fun delaRosaOpensTheHalfSheet() {
        val withOrigin = reduce(HomeState(), HomeEvent.UseMyLocation, source)
        val searching = reduce(withOrigin, HomeEvent.Focus(Field.B), source)
        val dela = source.search(Field.B, "Dela Rosa").filterIsInstance<SearchRow.PlaceRow>().single()
        val next = reduce(searching, HomeEvent.Pick(dela.place), source)
        assertEquals(Sheet.Half, next.sheet)
        assertTrue(source.resolve(next.origin!!, next.destination!!) is TripResult.Ready)
    }

    @Test
    fun outsideMakatiShowsTheNotice() {
        val withOrigin = reduce(HomeState(), HomeEvent.UseMyLocation, source)
        val searching = reduce(withOrigin, HomeEvent.Focus(Field.B), source)
        val outside = source.search(Field.B, "Outside").filterIsInstance<SearchRow.PlaceRow>().single()
        val next = reduce(searching, HomeEvent.Pick(outside.place), source)
        assertEquals(Sheet.Notice, next.sheet)
        assertTrue(source.resolve(next.origin!!, next.destination!!) is TripResult.NotInData)
    }

    @Test
    fun clearingEitherEndReturnsToPeek() {
        val ready = bothSet()
        val searching = reduce(ready, HomeEvent.Focus(Field.A), source)
        val cleared = reduce(searching, HomeEvent.ClearActive, source)
        assertEquals(null, cleared.origin)
        assertEquals(Sheet.Peek, cleared.sheet)
    }

    @Test
    fun sampleTripKeepsPlaceholderTokens() {
        val trip = (source.resolve(source.myLocation, delaRosa()) as TripResult.Ready).trip
        assertTrue(trip.sample)
        assertEquals("₱XX", trip.fare)
        assertEquals("XX min", trip.minutes)
        val ride = trip.legs.filterIsInstance<Leg.Ride>().single()
        assertEquals("SIGNBOARD (TBD)", ride.signboard)
        assertEquals("Stop 1  →  Stop 2", ride.stops)
    }

    @Test
    fun searchCanBeClosedWithoutPicking() {
        val searching = reduce(HomeState(), HomeEvent.Focus(Field.A), source)
        val closed = reduce(searching, HomeEvent.DismissSearch, source)
        assertNull(closed.activeField)
        assertEquals(Sheet.Peek, closed.sheet)
    }

    @Test
    fun compactCardExpandsInsteadOfOpeningSearch() {
        val trip = bothSet()
        val expanded = reduce(trip, HomeEvent.ExpandCard, source)
        assertTrue(expanded.cardExpanded)
        assertNull(expanded.activeField)
        assertEquals(Sheet.Half, expanded.sheet)
    }

    @Test
    fun askSampleMatchSetsBothPinsAndSaysItIsNotTheModel() {
        val asking = reduce(HomeState(), HomeEvent.OpenAsk, source)
        val drafted = reduce(asking, HomeEvent.AskDraft("Ayala Center to Dela Rosa St"), source)
        val next = reduce(drafted, HomeEvent.SubmitAsk, source)
        assertEquals("Ayala Center", next.origin?.name)
        assertEquals("Dela Rosa St", next.destination?.name)
        assertEquals(Sheet.Half, next.sheet)
        assertFalse(next.asking)
        assertTrue(next.askFeedback!!.contains("not the on-device model"))
    }

    @Test
    fun unmatchedAskStaysOpen() {
        val asking = reduce(HomeState(), HomeEvent.OpenAsk, source)
        val drafted = reduce(asking, HomeEvent.AskDraft("what's the weather"), source)
        val next = reduce(drafted, HomeEvent.SubmitAsk, source)
        assertTrue(next.asking)
        assertNull(next.origin)
        assertTrue(next.askFeedback!!.contains("Pick A and B on the map"))
    }

    @Test
    fun voiceStartOpensTheComposerAndListens() {
        val dirty = HomeState(askDraft = "old", askFeedback = "old feedback", activeField = Field.A)
        val next = reduce(dirty, HomeEvent.VoiceStart, source)
        assertTrue(next.asking)
        assertTrue(next.listening)
        assertEquals("", next.askDraft)
        assertNull(next.askFeedback)
        assertNull(next.activeField)
    }

    @Test
    fun partialUpdatesTheDraftOnlyWhileListening() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val partial = reduce(listening, HomeEvent.VoicePartial("Ayala"), source)
        assertEquals("Ayala", partial.askDraft)
        val stopped = reduce(partial, HomeEvent.VoiceStop, source)
        assertFalse(stopped.listening)
        assertEquals("Ayala", stopped.askDraft)
        val late = reduce(stopped, HomeEvent.VoicePartial("Ayala Center to"), source)
        assertEquals(stopped, late)
    }

    @Test
    fun voiceResultKeepsTheComposerOpenWithTheHeardText() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val next = reduce(listening, HomeEvent.VoiceResult("  Ayala Center to Dela Rosa St "), source)
        assertFalse(next.listening)
        assertTrue(next.asking)
        assertEquals("Ayala Center to Dela Rosa St", next.askDraft)
        assertEquals(VOICE_HEARD_FEEDBACK, next.askFeedback)
        assertNull(next.origin)
        assertNull(next.destination)
    }

    @Test
    fun blankVoiceResultSaysItDidNotCatchThat() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val next = reduce(listening, HomeEvent.VoiceResult("   "), source)
        assertFalse(next.listening)
        assertTrue(next.asking)
        assertEquals("", next.askDraft)
        assertEquals("I didn't catch that. Try again or type your question.", next.askFeedback)
    }

    @Test
    fun voiceErrorStopsListeningAndKeepsTheDraft() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val partial = reduce(listening, HomeEvent.VoicePartial("Ayala Center"), source)
        val next = reduce(partial, HomeEvent.VoiceError("Speech recognition failed."), source)
        assertFalse(next.listening)
        assertTrue(next.asking)
        assertEquals("Ayala Center", next.askDraft)
        assertEquals("Speech recognition failed.", next.askFeedback)
    }

    @Test
    fun submitAfterVoiceResultUsesTheTypedPath() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val heard = reduce(listening, HomeEvent.VoiceResult("Ayala Center to Dela Rosa St"), source)
        val next = reduce(heard, HomeEvent.SubmitAsk, source)
        assertEquals("Ayala Center", next.origin?.name)
        assertEquals("Dela Rosa St", next.destination?.name)
        assertFalse(next.listening)
        assertFalse(next.asking)
    }

    @Test
    fun closingAskWhileListeningStopsListening() {
        val listening = reduce(HomeState(), HomeEvent.VoiceStart, source)
        val next = reduce(listening, HomeEvent.CloseAsk, source)
        assertFalse(next.listening)
        assertFalse(next.asking)
    }

    private fun bothSet(): HomeState {
        val origin = reduce(HomeState(), HomeEvent.UseMyLocation, source)
        val searching = reduce(origin, HomeEvent.Focus(Field.B), source)
        val dela = source.search(Field.B, "Dela").filterIsInstance<SearchRow.PlaceRow>().single()
        return reduce(searching, HomeEvent.Pick(dela.place), source)
    }

    private fun delaRosa(): Place {
        return source.search(Field.B, "Dela Rosa").filterIsInstance<SearchRow.PlaceRow>().single().place
    }
}
