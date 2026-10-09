package app.commutenity.data.sample

import app.commutenity.domain.QaPost
import app.commutenity.domain.QaRoute
import app.commutenity.domain.QaState
import app.commutenity.domain.QaThread

/**
 * Mock posts and comments for the Q&A screen.
 * There is no account, no login, and no other rider behind these posts.
 */
object SampleQuestions {
    fun initial(): QaState = QaState(threads = threads)

    private val threads = listOf(
        QaThread(
            id = "sample-optimal",
            question = QaPost(
                id = "sample-optimal-q",
                body = "Ito ba ang pinaka-okay na way papuntang Dela Rosa St galing Ayala Center? " +
                    "Lakad muna sa Station Rd tapos jeep.",
                placeName = "Ayala Center → Dela Rosa St",
                score = 24,
                route = QaRoute(from = "Ayala Center", to = "Dela Rosa St"),
            ),
            comments = listOf(
                QaPost(
                    id = "sample-optimal-c1",
                    body = "Sample only: oo, 'yan ang pinakamadali kung hindi traffic sa Ayala Ave.",
                    placeName = null,
                    score = 9,
                ),
                QaPost(
                    id = "sample-optimal-c2",
                    body = "Sample only: pwede ring lakarin kung umaga, mga 15 minutes lang.",
                    placeName = null,
                    score = 3,
                ),
            ),
        ),
        QaThread(
            id = "sample-board",
            question = QaPost(
                id = "sample-board-q",
                body = "Saan ang sakayan ng jeep papuntang Pio del Pilar? Tama ba 'tong ruta ko?",
                placeName = "Dela Rosa St → Pio del Pilar",
                score = 11,
                route = QaRoute(from = "Dela Rosa St", to = "Pio del Pilar"),
            ),
            comments = listOf(
                QaPost(
                    id = "sample-board-c1",
                    body = "Sample only: tingnan ang signboard ng jeep. These votes are not real.",
                    placeName = null,
                    score = 4,
                ),
            ),
        ),
    )
}
