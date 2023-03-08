package com.kamui.rin.dict

import android.content.Context
import android.net.Uri
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.db.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream

val format = Json { ignoreUnknownKeys = true }

// TODO: support v3 schema
// TODO: custom pitch accent and frequency dictionaries

@kotlinx.serialization.Serializable
data class YomichanMeta(
    val title: String
)

@kotlinx.serialization.Serializable
data class YomichanDictionaryEntry(
    val expression: String,
    val reading: String,
    val definitionTags: String,
    val ruleIdentifiers: String,
    val popularity: Int,
    val meanings: List<String>,
    val sequence: Int,
    val termTags: List<String>,
)

fun decodeDictionaryEntries(stringData: String): List<YomichanDictionaryEntry> {
    val root: JsonArray = format.parseToJsonElement(stringData).jsonArray
    return root.map {
        YomichanDictionaryEntry(
            it.jsonArray[0].jsonPrimitive.content,
            it.jsonArray[1].jsonPrimitive.content,
            it.jsonArray[2].jsonPrimitive.content,
            it.jsonArray[3].jsonPrimitive.content,
            it.jsonArray[4].jsonPrimitive.intOrNull!!,
            it.jsonArray[5].jsonArray.toList().map { meaning -> meaning.jsonPrimitive.content },
            it.jsonArray[6].jsonPrimitive.intOrNull!!,
            it.jsonArray[7].jsonPrimitive.content.split(" "),
        )
    }
}

fun decodeTags(stringData: String, dictId: Long): List<Tag> {
    val root: JsonArray = format.parseToJsonElement(stringData).jsonArray
    return root.map {
        Tag(
            dictionaryId = dictId,
            name = it.jsonArray[0].jsonPrimitive.content,
            notes = it.jsonArray[3].jsonPrimitive.content,
        )
    }
}


class DictionaryManager {
    suspend fun importYomichan(uri: Uri, context: Context, onProgress: (status: String) -> Unit) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    onProgress("Scanning dictionary files")
                    if (input == null) throw FileNotFoundException("could not open dictionary")

                    val zipMap = ZipInputStream(input).use { stream ->
                        generateSequence { stream.nextEntry }
                            .filterNot { it.isDirectory }
                            .map { entry ->
                                val pair = Pair<String, ByteArray>(entry.name, stream.readBytes())
                                pair
                            }.toMap()
                    }
                    if (!zipMap.containsKey("index.json")) throw FileNotFoundException("index.json could not be found")
                    val index =
                        format.decodeFromString<YomichanMeta>(zipMap["index.json"]!!.decodeToString())
                    onProgress("Creating dictionary ${index.title}")
                    val dictId = AppDatabase.buildDatabase(context).dictionaryDao()
                        .insertDictionary(Dictionary(name = index.title))

                    onProgress("Importing tags from ${index.title}")
                    val tags =
                        zipMap.filter { (key, _) -> key.startsWith("tag_bank") && key.endsWith(".json") }
                            .map { (_, value) ->
                                decodeTags(value.decodeToString(), dictId)
                            }.flatten()
                    val tagMap = AppDatabase.buildDatabase(context).tagDao().insertTagsAndRetrieve(tags)
                        .associateBy { it.name }

                    onProgress("Importing entries from ${index.title}")
                    val termEntries =
                        zipMap.filter { (key, _) -> key.startsWith("term_bank_") && key.endsWith(".json") }
                            .map { (_, termBank) ->
                                val entries = decodeDictionaryEntries(termBank.decodeToString())
                                entries
                            }
                            .flatten()

                    val dbEntries = termEntries.map { entry ->
                        entry.meanings.map { meaning ->
                            meaning.replace("\n", "\n\n").trim { it <= ' ' }
                        }.map { meaning ->
                            Pair(DictEntry(
                                kanji = entry.expression,
                                meaning = meaning,
                                reading = entry.reading,
                                dictionaryId = dictId,
                                freq = null,
                                pitchAccent = null
                            ), entry.termTags.mapNotNull { tagMap[it] })
                        }
                    }.flatten()

                    onProgress("Inserting into database")
                    AppDatabase.buildDatabase(context).dictEntryDao()
                        .insertEntriesWithTags(dbEntries)

                    onProgress("Done")
                }
            }.onFailure { exception ->
                throw exception
            }
        }
    }
    suspend fun deleteDictionary(context: Context, dictionary: Dictionary, onDone: () -> Unit) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                AppDatabase.buildDatabase(context).dictionaryDao().deleteDictionary(dictionary)
            }
        }
    }
}