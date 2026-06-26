package com.shotyou.app.ui.screens.result

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    jobId: String,
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(jobId) { viewModel.load(jobId) }
    val job by viewModel.job.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val status = job?.status) {
                null, JobStatus.QUEUED, JobStatus.RUNNING ->
                    PendingState(modifier = Modifier.align(Alignment.Center))

                JobStatus.SUCCEEDED ->
                    SuccessState(job = job!!, onRegenerate = viewModel::retry)

                JobStatus.FAILED, JobStatus.CANCELLED ->
                    ErrorState(
                        modifier = Modifier.align(Alignment.Center),
                        message = job?.errorMessage,
                        cancelled = status == JobStatus.CANCELLED,
                        onRetry = viewModel::retry,
                    )
            }
        }
    }
}

@Composable
private fun PendingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Generating your perfect shot…",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "This usually takes a few moments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SuccessState(job: GenerationJob, onRegenerate: () -> Unit) {
    val context = LocalContext.current
    val resultUri = job.resultUri

    Box(modifier = Modifier.fillMaxSize()) {
        if (resultUri != null) {
            AsyncImage(
                model = resultUri,
                contentDescription = "Generated image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Bottom action bar floats over the full-bleed image.
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (resultUri != null) shareImage(context, resultUri)
                        },
                        enabled = resultUri != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Share", modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Regenerate", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    text = "Saved to your gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String?,
    cancelled: Boolean,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = if (cancelled) "Generation cancelled" else "Generation failed",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message ?: "Something went wrong. Please try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Retry", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun shareImage(context: android.content.Context, uri: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri.toUri())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image"))
}
