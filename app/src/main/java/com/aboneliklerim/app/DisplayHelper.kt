package com.aboneliklerim.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

object DisplayHelper {

    fun applyDisplaySettings(activity: Activity) {
        val prefs = activity.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        
        // 1. Eye Protection / Blue Light Overlay (App-scoped)
        val blueLightActive = prefs.getBoolean("blue_light_filter_enabled", false)
        toggleBlueLightFilter(activity, blueLightActive)
    }

    fun toggleBlueLightFilter(activity: Activity, active: Boolean) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        var overlay = activity.findViewById<View>(R.id.eye_protection_overlay)

        if (active) {
            if (overlay == null) {
                overlay = View(activity).apply {
                    id = R.id.eye_protection_overlay
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Very subtle amber/orange tint (approx 8% intensity)
                    setBackgroundColor(Color.parseColor("#14FF9800")) 
                    isClickable = false
                    isFocusable = false
                    elevation = 999f
                }
                rootView.addView(overlay)
            }
            overlay.visibility = View.VISIBLE
            overlay.bringToFront()
        } else {
            overlay?.visibility = View.GONE
        }
    }

    fun getScaledContext(context: Context): Context {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val scale = prefs.getFloat("ui_font_scale", 1.0f)
        
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.fontScale = scale
        
        // Ensure manually chosen locale is correctly preserved in custom wrapped context
        val activeLang = LocaleHelper.getActiveLanguage(context)
        val locale = java.util.Locale.forLanguageTag(activeLang)
        java.util.Locale.setDefault(locale)
        configuration.setLocale(locale)
        
        
        
        return context.createConfigurationContext(configuration)
    }
}
