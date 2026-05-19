package com.aboneliklerim.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
    var sharedContacts: List<SharedContact>? = emptyList()
)

class SubAdapter(
    private var subs: List<Subscription>,
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
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(sub.currency ?: "TRY", ctx)
        
        // Dinamik Kur Karşılığı (Sağ taraftaki sembollü fiyat için)
        val priceStr = "${numFmt.format(sub.price)} $currencySymbol"
        holder.tvPrice.text = priceStr

        try {
            val subColor = Color.parseColor(sub.color)
            holder.colorIndicator.setBackgroundColor(subColor)
        } catch (_: Exception) {
            holder.colorIndicator.setBackgroundColor(Color.parseColor("#6366f1"))
        }
        
        val tvSubIcon: TextView = holder.itemView.findViewById(R.id.tvSubIcon)
        tvSubIcon.text = sub.name.firstOrNull()?.toString()?.uppercase() ?: "?"
        try {
            tvSubIcon.background.setTint(Color.parseColor(sub.color))
        } catch (_: Exception) {}

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
            holder.tvPeriod.text = "$periodText • $typeText"
        }

        val days = calculateDaysRemaining(sub.startDate, sub.period)
        holder.tvDaysLeft.text = when { 
            days == 0 -> ctx.getString(R.string.today_payment)
            days == 1 -> ctx.getString(R.string.tomorrow_payment)
            days < 0 -> ctx.getString(R.string.expired)
            else -> ctx.getString(R.string.days_remaining, days)
        }
        
        holder.tvTags.text = sub.category
        try {
            val subColor = Color.parseColor(sub.color)
            holder.tvTags.setTextColor(subColor)
            holder.tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(subColor)
        } catch (_: Exception) {
            val fallback = Color.parseColor("#6366f1")
            holder.tvTags.setTextColor(fallback)
            holder.tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(fallback)
        }
        
        holder.tvDaysLeft.visibility = if (MainActivity.showTimeRemainingGlobal) View.VISIBLE else View.GONE
        holder.tvTags.visibility = if (MainActivity.showTagsGlobal) View.VISIBLE else View.GONE
        holder.ivBell.visibility = if (sub.notifyDaysBefore >= 0) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener { onClick(sub) }
    }

    override fun getItemCount() = subs.size

    fun updateData(newSubs: List<Subscription>) {
        subs = newSubs
        notifyDataSetChanged()
    }
}

fun getNextPaymentDate(startDateStr: String, period: String, afterDate: Calendar? = null): Calendar? {
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

    // Optimization: jump forward close to reference date, but keep safety margin to avoid overshoot
    val diffMillis = reference.timeInMillis - payCal.timeInMillis
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)

    if (diffDays > 30) {
        when (period) {
            "daily" -> payCal.add(Calendar.DAY_OF_YEAR, diffDays.toInt())
            "weekly" -> payCal.add(Calendar.WEEK_OF_YEAR, (diffDays / 7).toInt())
            "monthly" -> {
                val monthsToSkip = (diffDays / 30).toInt() - 1
                if (monthsToSkip > 0) {
                    payCal.add(Calendar.MONTH, monthsToSkip)
                    val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
                }
            }
            "yearly" -> {
                val yearsToSkip = (diffDays / 365).toInt() - 1
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
                payCal.add(Calendar.YEAR, 1)
                val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
            }
            "weekly" -> payCal.add(Calendar.WEEK_OF_YEAR, 1)
            "daily" -> payCal.add(Calendar.DAY_OF_YEAR, 1)
            else -> { // monthly
                payCal.add(Calendar.MONTH, 1)
                val maxDay = payCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                payCal.set(Calendar.DAY_OF_MONTH, Math.min(originalDay, maxDay))
            }
        }
    }
    return payCal
}

fun calculateDaysRemaining(startDate: String, period: String): Int {
    val nextPay = getNextPaymentDate(startDate, period) ?: return 0
    
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

class UpcomingAdapter(private val items: List<Pair<Subscription, Int>>, private val rates: Map<String, Double>) : RecyclerView.Adapter<UpcomingAdapter.UVH>() {
    class UVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvSubName)
        val tvPeriod: TextView = v.findViewById(R.id.tvSubPeriod)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val tvIcon: TextView = v.findViewById(R.id.tvSubIcon)
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
            holder.tvPeriod.text = "$periodText • $typeText"
        }
        
        holder.tvDaysLeft.visibility = android.view.View.VISIBLE
        holder.tvDaysLeft.text = when { 
            days == 0 -> ctx.getString(R.string.today_payment)
            days == 1 -> ctx.getString(R.string.tomorrow_payment)
            else -> ctx.getString(R.string.days_remaining, days)
        }

        val tvTags: TextView = holder.itemView.findViewById(R.id.tvSubTags)
        tvTags.text = sub.category
        tvTags.visibility = if (MainActivity.showTagsGlobal) android.view.View.VISIBLE else android.view.View.GONE
        try {
            val subColor = android.graphics.Color.parseColor(sub.color)
            tvTags.setTextColor(subColor)
            tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(subColor)
        } catch (_: Exception) {
            val fallback = android.graphics.Color.parseColor("#6366f1")
            tvTags.setTextColor(fallback)
            tvTags.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(fallback)
        }
        
        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(sub.currency ?: "TRY", ctx)
        
        val priceStr = "${numFmt.format(sub.price)} $currencySymbol"
        holder.tvPrice.text = priceStr

        holder.tvIcon.text = sub.name.take(1).uppercase()
        try { 
            val color = android.graphics.Color.parseColor(sub.color)
            holder.viewColor.setBackgroundColor(color)
            holder.tvIcon.background.setTint(color)
            holder.tvDaysLeft.setTextColor(color)
            holder.ivBell.visibility = if (sub.notifyDaysBefore >= 0) android.view.View.VISIBLE else android.view.View.GONE
            holder.ivBell.imageTintList = android.content.res.ColorStateList.valueOf(color)
        } catch (_: Exception) {}

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
    private var total: Double
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
        val lang = holder.itemView.context.resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = holder.itemView.context.getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, holder.itemView.context)
        
        holder.tvAmount.text = "${numFmt.format(item.value)} $currencySymbol"
        holder.progress.progress = percent
        holder.tvPercent.text = "%$percent"
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Map.Entry<String, Double>>, newTotal: Double) {
        items = newItems
        total = newTotal
        notifyDataSetChanged()
    }
}
