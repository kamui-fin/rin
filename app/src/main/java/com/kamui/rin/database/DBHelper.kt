package com.kamui.rin.database

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.kamui.rin.TagsHelper
import com.kamui.rin.deinflector.Deinflector
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class DBHelper(
    mContext: Context,
    private val disabledDicts: List<String>,
    private val shouldDeconj: Boolean,
    private val bilingualFirst: Boolean,
    private val inflectorJSON: JSONObject
) {
    private val db: AppDatabase = AppDatabase.buildDatabase(mContext)
    private val dao: DictDao = db.dictDao()
    var deinflector: Deinflector? = null

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Throws(JSONException::class)
    fun lookup(query: String): List<DictEntry> {
        val possible = normalizeWord(query)
        val entries: MutableList<DictEntry> = ArrayList()
        if (possible.isEmpty()) {
            possible.add(query)
        }
        for (p in possible) {
            var res: List<DictEntry>
            if (isAllKana(p)) {
                val convertedToHiragana: String = if (!allHiragana(p)) {
                    katakanaToHiragana(p)
                } else {
                    p
                }
                res = dao.searchEntryReading(convertedToHiragana, disabledDicts)
                if (res.isEmpty()) {
                    res = dao.searchEntryByKanji(p, disabledDicts)
                }
            } else {
                res = dao.searchEntryByKanji(p, disabledDicts)
            }
            entries.addAll(res)
        }
        if (bilingualFirst) {
            Collections.sort(entries, Collections.reverseOrder<Any>())
        } else {
            entries.sort()
        }
        return entries
    }

    // helper japanese methods
    private fun isAllKana(word: String): Boolean {
        for (element in word) {
            return if (element in 'ぁ'..'ゞ' || element in 'ァ'..'ヾ') {
                continue
            } else {
                false
            }
        }
        return true
    }

    @Throws(JSONException::class)
    fun normalizeWord(word: String): MutableList<String> {
        return if (shouldDeconj) {
            deconjugateWord(word.trim { it <= ' ' })
        } else {
            val res: MutableList<String> =
                ArrayList()
            res.add(word)
            res
        }
    }

    @Throws(JSONException::class)
    fun deconjugateWord(word: String?): MutableList<String> {
        val results: MutableList<String> =
            ArrayList()
        deinflector = Deinflector(inflectorJSON)
        val res: JSONArray = deinflector!!.deinflect(word)
        for (x in 0 until res.length()) {
            results.add(res.getJSONObject(x).getString("term"))
        }
        return results
    }

    private fun katakanaToHiragana(katakanaWord: String): String {
        val out = StringBuilder()
        for (element in katakanaWord) {
            out.append(toHiragana(element))
        }
        return out.toString()
    }

    companion object {
        fun toHiragana(c: Char): Char {
            if (isFullWidthKatakana(c)) {
                return (c.toInt() - 0x60).toChar()
            } else if (isHalfWidthKatakana(c)) {
                return (c.toInt() - 0xcf25).toChar()
            }
            return c
        }

        private fun isHalfWidthKatakana(c: Char): Boolean {
            return c in '\uff66'..'\uff9d'
        }

        private fun isFullWidthKatakana(c: Char): Boolean {
            return c in '\u30a1'..'\u30fe'
        }

        private fun isHiragana(c: Char): Boolean {
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

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        @Throws(IOException::class, JSONException::class)
        fun getSplittedTags(entry: DictEntry, mContext: Context): JSONArray {
            val helper = TagsHelper(mContext)
            val splitted: List<String> = entry.tags.split("\\s+")
            val info = JSONArray()
            for (tag in splitted) {
                if (tag.isNotEmpty()) {
                    val temp = JSONArray()
                    val tagInfo: Array<String> = helper.getFullTag(tag)
                    temp.put(tag)
                    temp.put(tagInfo[0])
                    temp.put(tagInfo[1])
                    info.put(temp)
                }
            }
            return info
        }
    }

}