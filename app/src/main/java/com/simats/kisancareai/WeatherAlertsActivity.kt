package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge

class WeatherAlertsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather_alerts)

        val btnGetStarted = findViewById<Button>(R.id.btn_get_started)
        val btnHaveAccount = findViewById<Button>(R.id.btn_have_account)

        btnGetStarted.setOnClickListener {
            navigateToCreateAccount()
        }

        btnHaveAccount.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToCreateAccount() {
        val intent = Intent(this, CreateAccountActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}