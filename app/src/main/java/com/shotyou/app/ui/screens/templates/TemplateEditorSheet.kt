package com.shotyou.app.ui.screens.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shotyou.app.R
import com.shotyou.app.domain.model.Template

/** Parses a comma-separated tag string into a trimmed, non-blank list. */
internal fun parseTags(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Bottom-sheet editor for a [Template]. Built-in templates open read-only and offer a
 * "Duplicate" action that hands back an editable copy ([Template.builtIn] = false, id 0).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorSheet(
    template: Template,
    onSave: (Template) -> Unit,
    onDelete: (Long) -> Unit,
    onDuplicate: (Template) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val readOnly = template.builtIn

    var name by remember { mutableStateOf(template.name) }
    var prompt by remember { mutableStateOf(template.prompt) }
    var tags by remember { mutableStateOf(template.tags.joinToString(", ")) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        when {
                            readOnly -> R.string.templates_builtin_title
                            template.id == 0L -> R.string.templates_new_title
                            else -> R.string.templates_edit_title
                        },
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (readOnly) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.templates_readonly)) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.templates_field_name)) },
                singleLine = true,
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.templates_field_prompt)) },
                readOnly = readOnly,
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(stringResource(R.string.templates_field_tags)) },
                supportingText = { Text(stringResource(R.string.templates_tags_helper)) },
                singleLine = true,
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth(),
            )

            if (readOnly) {
                val copyName = stringResource(R.string.templates_copy_suffix, template.name)
                Button(
                    onClick = {
                        onDuplicate(
                            template.copy(
                                id = 0L,
                                name = copyName,
                                builtIn = false,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.templates_duplicate))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (template.id != 0L) {
                        OutlinedButton(
                            onClick = { onDelete(template.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.templates_delete))
                        }
                    }
                    Button(
                        onClick = {
                            onSave(
                                template.copy(
                                    name = name.trim(),
                                    prompt = prompt.trim(),
                                    tags = parseTags(tags),
                                ),
                            )
                        },
                        enabled = name.isNotBlank() && prompt.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.templates_save))
                    }
                }
            }
        }
    }
}
