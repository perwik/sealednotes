package com.sealednotes.data.dao

import androidx.room.*
import com.sealednotes.data.entity.Note
import com.sealednotes.data.entity.NoteWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // ── Active notes (not archived, not trashed) ──

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 AND isTrashed = 0 
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getActiveNotes(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 AND isTrashed = 0 AND isPinned = 1
        ORDER BY updatedAt DESC
    """)
    fun getPinnedNotes(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 AND isTrashed = 0 AND isPinned = 0
        ORDER BY updatedAt DESC
    """)
    fun getUnpinnedNotes(): Flow<List<NoteWithDetails>>

    // ── Archive ──

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 1 AND isTrashed = 0 
        ORDER BY updatedAt DESC
    """)
    fun getArchivedNotes(): Flow<List<NoteWithDetails>>

    // ── Trash ──

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE isTrashed = 1 
        ORDER BY trashedAt DESC
    """)
    fun getTrashedNotes(): Flow<List<NoteWithDetails>>

    // ── Reminders ──

    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE reminderTime IS NOT NULL AND isTrashed = 0
        ORDER BY reminderTime ASC
    """)
    fun getNotesWithReminders(): Flow<List<NoteWithDetails>>

    @Query("""
        SELECT * FROM notes 
        WHERE reminderTime IS NOT NULL AND reminderTime <= :time AND isTrashed = 0
    """)
    suspend fun getDueReminders(time: Long): List<Note>

    // ── By label ──

    @Transaction
    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_label_cross_ref nlc ON n.id = nlc.noteId
        WHERE nlc.labelId = :labelId AND n.isArchived = 0 AND n.isTrashed = 0
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    fun getNotesByLabel(labelId: Long): Flow<List<NoteWithDetails>>

    // ── Search ──

    @Transaction
    @Query("""
        SELECT DISTINCT n.* FROM notes n
        LEFT JOIN checklist_items ci ON n.id = ci.noteId
        WHERE n.isTrashed = 0 
        AND (n.title LIKE '%' || :query || '%' 
             OR n.content LIKE '%' || :query || '%'
             OR ci.content LIKE '%' || :query || '%')
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteWithDetails>>

    // ── Single note ──

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteWithDetails?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdOnce(noteId: Long): NoteWithDetails?

    // ── CRUD ──

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :now WHERE id = :noteId")
    suspend fun setPinned(noteId: Long, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :now WHERE id = :noteId")
    suspend fun setArchived(noteId: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE notes SET isTrashed = 1, trashedAt = :now, updatedAt = :now 
        WHERE id = :noteId
    """)
    suspend fun moveToTrash(noteId: Long, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE notes SET isTrashed = 0, trashedAt = NULL, updatedAt = :now 
        WHERE id = :noteId
    """)
    suspend fun restoreFromTrash(noteId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1 AND trashedAt < :cutoff")
    suspend fun deleteOldTrashedNotes(cutoff: Long)

    // ── Reminder updates ──

    @Query("""
        UPDATE notes SET reminderTime = :time, reminderRecurrence = :recurrence, updatedAt = :now 
        WHERE id = :noteId
    """)
    suspend fun setReminder(
        noteId: Long,
        time: Long?,
        recurrence: String?,
        now: Long = System.currentTimeMillis()
    )
}
