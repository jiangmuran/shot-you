package com.shotyou.app.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.repository.GenerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: GenerationRepository,
) : ViewModel() {

    val jobs: StateFlow<List<GenerationJob>> = repository.observeJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retry(id: String) {
        viewModelScope.launch { runCatching { repository.retry(id) } }
    }

    fun cancel(id: String) {
        viewModelScope.launch { runCatching { repository.cancel(id) } }
    }

    fun clearFinished() {
        viewModelScope.launch { runCatching { repository.clearFinished() } }
    }
}
