package com.kamui.rin.dict.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.dict.YomichanMeta
import com.kamui.rin.dict.decodeDictionaryEntries
import com.kamui.rin.dict.decodeTags
import com.kamui.rin.dict.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.FileNotFoundException

class ImportDictionaryWorker(context: Context, parameters: WorkerParameters) :
    BaseDictionaryWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(inputData.getString("URI"))
            val progress = "Importing dictionary"
            setForeground(createForegroundInfo(progress))
            return@withContext try {
                val dict = importDictionary(uri)
                val data = workDataOf("DICT_ID" to dict.dictId, "DICT_TITLE" to dict.name)
                Result.success(data)
            } catch (throwable: Throwable) {
                onException(throwable)
                throwable.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun importDictionary(uri: Uri): Dictionary {
        return applicationContext.contentResolver.openInputStream(uri).use f@{ input ->
            updateNotification("Scanning dictionary files")
            if (input == null) {
                throw FileNotFoundException("could not open dictionary")
            }

            val zipMap = mapFilenameToBytes(input)
            if (!zipMap.containsKey("index.json")) {
                throw FileNotFoundException("index.json could not be found")
            }
            val index =
                format.decodeFromString<YomichanMeta>(zipMap["index.json"]!!.decodeToString())
            updateNotification("Creating dictionary ${index.title}")
            val dictId = AppDatabase.buildDatabase(applicationContext).dictionaryDao()
                .insertDictionary(Dictionary(name = index.title))
            Log.d("RIN", dictId.toString())

            updateNotification("Importing tags from ${index.title}")
            val tags =
                zipMap.filter { (key, _) -> key.startsWith("tag_bank") && key.endsWith(".json") }
                    .map { (_, value) ->
                        decodeTags(value.decodeToString(), dictId)
                    }.flatten()
            val tagMap =
                AppDatabase.buildDatabase(applicationContext).tagDao().insertTagsAndRetrieve(tags)
                    .associateBy { it.name }

            updateNotification("Importing entries from ${index.title}")
            val termEntries =
                zipMap.filter { (key, _) -> key.startsWith("term_bank_") && key.endsWith(".json") }
                    .map { (_, termBank) ->
                        decodeDictionaryEntries(termBank.decodeToString())
                    }.flatten()

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

            updateNotification("Inserting into database")
            AppDatabase.buildDatabase(applicationContext).dictEntryDao()
                .insertEntriesWithTags(dbEntries)

            return@f Dictionary(dictId, index.title)
        }
    }

    override fun getNotificationId(): Int {
        return 1; }
}