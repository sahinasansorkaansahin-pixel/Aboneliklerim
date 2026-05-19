package com.aboneliklerim.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    val SUPPORTED_LOCALES = listOf(
        "tr-TR", "en-US", "de-DE", "fr-FR", "it-IT", "es-ES", "pt-PT", "nl-NL",
        "sv-SE", "no-NO", "da-DK", "fi-FI", "pl-PL", "cs-CZ", "de-CH", "ja-JP", 
        "ko-KR", "zh-CN", "th-TH", "ar-AE"
    )

    fun getCountryFlag(localeTag: String): String {
        val parts = localeTag.split("-")
        val country = if (parts.size > 1) parts[1].uppercase(Locale.US) else ""
        return when (country) {
            "TR" -> "🇹🇷"; "US" -> "🇺🇸"; "DE" -> "🇩🇪"; "FR" -> "🇫🇷"
            "IT" -> "🇮🇹"; "ES" -> "🇪🇸"; "PT" -> "🇵🇹"; "NL" -> "🇳🇱"
            "SE" -> "🇸🇪"; "NO" -> "🇳🇴"; "DK" -> "🇩🇰"; "FI" -> "🇫🇮"
            "PL" -> "🇵🇱"; "CZ" -> "🇨🇿"; "CH" -> "🇨🇭"; "JP" -> "🇯🇵"
            "KR" -> "🇰🇷"; "CN" -> "🇨🇳"; "TH" -> "🇹🇭"; "AE" -> "🇦🇪"
            else -> "🏳️"
        }
    }

    fun getSupportedLanguageNames(context: Context): Array<String> {
        return getSupportedLanguageNames()
    }

    fun getSupportedLanguageNames(): Array<String> {
        return SUPPORTED_LOCALES.map { tag ->
            val flag = getCountryFlag(tag)
            val locale = Locale.forLanguageTag(tag)
            val name = when (tag) {
                "tr-TR" -> "Türkçe"
                "en-US" -> "English"
                "de-DE" -> "Deutsch"
                "fr-FR" -> "Français"
                "it-IT" -> "Italiano"
                "es-ES" -> "Español"
                "pt-PT" -> "Português"
                "nl-NL" -> "Nederlands"
                "sv-SE" -> "Svenska"
                "no-NO" -> "Norsk"
                "da-DK" -> "Dansk"
                "fi-FI" -> "Suomi"
                "pl-PL" -> "Polski"
                "cs-CZ" -> "Čeština"
                "de-CH" -> "Deutsch (Schweiz)"
                "ja-JP" -> "日本語"
                "ko-KR" -> "한국어"
                "zh-CN" -> "简体中文"
                "th-TH" -> "ไทย"
                "ar-AE" -> "العربية"
                else -> locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
            }
            "$flag $name"
        }.toTypedArray()
    }

    fun getActiveLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_language", null)
        if (savedLang != null) return savedLang
        val systemLocaleTag = Locale.getDefault().toLanguageTag()
        val systemLang = Locale.getDefault().language
        val match = SUPPORTED_LOCALES.find { it == systemLocaleTag } 
            ?: SUPPORTED_LOCALES.find { it.startsWith(systemLang) }
        return match ?: "en-US"
    }

    fun applyLocale(context: Context) {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_language", null)
        
        if (savedLang != null) {
            setLocale(savedLang)
        } else {
            val systemLocaleTag = Locale.getDefault().toLanguageTag()
            val systemLang = Locale.getDefault().language
            val match = SUPPORTED_LOCALES.find { it == systemLocaleTag } 
                ?: SUPPORTED_LOCALES.find { it.startsWith(systemLang) }
            
            if (match != null) {
                setLocale(match)
            } else {
                setLocale("en-US")
            }
        }
    }

    private fun setLocale(langTag: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(langTag)
        )
    }

    fun setManualLocale(context: Context, langTag: String) {
        context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .edit()
            .putString("selected_language", langTag)
            .remove("date_format") // Synchronize date format with the new country/language
            .apply()
        setLocale(langTag)
    }
}
