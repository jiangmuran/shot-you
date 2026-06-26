package com.shotyou.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shotyou.app.R
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.DefaultModels
import com.shotyou.app.domain.model.StylePreset
import com.shotyou.app.ui.labelRes
import com.shotyou.app.util.AppLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = AiSettings())
    val context = LocalContext.current

    // Ask for notification permission the first time the user enables a background feature
    // that needs to post notifications (Android 13+).
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled by the OS; the toggle itself is already persisted */ }

    fun requestNotificationsIfNeeded(enabling: Boolean) {
        if (!enabling) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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

            // 1. API
            item { SettingsSectionHeader(stringResource(R.string.settings_section_api)) }
            item {
                SettingsCard {
                    SettingsTextField(
                        value = settings.apiBaseUrl,
                        onValueChange = { v -> viewModel.update { it.copy(apiBaseUrl = v) } },
                        label = stringResource(R.string.settings_api_host_label),
                        placeholder = DefaultModels.API_BASE_URL,
                        supportingText = stringResource(R.string.settings_api_host_helper),
                    )
                    ApiKeyField(
                        value = settings.apiKey,
                        onValueChange = { v -> viewModel.update { it.copy(apiKey = v) } },
                        label = stringResource(R.string.settings_api_key_label),
                    )
                    SettingsTextField(
                        value = settings.vlmModel,
                        onValueChange = { v -> viewModel.update { it.copy(vlmModel = v) } },
                        label = stringResource(R.string.settings_model_vlm_label),
                        placeholder = DefaultModels.VLM,
                        supportingText = stringResource(R.string.settings_model_vlm_helper),
                    )
                    SettingsTextField(
                        value = settings.llmModel,
                        onValueChange = { v -> viewModel.update { it.copy(llmModel = v) } },
                        label = stringResource(R.string.settings_model_llm_label),
                        placeholder = DefaultModels.LLM,
                        supportingText = stringResource(R.string.settings_model_llm_helper),
                    )
                    SettingsTextField(
                        value = settings.imageModel,
                        onValueChange = { v -> viewModel.update { it.copy(imageModel = v) } },
                        label = stringResource(R.string.settings_model_image_label),
                        placeholder = DefaultModels.IMAGE,
                        supportingText = stringResource(R.string.settings_model_image_helper),
                    )
                }
            }

            // 2. Generation defaults
            item { SettingsSectionHeader(stringResource(R.string.settings_section_generation)) }
            item {
                SettingsCard {
                    Text(
                        stringResource(R.string.settings_default_style),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    StyleChips(
                        selectedId = settings.defaultStyle,
                        onSelect = { id -> viewModel.update { it.copy(defaultStyle = id) } },
                    )
                    SliderRow(
                        title = stringResource(R.string.settings_default_intensity),
                        value = settings.defaultIntensity.toFloat(),
                        valueLabel = stringResource(R.string.generate_intensity_value, settings.defaultIntensity),
                        range = 0f..100f,
                        steps = 0,
                        onValueChange = { v -> viewModel.update { it.copy(defaultIntensity = v.toInt()) } },
                    )
                }
            }

            // 3. Pricing
            item { SettingsSectionHeader(stringResource(R.string.settings_section_pricing)) }
            item {
                SettingsCard {
                    SettingsPriceField(
                        value = settings.pricePerImage,
                        onValueChange = { v -> viewModel.update { it.copy(pricePerImage = v) } },
                        label = stringResource(R.string.settings_price_per_image),
                    )
                    SettingsPriceField(
                        value = settings.pricePer1kInputTokens,
                        onValueChange = { v -> viewModel.update { it.copy(pricePer1kInputTokens = v) } },
                        label = stringResource(R.string.settings_price_per_1k_input),
                    )
                    SettingsPriceField(
                        value = settings.pricePer1kOutputTokens,
                        onValueChange = { v -> viewModel.update { it.copy(pricePer1kOutputTokens = v) } },
                        label = stringResource(R.string.settings_price_per_1k_output),
                    )
                    SettingsTextField(
                        value = settings.currencySymbol,
                        onValueChange = { v -> viewModel.update { it.copy(currencySymbol = v) } },
                        label = stringResource(R.string.settings_currency_symbol),
                        supportingText = stringResource(R.string.settings_pricing_helper),
                    )
                }
            }

            // 4. Queue
            item { SettingsSectionHeader(stringResource(R.string.settings_section_queue)) }
            item {
                SettingsCard {
                    SliderRow(
                        title = stringResource(R.string.settings_max_concurrent),
                        value = settings.maxConcurrentJobs.toFloat(),
                        valueLabel = settings.maxConcurrentJobs.toString(),
                        range = 1f..4f,
                        steps = 2,
                        onValueChange = { v -> viewModel.update { it.copy(maxConcurrentJobs = v.toInt()) } },
                    )
                    SliderRow(
                        title = stringResource(R.string.settings_min_interval),
                        value = settings.minRequestIntervalMs.toFloat(),
                        valueLabel = intervalLabel(settings.minRequestIntervalMs),
                        range = 0f..5000f,
                        steps = 9,
                        onValueChange = { v -> viewModel.update { it.copy(minRequestIntervalMs = v.toLong()) } },
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_auto_retry),
                        checked = settings.autoRetryOnFailure,
                        onCheckedChange = { c -> viewModel.update { it.copy(autoRetryOnFailure = c) } },
                    )
                    SliderRow(
                        title = stringResource(R.string.settings_max_retries),
                        value = settings.maxRetries.toFloat(),
                        valueLabel = settings.maxRetries.toString(),
                        range = 0f..5f,
                        steps = 4,
                        enabled = settings.autoRetryOnFailure,
                        onValueChange = { v -> viewModel.update { it.copy(maxRetries = v.toInt()) } },
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_require_wifi),
                        description = stringResource(R.string.settings_require_wifi_desc),
                        checked = settings.requireWifi,
                        onCheckedChange = { c -> viewModel.update { it.copy(requireWifi = c) } },
                    )
                }
            }

            // 5. Background
            item { SettingsSectionHeader(stringResource(R.string.settings_section_background)) }
            item {
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_run_in_background),
                        description = stringResource(R.string.settings_run_in_background_desc),
                        checked = settings.runInBackground,
                        onCheckedChange = { c ->
                            requestNotificationsIfNeeded(c)
                            viewModel.update { it.copy(runInBackground = c) }
                        },
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_progress_notifications),
                        description = stringResource(R.string.settings_progress_notifications_desc),
                        checked = settings.progressNotifications,
                        onCheckedChange = { c ->
                            requestNotificationsIfNeeded(c)
                            viewModel.update { it.copy(progressNotifications = c) }
                        },
                    )
                }
            }

            // 6. Prompt
            item { SettingsSectionHeader(stringResource(R.string.settings_section_prompt)) }
            item {
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_auto_optimize),
                        description = stringResource(R.string.settings_auto_optimize_desc),
                        checked = settings.autoOptimizePrompt,
                        onCheckedChange = { c -> viewModel.update { it.copy(autoOptimizePrompt = c) } },
                    )
                }
            }

            // 7. Language
            item { SettingsSectionHeader(stringResource(R.string.settings_section_language)) }
            item {
                SettingsCard {
                    LanguageSelector(
                        current = settings.appLanguage,
                        onSelect = { tag ->
                            AppLocale.apply(tag)
                            viewModel.update { it.copy(appLanguage = tag) }
                        },
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
                    stringResource(
                        if (configured) R.string.settings_status_ready_title else R.string.settings_status_setup_title,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        if (configured) R.string.settings_status_ready_body else R.string.settings_status_setup_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StyleChips(selectedId: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StylePreset.entries.forEach { preset ->
            FilterChip(
                selected = selectedId == preset.id,
                onClick = { onSelect(preset.id) },
                label = { Text(stringResource(preset.labelRes())) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(current: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "system" to R.string.settings_language_system,
        "zh" to R.string.settings_language_zh,
        "en" to R.string.settings_language_en,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (tag, labelRes) ->
            SegmentedButton(
                selected = current == tag,
                onClick = { onSelect(tag) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(stringResource(labelRes))
            }
        }
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

@Composable
private fun intervalLabel(ms: Long): String = when {
    ms == 0L -> stringResource(R.string.settings_interval_off)
    ms < 1000 -> stringResource(R.string.settings_interval_ms, ms.toInt())
    else -> stringResource(R.string.settings_interval_seconds, ms / 1000f)
}
