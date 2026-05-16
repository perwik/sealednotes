package com.sealednotes.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labels",
    indices = [Index(value = ["name"], unique = true)]
)
data class Label(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)
