package com.shotyou.app.ui.screens.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shotyou.app.R
import com.shotyou.app.domain.model.GenerationSession
import com.shotyou.app.domain.model.SessionStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onOpenTask: (String) -> Unit,
    onOpenCuration: (String) -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue_title)) },
                actions = {
                    IconButton(onClick = viewModel::togglePause) {
                        if (paused) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.queue_resume),
                            )
                        } else {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = stringResource(R.string.queue_pause),
                            )
                        }
                    }
                    if (tasks.any { it.done > 0 }) {
                        TextButton(onClick = viewModel::clearFinished) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.queue_clear_finished))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (sessions.isEmpty() && tasks.isEmpty()) {
            EmptyQueue(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sessions.isNotEmpty()) {
                    item(key = "sessions-header") {
                        SectionHeader(stringResource(R.string.queue_sessions_title))
                    }
                    items(sessions, key = { "session-${it.id}" }) { session ->
                        SessionCard(
                            session = session,
                            onOpenCuration = { onOpenCuration(session.id) },
                            onDelete = { viewModel.deleteSession(session.id) },
                        )
                    }
                }
                if (tasks.isNotEmpty()) {
                    item(key = "tasks-header") {
                        SectionHeader(stringResource(R.string.queue_tasks_title))
                    }
                    item(key = "tasks-progress") {
                        QueueProgressHeader(
                            done = tasks.sumOf { it.done },
                            total = tasks.sumOf { it.total },
                            paused = paused,
                        )
                    }
                    items(tasks, key = { "task-${it.batchId}" }) { task ->
                        TaskCard(task = task, onOpen = { onOpenTask(task.batchId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: GenerationSession,
    onOpenCuration: () -> Unit,
    onDelete: () -> Unit,
) {
    val ready = session.stage == SessionStage.READY_FOR_REVIEW
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionThumbnail(session = session)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    StageBadge(stage = session.stage)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = sessionSummary(session),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (session.stage == SessionStage.FAILED) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.queue_session_dismiss),
                        )
                    }
                }
            }

            if (ready) {
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onOpenCuration,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.queue_session_review))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionThumbnail(session: GenerationSession) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        session.photoUris.firstOrNull()?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .size(256)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (session.stage == SessionStage.CLASSIFYING || session.stage == SessionStage.GENERATING) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun StageBadge(stage: SessionStage) {
    val labelRes = when (stage) {
        SessionStage.CLASSIFYING -> R.string.queue_stage_classifying
        SessionStage.READY_FOR_REVIEW -> R.string.queue_stage_ready
        SessionStage.GENERATING -> R.string.queue_stage_generating
        SessionStage.DONE -> R.string.queue_stage_done
        SessionStage.FAILED -> R.string.queue_stage_failed
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = when (stage) {
                SessionStage.READY_FOR_REVIEW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                SessionStage.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            disabledLabelColor = when (stage) {
                SessionStage.READY_FOR_REVIEW -> MaterialTheme.colorScheme.primary
                SessionStage.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        border = null,
    )
}

@Composable
private fun sessionSummary(session: GenerationSession): String = when (session.stage) {
    SessionStage.CLASSIFYING ->
        stringResource(R.string.queue_session_classifying, session.photoCount)
    SessionStage.READY_FOR_REVIEW ->
        stringResource(R.string.queue_session_ready, session.groupCount)
    SessionStage.GENERATING ->
        stringResource(R.string.queue_session_generating)
    SessionStage.DONE ->
        stringResource(R.string.queue_session_done, session.groupCount)
    SessionStage.FAILED ->
        session.error?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.queue_session_failed)
}

@Composable
private fun QueueProgressHeader(done: Int, total: Int, paused: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.queue_overall_progress),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (paused) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            stringResource(R.string.queue_paused),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = Color(0xFF9A6700).copy(alpha = 0.14f),
                        disabledLabelColor = Color(0xFF9A6700),
                    ),
                    border = null,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.queue_progress_count, done, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) done.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(task: QueueTask, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (task.thumbnailUri != null) {
                    // Sized request: decode to a small bitmap so the list stays smooth with
                    // many items — never decode full-resolution images here.
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(task.thumbnailUri)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (task.running || task.done < task.total) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.queue_task_fallback),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (task.total > 0) task.done.toFloat() / task.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = taskStatusLine(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.queue_open_task),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun taskStatusLine(task: QueueTask): String {
    val parts = buildList {
        add(stringResource(R.string.queue_task_done_count, task.done, task.total))
        if (task.running) add(stringResource(R.string.queue_status_running))
        if (task.failed > 0) add(stringResource(R.string.queue_task_failed_count, task.failed))
    }
    return parts.joinToString(stringResource(R.string.queue_status_separator))
}

@Composable
private fun EmptyQueue(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.queue_empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.queue_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
