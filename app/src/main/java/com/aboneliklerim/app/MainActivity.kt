package com.aboneliklerim.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : BaseActivity() {

    companion object {
        var showTimeRemainingGlobal = true
        var showTagsGlobal = true
        var showActiveOnlyGlobal = true
        var paymentTypeFilterGlobal = 0 // 0: All, 1: Regular, 2: One-time
    }

    private var homeFragment = HomeFragment()
    private var upcomingFragment = UpcomingFragment()
    private var reportsFragment = ReportsFragment()
    private var moreFragment = MoreFragment()
    private var activeFragment: Fragment = homeFragment
    
    var currentRates: Map<String, Double> = emptyMap()
    
    private var mAdViewTop: AdView? = null
    private var mAdViewBottom: AdView? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var tabClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Google Play Hizmetleri Kontrolü
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean("is_premium_active", false)
        CurrencyService.isPremiumActive = isPremium

        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (availability == ConnectionResult.SUCCESS) {
            if (!isPremium) {
                MobileAds.initialize(this) {
                    Log.d("AdMob", "MainActivity: SDK Hazır.")
                    runOnUiThread {
                        loadBannerAds()
                        loadInterstitialAd()
                    }
                }
            } else {
                findViewById<View>(R.id.adViewTop)?.visibility = View.GONE
                findViewById<View>(R.id.adViewBottom)?.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, getString(R.string.play_services_update), Toast.LENGTH_LONG).show()
        }

        setupFragments(savedInstanceState)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        
        // Disable long-press tooltips
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val view = bottomNav.findViewById<View>(menuItem.itemId)
            view?.setOnLongClickListener { true }
        }

        bottomNav.setOnItemSelectedListener { item ->
            val targetFragment = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_upcoming -> upcomingFragment
                R.id.nav_reports -> reportsFragment
                R.id.nav_more -> moreFragment
                else -> null
            }

            if (targetFragment != null && activeFragment != targetFragment) {
                tabClickCount++
                if (tabClickCount >= 5) {
                    showInterstitialAd()
                    tabClickCount = 0
                }

                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .setReorderingAllowed(true)
                    .commit()
                activeFragment = targetFragment
                return@setOnItemSelectedListener true
            }
            false
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val prefsData = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            val json = prefsData.getString("subs_list", "[]")
            val type = object : com.google.gson.reflect.TypeToken<List<Subscription>>() {}.type
            val subscriptions: List<Subscription> = com.google.gson.Gson().fromJson(json, type) ?: emptyList()
            
            val prefsSettings = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val isPremium = prefsSettings.getBoolean("is_premium_active", false)
            val limit = if (isPremium) 100 else 8
            
            if (subscriptions.size >= limit) {
                if (!isPremium) {
                    startActivity(Intent(this, PremiumActivity::class.java))
                } else {
                    Toast.makeText(this, getString(R.string.premium_limit_max), Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }

            startActivityForResult(Intent(this, AddEditActivity::class.java), 100)
        }

        // Alternatif Giriş: Alttaki mor barın herhangi bir yerine uzun basınca Inspector açılır
        findViewById<View>(R.id.bottomAppBar)?.setOnLongClickListener {
            openInspector()
            true
        }

        scheduleNotifications()
        checkNotificationPermission()
    }

    private fun loadBannerAds() {
        try {
            mAdViewTop = findViewById(R.id.adViewTop)
            mAdViewBottom = findViewById(R.id.adViewBottom)

            val adRequest = AdRequest.Builder().build()
            
            // Üst Banner için özel Listener
            mAdViewTop?.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Üst Banner yüklenemedi: ${error.message} (Kod: ${error.code})")
                }
                override fun onAdLoaded() {
                    Log.d("AdMob", "Üst Banner başarıyla yüklendi.")
                }
            }

            // Alt Banner için özel Listener
            mAdViewBottom?.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Alt Banner yüklenemedi: ${error.message} (Kod: ${error.code})")
                }
                override fun onAdLoaded() {
                    Log.d("AdMob", "Alt Banner başarıyla yüklendi.")
                }
            }

            mAdViewTop?.loadAd(AdRequest.Builder().build())
            mAdViewBottom?.loadAd(AdRequest.Builder().build())
            
        } catch (e: Exception) {
            Log.e("AdMob", "Banner başlatma hatası: ${e.message}")
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3041412504257891/3575008419", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    Log.d("AdMob", "Geçiş Reklamı yüklendi.")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMob", "Geçiş Reklamı yüklenemedi: ${adError.message}")
                    mInterstitialAd = null
                }
            })
    }

    private fun showInterstitialAd() {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_premium_active", false)) return

        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMob", "Geçiş Reklamı kapatıldı.")
                    mInterstitialAd = null
                    loadInterstitialAd() // Bir sonraki kullanım için tekrar yükle
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e("AdMob", "Geçiş Reklamı gösterilemedi.")
                    mInterstitialAd = null
                }
            }
            mInterstitialAd?.show(this)
        } else {
            Log.d("AdMob", "Geçiş Reklamı henüz hazır değil.")
            loadInterstitialAd()
        }
    }

    private fun openInspector() {
        Toast.makeText(this, getString(R.string.ad_inspector_preparing), Toast.LENGTH_SHORT).show()
        MobileAds.openAdInspector(this) { error ->
            if (error != null) {
                Log.e("AdMob", "Inspector Açılamadı: ${error.message}")
                Toast.makeText(this, getString(R.string.error_general, error.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, moreFragment, "more").hide(moreFragment)
                add(R.id.fragmentContainer, reportsFragment, "reports").hide(reportsFragment)
                add(R.id.fragmentContainer, upcomingFragment, "upcoming").hide(upcomingFragment)
                add(R.id.fragmentContainer, homeFragment, "home")
            }.commit()
            activeFragment = homeFragment
        } else {
            // Restore fragments from FragmentManager to prevent overlapping duplicates
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            upcomingFragment = supportFragmentManager.findFragmentByTag("upcoming") as? UpcomingFragment ?: UpcomingFragment()
            reportsFragment = supportFragmentManager.findFragmentByTag("reports") as? ReportsFragment ?: ReportsFragment()
            moreFragment = supportFragmentManager.findFragmentByTag("more") as? MoreFragment ?: MoreFragment()

            // Re-find active fragment
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible } ?: homeFragment
        }
    }

    override fun onPause() {
        mAdViewTop?.pause()
        mAdViewBottom?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mAdViewTop?.resume()
        mAdViewBottom?.resume()
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        CurrencyService.isPremiumActive = prefs.getBoolean("is_premium_active", false)
    }

    override fun onDestroy() {
        mAdViewTop?.destroy()
        mAdViewBottom?.destroy()
        super.onDestroy()
    }

    private fun scheduleNotifications() {
        NotificationScheduler.scheduleAlarms(this)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activeFragment.onActivityResult(requestCode, resultCode, data)
    }
}
