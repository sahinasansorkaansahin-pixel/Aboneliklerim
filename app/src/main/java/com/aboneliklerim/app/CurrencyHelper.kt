package com.aboneliklerim.app

import android.content.Context
import java.util.*

object CurrencyHelper {
    
    fun getCurrencies(context: Context): List<CurrencyItem> {
        val activeLang = LocaleHelper.getActiveLanguage(context)
        val currentLocale = Locale.forLanguageTag(activeLang)
        return listOf("TRY", "USD", "EUR", "JPY", "GBP", "AZN", "KRW", "UAH", "SEK", "NOK", "DKK", "CAD", "AUD", "SGD", "AED", "SAR", "THB", "PLN", "CZK")
            .map { code ->
                try {
                    val curr = java.util.Currency.getInstance(code)
                    val flag = getFlagEmoji(code)
                    val symbol = getLocalizedSymbol(code, currentLocale)
                    CurrencyItem(flag, curr.getDisplayName(currentLocale), code, symbol)
                } catch (e: Exception) {
                    CurrencyItem("🏳️", code, code, "")
                }
            }
    }

    fun getLocalizedSymbol(currencyCode: String, locale: Locale): String {
        val manual = getManualSymbol(currencyCode)
        if (manual != null) return manual
        try {
            val curr = java.util.Currency.getInstance(currencyCode)
            val symbol = curr.getSymbol(locale)
            if (symbol == currencyCode) {
                return curr.symbol
            }
            return symbol
        } catch (e: Exception) {
            return currencyCode
        }
    }

    fun getLocalizedSymbol(currencyCode: String, context: Context): String {
        val activeLang = LocaleHelper.getActiveLanguage(context)
        return getLocalizedSymbol(currencyCode, Locale.forLanguageTag(activeLang))
    }

    private fun getFlagEmoji(currencyCode: String): String {
        return when(currencyCode) {
            "USD" -> "🇺🇸"; "EUR" -> "🇪🇺"; "GBP" -> "🇬🇧"; "TRY" -> "🇹🇷"; "AZN" -> "🇦🇿"
            "NOK" -> "🇳🇴"; "SGD" -> "🇸🇬"; "DKK" -> "🇩🇰"; "AUD" -> "🇦🇺"; "SEK" -> "🇸🇪"
            "CAD" -> "🇨🇦"; "AED" -> "🇦🇪"; "JPY" -> "🇯🇵"; "KRW" -> "🇰🇷"; "SAR" -> "🇸🇦"
            "CZK" -> "🇨🇿"; "PLN" -> "🇵🇱"; "THB" -> "🇹🇭"; "UAH" -> "🇺🇦"
            else -> "🏳️"
        }
    }

    fun getManualSymbol(currencyCode: String): String? {
        return when(currencyCode) {
            "TRY" -> "₺"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "KRW" -> "₩"
            "AZN" -> "₼"
            "UAH" -> "₴"
            "NOK", "SEK", "DKK" -> "kr"
            "SAR" -> "SR"
            "AED" -> "DH"
            "PLN" -> "zł"
            "CZK" -> "Kč"
            "THB" -> "฿"
            "AUD", "CAD", "SGD" -> "$"
            else -> null
        }
    }

    fun getDefaultCurrencyBasedOnLanguage(langCode: String? = null): String {
        val lang = langCode ?: Locale.getDefault().toLanguageTag()
        
        return when (lang) {
            "tr-TR" -> "TRY"
            "en-US" -> "USD"
            "en-GB" -> "GBP"
            "de-DE", "fr-FR", "it-IT", "es-ES", "pt-PT", "nl-NL", "fi-FI" -> "EUR"
            "ja-JP" -> "JPY"
            "ko-KR" -> "KRW"
            "az-AZ" -> "AZN"
            "uk-UA" -> "UAH"
            "ar-AE" -> "AED"
            "sv-SE" -> "SEK"
            "no-NO" -> "NOK"
            "da-DK" -> "DKK"
            "pl-PL" -> "PLN"
            "cs-CZ" -> "CZK"
            "en-CA" -> "CAD"
            "en-AU" -> "AUD"
            "en-SG" -> "SGD"
            "ar-SA" -> "SAR"
            "th-TH" -> "THB"
            else -> {
                if (lang.startsWith("tr")) "TRY"
                else if (lang.startsWith("en")) "USD"
                else if (lang.startsWith("az")) "AZN"
                else if (lang.startsWith("uk")) "UAH"
                else if (lang.startsWith("th")) "THB"
                else "USD"
            }
        }
    }
}

data class CurrencyItem(val flag: String, val name: String, val code: String, val symbol: String)

class CurrencyAdapter(
    private var items: List<CurrencyItem>,
    private val onSelected: (CurrencyItem) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<CurrencyAdapter.CurrViewHolder>() {

    class CurrViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val tvFlag: android.widget.TextView = view.findViewById(R.id.tvCurrencyFlag)
        val tvName: android.widget.TextView = view.findViewById(R.id.tvCurrencyName)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CurrViewHolder {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_currency, parent, false)
        return CurrViewHolder(v)
    }

    override fun onBindViewHolder(holder: CurrViewHolder, position: Int) {
        val item = items[position]
        holder.tvFlag.text = item.flag
        holder.tvName.text = "${item.name} (${item.code})"
        holder.itemView.setOnClickListener { onSelected(item) }
    }

    override fun getItemCount() = items.size

    fun filterList(newItems: List<CurrencyItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
