package app.commutenity.data.sample

import app.commutenity.domain.QaPost
import app.commutenity.domain.QaState
import app.commutenity.domain.QaThread

/**
 * Mock questions and answers for the Q&A screen.
 * There is no account, no login, and no other rider behind these posts.
 */
object SampleQuestions {
    fun initial(): QaState = QaState(threads = threads)

    private val threads = listOf(
        QaThread(
            id = "sample-board",
            question = QaPost(
                id = "sample-board-q",
                body = "Where do I board for Dela Rosa St from Ayala Center?",
                placeName = "Ayala Center",
            ),
            answers = listOf(
                QaPost(
                    id = "sample-board-a",
                    body = "Sample only: look for the stop on Station Rd. This is not a real rider answer.",
                    placeName = "Station Rd, San Lorenzo",
                ),
            ),
        ),
        QaThread(
            id = "sample-vehicle",
            question = QaPost(
                id = "sample-vehicle-q",
                body = "Is this the right jeep to Pio del Pilar?",
                placeName = "Dela Rosa St",
            ),
            answers = listOf(
                QaPost(
                    id = "sample-vehicle-a",
                    body = "Sample only: check the signboard. There is no login, so these votes are not real.",
                    placeName = "Pio del Pilar",
                ),
            ),
        ),
    )
}
