package com.sealednotes.data.dao

import androidx.room.*
import com.sealednotes.data.entity.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY isChecked ASC, sortOrder ASC")
    fun getItemsForNote(noteId: Long): Flow<List<ChecklistItem>>

    @Insert
    suspend fun insert(item: ChecklistItem): Long

    @Insert
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun update(item: ChecklistItem)

    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :itemId")
    suspend fun setChecked(itemId: Long, checked: Boolean)

    @Query("UPDATE checklist_items SET sortOrder = :order WHERE id = :itemId")
    suspend fun setSortOrder(itemId: Long, order: Int)

    @Query("UPDATE checklist_items SET indentLevel = :level WHERE id = :itemId")
    suspend fun setIndentLevel(itemId: Long, level: Int)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteAllForNote(noteId: Long)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId AND isChecked = 1")
    suspend fun deleteCheckedForNote(noteId: Long)

    @Query("UPDATE checklist_items SET isChecked = 0 WHERE noteId = :noteId")
    suspend fun uncheckAllForNote(noteId: Long)
}
