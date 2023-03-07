package com.kamui.rin.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kamui.rin.db.model.Dictionary

@Dao
interface DictionaryDao {
    @Insert
    fun insertDictionary(dictionary: Dictionary): Long

    @Delete
    fun deleteDictionary(dictionary: Dictionary)

    @Query("SELECT * FROM Dictionary")
    fun getAllDictionaries(): List<Dictionary>
}