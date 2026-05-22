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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ReportsFragment : Fragment() {

    private var reportMode = "DAILY" // DAILY, WEEKLY, MONTHLY, YEARLY
    private var subscriptions = listOf<Subscription>()
    private lateinit var categoryAdapter: CategoryReportAdapter
    private var monthlyTotal = 0.0
    private var currentRates = mapOf<String, Double>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscriptions = loadData()
        
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategoryBreakdown)
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CategoryReportAdapter(mutableListOf(), 0.0)
        rvCategories.adapter = categoryAdapter

        fetchRatesAndCalculate()

        val cardSummary = view.findViewById<View>(R.id.cardReportSummary)
        cardSummary.setOnClickListener {
            reportMode = when (reportMode) {
                "DAILY" -> "WEEKLY"
                "WEEKLY" -> "MONTHLY"
                "MONTHLY" -> "YEARLY"
                else -> "DAILY"
            }
            updateUI()
        }
    }

    private fun fetchRatesAndCalculate() {
        val ctx = context ?: return
        lifecycleScope.launch {
            try {
                currentRates = CurrencyService.getExchangeRates(ctx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            calculateBaseTotals()
            updateUI()
        }
    }

    private fun calculateBaseTotals() {
        val ctx = context ?: return
        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
            
        monthlyTotal = 0.0
        subscriptions.filter { !it.isArchived }.forEach { sub ->
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", currentRates)
            val price = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val monthly = when (sub.period) {
                "yearly"   -> price / (12.0 * pv)
                "weekly"   -> price * (4.33 / pv)
                "daily"    -> price * (30.0 / pv)
                "one-time" -> price
                else       -> price / pv
            }
            monthlyTotal += monthly
        }
    }

    private fun updateUI() {
        val view = view ?: return
        val ctx = context ?: return
        val tvTotal = view.findViewById<TextView>(R.id.tvReportTotalAmount)
        val tvPeriodLabel = view.findViewById<TextView>(R.id.tvReportPeriodLabel)
        val tvSubCount = view.findViewById<TextView>(R.id.tvReportSubCount)
        val tvMostExpensive = view.findViewById<TextView>(R.id.tvReportMostExpensive)

        val displayTotal = when (reportMode) {
            "DAILY" -> monthlyTotal / 30.0
            "YEARLY" -> monthlyTotal * 12.0
            "WEEKLY" -> monthlyTotal / 4.33
            else -> monthlyTotal
        }

        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, ctx)

        tvTotal.text = "${numFmt.format(displayTotal)}$currencySymbol"
        tvPeriodLabel.text = when(reportMode) {
            "DAILY" -> getString(R.string.daily_upper)
            "WEEKLY" -> getString(R.string.weekly_upper)
            "MONTHLY" -> getString(R.string.monthly_upper)
            else -> getString(R.string.yearly_upper)
        }
        
        val activeSubs = subscriptions.filter { !it.isArchived }
        tvSubCount.text = activeSubs.size.toString()

        val most = activeSubs.maxByOrNull { sub ->
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", currentRates)
            val priceNorm = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
            when (sub.period) {
                "yearly"  -> priceNorm / (12.0 * pv)
                "weekly"  -> priceNorm * (4.33 / pv)
                "daily"   -> priceNorm * (30.0 / pv)
                else      -> priceNorm / pv
            }
        }
        tvMostExpensive.text = most?.let { sub ->
            val itSym = CurrencyHelper.getLocalizedSymbol(sub.currency ?: "TRY", ctx)
            "${sub.name}: ${numFmt.format(sub.price)} $itSym"
        } ?: "-"
        tvMostExpensive.isSelected = true
        tvMostExpensive.setSingleLine(true)

        val categoryMap = mutableMapOf<String, Double>()
        subscriptions.filter { !it.isArchived }.forEach { sub ->
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", currentRates)
            val price = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val monthly = when (sub.period) {
                "yearly"   -> price / (12.0 * pv)
                "weekly"   -> price * (4.33 / pv)
                "daily"    -> price * (30.0 / pv)
                "one-time" -> price
                else       -> price / pv
            }
            val amount = when (reportMode) {
                "DAILY" -> monthly / 30.0
                "YEARLY" -> monthly * 12.0
                "WEEKLY" -> monthly / 4.33
                else -> monthly
            }
            categoryMap[sub.category] = (categoryMap[sub.category] ?: 0.0) + amount
        }

        val categoryList = categoryMap.entries.sortedByDescending { it.value }
        categoryAdapter.updateData(categoryList, displayTotal)

        val cardCategoryBreakdown = view.findViewById<View>(R.id.cardCategoryBreakdown)
        cardCategoryBreakdown.visibility = if (categoryList.isEmpty()) View.GONE else View.VISIBLE

        updateTrendUI(view)
    }

    private fun updateTrendUI(view: View) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, ctx)

        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonthIndex = calendar.get(java.util.Calendar.MONTH)

        // Find oldest active subscription start date
        var oldestYear = currentYear
        var oldestMonthIndex = currentMonthIndex
        subscriptions.filter { !it.isArchived }.forEach { sub ->
            try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val date = parser.parse(sub.startDate ?: "")
                if (date != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    val y = cal.get(java.util.Calendar.YEAR)
                    val m = cal.get(java.util.Calendar.MONTH)
                    if (y < oldestYear || (y == oldestYear && m < oldestMonthIndex)) {
                        oldestYear = y; oldestMonthIndex = m
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }

        val totalMonths = (currentYear - oldestYear) * 12 + (currentMonthIndex - oldestMonthIndex) + 1
        val monthsToShow = (if (totalMonths < 12) 12 else totalMonths).coerceAtMost(36)

        val monthValues = DoubleArray(monthsToShow)
        val monthNames  = Array(monthsToShow) { "" }
        val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())

        for (i in 0 until monthsToShow) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -(monthsToShow - 1 - i))
            monthValues[i] = calculateSpendingForMonth(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), defaultCurrency)
            monthNames[i]  = monthFormat.format(cal.time)
        }

        val chartView = view.findViewById<SpendingTrendChartView>(R.id.trendChartView)
        chartView?.setData(monthValues, monthNames, currencySymbol)
    }

    private fun calculateSpendingForMonth(year: Int, monthIndex: Int, defaultCurrency: String): Double {
        var total = 0.0
        val calEnd = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, monthIndex)
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }
        val endLimitTime = calEnd.timeInMillis

        subscriptions.filter { !it.isArchived }.forEach { sub ->
            val startMillis = try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                parser.parse(sub.startDate ?: "")?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (startMillis > endLimitTime) return@forEach

            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", currentRates)
            val price = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val monthValue = when (sub.period) {
                "yearly"   -> price / (12.0 * pv)
                "weekly"   -> price * (4.33 / pv)
                "daily"    -> price * (30.0 / pv)
                "one-time" -> {
                    val startCal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
                    if (startCal.get(java.util.Calendar.YEAR) == year && startCal.get(java.util.Calendar.MONTH) == monthIndex) {
                        price
                    } else {
                        0.0
                    }
                }
                else -> price / pv
            }
            total += monthValue
        }
        return total
    }

    private fun loadData(): List<Subscription> {
        val ctx = context ?: return emptyList()
        val json = ctx.getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }

    override fun onResume() {
        super.onResume()
        subscriptions = loadData()
        fetchRatesAndCalculate()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            subscriptions = loadData()
            fetchRatesAndCalculate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
