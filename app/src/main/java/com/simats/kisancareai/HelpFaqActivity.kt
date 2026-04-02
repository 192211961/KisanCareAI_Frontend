package com.simats.kisancareai

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class HelpFaqActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_faq)
        setupBottomNavigation()

        val btnBack = findViewById<View>(R.id.btn_back)

        // Back Button
        btnBack?.setOnClickListener {
            finish()
        }

        // FAQ Toggle Logic
        setupFaqToggle(R.id.layout_faq_1, R.id.answer_faq_1, R.id.arrow_faq_1)
        setupFaqToggle(R.id.layout_faq_2, R.id.answer_faq_2, R.id.arrow_faq_2)
        setupFaqToggle(R.id.layout_faq_3, R.id.answer_faq_3, R.id.arrow_faq_3)
        setupFaqToggle(R.id.layout_faq_4, R.id.answer_faq_4, R.id.arrow_faq_4)
        setupFaqToggle(R.id.layout_faq_5, R.id.answer_faq_5, R.id.arrow_faq_5)
        setupFaqToggle(R.id.layout_faq_6, R.id.answer_faq_6, R.id.arrow_faq_6)
        setupFaqToggle(R.id.layout_faq_7, R.id.answer_faq_7, R.id.arrow_faq_7)
        setupFaqToggle(R.id.layout_faq_8, R.id.answer_faq_8, R.id.arrow_faq_8)
    }

    private fun setupFaqToggle(layoutId: Int, answerId: Int, arrowId: Int) {
        val layout = findViewById<View>(layoutId)
        val answer = findViewById<TextView>(answerId)
        val arrow = findViewById<ImageView>(arrowId)

        layout.setOnClickListener {
            if (answer.visibility == View.GONE) {
                answer.visibility = View.VISIBLE
                arrow.rotation = 90f // Rotate arrow down
            } else {
                answer.visibility = View.GONE
                arrow.rotation = 0f // Reset arrow
            }
        }
    }
}
