package com.shotyou.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Section header used to group cards within the settings list. */
@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** A standard elevated card wrapper with consistent inner padding. */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

/**
 * Editable text field whose value is the source of truth held in settings. Local state is
 * synced from [value] only when it actually changes, so typing is never interrupted by the
 * asynchronous DataStore round-trip.
 */
@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Masked API-key field with a show/hide eye toggle. */
@Composable
fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value) }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = { Text("Stored on-device only") },
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (revealed) "Hide key" else "Show key",
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

/** A labelled row with a trailing [Switch] and an optional description line. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
