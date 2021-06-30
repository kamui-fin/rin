package com.kamui.rin.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictDao {
    @Query("SELECT * FROM dictionary WHERE kanji = :query AND dictname NOT IN (:disabled)")
    fun searchEntryByKanji(
        query: String,
        disabled: List<String?>
    ): List<DictEntry>

    @Query("SELECT * FROM dictionary WHERE reading = :query AND dictname NOT IN (:disabled)")
    fun searchEntryReading(
        query: String,
        disabled: List<String>
    ): List<DictEntry>
}