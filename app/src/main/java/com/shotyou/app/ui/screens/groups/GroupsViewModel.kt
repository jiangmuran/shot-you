package com.shotyou.app.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.ai.PromptComposer
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.StylePreset
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.GenerationVariant
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row in the curation screen: a group plus the user's include / prompt-hint choices. */
data class CurationItem(
    val group: PhotoGroup,
    val included: Boolean,
    val hint: String,
)

data class GroupsUiState(
    val items: List<CurationItem> = emptyList(),
    val starting: Boolean = false,
    val error: String? = null,
) {
    val selectedCount: Int get() = items.count { it.included }
    val hasGroups: Boolean get() = items.isNotEmpty()
}

/** The user's per-group overrides, keyed by group id. */
private data class Override(val included: Boolean? = null, val hint: String? = null)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val generationRepository: GenerationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val overrides = MutableStateFlow<Map<String, Override>>(emptyMap())
    private val starting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(sessionStore.groups, overrides, starting, error) { groups, ov, isStarting, err ->
            val items = groups.map { group ->
                val o = ov[group.id]
                CurationItem(
                    group = group,
                    included = o?.included ?: group.recommended,
                    hint = o?.hint ?: defaultHint(group),
                )
            }
            GroupsUiState(items = items, starting = isStarting, error = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    fun toggleIncluded(groupId: String) = overrides.update { map ->
        val group = sessionStore.groups.value.firstOrNull { it.id == groupId } ?: return@update map
        val current = map[groupId]
        val now = current?.included ?: group.recommended
        map + (groupId to (current ?: Override()).copy(included = !now))
    }

    fun onHintChange(groupId: String, value: String) = overrides.update { map ->
        map + (groupId to (map[groupId] ?: Override()).copy(hint = value))
    }

    fun consumeError() { error.value = null }

    /** Enqueue a batch for every checked group, then navigate to the queue via [onStarted]. */
    fun start(onStarted: () -> Unit) {
        if (starting.value) return
        val chosen = uiState.value.items.filter { it.included }
        if (chosen.isEmpty()) return
        starting.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val settings = settingsRepository.current()
                val style = StylePreset.fromId(settings.defaultStyle)
                chosen.forEach { item ->
                    val group = item.group
                    val basePrompt = item.hint.ifBlank { defaultBasePrompt(group) }
                    val variants = PromptComposer.variants(
                        basePrompt = basePrompt,
                        style = style,
                        intensity = settings.defaultIntensity,
                        count = settings.candidatesPerItem,
                    ).map { (label, p) -> GenerationVariant(p, label) }
                    generationRepository.enqueueBatch(
                        groupId = group.id,
                        groupTitle = group.title,
                        variants = variants,
                        referenceUris = group.referenceUris.ifEmpty { group.photoUris },
                    )
                }
                starting.value = false
                onStarted()
            } catch (e: Exception) {
                starting.value = false
                error.value = e.message
            }
        }
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }

    private companion object {
        /** Prompt-hint prefill: the VLM's reason, falling back to the group title. */
        fun defaultHint(group: PhotoGroup): String =
            group.reason.ifBlank { group.title }

        /** Fallback base prompt when the hint is cleared: built from the category + title. */
        fun defaultBasePrompt(group: PhotoGroup): String {
            val cat = group.category?.takeIf { it.isNotBlank() }
            return when {
                !group.title.isBlank() && cat != null -> "${group.title} ($cat)"
                !group.title.isBlank() -> group.title
                cat != null -> cat
                else -> "portrait"
            }
        }
    }
}
