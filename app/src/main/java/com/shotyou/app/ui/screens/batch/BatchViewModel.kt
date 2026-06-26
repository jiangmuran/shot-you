package com.shotyou.app.ui.screens.batch

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus
import com.shotyou.app.domain.repository.DeleteOutcome
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchUiState(
    val jobs: List<GenerationJob> = emptyList(),
    val originalUris: List<String> = emptyList(),
    val groupTitle: String? = null,
    val refining: Boolean = false,
    val deleting: Boolean = false,
    /** Number of candidates the last time the batch updated, so the UI can scroll to new ones. */
    val candidateRevision: Int = 0,
    val error: String? = null,
    val deletedCount: Int? = null,
) {
    val hasSucceeded: Boolean get() = jobs.any { it.status == JobStatus.SUCCEEDED }
}

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
    private val photoRepository: PhotoRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(batchId: String) {
        if (loaded) return
        loaded = true
        // Original photos come from the group the user is working on.
        val group = sessionStore.activeGroup.value
        _uiState.update { it.copy(originalUris = group?.photoUris.orEmpty(), groupTitle = group?.title) }
        viewModelScope.launch {
            generationRepository.observeBatch(batchId).collect { jobs ->
                val ordered = jobs.sortedBy { it.variantIndex }
                _uiState.update { it.copy(jobs = ordered, candidateRevision = ordered.size) }
            }
        }
    }

    fun retry(jobId: String) {
        viewModelScope.launch {
            try {
                generationRepository.retry(jobId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Retry failed") }
            }
        }
    }

    /**
     * Spin up a new "ask for changes" candidate that iterates on [current], reusing its prompt
     * plus the user's [suggestion] and feeding the current image back in as a reference.
     */
    fun askForChanges(current: GenerationJob, suggestion: String, label: String, batchId: String) {
        val resultUri = current.resultUri
        if (_uiState.value.refining || suggestion.isBlank() || resultUri == null) return
        _uiState.update { it.copy(refining = true, error = null) }
        viewModelScope.launch {
            try {
                val prompt = buildString {
                    append(current.prompt.trim())
                    append("\n\nRequested changes: ")
                    append(suggestion.trim())
                }
                val references = (listOf(resultUri) + current.referenceUris).distinct()
                generationRepository.addVariant(
                    batchId = batchId,
                    prompt = prompt,
                    label = label,
                    referenceUris = references,
                )
                _uiState.update { it.copy(refining = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(refining = false, error = e.message ?: "Could not refine") }
            }
        }
    }

    /**
     * Delete the given original photos. If the platform needs the user's consent the returned
     * [IntentSender] is handed to [onConsent] so the composable can launch it.
     */
    fun deleteOriginals(uris: List<String>, onConsent: (IntentSender) -> Unit) {
        if (_uiState.value.deleting || uris.isEmpty()) return
        _uiState.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                when (val outcome = photoRepository.deletePhotos(uris)) {
                    is DeleteOutcome.Deleted ->
                        _uiState.update { it.copy(deleting = false, deletedCount = outcome.count) }
                    is DeleteOutcome.NeedsConsent -> {
                        _uiState.update { it.copy(deleting = false) }
                        onConsent(outcome.intentSender)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(deleting = false, error = e.message ?: "Could not delete photos") }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun consumeDeleted() = _uiState.update { it.copy(deletedCount = null) }
}
