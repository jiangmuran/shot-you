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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onOpenResult: (String) -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val jobs by viewModel.jobs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
                actions = {
                    if (jobs.any { it.status.isFinished }) {
                        TextButton(onClick = viewModel::clearFinished) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear finished")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (jobs.isEmpty()) {
            EmptyQueue(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(jobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onOpenResult = { onOpenResult(job.id) },
                        onRetry = { viewModel.retry(job.id) },
                        onCancel = { viewModel.cancel(job.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCard(
    job: GenerationJob,
    onOpenResult: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val clickable = job.status == JobStatus.SUCCEEDED
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenResult,
        enabled = clickable,
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
            val thumbUri = job.resultUri ?: job.referenceUris.firstOrNull()
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbUri != null) {
                    AsyncImage(
                        model = thumbUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                if (job.status == JobStatus.QUEUED || job.status == JobStatus.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.groupTitle?.takeIf { it.isNotBlank() } ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = job.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                StatusChip(job.status)
            }

            when (job.status) {
                JobStatus.FAILED -> IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
                JobStatus.QUEUED, JobStatus.RUNNING -> IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun StatusChip(status: JobStatus) {
    val (label, color) = when (status) {
        JobStatus.QUEUED -> "Queued" to Color(0xFF6750A4)
        JobStatus.RUNNING -> "Running" to Color(0xFF1565C0)
        JobStatus.SUCCEEDED -> "Done" to Color(0xFF2E7D32)
        JobStatus.FAILED -> "Failed" to Color(0xFFC62828)
        JobStatus.CANCELLED -> "Cancelled" to Color(0xFF757575)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.14f),
            disabledLabelColor = color,
        ),
        border = null,
    )
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
        Text("No jobs yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Generated images you queue will appear here with live status.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val JobStatus.isFinished: Boolean
    get() = this == JobStatus.SUCCEEDED || this == JobStatus.FAILED || this == JobStatus.CANCELLED
