package app.commutenity.domain

import app.commutenity.data.sample.SampleQuestions
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
