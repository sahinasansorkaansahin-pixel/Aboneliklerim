package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.aboneliklerim.app.ThemeHelper
import com.aboneliklerim.app.MainActivity
import com.aboneliklerim.app.UpcomingActivity
import com.aboneliklerim.app.ReportsActivity
import com.aboneliklerim.app.AddEditActivity
import com.aboneliklerim.app.OnboardingActivity
import android.app.Activity


class MoreActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        setupNavigation()

        findViewById<LinearLayout>(R.id.rowRate).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${packageName}")))
            } catch (_: Exception) {}
        }
        findViewById<LinearLayout>(R.id.rowShare).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_msg, packageName))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_app_title)))
        }

        findViewById<LinearLayout>(R.id.rowTutorial).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        updateThemeUI()
        findViewById<LinearLayout>(R.id.rowTheme).setOnClickListener {
            showThemeSelector()
        }

        updateLanguageUI()
        findViewById<LinearLayout>(R.id.rowLanguage).setOnClickListener {
            showLanguageSelector()
        }

        // Setup legal policy overlay click events
        val overlay = findViewById<android.widget.FrameLayout>(R.id.layoutPolicyOverlay)
        val tvTitle = findViewById<TextView>(R.id.tvPolicyTitle)
        val tvContent = findViewById<TextView>(R.id.tvPolicyContent)
        val btnClose = findViewById<android.widget.ImageView>(R.id.btnClosePolicy)

        val showPolicy = { titleRes: Int, contentRes: Int ->
            tvTitle.text = getString(titleRes)
            tvContent.text = getString(contentRes)
            overlay.visibility = View.VISIBLE
        }

        findViewById<LinearLayout>(R.id.rowPrivacyPolicy).setOnClickListener {
            showPolicy(R.string.policy_privacy_title, R.string.policy_privacy_content)
        }
        findViewById<LinearLayout>(R.id.rowAccountDeletion).setOnClickListener {
            showPolicy(R.string.policy_deletion_title, R.string.policy_deletion_content)
        }
        findViewById<LinearLayout>(R.id.rowCopyright).setOnClickListener {
            showPolicy(R.string.policy_copyright_title, R.string.policy_copyright_content)
        }
        findViewById<LinearLayout>(R.id.rowAdPartners).setOnClickListener {
            showPolicy(R.string.policy_adpartners_title, R.string.policy_adpartners_content)
        }

        btnClose.setOnClickListener {
            overlay.visibility = View.GONE
        }
    }

    private fun updateLanguageUI() {
        val languages = LocaleHelper.getSupportedLanguageNames(this)
        val langCodes = LocaleHelper.SUPPORTED_LOCALES
        val activeLang = LocaleHelper.getActiveLanguage(this)
        var index = langCodes.indexOf(activeLang)
        if (index == -1) index = 1 // Fallback to en-US
        findViewById<TextView>(R.id.tvCurrentLanguage).text = languages[index]
    }

    private fun showLanguageSelector() {
        val languages = LocaleHelper.getSupportedLanguageNames(this)
        val langCodes = LocaleHelper.SUPPORTED_LOCALES
        val activeLang = LocaleHelper.getActiveLanguage(this)
        var checkedItem = langCodes.indexOf(activeLang)
        if (checkedItem == -1) checkedItem = 1 // Fallback to en-US

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val newLang = langCodes[which]
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(newLang)
                )
                LocaleHelper.setManualLocale(this, newLang)
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun updateThemeUI() {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme_mode", ThemeHelper.THEME_SMART)
        findViewById<TextView>(R.id.tvCurrentTheme).text = when(theme) {
            ThemeHelper.THEME_LIGHT -> getString(R.string.theme_light)
            ThemeHelper.THEME_DARK -> getString(R.string.theme_dark)
            ThemeHelper.THEME_SYSTEM -> getString(R.string.theme_system)
            else -> getString(R.string.theme_smart)
        }
    }

    private fun showThemeSelector() {
        val options = arrayOf(
            getString(R.string.theme_smart_desc),
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.system_default)
        )
        val values = arrayOf(ThemeHelper.THEME_SMART, ThemeHelper.THEME_LIGHT, ThemeHelper.THEME_DARK, ThemeHelper.THEME_SYSTEM)
        
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val current = prefs.getString("theme_mode", ThemeHelper.THEME_SMART)
        val checkedItem = values.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(R.string.select_dark_mode)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                ThemeHelper.setTheme(this, values[which])
                updateThemeUI()
                dialog.dismiss()
                recreate() // Recreate activity to apply theme fully
            }
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_more
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
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_more -> true
                else -> false
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 300 && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val overlay = findViewById<android.widget.FrameLayout>(R.id.layoutPolicyOverlay)
        if (overlay != null && overlay.visibility == android.view.View.VISIBLE) {
            overlay.visibility = android.view.View.GONE
        } else {
            super.onBackPressed()
        }
    }
}
