package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.aboneliklerim.app.Subscription
import com.aboneliklerim.app.CategoryReportAdapter
import com.aboneliklerim.app.AddEditActivity
import com.aboneliklerim.app.MainActivity
import com.aboneliklerim.app.UpcomingActivity
import com.aboneliklerim.app.MoreActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class ReportsActivity : BaseActivity() {

    private var reportMode = "DAILY" // DAILY, WEEKLY, MONTHLY, YEARLY
    private var subscriptions = listOf<Subscription>()
    private lateinit var categoryAdapter: CategoryReportAdapter
    private var monthlyTotal = 0.0
    private var currentRates = mapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        setupNavigation()

        subscriptions = loadData()
        fetchRatesAndCalculate()

        val cardSummary = findViewById<View>(R.id.cardReportSummary)
        cardSummary.setOnClickListener {
            reportMode = when (reportMode) {
                "DAILY" -> "WEEKLY"
                "WEEKLY" -> "MONTHLY"
                "MONTHLY" -> "YEARLY"
                else -> "DAILY"
            }
            updateUI()
        }

        val rvCategories = findViewById<RecyclerView>(R.id.rvCategoryBreakdown)
        rvCategories.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryReportAdapter(mutableListOf<Map.Entry<String, Double>>(), 0.0)
        rvCategories.adapter = categoryAdapter

        updateUI()
    }

    private fun calculateBaseTotals() {
        val defaultCurrency = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(LocaleHelper.getActiveLanguage(this))) ?: "TRY"
        monthlyTotal = 0.0
        subscriptions.forEach { sub ->
            val price = sub.price
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val monthly = when (sub.period) {
                "yearly"   -> price / (12.0 * pv)
                "weekly"   -> price * (4.33 / pv)
                "daily"    -> price * (30.0 / pv)
                "one-time" -> 0.0
                else       -> price / pv  // monthly
            }
            if (monthly > 0) {
                val amountInTry = CurrencyService.convertToTry(monthly, sub.currency ?: "TRY", currentRates)
                val targetAmount = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
                monthlyTotal += targetAmount
            }
        }
    }

    private fun updateUI() {
        val tvTotal = findViewById<TextView>(R.id.tvReportTotalAmount)
        val tvPeriodLabel = findViewById<TextView>(R.id.tvReportPeriodLabel)
        val tvSubCount = findViewById<TextView>(R.id.tvReportSubCount)
        val tvMostExpensive = findViewById<TextView>(R.id.tvReportMostExpensive)
        val scrollMostExpensive = findViewById<View>(R.id.scrollMostExpensive)
        
        scrollMostExpensive.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        val defaultCurrency = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(LocaleHelper.getActiveLanguage(this))) ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, this)

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
        tvTotal.text = numFmt.format(displayTotal)
        findViewById<TextView>(R.id.tvReportCurrencySymbol).text = currencySymbol
        tvPeriodLabel.text = when (reportMode) {
            "DAILY" -> getString(R.string.daily_upper)
            "WEEKLY" -> getString(R.string.weekly_upper)
            "MONTHLY" -> getString(R.string.monthly_upper)
            else -> getString(R.string.yearly_upper)
        }
        tvSubCount.text = subscriptions.size.toString()

        // Most expensive subscription normalized to default currency
        val most = subscriptions.maxByOrNull { sub ->
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", currentRates)
            CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
        }
        tvMostExpensive.text = most?.let { 
            val amountInTry = CurrencyService.convertToTry(it.price, it.currency ?: "TRY", currentRates)
            val convertedPrice = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
            "${it.name}: ${numFmt.format(convertedPrice)} $currencySymbol"
        } ?: "-"

        // Update Categories
        val categoryMap = mutableMapOf<String, Double>()
        subscriptions.forEach { sub ->
            val price = sub.price
            val pv = sub.periodValue.coerceAtLeast(1).toDouble()
            val monthly = when (sub.period) {
                "yearly"   -> price / (12.0 * pv)
                "weekly"   -> price * (4.33 / pv)
                "daily"    -> price * (30.0 / pv)
                "one-time" -> 0.0
                else       -> price / pv  // monthly
            }
            if (monthly > 0) {
                val amountInTry = CurrencyService.convertToTry(monthly, sub.currency ?: "TRY", currentRates)
                val targetAmount = CurrencyService.convertFromTry(amountInTry, defaultCurrency, currentRates)
                val amount = when (reportMode) {
                    "DAILY" -> targetAmount / 30.0
                    "YEARLY" -> targetAmount * 12.0
                    "WEEKLY" -> targetAmount / 4.33
                    else -> targetAmount
                }
                categoryMap[sub.category] = (categoryMap[sub.category] ?: 0.0) + amount
            }
        }

        val categoryList = categoryMap.entries.sortedByDescending { it.value }
        categoryAdapter.updateData(categoryList, displayTotal)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_reports
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_upcoming -> {
                    startActivity(Intent(this, UpcomingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_reports -> true
                R.id.nav_more -> {
                    startActivity(Intent(this, MoreActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
    }

    private fun fetchRatesAndCalculate() {
        lifecycleScope.launch {
            try {
                currentRates = CurrencyService.getExchangeRates(this@ReportsActivity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            calculateBaseTotals()
            updateUI()
        }
    }

    private fun loadData(): List<Subscription> {
        val json = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }
}

