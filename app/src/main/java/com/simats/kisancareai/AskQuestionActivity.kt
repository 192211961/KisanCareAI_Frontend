package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge

class AskQuestionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ask_question)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val etQuestion = findViewById<EditText>(R.id.et_question)
        val btnSubmit = findViewById<Button>(R.id.btn_submit)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Handle button state based on text input
        etQuestion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    btnSubmit.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                    btnSubmit.isEnabled = false
                } else {
                    btnSubmit.backgroundTintList = getColorStateList(R.color.vibrant_green)
                    btnSubmit.isEnabled = true
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSubmit.setOnClickListener {
            val question = etQuestion.text.toString()
            if (question.isNotEmpty()) {
                val intent = Intent(this, AiResponseActivity::class.java)
                intent.putExtra("QUERY_TEXT", question)
                intent.putExtra("IS_DISEASE_ANALYSIS", false)
                startActivity(intent)
                finish()
            }
        }
        
        // Disable initially if empty
        btnSubmit.isEnabled = false
    }
}
