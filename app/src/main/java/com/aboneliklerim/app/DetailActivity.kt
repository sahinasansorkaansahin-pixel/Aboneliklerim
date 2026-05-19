package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetailActivity : BaseActivity() {

    private var subscriptions = mutableListOf<Subscription>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        loadData()
        val subId = intent.getStringExtra("sub_id") ?: run { finish(); return }
        val sub = subscriptions.find { it.id == subId } ?: run { finish(); return }

        findViewById<TextView>(R.id.tvDetailName).text = sub.name ?: getString(R.string.subscription)
        findViewById<TextView>(R.id.tvDetailIcon).text = (sub.name ?: "A").take(1).uppercase()
        try {
            val colorStr = sub.color ?: "#6366f1"
            findViewById<View>(R.id.viewDetailColor).setBackgroundColor(android.graphics.Color.parseColor(colorStr))
            findViewById<TextView>(R.id.tvDetailIcon).backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorStr))
        } catch (_: Exception) {}

        val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val lang = resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"

        val subCurrency = sub.currency ?: "TRY"
        val currencySymbol = CurrencyHelper.getLocalizedSymbol(subCurrency, this)
        val priceStr = "${numFmt.format(sub.price)} $currencySymbol"

        val periodLabel = when (sub.period) { 
            "daily" -> getString(R.string.daily)
            "weekly" -> getString(R.string.weekly)
            "monthly" -> getString(R.string.monthly)
            "yearly" -> getString(R.string.yearly)
            else -> getString(R.string.one_time)
        }

        findViewById<TextView>(R.id.tvDetailPrice).text = "$priceStr / $periodLabel"
        
        val layoutShared = findViewById<View>(R.id.layoutShared)
        val viewSharedDivider = findViewById<View>(R.id.viewSharedDivider)
        val sharedCount = java.lang.Math.max(1, sub.sharedWith)
        if (sharedCount > 1) {
            layoutShared.visibility = View.VISIBLE
            viewSharedDivider.visibility = View.VISIBLE
            
            val perPersonCost = sub.price / sharedCount
            val perPersonStr = "${numFmt.format(perPersonCost)} $currencySymbol"
            
            findViewById<TextView>(R.id.tvDetailShared).text = "$sharedCount  —  " + getString(R.string.per_person) + " $perPersonStr"
            
            val containerContacts = findViewById<android.widget.LinearLayout>(R.id.containerDetailContacts)
            containerContacts.removeAllViews()
            
            val contactsList = sub.sharedContacts ?: emptyList()
            contactsList.forEach { contact ->
                val btn = Button(this).apply {
                    text = "WhatsApp: ${contact.name}"
                    isAllCaps = false
                    backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#25D366")) // WhatsApp Green
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 13f
                    minHeight = (40 * resources.displayMetrics.density).toInt()
                    setPadding(
                        (12 * resources.displayMetrics.density).toInt(), 
                        (6 * resources.displayMetrics.density).toInt(), 
                        (12 * resources.displayMetrics.density).toInt(), 
                        (6 * resources.displayMetrics.density).toInt()
                    )
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
                    
                    setOnClickListener {
                        val firstName = contact.name.split(" ").firstOrNull() ?: contact.name
                        var contactNumber = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                        val msgTemplate = getString(R.string.whatsapp_greeting, firstName) + getString(R.string.share_reminder_text, sub.name, perPersonStr)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            val url = "https://api.whatsapp.com/send?phone=$contactNumber&text=${java.net.URLEncoder.encode(msgTemplate, "UTF-8")}"
                            intent.data = android.net.Uri.parse(url)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msgTemplate)
                            }
                            startActivity(Intent.createChooser(fallbackIntent, getString(R.string.send_reminder)))
                        }
                    }
                }
                containerContacts.addView(btn)
            }
        } else {
            layoutShared.visibility = View.GONE
            viewSharedDivider.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvDetailCategory).text = sub.category ?: getString(R.string.general_cat)
        findViewById<TextView>(R.id.tvDetailCurrency).text = subCurrency
        findViewById<TextView>(R.id.tvDetailPaymentMethod).text = (sub.paymentMethod ?: "").ifEmpty { "-" }
        findViewById<TextView>(R.id.tvDetailNote).text = (sub.note ?: "").ifEmpty { "-" }
        // Total spent estimate (months since start)
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val startDate = try { fmt.parse(sub.startDate ?: "") } catch (_: Exception) { java.util.Date() }
        
        val notifyDayText = when (sub.notifyDaysBefore) { 
            -1 -> getString(R.string.off)
            0 -> getString(R.string.same_day)
            1 -> getString(R.string.one_day_before)
            3 -> getString(R.string.three_days_before)
            7 -> getString(R.string.seven_days_before)
            else -> getString(R.string.days_before_plural, sub.notifyDaysBefore)
        }
        
        // Calculate exact next notification date for clarity
        val nextNotifyStr = if (sub.notifyDaysBefore != -1) {
            val now = java.util.Calendar.getInstance()
            val timeParts = sub.notifyTime.split(":")
            val notifyHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
            val notifyMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            var payCal = getNextPaymentDate(sub.startDate, sub.period)
            var triggerCal = payCal?.clone() as? java.util.Calendar
            if (triggerCal != null) {
                triggerCal.add(java.util.Calendar.DAY_OF_YEAR, -sub.notifyDaysBefore)
                triggerCal.set(java.util.Calendar.HOUR_OF_DAY, notifyHour)
                triggerCal.set(java.util.Calendar.MINUTE, notifyMinute)
                triggerCal.set(java.util.Calendar.SECOND, 0)
                triggerCal.set(java.util.Calendar.MILLISECOND, 0)

                if (triggerCal.before(now) && sub.period != "one-time" && payCal != null) {
                    val nextRef = payCal.clone() as java.util.Calendar
                    nextRef.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    payCal = getNextPaymentDate(sub.startDate, sub.period, nextRef)
                    if (payCal != null) {
                        triggerCal = payCal.clone() as java.util.Calendar
                        triggerCal.add(java.util.Calendar.DAY_OF_YEAR, -sub.notifyDaysBefore)
                        triggerCal.set(java.util.Calendar.HOUR_OF_DAY, notifyHour)
                        triggerCal.set(java.util.Calendar.MINUTE, notifyMinute)
                        triggerCal.set(java.util.Calendar.SECOND, 0)
                        triggerCal.set(java.util.Calendar.MILLISECOND, 0)
                    }
                }
            }

            if (triggerCal != null) {
                val datePart = DateFormatHelper.formatForDisplay(this, triggerCal.time)
                val timePart = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(triggerCal.time)
                "$datePart $timePart"
            } else getString(R.string.off)
        } else getString(R.string.off)

        findViewById<TextView>(R.id.tvDetailNotify).text = nextNotifyStr

        val utcStart = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            val startCal = java.util.Calendar.getInstance().apply { time = startDate ?: java.util.Date() }
            set(java.util.Calendar.YEAR, startCal.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH, startCal.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, startCal.get(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val localNow = java.util.Calendar.getInstance()
        val utcNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.YEAR, localNow.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH, localNow.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, localNow.get(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffDays = ((utcNow.timeInMillis - utcStart.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val totalSpent = when (sub.period) {
            "daily" -> sub.price * (diffDays + 1)
            "weekly" -> sub.price * ((diffDays / 7) + 1)
            "yearly" -> sub.price * ((diffDays / 365) + 1)
            "one-time" -> sub.price
            else -> sub.price * ((diffDays / 30) + 1)
        }
        findViewById<TextView>(R.id.tvDetailTotalSpent).text = "${numFmt.format(totalSpent)} $currencySymbol"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Hide actions if requested (e.g. from Upcoming)
        if (intent.getBooleanExtra("hide_actions", false)) {
            findViewById<View>(R.id.btnEdit).visibility = View.GONE
            findViewById<View>(R.id.btnArchive).visibility = View.GONE
            findViewById<View>(R.id.btnDelete).visibility = View.GONE
        }

        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, AddEditActivity::class.java)
            intent.putExtra("sub_id", subId)
            startActivityForResult(intent, 100)
        }
        val btnArchive = findViewById<Button>(R.id.btnArchive)
        btnArchive.text = if (sub.isArchived) getString(R.string.unarchive) else getString(R.string.archive)
        
        btnArchive.setOnClickListener {
            val targetSub = subscriptions.find { it.id == subId }
            if (targetSub != null) {
                targetSub.isArchived = !targetSub.isArchived
                saveData()
                
                if (targetSub.isArchived) {
                    NotificationScheduler.cancelAlarm(this, subId)
                }
                NotificationScheduler.scheduleAlarms(this)
                
                setResult(RESULT_OK)
                finish()
                val msg = if (targetSub.isArchived) getString(R.string.msg_archived) else getString(R.string.msg_unarchived)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_Aboneliklerim_Dialog)
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete) { _, _ ->
                    NotificationScheduler.cancelAlarm(this, subId)
                    subscriptions.removeAll { it.id == subId }
                    saveData()
                    NotificationScheduler.scheduleAlarms(this)
                    setResult(RESULT_OK)
                    finish()
                }
                .setNegativeButton(R.string.cancel, null).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
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

                val subId = intent.getStringExtra("sub_id") ?: return
                val sub = subscriptions.find { it.id == subId } ?: return
                
                val currentContacts = (sub.sharedContacts ?: emptyList()).toMutableList()
                val isAlreadyAdded = currentContacts.any { 
                    it.name.equals(contactName, true) || 
                    it.phoneNumber.replace(Regex("[^0-9+]"), "") == contactNumber.replace(Regex("[^0-9+]"), "")
                }
                if (isAlreadyAdded) {
                    findViewById<TextView>(R.id.tvDetailSharedContactError)?.visibility = View.VISIBLE
                } else {
                    currentContacts.add(SharedContact(contactName, contactNumber))
                    sub.sharedContacts = currentContacts
                    saveData()
                    recreate()
                }
            }
        } else if (resultCode == RESULT_OK) { 
            setResult(RESULT_OK); recreate() 
        }
    }

    private fun saveData() {
        getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE)
            .edit().putString("subs_list", Gson().toJson(subscriptions)).apply()
    }

    private fun loadData() {
        val json = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Subscription>>() {}.type
            subscriptions = Gson().fromJson(json, type)
        }
    }
}
