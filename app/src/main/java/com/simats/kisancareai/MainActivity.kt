package com.simats.kisancareai

import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Schedule initial alarms if not already set
        scheduleInitialAlarms()

        // Initialize with Home Fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.btn_home)
        }

        setupTabNavigation()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }
    }

    private fun scheduleInitialAlarms() {
        val prefs = getSharedPreferences("kisanmitra_settings", MODE_PRIVATE)
        
        // Force schedule weather alarm to include the new 9:15 AM test slot
        if (prefs.getBoolean("weather_alerts", true)) {
            AlarmHelper.scheduleNextWeatherAlarm(this)
        }
        
        // Only schedule tips if not already set (standard behavior)
        if (prefs.getBoolean("is_first_launch_alarms", true)) {
            if (prefs.getBoolean("farming_tips", true)) {
                AlarmHelper.scheduleDailyTipAlarm(this)
            }
            prefs.edit().putBoolean("is_first_launch_alarms", false).apply()
        }
    }

    private fun setupTabNavigation() {
        val btnHome = findViewById<LinearLayout>(R.id.btn_home)
        val btnHelp = findViewById<LinearLayout>(R.id.btn_help)
        val btnSettings = findViewById<LinearLayout>(R.id.btn_settings)

        btnHome.setOnClickListener {
            loadFragment(HomeFragment(), R.id.btn_home)
        }

        btnHelp.setOnClickListener {
            loadFragment(HelpFragment(), R.id.btn_help)
        }

        btnSettings.setOnClickListener {
            loadFragment(SettingsFragment(), R.id.btn_settings)
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is HomeFragment) {
            loadFragment(HomeFragment(), R.id.btn_home)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFragment(fragment: Fragment, btnId: Int) {
        // Don't reload the same fragment if it's already visible
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null && currentFragment::class == fragment::class) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        updateNavUI(btnId)
    }

    private fun updateNavUI(selectedBtnId: Int) {
        // Reset all
        resetIconAndText(R.id.iv_home, R.id.tv_home)
        resetIconAndText(R.id.iv_help, R.id.tv_help)
        resetIconAndText(R.id.iv_settings, R.id.tv_settings)

        // Highlight selected
        when (selectedBtnId) {
            R.id.btn_home -> highlightIconAndText(R.id.iv_home, R.id.tv_home)
            R.id.btn_help -> highlightIconAndText(R.id.iv_help, R.id.tv_help)
            R.id.btn_settings -> highlightIconAndText(R.id.iv_settings, R.id.tv_settings)
        }
    }

    private fun resetIconAndText(iconId: Int, textId: Int) {
        findViewById<android.widget.ImageView>(iconId)?.let {
            it.setColorFilter(ContextCompat.getColor(this, R.color.nav_inactive))
        }
        findViewById<android.widget.TextView>(textId)?.let {
            it.setTextColor(ContextCompat.getColor(this, R.color.nav_inactive))
        }
    }

    private fun highlightIconAndText(iconId: Int, textId: Int) {
        findViewById<android.widget.ImageView>(iconId)?.let {
            it.setColorFilter(ContextCompat.getColor(this, R.color.vibrant_green))
        }
        findViewById<android.widget.TextView>(textId)?.let {
            it.setTextColor(ContextCompat.getColor(this, R.color.vibrant_green))
        }
    }
}