package app.commutenity.domain

import app.commutenity.data.sample.SampleQuestions
import org.junit.Assert.assertEquals
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
            thread.answers.forEach { answer -> assertTrue(answer.sample) }
        }
    }

    @Test
    fun emptyQuestionIsNotSaved() {
        val start = SampleQuestions.initial().copy(composer = QaComposer.Question, draft = "   ")
        val next = reduceQa(start, QaEvent.SaveDraft)
        assertEquals(start.threads.size, next.threads.size)
        assertEquals(QaComposer.Question, next.composer)
    }

    @Test
    fun typedQuestionStaysSample() {
        val start = SampleQuestions.initial().copy(composer = QaComposer.Question, draft = "Saan ang sakayan?")
        val next = reduceQa(start, QaEvent.SaveDraft)
        assertEquals(start.threads.size + 1, next.threads.size)
        assertTrue(next.threads.first().question.sample)
        assertEquals("Saan ang sakayan?", next.threads.first().question.body)
    }

    @Test
    fun voteClearsWhenPressedAgain() {
        val postId = "sample-board-q"
        val up = reduceQa(SampleQuestions.initial(), QaEvent.Vote(postId, QaVote.Up))
        assertEquals(QaVote.Up, up.votes[postId])
        val cleared = reduceQa(up, QaEvent.Vote(postId, QaVote.Up))
        assertNull(cleared.votes[postId])
    }
}
