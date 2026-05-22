package com.aboneliklerim.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class UpcomingFragment : Fragment() {

    private var subscriptions = listOf<Subscription>()
    private var pulseHandler: android.os.Handler? = null
    private var pulseRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upcoming, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvUpcoming)
        rv.layoutManager = LinearLayoutManager(requireContext())

        refreshData(rv)
    }

    private fun refreshData(rv: RecyclerView) {
        lifecycleScope.launch {
            loadDataAsync()
            loadAndDisplay(rv)
        }
    }

    private suspend fun loadDataAsync() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val json = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
            subscriptions = if (json != null) Gson().fromJson(json, object : TypeToken<MutableList<Subscription>>() {}.type) else emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden); if (!hidden) { val rv = view?.findViewById<RecyclerView>(R.id.rvUpcoming) ; if (rv != null) refreshData(rv) }
    }

    private fun loadAndDisplay(rv: RecyclerView) {
        lifecycleScope.launch {
            val ctx = context ?: return@launch
            val prefs = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val isPremium = prefs.getBoolean("is_premium_active", false)

            val upcoming = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                var base = subscriptions.filter { !it.isArchived }.map { sub ->
                    val daysUntil = calculateDaysRemaining(sub.startDate, sub.period, sub.periodValue)
                    Pair(sub, daysUntil)
                }.filter { it.second in 0..7 }
                
                base.sortedBy { it.second }
            }

            val currentView = view ?: return@launch
            val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
            val defaultCurrencySetting = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
            
            val displayCurrency = if (MainActivity.selectedDisplayCurrencyGlobal != null) MainActivity.selectedDisplayCurrencyGlobal!! else {
                defaultCurrencySetting
            }

            val rates = CurrencyService.getExchangeRates(ctx)
            val totalAmount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                var sum = 0.0
                upcoming.forEach { pair ->
                    val amountInTry = CurrencyService.convertToTry(pair.first.price, pair.first.currency, rates)
                    sum += CurrencyService.convertFromTry(amountInTry, displayCurrency, rates)
                }
                sum
            }
            
            val numFmt = java.text.NumberFormat.getInstance(Locale.getDefault()).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
            currentView.findViewById<TextView>(R.id.tvUpcomingTotalAmount).text = numFmt.format(totalAmount)
            currentView.findViewById<TextView>(R.id.tvUpcomingCurrencySymbol).text = CurrencyHelper.getLocalizedSymbol(displayCurrency, ctx)
            currentView.findViewById<TextView>(R.id.tvUpcomingCountLabel).text = getString(R.string.upcoming_count_simple, upcoming.size)

            val finalTargetCurrency = if (MainActivity.selectedDisplayCurrencyGlobal == null) defaultCurrencySetting else MainActivity.selectedDisplayCurrencyGlobal
            rv.adapter = UpcomingAdapter(upcoming, rates, finalTargetCurrency)
        }
    }
}
