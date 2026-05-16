package com.sealednotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val isChecklist: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val reminderTime: Long? = null,
    val reminderRecurrence: ReminderRecurrence? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReminderRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}
