package dev.blockabsbebsh.birdy

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.TimeUnit

class FeedSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val feed = FeedStore(applicationContext).sync()
            BirdyScheduler.scheduleRotation(applicationContext, feed)
            BirdyWidget().updateAll(applicationContext)
            Result.success()
        } catch (error: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

class RotationWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        BirdyWidget().updateAll(applicationContext)
        FeedStore(applicationContext).cachedFeed()?.let {
            BirdyScheduler.scheduleRotation(applicationContext, it)
        }
        return Result.success()
    }
}

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        BirdyWidget().updateAll(applicationContext)
        return Result.success()
    }
}

object BirdyScheduler {
    private val connected = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun start(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<FeedSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "birdy-feed-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
        syncNow(context)
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<FeedSyncWorker>()
            .setConstraints(connected)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "birdy-feed-sync-now",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun refreshWidgets(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "birdy-language-refresh",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleRotation(context: Context, feed: BirdFeed) {
        val request = OneTimeWorkRequestBuilder<RotationWorker>()
            .setInitialDelay(feed.nextRotationDelayMillis(Instant.now()), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "birdy-next-rotation",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
