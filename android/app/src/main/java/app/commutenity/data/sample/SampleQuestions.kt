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
                body = "Saan sasakay papuntang Dela Rosa St galing Ayala Center?",
                placeName = "Ayala Center",
            ),
            answers = listOf(
                QaPost(
                    id = "sample-board-a",
                    body = "Sample lang: hanapin ang sakayan sa Station Rd. Hindi ito totoong sagot ng rider.",
                    placeName = "Station Rd, San Lorenzo",
                ),
            ),
        ),
        QaThread(
            id = "sample-vehicle",
            question = QaPost(
                id = "sample-vehicle-q",
                body = "Tama ba ang jeep papuntang Pio del Pilar?",
                placeName = "Dela Rosa St",
            ),
            answers = listOf(
                QaPost(
                    id = "sample-vehicle-a",
                    body = "Sample lang: tingnan ang signboard. Walang login, kaya walang totoong boto dito.",
                    placeName = "Pio del Pilar",
                ),
            ),
        ),
    )
}
