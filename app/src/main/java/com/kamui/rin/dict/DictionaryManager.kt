package com.kamui.rin.dict

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.*
import kotlinx.serialization.decodeFromString
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.Thread.State
import java.util.zip.ZipInputStream

enum class DataStatus {
    SUCCESS,
    ERROR,
    LOADING,
    COMPLETE
}

// use inside LiveData
data class StateData<T>(
    val status: DataStatus,
    val data: T? = null,
    val progressData: String? = null,
    val error: Throwable? = null
)

class StateLiveData<T>(private val minimalLiveData: MutableLiveData<StateData<Unit>>? = null) : MutableLiveData<StateData<T>>() {
    fun postLoading(progress: String) {
        postValue(StateData(DataStatus.LOADING, progressData = progress))
    }

    fun postError(throwable: Throwable) {
        postValue(StateData(DataStatus.ERROR, error = throwable))
    }

    fun postSuccess(data: T) {
        postValue(StateData(DataStatus.SUCCESS, data = data))
    }

    fun postComplete() {
        postValue(StateData(DataStatus.COMPLETE))
    }

    override fun postValue(stateData: StateData<T>?) {
        super.postValue(stateData)
        minimalLiveData?.postValue(stateData?.copy(data = null) as StateData<Unit>)
    }
}

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
    fun deleteDictionary(
        dictionary: Dictionary,
        stateLiveData: StateLiveData<Long>
    ) {
        stateLiveData.postLoading("Deleting")
        AppDatabase.buildDatabase(context).dictionaryDao().deleteDictionary(dictionary)
        stateLiveData.postSuccess(dictionary.dictId)
    }

    fun importYomichanDictionary(
        uri: Uri,
        stateLiveData: StateLiveData<Dictionary>
    ) {
        context.contentResolver.openInputStream(uri).use { input ->
            stateLiveData.postLoading("Scanning dictionary files")
            if (input == null) {
                return stateLiveData.postError(FileNotFoundException("could not open dictionary"))
            }

            val zipMap = mapFilenameToBytes(input)
            if (!zipMap.containsKey("index.json")) {
                return stateLiveData.postError(FileNotFoundException("index.json could not be found"))
            }
            val index =
                format.decodeFromString<YomichanMeta>(zipMap["index.json"]!!.decodeToString())
            stateLiveData.postLoading("Creating dictionary ${index.title}")
            val dictId = AppDatabase.buildDatabase(context).dictionaryDao()
                .insertDictionary(Dictionary(name = index.title))

            stateLiveData.postLoading("Importing tags from ${index.title}")
            val tags =
                zipMap.filter { (key, _) -> key.startsWith("tag_bank") && key.endsWith(".json") }
                    .map { (_, value) ->
                        decodeTags(value.decodeToString(), dictId)
                    }.flatten()
            val tagMap =
                AppDatabase.buildDatabase(context).tagDao().insertTagsAndRetrieve(tags)
                    .associateBy { it.name }

            stateLiveData.postLoading("Importing entries from ${index.title}")
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

            stateLiveData.postLoading("Inserting into database")
            AppDatabase.buildDatabase(context).dictEntryDao()
                .insertEntriesWithTags(dbEntries)

            stateLiveData.postSuccess(Dictionary(dictId, index.title))
        }
    }

    fun importFrequencyList(
        uri: Uri,
        stateLiveData: StateLiveData<Unit>
    ) {
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) {
                return stateLiveData.postError(FileNotFoundException("could not open frequency list"))
            }
            stateLiveData.postLoading("Scanning frequency list files")
            val zipMap = mapFilenameToBytes(input)
            stateLiveData.postLoading("Importing frequency data")
            val frequencyList = zipMap.filter { (key, _) ->
                key.startsWith("term_meta_bank_") && key.endsWith(
                    ".json"
                )
            }
                .map { (_, termBank) -> decodeFrequencyEntries(termBank.decodeToString()) }
                .flatten()
            stateLiveData.postLoading("Inserting into database")
            val dao = AppDatabase.buildDatabase(context).frequencyDao()
            dao.clear()
            dao.insertFrequencies(frequencyList)
            stateLiveData.postComplete()
        }
    }

    fun importPitchAccent(
        uri: Uri,
        stateLiveData: StateLiveData<Unit>
    ) {
        context.contentResolver.openInputStream(uri).use { input ->
            stateLiveData.postLoading("Scanning pitch accent files")
            if (input == null) {
                return stateLiveData.postError(FileNotFoundException("could not open pitch accent dictionary"))
            }

            val zipMap = mapFilenameToBytes(input)
            stateLiveData.postLoading("Importing pitch accent data")
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
                stateLiveData.postError(Exception("Error: Invalid dictionary format. Rin currently only supports the old pitch accent dictionary format."))
            } else {
                stateLiveData.postLoading("Inserting into database")
                val dao = AppDatabase.buildDatabase(context).pitchAccentDao()
                dao.clear()
                dao.insertPitchAccents(pitchEntries)
                stateLiveData.postComplete()
            }
        }
    }
}