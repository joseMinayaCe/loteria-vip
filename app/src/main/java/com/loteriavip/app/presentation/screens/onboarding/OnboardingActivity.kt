package com.loteriavip.app.presentation.screens.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.messaging.FirebaseMessaging
import com.loteriavip.app.MainActivity
import com.loteriavip.app.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var dotsContainer: LinearLayout

    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboarding_pager)
        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        dotsContainer = findViewById(R.id.dots_container)

        val adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter

        setupDots(adapter.itemCount)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                when (position) {
                    adapter.itemCount - 1 -> {
                        btnNext.text = "¡Comenzar!"
                        btnSkip.visibility = View.GONE
                    }
                    else -> {
                        btnNext.text = "Siguiente"
                        btnSkip.visibility = View.VISIBLE
                    }
                }
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < adapter.itemCount - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots(count: Int) {
        dotsContainer.removeAllViews()
        dots.clear()
        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (i == 0) 24.dp else 8.dp, 8.dp
                ).apply { setMargins(4.dp, 0, 4.dp, 0) }
                background = ContextCompat.getDrawable(
                    context,
                    if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
            dots.add(dot)
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        dots.forEachIndexed { index, dot ->
            val isSelected = index == selected
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (isSelected) 24.dp else 8.dp
            }
            dot.background = ContextCompat.getDrawable(
                this,
                if (isSelected) R.drawable.dot_active else R.drawable.dot_inactive
            )
            dot.requestLayout()
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_shown", true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        fun shouldShow(context: Context): Boolean {
            val prefs = context.getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
            return !prefs.getBoolean("onboarding_shown", false)
        }
    }
}

// ─── Pager Adapter ───────────────────────────────────────────────────────────

class OnboardingPagerAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val PAGE_WELCOME = 0
        const val PAGE_NOTIFICATIONS = 1
        const val PAGE_READY = 2
    }

    override fun getItemCount() = 3
    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PAGE_WELCOME -> WelcomeHolder(inflater.inflate(R.layout.onboarding_page_welcome, parent, false))
            PAGE_NOTIFICATIONS -> NotifHolder(inflater.inflate(R.layout.onboarding_page_notifications, parent, false))
            else -> ReadyHolder(inflater.inflate(R.layout.onboarding_page_ready, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NotifHolder) holder.bind(context)
    }

    // ViewHolders
    class WelcomeHolder(v: View) : RecyclerView.ViewHolder(v)
    class ReadyHolder(v: View) : RecyclerView.ViewHolder(v)

    class NotifHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val chkGanaMas: CheckBox = v.findViewById(R.id.chk_gana_mas)
        private val chkLeidsa: CheckBox = v.findViewById(R.id.chk_leidsa)
        private val chkReal: CheckBox = v.findViewById(R.id.chk_real)
        private val chkPrimera: CheckBox = v.findViewById(R.id.chk_primera)
        private val chkLoteka: CheckBox = v.findViewById(R.id.chk_loteka)
        private val chkNewYork: CheckBox = v.findViewById(R.id.chk_new_york)
        private val chkFlorida: CheckBox = v.findViewById(R.id.chk_florida)
        private val chkKing: CheckBox = v.findViewById(R.id.chk_king)
        private val chkHaiti: CheckBox = v.findViewById(R.id.chk_haiti)

        fun bind(context: Context) {
            val prefs = context.getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)

            val checkboxMap = mapOf(
                chkGanaMas to "gana-mas",
                chkLeidsa to "leidsa",
                chkReal to "real",
                chkPrimera to "la-primera",
                chkLoteka to "loteka",
                chkNewYork to "new-york",
                chkFlorida to "florida",
                chkKing to "king-lottery",
                chkHaiti to "haiti-bolet"
            )

            checkboxMap.forEach { (chk, key) ->
                chk.isChecked = prefs.getBoolean("notification_$key", true)
                chk.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean("notification_$key", isChecked).apply()
                    try {
                        if (isChecked) {
                            FirebaseMessaging.getInstance().subscribeToTopic("loteria-$key")
                        } else {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-$key")
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }
}
