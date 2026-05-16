package com.sealednotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.NoteWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SealedNotesApp).repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<NoteWithDetails>> = _query
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.isEmpty()) flowOf(emptyList()) else repository.searchNotes(trimmed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }
}
