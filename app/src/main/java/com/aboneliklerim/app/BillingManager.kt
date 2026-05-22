package com.aboneliklerim.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {

    interface BillingListener {
        fun onBillingSetupFinished()
        fun onPremiumStatusChanged(isPremium: Boolean)
        fun onProductsLoaded(monthly: ProductDetails?, yearly: ProductDetails?, lifetime: ProductDetails?)
        fun onRestoreFinished(found: Boolean)
        fun onError(message: String)
    }

    private var billingClient: BillingClient
    private val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
    private var listener: BillingListener? = null

    var monthlyDetails: ProductDetails? = null
        private set
    var yearlyDetails: ProductDetails? = null
        private set
    var lifetimeDetails: ProductDetails? = null
        private set

    companion object {
        private const val TAG = "BillingManager"
        const val SKU_MONTHLY = "monthly_premium"
        const val SKU_YEARLY = "yearly_premium"
        const val SKU_LIFETIME = "yearlypremium"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        startConnection()
    }

    fun setListener(listener: BillingListener) {
        this.listener = listener
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected.")
                    checkPurchases()
                    queryProducts()
                    listener?.onBillingSetupFinished()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected.")
            }
        })
    }

    fun checkPurchases() {
        // Query Subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (hasActiveSub) {
                    updatePremiumStatus(true)
                    listener?.onRestoreFinished(true)
                } else {
                    // If no active sub, check one-time purchase (lifetime)
                    checkLifetimePurchase()
                }
            } else {
                listener?.onRestoreFinished(false)
            }
        }
    }

    private fun checkLifetimePurchase() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasLifetime = purchases.any {
                    it.products.contains(SKU_LIFETIME) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasLifetime) {
                    updatePremiumStatus(true)
                    listener?.onRestoreFinished(true)
                } else {
                    updatePremiumStatus(false)
                    listener?.onRestoreFinished(false)
                }
            } else {
                listener?.onRestoreFinished(false)
            }
        }
    }

    private fun queryProducts() {
        CoroutineScope(Dispatchers.IO).launch {
            // Subscriptions
            val subParams = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(SKU_MONTHLY).setProductType(BillingClient.ProductType.SUBS).build(),
                    QueryProductDetailsParams.Product.newBuilder().setProductId(SKU_YEARLY).setProductType(BillingClient.ProductType.SUBS).build()
                )).build()

            val subResult = billingClient.queryProductDetails(subParams)

            // Lifetime
            val inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(SKU_LIFETIME).setProductType(BillingClient.ProductType.INAPP).build()
                )).build()

            val inAppResult = billingClient.queryProductDetails(inAppParams)

            withContext(Dispatchers.Main) {
                subResult.productDetailsList?.forEach {
                    if (it.productId == SKU_MONTHLY) monthlyDetails = it
                    if (it.productId == SKU_YEARLY) yearlyDetails = it
                }
                inAppResult.productDetailsList?.forEach {
                    if (it.productId == SKU_LIFETIME) lifetimeDetails = it
                }
                listener?.onProductsLoaded(monthlyDetails, yearlyDetails, lifetimeDetails)
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply { if (offerToken != null) setOfferToken(offerToken) }
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled purchase.")
        } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            updatePremiumStatus(true)
        } else {
            listener?.onError("Error: ${result.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            updatePremiumStatus(true)
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                CoroutineScope(Dispatchers.IO).launch {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                }
            }
        }
    }

    private fun updatePremiumStatus(isPremium: Boolean) {
        prefs.edit().putBoolean("is_premium_active", isPremium).apply()
        CurrencyService.isPremiumActive = isPremium
        listener?.onPremiumStatusChanged(isPremium)
    }

    fun isPremium(): Boolean {
        return prefs.getBoolean("is_premium_active", false)
    }
}
