package com.kamui.rin.dict.worker

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kamui.rin.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteDictionaryWorker(context: Context, parameters: WorkerParameters) : BaseDictionaryWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val dictId = inputData.getLong("DICT_ID", 0) // FIXME: find out optional default
                val progress = "Deleting dictionary"
                setForeground(createForegroundInfo(progress))
                deleteDictionary(dictId)
                return@withContext Result.success()
            } catch (throwable: Throwable) {
                onException(throwable)
                throwable.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun deleteDictionary(dictId: Long) {
        AppDatabase.buildDatabase(applicationContext).dictionaryDao().deleteDictionary(dictId)
    }

    override fun getNotificationId(): Int { return 0; }
}