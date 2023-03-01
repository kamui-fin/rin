package com.kamui.rin.util

import android.content.Context
import android.net.Uri
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.DictEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream

data class Index(
    val title: String
)

data class Entry(
    val expression: String,
    val reading: String,
    val definition_tags: String,
    val rule_identifiers: String,
    val popularity: Int,
    val meanings: List<String>,
    val sequence: Int,
    val term_tags: String,
)

class DictionaryManager {
    suspend fun importYomichan(uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw FileNotFoundException("could not open dictionary")
                    val zipMap = ZipInputStream(input).use { stream ->
                        generateSequence { stream.nextEntry }
                            .filterNot { it.isDirectory }
                            .map { entry ->
                                Pair<String, ByteArray>(entry.name, stream.readAllBytes())
                            }.toMap()
                    }

                    if (!zipMap.containsKey("index.json")) throw FileNotFoundException("index.json could not be found")
                    // process index, title is enough
                    val index =
                        Json.decodeFromString<Index>(zipMap["index.json"]!!.decodeToString())
                    // TODO: process tags
                    // process term banks
                    val termEntries =
                        zipMap.keys.filter { it.startsWith("term_bank_") && it.endsWith(".json") }
                            .map { termBank ->
                                Json.decodeFromString<Array<Array<JsonElement>>>(zipMap[termBank]!!.decodeToString())
                                    .map { it ->
                                        Entry(
                                            it[0].toString(),
                                            it[1].toString(),
                                            it[2].toString(),
                                            it[3].toString(),
                                            it[4].toString().toInt(),
                                            it[5].jsonArray.toList().map { elm -> elm.toString() },
                                            it[6].toString().toInt(),
                                            it[7].toString()
                                        )
                                    }
                            }.flatten()
                    val dbEntries = termEntries.map { entry ->
                        entry.meanings.map { meaning ->
                            DictEntry(
                                kanji = entry.expression,
                                meaning = meaning,
                                reading = entry.reading,
                                tags = entry.term_tags,
                                dictionaryName = index.title,
                                freq = null,
                                pitchAccent = null
                            )
                        }
                    }.flatten()
                    AppDatabase.buildDatabase(context).dictDao().insertEntries(dbEntries)
                }
            }
        }
    }
}