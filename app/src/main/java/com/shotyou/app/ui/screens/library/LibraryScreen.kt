package com.shotyou.app.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shotyou.app.R
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.util.PhotoAccess
import com.shotyou.app.util.PhotoPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPhotosGrouped: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val grouped by viewModel.grouped.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Recompute from the source of truth rather than trusting individual grants.
        viewModel.onPermissionResult(PhotoPermissions.access(context))
    }

    // Re-evaluate access every time the screen returns to the foreground (covers the
    // user changing their selected-photos set via system settings / "Manage").
    DisposableEffectOnStart(lifecycleOwner) {
        viewModel.onPermissionResult(PhotoPermissions.access(context))
    }

    // Navigate once grouping succeeds.
    LaunchedEffect(grouped) {
        if (grouped) {
            viewModel.onGroupedHandled()
            onPhotosGrouped()
        }
    }

    // Surface errors via snackbar.
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.selectedCount > 0) {
                            stringResource(R.string.library_selected_count, state.selectedCount)
                        } else {
                            stringResource(R.string.library_title)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    if (state.selectedCount > 0) {
                        TextButton(onClick = viewModel::clearSelection) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.selectedCount >= 1,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.library_group_with_ai, state.selectedCount)) },
                    icon = {
                        if (state.grouping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Icon(Icons.Outlined.Tune, contentDescription = null)
                        }
                    },
                    onClick = { if (state.canGroup) viewModel.groupSelected() },
                    containerColor = if (state.canGroup) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.permission == PhotoAccess.NONE ->
                    PermissionRationale(
                        onGrant = { permissionLauncher.launch(PhotoPermissions.requestPermissions) },
                    )

                state.loading && state.photos.isEmpty() ->
                    LoadingState()

                state.isEmpty ->
                    EmptyLibraryState()

                else ->
                    PhotoGrid(
                        photos = state.photos,
                        selected = state.selectedUris,
                        onToggle = viewModel::toggleSelection,
                        onSetSelection = viewModel::setSelection,
                        showPartialBanner = state.permission == PhotoAccess.PARTIAL,
                        onManage = { permissionLauncher.launch(PhotoPermissions.requestPermissions) },
                    )
            }

            // Hint when only one photo is selected (need >= 2 to group).
            AnimatedVisibility(
                visible = state.selectedCount == 1 && !state.grouping,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        stringResource(R.string.library_select_min_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // Blocking overlay while grouping.
            AnimatedVisibility(
                visible = state.grouping,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                GroupingOverlay()
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSetSelection: (Set<String>) -> Unit,
    showPartialBanner: Boolean,
    onManage: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    // Latest selection, read inside the long-lived gesture handler without restarting it.
    val currentSelected by rememberUpdatedState(selected)

    // Drag-to-select state. The gesture starts after a long press on a thumbnail and then
    // extends a contiguous range (anchor..current) like the system photo picker. Items
    // outside the range revert to the selection captured when the drag began.
    var anchorIndex by remember { mutableStateOf(-1) }
    var dragSelecting by remember { mutableStateOf(true) }
    var baseSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    fun applyRange(currentIndex: Int) {
        if (anchorIndex < 0 || currentIndex < 0) return
        val lo = minOf(anchorIndex, currentIndex)
        val hi = maxOf(anchorIndex, currentIndex)
        val ranged = photos.subList(lo, hi + 1).map { it.uri }.toSet()
        onSetSelection(if (dragSelecting) baseSelection + ranged else baseSelection - ranged)
    }

    // Auto-scroll while a drag is held near the top/bottom edge, re-applying the range as
    // new rows scroll into view so selection keeps up with the finger.
    LaunchedEffect(dragging) {
        if (!dragging) return@LaunchedEffect
        while (dragging) {
            val viewportHeight = gridState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                val edge = 140f
                val y = pointer.y
                val delta = when {
                    y < edge -> -((edge - y) / edge) * 24f
                    y > viewportHeight - edge -> ((y - (viewportHeight - edge)) / edge) * 24f
                    else -> 0f
                }
                if (delta != 0f) {
                    gridState.scrollBy(delta)
                    applyRange(photoIndexAt(gridState, photos, pointer))
                }
            }
            withFrameNanos { }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photos) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val index = photoIndexAt(gridState, photos, offset)
                        if (index >= 0) {
                            anchorIndex = index
                            baseSelection = currentSelected
                            dragSelecting = photos[index].uri !in currentSelected
                            pointer = offset
                            dragging = true
                            applyRange(index)
                        } else {
                            anchorIndex = -1
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (anchorIndex >= 0) {
                            pointer = change.position
                            val index = photoIndexAt(gridState, photos, change.position)
                            if (index >= 0) applyRange(index)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        anchorIndex = -1
                    },
                    onDragCancel = {
                        dragging = false
                        anchorIndex = -1
                    },
                )
            },
    ) {
        if (showPartialBanner) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PartialAccessBanner(onManage = onManage)
            }
        }
        items(photos, key = { it.uri }) { photo ->
            PhotoThumbnail(
                photo = photo,
                selected = photo.uri in selected,
                onToggle = { onToggle(photo.uri) },
            )
        }
    }
}

/**
 * Maps a pointer position (local to the grid) to the index of the photo under it in
 * [photos], or -1 if the pointer is over empty space / the header banner.
 */
private fun photoIndexAt(
    gridState: LazyGridState,
    photos: List<Photo>,
    position: Offset,
): Int {
    val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        val key = info.key
        key is String &&
            position.x >= info.offset.x &&
            position.x <= info.offset.x + info.size.width &&
            position.y >= info.offset.y &&
            position.y <= info.offset.y + info.size.height
    } ?: return -1
    val uri = item.key as String
    return photos.indexOfFirst { it.uri == uri }
}

@Composable
private fun PhotoThumbnail(
    photo: Photo,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 0.86f else 1f, label = "thumbScale")
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(14.dp)),
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            )
        }

        // Selection check badge.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.28f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.library_selected_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(onManage: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.library_partial_access),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onManage) { Text(stringResource(R.string.action_manage)) }
        }
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    CenteredMessage(
        icon = Icons.Filled.PhotoLibrary,
        title = stringResource(R.string.library_permission_title),
        body = stringResource(R.string.library_permission_body),
    ) {
        Button(onClick = onGrant) { Text(stringResource(R.string.library_grant_access)) }
    }
}

@Composable
private fun EmptyLibraryState() {
    CenteredMessage(
        icon = Icons.Filled.PhotoLibrary,
        title = stringResource(R.string.library_empty_title),
        body = stringResource(R.string.library_empty_body),
    )
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.library_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun GroupingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.library_grouping_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    stringResource(R.string.library_grouping_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (action != null) {
            Box(Modifier.padding(top = 24.dp)) { action() }
        }
    }
}

/** Runs [onStart] whenever the lifecycle reaches the STARTED state. */
@Composable
private fun DisposableEffectOnStart(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onStart: () -> Unit,
) {
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) onStart()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
