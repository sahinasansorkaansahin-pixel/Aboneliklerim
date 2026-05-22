package com.aboneliklerim.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SubscriptionWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, SubscriptionWidget::class.java))
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, SubscriptionWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
            
            try {
                val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                val isWidgetEnabled = prefs.getBoolean("widget_enabled", true)
                val showConvertedPrices = prefs.getBoolean("show_converted_prices", false)
                val defaultCurrency = prefs.getString("default_currency", "TRY") ?: "TRY"
                val showTimeRemaining = prefs.getBoolean("show_time_remaining", true)
                val showTags = prefs.getBoolean("show_tags", true)
                
                if (!isWidgetEnabled) {
                    views.setViewVisibility(R.id.layoutWidgetContainer, View.GONE)
                    views.setViewVisibility(R.id.tvWidgetEmpty, View.VISIBLE)
                    views.setTextViewText(R.id.tvWidgetEmpty, context.getString(R.string.widget_status_off))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                val json = context.getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
                val type = object : TypeToken<MutableList<Subscription>>() {}.type
                val subs: List<Subscription> = if (json != null) Gson().fromJson(json, type) else emptyList()
                
                if (subs.isEmpty()) {
                    views.setViewVisibility(R.id.layoutWidgetContainer, View.GONE)
                    views.setViewVisibility(R.id.tvWidgetEmpty, View.VISIBLE)
                    views.setTextViewText(R.id.tvWidgetEmpty, context.getString(R.string.no_subs_at_all))
                } else {
                    val activeSubs = subs.filter { !it.isArchived && it.period != "one-time" }
                    val nearestSub = activeSubs.mapNotNull { sub ->
                        val daysUntil = calculateDaysRemaining(sub.startDate, sub.period, sub.periodValue)
                        if (daysUntil >= 0) sub to daysUntil else null
                    }.minByOrNull { it.second }?.first

                    if (nearestSub != null) {
                        views.setViewVisibility(R.id.layoutWidgetContainer, View.VISIBLE)
                        views.setViewVisibility(R.id.tvWidgetEmpty, View.GONE)

                        // Theme handling for widget
                        val themeMode = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("theme_mode", "smart")
                        val isDark = when(themeMode) {
                            "light" -> false
                            "dark" -> true
                            "system" -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                            else -> { // smart
                                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                hour >= 19 || hour < 7
                            }
                        }

                        val textColorMain = if (isDark) "#FFFFFF" else "#1A1A1A"
                        val textColorSecondary = if (isDark) "#8E8E93" else "#666666"

                        views.setInt(R.id.layoutWidgetContainer, "setBackgroundResource", if (isDark) R.drawable.rounded_widget_bg else R.drawable.rounded_widget_bg_light)
                        views.setTextColor(R.id.tvWidgetSubName, Color.parseColor(textColorMain))
                        views.setTextColor(R.id.tvWidgetPrice, Color.parseColor(textColorMain))
                        views.setTextColor(R.id.tvWidgetLetterIcon, Color.WHITE)
                        views.setTextColor(R.id.tvWidgetPeriod, Color.parseColor(textColorSecondary))
                        views.setTextColor(R.id.tvWidgetPaymentMethod, Color.parseColor(textColorSecondary))
                        views.setTextColor(R.id.tvWidgetEmpty, Color.parseColor(textColorSecondary))
                        
                        val appPurple = context.getColor(R.color.primary_indigo)
                        
                        // Apply purple to bars and icons
                        views.setInt(R.id.viewWidgetColorBar, "setColorFilter", appPurple)
                        views.setInt(R.id.ivWidgetBellIcon, "setColorFilter", appPurple)
                        views.setInt(R.id.ivWidgetTagIcon, "setColorFilter", appPurple)
                        views.setInt(R.id.viewWidgetPaymentDot, "setColorFilter", appPurple)
                        
                        views.setTextColor(R.id.tvWidgetDaysLeft, appPurple)
                        views.setTextColor(R.id.tvWidgetCategoryName, appPurple)

                        // Data Binding
                        views.setTextViewText(R.id.tvWidgetSubName, nearestSub.name)
                        
                        val periodText = if (nearestSub.period == "one-time") {
                            context.getString(R.string.one_time)
                        } else {
                            val pLabel = when(nearestSub.period) {
                                "daily" -> context.getString(R.string.daily)
                                "weekly" -> context.getString(R.string.weekly)
                                "monthly" -> context.getString(R.string.monthly)
                                "yearly" -> context.getString(R.string.yearly)
                                else -> context.getString(R.string.one_time)
                            }
                            "$pLabel • ${context.getString(R.string.regular)}"
                        }
                        views.setTextViewText(R.id.tvWidgetPeriod, periodText)
                        views.setTextViewText(R.id.tvWidgetCategoryName, nearestSub.category)

                        if (nearestSub.paymentMethod.isNotEmpty()) {
                            views.setViewVisibility(R.id.layoutWidgetPaymentRow, View.VISIBLE)
                            views.setTextViewText(R.id.tvWidgetPaymentMethod, nearestSub.paymentMethod)
                        } else {
                            views.setViewVisibility(R.id.layoutWidgetPaymentRow, View.GONE)
                        }

                        val days = calculateDaysRemaining(nearestSub.startDate, nearestSub.period, nearestSub.periodValue)
                        val daysText = when {
                            days == 0 -> context.getString(R.string.payment_today)
                            days == 1 -> context.getString(R.string.tomorrow_payment)
                            days < 0 -> context.getString(R.string.expired)
                            else -> context.getString(R.string.days_remaining, days)
                        }
                        views.setTextViewText(R.id.tvWidgetDaysLeft, daysText)
                        
                        // Apply User Settings
                        views.setViewVisibility(R.id.tvWidgetDaysLeft, if (showTimeRemaining) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.tvWidgetCategoryName, if (showTags) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.ivWidgetTagIcon, if (showTags) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.ivWidgetBellIcon, if (nearestSub.notifyDaysBefore >= 0) View.VISIBLE else View.GONE)

                        val rates = CurrencyService.loadRatesFromLocal(context)
                        val displayPrice = if (showConvertedPrices && defaultCurrency != (nearestSub.currency ?: "TRY")) {
                            val amountInTry = CurrencyService.convertToTry(nearestSub.price, nearestSub.currency ?: "TRY", rates)
                            CurrencyService.convertFromTry(amountInTry, defaultCurrency, rates)
                        } else {
                            nearestSub.price
                        }
                        val displayCurrencyCode = if (showConvertedPrices) defaultCurrency else (nearestSub.currency ?: "TRY")
                        val currencySymbol = CurrencyHelper.getLocalizedSymbol(displayCurrencyCode, context)
                        views.setTextViewText(R.id.tvWidgetPrice, "${String.format(java.util.Locale.US, "%.2f", displayPrice)} $currencySymbol")

                        // Icon handling
                        if (!nearestSub.imagePath.isNullOrEmpty()) {
                            views.setViewVisibility(R.id.ivWidgetLogo, View.VISIBLE)
                            views.setViewVisibility(R.id.ivWidgetLetterBg, View.GONE)
                            views.setViewVisibility(R.id.tvWidgetLetterIcon, View.GONE)
                            
                            AppWidgetTarget(context, R.id.ivWidgetLogo, views, appWidgetId).let { target ->
                                Glide.with(context.applicationContext)
                                    .asBitmap()
                                    .load(nearestSub.imagePath)
                                    .circleCrop()
                                    .into(target)
                            }
                        } else {
                            views.setViewVisibility(R.id.ivWidgetLogo, View.GONE)
                            views.setViewVisibility(R.id.ivWidgetLetterBg, View.VISIBLE)
                            views.setViewVisibility(R.id.tvWidgetLetterIcon, View.VISIBLE)
                            
                            val char = nearestSub.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                            views.setTextViewText(R.id.tvWidgetLetterIcon, char)
                            try {
                                views.setInt(R.id.ivWidgetLetterBg, "setColorFilter", Color.parseColor(nearestSub.color))
                            } catch (e: Exception) {
                                views.setInt(R.id.ivWidgetLetterBg, "setColorFilter", appPurple)
                            }
                        }
                        
                        // Intent
                        val intent = Intent(context, DetailActivity::class.java).apply {
                            putExtra("sub_id", nearestSub.id)
                            putExtra("hide_actions", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pendingIntent = PendingIntent.getActivity(context, nearestSub.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.layoutWidgetContainer, pendingIntent)
                    } else {
                        views.setViewVisibility(R.id.layoutWidgetContainer, View.GONE)
                        views.setViewVisibility(R.id.tvWidgetEmpty, View.VISIBLE)
                        views.setTextViewText(R.id.tvWidgetEmpty, context.getString(R.string.no_upcoming_subs))
                    }
                }
            } catch (e: Exception) { 
                e.printStackTrace()
                views.setViewVisibility(R.id.tvWidgetEmpty, View.VISIBLE)
                views.setTextViewText(R.id.tvWidgetEmpty, "Error") 
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
