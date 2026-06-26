package com.shotyou.app.ui.screens.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.repository.GenerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResultViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val jobId = MutableStateFlow<String?>(null)

    val job: StateFlow<GenerationJob?> = jobId
        .flatMapLatest { id ->
            if (id.isNullOrEmpty()) flowOf(null) else generationRepository.observeJob(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Called by the screen in a LaunchedEffect(jobId). Idempotent. */
    fun load(id: String) {
        if (jobId.value != id) jobId.value = id
    }

    fun retry() {
        val id = jobId.value ?: return
        viewModelScope.launch { generationRepository.retry(id) }
    }
}
