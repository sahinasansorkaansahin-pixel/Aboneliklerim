package com.aboneliklerim.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object CurrencyService {

    var isPremiumActive: Boolean = false

    private val fallbackRates = mapOf(
        "AED" to 0.080411,
        "AFN" to 1.386031,
        "ALL" to 1.801124,
        "ANG" to 0.039193,
        "AOA" to 20.575915,
        "ARS" to 30.482323,
        "AUD" to 0.030666,
        "AWG" to 0.039193,
        "AZN" to 0.037295,
        "BAM" to 0.036863,
        "BBD" to 0.043791,
        "BDT" to 2.692032,
        "BHD" to 0.008233,
        "BIF" to 65.300971,
        "BMD" to 0.021896,
        "BND" to 0.028006,
        "BOB" to 0.151807,
        "BRL" to 0.109608,
        "BSD" to 0.021896,
        "BTN" to 2.107039,
        "BWP" to 0.302891,
        "BYN" to 0.05997,
        "BZD" to 0.043791,
        "CAD" to 0.030144,
        "CDF" to 50.19403,
        "CHF" to 0.01723,
        "CLF" to 0.000498,
        "CLP" to 19.670883,
        "CNH" to 0.148885,
        "CNY" to 0.1495,
        "COP" to 81.541967,
        "CRC" to 9.927876,
        "CUP" to 0.525493,
        "CVE" to 2.078248,
        "CZK" to 0.457978,
        "DJF" to 3.891297,
        "DKK" to 0.140611,
        "DOP" to 1.292999,
        "DZD" to 2.912513,
        "EGP" to 1.158956,
        "ERN" to 0.328433,
        "ETB" to 3.465224,
        "EUR" to 0.018848,
        "FJD" to 0.048205,
        "FKP" to 0.016301,
        "FOK" to 0.140629,
        "GBP" to 0.016306,
        "GEL" to 0.058553,
        "GGP" to 0.016301,
        "GHS" to 0.252974,
        "GIP" to 0.016301,
        "GMD" to 1.62352,
        "GNF" to 191.809572,
        "GTQ" to 0.167352,
        "GYD" to 4.587995,
        "HKD" to 0.171571,
        "HNL" to 0.584021,
        "HRK" to 0.142008,
        "HTG" to 2.869454,
        "HUF" to 6.778217,
        "IDR" to 386.820591,
        "IMP" to 0.016301,
        "INR" to 2.10704,
        "IQD" to 28.74359,
        "IRR" to 29009.90099,
        "ISK" to 2.709389,
        "JEP" to 0.016301,
        "JMD" to 3.468476,
        "JOD" to 0.015524,
        "JPY" to 3.482316,
        "KES" to 2.841883,
        "KGS" to 1.917258,
        "KHR" to 88.5,
        "KID" to 0.030641,
        "KMF" to 9.272488,
        "KRW" to 32.983562,
        "KWD" to 0.00677,
        "KYD" to 0.018246,
        "KZT" to 10.298699,
        "LAK" to 481.284888,
        "LBP" to 1959.650717,
        "LKR" to 7.50792,
        "LRD" to 4.019903,
        "LSL" to 0.360421,
        "LYD" to 0.139471,
        "MAD" to 0.202178,
        "MDL" to 0.380612,
        "MGA" to 92.136986,
        "MKD" to 1.1667,
        "MMK" to 46.106781,
        "MNT" to 78.435073,
        "MOP" to 0.176718,
        "MRU" to 0.876694,
        "MUR" to 1.038882,
        "MVR" to 0.339145,
        "MWK" to 38.142465,
        "MXN" to 0.378999,
        "MYR" to 0.086824,
        "MZN" to 1.395739,
        "NAD" to 0.360421,
        "NGN" to 30.044909,
        "NIO" to 0.80792,
        "NOK" to 0.202181,
        "NPR" to 3.371263,
        "NZD" to 0.037272,
        "OMR" to 0.008419,
        "PAB" to 0.021896,
        "PEN" to 0.074813,
        "PGK" to 0.095663,
        "PHP" to 1.34929,
        "PKR" to 6.1149,
        "PLN" to 0.080007,
        "PYG" to 135.135603,
        "QAR" to 0.0797,
        "RON" to 0.09885,
        "RSD" to 2.215457,
        "RUB" to 1.558321,
        "RWF" to 32.085827,
        "SAR" to 0.082108,
        "SBD" to 0.173936,
        "SCR" to 0.315131,
        "SDG" to 9.804665,
        "SEK" to 0.204739,
        "SGD" to 0.02799,
        "SHP" to 0.016301,
        "SLE" to 0.532813,
        "SLL" to 532.869483,
        "SOS" to 12.52514,
        "SRD" to 0.821546,
        "SSP" to 103.070089,
        "STN" to 0.46177,
        "SYP" to 2.465679,
        "SZL" to 0.360421,
        "THB" to 0.714374,
        "TJS" to 0.20355,
        "TMT" to 0.076672,
        "TND" to 0.063677,
        "TOP" to 0.051956,
        "TRY" to 1.0,
        "TTD" to 0.149014,
        "TVD" to 0.030641,
        "TWD" to 0.691702,
        "TZS" to 57.685459,
        "UAH" to 0.968691,
        "UGX" to 82.794246,
        "USD" to 0.021896,
        "UYU" to 0.885239,
        "UZS" to 266.561623,
        "VES" to 11.551316,
        "VND" to 575.530006,
        "VUV" to 2.592379,
        "WST" to 0.058856,
        "XAF" to 12.363318,
        "XCD" to 0.059118,
        "XCG" to 0.039193,
        "XDR" to 0.015957,
        "XOF" to 12.363318,
        "XPF" to 2.249141,
        "YER" to 5.238282,
        "ZAR" to 0.360422,
    )

    fun getRate(currencyCode: String, context: Context): Double {
        val cached = loadRatesFromLocal(context)
        val rate = cached[currencyCode] ?: fallbackRates[currencyCode] ?: 1.0
        return if (rate <= 0.0) 1.0 else rate
    }

    fun saveRatesToLocal(context: Context, ratesMap: Map<String, Double>) {
        try {
            val json = Gson().toJson(ratesMap)
            context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("exchange_rates", json)
                .putLong("last_update_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadRatesFromLocal(context: Context): Map<String, Double> {
        try {
            val json = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                .getString("exchange_rates", null) ?: return emptyMap()
            val type = object : TypeToken<Map<String, Double>>() {}.type
            return Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }

    suspend fun getExchangeRates(context: Context? = null): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            if (context != null) {
                val cached = loadRatesFromLocal(context)
                val lastUpdate = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                    .getLong("last_update_time", 0)
                if (cached.isNotEmpty() && (System.currentTimeMillis() - lastUpdate) < 12 * 60 * 60 * 1000) {
                    return@withContext cached
                }
            }

            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://open.er-api.com/v6/latest/TRY")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString != null) {
                            val jsonObject = JsonParser.parseString(bodyString).asJsonObject
                            val ratesObject = jsonObject.getAsJsonObject("rates")
                            val tryRates = mutableMapOf<String, Double>()
                            for (entry in ratesObject.entrySet()) {
                                tryRates[entry.key] = entry.value.asDouble
                            }

                            if (context != null && tryRates.isNotEmpty()) {
                                saveRatesToLocal(context, tryRates)
                            }
                            return@withContext tryRates
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (context != null) {
                val cached = loadRatesFromLocal(context)
                if (cached.isNotEmpty()) return@withContext cached
            }
            return@withContext fallbackRates
        }
    }

    fun convertToTry(amount: Double, fromCurrency: String, rates: Map<String, Double>): Double {
        if (fromCurrency == "TRY") return amount
        val rate = rates[fromCurrency] ?: fallbackRates[fromCurrency] ?: 1.0
        if (rate <= 0.0) return amount
        return amount / rate
    }

    fun convertFromTry(amountInTry: Double, toCurrency: String, rates: Map<String, Double>): Double {
        if (toCurrency == "TRY") return amountInTry
        val rate = rates[toCurrency] ?: fallbackRates[toCurrency] ?: 1.0
        if (rate <= 0.0) return amountInTry
        return amountInTry * rate
    }
}
