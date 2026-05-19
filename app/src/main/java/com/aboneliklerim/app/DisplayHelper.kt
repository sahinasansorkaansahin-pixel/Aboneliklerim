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
        
        // 1. Eye Protection Overlay (App-scoped)
        val eyeActive = prefs.getBoolean("eye_protection_active", false)
        toggleEyeProtection(activity, eyeActive)
    }

    private fun toggleEyeProtection(activity: Activity, active: Boolean) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlayId = View.generateViewId()
        var overlay = activity.findViewById<View>(R.id.eye_protection_overlay)

        if (active) {
            if (overlay == null) {
                overlay = View(activity).apply {
                    id = R.id.eye_protection_overlay
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.parseColor("#1AFFB74D")) // Very subtle light orange tint (10%)
                    isClickable = false
                    isFocusable = false
                    elevation = 999f
                }
                rootView.addView(overlay)
            }
            overlay.visibility = View.VISIBLE
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
