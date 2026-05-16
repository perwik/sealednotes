package com.sealednotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val content: String = "",
    val isChecked: Boolean = false,
    val sortOrder: Int = 0,
    val indentLevel: Int = 0 // 0 = top level, 1 = indented sub-item
)
