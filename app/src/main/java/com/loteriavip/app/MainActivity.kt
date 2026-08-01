package com.loteriavip.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.loteriavip.app.databinding.ActivityMainBinding
import com.loteriavip.app.presentation.components.OnboardingNotificationDialog
import com.loteriavip.app.presentation.screens.live.LiveResultsFragment
import com.loteriavip.app.presentation.screens.favorites.FavoritesFragment
import com.loteriavip.app.presentation.screens.luck.LuckFragment
import com.loteriavip.app.presentation.screens.stats.StatsFragment
import com.loteriavip.app.presentation.screens.settings.SettingsFragment
import com.loteriavip.app.presentation.screens.notifications.NotificationHistoryFragment
import com.loteriavip.app.presentation.screens.notifications.NotificationHistoryViewModel
import com.loteriavip.app.presentation.screens.company.CompanyDrawsFragment
import com.loteriavip.app.domain.model.LotteryCompany
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val notificationViewModel: NotificationHistoryViewModel by viewModels()
    private val NOTIFICATION_PERMISSION_CODE = 1001
    private var selectedNavId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Forzar modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupBottomNavigation()
            setupDrawer()

            // Fragmento inicial
            if (savedInstanceState == null) {
                replaceFragmentPrimary(LiveResultsFragment())
            }

            // Inicializaciones secundarias
            setupAdMob()
            setupNotificationsBell()
            checkAndSetupNotifications()
            com.loteriavip.app.data.notification.NotificationChannelManager.createChannels(this)
            
            intent?.let { handleIntent(it) }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error during onCreate", e)
            showCrashScreen(e)
        }
    }

    private fun showCrashScreen(t: Throwable) {
        try {
            val scrollView = android.widget.ScrollView(this)
            val textView = android.widget.TextView(this).apply {
                text = "ERROR DE INICIALIZACIÓN (MAIN_ACTIVITY):\n\n${Log.getStackTraceString(t)}"
                setTextColor(android.graphics.Color.RED)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(32, 32, 32, 32)
            }
            scrollView.addView(textView)
            setContentView(scrollView)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun setupDrawer() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        val menu = binding.navigationView.menu
        menu.clear()

        val allLotteriesItem = menu.add(0, 999, 0, "Todas las Loterías")
        allLotteriesItem.setIcon(R.drawable.ic_calendar)

        LotteryCompany.ALL.forEachIndexed { index, company ->
            val item = menu.add(1, 1000 + index, index + 1, company.name)
            item.setIcon(R.drawable.ic_star)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()

            if (menuItem.itemId == 999) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                replaceFragmentPrimary(LiveResultsFragment())
                selectNavItem(R.id.nav_home)
            } else {
                val index = menuItem.itemId - 1000
                if (index in LotteryCompany.ALL.indices) {
                    val company = LotteryCompany.ALL[index]
                    replaceFragmentSecondary(CompanyDrawsFragment.newInstance(company.name))
                    selectNavItem(-1)
                }
            }
            true
        }
    }

    private fun setupNotificationsBell() {
        binding.btnNotifications.setOnClickListener {
            replaceFragmentSecondary(NotificationHistoryFragment())
            selectNavItem(-1)
        }

        lifecycleScope.launch {
            notificationViewModel.unreadCount.collect { count ->
                binding.tvNotificationBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                binding.tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: android.content.Intent) {
        val lotteryId = intent.getStringExtra("LOTTERY_ID")
        if (!lotteryId.isNullOrEmpty()) {
            selectNavItem(R.id.nav_home)
        }
    }

    private fun setupAdMob() {
        try {
            MobileAds.initialize(this) {}
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } catch (e: Throwable) {
            Log.e("AdMob", "Error initializing AdMob: ${e.message}")
        }
    }

    private fun subscribeToAllDefaultTopics() {
        val sharedPrefs = getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
        val isGlobalEnabled = sharedPrefs.getBoolean("notification_global_enabled", true)
        val defaultSubscriptions = listOf(
            "gana-mas", "leidsa", "real", "la-primera",
            "loteka", "anguila", "king-lottery", "la-suerte", "lotedom",
            "new-york", "florida", "haiti-bolet"
        )

        try {
            if (isGlobalEnabled) {
                FirebaseMessaging.getInstance().subscribeToTopic("loteria-all")
                FirebaseMessaging.getInstance().subscribeToTopic("all")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-all")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
            }
        } catch (e: Throwable) { e.printStackTrace() }

        defaultSubscriptions.forEach { key ->
            val isSubscribed = sharedPrefs.getBoolean("notification_$key", true)
            try {
                if (isGlobalEnabled && isSubscribed) {
                    FirebaseMessaging.getInstance().subscribeToTopic("loteria-$key")
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-$key")
                }
            } catch (e: Throwable) { e.printStackTrace() }
        }
    }

    private fun checkAndSetupNotifications() {
        val sharedPrefs = getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
        
        // Always make sure default FCM topics are subscribed if needed
        subscribeToAllDefaultTopics()

        // Check for POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+) whenever not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
                return
            }
        }

        val onboardingShown = sharedPrefs.getBoolean("onboarding_shown", false)
        if (!onboardingShown) {
            sharedPrefs.edit().putBoolean("onboarding_shown", true).apply()
            showOnboardingDialog()
        }
    }

    private fun showOnboardingDialog() {
        if (isFinishing || isDestroyed) return
        binding.root.post {
            if (!isFinishing && !isDestroyed) {
                try {
                    OnboardingNotificationDialog(this) {}.show()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            // Subscribe to topics regardless of whether permission was granted or denied
            // (topics are still useful for data-only messages on background)
            subscribeToAllDefaultTopics()
            showOnboardingDialog()
        }
    }

    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener {
            selectNavItem(R.id.nav_home)
            // Clear back stack and load home
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            replaceFragmentPrimary(LiveResultsFragment())
        }
        binding.navFavorites.setOnClickListener {
            selectNavItem(R.id.nav_favorites)
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            replaceFragmentPrimary(FavoritesFragment())
        }
        binding.navPredictions.setOnClickListener {
            selectNavItem(R.id.nav_predictions)
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            replaceFragmentPrimary(LuckFragment())
        }
        binding.navStats.setOnClickListener {
            selectNavItem(R.id.nav_stats)
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            replaceFragmentPrimary(StatsFragment())
        }
        binding.navSettings.setOnClickListener {
            selectNavItem(R.id.nav_settings)
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            replaceFragmentPrimary(SettingsFragment())
        }
        selectNavItem(R.id.nav_home)
    }

    private fun selectNavItem(id: Int) {
        selectedNavId = id
        val navViews = listOf(binding.navHome, binding.navFavorites, binding.navPredictions, binding.navStats, binding.navSettings)
        for (navView in navViews) {
            val isSelected = navView.id == id
            val color = ContextCompat.getColor(this, if (isSelected) R.color.green_primary else R.color.grey_dark)
            (navView.getChildAt(0) as? ImageView)?.imageTintList = ColorStateList.valueOf(color)
            (navView.getChildAt(1) as? TextView)?.setTextColor(color)
        }
    }

    /** Replace the main fragment WITHOUT adding to back stack (primary nav tabs) */
    private fun replaceFragmentPrimary(fragment: Fragment) {
        binding.appBarLayout.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commitAllowingStateLoss()
    }

    /** Replace with a secondary fragment that CAN be dismissed via back button */
    private fun replaceFragmentSecondary(fragment: Fragment) {
        if (fragment is CompanyDrawsFragment) {
            binding.appBarLayout.visibility = View.GONE
        } else {
            binding.appBarLayout.visibility = View.VISIBLE
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        // If the drawer is open, close it first
        if (binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            binding.drawerLayout.closeDrawers()
            return
        }
        // Pop secondary fragments off the back stack
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // Restore AppBar visibility after popping
            binding.appBarLayout.visibility = View.VISIBLE
            // Restore nav selection to home if back stack is now empty
            if (supportFragmentManager.backStackEntryCount == 0) {
                selectNavItem(R.id.nav_home)
            }
            return
        }
        // Otherwise let the system handle (will exit app if on root)
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        try { binding.adView.resume() } catch (e: Exception) {}
    }

    override fun onPause() {
        try { binding.adView.pause() } catch (e: Exception) {}
        super.onPause()
    }

    override fun onDestroy() {
        try { binding.adView.destroy() } catch (e: Exception) {}
        super.onDestroy()
    }
}
