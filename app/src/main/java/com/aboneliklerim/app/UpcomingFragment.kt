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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upcoming, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvUpcoming)
        rv.layoutManager = LinearLayoutManager(requireContext())
        loadAndDisplay(rv)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val rv = view?.findViewById<RecyclerView>(R.id.rvUpcoming) ?: return
            loadAndDisplay(rv)
        }
    }

    private fun loadAndDisplay(rv: RecyclerView) {
        val subs = loadData().filter { !it.isArchived }
        val upcoming = subs.map { sub ->
            val daysUntil = calculateDaysRemaining(sub.startDate, sub.period)
            Pair(sub, daysUntil)
        }.filter { it.second in 0..7 }.sortedBy { it.second } // Sadece önümüzdeki 7 günü göster

        // Özet Bilgileri Güncelle
        val ctx = context ?: return
        val currentView = view ?: return
        val tvTotal = currentView.findViewById<TextView>(R.id.tvUpcomingTotalAmount)
        val tvCount = currentView.findViewById<TextView>(R.id.tvUpcomingCountLabel)
        
        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        
        lifecycleScope.launch {
            val rates = CurrencyService.getExchangeRates(ctx)
            var totalAmount = 0.0
            upcoming.forEach { pair ->
                val amountInTry = CurrencyService.convertToTry(pair.first.price, pair.first.currency ?: "TRY", rates)
                val targetAmount = CurrencyService.convertFromTry(amountInTry, defaultCurrency, rates)
                totalAmount += targetAmount
            }
            
            val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, ctx)
            tvTotal.text = numFmt.format(totalAmount)
            currentView.findViewById<TextView>(R.id.tvUpcomingCurrencySymbol).text = currencySymbol
            tvCount.text = getString(R.string.upcoming_count_simple, upcoming.size) // Örn: "5 Yaklaşan Ödeme"

            rv.adapter = UpcomingAdapter(upcoming, rates)
        }
    }

    private fun loadData(): List<Subscription> {
        val ctx = context ?: return emptyList()
        val json = ctx.getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }
}
