package com.simats.kisancareai

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.AppCompatCheckBox

class NotificationSettingsActivity : BaseActivity() {
    
    private val PREFS_NAME = "kisanmitra_settings"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_settings)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Views
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val switchWeather = findViewById<SwitchCompat>(R.id.switch_weather)
        val switchTips = findViewById<SwitchCompat>(R.id.switch_tips)
        val cbSound = findViewById<AppCompatCheckBox>(R.id.cb_sound)
        val cbVibration = findViewById<AppCompatCheckBox>(R.id.cb_vibration)
        val cbDnd = findViewById<AppCompatCheckBox>(R.id.cb_dnd)

        // Load saved states
        switchWeather.isChecked = prefs.getBoolean("weather_alerts", true)
        switchTips.isChecked = prefs.getBoolean("farming_tips", true)
        cbSound.isChecked = prefs.getBoolean("enable_sound", true)
        cbVibration.isChecked = prefs.getBoolean("enable_vibration", true)
        cbDnd.isChecked = prefs.getBoolean("dnd_mode", false)

        // Live-save listeners
        switchWeather.setOnCheckedChangeListener { buttonView, isChecked -> 
            if (isChecked) {
                if (checkAndRequestNotificationPermission()) {
                    prefs.edit().putBoolean("weather_alerts", true).apply()
                    AlarmHelper.scheduleNextWeatherAlarm(this)
                } else {
                    buttonView.isChecked = false
                }
            } else {
                prefs.edit().putBoolean("weather_alerts", false).apply()
                AlarmHelper.cancelWeatherAlarms(this)
            }
        }
        switchTips.setOnCheckedChangeListener { buttonView, isChecked -> 
            if (isChecked) {
                if (checkAndRequestNotificationPermission()) {
                    prefs.edit().putBoolean("farming_tips", true).apply()
                    AlarmHelper.scheduleDailyTipAlarm(this)
                } else {
                    buttonView.isChecked = false
                }
            } else {
                prefs.edit().putBoolean("farming_tips", false).apply()
                AlarmHelper.cancelDailyTipAlarm(this)
            }
        }
        cbSound.setOnCheckedChangeListener { _, isChecked -> 
            prefs.edit().putBoolean("enable_sound", isChecked).apply() 
        }
        cbVibration.setOnCheckedChangeListener { _, isChecked -> 
            prefs.edit().putBoolean("enable_vibration", isChecked).apply() 
        }
        cbDnd.setOnCheckedChangeListener { _, isChecked -> 
            prefs.edit().putBoolean("dnd_mode", isChecked).apply() 
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnSave.setOnClickListener {
            Toast.makeText(this, getString(R.string.settings_preserved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkAndRequestNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 1002)
                return false
            }
        }
        return true
    }
}
