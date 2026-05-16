package com.sealednotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.Label
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LabelsUiState(
    val labels: List<Label> = emptyList(),
    val newLabelDraft: String = "",
    val editingLabelId: Long? = null,
    val editingDraft: String = "",
)

class LabelsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SealedNotesApp).repository

    private val _newLabelDraft = MutableStateFlow("")
    private val _editingLabelId = MutableStateFlow<Long?>(null)
    private val _editingDraft = MutableStateFlow("")

    val uiState: StateFlow<LabelsUiState> = combine(
        repository.getAllLabels(),
        _newLabelDraft,
        _editingLabelId,
        _editingDraft,
    ) { labels, newDraft, editingId, editingDraft ->
        LabelsUiState(labels, newDraft, editingId, editingDraft)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LabelsUiState(),
    )

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun updateNewLabelDraft(value: String) {
        _newLabelDraft.value = value
    }

    fun submitNewLabel() {
        val trimmed = _newLabelDraft.value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = uiState.value.labels
            if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
                _errors.emit("Label \"$trimmed\" already exists")
                return@launch
            }
            repository.createLabel(Label(name = trimmed))
            _newLabelDraft.value = ""
        }
    }

    fun startEditing(label: Label) {
        _editingLabelId.value = label.id
        _editingDraft.value = label.name
    }

    fun updateEditingDraft(value: String) {
        _editingDraft.value = value
    }

    fun submitEdit() {
        val id = _editingLabelId.value ?: return
        val trimmed = _editingDraft.value.trim()
        if (trimmed.isEmpty()) {
            cancelEditing()
            return
        }
        viewModelScope.launch {
            val existing = uiState.value.labels
            val current = existing.firstOrNull { it.id == id }
            if (current == null) {
                cancelEditing()
                return@launch
            }
            if (current.name == trimmed) {
                cancelEditing()
                return@launch
            }
            if (existing.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) {
                _errors.emit("Label \"$trimmed\" already exists")
                return@launch
            }
            repository.updateLabel(current.copy(name = trimmed))
            _editingLabelId.value = null
            _editingDraft.value = ""
        }
    }

    fun cancelEditing() {
        _editingLabelId.value = null
        _editingDraft.value = ""
    }

    fun deleteLabel(id: Long) {
        if (_editingLabelId.value == id) cancelEditing()
        viewModelScope.launch {
            repository.deleteLabel(id)
        }
    }
}
