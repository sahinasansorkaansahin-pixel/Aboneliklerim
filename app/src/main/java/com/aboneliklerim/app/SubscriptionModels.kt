package com.aboneliklerim.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

data class SharedContact(
    val name: String,
    val phoneNumber: String
)

data class Subscription(
    val id: String,
    val name: String,
    val price: Double,
    val period: String,
    val periodValue: Int = 1,
    val category: String,
    val startDate: String,
    val currency: String = "TRY",
    val color: String = "#6366f1",
    val notifyDaysBefore: Int = 3,
    val notifyTime: String = "09:00",
    val note: String = "",
    val paymentMethod: String = "",
    var isArchived: Boolean = false,
    var sharedWith: Int = 1,
    var sharedContacts: List<SharedContact>? = emptyList(),
    var imagePath: String? = null
)

class SubAdapter(
    private var subs: List<Subscription>,
    private var displayCurrency: String? = null,
    private var rates: Map<String, Double> = emptyMap(),
    private val onClick: (Subscription) -> Unit
) : RecyclerView.Adapter<SubAdapter.SubViewHolder>() {

    class SubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSubName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvDaysLeft: TextView = view.findViewById(R.id.tvRemainingDays)
        val tvTags: TextView = view.findViewById(R.id.tvSubTags)
        val tvPeriod: TextView = view.findViewById(R.id.tvSubPeriod)
        val colorIndicator: View = view.findViewById(R.id.viewColor)
        val ivBell: ImageView = view.findViewById(R.id.ivNotificationBell)
        val tvSubIcon: TextView = view.findViewById(R.id.tvSubIcon)
        val ivSubLogo: ImageView = view.findViewById(R.id.ivSubLogo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_subscription, parent, false)
        return SubViewHolder(v)
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        val sub = subs[position]
        holder.tvName.text = sub.name
        holder.tvName.isSelected = true
        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val ctx = holder.itemView.context
        
        val priceToShow: Double
        val currencyToShow: String
        
        if (displayCurrency != null && rates.isNotEmpty()) {
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", rates)
            priceToShow = CurrencyService.convertFromTry(amountInTry, displayCurrency!!, rates)
            currencyToShow = displayCurrency!!
        } else {
            priceToShow = sub.price
            currencyToShow = sub.currency ?: "TRY"
        }

        val currencySymbol = CurrencyHelper.getLocalizedSymbol(currencyToShow, ctx)
        val priceStr = "${numFmt.format(priceToShow)} $currencySymbol"
        holder.tvPrice.text = priceStr

        // Fixed App Color for the indicator bar
        holder.colorIndicator.setBackgroundColor(ctx.getColor(R.color.primary_indigo))
        
        if (!sub.imagePath.isNullOrEmpty()) {
            holder.ivSubLogo.visibility = View.VISIBLE
            holder.tvSubIcon.visibility = View.GONE
            Glide.with(ctx)
                .load(sub.imagePath)
                .circleCrop()
                .into(holder.ivSubLogo)
        } else {
            holder.ivSubLogo.visibility = View.GONE
            holder.tvSubIcon.visibility = View.VISIBLE
            holder.tvSubIcon.text = sub.name.firstOrNull()?.toString()?.uppercase() ?: "?"
            try {
                // Subscription color only for the circle
                holder.tvSubIcon.background.setTint(Color.parseColor(sub.color))
            } catch (_: Exception) {
                holder.tvSubIcon.background.setTint(ctx.getColor(R.color.primary_indigo))
            }
        }

        if (sub.period == "one-time") {
            holder.tvPeriod.text = ctx.getString(R.string.one_time)
        } else {
            val periodText = when(sub.period) {
                "daily" -> ctx.getString(R.string.daily)
                "weekly" -> ctx.getString(R.string.weekly)
                "monthly" -> ctx.getString(R.string.monthly)
                "yearly" -> ctx.getString(R.string.yearly)
                else -> ctx.getString(R.string.one_time)
            }
            val typeText = ctx.getString(R.string.regular)
            val prefix = if (sub.periodValue > 1) "${sub.periodValue}x " else ""
            holder.tvPeriod.text = "$prefix$periodText • $typeText"
        }

        val days = calculateDaysRemaining(sub.startDate, sub.period, sub.periodValue)
        holder.tvDaysLeft.text = when { 
            days == 0 -> ctx.getString(R.string.payment_today)
            days == 1 -> ctx.getString(R.string.tomorrow_payment)
            days < 0 -> ctx.getString(R.string.expired)
            else -> ctx.getString(R.string.days_remaining, days)
        }
        
        holder.tvTags.text = sub.category
        // Fixed App Color for tags, bell and today text
        val appColor = ctx.getColor(R.color.primary_indigo)
        holder.tvTags.setTextColor(appColor)
        holder.tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(appColor)
        holder.tvDaysLeft.setTextColor(appColor)
        holder.ivBell.imageTintList = android.content.res.ColorStateList.valueOf(appColor)
        
        // Also tint the category text (tvPeriod) if it's considered "tag" by user
        holder.tvPeriod.setTextColor(ctx.getColor(R.color.text_secondary)) 

        holder.tvDaysLeft.visibility = if (MainActivity.showTimeRemainingGlobal) View.VISIBLE else View.GONE
        holder.tvTags.visibility = if (MainActivity.showTagsGlobal) View.VISIBLE else View.GONE
        holder.ivBell.visibility = if (sub.notifyDaysBefore >= 0) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener { onClick(sub) }
    }

    override fun getItemCount() = subs.size

    fun updateData(newSubs: List<Subscription>, targetCurrency: String? = null, currentRates: Map<String, Double> = emptyMap()) {
        subs = newSubs
        displayCurrency = targetCurrency
        rates = currentRates
        notifyDataSetChanged()
    }
}

