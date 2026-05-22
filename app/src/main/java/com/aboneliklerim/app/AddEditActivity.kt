package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import com.aboneliklerim.app.Subscription
import com.aboneliklerim.app.SharedContact
import com.bumptech.glide.Glide

class ColorPickerAdapter(
    private val colors: List<String>,
    private var selectedColor: String,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val circle: View = view.findViewById(R.id.viewColorCircle)
        val check: View = view.findViewById(R.id.ivSelectedCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_color_circle, parent, false)
        return ColorViewHolder(v)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        holder.circle.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
        holder.check.visibility = if (color.lowercase() == selectedColor.lowercase()) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            selectedColor = color
            onColorSelected(color)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = colors.size
}

class AddEditActivity : BaseActivity() {

    private var editId: String? = null
    private var subscriptions = mutableListOf<Subscription>()
    
    private var currentSharedCount = 1
    private var currentSharedContacts = mutableListOf<SharedContact>()

    private var selectedCurrency = "TRY"
    private var selectedColor = "#6366f1"
    private var selectedImagePath: String? = null
    private var isRegularMode = true
    private var selectedCategory = ""
    private lateinit var tagsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var isInitialLoad = true 
    
    // Sekmeler arası bildirim hafızası
    private var lastRegularNotifyDays = -1
    private var lastOneTimeNotifyDays = -1

    private val COLOR_PALETTE = listOf(
        "#FFFFFF", "#FF5252", "#E91E63", "#F06292", 
        "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
        "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
        "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107",
        "#FF9800", "#795548", "#607D8B", "#9E9E9E"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        selectedCategory = getString(R.string.general_cat)
        findViewById<TextView>(R.id.btnTagsTriggerText).text = selectedCategory

        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val defaultCurr = prefs.getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(LocaleHelper.getActiveLanguage(this))) ?: "USD"
        selectedCurrency = defaultCurr

        editId = intent.getStringExtra("sub_id")
        
        tagsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: androidx.activity.result.ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val tag = result.data?.getStringExtra("selected_tag")
                if (tag != null) {
                    selectedCategory = tag
                    findViewById<TextView>(R.id.btnTagsTriggerText).text = tag
                }
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: androidx.activity.result.ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {}
                    selectedImagePath = uri.toString()
                    updateImagePreview()
                }
            }
        }

        lifecycleScope.launch {
            loadData()
            initUI()
        }
    }

    private fun initUI() {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        if (editId == null) {
            val isPremium = prefs.getBoolean("is_premium_active", false)
            val limit = if (isPremium) 100 else 8
            if (subscriptions.size >= limit) {
                if (!isPremium) {
                    startActivity(Intent(this, PremiumActivity::class.java))
                } else {
                    Toast.makeText(this, getString(R.string.premium_limit_max), Toast.LENGTH_LONG).show()
                }
                finish()
                return
            }
        }

        val etPrice = findViewById<EditText>(R.id.etPrice)
        val spinnerPeriod = findViewById<Spinner>(R.id.spinnerPeriod)
        val etNote = findViewById<EditText>(R.id.etNote)
        val tvNoteCharCount = findViewById<TextView>(R.id.tvNoteCharCount)
        val etName = findViewById<EditText>(R.id.etName)
        val tvNameCharCount = findViewById<TextView>(R.id.tvNameCharCount)
        val etPaymentMethod = findViewById<EditText>(R.id.etPaymentMethod)
        val tvPaymentCharCount = findViewById<TextView>(R.id.tvPaymentCharCount)

        etNote.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                tvNoteCharCount.text = "${s?.length ?: 0}/35"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val tvAddEditIcon = findViewById<TextView>(R.id.tvAddEditIcon)
        etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                tvNameCharCount.text = "${s?.length ?: 0}/20"
                val text = s?.toString()?.trim() ?: ""
                tvAddEditIcon?.text = if (text.isEmpty()) "A" else text.take(1).uppercase()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etPaymentMethod.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                tvPaymentCharCount.text = "${s?.length ?: 0}/20"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvCurrencyLabel = findViewById<TextView>(R.id.tvCurrencyLabel)
        val tvCurrencySymbol = findViewById<TextView>(R.id.tvCurrencySymbol)
        tvCurrencyLabel.text = selectedCurrency
        tvCurrencySymbol.text = CurrencyHelper.getLocalizedSymbol(selectedCurrency, this)
        
        etPrice.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() != current) {
                    var cleanString = s.toString().replace("[^\\d]".toRegex(), "")
                    if (cleanString.length > 6) { 
                        cleanString = cleanString.substring(0, 6)
                    }
                    
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val nf = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
                                minimumFractionDigits = 2
                                maximumFractionDigits = 2
                            }
                            val formatted = nf.format(parsed / 100)
                            current = formatted
                            etPrice.setText(formatted)
                            etPrice.setSelection(formatted.length)
                        } catch (_: Exception) {}
                    } else {
                        current = ""
                        etPrice.setText("")
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        if (editId == null) {
            // Keep default "Yeni Abonelik" title
        } else {
            // Bug 3 fix: change title in edit mode
            val titleView = findViewById<TextView>(R.id.tvAddEditTitle)
            titleView?.text = getString(R.string.edit_subscription)
        }
        val tvTabRegular = findViewById<TextView>(R.id.tvTabRegular)
        val tvTabOneTime = findViewById<TextView>(R.id.tvTabOneTime)
        val tabIndicator = findViewById<View>(R.id.tabIndicator)
        val layoutPeriodSection = findViewById<LinearLayout>(R.id.layoutPeriodSection)
        val tvDateLabel = findViewById<TextView>(R.id.tvDateLabel)
        val tvDateValue = findViewById<TextView>(R.id.tvDateValue)
        val btnTagsTrigger = findViewById<View>(R.id.btnTagsTrigger)
        val etPeriodValue = findViewById<EditText>(R.id.etPeriodValue)

        val spinnerNotify = findViewById<Spinner>(R.id.spinnerNotify)
        val allNotifyOptions = listOf(
            getString(R.string.off), 
            getString(R.string.same_day), 
            getString(R.string.one_day_before), 
            getString(R.string.three_days_before), 
            getString(R.string.seven_days_before)
        )
        val allNotifyDays = listOf(-1, 0, 1, 3, 7)

        fun updateNotifySpinner(maxDays: Int, initialValue: Int? = null) {
            val filteredOptions = mutableListOf<String>()
            val filteredDays = mutableListOf<Int>()
            
            for (i in allNotifyDays.indices) {
                if (allNotifyDays[i] < maxDays) {
                    filteredOptions.add(allNotifyOptions[i])
                    filteredDays.add(allNotifyDays[i])
                }
            }
            
            val adapter = ArrayAdapter(this, R.layout.spinner_item_white, filteredOptions)
            adapter.setDropDownViewResource(R.layout.spinner_item_white)
            spinnerNotify.adapter = adapter
            spinnerNotify.tag = filteredDays

            // Eğer bir başlangıç değeri geldiyse onu seç
            initialValue?.let { days ->
                val index = filteredDays.indexOf(days)
                if (index != -1) {
                    spinnerNotify.setSelection(index)
                }
            }
        }

        // Custom Tab Logic
        fun getMaxDaysForPeriod(periodPos: Int, pv: Int): Int {
            val baseDays = when (periodPos) { 0 -> 1; 1 -> 7; 2 -> 28; else -> 360 }
            return baseDays * pv
        }

        fun updateTabs(regular: Boolean) {
            // Save current notify selection before switching
            val currentNotifyDays = spinnerNotify.tag as? List<Int>
            val currentSelection = if (currentNotifyDays != null && spinnerNotify.selectedItemPosition < currentNotifyDays.size) {
                currentNotifyDays[spinnerNotify.selectedItemPosition]
            } else -1

            if (isRegularMode) lastRegularNotifyDays = currentSelection
            else lastOneTimeNotifyDays = currentSelection

            isRegularMode = regular
            if (regular) {
                tvTabRegular.setTextColor(Color.parseColor("#730692"))
                tvTabOneTime.setTextColor(Color.parseColor("#88730692"))
                layoutPeriodSection.visibility = View.VISIBLE
                tvDateLabel.text = getString(R.string.first_payment)

                val params = tabIndicator.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_START, R.id.tvTabRegular)
                params.addRule(RelativeLayout.ALIGN_END, R.id.tvTabRegular)
                tabIndicator.layoutParams = params

                val pv = etPeriodValue.text.toString().toIntOrNull() ?: 1
                val maxDays = getMaxDaysForPeriod(spinnerPeriod.selectedItemPosition, pv)
                updateNotifySpinner(maxDays, lastRegularNotifyDays)
            } else {
                tvTabRegular.setTextColor(Color.parseColor("#88730692"))
                tvTabOneTime.setTextColor(Color.parseColor("#730692"))
                layoutPeriodSection.visibility = View.GONE
                tvDateLabel.text = getString(R.string.first_payment_date)

                val params = tabIndicator.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_START, R.id.tvTabOneTime)
                params.addRule(RelativeLayout.ALIGN_END, R.id.tvTabOneTime)
                tabIndicator.layoutParams = params

                updateNotifySpinner(365, lastOneTimeNotifyDays)
            }
        }

        tvTabRegular.setOnClickListener { updateTabs(true) }
        tvTabOneTime.setOnClickListener { updateTabs(false) }
        
        btnTagsTrigger.setOnClickListener {
            val intent = Intent(this, TagsActivity::class.java)
            tagsLauncher.launch(intent)
        }

        val cardAddEditIcon = findViewById<View>(R.id.cardAddEditIcon)
        val cardAddEditColor = findViewById<View>(R.id.cardAddEditColor)
        cardAddEditIcon?.setOnClickListener { showImageOptionsDialog() }
        cardAddEditColor?.setOnClickListener { showColorPicker() }

        // Initial tab state - ONLY for new subscriptions
        if (editId == null) {
            tvTabRegular.post { updateTabs(true) }
        }

        // Spinners
        val periods = listOf(getString(R.string.daily), getString(R.string.weekly), getString(R.string.monthly), getString(R.string.yearly))
        val periodAdapter = ArrayAdapter(this, R.layout.spinner_item_white, periods)
        periodAdapter.setDropDownViewResource(R.layout.spinner_item_white)
        spinnerPeriod.adapter = periodAdapter


        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Bug 4 fix: only update notify spinner when in Regular mode
                if (!isInitialLoad && isRegularMode) {
                    val pv = etPeriodValue.text.toString().toIntOrNull() ?: 1
                    val maxDays = when (position) {
                        0 -> 1 * pv
                        1 -> 7 * pv
                        2 -> 28 * pv
                        else -> 360 * pv
                    }
                    updateNotifySpinner(maxDays, lastRegularNotifyDays)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Bildirim seçimi değiştikçe hafızayı güncelle
        spinnerNotify.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitialLoad) {
                    val currentNotifyDays = spinnerNotify.tag as? List<Int>
                    val currentSelection = if (currentNotifyDays != null && position < currentNotifyDays.size) {
                        currentNotifyDays[position]
                    } else -1

                    if (isRegularMode) {
                        lastRegularNotifyDays = currentSelection
                    } else {
                        lastOneTimeNotifyDays = currentSelection
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // Trigger initial state only for NEW subscriptions
        if (editId == null) {
            updateNotifySpinner(28)
        }

        // Also update notify when periodValue text changes
        etPeriodValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isInitialLoad && isRegularMode) {
                    val pv = s.toString().toIntOrNull() ?: 1
                    val maxDays = when (spinnerPeriod.selectedItemPosition) {
                        0 -> 1 * pv; 1 -> 7 * pv; 2 -> 28 * pv; else -> 360 * pv
                    }
                    updateNotifySpinner(maxDays, lastRegularNotifyDays)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Currency Picker — tap the label to open picker
        tvCurrencyLabel.setOnClickListener { showCurrencyPicker() }

        // Date selection
        var selectedDate = Calendar.getInstance()

        fun updateDateDisplay() {
            tvDateValue.text = DateFormatHelper.formatForDisplay(this, selectedDate.time)
        }
        updateDateDisplay()

        tvDateValue.setOnClickListener {
            val picker = android.app.DatePickerDialog(this, R.style.PurpleDatePickerTheme, { _, y, m, d ->
                selectedDate.set(y, m, d)
                updateDateDisplay()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
            picker.show()
        }

        val tvNotifyTime = findViewById<TextView>(R.id.tvNotifyTime)
        var selectedNotifyTime = "09:00"

        tvNotifyTime.setOnClickListener {
            val parts = selectedNotifyTime.split(":")
            val h = parts[0].toIntOrNull() ?: 9
            val m = parts[1].toIntOrNull() ?: 0
            
            val picker = android.app.TimePickerDialog(
                this, 
                R.style.PurpleDatePickerTheme, 
                { _, hourOfDay, minute ->
                    selectedNotifyTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    tvNotifyTime.text = selectedNotifyTime
                }, 
                h, m, true
            )
            picker.show()
        }

        val tvSharedCount = findViewById<TextView>(R.id.tvSharedCount)
        val btnPlusShared = findViewById<TextView>(R.id.btnPlusShared)
        val btnMinusShared = findViewById<TextView>(R.id.btnMinusShared)

        btnPlusShared.setOnClickListener {
            val isPremium = prefs.getBoolean("is_premium_active", false)
            if (!isPremium) {
                startActivity(Intent(this, PremiumActivity::class.java))
                return@setOnClickListener
            }
            if (currentSharedCount < 4) {
                currentSharedCount++
                updateSharedUI()
            }
        }
        btnMinusShared.setOnClickListener {
            if (currentSharedCount > 1) {
                currentSharedCount--
                if (currentSharedContacts.size > currentSharedCount - 1) {
                    currentSharedContacts = currentSharedContacts.take(currentSharedCount - 1).toMutableList()
                    renderSharedContacts()
                }
                updateSharedUI()
            }
        }

        findViewById<View>(R.id.btnAddSharedContact).setOnClickListener {
            if (currentSharedContacts.size < currentSharedCount - 1) {
                val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                try {
                    startActivityForResult(intent, 2001)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.toast_contacts_not_found), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_max_contacts_reached), Toast.LENGTH_SHORT).show()
            }
        }

        // Edit mode
        if (editId != null) {
            val sub = subscriptions.find { it.id == editId }
            sub?.let {
                currentSharedCount = java.lang.Math.max(1, it.sharedWith)
                currentSharedContacts = (it.sharedContacts ?: emptyList()).toMutableList()
                updateSharedUI()
                
                etName.setText(it.name)
                tvNameCharCount.text = "${it.name.length}/20"
                etNote.setText(it.note) 
                tvNoteCharCount.text = "${it.note?.length ?: 0}/35"
                etPaymentMethod.setText(it.paymentMethod)
                tvPaymentCharCount.text = "${it.paymentMethod?.length ?: 0}/20"
                
                // Format price for the text watcher
                val priceCents = (it.price * 100).toLong().toString()
                etPrice.setText(priceCents)
                
                selectedCurrency = it.currency ?: "TRY"
                tvCurrencyLabel.text = it.currency ?: "TRY"
                tvCurrencySymbol.text = CurrencyHelper.getLocalizedSymbol(it.currency ?: "TRY", this)
                selectedColor = it.color
                updateColorTriggerUI()
                
                if (it.period == "one-time") {
                    lastOneTimeNotifyDays = it.notifyDaysBefore
                    isRegularMode = false
                    findViewById<LinearLayout>(R.id.layoutPeriodSection).visibility = View.GONE
                    findViewById<TextView>(R.id.tvDateLabel).text = getString(R.string.first_payment_date)
                    
                    tvTabRegular.setTextColor(Color.parseColor("#88730692"))
                    tvTabOneTime.setTextColor(Color.parseColor("#730692"))
                    
                    tabIndicator.post {
                        val params = tabIndicator.layoutParams as RelativeLayout.LayoutParams
                        params.addRule(RelativeLayout.ALIGN_START, R.id.tvTabOneTime)
                        params.addRule(RelativeLayout.ALIGN_END, R.id.tvTabOneTime)
                        tabIndicator.layoutParams = params
                    }
                    updateNotifySpinner(365, it.notifyDaysBefore)
                } else {
                    lastRegularNotifyDays = it.notifyDaysBefore
                    isRegularMode = true
                    findViewById<LinearLayout>(R.id.layoutPeriodSection).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvDateLabel).text = getString(R.string.first_payment)

                    val pos = when(it.period) {
                        "daily" -> 0; "weekly" -> 1; "monthly" -> 2; "yearly" -> 3; else -> 2
                    }
                    spinnerPeriod.setSelection(pos)
                    // Bug 2 fix: restore periodValue
                    etPeriodValue.setText(it.periodValue.toString())

                    tvTabRegular.setTextColor(Color.parseColor("#730692"))
                    tvTabOneTime.setTextColor(Color.parseColor("#88730692"))

                    tabIndicator.post {
                        val params = tabIndicator.layoutParams as RelativeLayout.LayoutParams
                        params.addRule(RelativeLayout.ALIGN_START, R.id.tvTabRegular)
                        params.addRule(RelativeLayout.ALIGN_END, R.id.tvTabRegular)
                        tabIndicator.layoutParams = params
                    }
                    val pv = it.periodValue
                    val maxDays = when(it.period) {
                        "daily" -> 1 * pv; "weekly" -> 7 * pv; "monthly" -> 28 * pv; else -> 360 * pv
                    }
                    updateNotifySpinner(maxDays, it.notifyDaysBefore)
                }

                try {
                    val sdfSave = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = sdfSave.parse(it.startDate)
                    if (date != null) {
                        selectedDate.time = date
                        updateDateDisplay()
                    }
                } catch (_: Exception) {}

                selectedNotifyTime = it.notifyTime
                tvNotifyTime.text = selectedNotifyTime

                selectedCategory = it.category
                findViewById<TextView>(R.id.btnTagsTriggerText).text = it.category

                selectedImagePath = it.imagePath
                selectedColor = it.color
                updateColorTriggerUI()
                updateImagePreview()
                
                // End initial load after a delay to ensure spinner listeners don't fire prematurely
                spinnerNotify.postDelayed({ isInitialLoad = false }, 200)
            }
        } else {
            isInitialLoad = false
            updateColorTriggerUI()
            updateImagePreview()
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            var name = etName.text.toString().trim()
            val cleanString = etPrice.text.toString().replace("[^\\d]".toRegex(), "")
            val price = cleanString.toDoubleOrNull()?.div(100.0)

            val isPremium = getSharedPreferences("Settings", Context.MODE_PRIVATE).getBoolean("is_premium_active", false)
            val limit = if (isPremium) 100 else 8
            if (editId == null && subscriptions.size >= limit) {
                if (!isPremium) {
                    startActivity(Intent(this, PremiumActivity::class.java))
                } else {
                    Toast.makeText(this, getString(R.string.premium_limit_max), Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }

            if (name.isEmpty()) { etName.error = getString(R.string.error_name_required); return@setOnClickListener }
            if (price == null || price <= 0) { etPrice.error = getString(R.string.error_invalid_amount); return@setOnClickListener }

            val pType = if (isRegularMode) {
                when (spinnerPeriod.selectedItemPosition) { 0 -> "daily"; 1 -> "weekly"; 2 -> "monthly"; else -> "yearly" }
            } else "one-time"
            // Bug 2 fix: read periodValue from input
            val pValue = etPeriodValue.text.toString().toIntOrNull()?.coerceIn(1, 99) ?: 1

            val currentNotifyDaysList = spinnerNotify.tag as? List<Int> ?: listOf(-1, 0, 1, 3, 7)
            val notifyDays = currentNotifyDaysList[spinnerNotify.selectedItemPosition]
            
            var finalNotifyTime = selectedNotifyTime

            // --- CONFLICT CHECK LOGIC ---
            if (notifyDays >= 0) {
                val existingTriggerTimes = subscriptions
                    .filter { it.id != editId && !it.isArchived && it.notifyDaysBefore >= 0 }
                    .mapNotNull { NotificationScheduler.calculateNextTriggerTime(it) }

                fun checkConflict(timeStr: String): Long? {
                    val testSub = Subscription(
                        id = "temp", name = name, price = price, period = pType, category = selectedCategory,
                        startDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time),
                        notifyDaysBefore = notifyDays, notifyTime = timeStr
                    )
                    return NotificationScheduler.calculateNextTriggerTime(testSub)
                }

                var currentTrigger = checkConflict(finalNotifyTime)
                
                while (currentTrigger != null && existingTriggerTimes.contains(currentTrigger)) {
                    // Add 1 minute
                    val parts = finalNotifyTime.split(":")
                    var h = parts[0].toInt()
                    var m = parts[1].toInt()
                    m++
                    if (m >= 60) { m = 0; h++ }
                    if (h >= 24) { h = 0 }
                    finalNotifyTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                    currentTrigger = checkConflict(finalNotifyTime)
                }
            }

            val sub = Subscription(
                id = editId ?: System.currentTimeMillis().toString(),
                name = name,
                price = price,
                period = pType,
                periodValue = pValue,
                category = selectedCategory,
                startDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time),
                currency = selectedCurrency,
                color = selectedColor,
                notifyDaysBefore = notifyDays,
                notifyTime = finalNotifyTime,
                paymentMethod = etPaymentMethod.text.toString().trim(),
                note = etNote.text.toString(),
                isArchived = if (editId != null) subscriptions.find { it.id == editId }?.isArchived ?: false else false,
                sharedWith = currentSharedCount,
                sharedContacts = currentSharedContacts.toList(),
                imagePath = selectedImagePath
            )

            if (editId != null) subscriptions.removeAll { it.id == editId }
            subscriptions.add(sub)
            saveData()
            NotificationScheduler.scheduleAlarms(this)
            SubscriptionWidget.updateWidget(this)
            SubscriptionOneTimeWidget.updateWidget(this)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showColorPicker() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvColors)
        rv.layoutManager = GridLayoutManager(this, 4)
        
        val adapter = ColorPickerAdapter(COLOR_PALETTE, selectedColor) { color ->
            selectedColor = color
            updateColorTriggerUI()
        }
        rv.adapter = adapter

        view.findViewById<Button>(R.id.btnSelectColor).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnCustomColor).setOnClickListener {
            showCustomColorDialog { color ->
                selectedColor = color
                updateColorTriggerUI()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showCustomColorDialog(onColorPicked: (String) -> Unit) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvColorPickerTitle)?.text = getString(R.string.custom_color)
        view.findViewById<RecyclerView>(R.id.rvColors).visibility = View.GONE
        
        val container = view.findViewById<LinearLayout>(android.R.id.content) ?: view as LinearLayout
        val picker = ColorPickerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600).apply {
                setMargins(0, 0, 0, 48)
            }
        }
        container.addView(picker, 2)

        view.findViewById<Button>(R.id.btnSelectColor).setOnClickListener {
            val colorInt = picker.getColor()
            val hex = String.format("#%06X", (0xFFFFFF and colorInt))
            onColorPicked(hex)
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCustomColor).visibility = View.GONE

        dialog.show()
    }

    private fun updateColorTriggerUI() {
        val viewCircle = findViewById<View>(R.id.viewSelectedColorCircle)
        val tvIcon = findViewById<TextView>(R.id.tvAddEditIcon)
        
        try {
            val colorInt = Color.parseColor(selectedColor)
            val csl = android.content.res.ColorStateList.valueOf(colorInt)
            
            viewCircle?.backgroundTintList = csl
            tvIcon?.backgroundTintList = csl
            
            // Contrast check for tvIcon's text color
            val darkness = 1 - (0.299 * Color.red(colorInt) + 0.587 * Color.green(colorInt) + 0.114 * Color.blue(colorInt)) / 255
            tvIcon?.setTextColor(if (darkness < 0.5) Color.BLACK else Color.WHITE)
        } catch (e: Exception) {}
    }

    private fun updateImagePreview() {
        val ivAddEditLogo = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivAddEditLogo)
        val tvAddEditIcon = findViewById<TextView>(R.id.tvAddEditIcon)
        
        if (!selectedImagePath.isNullOrEmpty()) {
            ivAddEditLogo?.visibility = View.VISIBLE
            tvAddEditIcon?.visibility = View.GONE
            try {
                Glide.with(this)
                    .load(android.net.Uri.parse(selectedImagePath))
                    .circleCrop()
                    .placeholder(R.drawable.circle_color)
                    .into(ivAddEditLogo)
            } catch (e: Exception) {
                ivAddEditLogo?.visibility = View.GONE
                tvAddEditIcon?.visibility = View.VISIBLE
            }
        } else {
            ivAddEditLogo?.visibility = View.GONE
            tvAddEditIcon?.visibility = View.VISIBLE
        }
    }

    private fun showImageOptionsDialog() {
        val options = if (selectedImagePath.isNullOrEmpty()) {
            arrayOf(getString(R.string.add_photo))
        } else {
            arrayOf(getString(R.string.change_photo), getString(R.string.remove_photo))
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_photo))
            .setItems(options) { dialog, which ->
                if (options[which] == getString(R.string.add_photo) || options[which] == getString(R.string.change_photo)) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    imagePickerLauncher.launch(intent)
                } else if (options[which] == getString(R.string.remove_photo)) {
                    selectedImagePath = null
                    updateImagePreview()
                }
            }
            .show()
    }

    private fun showCurrencyPicker() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_currency_picker, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvCurrencies)
        val etSearch = view.findViewById<EditText>(R.id.etSearchCurrency)

        val allCurrencies = CurrencyHelper.getCurrencies(this)
        val adapter = CurrencyAdapter(allCurrencies) { item ->
            selectedCurrency = item.code
            findViewById<TextView>(R.id.tvCurrencyLabel).text = item.code
            findViewById<TextView>(R.id.tvCurrencySymbol).text = item.symbol
            dialog.dismiss()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val filtered = allCurrencies.filter { it.name.contains(s.toString(), true) || it.code.contains(s.toString(), true) }
                adapter.filterList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.show()
    }

    private fun renderSharedContacts() {
        val container = findViewById<android.widget.LinearLayout>(R.id.containerSharedContacts)
        if (container == null) return
        container.removeAllViews()
        findViewById<TextView>(R.id.tvSharedContactError)?.visibility = View.GONE
        currentSharedContacts.forEachIndexed { index, contact ->
            val view = layoutInflater.inflate(R.layout.item_shared_contact, container, false)
            view.findViewById<TextView>(R.id.tvContactName).text = contact.name
            view.findViewById<View>(R.id.btnRemoveContact).setOnClickListener {
                currentSharedContacts.removeAt(index)
                renderSharedContacts()
            }
            container.addView(view)
        }
    }
    
    private fun updateSharedUI() {
        val tvSharedCount = findViewById<TextView>(R.id.tvSharedCount)
        tvSharedCount?.text = currentSharedCount.toString()
        val layoutContacts = findViewById<View>(R.id.layoutSharedContacts)
        if (layoutContacts != null) {
            if (currentSharedCount > 1) {
                layoutContacts.visibility = View.VISIBLE
            } else {
                layoutContacts.visibility = View.GONE
                currentSharedContacts.clear()
            }
            renderSharedContacts()
        }
    }

    private fun periodLabel(p: String) = when (p) { 
        "monthly" -> getString(R.string.monthly)
        "yearly" -> getString(R.string.yearly)
        "weekly" -> getString(R.string.weekly)
        else -> getString(R.string.daily)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val contactUri = data.data ?: return
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = contentResolver.query(contactUri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                val contactName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                val contactNumber = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                cursor.close()

                val isAlreadyAdded = currentSharedContacts.any { 
                    it.name.equals(contactName, true) || 
                    it.phoneNumber.replace(Regex("[^0-9+]"), "") == contactNumber.replace(Regex("[^0-9+]"), "")
                }
                if (isAlreadyAdded) {
                    findViewById<TextView>(R.id.tvSharedContactError)?.visibility = View.VISIBLE
                } else {
                    currentSharedContacts.add(SharedContact(contactName, contactNumber))
                    renderSharedContacts()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean("is_premium_active", false)
        val layoutSharedLockOverlay = findViewById<View>(R.id.layoutSharedLockOverlay)
        if (layoutSharedLockOverlay != null) {
            if (!isPremium) {
                layoutSharedLockOverlay.visibility = View.VISIBLE
                layoutSharedLockOverlay.setOnClickListener {
                    startActivity(Intent(this, PremiumActivity::class.java))
                }
            } else {
                layoutSharedLockOverlay.visibility = View.GONE
            }
        }
    }

    private fun saveData() {
        getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            .edit().putString("subs_list", Gson().toJson(subscriptions)).commit()
    }

    private suspend fun loadData() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val json = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<Subscription>>() {}.type
                subscriptions = Gson().fromJson(json, type)
            }
        }
    }
}
