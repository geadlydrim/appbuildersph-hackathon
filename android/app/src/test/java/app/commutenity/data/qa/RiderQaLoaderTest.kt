package app.commutenity.data.qa

import app.commutenity.data.pack.CommutePack
import app.commutenity.data.pack.PackTripSource
import app.commutenity.domain.QaState
import app.commutenity.domain.evidenceFor
import app.commutenity.domain.orderingVotes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiderQaLoaderTest {
    private val json = File("../../data/mock/rider-qa.json").readText()
    private val pack = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
    private val source = PackTripSource(pack)

    private val jeepKey = "jeep-buendia-lrt#1"
    private val busKey = "bus-buendia-lrt#1"

    private fun validKeys(fromId: String, toId: String): Set<String> {
        val from = pack.places.firstOrNull { it.id == fromId }?.toPlace() ?: return emptySet()
        val to = pack.places.firstOrNull { it.id == toId }?.toPlace() ?: return emptySet()
        return source.candidates(from, to).map { it.key }.toSet()
    }

    private fun load(): QaState = RiderQaLoader.parse(json, ::validKeys)

    @Test
    fun everyThreadAndAnswerLoadsAsMock() {
        val state = load()
        assertEquals(3, state.threads.size)
        assertEquals(listOf(4, 1, 1), state.threads.map { it.comments.size })
        state.threads.forEach { thread ->
            assertNotNull(thread.question.route)
            (listOf(thread.question) + thread.comments).forEach { post ->
                assertTrue(post.sample)
                assertFalse(post.mine)
            }
        }
        assertEquals(
            listOf(PackTripSource.HERO_ORIGIN_ID, PackTripSource.HERO_DESTINATION_ID),
            state.threads.first().question.route?.let { listOf(it.fromId, it.toId) },
        )
    }

    @Test
    fun jeepHasTwoTiedAnswersAndBusOne() {
        val state = load()
        assertEquals(2, state.evidenceFor(jeepKey))
        assertEquals(1, state.evidenceFor(busKey))
    }

    @Test
    fun reversePairAndGeneralThreadsCarryNoTiedAnswers() {
        val state = load()
        val reverse = state.threads.first { it.question.route?.fromId == PackTripSource.HERO_DESTINATION_ID }
        assertTrue(reverse.comments.none { it.workedTrip != null })
        val general = state.threads.last()
        assertTrue(general.comments.none { it.workedTrip != null })
    }

    @Test
    fun answerWithAnUnknownCandidateKeyLoadsUntied() {
        val state = RiderQaLoader.parse(json) { _, _ -> setOf(jeepKey) }
        assertEquals(2, state.evidenceFor(jeepKey))
        assertEquals(0, state.evidenceFor(busKey))
        val bus = state.threads.first().comments.first { it.id == "mock-hero-choice-a3" }
        assertNull(bus.workedTrip)
        assertEquals(4, state.threads.first().comments.size)
    }

    @Test
    fun heroPairShowsMockEvidenceButItNeverChangesTheOrder() {
        val state = load()
        assertEquals(2, state.evidenceFor(jeepKey))
        assertEquals(0, state.orderingVotes(jeepKey, heroPair = true))
        assertEquals(0, state.orderingVotes(busKey, heroPair = true))
    }

    @Test
    fun offTheHeroPairTheSameMockAnswersCount() {
        val state = load()
        assertEquals(2, state.orderingVotes(jeepKey, heroPair = false))
        assertEquals(1, state.orderingVotes(busKey, heroPair = false))
    }
}
