package com.kamui.rin.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kamui.rin.db.model.DictEntry

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntries(entries: List<DictEntry>)
}