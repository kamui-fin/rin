package com.kamui.rin.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(
            value = ["dictionaryId"],
            name = "idx_dictionary_tag_ref",
            unique = false
        )
    ],
    foreignKeys = [ForeignKey(
        entity = Dictionary::class,
        parentColumns = arrayOf("dictId"),
        childColumns = arrayOf("dictionaryId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) var tagId: Long = 0,
    val dictionaryId: Long,
    val name: String,
    val notes: String,
)
