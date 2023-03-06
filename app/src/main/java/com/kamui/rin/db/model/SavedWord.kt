package com.kamui.rin.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
)
data class SavedWord(
    @PrimaryKey(autoGenerate = true) var savedWordId: Long = 0,
    var kanji: String,
)