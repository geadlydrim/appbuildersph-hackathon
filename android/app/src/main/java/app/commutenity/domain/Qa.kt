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

/** One trip choice an answer can be tied to; [key] is the candidate key, [label] is display text only. */
data class QaCandidate(val key: String, val label: String)

data class QaState(
    val open: Boolean = false,
    val trip: QaTrip? = null,
    val onlyTrip: Boolean = false,
    val threadId: String? = null,
    val asking: Boolean = false,
    val draft: String = "",
    val focusComment: Boolean = false,
    val markWorkedKey: String? = null,
    val candidates: List<QaCandidate> = emptyList(),
    val threads: List<QaThread> = emptyList(),
    val votes: Map<String, QaVote> = emptyMap(),
    val saveError: String? = null,
    val savedNotice: Boolean = false,
)

/**
 * "N riders say this works" for one trip: every mock answer tied to it, plus at most one for this phone (D39).
 * Arrow votes (D36) never count here.
 */
fun QaState.evidenceFor(tripKey: String): Int = tiedAnswers(tripKey, includeMock = true)

/**
 * The "worked" votes the trip finder may use to break an exact tie (D16). Same as [evidenceFor], except that
 * on the hero pair mock answers are shown but never change the order (D13).
 */
fun QaState.orderingVotes(tripKey: String, heroPair: Boolean): Int = tiedAnswers(tripKey, includeMock = !heroPair)

private fun QaState.tiedAnswers(tripKey: String, includeMock: Boolean): Int {
    val tied = threads.flatMap { it.comments }.filter { it.workedTrip == tripKey }
    val mock = if (includeMock) tied.count { !it.mine } else 0
    return mock + minOf(1, tied.count { it.mine })
}

fun QaState.visibleThreads(): List<QaThread> {
    val route = trip?.route
    if (!onlyTrip || route == null) return threads
    return threads.filter { it.question.route?.samePair(route) == true }
}

/** This phone already tied an answer to a trip for the pair on screen; one phone is one rider (D39). */
fun QaState.alreadyMarkedWorked(): Boolean {
    val route = trip?.route ?: return false
    return threads.any { thread ->
        thread.question.route?.samePair(route) == true && thread.comments.any { it.mine && it.workedTrip != null }
    }
}

fun QaState.isAboutTrip(thread: QaThread): Boolean {
    val route = trip?.route ?: return false
    return thread.question.route?.samePair(route) == true
}

/** Tying an answer to a trip only makes sense in a thread about the pair on screen, once per phone. */
fun QaState.canMarkWorked(thread: QaThread): Boolean = isAboutTrip(thread) && !alreadyMarkedWorked()

fun QaState.scoreOf(post: QaPost): Int = post.score + when (votes[post.id]) {
    QaVote.Up -> 1
    QaVote.Down -> -1
    null -> 0
}

sealed interface QaEvent {
    data class Open(
        val trip: QaTrip? = null,
        val onlyTrip: Boolean = false,
        val candidates: List<QaCandidate> = emptyList(),
    ) : QaEvent
    data object Close : QaEvent
    data object ShowAll : QaEvent
    data class OpenThread(val id: String, val focusComment: Boolean = false) : QaEvent
    data object BackToList : QaEvent
    data object StartQuestion : QaEvent
    data class Draft(val value: String) : QaEvent
    data object CancelDraft : QaEvent
    data object SaveDraft : QaEvent
    data class Vote(val postId: String, val vote: QaVote) : QaEvent
    /** Tie the next answer to [key] (one of the open candidates); the same key again, or null, unties it. */
    data class MarkWorked(val key: String?) : QaEvent
}

fun reduceQa(state: QaState, event: QaEvent): QaState {
    val fresh = state.copy(asking = false, draft = "", focusComment = false, markWorkedKey = null, saveError = null, savedNotice = false)
    return when (event) {
        is QaEvent.Open -> fresh.copy(
            open = true,
            trip = event.trip,
            onlyTrip = event.onlyTrip && event.trip != null,
            candidates = event.candidates,
            threadId = null,
        )
        QaEvent.Close -> fresh.copy(open = false, threadId = null)
        QaEvent.ShowAll -> state.copy(onlyTrip = false)
        is QaEvent.OpenThread -> fresh.copy(threadId = event.id, focusComment = event.focusComment)
        QaEvent.BackToList -> fresh.copy(threadId = null)
        QaEvent.StartQuestion -> fresh.copy(asking = true)
        is QaEvent.Draft -> state.copy(draft = event.value, saveError = null)
        QaEvent.CancelDraft -> state.copy(asking = false, draft = "", focusComment = false, markWorkedKey = null, saveError = null)
        QaEvent.SaveDraft -> saveDraft(state)
        is QaEvent.Vote -> state.copy(votes = toggleVote(state.votes, event.postId, event.vote))
        is QaEvent.MarkWorked -> {
            val thread = state.threads.firstOrNull { it.id == state.threadId }
            val allowed = thread != null && state.canMarkWorked(thread) &&
                (event.key == null || state.candidates.any { it.key == event.key })
            if (allowed) state.copy(markWorkedKey = event.key.takeIf { it != state.markWorkedKey }) else state
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
        val worked = state.markWorkedKey?.takeIf { key -> state.canMarkWorked(thread) && state.candidates.any { it.key == key } }
        val comment = QaPost(id = id, body = body, placeName = null, workedTrip = worked, sample = true, mine = true)
        state.threads.map { if (it.id == thread.id) it.copy(comments = it.comments + comment) else it }
    }
    return state.copy(
        threads = threads,
        asking = false,
        draft = "",
        focusComment = false,
        markWorkedKey = null,
        saveError = null,
        savedNotice = true,
    )
}

private fun toggleVote(votes: Map<String, QaVote>, postId: String, vote: QaVote): Map<String, QaVote> {
    val next = votes.toMutableMap()
    if (next[postId] == vote) next.remove(postId) else next[postId] = vote
    return next
}
