package com.opensplit.data.local

import com.opensplit.domain.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Session write-through cache for groups: a group created or edited on this device shows up
 * immediately, before Firestore's snapshot listener reports it.
 */
object InMemoryDataStore {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    fun addGroup(group: Group) {
        _groups.update { current -> (current + group).distinctBy { it.id } }
    }

    fun updateGroup(group: Group) {
        _groups.update { current ->
            if (current.any { it.id == group.id }) {
                current.map { if (it.id == group.id) group else it }
            } else {
                current + group
            }
        }
    }

    fun removeGroup(groupId: String) {
        _groups.update { current -> current.filterNot { it.id == groupId } }
    }
}
