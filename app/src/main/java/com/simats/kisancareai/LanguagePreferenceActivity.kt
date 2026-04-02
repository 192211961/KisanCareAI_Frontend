package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguagePreferenceActivity : BaseActivity() {
    private var selectedLanguageCode = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language_preference)

        val chipEnglish = findViewById<TextView>(R.id.chip_english)
        val chipHindi = findViewById<TextView>(R.id.chip_hindi)
        val chipTelugu = findViewById<TextView>(R.id.chip_telugu)
        val chipTamil = findViewById<TextView>(R.id.chip_tamil)

        val languageChips = arrayOf(chipEnglish, chipHindi, chipTelugu, chipTamil)

        val voiceEnabled = findViewById<LinearLayout>(R.id.voice_enabled)
        val voiceDisabled = findViewById<LinearLayout>(R.id.voice_disabled)

        // Set defaults from current app locale
        val currentLocale = LocaleHelper.getLanguage(this) ?: "en"
        selectedLanguageCode = currentLocale
        
        updateChipSelection(languageChips, currentLocale)
        voiceEnabled.isSelected = true

        chipEnglish.setOnClickListener {
            selectedLanguageCode = "en"
            updateChipSelection(languageChips, "en")
        }
        chipHindi.setOnClickListener {
            selectedLanguageCode = "hi"
            updateChipSelection(languageChips, "hi")
        }
        chipTelugu.setOnClickListener {
            selectedLanguageCode = "te"
            updateChipSelection(languageChips, "te")
        }
        chipTamil.setOnClickListener {
            selectedLanguageCode = "ta"
            updateChipSelection(languageChips, "ta")
        }

        voiceEnabled.setOnClickListener {
            voiceEnabled.isSelected = true
            voiceDisabled.isSelected = false
        }

        voiceDisabled.setOnClickListener {
            voiceEnabled.isSelected = false
            voiceDisabled.isSelected = true
        }

        findViewById<Button>(R.id.btn_complete_setup).setOnClickListener {
            // Apply language globally
            LocaleHelper.setLocale(this, selectedLanguageCode)

            // Optional: for better AppCompat support
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(selectedLanguageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateChipSelection(chips: Array<TextView>, langCode: String) {
        chips.forEach { it.isSelected = false }
        when (langCode) {
            "en" -> chips[0].isSelected = true
            "hi" -> chips[1].isSelected = true
            "te" -> chips[2].isSelected = true
            "ta" -> chips[3].isSelected = true
        }
    }
}
