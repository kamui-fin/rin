package com.kamui.rin.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary",
    indices = [Index(
        value = ["kanji", "reading"],
        name = "idx_word_reading",
        unique = false
    )]
)
data class DictEntry(
    @PrimaryKey var id: Int,
    @ColumnInfo(name = "kanji", typeAffinity = 2) var kanji: String,
    @ColumnInfo(name = "reading", typeAffinity = 2) var reading: String,
    @ColumnInfo(name = "tags", typeAffinity = 2) var tags: String,
    @ColumnInfo(name = "meaning", typeAffinity = 2) private var meaning: String,
    @ColumnInfo(name = "dictname", typeAffinity = 2) var dictionaryName: String,
    @ColumnInfo(name = "orderdict", typeAffinity = 3) val dictOrder: Int,
    @ColumnInfo(name = "freq", typeAffinity = 3) var freq: Int?,
    @ColumnInfo(name = "pitchaccent", typeAffinity = 2) var pitchAccent: String?,
) : Comparable<DictEntry> {

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