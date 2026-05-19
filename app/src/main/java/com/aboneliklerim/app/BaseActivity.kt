package com.aboneliklerim.app

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply DPI / Font Scaling globally
        val scaledContext = DisplayHelper.getScaledContext(newBase)
        super.attachBaseContext(scaledContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Theme before super.onCreate
        ThemeHelper.applyTheme(this)
        LocaleHelper.applyLocale(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // Apply Brightness and Eye Protection Overlay
        DisplayHelper.applyDisplaySettings(this)
    }
}
