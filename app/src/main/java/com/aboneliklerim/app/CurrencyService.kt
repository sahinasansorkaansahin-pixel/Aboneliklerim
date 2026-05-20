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
        "AED" to 0.080617,
        "AUD" to 0.030751,
        "CAD" to 0.030194,
        "IDR" to 450.0,
        "INR" to 2.45,
        "CZK" to 0.459427,
        "DKK" to 0.140941,
        "EUR" to 0.018892,
        "GBP" to 0.016478,
        "JPY" to 3.485347,
        "KRW" to 32.907117,
        "NOK" to 0.204493,
        "PLN" to 0.080234,
        "SAR" to 0.082319,
        "SEK" to 0.207242,
        "SGD" to 0.028107,
        "THB" to 0.715,
        "TRY" to 1.0,
        "USD" to 0.021952
    )

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
