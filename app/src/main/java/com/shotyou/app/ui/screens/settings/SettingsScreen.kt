package com.shotyou.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shotyou.app.domain.model.AiProviderType
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.DefaultModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = AiSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item { StatusBanner(settings) }

            item { SettingsSectionHeader("AI Providers") }
            item {
                ProviderRoleCard(
                    role = "Vision (VLM)",
                    description = "Groups your photos by understanding their content.",
                    provider = settings.vlmProvider,
                    model = settings.vlmModel,
                    onProviderChange = { p -> viewModel.update { it.copy(vlmProvider = p) } },
                    onModelChange = { m -> viewModel.update { it.copy(vlmModel = m) } },
                    placeholderFor = { defaultVlm(it) },
                )
            }
            item {
                ProviderRoleCard(
                    role = "Prompt (LLM)",
                    description = "Refines and optimises your text prompts.",
                    provider = settings.llmProvider,
                    model = settings.llmModel,
                    onProviderChange = { p -> viewModel.update { it.copy(llmProvider = p) } },
                    onModelChange = { m -> viewModel.update { it.copy(llmModel = m) } },
                    placeholderFor = { defaultLlm(it) },
                )
            }
            item {
                ProviderRoleCard(
                    role = "Image",
                    description = "Generates the final images.",
                    provider = settings.imageProvider,
                    model = settings.imageModel,
                    onProviderChange = { p -> viewModel.update { it.copy(imageProvider = p) } },
                    onModelChange = { m -> viewModel.update { it.copy(imageModel = m) } },
                    placeholderFor = { defaultImage(it) },
                )
            }

            item { SettingsSectionHeader("API Keys") }
            item {
                SettingsCard {
                    ApiKeyField(
                        value = settings.geminiKey,
                        onValueChange = { k -> viewModel.update { it.copy(geminiKey = k) } },
                        label = "${AiProviderType.GEMINI.displayName} key",
                    )
                    ApiKeyField(
                        value = settings.openAiKey,
                        onValueChange = { k -> viewModel.update { it.copy(openAiKey = k) } },
                        label = "${AiProviderType.OPENAI.displayName} key",
                    )
                }
            }

            item { SettingsSectionHeader("Queue & quota") }
            item {
                SettingsCard {
                    SliderRow(
                        title = "Max concurrent jobs",
                        value = settings.maxConcurrentJobs.toFloat(),
                        valueLabel = settings.maxConcurrentJobs.toString(),
                        range = 1f..4f,
                        steps = 2,
                        onValueChange = { v ->
                            viewModel.update { it.copy(maxConcurrentJobs = v.toInt()) }
                        },
                    )
                    SliderRow(
                        title = "Min request interval",
                        value = settings.minRequestIntervalMs.toFloat(),
                        valueLabel = formatInterval(settings.minRequestIntervalMs),
                        range = 0f..5000f,
                        steps = 9,
                        onValueChange = { v ->
                            viewModel.update { it.copy(minRequestIntervalMs = v.toLong()) }
                        },
                    )
                    SwitchRow(
                        title = "Auto-retry on failure",
                        checked = settings.autoRetryOnFailure,
                        onCheckedChange = { c -> viewModel.update { it.copy(autoRetryOnFailure = c) } },
                    )
                    SliderRow(
                        title = "Max retries",
                        value = settings.maxRetries.toFloat(),
                        valueLabel = settings.maxRetries.toString(),
                        range = 0f..5f,
                        steps = 4,
                        enabled = settings.autoRetryOnFailure,
                        onValueChange = { v -> viewModel.update { it.copy(maxRetries = v.toInt()) } },
                    )
                    SwitchRow(
                        title = "Require Wi-Fi",
                        description = "Only run jobs while connected to Wi-Fi.",
                        checked = settings.requireWifi,
                        onCheckedChange = { c -> viewModel.update { it.copy(requireWifi = c) } },
                    )
                }
            }

            item { SettingsSectionHeader("Prompt") }
            item {
                SettingsCard {
                    SwitchRow(
                        title = "Auto-optimise prompts",
                        description = "Let the prompt LLM refine each prompt before image generation.",
                        checked = settings.autoOptimizePrompt,
                        onCheckedChange = { c -> viewModel.update { it.copy(autoOptimizePrompt = c) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(settings: AiSettings) {
    val configured = settings.isConfigured
    val container = if (configured) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = if (configured) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val icon: ImageVector = if (configured) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(
                    if (configured) "Ready to go" else "Setup needed",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (configured) {
                        "All selected providers have an API key."
                    } else {
                        "Add an API key below for each provider you use."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderRoleCard(
    role: String,
    description: String,
    provider: AiProviderType,
    model: String,
    onProviderChange: (AiProviderType) -> Unit,
    onModelChange: (String) -> Unit,
    placeholderFor: (AiProviderType) -> String,
) {
    SettingsCard {
        Text(role, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val options = AiProviderType.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = provider == type,
                    onClick = { onProviderChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(type.displayName)
                }
            }
        }
        SettingsTextField(
            value = model,
            onValueChange = onModelChange,
            label = "Model id",
            placeholder = placeholderFor(provider),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
        )
    }
}

private fun formatInterval(ms: Long): String =
    if (ms == 0L) "Off" else if (ms < 1000) "$ms ms" else "%.1f s".format(ms / 1000f)

private fun defaultVlm(type: AiProviderType): String = when (type) {
    AiProviderType.GEMINI -> DefaultModels.GEMINI_VLM
    AiProviderType.OPENAI -> DefaultModels.OPENAI_VLM
}

private fun defaultLlm(type: AiProviderType): String = when (type) {
    AiProviderType.GEMINI -> DefaultModels.GEMINI_LLM
    AiProviderType.OPENAI -> DefaultModels.OPENAI_LLM
}

private fun defaultImage(type: AiProviderType): String = when (type) {
    AiProviderType.GEMINI -> DefaultModels.GEMINI_IMAGE
    AiProviderType.OPENAI -> DefaultModels.OPENAI_IMAGE
}
