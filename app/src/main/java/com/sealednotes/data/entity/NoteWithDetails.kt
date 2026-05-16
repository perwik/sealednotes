package com.sealednotes.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class NoteWithDetails(
    @Embedded val note: Note,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val checklistItems: List<ChecklistItem> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteLabelCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "labelId"
        )
    )
    val labels: List<Label> = emptyList()
)
