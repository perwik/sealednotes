package com.sealednotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.NoteWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val notes: List<NoteWithDetails> = emptyList(),
    val selectedNoteIds: Set<Long> = emptySet(),
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SealedNotesApp).repository

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<TrashUiState> = combine(
        repository.getTrashedNotes(),
        _selectedNoteIds,
    ) { notes, selected ->
        val visibleIds = notes.map { it.note.id }.toSet()
        TrashUiState(
            notes = notes,
            selectedNoteIds = selected intersect visibleIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState(),
    )

    fun toggleSelection(noteId: Long) {
        _selectedNoteIds.update { current ->
            if (noteId in current) current - noteId else current + noteId
        }
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun restoreSelected() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.restoreFromTrash(it) }
            _selectedNoteIds.value = emptySet()
        }
    }

    fun deletePermanentlySelected() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.deleteNotePermanently(it) }
            _selectedNoteIds.value = emptySet()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val ids = uiState.value.notes.map { it.note.id }
            ids.forEach { repository.deleteNotePermanently(it) }
            _selectedNoteIds.value = emptySet()
        }
    }
}
