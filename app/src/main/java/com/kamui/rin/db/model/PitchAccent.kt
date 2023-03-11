package com.kamui.rin.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PitchAccent(
    @PrimaryKey(autoGenerate = true) var pitchId: Long = 0,
    val kanji: String,
    val pitch: String,
)