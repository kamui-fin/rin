package com.kamui.rin.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kamui.rin.db.dao.*
import com.kamui.rin.db.model.*

@DeleteColumn(tableName = "DictEntry", columnName = "pitchAccent")
@DeleteColumn(tableName = "DictEntry", columnName = "freq")
class DeleteFreqPitchColumn : AutoMigrationSpec

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE PitchAccent(
                pitchId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                kanji TEXT NOT NULL,
                pitch TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    version = 5,
    entities = [PitchAccent::class, Frequency::class, DictEntry::class, DictEntryTagCrossRef::class, SavedWord::class, Tag::class, Dictionary::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 2,
            to = 3
        ),
        AutoMigration(
            from = 4,
            to = 5,
            DeleteFreqPitchColumn::class
        ),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictEntryDao(): DictEntryDao
    abstract fun savedDao(): SavedWordDao
    abstract fun tagDao(): TagDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun frequencyDao(): FrequencyDao
    abstract fun pitchAccentDao(): PitchAccentDao

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
                    .addMigrations(MIGRATION_3_4)
                    .build()
            }
            return INSTANCE
        }
    }
}
