package com.kamui.rin.dict.worker

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.PitchAccent
import com.kamui.rin.dict.decodeDictionaryEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class ImportPitchWorker(context: Context, parameters: WorkerParameters) :
    BaseDictionaryWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(inputData.getString("URI"))
            val progress = "Importing pitch accent data"
            setForeground(createForegroundInfo(progress))
            return@withContext try {
                importPitchAccents(uri)
                Result.success()
            } catch (throwable: Throwable) {
                onException(throwable)
                throwable.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun importPitchAccents(uri: Uri) {
        return applicationContext.contentResolver.openInputStream(uri).use f@{ input ->
            updateNotification("Scanning pitch accent files")
            if (input == null) {
                throw FileNotFoundException("could not open pitch accent dictionary")
            }

            val zipMap = mapFilenameToBytes(input)
            updateNotification("Importing pitch accent data")
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
                throw Exception("Error: Invalid dictionary format. Rin currently only supports the old pitch accent dictionary format.")
            } else {
                updateNotification("Inserting into database")
                val dao = AppDatabase.buildDatabase(applicationContext).pitchAccentDao()
                dao.clear()
                dao.insertPitchAccents(pitchEntries)
            }
        }
    }

    override fun getNotificationId(): Int {
        return 2; }
}
