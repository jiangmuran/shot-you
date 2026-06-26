package com.shotyou.app.ui.screens.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.PromptComposer
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.StylePreset
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.TemplateRepository
import com.shotyou.app.domain.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerateUiState(
    val group: PhotoGroup? = null,
    val selectedReferenceUris: Set<String> = emptySet(),
    val prompt: String = "",
    val templates: List<Template> = emptyList(),
    val style: StylePreset = StylePreset.REALISTIC,
    val intensity: Int = 50,
    val optimizing: Boolean = false,
    val enqueuing: Boolean = false,
    val error: String? = null,
) {
    val canGenerate: Boolean
        get() = group != null &&
            prompt.isNotBlank() &&
            selectedReferenceUris.isNotEmpty() &&
            !optimizing &&
            !enqueuing
}

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val templateRepository: TemplateRepository,
    private val generationRepository: GenerationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(InternalState())

    private val templates: StateFlow<List<Template>> = templateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<GenerateUiState> =
        combine(
            sessionStore.activeGroup,
            mutableState,
            templates,
            settingsRepository.settings,
        ) { group, state, templateList, settings ->
            // Initialise reference selection from the group the first time we see it,
            // and style/intensity from the user's saved defaults until they tweak them.
            val selection = state.selection ?: defaultSelection(group)
            GenerateUiState(
                group = group,
                selectedReferenceUris = selection,
                prompt = state.prompt,
                templates = templateList,
                style = state.style ?: StylePreset.fromId(settings.defaultStyle),
                intensity = state.intensity ?: settings.defaultIntensity,
                optimizing = state.optimizing,
                enqueuing = state.enqueuing,
                error = state.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenerateUiState())

    fun toggleReference(uri: String) = mutableState.update { state ->
        val current = state.selection ?: defaultSelection(sessionStore.activeGroup.value)
        val next = if (uri in current) current - uri else current + uri
        // Keep at least one reference selected.
        if (next.isEmpty()) state else state.copy(selection = next)
    }

    fun onPromptChange(value: String) = mutableState.update { it.copy(prompt = value) }

    fun setStyle(style: StylePreset) = mutableState.update { it.copy(style = style) }

    fun setIntensity(intensity: Int) = mutableState.update { it.copy(intensity = intensity.coerceIn(0, 100)) }

    fun applyTemplate(template: Template) = mutableState.update { it.copy(prompt = template.prompt) }

    fun appendAspect(aspect: String) = mutableState.update { state ->
        val hint = aspectHint(aspect)
        val sep = if (state.prompt.isBlank() || state.prompt.endsWith(" ")) "" else " "
        state.copy(prompt = state.prompt + sep + hint)
    }

    fun consumeError() = mutableState.update { it.copy(error = null) }

    fun optimize() {
        val state = mutableState.value
        if (state.optimizing) return
        val group = sessionStore.activeGroup.value
        val raw = state.prompt
        if (raw.isBlank()) return
        mutableState.update { it.copy(optimizing = true, error = null) }
        viewModelScope.launch {
            try {
                val optimized = templateRepository.optimizePrompt(raw, group?.title, group?.reason)
                mutableState.update { it.copy(prompt = optimized, optimizing = false) }
            } catch (e: AiException) {
                mutableState.update { it.copy(optimizing = false, error = e.message ?: "Optimization failed") }
            } catch (e: Exception) {
                mutableState.update { it.copy(optimizing = false, error = e.message ?: "Optimization failed") }
            }
        }
    }

    fun generate(onJobEnqueued: (String) -> Unit) {
        val state = mutableState.value
        val group = sessionStore.activeGroup.value ?: return
        if (state.enqueuing || state.prompt.isBlank()) return
        val references = (state.selection ?: defaultSelection(group)).toList()
        if (references.isEmpty()) return
        mutableState.update { it.copy(enqueuing = true, error = null) }
        viewModelScope.launch {
            try {
                val settings = settingsRepository.current()
                val style = state.style ?: StylePreset.fromId(settings.defaultStyle)
                val intensity = state.intensity ?: settings.defaultIntensity
                val finalPrompt = PromptComposer.compose(
                    basePrompt = state.prompt,
                    style = style,
                    intensity = intensity,
                    suggestion = null,
                )
                val jobId = generationRepository.enqueue(
                    groupId = group.id,
                    groupTitle = group.title,
                    prompt = finalPrompt,
                    referenceUris = references,
                )
                mutableState.update { it.copy(enqueuing = false) }
                onJobEnqueued(jobId)
            } catch (e: Exception) {
                mutableState.update { it.copy(enqueuing = false, error = e.message ?: "Could not start generation") }
            }
        }
    }

    private data class InternalState(
        val selection: Set<String>? = null,
        val prompt: String = "",
        val style: StylePreset? = null,
        val intensity: Int? = null,
        val optimizing: Boolean = false,
        val enqueuing: Boolean = false,
        val error: String? = null,
    )

    private inline fun MutableStateFlow<InternalState>.update(transform: (InternalState) -> InternalState) {
        value = transform(value)
    }

    private companion object {
        fun defaultSelection(group: PhotoGroup?): Set<String> = when {
            group == null -> emptySet()
            group.referenceUris.isNotEmpty() -> group.referenceUris.toSet()
            else -> group.photoUris.toSet()
        }

        fun aspectHint(aspect: String): String = when (aspect) {
            "hair" -> "Adjust the hair: "
            "expression" -> "Change the expression: "
            "pose" -> "Refine the pose: "
            "position" -> "Reposition the subject: "
            else -> "$aspect: "
        }
    }
}
