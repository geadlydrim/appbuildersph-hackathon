package app.commutenity.ui.qa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.QaComposer
import app.commutenity.domain.QaEvent
import app.commutenity.domain.QaPost
import app.commutenity.domain.QaState
import app.commutenity.domain.QaThread
import app.commutenity.domain.QaVote
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = if (thread == null) "Mapa" else "Mga tanong",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                if (thread == null) onEvent(QaEvent.Close) else onEvent(QaEvent.BackToList)
            },
        )
        Text(
            text = "Mga tanong",
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Sample data. Walang account at walang login. Hindi ito totoong sagot ng rider.",
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
    if (state.composer == QaComposer.Question) {
        DraftBox(
            label = "Magtanong",
            draft = state.draft,
            onDraft = { onEvent(QaEvent.Draft(it)) },
            onSave = { onEvent(QaEvent.SaveDraft) },
            onCancel = { onEvent(QaEvent.CancelDraft) },
        )
    } else {
        Text(
            text = "Magtanong",
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
    if (state.threads.isEmpty()) {
        Text(
            text = "Wala pang naka-save na tanong.",
            color = colors.ink,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
    state.threads.forEach { thread ->
        Column(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .clickable { onEvent(QaEvent.OpenThread(thread.id)) }
                .padding(14.dp),
        ) {
            SampleChip()
            Text(
                text = thread.question.body,
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            PlaceLine(thread.question.placeName)
            Text(
                text = "${thread.answers.size} sagot · sample",
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ThreadDetail(thread: QaThread, state: QaState, onEvent: (QaEvent) -> Unit) {
    PostCard(thread.question, state, onEvent)
    thread.answers.forEach { answer ->
        PostCard(answer, state, onEvent, answer = true)
    }
    if (state.composer == QaComposer.Answer) {
        DraftBox(
            label = "Sumagot",
            draft = state.draft,
            onDraft = { onEvent(QaEvent.Draft(it)) },
            onSave = { onEvent(QaEvent.SaveDraft) },
            onCancel = { onEvent(QaEvent.CancelDraft) },
        )
    } else {
        val colors = LocalCommuteColors.current
        Text(
            text = "Sumagot",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.ink, RoundedCornerShape(12.dp))
                .clickable { onEvent(QaEvent.StartAnswer) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
    Text(
        text = "Na-save sa phone mo · sample lang, walang login.",
        color = colorsMuted(),
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun colorsMuted() = LocalCommuteColors.current.muted

@Composable
private fun PostCard(
    post: QaPost,
    state: QaState,
    onEvent: (QaEvent) -> Unit,
    answer: Boolean = false,
) {
    val colors = LocalCommuteColors.current
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(14.dp),
    ) {
        SampleChip()
        Text(
            text = if (answer) "Sagot" else "Tanong",
            color = colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = post.body,
            color = colors.ink,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        PlaceLine(post.placeName)
        VoteRow(
            selected = state.votes[post.id],
            onVote = { onEvent(QaEvent.Vote(post.id, it)) },
        )
    }
}

@Composable
private fun PlaceLine(placeName: String?) {
    if (placeName.isNullOrBlank()) return
    val colors = LocalCommuteColors.current
    Text(
        text = placeName,
        color = colors.muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun VoteRow(selected: QaVote?, onVote: (QaVote) -> Unit) {
    val colors = LocalCommuteColors.current
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VoteChip("Up", selected == QaVote.Up) { onVote(QaVote.Up) }
        VoteChip("Down", selected == QaVote.Down) { onVote(QaVote.Down) }
        Text(
            text = "Hindi binabago ang pamasahe",
            color = colors.faint,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun VoteChip(label: String, on: Boolean, onClick: () -> Unit) {
    val colors = LocalCommuteColors.current
    Text(
        text = label,
        color = if (on) colors.paraOn else colors.ink,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (on) colors.ink else colors.sample)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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

@Composable
private fun DraftBox(
    label: String,
    draft: String,
    onDraft: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalCommuteColors.current
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(14.dp),
    ) {
        Text(label, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            textStyle = TextStyle(color = colors.ink, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.ink),
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.map)
                .padding(12.dp),
            decorationBox = { inner ->
                if (draft.isEmpty()) {
                    Text("Isulat dito", color = colors.faint, fontSize = 16.sp)
                }
                inner()
            },
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "I-save",
                color = colors.paraOn,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.ink)
                    .clickable(onClick = onSave)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Text(
                text = "Kanselahin",
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.ink, RoundedCornerShape(12.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
