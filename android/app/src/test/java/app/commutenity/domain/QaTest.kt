package app.commutenity.domain

import app.commutenity.data.sample.SampleQuestions
import app.commutenity.data.sample.SampleTripSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QaTest {
    @Test
    fun sampleThreadsAreMockAndHaveNoLoginIdentity() {
        val state = SampleQuestions.initial()
        assertTrue(state.threads.isNotEmpty())
        state.threads.forEach { thread ->
            assertTrue(thread.question.sample)
            assertNotNull(thread.question.route)
            thread.comments.forEach { comment -> assertTrue(comment.sample) }
        }
    }

    @Test
    fun emptyQuestionIsNotPosted() {
        val start = reduceQa(SampleQuestions.initial(), QaEvent.StartQuestion).copy(draft = "   ")
        val next = reduceQa(start, QaEvent.SaveDraft)
        assertEquals(start.threads.size, next.threads.size)
        assertTrue(next.asking)
        assertEquals("Write a question before posting.", next.saveError)
    }

    @Test
    fun typedQuestionStaysSample() {
        val start = reduceQa(SampleQuestions.initial(), QaEvent.StartQuestion)
        val next = reduceQa(reduceQa(start, QaEvent.Draft("Where is the stop?")), QaEvent.SaveDraft)
        assertEquals(start.threads.size + 1, next.threads.size)
        assertTrue(next.threads.first().question.sample)
        assertEquals("Where is the stop?", next.threads.first().question.body)
        assertFalse(next.asking)
    }

    @Test
    fun commentButtonOpensThreadReadyToComment() {
        val opened = reduceQa(SampleQuestions.initial(), QaEvent.OpenThread("sample-board", focusComment = true))
        assertEquals("sample-board", opened.threadId)
        assertTrue(opened.focusComment)

        val before = opened.threads.first { it.id == "sample-board" }.comments.size
        val posted = reduceQa(reduceQa(opened, QaEvent.Draft("Sa may 7-Eleven")), QaEvent.SaveDraft)
        val after = posted.threads.first { it.id == "sample-board" }.comments
        assertEquals(before + 1, after.size)
        assertTrue(after.last().sample)
        assertTrue(posted.savedNotice)
    }

    @Test
    fun emptyCommentIsNotPosted() {
        val opened = reduceQa(SampleQuestions.initial(), QaEvent.OpenThread("sample-board"))
        val next = reduceQa(opened, QaEvent.SaveDraft)
        assertEquals("Write a comment before posting.", next.saveError)
    }

    private val ayalaTrip = QaTrip(
        key = SampleTripSource.keyFor("ayala-center", "dela-rosa"),
        route = QaRoute("ayala-center", "dela-rosa", "Ayala Center", "Dela Rosa St"),
    )

    @Test
    fun evidenceCountsOnlyAnswersTiedToThatTrip() {
        val start = SampleQuestions.initial()
        assertEquals(1, start.evidenceFor(ayalaTrip.key))
        assertEquals(0, start.evidenceFor(SampleTripSource.keyFor("dela-rosa", "ayala-center")))
    }

    @Test
    fun tripLinkShowsOnlyThatPairUntilShowAll() {
        val filtered = reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, onlyTrip = true))
        assertEquals(listOf("sample-optimal"), filtered.visibleThreads().map { it.id })
        assertEquals(2, reduceQa(filtered, QaEvent.ShowAll).visibleThreads().size)
    }

    private val ayalaCandidates = listOf(QaCandidate(ayalaTrip.key, "Sample trip"), QaCandidate("other-trip", "Other trip"))

    @Test
    fun workedChipOnlyTiesAnswersInAThreadAboutTheTripOnScreen() {
        val open = reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, candidates = ayalaCandidates))

        val other = reduceQa(open, QaEvent.OpenThread("sample-board"))
        assertNull(reduceQa(other, QaEvent.MarkWorked(ayalaTrip.key)).markWorkedKey)

        val same = reduceQa(open, QaEvent.OpenThread("sample-optimal"))
        val posted = reduceQa(
            reduceQa(reduceQa(same, QaEvent.MarkWorked(ayalaTrip.key)), QaEvent.Draft("Gumana")),
            QaEvent.SaveDraft,
        )
        assertEquals(2, posted.evidenceFor(ayalaTrip.key))
        assertEquals(ayalaTrip.key, posted.threads.first { it.id == "sample-optimal" }.comments.last().workedTrip)
        assertNull(posted.markWorkedKey)
    }

    @Test
    fun anAnswerCanOnlyBeTiedToAnOpenCandidate() {
        val same = reduceQa(
            reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, candidates = ayalaCandidates)),
            QaEvent.OpenThread("sample-optimal"),
        )
        assertNull(reduceQa(same, QaEvent.MarkWorked("not-a-candidate")).markWorkedKey)
        assertEquals("other-trip", reduceQa(same, QaEvent.MarkWorked("other-trip")).markWorkedKey)
    }

    @Test
    fun pressingTheSameCandidateAgainOrNullUntiesIt() {
        val same = reduceQa(
            reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, candidates = ayalaCandidates)),
            QaEvent.OpenThread("sample-optimal"),
        )
        val tied = reduceQa(same, QaEvent.MarkWorked(ayalaTrip.key))
        assertEquals(ayalaTrip.key, tied.markWorkedKey)
        assertNull(reduceQa(tied, QaEvent.MarkWorked(ayalaTrip.key)).markWorkedKey)
        assertNull(reduceQa(tied, QaEvent.MarkWorked(null)).markWorkedKey)

        val untied = reduceQa(reduceQa(tied, QaEvent.MarkWorked(null)), QaEvent.Draft("Text lang"))
        val posted = reduceQa(untied, QaEvent.SaveDraft)
        assertNull(posted.threads.first { it.id == "sample-optimal" }.comments.last().workedTrip)
    }

    @Test
    fun onePhoneCountsAsOneRiderForATrip() {
        var state = reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, candidates = ayalaCandidates))
        state = reduceQa(state, QaEvent.OpenThread("sample-optimal"))
        repeat(3) {
            state = reduceQa(
                reduceQa(reduceQa(state, QaEvent.MarkWorked(ayalaTrip.key)), QaEvent.Draft("Gumana $it")),
                QaEvent.SaveDraft,
            )
        }
        assertEquals(2, state.evidenceFor(ayalaTrip.key))
        assertTrue(state.alreadyMarkedWorked())
        assertFalse(state.canMarkWorked(state.threads.first { it.id == "sample-optimal" }))
        assertEquals(1, state.threads.first { it.id == "sample-optimal" }.comments.count { it.mine && it.workedTrip != null })
    }

    @Test
    fun aPhonesTwoTiedAnswersForTheSamePairCountOnce() {
        val route = ayalaTrip.route
        fun mine(id: String, key: String) = QaPost(id, "Gumana", null, route = null, workedTrip = key, sample = true, mine = true)
        val thread = QaThread(
            id = "t",
            question = QaPost("t-q", "?", null, route = route),
            comments = listOf(mine("c1", "trip-a"), mine("c2", "trip-a"), mine("c3", "trip-b")),
        )
        val state = QaState(threads = listOf(thread))
        assertEquals(1, state.evidenceFor("trip-a"))
        assertEquals(1, state.evidenceFor("trip-b"))
        assertEquals(1, state.orderingVotes("trip-a", heroPair = false))
        assertEquals(1, state.orderingVotes("trip-a", heroPair = true))
    }

    @Test
    fun arrowVotesNeverChangeEvidence() {
        val start = SampleQuestions.initial()
        val tied = start.threads.first { it.id == "sample-optimal" }.comments.first { it.workedTrip != null }
        val voted = reduceQa(reduceQa(start, QaEvent.Vote(tied.id, QaVote.Up)), QaEvent.Vote(start.threads.first().question.id, QaVote.Down))
        assertEquals(start.evidenceFor(ayalaTrip.key), voted.evidenceFor(ayalaTrip.key))
        assertEquals(start.orderingVotes(ayalaTrip.key, heroPair = false), voted.orderingVotes(ayalaTrip.key, heroPair = false))
    }

    @Test
    fun heroPairShowsMockEvidenceButOnlyCountsMyOwnForOrdering() {
        val mock = QaPost("m1", "Gumana", null, workedTrip = "trip-a", sample = true, mine = false)
        val mine = QaPost("c1", "Gumana", null, workedTrip = "trip-a", sample = true, mine = true)
        val thread = QaThread("t", QaPost("t-q", "?", null, route = ayalaTrip.route), listOf(mock, mine))
        val state = QaState(threads = listOf(thread))
        assertEquals(2, state.evidenceFor("trip-a"))
        assertEquals(1, state.orderingVotes("trip-a", heroPair = true))
        assertEquals(2, state.orderingVotes("trip-a", heroPair = false))
    }

    @Test
    fun untiedAnswersCountForNothing() {
        val state = SampleQuestions.initial()
        assertEquals(0, state.evidenceFor("missing"))
        assertEquals(0, state.orderingVotes("missing", heroPair = false))
    }

    @Test
    fun questionAskedFromATripListStaysInThatList() {
        val filtered = reduceQa(SampleQuestions.initial(), QaEvent.Open(ayalaTrip, onlyTrip = true))
        val asked = reduceQa(
            reduceQa(reduceQa(filtered, QaEvent.StartQuestion), QaEvent.Draft("May jeep pa ba?")),
            QaEvent.SaveDraft,
        )
        assertEquals("May jeep pa ba?", asked.visibleThreads().first().question.body)
    }

    @Test
    fun votesMoveTheScoreAndClearWhenPressedAgain() {
        val start = SampleQuestions.initial()
        val post = start.threads.first().question
        val up = reduceQa(start, QaEvent.Vote(post.id, QaVote.Up))
        assertEquals(post.score + 1, up.scoreOf(post))
        val down = reduceQa(up, QaEvent.Vote(post.id, QaVote.Down))
        assertEquals(post.score - 1, down.scoreOf(post))
        val cleared = reduceQa(down, QaEvent.Vote(post.id, QaVote.Down))
        assertNull(cleared.votes[post.id])
        assertEquals(post.score, cleared.scoreOf(post))
    }
}
