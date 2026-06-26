package com.shotyou.app.ui.screens.generate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shotyou.app.domain.model.Template

private val EDITABLE_ASPECTS = listOf("hair", "expression", "pose", "position")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onJobEnqueued: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val group = state.group
        if (group == null) {
            NoGroupSelected(modifier = Modifier.padding(padding), onBack = onBack)
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // 1. References
            SectionHeader("References", "Tap to choose which shots feed the model")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(group.photoUris, key = { it }) { uri ->
                    SelectableThumbnail(
                        uri = uri,
                        selected = uri in state.selectedReferenceUris,
                        onClick = { viewModel.toggleReference(uri) },
                    )
                }
            }

            // 2. Template
            SectionHeader("Template", "Start from a saved prompt", modifier = Modifier.padding(top = 24.dp))
            if (state.templates.isEmpty()) {
                Text(
                    text = "No templates saved yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.templates, key = { it.id }) { template ->
                        TemplateChip(template = template, onClick = { viewModel.applyTemplate(template) })
                    }
                }
            }

            // 3. Prompt
            SectionHeader("Prompt", "Describe the shot you want", modifier = Modifier.padding(top = 24.dp))
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::onPromptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                minLines = 4,
                placeholder = { Text("e.g. studio portrait, soft window light, confident smile") },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EDITABLE_ASPECTS.forEach { aspect ->
                    AssistChip(
                        onClick = { viewModel.appendAspect(aspect) },
                        label = { Text(aspect) },
                    )
                }
            }
            OutlinedButton(
                onClick = viewModel::optimize,
                enabled = state.prompt.isNotBlank() && !state.optimizing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                if (state.optimizing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Optimizing…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Optimize with AI", modifier = Modifier.padding(start = 8.dp))
                }
            }

            // 4. Generate
            Button(
                onClick = { viewModel.generate(onJobEnqueued) },
                enabled = state.canGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp),
            ) {
                if (state.enqueuing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text("Generate", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectableThumbnail(uri: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(14.dp),
                )
            }
        } else {
            // Dim unselected references so the chosen ones stand out.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateChip(template: Template, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(template.name) },
        leadingIcon = {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun NoGroupSelected(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No group selected", style = MaterialTheme.typography.titleMedium)
        Text(
            "Go back and pick a group to generate from.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text("Back to Groups")
        }
    }
}
