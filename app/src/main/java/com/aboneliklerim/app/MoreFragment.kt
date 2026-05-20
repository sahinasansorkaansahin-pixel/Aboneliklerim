package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.ProgressBar
import android.app.Activity
import com.aboneliklerim.app.FirebaseManager
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.result.contract.ActivityResultContracts
import com.aboneliklerim.app.ThemeHelper
import com.aboneliklerim.app.OnboardingActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import com.google.gson.JsonParser

class MoreFragment : Fragment() {
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), getString(R.string.google_login_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.rowGoogleSignIn).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        view.findViewById<LinearLayout>(R.id.rowGuestSignIn).setOnClickListener {
            val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val isPremium = prefs.getBoolean("is_premium_active", false)
            if (!isPremium) {
                startActivity(Intent(requireContext(), PremiumActivity::class.java))
                return@setOnClickListener
            }
            prefs.edit().putBoolean("is_guest_active", true).apply()
            updateUserInfo(view)
        }

        view.findViewById<View>(R.id.btnLogoutMore).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirm_msg))
                .setPositiveButton(getString(R.string.logout)) { _, _ ->
                    auth.signOut()
                    googleSignInClient.signOut()
                    val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    prefs.edit().remove("is_guest_active").apply()
                    updateUserInfo(view)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        val tvSyncStatusView = view.findViewById<TextView>(R.id.tvSyncStatusMore)

        view.findViewById<View>(R.id.btnUploadMore)?.setOnClickListener {
            val subs = loadLocalSubscriptions()
            if (subs.isEmpty()) {
                tvSyncStatusView?.text = getString(R.string.no_subs_to_backup)
                tvSyncStatusView?.visibility = View.VISIBLE
                view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                return@setOnClickListener
            }

            FirebaseManager.syncDataToCloud(requireContext(), subs,
                onSuccess = {
                    tvSyncStatusView?.text = getString(R.string.backup_success)
                    tvSyncStatusView?.visibility = View.VISIBLE
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 2000)
                },
                onFailure = { _ ->
                    tvSyncStatusView?.text = getString(R.string.backup_error)
                    tvSyncStatusView?.visibility = View.VISIBLE
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                }
            )
        }

        view.findViewById<View>(R.id.btnDownloadMore)?.setOnClickListener {
            FirebaseManager.fetchDataFromCloud(
                onResult = { subs, _ ->
                    if (!subs.isNullOrEmpty()) {
                        val prefs = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
                        val json = Gson().toJson(subs)
                        prefs.edit().putString("subs_list", json).apply()
                        tvSyncStatusView?.text = getString(R.string.restore_success)
                        tvSyncStatusView?.visibility = View.VISIBLE
                        updateUserInfo(view)
                    } else {
                        tvSyncStatusView?.text = getString(R.string.no_cloud_data_found)
                        tvSyncStatusView?.visibility = View.VISIBLE
                    }
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                },
                onFailure = { _ ->
                    tvSyncStatusView?.text = getString(R.string.backup_error)
                    tvSyncStatusView?.visibility = View.VISIBLE
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                }
            )
        }

        view.findViewById<View>(R.id.btnDeleteCloudMore)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(R.string.delete_cloud_confirm_title))
                .setMessage(getString(R.string.delete_cloud_confirm_msg))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    FirebaseManager.deleteCloudData(
                        onSuccess = {
                            tvSyncStatusView?.text = getString(R.string.delete_cloud_success)
                            tvSyncStatusView?.visibility = View.VISIBLE
                            view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                        },
                        onFailure = {
                            tvSyncStatusView?.text = getString(R.string.delete_cloud_error)
                            tvSyncStatusView?.visibility = View.VISIBLE
                            view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                        }
                    )
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        view.findViewById<View>(R.id.cardPremium)?.setOnClickListener {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }

        updateUserInfo(view)
        
        view.findViewById<LinearLayout>(R.id.rowRate).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${requireContext().packageName}")))
            } catch (_: Exception) {}
        }
        view.findViewById<LinearLayout>(R.id.rowShare).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_msg, requireContext().packageName))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_app_title)))
        }

        view.findViewById<LinearLayout>(R.id.rowTutorial).setOnClickListener {
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
        }


        updateThemeUI()
        view.findViewById<LinearLayout>(R.id.rowTheme).setOnClickListener {
            showThemeSelector()
        }

        updateLanguageUI()
        view.findViewById<LinearLayout>(R.id.rowLanguage).setOnClickListener {
            showLanguageSelector()
        }

        updateDefaultCurrencyUI()
        view.findViewById<LinearLayout>(R.id.rowDefaultCurrency).setOnClickListener {
            showDefaultCurrencySelector()
        }

        updateDateFormatUI()
        view.findViewById<android.widget.LinearLayout>(R.id.rowDateFormat).setOnClickListener {
            showDateFormatSelector()
        }



        // --- YEREL YEDEKLEME İŞLEMLERİ ---
        view.findViewById<View>(R.id.rowLocalBackup).setOnClickListener {
            val subs = loadLocalSubscriptions()
            if (subs.isEmpty()) {
                tvSyncStatusView?.text = getString(R.string.no_subs_to_backup)
                tvSyncStatusView?.visibility = View.VISIBLE
                view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                return@setOnClickListener
            }
            val prefs = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            prefs.edit().putString("subs_backup", Gson().toJson(subs)).apply()
            tvSyncStatusView?.text = getString(R.string.local_backup_success)
            tvSyncStatusView?.visibility = View.VISIBLE
            view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 2000)
        }

        view.findViewById<View>(R.id.rowLocalRestore).setOnClickListener {
            val prefs = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            val backupJson = prefs.getString("subs_backup", null)
            if (backupJson == null) {
                tvSyncStatusView?.text = getString(R.string.no_local_backup)
                tvSyncStatusView?.visibility = View.VISIBLE
                view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                return@setOnClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(R.string.local_restore))
                .setMessage(getString(R.string.local_restore_confirm))
                .setPositiveButton(getString(R.string.restore)) { _, _ ->
                    prefs.edit().putString("subs_list", backupJson).apply()
                    tvSyncStatusView?.text = getString(R.string.local_restore_success)
                    tvSyncStatusView?.visibility = View.VISIBLE
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 2000)
                    updateUserInfo(view)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        view.findViewById<View>(R.id.rowLocalDelete).setOnClickListener {
            val prefs = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            if (prefs.getString("subs_backup", null) == null) {
                tvSyncStatusView?.text = getString(R.string.no_local_backup)
                tvSyncStatusView?.visibility = View.VISIBLE
                view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 3000)
                return@setOnClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(getString(R.string.local_delete))
                .setMessage(getString(R.string.local_delete_confirm))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    prefs.edit().remove("subs_backup").apply()
                    tvSyncStatusView?.text = getString(R.string.local_delete_success)
                    tvSyncStatusView?.visibility = View.VISIBLE
                    view.postDelayed({ tvSyncStatusView?.visibility = View.GONE }, 2000)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // --- REKLAM KİMLİĞİ ÖĞRENME (Testerlar İçin - Herkese Açık) ---
        view.findViewById<LinearLayout>(R.id.rowShowAdId).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val idInfo = AdvertisingIdClient.getAdvertisingIdInfo(requireContext())
                    val adId = idInfo.id ?: "Bilinmiyor"
                    withContext(Dispatchers.Main) {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AdID", adId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), getString(R.string.ad_id_copied, adId), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.ad_id_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        // --- PREMIUM TESTER TOGGLE ---
        val tvCurrentPremiumStatus = view.findViewById<TextView>(R.id.tvCurrentPremiumStatus)
        val rowTogglePremium = view.findViewById<LinearLayout>(R.id.rowTogglePremium)
        if (tvCurrentPremiumStatus != null && rowTogglePremium != null) {
            fun updateStatusUI() {
                val isPremium = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    .getBoolean("is_premium_active", false)
                val activeLang = LocaleHelper.getActiveLanguage(requireContext())
                tvCurrentPremiumStatus.text = if (isPremium) {
                    if (activeLang == "tr") "Aktif" else "Active"
                } else {
                    if (activeLang == "tr") "Pasif" else "Inactive"
                }
                tvCurrentPremiumStatus.setTextColor(
                    if (isPremium) androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_indigo)
                    else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
            
            updateStatusUI()
            
            rowTogglePremium.setOnClickListener {
                val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
                val isPremium = prefs.getBoolean("is_premium_active", false)
                val newStatus = !isPremium
                
                prefs.edit().putBoolean("is_premium_active", newStatus).apply()
                CurrencyService.isPremiumActive = newStatus
                
                Toast.makeText(
                    requireContext(),
                    if (newStatus) {
                        if (LocaleHelper.getActiveLanguage(requireContext()) == "tr") "Premium Aktif Edildi! 💎" else "Premium Activated! 💎"
                    } else {
                        if (LocaleHelper.getActiveLanguage(requireContext()) == "tr") "Premium Devre Dışı Bırakıldı!" else "Premium Deactivated!"
                    },
                    Toast.LENGTH_SHORT
                ).show()
                
                updateStatusUI()
                updatePremiumOverlay(view)
                updateUserInfo(view)
                
                // Recreate activity so all other fragments refresh instantly!
                requireActivity().recreate()
            }
        }

        setupCurrencyConverter(view)
        setupStreamingPrices(view)
        updatePremiumOverlay(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { updatePremiumOverlay(it) }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view?.let { updatePremiumOverlay(it) }
        }
    }

    private fun updatePremiumOverlay(view: View) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean("is_premium_active", false)
        val overlay = view.findViewById<View>(R.id.layoutConverterPremiumOverlay)
        if (!isPremium) {
            overlay?.visibility = View.VISIBLE
            overlay?.setOnClickListener {
                startActivity(Intent(ctx, PremiumActivity::class.java))
            }
        } else {
            overlay?.visibility = View.GONE
        }

        val ivGuestLock = view.findViewById<View>(R.id.ivGuestLock)
        val tvGuestArrow = view.findViewById<View>(R.id.tvGuestArrow)
        if (!isPremium) {
            ivGuestLock?.visibility = View.VISIBLE
            tvGuestArrow?.visibility = View.GONE
        } else {
            ivGuestLock?.visibility = View.GONE
            tvGuestArrow?.visibility = View.VISIBLE
        }
    }


    private fun updateUserInfo(view: View) {
        val user = auth.currentUser
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isGuestActive = prefs.getBoolean("is_guest_active", false)
        val loggedIn = user != null || isGuestActive
        val isGuest = (user != null && user.isAnonymous) || isGuestActive
        view.findViewById<androidx.cardview.widget.CardView>(R.id.cardGoogleWrapper)?.visibility = if (loggedIn) View.GONE else View.VISIBLE
        val cardUser = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardUserInfo)
        cardUser?.visibility = if (loggedIn) View.VISIBLE else View.GONE
        if (loggedIn) {
            val name = if (isGuest) getString(R.string.guest_user) else (user?.displayName ?: "")
            val email = if (isGuest) "" else (user?.email ?: "")
            view.findViewById<TextView>(R.id.tvWelcomeMessage)?.text = if (name.isNotEmpty()) getString(R.string.welcome_user, name) else getString(R.string.welcome_simple)
            view.findViewById<TextView>(R.id.tvUserEmail)?.text = email
            view.findViewById<TextView>(R.id.tvUserEmail)?.visibility = if (isGuest) View.GONE else View.VISIBLE
            view.findViewById<View>(R.id.layoutCloudButtons)?.visibility = if (isGuest) View.GONE else View.VISIBLE
            view.findViewById<View>(R.id.layoutLocalBackupActions)?.visibility = if (isGuest) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.dividerAccountLocal)?.visibility = if (isGuest) View.VISIBLE else View.GONE
            val ivProfile = view.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivUserProfile)
            if (ivProfile != null) {
                if (isGuest) {
                    ivProfile.setImageResource(R.drawable.ic_guest_user)
                    ivProfile.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_indigo))
                } else {
                    ivProfile.clearColorFilter()
                    val googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
                    val photoUrl = googleAccount?.photoUrl ?: user?.photoUrl
                    if (photoUrl != null) {
                        com.bumptech.glide.Glide.with(this).load(photoUrl).circleCrop().placeholder(R.drawable.ic_default_avatar).diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL).into(ivProfile)
                    } else {
                        ivProfile.setImageResource(R.drawable.ic_default_avatar)
                    }
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val view = view ?: return
        val layoutStatus = view.findViewById<LinearLayout>(R.id.layoutLoginStatus)
        val tvStatus = view.findViewById<TextView>(R.id.tvLoginStatus)
        layoutStatus?.visibility = View.VISIBLE
        tvStatus?.text = getString(R.string.logging_in)
        view.findViewById<LinearLayout>(R.id.rowGoogleSignIn)?.visibility = View.GONE
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                val settingsPrefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
                settingsPrefs.edit().remove("is_guest_active").apply()
                tvStatus?.text = getString(R.string.syncing_data)
                FirebaseManager.fetchDataFromCloud(onResult = { subs, profile ->
                    if (subs != null) {
                        val prefs = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
                        prefs.edit().putString("subs_list", Gson().toJson(subs)).apply()
                    }
                    tvStatus?.text = getString(R.string.login_done)
                    view.postDelayed({ updateUserInfo(view); layoutStatus?.visibility = View.GONE }, 1000)
                }, onFailure = {
                    tvStatus?.text = getString(R.string.login_done)
                    view.postDelayed({ updateUserInfo(view); layoutStatus?.visibility = View.GONE }, 1000)
                })
            } else {
                layoutStatus?.visibility = View.GONE
                view.findViewById<LinearLayout>(R.id.rowGoogleSignIn)?.visibility = View.VISIBLE
                Toast.makeText(requireContext(), getString(R.string.error_general, task.exception?.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun updateThemeUI() {
        val view = view ?: return
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme_mode", ThemeHelper.THEME_SMART)
        view.findViewById<TextView>(R.id.tvCurrentTheme).text = when(theme) {
            ThemeHelper.THEME_LIGHT -> getString(R.string.theme_light)
            ThemeHelper.THEME_DARK -> getString(R.string.theme_dark)
            ThemeHelper.THEME_SYSTEM -> getString(R.string.theme_system)
            else -> getString(R.string.theme_smart)
        }
    }

    private fun showThemeSelector() {
        val options = arrayOf(getString(R.string.theme_smart_desc), getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system))
        val values = arrayOf(ThemeHelper.THEME_SMART, ThemeHelper.THEME_LIGHT, ThemeHelper.THEME_DARK, ThemeHelper.THEME_SYSTEM)
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val current = prefs.getString("theme_mode", ThemeHelper.THEME_SMART)
        val checkedItem = values.indexOf(current).coerceAtLeast(0)
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog).setTitle(R.string.select_dark_mode).setSingleChoiceItems(options, checkedItem) { dialog, which ->
            ThemeHelper.setTheme(requireActivity(), values[which])
            updateThemeUI()
            dialog.dismiss()
        }.show()
    }

    private fun updateLanguageUI() {
        val view = view ?: return
        val languages = LocaleHelper.getSupportedLanguageNames(requireContext())
        val langCodes = LocaleHelper.SUPPORTED_LOCALES
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        var index = langCodes.indexOf(activeLang)
        if (index == -1) index = 1 // Fallback to en-US
        view.findViewById<TextView>(R.id.tvCurrentLanguage).text = languages[index]
    }

    private fun showLanguageSelector() {
        val languages = LocaleHelper.getSupportedLanguageNames(requireContext())
        val langCodes = LocaleHelper.SUPPORTED_LOCALES
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        var checkedItem = langCodes.indexOf(activeLang)
        if (checkedItem == -1) checkedItem = 1 // Fallback to en-US
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog).setTitle(R.string.select_language).setSingleChoiceItems(languages, checkedItem) { dialog, which ->
            LocaleHelper.setManualLocale(requireContext(), langCodes[which])
            requireActivity().recreate()
            dialog.dismiss()
        }.show()
    }

    private fun updateDefaultCurrencyUI() {
        val view = view ?: return
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        val defaultCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)) ?: "TRY"
        view.findViewById<TextView>(R.id.tvCurrentDefaultCurrency).text = defaultCurrency
    }

    private fun showDefaultCurrencySelector() {
        val currencies = CurrencyHelper.getCurrencies(requireContext())
        val names = currencies.map { "${it.flag} ${it.name} (${it.code})" }.toTypedArray()
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        val currentCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)) ?: "TRY"
        val checkedItem = currencies.indexOfFirst { it.code == currentCurrency }.coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(getString(R.string.default_currency))
            .setSingleChoiceItems(names, checkedItem) { dialog, which ->
                prefs.edit().putString("default_currency", currencies[which].code).apply()
                updateDefaultCurrencyUI()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateDateFormatUI() {
        val view = view ?: return
        val currentFormat = DateFormatHelper.getSelectedFormat(requireContext())
        view.findViewById<TextView>(R.id.tvCurrentDateFormat).text = currentFormat
    }

    private fun showDateFormatSelector() {
        val formats = DateFormatHelper.FORMATS.toTypedArray()
        val currentFormat = DateFormatHelper.getSelectedFormat(requireContext())
        val checkedItem = formats.indexOf(currentFormat).coerceAtLeast(0)
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog).setTitle(R.string.select_date_format).setSingleChoiceItems(formats, checkedItem) { dialog, which ->
            requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("date_format", formats[which]).apply()
            updateDateFormatUI()
            dialog.dismiss()
        }.show()
    }

    private fun setupCurrencyConverter(view: View) {
        val etAmount = view.findViewById<EditText>(R.id.etCurrencyAmount)
        val tvBase = view.findViewById<TextView>(R.id.tvBaseCurrencyCode)
        val tvTarget = view.findViewById<TextView>(R.id.tvTargetCurrencyCode)
        val tvResult = view.findViewById<TextView>(R.id.tvConvertedAmount)
        val btnUpdate = view.findViewById<View>(R.id.btnUpdateCurrency)

        val currencies = CurrencyHelper.getCurrencies(requireContext())
        var baseCurrency = currencies.find { it.code == "TRY" } ?: currencies[0]
        var targetCurrency = currencies.find { it.code == "USD" } ?: currencies[1]

        tvBase.text = baseCurrency.code
        tvTarget.text = targetCurrency.code

        fun perform() {
            val amtText = etAmount.text.toString()
            if (amtText.isEmpty()) return
            val amt = amtText.toDoubleOrNull() ?: 1.0

            if (baseCurrency.code == targetCurrency.code) {
                tvResult.text = String.format(java.util.Locale.US, "%.2f", amt)
                return
            }

            tvResult.text = "..."

            lifecycleScope.launch {
                try {
                    val rates = CurrencyService.getExchangeRates(requireContext())
                    val amountInTry = CurrencyService.convertToTry(amt, baseCurrency.code, rates)
                    val converted = CurrencyService.convertFromTry(amountInTry, targetCurrency.code, rates)
                    tvResult.text = String.format(java.util.Locale.US, "%.2f", converted)
                } catch (e: Exception) {
                    e.printStackTrace()
                    tvResult.text = "Error"
                }
            }
        }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                perform()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tvBase.setOnClickListener {
            showCurrencySelector(currencies) { selected ->
                baseCurrency = selected
                tvBase.text = selected.code
                perform()
            }
        }

        tvTarget.setOnClickListener {
            showCurrencySelector(currencies) { selected ->
                targetCurrency = selected
                tvTarget.text = selected.code
                perform()
            }
        }

        btnUpdate.setOnClickListener { perform() }
    }

    private fun showCurrencySelector(currencies: List<CurrencyItem>, onSelected: (CurrencyItem) -> Unit) {
        val names = currencies.map { "${it.flag} ${it.name} (${it.code})" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(getString(R.string.currencies))
            .setItems(names) { _, which ->
                onSelected(currencies[which])
            }
            .show()
    }

    private fun loadLocalSubscriptions(): List<Subscription> {
        val json = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<Subscription>>() {}.type) else emptyList()
    }

    private fun setupStreamingPrices(view: View) {
        val cardStreamingPrices = view.findViewById<View>(R.id.cardStreamingPrices) ?: return
        val spinner = view.findViewById<Spinner>(R.id.spinnerStreamingPlatforms) ?: return
        
        lifecycleScope.launch {
            try {
                val rates = CurrencyService.getExchangeRates(requireContext())
                val platforms = StreamingPriceService.fetchAndSyncPrices(requireContext(), forceRefresh = false)
                
                if (platforms.isNotEmpty()) {
                    val platformNames = mutableListOf(getString(R.string.select_platform))
                    platformNames.addAll(platforms.map { it.name })
                    
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, platformNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                            if (position == 0) {
                                view.findViewById<ImageView>(R.id.imgSelectedStreamingLogo).visibility = View.INVISIBLE
                                view.findViewById<TextView>(R.id.tvStreamingSelectedTrend).visibility = View.GONE
                                view.findViewById<LinearLayout>(R.id.layoutStreamingDetails).visibility = View.GONE
                                view.findViewById<ImageView>(R.id.imgSelectedStreamingBell).visibility = View.GONE
                            } else {
                                val selectedPlatform = platforms[position - 1]
                                updateSelectedPlatformUI(view, selectedPlatform, rates)
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                    
                    // Initial selection: Placeholder
                    spinner.setSelection(0)
                } else {
                    cardStreamingPrices.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cardStreamingPrices.visibility = View.GONE
            }
        }
    }

    private fun updateSelectedPlatformUI(viewParent: View, platform: StreamingPriceService.StreamingPlatform, rates: Map<String, Double>) {
        val imgLogo = viewParent.findViewById<ImageView>(R.id.imgSelectedStreamingLogo)
        val tvTrend = viewParent.findViewById<TextView>(R.id.tvStreamingSelectedTrend)
        val plansContainer = viewParent.findViewById<LinearLayout>(R.id.layoutStreamingSelectedPlans)
        val btnWeb = viewParent.findViewById<View>(R.id.btnOpenStreamingWeb)
        val imgBell = viewParent.findViewById<ImageView>(R.id.imgSelectedStreamingBell)
        
        // Show details container and logo
        viewParent.findViewById<LinearLayout>(R.id.layoutStreamingDetails).visibility = View.VISIBLE
        imgLogo.visibility = View.VISIBLE
        imgBell.visibility = View.VISIBLE
        
        val isNotifEnabled = StreamingPriceService.isNotificationEnabledForPlatform(requireContext(), platform.id)
        updateBellIcon(imgBell, isNotifEnabled)

        imgBell.setOnClickListener {
            val newState = !StreamingPriceService.isNotificationEnabledForPlatform(requireContext(), platform.id)
            StreamingPriceService.setNotificationEnabledForPlatform(requireContext(), platform.id, newState)
            updateBellIcon(imgBell, newState)
            val msg = if (newState) {
                getString(R.string.notif_platform_enabled_toast, platform.name)
            } else {
                getString(R.string.notif_platform_disabled_toast, platform.name)
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        
        // Load Logo
        val logoResId = requireContext().resources.getIdentifier(platform.logo_res, "drawable", requireContext().packageName)
        if (logoResId != 0) {
            imgLogo.setImageResource(logoResId)
        } else {
            imgLogo.setImageResource(android.R.drawable.ic_menu_slideshow)
        }
        
        // Calculate Trend
        val prevPrice = StreamingPriceService.getPreviousPrice(requireContext(), platform.id)
        if (prevPrice != null && prevPrice > 0.0) {
            val changePercent = ((platform.base_price - prevPrice) / prevPrice) * 100
            if (changePercent > 0.05) {
                tvTrend.text = getString(R.string.price_increased, String.format(java.util.Locale.US, "%.0f%%", changePercent))
                tvTrend.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                tvTrend.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEAEA"))
                tvTrend.setTextColor(android.graphics.Color.parseColor("#FF5252"))
                tvTrend.visibility = View.VISIBLE
            } else if (changePercent < -0.05) {
                tvTrend.text = getString(R.string.price_decreased, String.format(java.util.Locale.US, "%.0f%%", Math.abs(changePercent)))
                tvTrend.setBackgroundResource(R.drawable.bg_shared_contact_pill)
                tvTrend.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAFBEA"))
                tvTrend.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvTrend.visibility = View.VISIBLE
            } else {
                tvTrend.visibility = View.GONE
            }
        } else {
            tvTrend.visibility = View.GONE
        }
        
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        
        // Populate plans
        plansContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (plan in platform.plans) {
            val planRow = inflater.inflate(R.layout.item_streaming_plan_row, plansContainer, false)
            val tvPlanName = planRow.findViewById<TextView>(R.id.tvPlanName)
            val tvPlanPrice = planRow.findViewById<TextView>(R.id.tvPlanPrice)
            val tvPlanPeriod = planRow.findViewById<TextView>(R.id.tvPlanPeriod)
            
            tvPlanName.text = plan.name
            
            // Display local price directly from JSON
            val planSymbol = CurrencyHelper.getLocalizedSymbol(plan.currency, requireContext())
            val formattedPrice = String.format(java.util.Locale.US, "%,.2f", plan.price)
            tvPlanPrice.text = "$formattedPrice $planSymbol"
            
            tvPlanPeriod.text = when (plan.period) {
                "monthly" -> getString(R.string.period_monthly_short)
                "yearly" -> getString(R.string.period_yearly_short)
                else -> "/${plan.period}"
            }
            plansContainer.addView(planRow)
        }
        
        // Web Link Button Dynamic Country URL
        btnWeb.setOnClickListener {
            try {
                val countryCode = activeLang.split("-").last().lowercase()
                var finalUrl = platform.official_url
                
                when (platform.id) {
                    "spotify" -> finalUrl = "https://www.spotify.com/$countryCode/premium/"
                    "apple_music" -> finalUrl = if (countryCode == "us") "https://www.apple.com/apple-music/" else "https://www.apple.com/$countryCode/apple-music/"
                    "prime_video" -> finalUrl = "https://www.primevideo.com/"
                    "netflix" -> finalUrl = "https://www.netflix.com/"
                }
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateBellIcon(imageView: ImageView, enabled: Boolean) {
        imageView.setImageResource(R.drawable.ic_bell)
        val colorStr = if (enabled) "#4CAF50" else "#FF5252"
        imageView.setColorFilter(android.graphics.Color.parseColor(colorStr))
    }
}
