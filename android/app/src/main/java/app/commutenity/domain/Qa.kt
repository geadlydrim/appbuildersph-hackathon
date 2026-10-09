package app.commutenity.domain

enum class QaVote { Up, Down }

data class QaPost(
    val id: String,
    val body: String,
    val placeName: String?,
    val sample: Boolean = true,
)

data class QaThread(
    val id: String,
    val question: QaPost,
    val answers: List<QaPost>,
)

enum class QaComposer { None, Question, Answer }

data class QaState(
    val open: Boolean = false,
    val threadId: String? = null,
    val composer: QaComposer = QaComposer.None,
    val draft: String = "",
    val threads: List<QaThread> = emptyList(),
    val votes: Map<String, QaVote> = emptyMap(),
)

sealed interface QaEvent {
    data object Open : QaEvent
    data object Close : QaEvent
    data class OpenThread(val id: String) : QaEvent
    data object BackToList : QaEvent
    data object StartQuestion : QaEvent
    data object StartAnswer : QaEvent
    data class Draft(val value: String) : QaEvent
    data object CancelDraft : QaEvent
    data object SaveDraft : QaEvent
    data class Vote(val postId: String, val vote: QaVote) : QaEvent
}

fun reduceQa(state: QaState, event: QaEvent): QaState {
    return when (event) {
        QaEvent.Open -> state.copy(open = true, threadId = null, composer = QaComposer.None, draft = "")
        QaEvent.Close -> state.copy(open = false, threadId = null, composer = QaComposer.None, draft = "")
        is QaEvent.OpenThread -> state.copy(threadId = event.id, composer = QaComposer.None, draft = "")
        QaEvent.BackToList -> state.copy(threadId = null, composer = QaComposer.None, draft = "")
        QaEvent.StartQuestion -> state.copy(composer = QaComposer.Question, draft = "")
        QaEvent.StartAnswer -> {
            if (state.threadId == null) state else state.copy(composer = QaComposer.Answer, draft = "")
        }
        is QaEvent.Draft -> state.copy(draft = event.value)
        QaEvent.CancelDraft -> state.copy(composer = QaComposer.None, draft = "")
        QaEvent.SaveDraft -> saveDraft(state)
        is QaEvent.Vote -> state.copy(votes = toggleVote(state.votes, event.postId, event.vote))
    }
}

private fun saveDraft(state: QaState): QaState {
    val body = state.draft.trim()
    if (body.isEmpty()) return state
    val post = QaPost(
        id = "local-${state.threads.size}-${state.threads.sumOf { it.answers.size }}",
        body = body,
        placeName = null,
        sample = true,
    )
    val threads = when (state.composer) {
        QaComposer.Question -> listOf(
            QaThread(id = post.id, question = post, answers = emptyList()),
        ) + state.threads
        QaComposer.Answer -> {
            val threadId = state.threadId ?: return state
            state.threads.map { thread ->
                if (thread.id == threadId) thread.copy(answers = thread.answers + post) else thread
            }
        }
        QaComposer.None -> state.threads
    }
    return state.copy(threads = threads, composer = QaComposer.None, draft = "")
}

private fun toggleVote(votes: Map<String, QaVote>, postId: String, vote: QaVote): Map<String, QaVote> {
    val next = votes.toMutableMap()
    if (next[postId] == vote) next.remove(postId) else next[postId] = vote
    return next
}
