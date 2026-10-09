package app.commutenity.ui.qa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.QaEvent
import app.commutenity.domain.QaPost
import app.commutenity.domain.QaState
import app.commutenity.domain.QaThread
import app.commutenity.domain.QaVote
import app.commutenity.domain.scoreOf
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta

@Composable
fun QaScreen(
    state: QaState,
    onEvent: (QaEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCommuteColors.current
    val thread = state.threads.firstOrNull { it.id == state.threadId }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.map)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = if (thread == null) "← Back to map" else "← Questions",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                if (thread == null) onEvent(QaEvent.Close) else onEvent(QaEvent.BackToList)
            },
        )
        Text(
            text = "Questions",
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Sample data. No account and no login. These are not real rider answers.",
            color = colors.ink,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.reason)
                .padding(12.dp),
        )
        if (thread == null) {
            QuestionList(state, onEvent)
        } else {
            ThreadDetail(thread, state, onEvent)
        }
    }
}

@Composable
private fun QuestionList(state: QaState, onEvent: (QaEvent) -> Unit) {
    val colors = LocalCommuteColors.current
    if (state.asking) {
        DraftBox(
            placeholder = "Ask about a route or a stop",
            draft = state.draft,
            onDraft = { onEvent(QaEvent.Draft(it)) },
            onPost = { onEvent(QaEvent.SaveDraft) },
            onCancel = { onEvent(QaEvent.CancelDraft) },
            error = state.saveError,
            focus = true,
        )
    } else {
        Text(
            text = "Ask a question",
            color = colors.paraOn,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.ink)
                .clickable { onEvent(QaEvent.StartQuestion) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
    if (state.savedNotice) SavedNotice()
    if (state.threads.isEmpty()) {
        Text(
            text = "No saved questions yet.",
            color = colors.ink,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
    state.threads.forEach { thread ->
        PostCard(
            post = thread.question,
            state = state,
            onEvent = onEvent,
            commentCount = thread.comments.size,
            onOpen = { onEvent(QaEvent.OpenThread(thread.id)) },
            onComment = { onEvent(QaEvent.OpenThread(thread.id, focusComment = true)) },
        )
    }
}

@Composable
private fun ThreadDetail(thread: QaThread, state: QaState, onEvent: (QaEvent) -> Unit) {
    val colors = LocalCommuteColors.current
    val focusRequester = remember { FocusRequester() }
    PostCard(
        post = thread.question,
        state = state,
        onEvent = onEvent,
        commentCount = thread.comments.size,
        onComment = { focusRequester.requestFocus() },
    )
    DraftBox(
        placeholder = "Add a comment",
        draft = state.draft,
        onDraft = { onEvent(QaEvent.Draft(it)) },
        onPost = { onEvent(QaEvent.SaveDraft) },
        onCancel = { onEvent(QaEvent.CancelDraft) },
        error = state.saveError,
        focus = state.focusComment,
        focusRequester = focusRequester,
        showButtons = state.draft.isNotEmpty() || state.saveError != null,
    )
    if (state.savedNotice) SavedNotice()
    if (thread.comments.isEmpty()) {
        Text(
            text = "No comments yet.",
            color = colors.muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    thread.comments.forEach { comment ->
        CommentCard(comment, state, onEvent)
    }
}

@Composable
private fun PostCard(
    post: QaPost,
    state: QaState,
    onEvent: (QaEvent) -> Unit,
    commentCount: Int,
    onComment: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    val colors = LocalCommuteColors.current
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(start = 6.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
    ) {
        VoteColumn(post, state, onEvent)
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            SampleChip()
            Text(
                text = post.body,
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!post.placeName.isNullOrBlank()) {
                Text(
                    text = post.placeName,
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            post.route?.let { RoutePreview(it, Modifier.padding(top = 10.dp)) }
            CommentButton(commentCount, onComment, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun CommentCard(comment: QaPost, state: QaState, onEvent: (QaEvent) -> Unit) {
    val colors = LocalCommuteColors.current
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 6.dp),
    ) {
        SampleChip()
        Text(
            text = comment.body,
            color = colors.ink,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        val vote = state.votes[comment.id]
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoteArrow(up = true, selected = vote == QaVote.Up) { onEvent(QaEvent.Vote(comment.id, QaVote.Up)) }
            ScoreText(state.scoreOf(comment), vote)
            VoteArrow(up = false, selected = vote == QaVote.Down) { onEvent(QaEvent.Vote(comment.id, QaVote.Down)) }
        }
    }
}

@Composable
private fun VoteColumn(post: QaPost, state: QaState, onEvent: (QaEvent) -> Unit) {
    val vote = state.votes[post.id]
    Column(
        Modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VoteArrow(up = true, selected = vote == QaVote.Up) { onEvent(QaEvent.Vote(post.id, QaVote.Up)) }
        ScoreText(state.scoreOf(post), vote)
        VoteArrow(up = false, selected = vote == QaVote.Down) { onEvent(QaEvent.Vote(post.id, QaVote.Down)) }
    }
}

@Composable
private fun ScoreText(score: Int, vote: QaVote?) {
    val colors = LocalCommuteColors.current
    Text(
        text = score.toString(),
        color = when (vote) {
            QaVote.Up -> colors.pinA
            QaVote.Down -> colors.route
            null -> colors.ink
        },
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
}

@Composable
private fun VoteArrow(up: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalCommuteColors.current
    val tint: Color = when {
        !selected -> colors.muted
        up -> colors.pinA
        else -> colors.route
    }
    Canvas(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (up) "Upvote" else "Downvote" }
            .padding(9.dp),
    ) {
        val w = size.width
        val h = size.height
        val arrow = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.94f, h * 0.52f)
            lineTo(w * 0.68f, h * 0.52f)
            lineTo(w * 0.68f, h * 0.94f)
            lineTo(w * 0.32f, h * 0.94f)
            lineTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.06f, h * 0.52f)
            close()
        }
        if (!up) {
            arrow.transform(
                androidx.compose.ui.graphics.Matrix().apply {
                    translate(0f, h)
                    scale(1f, -1f)
                },
            )
        }
        if (selected) {
            drawPath(arrow, tint)
        } else {
            drawPath(arrow, tint, style = Stroke(width = 1.8.dp.toPx()))
        }
    }
}

@Composable
private fun CommentButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(colors.sample)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 1.6.dp.toPx()
            val bubbleHeight = size.height * 0.78f
            drawRoundRect(
                color = colors.ink,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, bubbleHeight - stroke),
                cornerRadius = CornerRadius(size.width * 0.25f),
                style = Stroke(width = stroke),
            )
            val tail = Path().apply {
                moveTo(size.width * 0.28f, bubbleHeight - stroke)
                lineTo(size.width * 0.22f, size.height)
                lineTo(size.width * 0.5f, bubbleHeight - stroke)
            }
            drawPath(tail, colors.ink, style = Stroke(width = stroke))
        }
        Text(
            text = when (count) {
                0 -> "Comment"
                1 -> "1 comment"
                else -> "$count comments"
            },
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SavedNotice() {
    Text(
        text = "Posted on this phone for this session only. Sample data, no login.",
        color = LocalCommuteColors.current.muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun SampleChip() {
    val colors = LocalCommuteColors.current
    Text(
        text = "Sample",
        color = colors.sampleOn,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(colors.sample)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraftBox(
    placeholder: String,
    draft: String,
    onDraft: (String) -> Unit,
    onPost: () -> Unit,
    onCancel: () -> Unit,
    error: String? = null,
    focus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    showButtons: Boolean = true,
) {
    val colors = LocalCommuteColors.current
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focus) {
        if (focus) focusRequester.requestFocus()
    }
    LaunchedEffect(focused, showButtons, error) {
        if (focused) {
            // Wait for the keyboard to finish resizing the window before scrolling.
            delay(300)
            bringIntoView.bringIntoView()
        }
    }
    Column(
        Modifier
            .bringIntoViewRequester(bringIntoView)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(12.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            textStyle = TextStyle(color = colors.ink, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.ink),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .clip(RoundedCornerShape(12.dp))
                .background(colors.map)
                .padding(12.dp),
            decorationBox = { inner ->
                if (draft.isEmpty()) {
                    Text(placeholder, color = colors.faint, fontSize = 16.sp)
                }
                inner()
            },
        )
        if (error != null) {
            Text(error, color = colors.pinA, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (showButtons) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Post",
                    color = colors.paraOn,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.ink)
                        .clickable {
                            if (draft.isNotBlank()) focusManager.clearFocus()
                            onPost()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Text(
                    text = "Cancel",
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.ink, RoundedCornerShape(12.dp))
                        .clickable {
                            focusManager.clearFocus()
                            onCancel()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
