package com.aboneliklerim.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class AbonelikApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applyLocale(this)

        // 1. Firebase Başlatma
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. BillingManager Başlatma (Premium durumu kontrolü için)
        BillingManager.getInstance(this)

        // 3. Platform Fiyat Takibi Zamanlayıcısı
        setupPeriodicPriceSync()
    }

    private fun setupPeriodicPriceSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<StreamingPriceSyncWorker>(
                12, TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "StreamingPriceSync",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
