package com.kamui.rin.dict.worker

import android.net.Uri
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.dict.YomichanMeta
import com.kamui.rin.dict.decodeDictionaryEntries
import com.kamui.rin.dict.decodeTags
import com.kamui.rin.dict.format
import kotlinx.serialization.decodeFromString
import java.io.FileNotFoundException
import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.dict.decodeFrequencyEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportFrequencyWorker(context: Context, parameters: WorkerParameters) :
    BaseDictionaryWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(inputData.getString("URI"))
            val progress = "Importing frequency list"
            setForeground(createForegroundInfo(progress))
            return@withContext try {
                importFrequencyList(uri)
                Result.success()
            } catch (throwable: Throwable) {
                onException(throwable)
                throwable.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun importFrequencyList(uri: Uri) {
        return applicationContext.contentResolver.openInputStream(uri).use f@{ input ->
            if (input == null) {
                throw FileNotFoundException("could not open frequency list")
            }
            updateNotification("Scanning frequency list files")
            val zipMap = mapFilenameToBytes(input)
            updateNotification("Importing frequency data")
            val frequencyList = zipMap.filter { (key, _) ->
                key.startsWith("term_meta_bank_") && key.endsWith(
                    ".json"
                )
            }
                .map { (_, termBank) -> decodeFrequencyEntries(termBank.decodeToString()) }
                .flatten()
            updateNotification("Inserting into database")
            val dao = AppDatabase.buildDatabase(applicationContext).frequencyDao()
            dao.clear()
            dao.insertFrequencies(frequencyList)
        }
    }

    override fun getNotificationId(): Int { return 2; }
}