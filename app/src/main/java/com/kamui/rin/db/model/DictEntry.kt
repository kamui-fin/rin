package com.kamui.rin.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["dictId"],
            childColumns = ["dictionaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(
        value = ["kanji", "reading"],
        name = "idx_word_reading",
        unique = false
    ), Index(
        value = ["dictionaryId"],
        name = "idx_dictionary_entry_ref",
        unique = false
    )]
)
data class DictEntry(
    @PrimaryKey(autoGenerate = true) var entryId: Long = 0,
    var kanji: String,
    var meaning: String,
    var reading: String,
    var dictionaryId: Long,
)

@Entity(primaryKeys = ["entryId", "tagId"])
data class DictEntryTagCrossRef(
    val entryId: Long,
    @ColumnInfo(index = true)
    val tagId: Long,
)