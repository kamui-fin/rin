package com.kamui.rin.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kamui.rin.db.model.PitchAccent

@Dao
interface PitchAccentDao {
    @Query("DELETE FROM PitchAccent")
    fun clear()

    @Insert
    fun insertPitchAccents(freq: List<PitchAccent>)

    @Query("SELECT pitch FROM PitchAccent WHERE kanji = :kanji")
    fun getPitchForWord(kanji: String): String?
}
