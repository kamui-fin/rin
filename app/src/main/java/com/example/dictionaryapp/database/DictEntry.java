package com.example.dictionaryapp.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "DICTIONARY", indices = @Index(value = {"KANJI", "READING"}, name = "idx_word_reading", unique = false))
public class DictEntry implements Comparable<DictEntry> {
    @PrimaryKey()
    private int ID;
    @ColumnInfo(name = "KANJI", typeAffinity = 2)
    private String kanji;
    @ColumnInfo(name = "READING", typeAffinity = 2)
    private String reading;
    @ColumnInfo(name = "TAGS", typeAffinity = 2)
    private String tags;
    @ColumnInfo(name = "MEANING", typeAffinity = 2)
    private String meaning;
    @ColumnInfo(name = "DICTNAME", typeAffinity = 2)
    private String dictionaryName;
    @ColumnInfo(name = "ORDERDICT", typeAffinity = 3)
    private Integer dictOrder;


    public DictEntry(String kanji, String reading, String tags, String meaning, String dictionaryName, Integer dictOrder) {
        this.kanji = kanji;
        this.reading = reading;
        this.tags = tags;
        this.meaning = meaning;
        this.dictionaryName = dictionaryName;
        this.dictOrder = dictOrder;
    }

    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public void setKanji(String kanji) {
        this.kanji = kanji;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getReading() {
        return reading;
    }

    public String getDictionaryName() {
        return dictionaryName;
    }

    public int getID() {
        return ID;
    }

    public String getMeaning() {
        return meaning.replace("\n", "\n\n").trim();
    }

    public String getTags() {
        return tags;
    }

    public String getKanji() {
        return kanji;
    }

    public Integer getDictOrder() {
        return dictOrder;
    }

    public String getShortenedDictName() {
        switch (this.dictionaryName) {
            case "JMdict (English)":
                return "JMdict";
            case "研究社　新和英大辞典　第５版":
                return "研究社";
            case "新明解国語辞典 第五版":
                return "新明解";
            case "三省堂　スーパー大辞林":
                return "大辞林";
            case "明鏡国語辞典":
                return "明鏡";
        }
        return this.dictionaryName;
    }

    @Override
    public String toString() {
        return getKanji() + "\t" + getReading() + "\t" + getDictionaryName();
    }

    @Override
    public int compareTo(DictEntry u) {
        return getDictOrder().compareTo(u.getDictOrder());
    }
}
