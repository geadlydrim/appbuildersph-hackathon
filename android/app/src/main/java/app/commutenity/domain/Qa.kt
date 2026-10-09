package app.commutenity.domain

enum class QaVote { Up, Down }

/** Origin → destination of a post, matched by place id; the names are only for display. */
data class QaRoute(val fromId: String, val toId: String, val from: String, val to: String) {
    fun samePair(other: QaRoute) = fromId == other.fromId && toId == other.toId
}

/** The trip on the map when Questions was opened. */
data class QaTrip(val key: String, val route: QaRoute)

data class QaPost(
    val id: String,
    val body: String,
    val placeName: String?,
    val score: Int = 1,
    val route: QaRoute? = null,
    /** Candidate key of the trip this answer says worked (D34); null = text only. */
    val workedTrip: String? = null,
    val sample: Boolean = true,
    /** Posted on this phone in this session. */
    val mine: Boolean = false,
)

data class QaThread(
    val id: String,
    val question: QaPost,
    val comments: List<QaPost>,
)

data class QaState(
    val open: Boolean = false,
    val trip: QaTrip? = null,
    val onlyTrip: Boolean = false,
    val threadId: String? = null,
    val asking: Boolean = false,
    val draft: String = "",
    val focusComment: Boolean = false,
    val markWorked: Boolean = false,
    val threads: List<QaThread> = emptyList(),
    val votes: Map<String, QaVote> = emptyMap(),
    val saveError: String? = null,
    val savedNotice: Boolean = false,
)

/** "N riders say this works" for one trip. Arrow votes (D36) never count here. */
fun QaState.evidenceFor(tripKey: String): Int =
    threads.sumOf { thread -> thread.comments.count { it.workedTrip == tripKey } }

fun QaState.visibleThreads(): List<QaThread> {
    val route = trip?.route
    if (!onlyTrip || route == null) return threads
    return threads.filter { it.question.route?.samePair(route) == true }
}

/** This phone already said the trip on screen worked; one phone is one rider. */
fun QaState.alreadyMarkedWorked(): Boolean {
    val key = trip?.key ?: return false
    return threads.any { thread -> thread.comments.any { it.mine && it.workedTrip == key } }
}

fun QaState.isAboutTrip(thread: QaThread): Boolean {
    val route = trip?.route ?: return false
    return thread.question.route?.samePair(route) == true
}

/** The "This trip worked" chip only makes sense in a thread about the trip on screen, once per phone. */
fun QaState.canMarkWorked(thread: QaThread): Boolean = isAboutTrip(thread) && !alreadyMarkedWorked()

fun QaState.scoreOf(post: QaPost): Int = post.score + when (votes[post.id]) {
    QaVote.Up -> 1
    QaVote.Down -> -1
    null -> 0
}

sealed interface QaEvent {
    data class Open(val trip: QaTrip? = null, val onlyTrip: Boolean = false) : QaEvent
    data object Close : QaEvent
    data object ShowAll : QaEvent
    data class OpenThread(val id: String, val focusComment: Boolean = false) : QaEvent
    data object BackToList : QaEvent
    data object StartQuestion : QaEvent
    data class Draft(val value: String) : QaEvent
    data object CancelDraft : QaEvent
    data object SaveDraft : QaEvent
    data class Vote(val postId: String, val vote: QaVote) : QaEvent
    data object ToggleWorked : QaEvent
}

fun reduceQa(state: QaState, event: QaEvent): QaState {
    val fresh = state.copy(asking = false, draft = "", focusComment = false, markWorked = false, saveError = null, savedNotice = false)
    return when (event) {
        is QaEvent.Open -> fresh.copy(
            open = true,
            trip = event.trip,
            onlyTrip = event.onlyTrip && event.trip != null,
            threadId = null,
        )
        QaEvent.Close -> fresh.copy(open = false, threadId = null)
        QaEvent.ShowAll -> state.copy(onlyTrip = false)
        is QaEvent.OpenThread -> fresh.copy(threadId = event.id, focusComment = event.focusComment)
        QaEvent.BackToList -> fresh.copy(threadId = null)
        QaEvent.StartQuestion -> fresh.copy(asking = true)
        is QaEvent.Draft -> state.copy(draft = event.value, saveError = null)
        QaEvent.CancelDraft -> state.copy(asking = false, draft = "", focusComment = false, markWorked = false, saveError = null)
        QaEvent.SaveDraft -> saveDraft(state)
        is QaEvent.Vote -> state.copy(votes = toggleVote(state.votes, event.postId, event.vote))
        QaEvent.ToggleWorked -> {
            val thread = state.threads.firstOrNull { it.id == state.threadId }
            if (thread != null && state.canMarkWorked(thread)) state.copy(markWorked = !state.markWorked) else state
        }
    }
}

private fun saveDraft(state: QaState): QaState {
    val body = state.draft.trim()
    val threadId = state.threadId
    if (threadId == null && !state.asking) return state
    if (body.isEmpty()) {
        val what = if (threadId == null) "question" else "comment"
        return state.copy(saveError = "Write a $what before posting.")
    }
    val thread = state.threads.firstOrNull { it.id == threadId }
    val id = "local-${state.threads.size}-${state.threads.sumOf { it.comments.size }}"
    val threads = if (thread == null) {
        // Asked from a trip's filtered list: tie the question to that pair so it stays in view.
        val route = if (state.onlyTrip) state.trip?.route else null
        val question = QaPost(id = id, body = body, placeName = null, route = route, sample = true, mine = true)
        listOf(QaThread(id = id, question = question, comments = emptyList())) + state.threads
    } else {
        val worked = if (state.markWorked && state.canMarkWorked(thread)) state.trip?.key else null
        val comment = QaPost(id = id, body = body, placeName = null, workedTrip = worked, sample = true, mine = true)
        state.threads.map { if (it.id == thread.id) it.copy(comments = it.comments + comment) else it }
    }
    return state.copy(
        threads = threads,
        asking = false,
        draft = "",
        focusComment = false,
        markWorked = false,
        saveError = null,
        savedNotice = true,
    )
}

private fun toggleVote(votes: Map<String, QaVote>, postId: String, vote: QaVote): Map<String, QaVote> {
    val next = votes.toMutableMap()
    if (next[postId] == vote) next.remove(postId) else next[postId] = vote
    return next
}
