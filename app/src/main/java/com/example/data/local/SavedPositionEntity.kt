package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_positions")
data class SavedPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fen: String,
    val label: String,
    val bestMoveSan: String,
    val evalText: String,
    val sourceLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)
