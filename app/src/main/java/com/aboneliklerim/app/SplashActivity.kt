package com.aboneliklerim.app
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Smart first-launch language setup:
        // Only run if the user has never set a language preference (app locales is empty).
        // Use the system locale as a ONE-TIME hint — Turkish system → Turkish, everything else → English.
        // After this, the user's manual choice in Settings is the only thing that matters.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            LocaleHelper.applyLocale(this)
        }
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Billing durumunu arka planda senkronize et (sessizce)
        BillingManager.getInstance(this).checkPurchases()

        // Hide status bar for a clean splash look
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Delay before transitioning
        Handler(Looper.getMainLooper()).postDelayed({
            val isFirstRun = getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("first_run", true)

            val intent = if (isFirstRun) {
                Intent(this, OnboardingActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }

            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1200)
    }
}
