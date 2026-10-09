package app.commutenity.domain

import app.commutenity.data.sample.SampleTripSource
import org.junit.Assert.assertEquals
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
