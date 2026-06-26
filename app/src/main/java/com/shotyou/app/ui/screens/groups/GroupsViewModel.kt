package com.shotyou.app.ui.screens.groups

import androidx.lifecycle.ViewModel
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {

    val groups: StateFlow<List<PhotoGroup>> = sessionStore.groups

    /** Marks [group] as the active group and lets the caller navigate forward. */
    fun openGroup(group: PhotoGroup) {
        sessionStore.setActiveGroup(group)
    }
}
