package com.aboneliklerim.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object StreamingPriceService {

    private const val PREFS_NAME = "streaming_prices_prefs"
    private const val KEY_PRICES_JSON = "cached_prices_json"
    private const val KEY_PREV_PRICES = "prev_prices_map"
    private const val KEY_LAST_UPDATE = "last_update_time"

    // Default remote raw file on GitHub main repository containing the updated pricing
    private const val REMOTE_URL = "https://raw.githubusercontent.com/sahinasansorkaansahin-pixel/Aboneliklerim/main/app/src/main/assets/streaming_prices.json"

    data class Plan(
        val name: String,
        val price: Double,
        val currency: String,
        val period: String
    )

    data class RegionalData(
        val base_price: Double,
        val base_currency: String,
        val plans: List<Plan>
    )

    data class StreamingPlatform(
        val id: String,
        val name: String,
        val base_price: Double,
        val base_currency: String,
        val logo_res: String,
        val official_url: String,
        val plans: List<Plan>,
        val regional: Map<String, RegionalData>? = null,
        val supported_currencies: List<String>? = null
    )

    private fun resolvePlatformsForUserCurrency(context: Context, platforms: List<StreamingPlatform>): List<StreamingPlatform> {
        val activeLang = LocaleHelper.getActiveLanguage(context)
        val targetCurrency = CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)

        return platforms
            .filter { platform ->
                platform.supported_currencies == null || platform.supported_currencies.contains(targetCurrency)
            }
            .map { platform ->
                val regionalData = platform.regional?.get(targetCurrency)
                if (regionalData != null) {
                    platform.copy(
                        base_price = regionalData.base_price,
                        base_currency = regionalData.base_currency,
                        plans = regionalData.plans
                    )
                } else {
                    platform
                }
            }
    }

    private fun getRawLocalFallback(context: Context): List<StreamingPlatform> {
        return try {
            val inputStream = context.assets.open("streaming_prices.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<StreamingPlatform>>() {}.type
            val fullList: List<StreamingPlatform> = Gson().fromJson(reader, type)
            fullList.filter { it.id == "prime_video" || it.id == "netflix" || it.id == "spotify" || it.id == "apple_music" }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getLocalFallback(context: Context): List<StreamingPlatform> {
        return resolvePlatformsForUserCurrency(context, getRawLocalFallback(context))
    }

    suspend fun fetchAndSyncPrices(context: Context, forceRefresh: Boolean = false): List<StreamingPlatform> {
        return withContext(Dispatchers.IO) {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedJson = sharedPrefs.getString(KEY_PRICES_JSON, null)
            val lastUpdate = sharedPrefs.getLong(KEY_LAST_UPDATE, 0)

            // If updated in the last 12 hours, cached data contains supported_currencies, and not forced, return cache
            if (!forceRefresh && cachedJson != null && cachedJson.contains("supported_currencies") && (System.currentTimeMillis() - lastUpdate) < 12 * 60 * 60 * 1000) {
                try {
                    val type = object : TypeToken<List<StreamingPlatform>>() {}.type
                    val list: List<StreamingPlatform> = Gson().fromJson(cachedJson, type)
                    val filtered = list.filter { it.id == "prime_video" || it.id == "netflix" || it.id == "spotify" || it.id == "apple_music" }
                    return@withContext resolvePlatformsForUserCurrency(context, filtered)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Try to fetch from remote
            var fetchedList: List<StreamingPlatform>? = null
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(REMOTE_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString != null) {
                            val type = object : TypeToken<List<StreamingPlatform>>() {}.type
                            val list: List<StreamingPlatform> = Gson().fromJson(bodyString, type)
                            fetchedList = list.filter { it.id == "prime_video" || it.id == "netflix" || it.id == "spotify" || it.id == "apple_music" }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (fetchedList != null && fetchedList!!.isNotEmpty()) {
                val localList = getRawLocalFallback(context)
                val finalFetchedList = (fetchedList!! + localList).distinctBy { it.id }

                // Check and store price changes for trend analysis
                val prevPricesJson = sharedPrefs.getString(KEY_PREV_PRICES, "{}")
                val typeMap = object : TypeToken<Map<String, Double>>() {}.type
                val prevPricesMap: MutableMap<String, Double> = Gson().fromJson(prevPricesJson, typeMap) ?: mutableMapOf()

                // Calculate which base prices changed
                val oldList: List<StreamingPlatform>? = if (cachedJson != null) {
                    try {
                        val type = object : TypeToken<List<StreamingPlatform>>() {}.type
                        val list: List<StreamingPlatform> = Gson().fromJson(cachedJson, type)
                        list.filter { it.id == "prime_video" || it.id == "netflix" || it.id == "spotify" || it.id == "apple_music" }
                    } catch (e: Exception) { null }
                } else {
                    null
                }

                if (oldList != null) {
                    val oldMap = oldList.associateBy { it.id }
                    for (plat in finalFetchedList) {
                        val oldPlat = oldMap[plat.id]
                        if (oldPlat != null && oldPlat.base_price != plat.base_price) {
                            prevPricesMap[plat.id] = oldPlat.base_price
                        }
                    }
                }

                sharedPrefs.edit()
                    .putString(KEY_PRICES_JSON, Gson().toJson(finalFetchedList))
                    .putString(KEY_PREV_PRICES, Gson().toJson(prevPricesMap))
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply()

                return@withContext resolvePlatformsForUserCurrency(context, finalFetchedList)
            }

            // Fallback to local cache
            if (cachedJson != null) {
                try {
                    val type = object : TypeToken<List<StreamingPlatform>>() {}.type
                    val list: List<StreamingPlatform> = Gson().fromJson(cachedJson, type)
                    val filtered = list.filter { it.id == "prime_video" || it.id == "netflix" || it.id == "spotify" || it.id == "apple_music" }
                    return@withContext resolvePlatformsForUserCurrency(context, filtered)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback to local asset
            return@withContext getLocalFallback(context)
        }
    }

    fun getPreviousPrice(context: Context, platformId: String): Double? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevPricesJson = sharedPrefs.getString(KEY_PREV_PRICES, "{}")
        val typeMap = object : TypeToken<Map<String, Double>>() {}.type
        val prevPricesMap: Map<String, Double> = Gson().fromJson(prevPricesJson, typeMap) ?: emptyMap()
        return prevPricesMap[platformId]
    }

    fun getLastUpdateTime(context: Context): Long {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getLong(KEY_LAST_UPDATE, 0)
    }
}
