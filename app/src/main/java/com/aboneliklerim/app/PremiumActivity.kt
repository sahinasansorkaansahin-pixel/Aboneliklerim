package com.aboneliklerim.app

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.ProductDetails
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PremiumActivity : AppCompatActivity() {

    private lateinit var billingManager: BillingManager
    private val prefs by lazy { getSharedPreferences("Settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        billingManager = BillingManager.getInstance(this)

        findViewById<FloatingActionButton>(R.id.fabClosePremium).setOnClickListener { finish() }

        setupBilling()
        setupInfoButtons()
        setupPolicyButtons()
    }

    private fun setupBilling() {
        billingManager.setListener(object : BillingManager.BillingListener {
            override fun onBillingSetupFinished() {
                updateUI()
            }

            override fun onPremiumStatusChanged(isPremium: Boolean) {
                if (isPremium) {
                    Toast.makeText(this@PremiumActivity, getString(R.string.premium_active_toast), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onProductsLoaded(monthly: ProductDetails?, yearly: ProductDetails?, lifetime: ProductDetails?) {
                updateUI()
            }

            override fun onError(message: String) {
                Toast.makeText(this@PremiumActivity, message, Toast.LENGTH_LONG).show()
            }
        })

        updateUI()

        findViewById<MaterialButton>(R.id.btnPlanMonthly).setOnClickListener {
            billingManager.monthlyDetails?.let { billingManager.launchBillingFlow(this, it) }
                ?: showBillingUnavailable()
        }

        findViewById<MaterialButton>(R.id.btnPlanYearly).setOnClickListener {
            billingManager.yearlyDetails?.let { billingManager.launchBillingFlow(this, it) }
                ?: showBillingUnavailable()
        }

        findViewById<MaterialButton>(R.id.btnPlanLifetime).setOnClickListener {
            billingManager.lifetimeDetails?.let { billingManager.launchBillingFlow(this, it) }
                ?: showBillingUnavailable()
        }

        findViewById<TextView>(R.id.btnRestorePurchases).setOnClickListener {
            billingManager.checkPurchases()
            Toast.makeText(this, R.string.syncing_data, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val monthly = billingManager.monthlyDetails
        val yearly = billingManager.yearlyDetails
        val lifetime = billingManager.lifetimeDetails

        runOnUiThread {
            monthly?.let {
                val price = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                findViewById<MaterialButton>(R.id.btnPlanMonthly).text = "${getString(R.string.plan_monthly)} — $price"
            }
            yearly?.let {
                val price = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                findViewById<MaterialButton>(R.id.btnPlanYearly).text = "${getString(R.string.plan_yearly)} — $price"
            }
            lifetime?.let {
                val price = it.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                findViewById<MaterialButton>(R.id.btnPlanLifetime).text = "${getString(R.string.plan_lifetime)} — $price"
            }
        }
    }

    private fun showBillingUnavailable() {
        AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(getString(R.string.premium_get_service))
            .setMessage(getString(R.string.billing_connection_failed))
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun setupInfoButtons() {
        fun showInfo(titleRes: Int, msgRes: Int) {
            AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(titleRes))
                .setMessage(getString(msgRes))
                .setPositiveButton("OK", null)
                .show()
        }
        findViewById<TextView>(R.id.btnInfoNoAds).setOnClickListener { showInfo(R.string.premium_feature_no_ads, R.string.premium_info_no_ads) }
        findViewById<TextView>(R.id.btnInfoCurrency).setOnClickListener { showInfo(R.string.premium_feature_currency, R.string.premium_info_currency) }
        findViewById<TextView>(R.id.btnInfoSharedCost).setOnClickListener { showInfo(R.string.premium_feature_shared_cost, R.string.premium_info_shared_cost) }
        findViewById<TextView>(R.id.btnInfoLimit).setOnClickListener { showInfo(R.string.premium_feature_limit, R.string.premium_info_limit) }
        findViewById<TextView>(R.id.btnInfoTrend).setOnClickListener { showInfo(R.string.premium_feature_trend, R.string.premium_info_trend) }
        findViewById<TextView>(R.id.btnInfoGuestLogin).setOnClickListener { showInfo(R.string.premium_feature_guest_login, R.string.premium_info_guest_login) }
    }

    private fun setupPolicyButtons() {
        fun showPolicy(titleRes: Int, msgRes: Int) {
            AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(titleRes))
                .setMessage(getString(msgRes))
                .setPositiveButton("OK", null).show()
        }
        findViewById<TextView>(R.id.tvPremiumPrivacy).setOnClickListener { showPolicy(R.string.policy_privacy_title, R.string.policy_privacy_content) }
        findViewById<TextView>(R.id.tvPremiumTerms).setOnClickListener { showPolicy(R.string.policy_copyright_title, R.string.policy_copyright_content) }
        findViewById<TextView>(R.id.tvPremiumDeletion).setOnClickListener { showPolicy(R.string.policy_deletion_title, R.string.policy_deletion_content) }
        findViewById<TextView>(R.id.tvPremiumAdPartners).setOnClickListener { showPolicy(R.string.policy_adpartners_title, R.string.policy_adpartners_content) }
    }
}
