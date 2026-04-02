package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge

class FeedbackActivity : BaseActivity() {

    private var currentRating = 0
    private lateinit var btnSubmit: Button
    private val stars = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnSkip = findViewById<TextView>(R.id.btn_skip)
        btnSubmit = findViewById(R.id.btn_submit_feedback)

        stars.add(findViewById(R.id.star1))
        stars.add(findViewById(R.id.star2))
        stars.add(findViewById(R.id.star3))
        stars.add(findViewById(R.id.star4))
        stars.add(findViewById(R.id.star5))

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                setRating(index + 1)
            }
        }

        val chips = listOf(
            findViewById<TextView>(R.id.chip_ai_accuracy),
            findViewById<TextView>(R.id.chip_app_usability),
            findViewById<TextView>(R.id.chip_response_speed),
            findViewById<TextView>(R.id.chip_overall)
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                chip.isSelected = !chip.isSelected
            }
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnSkip.setOnClickListener {
            navigateToHome()
        }

        btnSubmit.setOnClickListener {
            val intent = Intent(this, FeedbackSuccessActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setRating(rating: Int) {
        currentRating = rating
        for (i in 0 until 5) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star_filled)
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline)
            }
        }
        btnSubmit.isEnabled = true
        btnSubmit.backgroundTintList = getColorStateList(R.color.vibrant_green)
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
