package com.kamui.rin.db.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kamui.rin.db.model.SavedWord
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM SavedWord")
    fun getAllSaved(): Flow<List<SavedWord>>

    @Query("SELECT EXISTS(SELECT * FROM SavedWord WHERE kanji = :kanji)")
    fun existsWord(kanji: String): Boolean

    @Query("DELETE FROM SavedWord")
    fun deleteAllWords()

    @Delete
    fun deleteWord(word: SavedWord)

    @Query("DELETE FROM SavedWord WHERE kanji = :kanji")
    fun deleteWordByKanji(kanji: String)

    @Insert
    fun insertWord(word: SavedWord)
}