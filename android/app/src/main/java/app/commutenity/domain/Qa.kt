package app.commutenity.domain

enum class QaVote { Up, Down }

/** A from → to pair drawn on the sample map preview of a post. */
data class QaRoute(val from: String, val to: String)

data class QaPost(
    val id: String,
    val body: String,
    val placeName: String?,
    val score: Int = 1,
    val route: QaRoute? = null,
    val sample: Boolean = true,
)

data class QaThread(
    val id: String,
    val question: QaPost,
    val comments: List<QaPost>,
)

data class QaState(
    val open: Boolean = false,
    val threadId: String? = null,
    val asking: Boolean = false,
    val draft: String = "",
    val focusComment: Boolean = false,
    val threads: List<QaThread> = emptyList(),
    val votes: Map<String, QaVote> = emptyMap(),
    val saveError: String? = null,
    val savedNotice: Boolean = false,
)

fun QaState.scoreOf(post: QaPost): Int = post.score + when (votes[post.id]) {
    QaVote.Up -> 1
    QaVote.Down -> -1
    null -> 0
}

sealed interface QaEvent {
    data object Open : QaEvent
    data object Close : QaEvent
    data class OpenThread(val id: String, val focusComment: Boolean = false) : QaEvent
    data object BackToList : QaEvent
    data object StartQuestion : QaEvent
    data class Draft(val value: String) : QaEvent
    data object CancelDraft : QaEvent
    data object SaveDraft : QaEvent
    data class Vote(val postId: String, val vote: QaVote) : QaEvent
}

fun reduceQa(state: QaState, event: QaEvent): QaState {
    val fresh = state.copy(asking = false, draft = "", focusComment = false, saveError = null, savedNotice = false)
    return when (event) {
        QaEvent.Open -> fresh.copy(open = true, threadId = null)
        QaEvent.Close -> fresh.copy(open = false, threadId = null)
        is QaEvent.OpenThread -> fresh.copy(threadId = event.id, focusComment = event.focusComment)
        QaEvent.BackToList -> fresh.copy(threadId = null)
        QaEvent.StartQuestion -> fresh.copy(asking = true)
        is QaEvent.Draft -> state.copy(draft = event.value, saveError = null)
        QaEvent.CancelDraft -> state.copy(asking = false, draft = "", focusComment = false, saveError = null)
        QaEvent.SaveDraft -> saveDraft(state)
        is QaEvent.Vote -> state.copy(votes = toggleVote(state.votes, event.postId, event.vote))
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
    val post = QaPost(
        id = "local-${state.threads.size}-${state.threads.sumOf { it.comments.size }}",
        body = body,
        placeName = null,
        sample = true,
    )
    val threads = if (threadId == null) {
        listOf(QaThread(id = post.id, question = post, comments = emptyList())) + state.threads
    } else {
        state.threads.map { thread ->
            if (thread.id == threadId) thread.copy(comments = thread.comments + post) else thread
        }
    }
    return state.copy(
        threads = threads,
        asking = false,
        draft = "",
        focusComment = false,
        saveError = null,
        savedNotice = true,
    )
}

private fun toggleVote(votes: Map<String, QaVote>, postId: String, vote: QaVote): Map<String, QaVote> {
    val next = votes.toMutableMap()
    if (next[postId] == vote) next.remove(postId) else next[postId] = vote
    return next
}
