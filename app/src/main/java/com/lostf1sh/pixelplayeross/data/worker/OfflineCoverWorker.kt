package com.lostf1sh.pixelplayeross.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lostf1sh.pixelplayeross.data.offline.OfflineCopyCovers
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Saves the covers of offline copies, after each one completes and after each sync. */
@HiltWorker
class OfflineCoverWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val covers: OfflineCopyCovers
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when {
        covers.saveMissing() -> Result.success()
        runAttemptCount < MAX_RETRIES -> Result.retry()
        else -> Result.success()
    }

    companion object {
        private const val WORK_NAME = "offline_cover_save"
        /** Low on purpose: the next sync enqueues the work again, so a server that stays down does not keep it alive. */
        private const val MAX_RETRIES = 3
        private const val BACKOFF_SECONDS = 30L

        /** Appends, not replaces: a running save is never cancelled half way through a cover. */
        fun enqueue(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<OfflineCoverWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
