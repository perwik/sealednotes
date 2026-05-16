package com.sealednotes.data.dao

import androidx.room.*
import com.sealednotes.data.entity.Label
import com.sealednotes.data.entity.NoteLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM labels ORDER BY sortOrder ASC, name ASC")
    fun getAllLabels(): Flow<List<Label>>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: Long): Label?

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN note_label_cross_ref nlc ON l.id = nlc.labelId
        WHERE nlc.noteId = :noteId
        ORDER BY l.name ASC
    """)
    fun getLabelsForNote(noteId: Long): Flow<List<Label>>

    @Insert
    suspend fun insert(label: Label): Long

    @Update
    suspend fun update(label: Label)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteById(labelId: Long)

    // ── Note-Label relationships ──

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLabelToNote(crossRef: NoteLabelCrossRef)

    @Delete
    suspend fun removeLabelFromNote(crossRef: NoteLabelCrossRef)

    @Query("DELETE FROM note_label_cross_ref WHERE noteId = :noteId")
    suspend fun removeAllLabelsFromNote(noteId: Long)
}
