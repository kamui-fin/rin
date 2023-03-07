package com.kamui.rin.dict

import android.content.Context
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.dao.DictEntryDao
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.Settings
import com.kamui.rin.db.model.Dictionary
import java.util.*
import kotlin.collections.ArrayList

class Lookup(
    context: Context,
    deinflectionText: String,
    private val settings: Settings
) {
    private val db: AppDatabase = AppDatabase.buildDatabase(context)
    private val dao: DictEntryDao = db.dictEntryDao()
    private var deinflector: Deinflector = Deinflector(deinflectionText)

    fun lookup(query: String): List<Pair<DictEntry, Dictionary>> {
        val possibleVariations = normalizeWord(query).toMutableList()
        val entries: MutableList<Pair<DictEntry, Dictionary>> = ArrayList()

        if (possibleVariations.isEmpty()) {
            possibleVariations.add(query)
        }

        for (variation in possibleVariations) {
            var results: Map<DictEntry, Dictionary>
            if (isAllKana(variation)) {
                val convertedToHiragana: String = if (!allHiragana(variation)) {
                    katakanaToHiragana(variation)
                } else {
                    variation
                }
                results = dao.searchEntryByReading(convertedToHiragana, settings.disabledDicts())
                if (results.isEmpty()) {
                    results = dao.searchEntryByKanji(variation, settings.disabledDicts())
                }
            } else {
                results = dao.searchEntryByKanji(variation, settings.disabledDicts())
            }
            entries.addAll(results.toList())
        }

        entries.sortWith(compareBy { it.second })
        return entries
    }

    private fun normalizeWord(word: String): List<String> {
        return if (settings.shouldDeconjugate()) {
            deconjugateWord(word.trim { it <= ' ' })
        } else {
            mutableListOf(word)
        }
    }

    private fun deconjugateWord(word: String): List<String> {
        return deinflector.deinflect(word).map { d -> d.term }
    }
}