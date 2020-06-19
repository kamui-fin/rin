package com.example.dictionaryapp.database;

import com.example.dictionaryapp.DictEntryAdapter;

import java.util.List;

public class DictEntry implements Comparable<DictEntry>{
    private String kanji;
    private String reading;
    private String tags;
    private String meaning;
    private String dictionaryName;
    private Integer dictOrder;

    public String getDictionaryName() {
        return dictionaryName;
    }

    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }



    public DictEntry(String kanji, String reading, String tags, String meaning, String dictionaryName, Integer dictOrder) {
        this.kanji = kanji;
        this.reading = reading;
        this.tags = tags;
        this.meaning = meaning;
        this.dictionaryName = dictionaryName;
        this.dictOrder = dictOrder;
    }

    public Integer getDictOrder() {
        return dictOrder;
    }

    public String getShortenedDictName(){
        switch (this.dictionaryName){
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

    public String getKanji() {
        return kanji;
    }

    public void setKanji(String kanji) {
        this.kanji = kanji;
    }


    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }


    public String getMeaning() {
        return meaning.replace("\n", "\n\n").trim();
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }



    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    @Override
    public String toString(){
        return  getKanji() + "\t" + getReading() + "\t" + getDictionaryName();
    }

    @Override
    public int compareTo(DictEntry u) {
        return getDictOrder().compareTo(u.getDictOrder());
    }
}
