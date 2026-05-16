package com.sealednotes.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_label_cross_ref",
    primaryKeys = ["noteId", "labelId"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Label::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["labelId"])
    ]
)
data class NoteLabelCrossRef(
    val noteId: Long,
    val labelId: Long
)
