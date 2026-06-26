package com.shotyou.app.ui.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Aggregated usage figures shown on the dashboard. */
data class UsageUiState(
    val totalCalls: Int = 0,
    val successCalls: Int = 0,
    val failedCalls: Int = 0,
    val totalPromptTokens: Int = 0,
    val totalCompletionTokens: Int = 0,
    val totalImages: Int = 0,
    val totalCostUsd: Double = 0.0,
    val byOperation: List<Pair<AiOperation, Int>> = emptyList(),
    val byProvider: List<Pair<String, Int>> = emptyList(),
    val recent: List<UsageRecord> = emptyList(),
) {
    val totalTokens: Int get() = totalPromptTokens + totalCompletionTokens
}

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: UsageRepository,
) : ViewModel() {

    val state: StateFlow<UsageUiState> = repository.observeRecords()
        .map { records -> aggregate(records) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageUiState())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }

    private fun aggregate(records: List<UsageRecord>): UsageUiState {
        if (records.isEmpty()) return UsageUiState()

        val byOperation = AiOperation.entries
            .map { op -> op to records.count { it.operation == op } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val byProvider = records.groupingBy { it.provider }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        return UsageUiState(
            totalCalls = records.size,
            successCalls = records.count { it.success },
            failedCalls = records.count { !it.success },
            totalPromptTokens = records.sumOf { it.promptTokens },
            totalCompletionTokens = records.sumOf { it.completionTokens },
            totalImages = records.sumOf { it.imageCount },
            totalCostUsd = records.sumOf { it.estimatedCostUsd },
            byOperation = byOperation,
            byProvider = byProvider,
            recent = records.take(20),
        )
    }
}
