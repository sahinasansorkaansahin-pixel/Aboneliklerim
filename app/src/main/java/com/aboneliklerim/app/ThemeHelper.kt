package com.aboneliklerim.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

object ThemeHelper {
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"
    const val THEME_SMART = "smart"

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme_mode", THEME_SMART) ?: THEME_SMART

        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            THEME_SMART -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hour >= 19 || hour < 7) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    fun setTheme(context: Context, themeMode: String) {
        context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", themeMode).apply()
        applyTheme(context)
    }
}
