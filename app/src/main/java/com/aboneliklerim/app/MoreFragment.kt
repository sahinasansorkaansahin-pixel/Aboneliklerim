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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.ProgressBar
import android.app.Activity
import com.aboneliklerim.app.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.graphics.drawable.GradientDrawable



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
            val manager = com.google.android.play.core.review.ReviewManagerFactory.create(requireContext())
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        // The flow has finished.
                    }
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${requireContext().packageName}")))
                    } catch (_: Exception) {}
                }
            }
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

        updateDateFormatUI()
        view.findViewById<android.widget.LinearLayout>(R.id.rowDateFormat).setOnClickListener {
            showDateFormatSelector()
        }

        updateDefaultCurrencyUI()
        view.findViewById<android.widget.LinearLayout>(R.id.rowDefaultCurrency)?.setOnClickListener {
            showDefaultCurrencySelector()
        }

        // --- BLUE LIGHT FILTER TOGGLE ---
        val swBlueLightFilter = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swBlueLightFilter)
        val rowBlueLightFilter = view.findViewById<LinearLayout>(R.id.rowBlueLightFilter)
        val displayPrefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        var isBlueLightActive = displayPrefs.getBoolean("blue_light_filter_enabled", false)
        swBlueLightFilter?.isChecked = isBlueLightActive

        rowBlueLightFilter?.setOnClickListener {
            isBlueLightActive = !isBlueLightActive
            swBlueLightFilter?.isChecked = isBlueLightActive
            displayPrefs.edit().putBoolean("blue_light_filter_enabled", isBlueLightActive).apply()
            val currentActivity = activity
            if (currentActivity is BaseActivity) {
                if (isBlueLightActive) {
                    currentActivity.showBlueLightFilter()
                } else {
                    currentActivity.hideBlueLightFilter()
                }
            }
        }

        // --- APP LOCK TOGGLE ---
        val swAppLock = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swAppLock)
        val rowAppLock = view.findViewById<LinearLayout>(R.id.rowAppLock)
        val settingsPrefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        var isLockEnabled = settingsPrefs.getBoolean("app_lock_enabled", false)
        swAppLock?.isChecked = isLockEnabled

        rowAppLock?.setOnClickListener {
            isLockEnabled = !isLockEnabled
            swAppLock?.isChecked = isLockEnabled
            settingsPrefs.edit().putBoolean("app_lock_enabled", isLockEnabled).apply()
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

        setupCurrencyConverter(view)
        setupNewsSection(view)
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

    private fun updateDefaultCurrencyUI() {
        val view = view ?: return
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        val currentCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)) ?: "USD"
        view.findViewById<TextView>(R.id.tvCurrentDefaultCurrency)?.text = currentCurrency
    }

    private fun showDefaultCurrencySelector() {
        val currencies = CurrencyHelper.getCurrencies(requireContext())
        val names = currencies.map { "${it.flag} ${it.name} (${it.code})" }.toTypedArray()
        
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val activeLang = LocaleHelper.getActiveLanguage(requireContext())
        val currentCurrency = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(activeLang)) ?: "USD"
        
        val checkedItem = currencies.indexOfFirst { it.code == currentCurrency }.coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_Aboneliklerim_Dialog)
            .setTitle(getString(R.string.default_currency))
            .setSingleChoiceItems(names, checkedItem) { dialog, which ->
                val selected = currencies[which].code
                prefs.edit().putString("default_currency", selected).apply()
                updateDefaultCurrencyUI()
                dialog.dismiss()
            }
            .show()
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

    private suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        val cleanLang = targetLang.substringBefore('-').lowercase().trim()
        if (cleanLang == "en" || text.isBlank()) {
            return@withContext text
        }
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$cleanLang&dt=t&q=$encodedText"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotEmpty()) {
                        val jsonArray = JsonParser.parseString(body).asJsonArray
                        val segments = jsonArray.get(0).asJsonArray
                        val sb = StringBuilder()
                        for (i in 0 until segments.size()) {
                            val segment = segments.get(i).asJsonArray
                            sb.append(segment.get(0).asString)
                        }
                        return@withContext sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MoreFragment", "Translation failed for language $cleanLang: $text", e)
        }
        return@withContext text
    }

    private suspend fun translateNewsItem(item: NewsItem, targetLang: String): NewsItem = kotlinx.coroutines.coroutineScope {
        val cleanLang = targetLang.substringBefore('-').lowercase().trim()
        if (cleanLang == "en" || item.type == "placeholder") return@coroutineScope item
        
        val isCurated = item.link == "https://www.playstation.com" ||
                        item.link == "https://www.netflix.com" ||
                        item.link == "https://www.spotify.com" ||
                        item.link == "https://www.primevideo.com"
        
        if (cleanLang == "tr" && isCurated) return@coroutineScope item
        
        val translatedTitleJob = async(Dispatchers.IO) { translateText(item.title, cleanLang) }
        val translatedDescJob = async(Dispatchers.IO) { translateText(item.description, cleanLang) }
        
        item.copy(
            title = translatedTitleJob.await(),
            description = translatedDescJob.await()
        )
    }

    private fun setupNewsSection(view: View) {
        val rvNews = view.findViewById<RecyclerView>(R.id.rvNews) ?: return
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbNewsLoading)
        val tvRefresh = view.findViewById<TextView>(R.id.tvRefreshNews)
        val etNewsSearch = view.findViewById<EditText>(R.id.etNewsSearch)
        val ivClearNewsSearch = view.findViewById<ImageView>(R.id.ivClearNewsSearch)
        
        rvNews.layoutManager = LinearLayoutManager(context)
        
        var currentSearchQuery = ""
        var searchJob: kotlinx.coroutines.Job? = null
        
        fun loadNews() {
            val query = currentSearchQuery.trim()
            val context = context ?: return
            val activeLang = LocaleHelper.getActiveLanguage(context)
            val targetLang = activeLang.substringBefore('-').lowercase().trim()
            val isTr = targetLang.startsWith("tr", ignoreCase = true)

            pbLoading?.visibility = View.VISIBLE
            tvRefresh?.visibility = View.GONE
            
            lifecycleScope.launch {
                val liveItems = mutableListOf<NewsItem>()
                
                withContext(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                            
                        val githubUrl = "https://raw.githubusercontent.com/sahinasansorkaansahin-pixel/Aboneliklerim/main/news.json"
                        val request = Request.Builder()
                            .url(githubUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .build()
                            
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                if (body.isNotEmpty()) {
                                    val items: List<NewsItem> = Gson().fromJson(
                                        body,
                                        object : TypeToken<List<NewsItem>>() {}.type
                                    )
                                    Log.d("MoreFragment", "Fetched ${items.size} items from GitHub")
                                    liveItems.addAll(items)
                                } else {
                                    Log.w("MoreFragment", "Empty body from GitHub news")
                                }
                            } else {
                                Log.w("MoreFragment", "Unsuccessful response from GitHub news: ${response.code}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MoreFragment", "Error fetching news from GitHub, using local fallback", e)
                    }
                    
                    if (liveItems.isEmpty()) {
                        liveItems.addAll(getCuratedFallbackNews(false))
                    }
                }
                
                // Filter duplicates in live fetched items by lowercase title.
                val distinctLive = liveItems.distinctBy { it.title.lowercase().trim().replace(Regex("[^a-z0-9]"), "") }
                Log.d("MoreFragment", "Total live: ${liveItems.size}, distinct: ${distinctLive.size}")
                
                val selectedItems = mutableListOf<NewsItem>()
                val activeQueryTerms = mutableListOf<String>()
                
                if (query.isEmpty()) {
                    // No search query: filter only targeted/relevant news about digital subscription/streaming/gaming
                    val matchingLive = distinctLive.filter { item ->
                        isTargetedNews(item.title, item.description, isTr)
                    }
                    Log.d("MoreFragment", "Matching live for empty query (targeted): ${matchingLive.size}")
                    
                    // Score and sort matching live items by regression model
                    val scoredMatches = matchingLive.mapIndexed { index, item ->
                        val baseScore = calculateNewsScore(item, index)
                        Pair(item, baseScore)
                    }.sortedByDescending { it.second }.map { it.first }
                    
                    selectedItems.addAll(scoredMatches)
                    
                    if (selectedItems.size < 4) {
                        val curatedList = getCuratedFallbackNews(isTr)
                        for (curated in curatedList) {
                            if (selectedItems.size >= 4) break
                            val isDuplicate = selectedItems.any { 
                                it.link == curated.link || 
                                it.title.lowercase().trim().replace(Regex("[^a-z0-9]"), "") == 
                                curated.title.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
                            }
                            if (!isDuplicate) {
                                selectedItems.add(curated)
                            }
                        }
                    }
                } else {
                    // Search query is active: filter matching search terms
                    // Smart Query Translation: translate query to English first to search English feeds effectively!
                    val englishQuery = if (targetLang != "en") {
                        translateText(query, "en")
                    } else {
                        query
                    }
                    
                    val queryTerms = (getQueryTerms(query) + getQueryTerms(englishQuery))
                        .map { it.lowercase().trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    
                    activeQueryTerms.addAll(queryTerms)
                    
                    // Compile regexes for short terms once outside the loop for maximum performance
                    val queryRegexes = queryTerms.map { term ->
                        if (term.length < 4) {
                            Pair(term, Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE))
                        } else {
                            Pair(term, null)
                        }
                    }
                    
                    val matchingLive = distinctLive.filter { item ->
                        val text = "${item.title} ${item.description}".lowercase()
                        queryRegexes.any { (term, regex) ->
                            if (regex != null) {
                                regex.containsMatchIn(text)
                            } else {
                                text.contains(term)
                            }
                        }
                    }
                    Log.d("MoreFragment", "Matching live for query '$query': ${matchingLive.size}")
                    
                    // Score and sort matching live items by regression model (adding extra query match weight)
                    val scoredMatches = matchingLive.mapIndexed { index, item ->
                        val baseScore = calculateNewsScore(item, index)
                        val boost = if (queryTerms.any { item.title.lowercase().contains(it) }) 50.0 else 20.0
                        Pair(item, baseScore + boost)
                    }.sortedByDescending { it.second }.map { it.first }
                    
                    selectedItems.addAll(scoredMatches)
                }
                
                // Add highly-polished empty-state placeholder if no matching news articles are found
                if (selectedItems.isEmpty()) {
                    val placeholderTitle = if (isTr) "Eşleşen Haber Bulunamadı" else "No Matching News Found"
                    val placeholderDesc = if (isTr) {
                        if (query.isEmpty()) "Şu an için güncel bir haber bulunamadı. Lütfen daha sonra tekrar deneyin."
                        else "'$query' ile ilgili herhangi bir güncel haber bulunamadı. Lütfen başka bir arama yapın."
                    } else {
                        if (query.isEmpty()) "No active news found at the moment. Please try again later."
                        else "No active news found matching '$query'. Please try a different query."
                    }
                    
                    selectedItems.add(
                        NewsItem(
                            title = placeholderTitle,
                            description = placeholderDesc,
                            link = "",
                            source = "Aboneliklerim",
                            date = "",
                            type = "placeholder"
                        )
                    )
                }
                
                // Final selection of exactly max 4 items
                val finalNewsList = selectedItems.take(4)
                
                // Dynamically translate the selected news items in parallel!
                val translatedNewsList = finalNewsList.map { item ->
                    async { translateNewsItem(item, targetLang) }
                }.awaitAll()
                
                rvNews.adapter = NewsAdapter(translatedNewsList, activeQueryTerms)
                
                pbLoading?.visibility = View.GONE
                tvRefresh?.visibility = View.VISIBLE
            }
        }
        
        tvRefresh?.setOnClickListener {
            loadNews()
        }
        
        // Search Input TextWatcher with 400ms Debounce
        etNewsSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""
                ivClearNewsSearch?.visibility = if (input.isNotEmpty()) View.VISIBLE else View.GONE
                
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(400)
                    if (input != currentSearchQuery) {
                        currentSearchQuery = input
                        loadNews()
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Clear Search Box click listener
        ivClearNewsSearch?.setOnClickListener {
            etNewsSearch?.setText("")
            ivClearNewsSearch.visibility = View.GONE
            currentSearchQuery = ""
            loadNews()
        }
        
        loadNews()
    }


    private fun calculateNewsScore(item: NewsItem, feedPosition: Int): Double {
        val text = "${item.title} ${item.description}".lowercase()
        
        // Feature 1: Platform mentions count
        val platforms = listOf(
            "netflix", "spotify", "disney", "hulu", "youtube", "hbo", "max", "prime", "amazon", "paramount", "apple tv",
            "apple music", "tidal", "deezer", "peacock", "crunchyroll", "audible", "exxen", "blutv", "gain", "tod",
            "playstation", "xbox", "game pass", "nintendo", "geforce now", "steam", "openai", "chatgpt"
        )
        val uniquePlatformsCount = platforms.count { text.contains(it) }
        val fPlatform = uniquePlatformsCount.toDouble()
        
        // Feature 2: Subscription keyword matches
        val subKeywords = listOf(
            "subscription", "membership", "premium", "digital platform", "streaming", "stream", "live", "service",
            "abonelik", "üye", "yayın platformu", "dijital platform", "paket", "hesap", "hesabı", "plan", "servis"
        )
        val subTermsCount = subKeywords.count { text.contains(it) }
        val fSubTerm = subTermsCount.toDouble()
        
        // Feature 3: Recency Score based on feed position (valuing new parsed news higher than fallback curated items)
        val fRecency = if (feedPosition >= 0) {
            Math.max(0.0, 1.0 - (feedPosition.toDouble() / 25.0))
        } else {
            0.5
        }
        
        // Feature 4: Title quality
        val fTitleLength = item.title.length.toDouble() / 100.0
        
        // Linear regression weights/coefficients for scoring model
        val w0 = 10.0
        val wPlatform = 30.0
        val wSubTerm = 15.0
        val wRecency = 15.0
        val wTitleLength = 5.0
        
        return w0 + (wPlatform * fPlatform) + (wSubTerm * fSubTerm) + (wRecency * fRecency) + (wTitleLength * fTitleLength)
    }

    private fun getArticleCategory(title: String, desc: String): String {
        val text = "$title $desc".lowercase()
        
        val videoKeywords = listOf("netflix", "disney", "hbo", "max", "prime video", "hulu", "peacock", "apple tv", "blutv", "exxen", "gain", "tod", "film", "dizi", "sinema", "movie", "series", "show")
        val musicKeywords = listOf("spotify", "apple music", "youtube music", "music", "müzik", "podcast", "tidal", "deezer", "audible", "song", "şarkı", "albüm")
        val gamingKeywords = listOf("playstation", "xbox", "game pass", "nintendo", "geforce now", "switch", "ea play", "game", "oyun", "steam")
        
        return when {
            videoKeywords.any { text.contains(it) } -> "video_streaming"
            musicKeywords.any { text.contains(it) } -> "music_audio"
            gamingKeywords.any { text.contains(it) } -> "gaming_interactive"
            else -> "general_digital"
        }
    }

    private fun getQueryTerms(query: String): List<String> {
        val q = query.lowercase().trim()
        val terms = mutableListOf(q)
        when (q) {
            "playstation", "ps", "ps5", "ps4" -> {
                terms.addAll(listOf("playstation", "ps5", "ps4", "ps plus", "playstation plus", "sony interactive", "dualsense", "dualshock"))
            }
            "xbox", "game pass", "gamepass" -> {
                terms.addAll(listOf("xbox", "game pass", "gamepass", "series x", "series s", "microsoft gaming", "xbox live"))
            }
            "spotify" -> {
                terms.addAll(listOf("spotify", "music streaming", "playlist", "daniel ek"))
            }
            "netflix" -> {
                terms.addAll(listOf("netflix", "streaming giant", "squid game", "netflix premium"))
            }
            "chatgpt", "openai", "gpt" -> {
                terms.addAll(listOf("chatgpt", "openai", "gpt-4", "gpt-4o", "sam altman", "generative ai"))
            }
        }
        return terms.distinct()
    }

    private fun highlightText(text: String, searchTerms: List<String>): android.text.Spannable {
        val spannable = android.text.SpannableStringBuilder(text)
        if (searchTerms.isEmpty()) return spannable
        
        val lowerText = text.lowercase()
        for (term in searchTerms) {
            if (term.length < 3) continue // Avoid highlighting short words
            var startIdx = lowerText.indexOf(term)
            while (startIdx != -1) {
                val endIdx = startIdx + term.length
                
                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    startIdx,
                    endIdx,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                startIdx = lowerText.indexOf(term, endIdx)
            }
        }
        return spannable
    }

    private fun determineNewsType(title: String, desc: String): String {
        val fullText = "$title $desc".lowercase()
        return when {
            fullText.contains("hike") || fullText.contains("increase") || fullText.contains("raise") || fullText.contains("price") || fullText.contains("zam") || fullText.contains("fiyat") || fullText.contains("güncelleme") || fullText.contains("artış") -> "hike"
            fullText.contains("discount") || fullText.contains("save") || fullText.contains("deal") || fullText.contains("sale") || fullText.contains("off") || fullText.contains("indirim") || fullText.contains("ucuz") || fullText.contains("fırsat") -> "discount"
            else -> "campaign"
        }
    }

    private fun isTargetedNews(title: String, desc: String, isTr: Boolean): Boolean {
        val fullText = "$title $desc".lowercase()
        val platforms = listOf(
            "netflix", "spotify", "disney", "hulu", "youtube", "hbo", "max", "prime", "amazon", "paramount", "apple tv",
            "apple music", "tidal", "deezer", "peacock", "crunchyroll", "audible", "premium", "subscription", "abonelik",
            "exxen", "blutv", "gain", "tod", "playstation", "xbox", "game pass", "nintendo", "geforce now", "steam", "openai", "chatgpt"
        )
        
        val matchesPlatform = platforms.any { fullText.contains(it) }
        if (!matchesPlatform) return false
        
        // Blocklist to avoid general hardware, device, and retail product deals
        val blocklist = if (isTr) {
            listOf("telefon", "akıllı telefon", "xiaomi", "oppo", "samsung", "iphone", "huawei", "realme", "redmi", "vivo", "ekran kartı", "pc", "laptop", "bilgisayar", "araba", "otomobil", "motor", "ppf", "kaplama", "saat", "akıllı saat", "süpürge", "kulaklık", "donanım", "2. el", "akıllı ev", "akıllı süpürge", "kulaklığı")
        } else {
            listOf("phone", "smartphone", "xiaomi", "samsung", "iphone", "laptop", "monitor", "gpu", "nvidia", "intel", "amd", "macbook", "grill", "patio", "lawn", "watch", "drone", "vacuum", "headphone", "earbud", "charger", "soundbar", "speaker", "fitness tracker", "smart home")
        }
        
        val matchesBlock = blocklist.any { fullText.contains(it) }
        return !matchesBlock
    }

    private fun getCuratedFallbackNews(isTr: Boolean): List<NewsItem> {
        return if (isTr) {
            listOf(
                NewsItem(
                    title = "PlayStation Plus Abonelik Ücretlerinde Yeni Düzenleme Yapıldı",
                    description = "Sony, PlayStation Plus üyelik paketlerinin fiyatlarında ve kütüphanesinde yeni bir güncellemeye gitti. Güncel oyun katalogları aboneleri bekliyor.",
                    link = "https://www.playstation.com",
                    source = "PlayStation",
                    date = "Mayıs 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Netflix, Premium Paketine Yeni Yapay Zeka Özellikleri Ekliyor",
                    description = "Dijital yayın devi Netflix, abonelerinin içerikleri daha kolay keşfetmesini sağlayacak akıllı kişiselleştirme özelliklerini duyurdu.",
                    link = "https://www.netflix.com",
                    source = "Netflix",
                    date = "Mayıs 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Spotify Premium için Akıllı Çalma Listesi Özelliği Geliyor",
                    description = "Spotify, Premium kullanıcılarına özel yapay zeka destekli akıllı çalma listesi oluşturma özelliğini dünya genelinde yaygınlaştırıyor.",
                    link = "https://www.spotify.com",
                    source = "Spotify",
                    date = "Mayıs 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Amazon Prime Video Yeni Popüler Dizilerini ve Fırsatlarını Açıkladı",
                    description = "Amazon Prime Video, önümüzdeki dönemde kütüphanesine eklenecek yeni orijinal yapımlarının takvimini ve üyelik avantajlarını paylaştı.",
                    link = "https://www.primevideo.com",
                    source = "Prime Video",
                    date = "Mayıs 2026",
                    type = "campaign"
                )
            )
        } else {
            listOf(
                NewsItem(
                    title = "PlayStation Plus Subscription Plans Receive Pricing and Catalog Updates",
                    description = "Sony has announced new updates for PlayStation Plus plans. A rich catalog of newly added games and exclusive member benefits awaits active subscribers.",
                    link = "https://www.playstation.com",
                    source = "PlayStation",
                    date = "May 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Netflix Enhances Premium Experience with New Smart Recommendation Tools",
                    description = "Streaming giant Netflix is rolling out brand new personalized features designed to help Premium members discover content more intuitively.",
                    link = "https://www.netflix.com",
                    source = "Netflix",
                    date = "May 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Spotify Premium Expands AI-Powered Smart Playlist Tool to More Users",
                    description = "Spotify is widely expanding its text-prompted AI playlist generator tool, allowing Premium users to instantly build tailored music lists.",
                    link = "https://www.spotify.com",
                    source = "Spotify",
                    date = "May 2026",
                    type = "campaign"
                ),
                NewsItem(
                    title = "Amazon Prime Video Showcases Upcoming Premium Original Series and Deals",
                    description = "Amazon Prime Video has officially announced dates and preview details for its highly anticipated list of original series and platform benefits.",
                    link = "https://www.primevideo.com",
                    source = "Prime Video",
                    date = "May 2026",
                    type = "campaign"
                )
            )
        }
    }

    inner class NewsAdapter(
        private val items: List<NewsItem>,
        private val searchTerms: List<String> = emptyList()
    ) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
        
        inner class NewsViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvSource: TextView = itemView.findViewById(R.id.tvNewsSource)
            val tvCategory: TextView = itemView.findViewById(R.id.tvNewsCategory)
            val tvDate: TextView = itemView.findViewById(R.id.tvNewsDate)
            val tvTitle: TextView = itemView.findViewById(R.id.tvNewsTitle)
            val tvDesc: TextView = itemView.findViewById(R.id.tvNewsDesc)
            val divider: View = itemView.findViewById(R.id.vNewsDivider)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
            return NewsViewHolder(view)
        }

        override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context
            
            if (item.type == "placeholder") {
                holder.tvTitle.text = item.title
                holder.tvDesc.text = item.description
                holder.tvDate.text = ""
                holder.tvSource.text = ""
                
                holder.tvSource.visibility = View.GONE
                holder.tvDate.visibility = View.GONE
                holder.tvCategory.visibility = View.GONE
                holder.divider.visibility = View.GONE
                
                holder.tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                holder.tvDesc.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                
                holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL)
                holder.tvTitle.gravity = android.view.Gravity.CENTER
                holder.tvDesc.gravity = android.view.Gravity.CENTER
                
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
                holder.itemView.background = null
                holder.itemView.setOnClickListener(null)
            } else {
                holder.tvTitle.text = highlightText(item.title, searchTerms)
                holder.tvDesc.text = highlightText(item.description, searchTerms)
                holder.tvDate.text = item.date
                holder.tvSource.text = item.source
                
                holder.tvSource.visibility = View.VISIBLE
                holder.tvDate.visibility = View.VISIBLE
                holder.tvCategory.visibility = View.GONE
                holder.divider.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE
                
                holder.tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_main))
                holder.tvDesc.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.tvTitle.gravity = android.view.Gravity.START
                holder.tvDesc.gravity = android.view.Gravity.START
                
                val sourceBgColor = android.graphics.Color.parseColor("#1A730692")
                val sourceTextColor = android.graphics.Color.parseColor("#730692")
                holder.tvSource.background = createBadgeDrawable(sourceBgColor, 6f, context)
                holder.tvSource.setTextColor(sourceTextColor)
                
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
                
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val typedArray = context.obtainStyledAttributes(attrs)
                val backgroundResource = typedArray.getDrawable(0)
                holder.itemView.background = backgroundResource
                typedArray.recycle()
                
                holder.itemView.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Link cannot be opened", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size
        
        private fun createBadgeDrawable(backgroundColor: Int, cornerRadiusDp: Float, context: Context): GradientDrawable {
            val gd = GradientDrawable()
            gd.setColor(backgroundColor)
            val density = context.resources.displayMetrics.density
            gd.cornerRadius = cornerRadiusDp * density
            return gd
        }
    }

}

data class NewsItem(
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val date: String,
    val type: String
)
