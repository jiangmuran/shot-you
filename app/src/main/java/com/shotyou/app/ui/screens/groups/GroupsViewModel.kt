package com.shotyou.app.ui.screens.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.ai.PromptComposer
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.StylePreset
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.GenerationVariant
import com.shotyou.app.domain.repository.SessionRepository
import com.shotyou.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
private data class GroupOverride(val included: Boolean? = null, val hint: String? = null)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val generationRepository: GenerationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"]) {
        "GroupsViewModel requires a sessionId route argument"
    }

    /** The session's groups, observed from the repository (built by background classification). */
    private val groups: StateFlow<List<PhotoGroup>> =
        sessionRepository.observeSession(sessionId)
            .map { it?.groups ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val overrides = MutableStateFlow<Map<String, GroupOverride>>(emptyMap())
    private val starting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(groups, overrides, starting, error) { groups, ov, isStarting, err ->
            val items = groups.map { group ->
                val o = ov[group.id]
                CurationItem(
                    group = group,
                    // Default to CHECKED so the user can always Start; the VLM's "skip"
                    // suggestion is shown as a badge (group.recommended) rather than auto-unchecking.
                    included = o?.included ?: true,
                    hint = o?.hint ?: defaultHint(group),
                )
            }
            GroupsUiState(items = items, starting = isStarting, error = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    fun toggleIncluded(groupId: String) = overrides.update { map ->
        val current = map[groupId]
        // Must match the display default (CHECKED) so a single tap actually toggles.
        val now = current?.included ?: true
        map + (groupId to (current ?: GroupOverride()).copy(included = !now))
    }

    fun onHintChange(groupId: String, value: String) = overrides.update { map ->
        map + (groupId to (map[groupId] ?: GroupOverride()).copy(hint = value))
    }

    fun consumeError() { error.value = null }

    /** Enqueue a batch for every checked group, mark the session GENERATING, then navigate
     *  to the queue via [onStarted]. */
    fun start(onStarted: () -> Unit) {
        if (starting.value) return
        val chosen = uiState.value.items.filter { it.included }
        if (chosen.isEmpty()) return
        starting.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val settings = settingsRepository.current()
                chosen.forEach { item ->
                    val group = item.group
                    // Per-category style rule (e.g. people→portrait, scenery→travel), else default.
                    val style = StylePreset.fromId(settings.styleFor(group.category))
                    val basePrompt = item.hint.ifBlank { defaultBasePrompt(group) }
                    val variants = PromptComposer.variants(
                        basePrompt = basePrompt,
                        style = style,
                        intensity = settings.defaultIntensity,
                        count = settings.candidatesPerItem,
                        chinese = com.shotyou.app.util.LangUtil.isChinese(),
                    ).map { (label, p) -> GenerationVariant(p, label) }
                    generationRepository.enqueueBatch(
                        groupId = group.id,
                        groupTitle = group.title,
                        variants = variants,
                        referenceUris = group.referenceUris.ifEmpty { group.photoUris },
                    )
                }
                runCatching { sessionRepository.markGenerating(sessionId) }
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
        // The prompt is an INSTRUCTION (what to do) — the reference photos carry the actual
        // content. Do NOT prefill it with the VLM's description/reason.
        fun defaultHint(group: PhotoGroup): String = defaultBasePrompt(group)

        fun defaultBasePrompt(group: PhotoGroup): String =
            if (com.shotyou.app.util.LangUtil.isChinese()) {
                "把这组照片融合并精修成一张最自然、好看的照片"
            } else {
                "Merge and refine this group into one natural, flattering photo"
            }
    }
}
