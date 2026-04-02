package com.simats.kisancareai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PrivacySecurityActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_security)

        setupPermissions()
    }

    private lateinit var switchCamera: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchMic: androidx.appcompat.widget.SwitchCompat

    private fun setupPermissions() {

        // Back Button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }


        // SharedPreferences for Persistence
        val sharedPrefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        val switchLocation = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_location)
        switchCamera = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_camera)
        switchMic = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_mic)
        val switchNotif = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_notifications)

        // Load states
        switchLocation.isChecked = sharedPrefs.getBoolean("location", true)
        switchCamera.isChecked = sharedPrefs.getBoolean("camera", true)
        switchMic.isChecked = sharedPrefs.getBoolean("mic", true)
        switchNotif.isChecked = sharedPrefs.getBoolean("notif", true)

        // Save on change
        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("location", isChecked).apply()
        }
        
        switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
                }
            }
            editor.putBoolean("camera", isChecked).apply()
        }

        switchMic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 102)
                }
            }
            editor.putBoolean("mic", isChecked).apply()
        }

        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notif", isChecked).apply()
        }

        // Row clicks to trigger toggle
        findViewById<android.view.View>(R.id.row_camera).setOnClickListener {
            switchCamera.isChecked = !switchCamera.isChecked
        }
        findViewById<android.view.View>(R.id.row_mic).setOnClickListener {
            switchMic.isChecked = !switchMic.isChecked
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val sharedPrefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        when (requestCode) {
            101 -> { // Camera
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Stay checked, already handled in listener
                } else {
                    switchCamera.isChecked = false
                    editor.putBoolean("camera", false).apply()
                    Toast.makeText(this, getString(R.string.camera_denied), Toast.LENGTH_SHORT).show()
                }
            }
            102 -> { // Mic
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Stay checked
                } else {
                    switchMic.isChecked = false
                    editor.putBoolean("mic", false).apply()
                    Toast.makeText(this, getString(R.string.mic_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
