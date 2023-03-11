package com.kamui.rin.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Frequency(
    @PrimaryKey(autoGenerate = true) var freqId: Long = 0,
    val kanji: String,
    val frequency: Long,
)
