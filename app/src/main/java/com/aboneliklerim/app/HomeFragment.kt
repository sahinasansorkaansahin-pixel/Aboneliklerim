package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.aboneliklerim.app.Subscription
import com.aboneliklerim.app.SubAdapter
import com.aboneliklerim.app.DetailActivity
import com.aboneliklerim.app.MainActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {

    private var subscriptions = mutableListOf<Subscription>()
    private lateinit var adapter: SubAdapter
    private var currentSortType = "Name"
    private var isSortAscending = true
    private var pulseHandler: android.os.Handler? = null
    private var pulseRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadData()

        val rv = view.findViewById<RecyclerView>(R.id.rvSubscriptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubAdapter(subscriptions) { sub ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("sub_id", sub.id)
            startActivityForResult(intent, 100)
        }
        rv.adapter = adapter
        
        // Başlangıçta kurları çek ve listeyi güncelle
        lifecycleScope.launch {
            val rates = CurrencyService.getExchangeRates(requireContext())
            (requireActivity() as? MainActivity)?.currentRates = rates
            adapter.notifyDataSetChanged()
        }
        // Apply initial filter
        sortSubscriptions()

        view.findViewById<ImageButton>(R.id.btnOptions).setOnClickListener { showFilterDialog() }
        view.findViewById<ImageButton>(R.id.btnSort).setOnClickListener { showSortDialog() }
        
        updateSummary()
    }



    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)
        dialog.setContentView(view)

        val tvDateStatus = view.findViewById<TextView>(R.id.tvToggleDateStatus)
        val tvTagsStatus = view.findViewById<TextView>(R.id.tvToggleTagsStatus)
        val tvArchiveStatus = view.findViewById<TextView>(R.id.tvArchiveStatus)

        fun updateUI() {
            tvDateStatus.text = if (MainActivity.showTimeRemainingGlobal) getString(R.string.hide_time_remaining) else getString(R.string.show_time_remaining)
            tvTagsStatus.text = if (MainActivity.showTagsGlobal) getString(R.string.hide_tags) else getString(R.string.show_tags)
            tvArchiveStatus.text = if (MainActivity.showActiveOnlyGlobal) getString(R.string.show_archived_only) else getString(R.string.show_active_only)
            val ptStatus = when(MainActivity.paymentTypeFilterGlobal) {
                1 -> getString(R.string.filter_regular_only)
                2 -> getString(R.string.filter_one_time_only)
                else -> getString(R.string.filter_all)
            }
            view.findViewById<TextView>(R.id.tvPaymentTypeStatus).text = ptStatus
        }

        updateUI()

        view.findViewById<View>(R.id.rowToggleDate).setOnClickListener {
            MainActivity.showTimeRemainingGlobal = !MainActivity.showTimeRemainingGlobal
            updateUI()
            adapter.notifyDataSetChanged()
        }

        view.findViewById<View>(R.id.rowToggleTags).setOnClickListener {
            MainActivity.showTagsGlobal = !MainActivity.showTagsGlobal
            updateUI()
            adapter.notifyDataSetChanged()
        }

        view.findViewById<View>(R.id.rowToggleArchive).setOnClickListener {
            MainActivity.showActiveOnlyGlobal = !MainActivity.showActiveOnlyGlobal
            updateUI()
            sortSubscriptions()
        }

        view.findViewById<View>(R.id.rowTogglePaymentType).setOnClickListener {
            MainActivity.paymentTypeFilterGlobal = (MainActivity.paymentTypeFilterGlobal + 1) % 3
            updateUI()
            sortSubscriptions()
        }

        dialog.show()
    }

    private fun showSortDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)
        dialog.setContentView(view)

        val ivNameDesc = view.findViewById<ImageView>(R.id.ivSortNameDesc)
        val ivNameAsc = view.findViewById<ImageView>(R.id.ivSortNameAsc)
        val ivPriceDesc = view.findViewById<ImageView>(R.id.ivSortPriceDesc)
        val ivPriceAsc = view.findViewById<ImageView>(R.id.ivSortPriceAsc)
        val ivDateDesc = view.findViewById<ImageView>(R.id.ivSortDateDesc)
        val ivDateAsc = view.findViewById<ImageView>(R.id.ivSortDateAsc)
        val ivColorDesc = view.findViewById<ImageView>(R.id.ivSortColorDesc)
        val ivColorAsc = view.findViewById<ImageView>(R.id.ivSortColorAsc)
        val ivPaymentTypeDesc = view.findViewById<ImageView>(R.id.ivSortPaymentTypeDesc)
        val ivPaymentTypeAsc = view.findViewById<ImageView>(R.id.ivSortPaymentTypeAsc)

        fun resetIcons() {
            listOf(ivNameDesc, ivNameAsc, ivPriceDesc, ivPriceAsc, ivDateDesc, ivDateAsc, ivColorDesc, ivColorAsc, 
                   ivPaymentTypeDesc, ivPaymentTypeAsc).forEach {
                it.setBackgroundResource(0)
                it.imageTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.text_secondary))
            }
        }

        fun updateSortIcons() {
            resetIcons()
            val (desc, asc) = when(currentSortType) {
                "Name" -> ivNameDesc to ivNameAsc
                "Price" -> ivPriceDesc to ivPriceAsc
                "Date" -> ivDateDesc to ivDateAsc
                "PaymentType" -> ivPaymentTypeDesc to ivPaymentTypeAsc
                else -> ivColorDesc to ivColorAsc
            }
            val active = if (isSortAscending) asc else desc
            active.setBackgroundResource(R.drawable.circle_outline_selected)
            active.imageTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary_indigo))
        }

        updateSortIcons()

        ivNameDesc.setOnClickListener { currentSortType = "Name"; isSortAscending = false; sortSubscriptions(); updateSortIcons() }
        ivNameAsc.setOnClickListener { currentSortType = "Name"; isSortAscending = true; sortSubscriptions(); updateSortIcons() }
        ivPriceDesc.setOnClickListener { currentSortType = "Price"; isSortAscending = false; sortSubscriptions(); updateSortIcons() }
        ivPriceAsc.setOnClickListener { currentSortType = "Price"; isSortAscending = true; sortSubscriptions(); updateSortIcons() }
        ivDateDesc.setOnClickListener { currentSortType = "Date"; isSortAscending = false; sortSubscriptions(); updateSortIcons() }
        ivDateAsc.setOnClickListener { currentSortType = "Date"; isSortAscending = true; sortSubscriptions(); updateSortIcons() }
        ivColorDesc.setOnClickListener { currentSortType = "Color"; isSortAscending = false; sortSubscriptions(); updateSortIcons() }
        ivColorAsc.setOnClickListener { currentSortType = "Color"; isSortAscending = true; sortSubscriptions(); updateSortIcons() }
        ivPaymentTypeDesc.setOnClickListener { currentSortType = "PaymentType"; isSortAscending = false; sortSubscriptions(); updateSortIcons() }
        ivPaymentTypeAsc.setOnClickListener { currentSortType = "PaymentType"; isSortAscending = true; sortSubscriptions(); updateSortIcons() }

        dialog.show()
    }

    private fun sortSubscriptions() {
        lifecycleScope.launch {
            val rates = (requireActivity() as? MainActivity)?.currentRates?.takeIf { it.isNotEmpty() }
                ?: CurrencyService.getExchangeRates(requireContext())

            var baseList = if (MainActivity.showActiveOnlyGlobal) subscriptions.filter { !it.isArchived } else subscriptions.filter { it.isArchived }
            
            // Ödeme tipi filtreleme (0: All, 1: Regular, 2: One-time)
            baseList = when(MainActivity.paymentTypeFilterGlobal) {
                1 -> baseList.filter { it.period != "one-time" }
                2 -> baseList.filter { it.period == "one-time" }
                else -> baseList
            }

            val sortedList = when (currentSortType) {
                "Price" -> baseList.sortedBy { sub ->
                    // Convert each subscription's price to TRY for accurate, normalized sorting
                    CurrencyService.convertToTry(sub.price, sub.currency ?: "TRY", rates)
                }
                "Date" -> baseList.sortedBy { calculateDaysRemaining(it.startDate, it.period, it.periodValue) }
                "Color" -> baseList.sortedBy { it.color }
                "PaymentType" -> baseList.sortedBy { if (it.period == "one-time") 1 else 0 }
                else -> baseList.sortedBy { it.name.lowercase(java.util.Locale.getDefault()) }
            }
            val sortedFinal = if (isSortAscending) sortedList else sortedList.reversed()
            adapter.updateData(sortedFinal)
            updateSummary(sortedFinal)
        }
    }

    private fun updateSummary(listOverride: List<Subscription>? = null) {
        val ctx = context ?: return
        val currentView = view ?: return
        
        val filteredList = listOverride ?: run {
            var base = if (MainActivity.showActiveOnlyGlobal) subscriptions.filter { !it.isArchived } else subscriptions.filter { it.isArchived }
            when(MainActivity.paymentTypeFilterGlobal) {
                1 -> base.filter { it.period != "one-time" }
                2 -> base.filter { it.period == "one-time" }
                else -> base
            }
        }

        val lang = requireContext().resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("default_currency", CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)) ?: "TRY"
        
        lifecycleScope.launch {
            val rates = (requireActivity() as? MainActivity)?.currentRates ?: CurrencyService.getExchangeRates(requireContext())
            var monthlyTotal = 0.0
            filteredList.forEach { sub ->
                val price = sub.price
                val pv = sub.periodValue.coerceAtLeast(1).toDouble()
                val monthlyPrice = when (sub.period) {
                    "yearly"  -> price / (12.0 * pv)
                    "weekly"  -> price * (4.33 / pv)
                    "daily"   -> price * (30.0 / pv)
                    "one-time" -> 0.0
                    else      -> price / pv  // monthly
                }
                if (monthlyPrice > 0) {
                    val amountInTry = CurrencyService.convertToTry(monthlyPrice, sub.currency ?: "TRY", rates)
                    val targetAmount = CurrencyService.convertFromTry(amountInTry, defaultCurrency, rates)
                    monthlyTotal += targetAmount
                }
            }

            val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            val currencySymbol = CurrencyHelper.getLocalizedSymbol(defaultCurrency, requireContext())
            currentView.findViewById<TextView>(R.id.tvTotalAmount)?.text = numFmt.format(monthlyTotal)
            currentView.findViewById<TextView>(R.id.tvHomeCurrencySymbol)?.text = currencySymbol
        }
        val countStr = if (MainActivity.showActiveOnlyGlobal) {
            getString(R.string.active_count, filteredList.size)
        } else {
            getString(R.string.archived_count, filteredList.size)
        }
        currentView.findViewById<TextView>(R.id.tvSubCount)?.text = countStr
    }

    private fun loadData() {
        val json = requireContext().getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null)
        val lang = requireContext().resources.configuration.locales.get(0).toLanguageTag()
        val defaultCurrency = CurrencyHelper.getDefaultCurrencyBasedOnLanguage(lang)
        if (json != null && json != "[]") {
            val type = object : TypeToken<MutableList<Subscription>>() {}.type
            subscriptions = Gson().fromJson(json, type)
        } else {
            subscriptions.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        // Her geri gelindiğinde verileri tazele
        loadData()
        sortSubscriptions()
        updateSummary()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadData()
            sortSubscriptions()
            updateSummary()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            loadData()
            sortSubscriptions()
            updateSummary()
        }
    }

}
