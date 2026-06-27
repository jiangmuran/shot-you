package com.shotyou.app.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shotyou.app.R
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.ui.categoryLabelRes
import com.shotyou.app.ui.components.ZoomableImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = state.error
    val genericError = stringResource(R.string.groups_start_error)
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage.ifBlank { genericError })
            viewModel.consumeError()
        }
    }

    // The group currently shown full-screen in the zoom viewer (null = closed).
    var viewerGroup by remember { mutableStateOf<PhotoGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.groups_curate_title)) },
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
        bottomBar = {
            if (state.hasGroups) {
                StartBar(
                    count = state.selectedCount,
                    starting = state.starting,
                    onStart = { viewModel.start(onStarted) },
                )
            }
        },
    ) { padding ->
        if (!state.hasGroups) {
            EmptyGroups(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.groups_curate_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(state.items, key = { it.group.id }) { item ->
                    GroupCard(
                        item = item,
                        onToggle = { viewModel.toggleIncluded(item.group.id) },
                        onHintChange = { viewModel.onHintChange(item.group.id, it) },
                        onZoom = { viewerGroup = item.group },
                    )
                }
            }
        }
    }

    viewerGroup?.let { group ->
        GroupViewer(group = group, onDismiss = { viewerGroup = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCard(
    item: CurationItem,
    onToggle: () -> Unit,
    onHintChange: (String) -> Unit,
    onZoom: () -> Unit,
) {
    val group = item.group
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.included,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(end = 4.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = {}, // informational label, not a selection control
                            label = { Text(stringResource(categoryLabelRes(group.category))) },
                        )
                        Text(
                            text = stringResource(
                                if (group.recommended) R.string.groups_recommended
                                else R.string.groups_not_recommended,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (group.recommended) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(onClick = onZoom),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(group.photoUris, key = { it }) { uri ->
                    Thumbnail(uri = uri, isReference = uri in group.referenceUris)
                }
            }

            Text(
                text = pluralStringResource(R.plurals.groups_photo_count, group.size, group.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            HintField(
                value = item.hint,
                onValueChange = onHintChange,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun HintField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(stringResource(R.string.groups_hint_label)) },
        placeholder = { Text(stringResource(R.string.groups_hint_placeholder)) },
        minLines = 2,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StartBar(count: Int, starting: Boolean, onStart: () -> Unit) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Button(
                onClick = onStart,
                enabled = count > 0 && !starting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        starting -> stringResource(R.string.groups_starting)
                        count == 0 -> stringResource(R.string.groups_start_none)
                        else -> stringResource(R.string.groups_start, count)
                    },
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(uri: String, isReference: Boolean) {
    Box(modifier = Modifier.size(84.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .size(256)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        )
        if (isReference) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.groups_reference_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupViewer(group: PhotoGroup, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                val uris = group.photoUris
                val pagerState = rememberPagerState(pageCount = { uris.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomableImage(
                        model = uris[page],
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.groups_viewer_close_cd),
                        tint = Color.White,
                    )
                }
                if (uris.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.groups_viewer_count,
                                pagerState.currentPage + 1,
                                uris.size,
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGroups(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.groups_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.groups_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
