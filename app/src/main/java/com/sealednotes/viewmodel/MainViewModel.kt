package com.sealednotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.Label
import com.sealednotes.data.entity.NoteWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NoteFilter {
    ALL, REMINDERS, ARCHIVE
}

data class MainUiState(
    val pinnedNotes: List<NoteWithDetails> = emptyList(),
    val unpinnedNotes: List<NoteWithDetails> = emptyList(),
    val labels: List<Label> = emptyList(),
    val currentFilter: NoteFilter = NoteFilter.ALL,
    val currentLabelId: Long? = null,
    val isGridView: Boolean = true,
    val selectedNoteIds: Set<Long> = emptySet(),
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SealedNotesApp).repository

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    private val _currentLabelId = MutableStateFlow<Long?>(null)
    private val _isGridView = MutableStateFlow(true)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())

    private val baseState: Flow<MainUiState> = combine(
        _currentFilter,
        _currentLabelId,
        _isGridView,
        repository.getAllLabels()
    ) { filter, labelId, gridView, labels ->
        Triple(Triple(filter, labelId, gridView), labels, Unit)
    }.flatMapLatest { (config, labels, _) ->
        val (filter, labelId, gridView) = config

        val notesFlow = when {
            labelId != null -> repository.getNotesByLabel(labelId)
            filter == NoteFilter.REMINDERS -> repository.getNotesWithReminders()
            filter == NoteFilter.ARCHIVE -> repository.getArchivedNotes()
            else -> repository.getActiveNotes()
        }

        notesFlow.map { notes ->
            val pinned = if (filter == NoteFilter.ARCHIVE) emptyList()
            else notes.filter { it.note.isPinned }

            val unpinned = if (filter == NoteFilter.ARCHIVE) notes
            else notes.filter { !it.note.isPinned }

            MainUiState(
                pinnedNotes = pinned,
                unpinnedNotes = unpinned,
                labels = labels,
                currentFilter = filter,
                currentLabelId = labelId,
                isGridView = gridView,
            )
        }
    }

    val uiState: StateFlow<MainUiState> = baseState
        .combine(_selectedNoteIds) { state, selected ->
            // Drop any selections that no longer correspond to visible notes.
            val visibleIds = (state.pinnedNotes + state.unpinnedNotes).map { it.note.id }.toSet()
            val effective = selected intersect visibleIds
            state.copy(selectedNoteIds = effective)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(),
        )

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
        _currentLabelId.value = null
        _selectedNoteIds.value = emptySet()
    }

    fun setLabelFilter(labelId: Long) {
        _currentLabelId.value = labelId
        _currentFilter.value = NoteFilter.ALL
        _selectedNoteIds.value = emptySet()
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    // ── Single-note ops (kept for callers outside selection mode) ──

    fun togglePinned(noteId: Long, currentlyPinned: Boolean) {
        viewModelScope.launch { repository.setPinned(noteId, !currentlyPinned) }
    }

    fun archiveNote(noteId: Long) {
        viewModelScope.launch { repository.setArchived(noteId, true) }
    }

    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch { repository.setArchived(noteId, false) }
    }

    fun trashNote(noteId: Long) {
        viewModelScope.launch { repository.moveToTrash(noteId) }
    }

    // ── Selection mode ──

    fun toggleSelection(noteId: Long) {
        _selectedNoteIds.update { current ->
            if (noteId in current) current - noteId else current + noteId
        }
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun trashSelected() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.moveToTrash(it) }
            _selectedNoteIds.value = emptySet()
        }
    }

    /**
     * If any selected note is unpinned, pin them all; otherwise unpin them all.
     * Mirrors Google Keep's behavior.
     */
    fun togglePinSelected() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val state = uiState.value
        val notes = (state.pinnedNotes + state.unpinnedNotes).filter { it.note.id in ids }
        val anyUnpinned = notes.any { !it.note.isPinned }
        viewModelScope.launch {
            notes.forEach { repository.setPinned(it.note.id, anyUnpinned) }
            _selectedNoteIds.value = emptySet()
        }
    }

    fun setArchivedForSelected(archive: Boolean) {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.setArchived(it, archive) }
            _selectedNoteIds.value = emptySet()
        }
    }

    /**
     * If every selected note has the label, removes it from all of them.
     * Otherwise, adds it to all of them. Selection is preserved.
     */
    fun toggleLabelForSelected(labelId: Long) {
        val state = uiState.value
        val ids = state.selectedNoteIds
        if (ids.isEmpty()) return
        val all = state.pinnedNotes + state.unpinnedNotes
        val selected = all.filter { it.note.id in ids }
        val allHave = selected.isNotEmpty() && selected.all { nwd -> nwd.labels.any { it.id == labelId } }
        viewModelScope.launch {
            if (allHave) {
                selected.forEach { repository.removeLabelFromNote(it.note.id, labelId) }
            } else {
                selected.forEach { repository.addLabelToNote(it.note.id, labelId) }
            }
        }
    }
}
