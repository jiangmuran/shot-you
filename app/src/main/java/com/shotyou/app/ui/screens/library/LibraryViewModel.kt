package com.shotyou.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.repository.GroupingRepository
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.session.SessionStore
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
    val loading: Boolean = false,
    val grouping: Boolean = false,
    val error: String? = null,
) {
    val selectedCount: Int get() = selectedUris.size
    val canGroup: Boolean get() = selectedCount >= 2 && !grouping
    val isEmpty: Boolean get() = !loading && photos.isEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val groupingRepository: GroupingRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /** One-shot signal: emit true to trigger navigation after a successful grouping. */
    private val _grouped = MutableStateFlow(false)
    val grouped: StateFlow<Boolean> = _grouped.asStateFlow()

    /** Called by the screen whenever the permission state is (re)computed. */
    fun onPermissionResult(access: PhotoAccess) {
        val previous = _uiState.value.permission
        _uiState.update { it.copy(permission = access) }
        if (access != PhotoAccess.NONE) {
            // (Re)load when access is granted or the user changed the selected set.
            if (previous != access || _uiState.value.photos.isEmpty()) {
                refresh()
            }
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

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun groupSelected() {
        val state = _uiState.value
        if (!state.canGroup) return
        // Preserve gallery (newest-first) order in the selection.
        val selected = state.photos.map { it.uri }.filter { it in state.selectedUris }
        viewModelScope.launch {
            _uiState.update { it.copy(grouping = true, error = null) }
            sessionStore.setSelectedUris(selected)
            val result = runCatching { groupingRepository.groupPhotos(selected) }
            result.onSuccess { groups ->
                sessionStore.setGroups(groups)
                _uiState.update { it.copy(grouping = false) }
                _grouped.value = true
            }.onFailure { t ->
                val message = when (t) {
                    is AiException -> t.message ?: "AI grouping failed"
                    else -> t.message ?: "Something went wrong while grouping"
                }
                _uiState.update { it.copy(grouping = false, error = message) }
            }
        }
    }

    /** Reset the navigation signal once the screen has consumed it. */
    fun onGroupedHandled() {
        _grouped.value = false
    }
}
