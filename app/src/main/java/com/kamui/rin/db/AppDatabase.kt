package com.kamui.rin.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kamui.rin.db.dao.DictDao
import com.kamui.rin.db.dao.SavedDao
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.SavedWord

@Database(
    entities = [DictEntry::class, SavedWord::class], version = 2, exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictDao(): DictDao
    abstract fun savedDao(): SavedDao

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
                    .createFromAsset("dict.db")
                    .build()
            }
            return INSTANCE
        }
    }
}