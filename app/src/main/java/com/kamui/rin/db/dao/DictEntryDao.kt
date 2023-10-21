package com.kamui.rin.db.dao

import androidx.room.*
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.DictEntryTagCrossRef
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.db.model.Tag

@Dao
interface DictEntryDao {
    @Transaction
    @Query("SELECT * FROM DictEntry WHERE entryId = :id")
    fun searchEntryById(
        id: Long
    ): DictEntry

    @Transaction
    @Query(
        "SELECT * FROM DictEntry " +
                "JOIN Dictionary ON Dictionary.dictId = DictEntry.dictionaryId " +
                "WHERE kanji = :query AND Dictionary.dictId NOT IN (:disabled)"
    )
    fun searchEntryByKanji(
        query: String,
        disabled: List<Long?>
    ): Map<DictEntry, Dictionary>

    @Transaction
    @Query(
        "SELECT * FROM DictEntry " +
                "JOIN Dictionary ON Dictionary.dictId = DictEntry.dictionaryId " +
                "WHERE reading = :query AND Dictionary.dictId NOT IN (:disabled)"
    )
    fun searchEntryByReading(
        query: String,
        disabled: List<Long>
    ): Map<DictEntry, Dictionary>

    @Query(
        "SELECT Tag.* from DictEntryTagCrossRef " +
                "JOIN Tag ON Tag.tagId = DictEntryTagCrossRef.tagId " +
                "WHERE DictEntryTagCrossRef.entryId = :entryId"
    )
    fun getTagsForEntry(entryId: Long): List<Tag>

    @Insert
    fun insertEntry(entry: DictEntry): Long

    @Insert
    fun insertTagForEntry(entryTagCrossRef: DictEntryTagCrossRef)

    @Transaction
    fun insertEntriesWithTags(entryTags: List<Pair<DictEntry, List<Tag>>>) {
        for ((entry, tags) in entryTags) {
            val entryId = insertEntry(entry)
            for (tag in tags) {
                val tagId = tag.tagId
                insertTagForEntry(DictEntryTagCrossRef(entryId, tagId))
            }
        }
    }
}