package com.shotyou.app.domain.session

import com.shotyou.app.domain.model.PhotoGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds transient cross-screen state for one "session" (a batch the user is working on):
 * the selected photo uris, the VLM grouping result, and the group currently being
 * generated. Avoids serializing large objects through navigation arguments.
 */
@Singleton
class SessionStore @Inject constructor() {

    private val _selectedUris = MutableStateFlow<List<String>>(emptyList())
    val selectedUris: StateFlow<List<String>> = _selectedUris.asStateFlow()

    private val _groups = MutableStateFlow<List<PhotoGroup>>(emptyList())
    val groups: StateFlow<List<PhotoGroup>> = _groups.asStateFlow()

    private val _activeGroup = MutableStateFlow<PhotoGroup?>(null)
    val activeGroup: StateFlow<PhotoGroup?> = _activeGroup.asStateFlow()

    fun setSelectedUris(uris: List<String>) { _selectedUris.value = uris }
    fun setGroups(groups: List<PhotoGroup>) { _groups.value = groups }
    fun setActiveGroup(group: PhotoGroup?) { _activeGroup.value = group }

    fun clear() {
        _selectedUris.value = emptyList()
        _groups.value = emptyList()
        _activeGroup.value = null
    }
}
