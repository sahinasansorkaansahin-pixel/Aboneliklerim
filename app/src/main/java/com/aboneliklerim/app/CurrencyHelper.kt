package com.aboneliklerim.app

import android.content.Context
import java.util.*

object CurrencyHelper {
    
    fun getCurrencies(context: Context): List<CurrencyItem> {
        val activeLang = LocaleHelper.getActiveLanguage(context)
        val currentLocale = Locale.forLanguageTag(activeLang)
        val rawCodes = listOf(
            "TRY", "USD", "EUR", "GBP", "CHF", "KWD", "BHD", "OMR", "JOD", "CAD", "AUD", "JPY", "KRW", "AZN", "UAH", "NOK", "SEK", "DKK", "SAR", "AED", "PLN", "CZK", "THB", "SGD", "AFN", "ALL", "ANG", "AOA", "ARS", "AWG", "BAM", "BBD", "BDT", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN", "BWP", "BYN", "BZD", "CDF", "CLF", "CLP", "CNH", "CNY", "COP", "CRC", "CUP", "CVE", "DJF", "DOP", "DZD", "EGP", "ERN", "ETB", "FJD", "FKP", "FOK", "GEL", "GGP", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "IMP", "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "KES", "KGS", "KHR", "KID", "KMF", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NPR", "NZD", "PAB", "PEN", "PGK", "PHP", "PKR", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SBD", "SCR", "SDG", "SHP", "SLE", "SLL", "SOS", "SRD", "SSP", "STN", "SYP", "SZL", "TJS", "TMT", "TND", "TOP", "TTD", "TVD", "TWD", "TZS", "UGX", "UYU", "UZS", "VES", "VND", "VUV", "WST", "XAF", "XCD", "XCG", "XDR", "XOF", "XPF", "YER", "ZAR", "ZMW"
        )
        val sortedCodes = rawCodes.sortedBy { code ->
            CurrencyService.getRate(code, context)
        }
        return sortedCodes.map { code ->
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
        if (currencyCode.length < 2) return "🏳️"
        
        when (currencyCode) {
            "EUR" -> return "🇪🇺"
            "BTC" -> return "🪙"
            "ETH" -> return "🪙"
            "XOF" -> return "🏳️"
            "XAF" -> return "🏳️"
            "XPF" -> return "🇵🇫"
            "XCD" -> return "🏳️"
            "XDR" -> return "🏳️"
            "ANG" -> return "🇨🇼"
            "AWG" -> return "🇦🇼"
            "FOK" -> return "🇫🇴"
            "GGP" -> return "🇬🇬"
            "GIP" -> return "🇬🇮"
            "IMP" -> return "🇮🇲"
            "JEP" -> return "🇯🇪"
            "KID" -> return "🇰🇮"
            "TVD" -> return "🇹🇻"
        }

        val countryCode = currencyCode.substring(0, 2).uppercase()
        val firstChar = countryCode[0]
        val secondChar = countryCode[1]
        
        if (firstChar in 'A'..'Z' && secondChar in 'A'..'Z') {
            val firstCodePoint = 0x1F1E6 + (firstChar - 'A')
            val secondCodePoint = 0x1F1E6 + (secondChar - 'A')
            val firstChars = Character.toChars(firstCodePoint)
            val secondChars = Character.toChars(secondCodePoint)
            return String(firstChars) + String(secondChars)
        }
        return "🏳️"
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
