package com.aboneliklerim.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object DateFormatHelper {
    
    val FORMATS = listOf(
        "dd.MM.yyyy",
        "MM/dd/yyyy",
        "yyyy-MM-dd",
        "dd MMM yyyy"
    )

    fun getDefaultFormat(context: Context): String {
        val activeLang = LocaleHelper.getActiveLanguage(context)
        return when {
            activeLang.startsWith("en-US") -> "MM/dd/yyyy"
            activeLang.startsWith("ja") || activeLang.startsWith("ko") || activeLang.startsWith("zh") || activeLang.startsWith("sv") -> "yyyy-MM-dd"
            else -> "dd.MM.yyyy"
        }
    }

    fun getSelectedFormat(context: Context): String {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        return prefs.getString("date_format", getDefaultFormat(context)) ?: getDefaultFormat(context)
    }

    fun formatForDisplay(context: Context, dateStr: String): String {
        // Data is always stored as yyyy-MM-dd
        try {
            val sdfInternal = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInternal.parse(dateStr) ?: return dateStr
            val selectedFormat = getSelectedFormat(context)
            val appLocale = Locale.forLanguageTag(LocaleHelper.getActiveLanguage(context))
            val sdfDisplay = SimpleDateFormat(selectedFormat, appLocale)
            return sdfDisplay.format(date)
        } catch (e: Exception) {
            return dateStr
        }
    }
    
    fun formatForDisplay(context: Context, date: Date): String {
        val selectedFormat = getSelectedFormat(context)
        val appLocale = Locale.forLanguageTag(LocaleHelper.getActiveLanguage(context))
        val sdfDisplay = SimpleDateFormat(selectedFormat, appLocale)
        return sdfDisplay.format(date)
    }
}
