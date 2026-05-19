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
    private var trendHandler: android.os.Handler? = null
    private var trendRunnable: Runnable? = null

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
            
            val monthly = when (sub.period) {
                "yearly" -> price / 12.0
                "weekly" -> price * 4.33
                "daily" -> price * 30.0
                "one-time" -> price
                else -> price
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
            when (sub.period) {
                "yearly" -> sub.price
                "monthly" -> sub.price * 12
                "weekly" -> sub.price * 52
                "daily" -> sub.price * 365
                else -> sub.price
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
            
            val monthly = when (sub.period) {
                "yearly" -> price / 12.0
                "weekly" -> price * 4.33
                "daily" -> price * 30.0
                "one-time" -> price
                else -> price
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
        val isPremium = prefs.getBoolean("is_premium_active", false)
        
        val overlay = view.findViewById<View>(R.id.layoutTrendPremiumOverlay)
        val hsvTrendView = view.findViewById<View>(R.id.hsvTrend)
        
        if (!isPremium) {
            // Apply blur on Android 12+, alpha fade on older devices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                hsvTrendView?.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                )
            } else {
                hsvTrendView?.alpha = 0.15f
            }
            hsvTrendView?.visibility = View.VISIBLE
            overlay?.visibility = View.VISIBLE
            overlay?.setOnClickListener {
                startActivity(android.content.Intent(ctx, PremiumActivity::class.java))
            }
            return
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                hsvTrendView?.setRenderEffect(null)
            } else {
                hsvTrendView?.alpha = 1f
            }
            hsvTrendView?.visibility = View.VISIBLE
            overlay?.visibility = View.GONE
        }

        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, ctx)

        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonthIndex = calendar.get(java.util.Calendar.MONTH) // 0-11

        // 1. Find the oldest start date among active subscriptions
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
                        oldestYear = y
                        oldestMonthIndex = m
                    }
                }
            } catch (e: Exception) {}
        }

        // Calculate months between oldest start date and current month
        val totalMonths = (currentYear - oldestYear) * 12 + (currentMonthIndex - oldestMonthIndex) + 1
        val monthsToShow = if (totalMonths < 12) 12 else totalMonths
        // Safe cap to avoid populating too many months
        val safeMonthsToShow = if (monthsToShow > 36) 36 else monthsToShow

        val monthValues = DoubleArray(safeMonthsToShow)
        val monthNames = Array(safeMonthsToShow) { "" }

        val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())

        for (i in 0 until safeMonthsToShow) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -(safeMonthsToShow - 1 - i))
            val year = cal.get(java.util.Calendar.YEAR)
            val monthIdx = cal.get(java.util.Calendar.MONTH)
            
            monthValues[i] = calculateSpendingForMonth(year, monthIdx, defaultCurrency)
            monthNames[i] = monthFormat.format(cal.time)
        }

        val maxVal = monthValues.maxOrNull() ?: 1.0
        val safeMax = if (maxVal <= 0.0) 1.0 else maxVal

        val container = view.findViewById<android.widget.LinearLayout>(R.id.llTrendBarsContainer)
        if (container != null) {
            container.removeAllViews()
            val inflater = android.view.LayoutInflater.from(ctx)
            
            for (i in 0 until safeMonthsToShow) {
                val barView = inflater.inflate(R.layout.item_trend_bar, container, false)
                val tvVal = barView.findViewById<TextView>(R.id.tvTrendVal)
                val vBar = barView.findViewById<View>(R.id.barTrend)
                val tvMonth = barView.findViewById<TextView>(R.id.tvTrendMonth)

                val value = monthValues[i]
                val monthName = monthNames[i]

                val numFmtTrend = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 0
                }

                tvVal.text = "${numFmtTrend.format(value)}$currencySymbol"
                tvMonth.text = monthName

                val maxBarHeightPx = (80 * ctx.resources.displayMetrics.density).toInt()
                val heightPx = ((value / safeMax) * maxBarHeightPx).toInt()
                val minHeightPx = (4 * ctx.resources.displayMetrics.density).toInt()
                val safeHeightPx = if (heightPx < minHeightPx) minHeightPx else heightPx

                val lp = vBar.layoutParams
                lp.height = safeHeightPx
                vBar.layoutParams = lp

                container.addView(barView)
            }
        }

        // Scroll to the end (most recent month) dynamically
        val hsvTrend = view.findViewById<android.widget.HorizontalScrollView>(R.id.hsvTrend)
        if (hsvTrend != null) {
            hsvTrend.post {
                hsvTrend.fullScroll(View.FOCUS_RIGHT)
            }
        }

        val hintView = view.findViewById<android.widget.ImageView>(R.id.ivSwipeHintTrend)
        if (hintView != null) {
            trendHandler?.removeCallbacksAndMessages(null)
            
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            trendHandler = handler
            
            val swipeRunnable = object : Runnable {
                override fun run() {
                    val ctx = context ?: return
                    val hsv = hsvTrend ?: return
                    
                    // Determine if scrolled all the way to the left (start of history)
                    val isFarLeft = hsv.scrollX < 30
                    
                    if (isFarLeft) {
                        // Point Right to guide user back to the present/future
                        hintView.setImageResource(R.drawable.ic_swipe_horizontal_right)
                        hintView.alpha = 0f
                        hintView.translationX = -40f
                        hintView.visibility = View.VISIBLE
                        
                        hintView.animate()
                            .alpha(0.6f)
                            .translationX(40f)
                            .setDuration(1200)
                            .withEndAction {
                                hintView.animate()
                                    .alpha(0f)
                                    .setDuration(400)
                                    .withEndAction {
                                        handler.postDelayed(this, 4000)
                                    }
                                    .start()
                            }
                            .start()
                    } else {
                        // Point Left to guide user to check past history
                        hintView.setImageResource(R.drawable.ic_swipe_horizontal)
                        hintView.alpha = 0f
                        hintView.translationX = 40f
                        hintView.visibility = View.VISIBLE
                        
                        hintView.animate()
                            .alpha(0.6f)
                            .translationX(-40f)
                            .setDuration(1200)
                            .withEndAction {
                                hintView.animate()
                                    .alpha(0f)
                                    .setDuration(400)
                                    .withEndAction {
                                        handler.postDelayed(this, 4000)
                                    }
                                    .start()
                            }
                            .start()
                    }
                }
            }
            trendRunnable = swipeRunnable
            
            // Start the hint animation
            handler.postDelayed(swipeRunnable, 1500)

            // Hide the hint during scroll, but show again after 3 seconds of scroll inactivity (idle)
            if (hsvTrend != null) {
                hsvTrend.setOnScrollChangeListener { _, _, _, _, _ ->
                    // Remove pending callbacks and cancel ongoing animation immediately
                    handler.removeCallbacks(swipeRunnable)
                    hintView.animate().cancel()
                    hintView.alpha = 0f
                    hintView.visibility = View.GONE
                    
                    // Schedule to show again after 3 seconds of idle/no scrolling
                    handler.postDelayed(swipeRunnable, 3000)
                }
            }
        }
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

            val monthValue = when (sub.period) {
                "yearly" -> price / 12.0
                "weekly" -> price * 4.33
                "daily" -> price * 30.0
                "one-time" -> {
                    val startCal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
                    if (startCal.get(java.util.Calendar.YEAR) == year && startCal.get(java.util.Calendar.MONTH) == monthIndex) {
                        price
                    } else {
                        0.0
                    }
                }
                else -> price
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
        trendHandler?.removeCallbacksAndMessages(null)
        trendHandler = null
        trendRunnable = null
        super.onDestroyView()
    }
}
