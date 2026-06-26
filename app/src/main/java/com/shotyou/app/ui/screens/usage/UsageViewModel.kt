package com.shotyou.app.ui.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A usage record paired with its computed cost (from the configured prices). */
data class UsageEntry(
    val record: UsageRecord,
    val cost: Double,
)

/** Aggregated usage figures shown on the dashboard. */
data class UsageUiState(
    val totalCalls: Int = 0,
    val successCalls: Int = 0,
    val failedCalls: Int = 0,
    val totalPromptTokens: Int = 0,
    val totalCompletionTokens: Int = 0,
    val totalImages: Int = 0,
    val totalCost: Double = 0.0,
    val currencySymbol: String = "$",
    val pricesConfigured: Boolean = false,
    val byOperation: List<Pair<AiOperation, Int>> = emptyList(),
    val byProvider: List<Pair<String, Int>> = emptyList(),
    val recent: List<UsageEntry> = emptyList(),
) {
    val totalTokens: Int get() = totalPromptTokens + totalCompletionTokens
}

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: UsageRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UsageUiState> =
        combine(repository.observeRecords(), settingsRepository.settings) { records, settings ->
            aggregate(records, settings)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageUiState())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }

    private fun aggregate(records: List<UsageRecord>, settings: AiSettings): UsageUiState {
        val pricesConfigured = settings.pricePerImage > 0.0 ||
            settings.pricePer1kInputTokens > 0.0 ||
            settings.pricePer1kOutputTokens > 0.0

        if (records.isEmpty()) {
            return UsageUiState(
                currencySymbol = settings.currencySymbol,
                pricesConfigured = pricesConfigured,
            )
        }

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
            totalCost = records.sumOf { costOf(it, settings) },
            currencySymbol = settings.currencySymbol,
            pricesConfigured = pricesConfigured,
            byOperation = byOperation,
            byProvider = byProvider,
            recent = records.take(20).map { UsageEntry(it, costOf(it, settings)) },
        )
    }

    // Token prices are configured per 1,000,000 tokens (the fields are named *1k* for
    // historical reasons but are interpreted per-million).
    private fun costOf(r: UsageRecord, s: AiSettings): Double =
        r.imageCount * s.pricePerImage +
            r.promptTokens / 1_000_000.0 * s.pricePer1kInputTokens +
            r.completionTokens / 1_000_000.0 * s.pricePer1kOutputTokens
}
