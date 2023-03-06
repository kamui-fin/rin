package com.kamui.rin.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kamui.rin.db.dao.DictEntryDao
import com.kamui.rin.db.dao.DictionaryDao
import com.kamui.rin.db.dao.SavedWordDao
import com.kamui.rin.db.dao.TagDao
import com.kamui.rin.db.model.*

@Database(
    entities = [DictEntry::class, DictEntryTagCrossRef::class, SavedWord::class, Tag::class, Dictionary::class], version = 2, exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictEntryDao(): DictEntryDao
    abstract fun savedDao(): SavedWordDao
    abstract fun tagDao(): TagDao
    abstract fun dictionaryDao(): DictionaryDao


    companion object {
        @Volatile
        private lateinit var INSTANCE: AppDatabase

        fun buildDatabase(context: Context): AppDatabase {
            if (!this::INSTANCE.isInitialized) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dict.db"
                )
                    .build()
            }
            return INSTANCE
        }
    }
}