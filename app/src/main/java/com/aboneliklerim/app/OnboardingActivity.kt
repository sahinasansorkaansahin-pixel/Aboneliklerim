package com.aboneliklerim.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

data class OnboardingItem(val title: String, val description: String, val icon: Int)

class OnboardingActivity : BaseActivity() {

    private val items by lazy {
        listOf(
            OnboardingItem(getString(R.string.onboarding_title_1), getString(R.string.onboarding_desc_1), R.drawable.icons),
            OnboardingItem(getString(R.string.onboarding_title_2), getString(R.string.onboarding_desc_2), R.drawable.saat),
            OnboardingItem(getString(R.string.onboarding_title_9), getString(R.string.onboarding_desc_9), R.drawable.ic_cost_sharing),
            OnboardingItem(getString(R.string.onboarding_title_5), getString(R.string.onboarding_desc_5), R.drawable.global),
            OnboardingItem(getString(R.string.onboarding_title_4), getString(R.string.onboarding_desc_4), R.drawable.istatistik),
            OnboardingItem(getString(R.string.onboarding_title_7), getString(R.string.onboarding_desc_7), R.drawable.senkronize),
            OnboardingItem(getString(R.string.onboarding_title_3), getString(R.string.onboarding_desc_3), R.drawable.dark),
            OnboardingItem(getString(R.string.onboarding_title_6), getString(R.string.onboarding_desc_6), R.drawable.category),
            OnboardingItem(getString(R.string.onboarding_title_8), getString(R.string.onboarding_desc_8), R.drawable.start)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerOnboarding)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.adapter = OnboardingAdapter(items) { finishOnboarding() }

        val hintView = findViewById<ImageView>(R.id.ivSwipeHintOnboarding)
        if (hintView != null) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val swipeRunnable = object : Runnable {
                override fun run() {
                    if (isDestroyed || isFinishing) return
                    
                    // Only show if the current page is NOT the last page (nowhere to slide to!)
                    if (viewPager.currentItem < items.size - 1) {
                        // Reset alpha and translation
                        hintView.alpha = 0f
                        hintView.translationY = 40f
                        hintView.visibility = View.VISIBLE
                        
                        // Animate: Slide Up & Fade In (up to 0.6f transparency), then Fade Out
                        hintView.animate()
                            .alpha(0.6f)
                            .translationY(-40f)
                            .setDuration(2200)
                            .withEndAction {
                                hintView.animate()
                                    .alpha(0f)
                                    .setDuration(400)
                                    .withEndAction {
                                        // Repeat periodically in 4 seconds
                                        handler.postDelayed(this, 4000)
                                    }
                                    .start()
                            }
                            .start()
                    } else {
                        hintView.visibility = View.GONE
                    }
                }
            }
            
            // Start the hint animation
            handler.postDelayed(swipeRunnable, 1500)

            // Reset the idle timer when page changes
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    handler.removeCallbacks(swipeRunnable)
                    hintView.animate().cancel()
                    hintView.alpha = 0f
                    hintView.visibility = View.GONE
                    
                    // Schedule to start prompting again after 3 seconds of idle time on this page!
                    if (position < items.size - 1) {
                        handler.postDelayed(swipeRunnable, 3000)
                    }
                }
            })
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .edit().putBoolean("first_run", false).apply()
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

class OnboardingAdapter(private val items: List<OnboardingItem>, private val onFinish: () -> Unit) : RecyclerView.Adapter<OnboardingAdapter.OVH>() {
    class OVH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivOnboarding)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val desc: TextView = v.findViewById(R.id.tvDescription)
        val btn: Button = v.findViewById(R.id.btnNext)
        val tvSkip: TextView = v.findViewById(R.id.tvSkipItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        OVH(LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: OVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.desc.text = item.description
        holder.iv.setImageResource(item.icon)
        
        if (position == 0) {
            holder.iv.rotation = -15f
            val size = (260 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.iv.layoutParams.width = size
            holder.iv.layoutParams.height = size
        } else {
            holder.iv.rotation = 0f
            val size = (160 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.iv.layoutParams.width = size
            holder.iv.layoutParams.height = size
        }
        if (item.icon == R.drawable.global) {
            holder.iv.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#701b8b"))
        } else {
            holder.iv.imageTintList = null
        }

        // Her kartın kendi "Atla" butonu ve tıklama işlemi
        if (position == items.size - 1) {
            holder.tvSkip.visibility = View.GONE
            holder.btn.visibility = View.VISIBLE
            holder.btn.text = holder.itemView.context.getString(R.string.start_now)
            holder.btn.setOnClickListener { onFinish() }
        } else {
            holder.tvSkip.visibility = View.VISIBLE
            holder.tvSkip.setOnClickListener { onFinish() }
            holder.btn.visibility = View.GONE
        }
    }
}
