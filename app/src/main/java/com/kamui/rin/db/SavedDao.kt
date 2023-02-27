package com.kamui.rin.db


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDao {
    @Query("SELECT * FROM saved_words")
    fun getAllSaved(): Flow<List<SavedWord>>

    @Query("SELECT EXISTS(SELECT * FROM saved_words WHERE kanji = :kanji)")
    fun existsWord(kanji: String): Boolean

    @Delete
    fun deleteWord(word: SavedWord)

    @Query("DELETE FROM saved_words WHERE kanji = :kanji")
    fun deleteWordByKanji(kanji: String)

    @Insert
    fun insertWord(word: SavedWord)
}