package com.sealednotes.data.repository

import com.sealednotes.data.dao.ChecklistItemDao
import com.sealednotes.data.dao.LabelDao
import com.sealednotes.data.dao.NoteDao
import com.sealednotes.data.entity.*
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val checklistItemDao: ChecklistItemDao,
    private val labelDao: LabelDao
) {

    // ── Notes ──

    fun getActiveNotes(): Flow<List<NoteWithDetails>> = noteDao.getActiveNotes()
    fun getPinnedNotes(): Flow<List<NoteWithDetails>> = noteDao.getPinnedNotes()
    fun getUnpinnedNotes(): Flow<List<NoteWithDetails>> = noteDao.getUnpinnedNotes()
    fun getArchivedNotes(): Flow<List<NoteWithDetails>> = noteDao.getArchivedNotes()
    fun getTrashedNotes(): Flow<List<NoteWithDetails>> = noteDao.getTrashedNotes()
    fun getNotesWithReminders(): Flow<List<NoteWithDetails>> = noteDao.getNotesWithReminders()
    fun getNotesByLabel(labelId: Long): Flow<List<NoteWithDetails>> = noteDao.getNotesByLabel(labelId)
    fun searchNotes(query: String): Flow<List<NoteWithDetails>> = noteDao.searchNotes(query)
    fun getNoteById(noteId: Long): Flow<NoteWithDetails?> = noteDao.getNoteById(noteId)

    suspend fun getNoteByIdOnce(noteId: Long): NoteWithDetails? = noteDao.getNoteByIdOnce(noteId)
    suspend fun getDueReminders(time: Long): List<Note> = noteDao.getDueReminders(time)

    suspend fun createNote(note: Note): Long = noteDao.insert(note)
    suspend fun updateNote(note: Note) = noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun setPinned(noteId: Long, pinned: Boolean) = noteDao.setPinned(noteId, pinned)
    suspend fun setArchived(noteId: Long, archived: Boolean) = noteDao.setArchived(noteId, archived)
    suspend fun moveToTrash(noteId: Long) = noteDao.moveToTrash(noteId)
    suspend fun restoreFromTrash(noteId: Long) = noteDao.restoreFromTrash(noteId)
    suspend fun deleteNotePermanently(noteId: Long) = noteDao.deleteById(noteId)
    suspend fun deleteOldTrashedNotes(cutoffMs: Long) = noteDao.deleteOldTrashedNotes(cutoffMs)

    suspend fun setReminder(noteId: Long, time: Long?, recurrence: ReminderRecurrence?) {
        noteDao.setReminder(noteId, time, recurrence?.name)
    }

    // ── Checklist items ──

    fun getChecklistItems(noteId: Long): Flow<List<ChecklistItem>> =
        checklistItemDao.getItemsForNote(noteId)

    suspend fun addChecklistItem(item: ChecklistItem): Long = checklistItemDao.insert(item)
    suspend fun addChecklistItems(items: List<ChecklistItem>) = checklistItemDao.insertAll(items)
    suspend fun updateChecklistItem(item: ChecklistItem) = checklistItemDao.update(item)
    suspend fun setItemChecked(itemId: Long, checked: Boolean) = checklistItemDao.setChecked(itemId, checked)
    suspend fun setItemSortOrder(itemId: Long, order: Int) = checklistItemDao.setSortOrder(itemId, order)
    suspend fun setItemIndentLevel(itemId: Long, level: Int) = checklistItemDao.setIndentLevel(itemId, level)
    suspend fun deleteChecklistItem(itemId: Long) = checklistItemDao.deleteById(itemId)
    suspend fun deleteAllChecklistItems(noteId: Long) = checklistItemDao.deleteAllForNote(noteId)
    suspend fun deleteCheckedItems(noteId: Long) = checklistItemDao.deleteCheckedForNote(noteId)
    suspend fun uncheckAllItems(noteId: Long) = checklistItemDao.uncheckAllForNote(noteId)

    // ── Labels ──

    fun getAllLabels(): Flow<List<Label>> = labelDao.getAllLabels()
    fun getLabelsForNote(noteId: Long): Flow<List<Label>> = labelDao.getLabelsForNote(noteId)

    suspend fun getLabelById(labelId: Long): Label? = labelDao.getLabelById(labelId)
    suspend fun createLabel(label: Label): Long = labelDao.insert(label)
    suspend fun updateLabel(label: Label) = labelDao.update(label)
    suspend fun deleteLabel(labelId: Long) = labelDao.deleteById(labelId)

    suspend fun addLabelToNote(noteId: Long, labelId: Long) {
        labelDao.addLabelToNote(NoteLabelCrossRef(noteId, labelId))
    }

    suspend fun removeLabelFromNote(noteId: Long, labelId: Long) {
        labelDao.removeLabelFromNote(NoteLabelCrossRef(noteId, labelId))
    }

    suspend fun removeAllLabelsFromNote(noteId: Long) {
        labelDao.removeAllLabelsFromNote(noteId)
    }
}