fun getNextPaymentDate(startDateStr: String, period: String, periodValue: Int = 1, afterDate: Calendar? = null): Calendar? {
    val pv = if (periodValue < 1) 1 else periodValue
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val start = try { sdf.parse(startDateStr) } catch (e: Exception) { null } ?: return null
    
    val startCal = Calendar.getInstance().apply { time = start }
    val originalDay = startCal.get(Calendar.DAY_OF_MONTH)

    val payCal = Calendar.getInstance().apply {
        time = start
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val reference = (afterDate ?: Calendar.getInstance()).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (period == "one-time") return payCal
    if (payCal.after(reference) || payCal.equals(reference)) return payCal

    // Optimization: jump forward close to reference date
    val diffMillis = reference.timeInMillis - payCal.timeInMillis
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)

    if (diffDays > 30) {
        when (period) {
            "daily"  -> payCal.add(Calendar.DAY_OF_YEAR, (diffDays / pv).toInt() * pv)
            "weekly" -> payCal.add(Calendar.WEEK_OF_YEAR, (diffDays / (7 * pv)).toInt() * pv)
            "monthly" -> {
                val monthsToSkip = ((diffDays / 30) / pv).toInt() * pv - pv
                if (monthsToSkip > 0) {
                    payCal.add(Calendar.MONTH, monthsToSkip)
                    val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
                }
            }
            "yearly" -> {
                val yearsToSkip = ((diffDays / 365) / pv).toInt() * pv - pv
                if (yearsToSkip > 0) {
                    payCal.add(Calendar.YEAR, yearsToSkip)
                    val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
                }
            }
        }
    }

    while (payCal.before(reference)) {
        when (period) {
            "yearly" -> {
                payCal.add(Calendar.YEAR, pv)
                val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
            }
            "weekly"  -> payCal.add(Calendar.WEEK_OF_YEAR, pv)
            "daily"   -> payCal.add(Calendar.DAY_OF_YEAR, pv)
            else -> { // monthly
                payCal.add(Calendar.MONTH, pv)
                val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
            }
        }
    }
    return payCal
}

fun calculateDaysRemaining(startDate: String, period: String, periodValue: Int = 1): Int {
    val nextPay = getNextPaymentDate(startDate, period, periodValue) ?: return 0
    
    val utcNext = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, nextPay.get(Calendar.YEAR))
        set(Calendar.MONTH, nextPay.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, nextPay.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val localNow = Calendar.getInstance()
    val utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, localNow.get(Calendar.YEAR))
        set(Calendar.MONTH, localNow.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localNow.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val diffMillis = utcNext.timeInMillis - utcNow.timeInMillis
    return (diffMillis / (1000L * 60 * 60 * 24)).toInt()
}

