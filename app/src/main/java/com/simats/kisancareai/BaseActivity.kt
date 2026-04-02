package com.simats.kisancareai

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            handleTapFeedback()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun handleTapFeedback() {
        try {
            val prefs = getSharedPreferences("kisanmitra_settings", Context.MODE_PRIVATE) ?: return
            
            // Handle Vibration
            if (prefs.getBoolean("enable_vibration", true)) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(20)
                    }
                }
            }

            // Handle Sound
            if (prefs.getBoolean("enable_sound", true)) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
            }
        } catch (e: Exception) {
            // Silently fail to prevent app crash if feedback fails
            e.printStackTrace()
        }
    }

    protected fun setupBottomNavigation() {
        val btnHome = findViewById<LinearLayout>(R.id.btn_home) ?: return
        val btnHelp = findViewById<LinearLayout>(R.id.btn_help)
        val btnSettings = findViewById<LinearLayout>(R.id.btn_settings)

        // Reset all to inactive state first
        resetNavState()

        // Highlight based on current activity
        when (this) {
            is MainActivity -> highlightNavButton(R.id.iv_home, R.id.tv_home)
            is HelpFaqActivity -> highlightNavButton(R.id.iv_help, R.id.tv_help)
            is SettingsPremiumActivity -> highlightNavButton(R.id.iv_settings, R.id.tv_settings)
        }

        btnHome.setOnClickListener {
            if (this !is MainActivity) {
                startActivity(android.content.Intent(this, MainActivity::class.java))
                finish()
            }
        }

        btnHelp?.setOnClickListener {
            if (this !is HelpFaqActivity) {
                startActivity(android.content.Intent(this, HelpFaqActivity::class.java))
                if (this !is MainActivity) finish()
            }
        }

        btnSettings?.setOnClickListener {
            if (this !is SettingsPremiumActivity) {
                startActivity(android.content.Intent(this, SettingsPremiumActivity::class.java))
                if (this !is MainActivity) finish()
            }
        }
    }

    private fun resetNavState() {
        val navButtons = listOf(
            Pair(R.id.iv_home, R.id.tv_home),
            Pair(R.id.iv_help, R.id.tv_help),
            Pair(R.id.iv_settings, R.id.tv_settings)
        )

        for (button in navButtons) {
            findViewById<android.widget.ImageView>(button.first)?.let {
                it.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.nav_inactive))
            }
            findViewById<android.widget.TextView>(button.second)?.let {
                it.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.nav_inactive))
            }
        }
    }

    private fun highlightNavButton(iconId: Int, textId: Int) {
        findViewById<android.widget.ImageView>(iconId)?.let {
            it.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.vibrant_green))
        }
        findViewById<android.widget.TextView>(textId)?.let {
            it.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.vibrant_green))
        }
    }
}
