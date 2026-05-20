package com.aboneliklerim.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StreamingPriceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Fetch and sync prices
            StreamingPriceService.fetchAndSyncPrices(applicationContext, forceRefresh = true)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
