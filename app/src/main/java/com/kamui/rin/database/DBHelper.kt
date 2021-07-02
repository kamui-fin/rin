package com.kamui.rin.database

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.kamui.rin.Tag
import com.kamui.rin.TagsHelper
import com.kamui.rin.deinflector.Deinflector
import java.util.*
import kotlin.collections.ArrayList

class DBHelper(
    mContext: Context,
    private val disabledDicts: List<String>,
    private val shouldDeconj: Boolean,
    private val bilingualFirst: Boolean,
    deinflectionText: String,
) {
    private val db: AppDatabase = AppDatabase.buildDatabase(mContext)
    private val dao: DictDao = db.dictDao()
    private var deinflector: Deinflector = Deinflector(deinflectionText)

    fun lookup(query: String): List<DictEntry> {
        val possibleVariations = normalizeWord(query).toMutableList()
        val entries: MutableList<DictEntry> = ArrayList()
        if (possibleVariations.isEmpty()) {
            possibleVariations.add(query)
        }

        for (variation in possibleVariations) {
            var results: List<DictEntry>
            if (isAllKana(variation)) {
                val convertedToHiragana: String = if (!allHiragana(variation)) {
                    katakanaToHiragana(variation)
                } else {
                    variation
                }
                results = dao.searchEntryReading(convertedToHiragana, disabledDicts)
                if (results.isEmpty()) {
                    results = dao.searchEntryByKanji(variation, disabledDicts)
                }
            } else {
                results = dao.searchEntryByKanji(variation, disabledDicts)
            }
            entries.addAll(results)
        }

        if (bilingualFirst) {
            Collections.sort(entries, Collections.reverseOrder<Any>())
        } else {
            entries.sort()
        }
        return entries
    }

    private fun normalizeWord(word: String): List<String> {
        return if (shouldDeconj) {
            deconjugateWord(word.trim { it <= ' ' })
        } else {
            mutableListOf(word)
        }
    }

    private fun deconjugateWord(word: String): List<String> {
        return deinflector.deinflect(word).map { d -> d.term }
    }
}

fun toHiragana(c: Char): Char {
    if (isFullWidthKatakana(c)) {
        return (c.toInt() - 0x60).toChar()
    } else if (isHalfWidthKatakana(c)) {
        return (c.toInt() - 0xcf25).toChar()
    }
    return c
}

fun isHalfWidthKatakana(c: Char): Boolean {
    return c in '\uff66'..'\uff9d'
}

fun isFullWidthKatakana(c: Char): Boolean {
    return c in '\u30a1'..'\u30fe'
}

fun isHiragana(c: Char): Boolean {
    return c in '\u3041'..'\u309e'
}

fun allHiragana(word: String): Boolean {
    for (x in word.toCharArray()) {
        if (!isHiragana(x)) {
            return false
        }
    }
    return true
}

fun isAllKana(word: String): Boolean {
    for (element in word) {
        return if (element in 'ぁ'..'ゞ' || element in 'ァ'..'ヾ') {
            continue
        } else {
            false
        }
    }
    return true
}

fun katakanaToHiragana(katakanaWord: String): String {
    return katakanaWord.map { c -> toHiragana(c) }.toString()
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getTagsFromSplitted(entry: DictEntry, mContext: Context): List<Tag> {
    val helper = TagsHelper(mContext)
    val splitted: List<String> = entry.tags.split("\\s+")
    return splitted.map { w -> helper.getTagFromName(w) }
}