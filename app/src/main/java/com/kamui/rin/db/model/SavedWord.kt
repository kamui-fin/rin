package com.kamui.rin.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_words",
)
data class SavedWord(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "kanji", typeAffinity = ColumnInfo.TEXT) var kanji: String,
)