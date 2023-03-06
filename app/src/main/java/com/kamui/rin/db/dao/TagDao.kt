package com.kamui.rin.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kamui.rin.db.model.Tag

@Dao
interface TagDao {
    @Insert
    fun insertTags(tags: List<Tag>): List<Long>

    @Query("SELECT * FROM Tag WHERE tagId IN (:ids)")
    fun getTagsFromIds(ids: List<Long>): List<Tag>

    @Transaction
    fun insertTagsAndRetrieve(tags: List<Tag>): List<Tag> {
        val insertedIds = insertTags(tags)
        return getTagsFromIds(insertedIds)
    }
}