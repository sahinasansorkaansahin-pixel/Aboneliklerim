package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StreamingPricesActivity : BaseActivity() {

    private lateinit var rvPlatforms: RecyclerView
    private lateinit var tvLastUpdate: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var loadingOverlay: View
    private lateinit var btnRefresh: ImageButton

    private var targetCurrency = "TRY"
    private var activeLang = "en"
    private var exchangeRates = mapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming_prices)

        // Read preferences
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        activeLang = LocaleHelper.getActiveLanguage(this)
        targetCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)) ?: "TRY"

        // Initialize UI components
        rvPlatforms = findViewById<RecyclerView>(R.id.rvStreamingPlatforms)
        tvLastUpdate = findViewById<TextView>(R.id.tvLastUpdate)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        btnRefresh = findViewById<ImageButton>(R.id.btnRefresh)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnRefresh.setOnClickListener {
            refreshPrices(force = true)
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshPrices(force = true)
        }

        rvPlatforms.layoutManager = LinearLayoutManager(this)

        // Load initial data
        refreshPrices(force = false)
    }

    private fun refreshPrices(force: Boolean) {
        lifecycleScope.launch {
            if (force) {
                loadingOverlay.visibility = View.VISIBLE
            }
            swipeRefreshLayout.isRefreshing = true

            try {
                // Get exchange rates
                exchangeRates = CurrencyService.getExchangeRates(this@StreamingPricesActivity)

                // Sync pricing list
                val platforms = StreamingPriceService.fetchAndSyncPrices(this@StreamingPricesActivity, forceRefresh = force)

                if (platforms.isNotEmpty()) {
                    bindData(platforms)
                    if (force) {
                        Toast.makeText(this@StreamingPricesActivity, getString(R.string.update_prices_success), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (force) {
                        Toast.makeText(this@StreamingPricesActivity, getString(R.string.update_prices_error), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (force) {
                    Toast.makeText(this@StreamingPricesActivity, getString(R.string.update_prices_error), Toast.LENGTH_SHORT).show()
                }
            } finally {
                swipeRefreshLayout.isRefreshing = false
                loadingOverlay.visibility = View.GONE
                updateLastUpdateText()
            }
        }
    }

    private fun bindData(platforms: List<StreamingPriceService.StreamingPlatform>) {
        rvPlatforms.adapter = StreamingPlatformAdapter(this, platforms, targetCurrency, exchangeRates, activeLang)
    }

    private fun updateLastUpdateText() {
        val lastUpdateMillis = StreamingPriceService.getLastUpdateTime(this)
        if (lastUpdateMillis > 0) {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(lastUpdateMillis))
            tvLastUpdate.text = getString(R.string.last_update, dateStr)
        } else {
            tvLastUpdate.text = ""
        }
    }

    class StreamingPlatformAdapter(
        private val context: Context,
        private val items: List<StreamingPriceService.StreamingPlatform>,
        private val targetCurrency: String,
        private val rates: Map<String, Double>,
        private val activeLang: String
    ) : RecyclerView.Adapter<StreamingPlatformAdapter.PlatformViewHolder>() {

        class PlatformViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgLogo: ImageView = view.findViewById(R.id.imgPlatformLogo)
            val tvName: TextView = view.findViewById(R.id.tvPlatformName)
            val tvTrendBadge: TextView = view.findViewById(R.id.tvTrendBadge)
            val plansContainer: LinearLayout = view.findViewById(R.id.plansContainer)
            val btnOpenWeb: View = view.findViewById(R.id.btnOpenWeb)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
            val v = LayoutInflater.from(context).inflate(R.layout.item_streaming_platform, parent, false)
            return PlatformViewHolder(v)
        }

        override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name

            // Load Logo dynamically from resources
            val logoResId = context.resources.getIdentifier(item.logo_res, "drawable", context.packageName)
            if (logoResId != 0) {
                holder.imgLogo.setImageResource(logoResId)
            } else {
                holder.imgLogo.setImageResource(android.R.drawable.ic_menu_slideshow)
            }

            // Calculate price trend / badge
            val prevPrice = StreamingPriceService.getPreviousPrice(context, item.id)
            if (prevPrice != null && prevPrice > 0.0) {
                val changePercent = ((item.base_price - prevPrice) / prevPrice) * 100
                if (changePercent > 0.05) {
                    holder.tvTrendBadge.text = context.getString(R.string.price_increased, String.format(Locale.US, "%.0f%%", changePercent))
                    holder.tvTrendBadge.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                    holder.tvTrendBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEAEA")) // Light Red background
                    holder.tvTrendBadge.setTextColor(android.graphics.Color.parseColor("#FF5252")) // Red text
                    holder.tvTrendBadge.visibility = View.VISIBLE
                } else if (changePercent < -0.05) {
                    holder.tvTrendBadge.text = context.getString(R.string.price_decreased, String.format(Locale.US, "%.0f%%", Math.abs(changePercent)))
                    holder.tvTrendBadge.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                    holder.tvTrendBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAFBEA")) // Light Green background
                    holder.tvTrendBadge.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green text
                    holder.tvTrendBadge.visibility = View.VISIBLE
                } else {
                    holder.tvTrendBadge.text = context.getString(R.string.stable)
                    holder.tvTrendBadge.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                    holder.tvTrendBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F3F4F6")) // Light Grey background
                    holder.tvTrendBadge.setTextColor(android.graphics.Color.parseColor("#6B7280")) // Grey text
                    holder.tvTrendBadge.visibility = View.VISIBLE
                }
            } else {
                holder.tvTrendBadge.text = context.getString(R.string.stable)
                holder.tvTrendBadge.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                holder.tvTrendBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F3F4F6")) // Light Grey background
                holder.tvTrendBadge.setTextColor(android.graphics.Color.parseColor("#6B7280")) // Grey text
                holder.tvTrendBadge.visibility = View.VISIBLE
            }

            // Populate Plans programmatically
            holder.plansContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)
            val symbol = CurrencyHelper.getLocalizedSymbol(targetCurrency, context)

            for (plan in item.plans) {
                val planRow = inflater.inflate(R.layout.item_streaming_plan_row, holder.plansContainer, false)
                val tvPlanName = planRow.findViewById<TextView>(R.id.tvPlanName)
                val tvPlanPrice = planRow.findViewById<TextView>(R.id.tvPlanPrice)
                val tvPlanPeriod = planRow.findViewById<TextView>(R.id.tvPlanPeriod)

                tvPlanName.text = plan.name

                // Convert price
                val amountInTry = CurrencyService.convertToTry(plan.price, plan.currency, rates)
                val targetAmount = CurrencyService.convertFromTry(amountInTry, targetCurrency, rates)
                
                // Format price
                val formattedPrice = String.format(Locale.US, "%,.2f", targetAmount)
                tvPlanPrice.text = "$formattedPrice $symbol"

                // Format Period
                tvPlanPeriod.text = when (plan.period) {
                    "monthly" -> if (activeLang.startsWith("tr")) "/ay" else "/mo"
                    "yearly" -> if (activeLang.startsWith("tr")) "/yıl" else "/yr"
                    else -> "/${plan.period}"
                }

                holder.plansContainer.addView(planRow)
            }

            // Web link click
            holder.btnOpenWeb.setOnClickListener {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.official_url))
                    context.startActivity(browserIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
