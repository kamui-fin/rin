package com.kamui.rin.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictDao {
    @Query("SELECT * FROM dictionary WHERE id = :id")
    fun searchEntryById(
        id: Int
    ): DictEntry

    @Query("SELECT * FROM dictionary WHERE kanji = :query AND dictname NOT IN (:disabled)")
    fun searchEntryByKanji(
        query: String,
        disabled: List<String?>
    ): List<DictEntry>

    @Query("SELECT * FROM dictionary WHERE reading = :query AND dictname NOT IN (:disabled)")
    fun searchEntryByReading(
        query: String,
        disabled: List<String>
    ): List<DictEntry>
}