package com.kamui.rin.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DictEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictDao(): DictDao

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