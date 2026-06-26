package com.shotyou.app.ui.screens.usage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.UsageRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    viewModel: UsageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage") },
                actions = {
                    if (state.totalCalls > 0) {
                        TextButton(onClick = viewModel::clear) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.width(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.totalCalls == 0) {
            EmptyUsage(padding)
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total calls", state.totalCalls.toString(), Modifier.weight(1f))
                    SummaryCard("Est. cost", "$" + "%.4f".format(state.totalCostUsd), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Images", state.totalImages.toString(), Modifier.weight(1f))
                    SummaryCard("Tokens", formatCount(state.totalTokens), Modifier.weight(1f))
                }
            }
            item {
                SectionCard("By operation") {
                    val maxOp = state.byOperation.maxOfOrNull { it.second } ?: 1
                    state.byOperation.forEach { (op, count) ->
                        BarRow(op.label, count, maxOp, operationColor(op))
                    }
                }
            }
            item {
                SectionCard("By provider") {
                    val maxProv = state.byProvider.maxOfOrNull { it.second } ?: 1
                    state.byProvider.forEach { (provider, count) ->
                        BarRow(provider, count, maxProv, MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Text(
                    "Recent calls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.recent, key = { it.id }) { record ->
                RecentRow(record)
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BarRow(label: String, count: Int, max: Int, color: Color) {
    val fraction = if (max <= 0) 0f else (count.toFloat() / max).coerceIn(0.02f, 1f)
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun RecentRow(record: UsageRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (record.success) Color(0xFF2E7D32) else Color(0xFFC62828)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    record.operation.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${record.provider} · ${record.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$" + "%.4f".format(record.estimatedCostUsd),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatTime(record.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyUsage(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No usage yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grouping, prompt and image-generation calls will be tallied here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val AiOperation.label: String
    get() = when (this) {
        AiOperation.GROUPING -> "Grouping"
        AiOperation.PROMPT_OPTIMIZE -> "Prompt optimize"
        AiOperation.IMAGE_GENERATION -> "Image generation"
    }

private fun operationColor(op: AiOperation): Color = when (op) {
    AiOperation.GROUPING -> Color(0xFF6750A4)
    AiOperation.PROMPT_OPTIMIZE -> Color(0xFF1565C0)
    AiOperation.IMAGE_GENERATION -> Color(0xFF2E7D32)
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))
