package com.shotyou.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exposes the persisted [AiSettings] and applies edits. Every edit is a partial
 * transform persisted via [SettingsRepository.update], so the UI only ever describes
 * the field it changed.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: Flow<AiSettings> = settingsRepository.settings

    fun update(transform: (AiSettings) -> AiSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
