package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import com.aboneliklerim.app.Subscription
import com.aboneliklerim.app.UpcomingAdapter
import com.aboneliklerim.app.MainActivity
import com.aboneliklerim.app.ReportsActivity
import com.aboneliklerim.app.MoreActivity
import com.aboneliklerim.app.AddEditActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class UpcomingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upcoming)

        setupNavigation()

        val subs = loadData()
        val rv = findViewById<RecyclerView>(R.id.rvUpcoming)
        rv.layoutManager = LinearLayoutManager(this)

        val upcoming = subs.map { sub ->
            val daysUntil = calculateDaysRemaining(sub.startDate, sub.period, sub.periodValue)
            Pair(sub, daysUntil)
        }.sortedBy { it.second }

        lifecycleScope.launch {
            val rates = CurrencyService.getExchangeRates(this@UpcomingActivity)
            rv.adapter = UpcomingAdapter(upcoming, rates)
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_upcoming
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_upcoming -> true
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, MoreActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val intent = Intent(this, AddEditActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadData(): List<Subscription> {
        val json = getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }
}
