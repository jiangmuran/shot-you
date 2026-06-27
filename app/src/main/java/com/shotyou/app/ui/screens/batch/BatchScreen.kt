package com.shotyou.app.ui.screens.batch

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shotyou.app.R
import com.shotyou.app.ui.components.ZoomableImageDialog
import kotlinx.coroutines.launch
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus

private enum class CleanupMode { KEEP_ALL, KEEP_ONE, DELETE_ALL }

/** A pending destructive delete awaiting confirmation. */
private data class PendingDelete(val uris: List<String>, val keepOne: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    batchId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel(),
) {
    LaunchedEffect(batchId) { viewModel.load(batchId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Count for a consent-gated delete; the actual deletion happens in the system dialog.
    var consentCount by remember { mutableIntStateOf(0) }
    // The candidate image currently shown full-screen in the zoom viewer (null = closed).
    var zoomUri by remember { mutableStateOf<String?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && consentCount > 0) {
            val count = consentCount
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.batch_deleted_snackbar, count))
            }
        }
        consentCount = 0
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    LaunchedEffect(state.deletedCount) {
        state.deletedCount?.let { count ->
            snackbarHostState.showSnackbar(context.getString(R.string.batch_deleted_snackbar, count))
            viewModel.consumeDeleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.jobs.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { state.jobs.size })
        // Jump to the newest candidate when an "ask for changes" iteration appears.
        var lastRevision by remember { mutableStateOf(state.candidateRevision) }
        LaunchedEffect(state.candidateRevision) {
            if (state.candidateRevision > lastRevision && lastRevision > 0) {
                pagerState.animateScrollToPage(state.jobs.lastIndex)
            }
            lastRevision = state.candidateRevision
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                pageSpacing = 12.dp,
            ) { page ->
                CandidatePage(
                    job = state.jobs[page],
                    index = page,
                    onRetry = { viewModel.retry(state.jobs[page].id) },
                    onZoom = { uri -> zoomUri = uri },
                )
            }

            PageIndicator(
                count = state.jobs.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )

            val current = state.jobs.getOrNull(pagerState.currentPage)

            // Actions: share + ask for changes.
            ActionsSection(
                current = current,
                refining = state.refining,
                onShare = { uri ->
                    shareImage(context, uri, context.getString(R.string.batch_share_chooser))
                },
                onAsk = { suggestion ->
                    if (current != null) {
                        viewModel.askForChanges(
                            current = current,
                            suggestion = suggestion,
                            label = context.getString(R.string.batch_ask_label),
                            batchId = batchId,
                        )
                    }
                },
            )

            // Original-photo cleanup.
            CleanupCard(
                enabled = state.hasSucceeded,
                deleting = state.deleting,
                originalUris = state.originalUris,
                onApply = { uris ->
                    viewModel.deleteOriginals(uris) { sender ->
                        consentCount = uris.size
                        deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                },
                modifier = Modifier.padding(top = 24.dp),
            )

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.batch_done), style = MaterialTheme.typography.titleMedium)
            }
        }

        zoomUri?.let { uri ->
            ZoomableImageDialog(model = uri, onDismiss = { zoomUri = null })
        }
    }
}

