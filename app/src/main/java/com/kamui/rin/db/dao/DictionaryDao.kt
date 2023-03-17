package com.kamui.rin.db.dao

import androidx.room.*
import com.kamui.rin.db.model.Dictionary

@Dao
interface DictionaryDao {
    @Insert
    fun insertDictionary(dictionary: Dictionary): Long

    @Delete
    fun deleteDictionary(dictionary: Dictionary)

    @Update
    fun updateDictionary(dictionary: Dictionary)

    @Query("SELECT * FROM Dictionary")
    fun getAllDictionaries(): List<Dictionary>
}