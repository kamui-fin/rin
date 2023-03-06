package com.kamui.rin.db.model

import androidx.room.*

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
    // FIXME:
    var pitchAccent: String?,
    var freq: Int?,
)

@Entity(primaryKeys = ["entryId", "tagId"])
data class DictEntryTagCrossRef(
    val entryId: Long,
    @ColumnInfo(index = true)
    val tagId: Long,
)