@Composable
private fun CandidatePage(job: GenerationJob, index: Int, onRetry: () -> Unit, onZoom: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (job.status) {
                JobStatus.QUEUED, JobStatus.RUNNING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.batch_generating, index + 1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                    )
                }

                JobStatus.SUCCEEDED -> {
                    val uri = job.resultUri
                    if (uri != null) {
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(R.string.batch_image_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onZoom(uri) },
                        )
                    }
                    job.variantLabel?.let { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                JobStatus.FAILED, JobStatus.CANCELLED -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = stringResource(R.string.batch_failed_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    job.errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.batch_retry), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val active = i == current
            Box(
                modifier = Modifier
                    .size(if (active) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

@Composable
private fun ActionsSection(
    current: GenerationJob?,
    refining: Boolean,
    onShare: (String) -> Unit,
    onAsk: (String) -> Unit,
) {
    val ready = current?.status == JobStatus.SUCCEEDED && current.resultUri != null
    // Reset the suggestion when the user swipes to a different candidate.
    var suggestion by remember(current?.id) { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { current?.resultUri?.let(onShare) },
            enabled = ready,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.batch_share), modifier = Modifier.padding(start = 8.dp))
        }
    }

    Text(
        text = stringResource(R.string.batch_ask_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp),
    )
    Text(
        text = stringResource(R.string.batch_ask_sub),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = suggestion,
        onValueChange = { suggestion = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        minLines = 2,
        enabled = !refining,
        placeholder = { Text(stringResource(R.string.batch_ask_placeholder)) },
    )
    if (!ready) {
        Text(
            text = stringResource(R.string.batch_ask_needs_success),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Button(
        onClick = {
            onAsk(suggestion)
            suggestion = ""
        },
        enabled = ready && suggestion.isNotBlank() && !refining,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        if (refining) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.batch_ask_button), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun CleanupCard(
    enabled: Boolean,
    deleting: Boolean,
    originalUris: List<String>,
    onApply: (uris: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(CleanupMode.KEEP_ALL) }
    var keptUri by remember(originalUris) { mutableStateOf(originalUris.firstOrNull()) }
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.batch_cleanup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    if (enabled) R.string.batch_cleanup_sub else R.string.batch_cleanup_locked,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CleanupOption(
                label = stringResource(R.string.batch_cleanup_keep_all),
                selected = mode == CleanupMode.KEEP_ALL,
                enabled = enabled,
                onSelect = { mode = CleanupMode.KEEP_ALL },
            )
            CleanupOption(
                label = stringResource(R.string.batch_cleanup_keep_one),
                selected = mode == CleanupMode.KEEP_ONE,
                enabled = enabled && originalUris.size > 1,
                onSelect = { mode = CleanupMode.KEEP_ONE },
            )
            CleanupOption(
                label = stringResource(R.string.batch_cleanup_delete_all),
                selected = mode == CleanupMode.DELETE_ALL,
                enabled = enabled && originalUris.isNotEmpty(),
                onSelect = { mode = CleanupMode.DELETE_ALL },
            )

            if (mode == CleanupMode.KEEP_ONE && enabled) {
                Text(
                    text = stringResource(R.string.batch_cleanup_pick),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(originalUris, key = { it }) { uri ->
                        KeepThumbnail(uri = uri, selected = uri == keptUri, onClick = { keptUri = uri })
                    }
                }
            }

            if (mode != CleanupMode.KEEP_ALL) {
                Button(
                    onClick = {
                        when (mode) {
                            CleanupMode.DELETE_ALL ->
                                pending = PendingDelete(originalUris, keepOne = false)
                            CleanupMode.KEEP_ONE -> {
                                val keep = keptUri
                                if (keep != null) {
                                    pending = PendingDelete(originalUris.filterNot { it == keep }, keepOne = true)
                                }
                            }
                            CleanupMode.KEEP_ALL -> Unit
                        }
                    },
                    enabled = enabled && !deleting &&
                        (mode == CleanupMode.DELETE_ALL || keptUri != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(stringResource(R.string.batch_cleanup_apply))
                    }
                }
            }
        }
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.batch_delete_title)) },
            text = {
                Text(
                    stringResource(
                        if (p.keepOne) R.string.batch_delete_keep_one_msg else R.string.batch_delete_all_msg,
                        p.uris.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onApply(p.uris)
                    pending = null
                }) {
                    Text(stringResource(R.string.batch_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun CleanupOption(label: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun KeepThumbnail(uri: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .size(256)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.batch_kept_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(14.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.batch_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private fun shareImage(context: android.content.Context, uri: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri.toUri())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
