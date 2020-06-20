package com.example.dictionaryapp.database;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;
@Dao
public interface DictDao {

    @Query("SELECT * FROM dictionary WHERE KANJI = :query AND DICTNAME NOT IN (:disabled)")
    public List<DictEntry> searchEntryByKanji(String query, List<String> disabled);

    @Query("SELECT * FROM dictionary WHERE READING = :query AND DICTNAME NOT IN (:disabled)")
    public List<DictEntry> searchEntryReading(String query, List<String> disabled);


}
