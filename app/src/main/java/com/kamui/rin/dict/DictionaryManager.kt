package com.kamui.rin.dict

import android.content.Context
import android.net.Uri
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.ZipInputStream

fun mapFilenameToBytes(input: InputStream): Map<String, ByteArray> {
    return ZipInputStream(input).use { stream ->
        generateSequence { stream.nextEntry }
            .filterNot { it.isDirectory }
            .map { entry ->
                val pair = Pair<String, ByteArray>(entry.name, stream.readBytes())
                pair
            }.toMap()
    }
}


class DictionaryManager(val context: Context) {
    suspend fun deleteDictionary(
        dictionary: Dictionary,
        onProgress: (String, Long?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                onProgress("Deleting", null)
                AppDatabase.buildDatabase(context).dictionaryDao().deleteDictionary(dictionary)
                onProgress("Done", dictionary.dictId)
            }.onFailure { exception ->
                throw exception
            }
        }
    }

    suspend fun importYomichanDictionary(
        uri: Uri,
        onProgress: (status: String, finalData: Dictionary?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    onProgress("Scanning dictionary files", null)
                    if (input == null) throw FileNotFoundException("could not open dictionary")

                    val zipMap = mapFilenameToBytes(input)
                    if (!zipMap.containsKey("index.json")) throw FileNotFoundException("index.json could not be found")
                    val index =
                        format.decodeFromString<YomichanMeta>(zipMap["index.json"]!!.decodeToString())
                    onProgress("Creating dictionary ${index.title}", null)
                    val dictId = AppDatabase.buildDatabase(context).dictionaryDao()
                        .insertDictionary(Dictionary(name = index.title))

                    onProgress("Importing tags from ${index.title}", null)
                    val tags =
                        zipMap.filter { (key, _) -> key.startsWith("tag_bank") && key.endsWith(".json") }
                            .map { (_, value) ->
                                decodeTags(value.decodeToString(), dictId)
                            }.flatten()
                    val tagMap =
                        AppDatabase.buildDatabase(context).tagDao().insertTagsAndRetrieve(tags)
                            .associateBy { it.name }

                    onProgress("Importing entries from ${index.title}", null)
                    val termEntries =
                        zipMap.filter { (key, _) -> key.startsWith("term_bank_") && key.endsWith(".json") }
                            .map { (_, termBank) ->
                                decodeDictionaryEntries(termBank.decodeToString())
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
                            ), entry.termTags.mapNotNull { tagMap[it] })
                        }
                    }.flatten()

                    onProgress("Inserting into database", null)
                    AppDatabase.buildDatabase(context).dictEntryDao()
                        .insertEntriesWithTags(dbEntries)

                    onProgress("Done", Dictionary(dictId, index.title))
                }
            }.onFailure { exception ->
                throw exception
            }
        }
    }

    suspend fun importFrequencyList(
        uri: Uri,
        onProgress: (status: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw FileNotFoundException("could not open frequency list")
                    onProgress("Scanning frequency list files")
                    val zipMap = mapFilenameToBytes(input)
                    onProgress("Importing frequency data")
                    val frequencyList = zipMap.filter { (key, _) ->
                        key.startsWith("term_meta_bank_") && key.endsWith(
                            ".json"
                        )
                    }
                        .map { (_, termBank) -> decodeFrequencyEntries(termBank.decodeToString()) }
                        .flatten()
                    onProgress("Inserting into database")
                    val dao = AppDatabase.buildDatabase(context).frequencyDao()
                    dao.clear()
                    dao.insertFrequencies(frequencyList)
                    onProgress("Done")
                }
            }.onFailure { exception ->
                throw exception
            }
        }
    }

    suspend fun importPitchAccent(
        uri: Uri,
        onProgress: (status: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    onProgress("Scanning pitch accent files")
                    if (input == null) throw FileNotFoundException("could not open pitch accent dictionary")

                    val zipMap = mapFilenameToBytes(input)
                    onProgress("Importing pitch accent data")
                    val pitchEntries =
                        zipMap.filter { (key, _) -> key.startsWith("term_bank_") && key.endsWith(".json") }
                            .map { (_, termBank) -> decodeDictionaryEntries(termBank.decodeToString()) }
                            .flatten()
                            .map { entry ->
                                PitchAccent(
                                    kanji = entry.expression,
                                    pitch = entry.meanings.joinToString("\n") {
                                        it.trim { it <= ' ' }
                                    }
                                )
                            }

                    if (pitchEntries.isEmpty()) {
                        onProgress("Error: Invalid dictionary format. Rin currently only supports the old pitch accent dictionary format.")
                    } else {
                        onProgress("Inserting into database")
                        val dao = AppDatabase.buildDatabase(context).pitchAccentDao()
                        dao.clear()
                        dao.insertPitchAccents(pitchEntries)
                        onProgress("Done")
                    }
                }
            }.onFailure { exception ->
                throw exception
            }
        }
    }
}