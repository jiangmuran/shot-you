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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                        if (state.selectedCount > 0) "${state.selectedCount} selected" else "Library",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    if (state.selectedCount > 0) {
                        TextButton(onClick = viewModel::clearSelection) { Text("Clear") }
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
                    text = { Text("Group ${state.selectedCount} with AI") },
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
                        "Select at least 2 photos to group",
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
    showPartialBanner: Boolean,
    onManage: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
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
                    contentDescription = "Selected",
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
                "You've granted access to selected photos",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onManage) { Text("Manage") }
        }
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    CenteredMessage(
        icon = Icons.Filled.PhotoLibrary,
        title = "Access your photos",
        body = "Shot You needs access to your gallery so you can pick photos and group the best shots with AI.",
    ) {
        Button(onClick = onGrant) { Text("Grant access") }
    }
}

@Composable
private fun EmptyLibraryState() {
    CenteredMessage(
        icon = Icons.Filled.PhotoLibrary,
        title = "No photos yet",
        body = "Photos you take will show up here, ready to group and enhance with AI.",
    )
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                "Loading your photos…",
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
                    "Grouping with AI…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    "Finding the best shots in your selection",
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
