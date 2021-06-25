package com.kamui.rin.database

import androidx.room.Dao
import androidx.room.Query
import com.kamui.rin.database.AppDatabase
import com.kamui.rin.database.DBHelper
import com.kamui.rin.database.DictEntry

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