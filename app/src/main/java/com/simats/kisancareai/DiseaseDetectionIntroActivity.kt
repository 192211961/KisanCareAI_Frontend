package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge

class DiseaseDetectionIntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disease_detection_intro)

        val btnNext = findViewById<Button>(R.id.btn_next)
        val btnSkip = findViewById<Button>(R.id.btn_skip)

        btnNext.setOnClickListener {
            val intent = Intent(this, WeatherAlertsActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnSkip.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