class UpcomingAdapter(
    private val items: List<Pair<Subscription, Int>>, 
    private val rates: Map<String, Double>,
    private var displayCurrency: String? = null
) : RecyclerView.Adapter<UpcomingAdapter.UVH>() {
    class UVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvSubName)
        val tvPeriod: TextView = v.findViewById(R.id.tvSubPeriod)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val tvIcon: TextView = v.findViewById(R.id.tvSubIcon)
        val ivLogo: ImageView = v.findViewById(R.id.ivSubLogo)
        val viewColor: android.view.View = v.findViewById(R.id.viewColor)
        val tvDaysLeft: TextView = v.findViewById(R.id.tvRemainingDays)
        val ivBell: ImageView = v.findViewById(R.id.ivNotificationBell)
    }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
        UVH(android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_subscription, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: UVH, position: Int) {
        val (sub, days) = items[position]
        holder.tvName.text = sub.name
        holder.tvName.isSelected = true
        val ctx = holder.itemView.context
        
        val priceToShow: Double
        val currencyToShow: String
        
        if (displayCurrency != null && rates.isNotEmpty()) {
            val amountInTry = CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", rates)
            priceToShow = CurrencyService.convertFromTry(amountInTry, displayCurrency!!, rates)
            currencyToShow = displayCurrency!!
        } else {
            priceToShow = sub.price
            currencyToShow = sub.currency ?: "TRY"
        }

        if (sub.period == "one-time") {
            holder.tvPeriod.text = ctx.getString(R.string.one_time)
        } else {
            val periodText = when(sub.period) {
                "daily" -> ctx.getString(R.string.daily)
                "weekly" -> ctx.getString(R.string.weekly)
                "monthly" -> ctx.getString(R.string.monthly)
                "yearly" -> ctx.getString(R.string.yearly)
                else -> ctx.getString(R.string.one_time)
            }
            val typeText = ctx.getString(R.string.regular)
            val prefix = if (sub.periodValue > 1) "${sub.periodValue}x " else ""
            holder.tvPeriod.text = "$prefix$periodText • $typeText"
        }
        
        holder.tvDaysLeft.visibility = android.view.View.VISIBLE
        holder.tvDaysLeft.text = when { 
            days == 0 -> ctx.getString(R.string.payment_today)
            days == 1 -> ctx.getString(R.string.tomorrow_payment)
            else -> ctx.getString(R.string.days_remaining, days)
        }

        val tvTags: TextView = holder.itemView.findViewById(R.id.tvSubTags)
        tvTags.text = sub.category
        tvTags.visibility = if (MainActivity.showTagsGlobal) android.view.View.VISIBLE else android.view.View.GONE
        
        // Fixed App Color
        val appColor = ctx.getColor(R.color.primary_indigo)
        tvTags.setTextColor(appColor)
        tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(appColor)
        holder.tvDaysLeft.setTextColor(appColor)
        holder.viewColor.setBackgroundColor(appColor)
        holder.ivBell.imageTintList = android.content.res.ColorStateList.valueOf(appColor)
        
        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(currencyToShow, ctx)
        
        val priceStr = "${numFmt.format(priceToShow)} $currencySymbol"
        holder.tvPrice.text = priceStr

        if (!sub.imagePath.isNullOrEmpty()) {
            holder.ivLogo.visibility = android.view.View.VISIBLE
            holder.tvIcon.visibility = android.view.View.GONE
            Glide.with(ctx)
                .load(sub.imagePath)
                .circleCrop()
                .into(holder.ivLogo)
        } else {
            holder.ivLogo.visibility = android.view.View.GONE
            holder.tvIcon.visibility = android.view.View.VISIBLE
            holder.tvIcon.text = sub.name.take(1).uppercase()
            try {
                // Subscription color for circle
                holder.tvIcon.background.setTint(android.graphics.Color.parseColor(sub.color))
            } catch (_: Exception) {
                holder.tvIcon.background.setTint(appColor)
            }
        }

        holder.ivBell.visibility = if (sub.notifyDaysBefore >= 0) android.view.View.VISIBLE else android.view.View.GONE

        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(ctx, DetailActivity::class.java)
            intent.putExtra("sub_id", sub.id)
            intent.putExtra("hide_actions", true)
            if (ctx is androidx.appcompat.app.AppCompatActivity) {
                ctx.startActivityForResult(intent, 100)
            } else {
                ctx.startActivity(intent)
            }
        }
    }
}

class CategoryReportAdapter(
    private var items: List<Map.Entry<String, Double>>,
    private var total: Double,
    private var displayCurrency: String? = null
) : RecyclerView.Adapter<CategoryReportAdapter.CVH>() {

    class CVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvCatName)
        val tvAmount: TextView = v.findViewById(R.id.tvCatAmount)
        val progress: android.widget.ProgressBar = v.findViewById(R.id.pbCatProgress)
        val tvPercent: TextView = v.findViewById(R.id.tvCatPercent)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CVH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_category_report, parent, false)
        return CVH(v)
    }

    override fun onBindViewHolder(holder: CVH, position: Int) {
        val item = items[position]
        val percent = if (total > 0) (item.value / total * 100).toInt() else 0
        
        holder.tvName.text = item.key
        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val ctx = holder.itemView.context
        val lang = ctx.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = ctx.getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(displayCurrency ?: defaultCurrency, ctx)
        
        holder.tvAmount.text = "${numFmt.format(item.value)} $currencySymbol"
        holder.progress.progress = percent
        holder.tvPercent.text = "%$percent"
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Map.Entry<String, Double>>, newTotal: Double, currency: String? = null) {
        items = newItems
        total = newTotal
        displayCurrency = currency
        notifyDataSetChanged()
    }
}
