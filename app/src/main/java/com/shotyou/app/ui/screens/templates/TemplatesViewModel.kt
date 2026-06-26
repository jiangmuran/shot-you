package com.shotyou.app.ui.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the template library screen: observes templates and persists edits. */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    val templates: StateFlow<List<Template>> = templateRepository.observeTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun save(template: Template) {
        viewModelScope.launch { templateRepository.upsert(template) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { templateRepository.delete(id) }
    }
}
