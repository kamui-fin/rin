package com.kamui.rin.database;

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "DICTIONARY",
    indices = [Index(
        value = ["KANJI", "READING"],
        name = "idx_word_reading",
        unique = false
    )]
)
data class DictEntry(
    @PrimaryKey var iD: Int,
    @ColumnInfo(name = "FREQ") var freq: Int,
    @ColumnInfo(name = "PITCHACCENT") var pitchAccent: String,
    @ColumnInfo(name = "KANJI", typeAffinity = 2) var kanji: String,
    @ColumnInfo(name = "READING", typeAffinity = 2) var reading: String,
    @ColumnInfo(name = "TAGS", typeAffinity = 2) var tags: String,
    @ColumnInfo(name = "MEANING", typeAffinity = 2) private var meaning: String,
    @ColumnInfo(name = "DICTNAME", typeAffinity = 2) var dictionaryName: String,
    @ColumnInfo(name = "ORDERDICT", typeAffinity = 3) val dictOrder: Int
) : Comparable<DictEntry> {

    fun setMeaning(meaning: String) {
        this.meaning = meaning
    }

    fun getMeaning(): String {
        return meaning.replace("\n", "\n\n").trim { it <= ' ' }
    }

    val shortenedDictName: String
        get() {
            when (dictionaryName) {
                "JMdict (English)" -> return "JMdict"
                "研究社　新和英大辞典　第５版" -> return "研究社"
                "新明解国語辞典 第五版" -> return "新明解"
                "三省堂　スーパー大辞林" -> return "大辞林"
                "明鏡国語辞典" -> return "明鏡"
            }
            return dictionaryName
        }

    override fun toString(): String {
        return kanji + "\t" + reading + "\t" + dictionaryName
    }

    override operator fun compareTo(other: DictEntry): Int {
        return dictOrder.compareTo(other.dictOrder)
    }
}