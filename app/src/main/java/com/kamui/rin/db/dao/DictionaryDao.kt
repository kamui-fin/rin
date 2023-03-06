package com.kamui.rin.db.dao

import androidx.room.Dao
import androidx.room.Insert
import com.kamui.rin.db.model.Dictionary

@Dao
interface DictionaryDao {
    @Insert
    fun insertDictionary(dictionary: Dictionary): Long
}