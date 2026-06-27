package com.shotyou.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.repository.SessionRepository
import com.shotyou.app.util.PhotoAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable UI state for the photo-library screen. */
data class LibraryUiState(
    val permission: PhotoAccess = PhotoAccess.NONE,
    val photos: List<Photo> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    /** Uris already sent to a classification session (for "already processed" marking). */
    val processedUris: Set<String> = emptySet(),
    val loading: Boolean = false,
    val starting: Boolean = false,
    val error: String? = null,
) {
    val selectedCount: Int get() = selectedUris.size
    val canClassify: Boolean get() = selectedCount >= 2 && !starting
    val isEmpty: Boolean get() = !loading && photos.isEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /** One-shot signal: emit true once a classification session has been enqueued so the
     *  screen can navigate to the Queue. */
    private val _classificationStarted = MutableStateFlow(false)
    val classificationStarted: StateFlow<Boolean> = _classificationStarted.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeProcessedUris().collect { processed ->
                _uiState.update { it.copy(processedUris = processed) }
            }
        }
    }

    /** Called by the screen whenever the permission state is (re)computed. */
    fun onPermissionResult(access: PhotoAccess) {
        _uiState.update { it.copy(permission = access) }
        if (access != PhotoAccess.NONE) {
            // Always reload on a non-NONE result: with PARTIAL access the user re-opens the
            // photo picker to add more, and the access value stays PARTIAL — so we must refresh
            // to pick up the newly-selected photos.
            refresh()
        } else {
            _uiState.update { it.copy(photos = emptyList(), selectedUris = emptySet()) }
        }
    }

    fun refresh() {
        if (_uiState.value.permission == PhotoAccess.NONE) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = runCatching { photoRepository.queryImages() }
            result.onSuccess { photos ->
                val available = photos.map { it.uri }.toSet()
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        photos = photos,
                        // Drop selections that no longer exist (e.g. partial-access change).
                        selectedUris = state.selectedUris.intersect(available),
                    )
                }
            }.onFailure { t ->
                _uiState.update { it.copy(loading = false, error = t.message ?: "Failed to load photos") }
            }
        }
    }

    fun toggleSelection(uri: String) {
        _uiState.update { state ->
            val next = state.selectedUris.toMutableSet()
            if (!next.add(uri)) next.remove(uri)
            state.copy(selectedUris = next)
        }
    }

    /** Replace the whole selection set. Used by the drag-to-select gesture in the grid. */
    fun setSelection(uris: Set<String>) {
        _uiState.update { it.copy(selectedUris = uris) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Start background classification for the current selection. Returns (almost) immediately
     * after enqueuing — classification runs off the UI thread — and emits the one-shot
     * navigation signal so the screen can jump to the Queue.
     */
    fun classifySelected() {
        val state = _uiState.value
        if (!state.canClassify) return
        // Preserve gallery (newest-first) order in the selection.
        val selected = state.photos.map { it.uri }.filter { it in state.selectedUris }
        viewModelScope.launch {
            _uiState.update { it.copy(starting = true, error = null) }
            val result = runCatching { sessionRepository.startClassification(selected) }
            result.onSuccess {
                _uiState.update { it.copy(starting = false, selectedUris = emptySet()) }
                _classificationStarted.value = true
            }.onFailure { t ->
                _uiState.update {
                    it.copy(starting = false, error = t.message ?: "Could not start classification")
                }
            }
        }
    }

    /** Reset the navigation signal once the screen has consumed it. */
    fun onClassificationStartedHandled() {
        _classificationStarted.value = false
    }
}
