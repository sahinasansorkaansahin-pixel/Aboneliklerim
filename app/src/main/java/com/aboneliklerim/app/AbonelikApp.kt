package com.aboneliklerim.app

import android.app.Application
import com.google.firebase.FirebaseApp

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

        // 3. Presence Tracking
        PresenceManager.startTracking()
    }
}
