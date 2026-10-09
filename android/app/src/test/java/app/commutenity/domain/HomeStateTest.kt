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

    @Test
    fun askKeepsTheDirectionTheRiderWrote() {
        val match = matchAsk("from Dela Rosa St to Ayala Center", source)
        assertEquals("Dela Rosa St", match.origin?.name)
        assertEquals("Ayala Center", match.destination?.name)
    }

    @Test
    fun askReadsTagalogCuesInEitherOrder() {
        val match = matchAsk("Paano pumunta sa Dela Rosa St galing Ayala Center?", source)
        assertEquals("Ayala Center", match.origin?.name)
        assertEquals("Dela Rosa St", match.destination?.name)
    }

    @Test
    fun askWithOnePlaceAndNoFromCueIsTheDestination() {
        val match = matchAsk("how do I get to Dela Rosa St", source)
        assertNull(match.origin)
        assertEquals("Dela Rosa St", match.destination?.name)
        val start = matchAsk("galing Ayala Center", source)
        assertEquals("Ayala Center", start.origin?.name)
        assertNull(start.destination)
    }

    @Test
    fun samePlaceForBothEndsShowsNoTrip() {
        val origin = reduce(HomeState(), HomeEvent.UseMyLocation, source)
        val searching = reduce(origin, HomeEvent.Focus(Field.B), source)
        val next = reduce(searching, HomeEvent.Pick(source.myLocation), source)
        assertEquals(Sheet.Peek, next.sheet)
        assertFalse(canOpenTrip(next))
        assertEquals("Start and destination are the same place. Change one.", peekMessage(next))
    }

    @Test
    fun collapsedTripSaysHowToBringItBack() {
        val collapsed = reduce(bothSet(), HomeEvent.SettleSheet(Sheet.Peek), source)
        assertEquals("Ayala Center → Dela Rosa St. Swipe up to see the trip.", peekMessage(collapsed))
        assertEquals("Pick A and B on the map.", peekMessage(HomeState()))
    }

    @Test
    fun noticeAsksToChangeTheEndThatIsOutsideMakati() {
        val outside = source.search(Field.A, "Outside").filterIsInstance<SearchRow.PlaceRow>().single().place
        val searching = reduce(bothSet(), HomeEvent.Focus(Field.A), source)
        val next = reduce(searching, HomeEvent.Pick(outside), source)
        assertEquals(Sheet.Notice, next.sheet)
        assertEquals(Field.A, outsideField(next))
        val editing = reduce(next, HomeEvent.Focus(Field.A), source)
        assertEquals("Not in my data yet. Only Makati is covered for now.", peekMessage(editing))
        assertEquals(Field.B, outsideField(reduce(reduce(bothSet(), HomeEvent.Focus(Field.B), source), HomeEvent.Pick(outside), source)))
    }

    @Test
    fun mapTapsSetAThenBThenStartOver() {
        val a = reduce(HomeState(), HomeEvent.MapTap(14.558012, 121.018547), source)
        assertEquals("Pinned spot", a.origin?.name)
        assertEquals(14.558012, a.origin!!.lat!!, 1e-9)
        assertNull(a.destination)
        assertEquals(Sheet.Peek, a.sheet)

        val ab = reduce(a, HomeEvent.MapTap(14.557063, 121.008188), source)
        assertEquals(121.008188, ab.destination!!.lng!!, 1e-9)
        assertEquals(Sheet.Half, ab.sheet)

        val again = reduce(ab, HomeEvent.MapTap(14.55, 121.02), source)
        assertEquals(14.55, again.origin!!.lat!!, 1e-9)
        assertNull(again.destination)
        assertEquals(Sheet.Peek, again.sheet)
    }

    @Test
    fun mapTapFillsTheOpenSearchBoxAndClosesIt() {
        val searchingA = reduce(bothSet(), HomeEvent.Focus(Field.A), source)
        val next = reduce(searchingA, HomeEvent.MapTap(14.56, 121.01), source)
        assertEquals("Pinned spot", next.origin?.name)
        assertEquals("Dela Rosa St", next.destination?.name)
        assertNull(next.activeField)
        assertEquals("", next.query)
    }

    @Test
    fun mapTapOutsideMakatiShowsTheNotice() {
        val a = reduce(HomeState(), HomeEvent.MapTap(14.558, 121.018), source)
        val outside = reduce(a, HomeEvent.MapTap(14.59, 121.07), source)
        assertFalse(outside.destination!!.inMakati)
        assertEquals(Sheet.Notice, outside.sheet)
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
