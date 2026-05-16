package com.sealednotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.ChecklistItem
import com.sealednotes.data.entity.Label
import com.sealednotes.data.entity.Note
import com.sealednotes.data.entity.ReminderRecurrence
import com.sealednotes.data.repository.NoteRepository
import com.sealednotes.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditableChecklistItem(
    val id: Long = 0L,
    val content: String = "",
    val isChecked: Boolean = false,
)

data class NoteEditorUiState(
    val isLoading: Boolean = true,
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val isChecklist: Boolean = false,
    val checklistItems: List<EditableChecklistItem> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val selectedLabelIds: Set<Long> = emptySet(),
    val reminderTime: Long? = null,
    val reminderRecurrence: ReminderRecurrence = ReminderRecurrence.NONE,
)

class NoteEditorViewModel(
    application: Application,
    initialNoteId: Long?,
    isNewNote: Boolean,
    isChecklistByDefault: Boolean,
    preselectedLabelId: Long? = null,
) : AndroidViewModel(application) {

    private val repository: NoteRepository = (application as SealedNotesApp).repository

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            isLoading = !isNewNote && initialNoteId != null,
            noteId = if (isNewNote) null else initialNoteId,
            isChecklist = isChecklistByDefault,
            selectedLabelIds = if (isNewNote && preselectedLabelId != null) setOf(preselectedLabelId) else emptySet(),
        )
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    val allLabels: StateFlow<List<Label>> = repository.getAllLabels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        if (!isNewNote && initialNoteId != null) {
            loadNote(initialNoteId)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadNote(noteId: Long) {
        viewModelScope.launch {
            val details = repository.getNoteByIdOnce(noteId)
            if (details == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    noteId = noteId,
                    title = details.note.title,
                    content = details.note.content,
                    isChecklist = details.note.isChecklist,
                    checklistItems = details.checklistItems
                        .sortedBy { ci -> ci.sortOrder }
                        .map { ci -> EditableChecklistItem(ci.id, ci.content, ci.isChecked) },
                    isPinned = details.note.isPinned,
                    isArchived = details.note.isArchived,
                    selectedLabelIds = details.labels.map { l -> l.id }.toSet(),
                    reminderTime = details.note.reminderTime,
                    reminderRecurrence = details.note.reminderRecurrence ?: ReminderRecurrence.NONE,
                )
            }
        }
    }

    fun toggleLabel(labelId: Long) {
        _uiState.update { state ->
            val current = state.selectedLabelIds
            val next = if (labelId in current) current - labelId else current + labelId
            state.copy(selectedLabelIds = next)
        }
    }

    fun setReminder(timeMillis: Long, recurrence: ReminderRecurrence) {
        _uiState.update { it.copy(reminderTime = timeMillis, reminderRecurrence = recurrence) }
    }

    fun clearReminder() {
        _uiState.update { it.copy(reminderTime = null, reminderRecurrence = ReminderRecurrence.NONE) }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }

    fun updateContent(value: String) = _uiState.update { it.copy(content = value) }

    fun togglePinned() = _uiState.update { it.copy(isPinned = !it.isPinned) }

    fun toggleArchived() = _uiState.update { it.copy(isArchived = !it.isArchived) }

    fun addChecklistItem() {
        _uiState.update { it.copy(checklistItems = it.checklistItems + EditableChecklistItem()) }
    }

    fun updateChecklistItem(index: Int, content: String) {
        _uiState.update { state ->
            state.copy(checklistItems = state.checklistItems.mapIndexed { i, item ->
                if (i == index) item.copy(content = content) else item
            })
        }
    }

    fun setChecklistItemChecked(index: Int, checked: Boolean) {
        _uiState.update { state ->
            state.copy(checklistItems = state.checklistItems.mapIndexed { i, item ->
                if (i == index) item.copy(isChecked = checked) else item
            })
        }
    }

    fun deleteChecklistItem(index: Int) {
        _uiState.update { state ->
            state.copy(checklistItems = state.checklistItems.filterIndexed { i, _ -> i != index })
        }
    }

    /**
     * Persists the current state. New notes that are completely empty are discarded.
     * Returns the saved note id, or null if discarded.
     */
    suspend fun save(): Long? {
        val state = _uiState.value
        if (state.isLoading) return state.noteId

        val cleanedItems = state.checklistItems
            .map { it.copy(content = it.content.trim()) }
            .filter { it.content.isNotEmpty() }

        val isEffectivelyEmpty = state.title.isBlank() &&
            (if (state.isChecklist) cleanedItems.isEmpty() else state.content.isBlank())

        if (state.noteId == null && isEffectivelyEmpty) return null

        val now = System.currentTimeMillis()
        val savedId: Long
        if (state.noteId == null) {
            val newNote = Note(
                title = state.title.trim(),
                content = if (state.isChecklist) "" else state.content,
                isChecklist = state.isChecklist,
                isPinned = state.isPinned,
                isArchived = state.isArchived,
                reminderTime = state.reminderTime,
                reminderRecurrence = state.reminderRecurrence.takeIf { it != ReminderRecurrence.NONE },
                createdAt = now,
                updatedAt = now,
            )
            savedId = repository.createNote(newNote)
            if (state.isChecklist && cleanedItems.isNotEmpty()) {
                repository.addChecklistItems(cleanedItems.mapIndexed { i, item ->
                    ChecklistItem(
                        noteId = savedId,
                        content = item.content,
                        isChecked = item.isChecked,
                        sortOrder = i,
                    )
                })
            }
        } else {
            savedId = state.noteId
            val existing = repository.getNoteByIdOnce(savedId)?.note
            val base = existing ?: Note(id = savedId, createdAt = now)
            val updated = base.copy(
                title = state.title.trim(),
                content = if (state.isChecklist) "" else state.content,
                isChecklist = state.isChecklist,
                isPinned = state.isPinned,
                isArchived = state.isArchived,
                reminderTime = state.reminderTime,
                reminderRecurrence = state.reminderRecurrence.takeIf { it != ReminderRecurrence.NONE },
                updatedAt = now,
            )
            repository.updateNote(updated)

            if (state.isChecklist) {
                // Simple sync: replace all items. Cheap for typical note sizes.
                repository.deleteAllChecklistItems(savedId)
                if (cleanedItems.isNotEmpty()) {
                    repository.addChecklistItems(cleanedItems.mapIndexed { i, item ->
                        ChecklistItem(
                            noteId = savedId,
                            content = item.content,
                            isChecked = item.isChecked,
                            sortOrder = i,
                        )
                    })
                }
            } else {
                repository.deleteAllChecklistItems(savedId)
            }
        }

        // Sync labels: remove all and re-add selected. Cheap for typical note sizes.
        repository.removeAllLabelsFromNote(savedId)
        state.selectedLabelIds.forEach { repository.addLabelToNote(savedId, it) }

        // Sync the AlarmManager alarm with the persisted reminder.
        val app = getApplication<Application>()
        val reminder = state.reminderTime
        if (reminder != null) {
            ReminderScheduler.scheduleReminder(
                app, savedId, state.title.trim(), state.content.take(200), reminder
            )
        } else {
            ReminderScheduler.cancelReminder(app, savedId)
        }

        _uiState.update { it.copy(noteId = savedId) }
        return savedId
    }

    suspend fun moveToTrash() {
        val id = _uiState.value.noteId ?: return
        repository.moveToTrash(id)
    }

    class Factory(
        private val application: Application,
        private val noteId: Long?,
        private val isNewNote: Boolean,
        private val isChecklist: Boolean,
        private val preselectedLabelId: Long? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NoteEditorViewModel::class.java))
            return NoteEditorViewModel(
                application, noteId, isNewNote, isChecklist, preselectedLabelId
            ) as T
        }
    }
}
