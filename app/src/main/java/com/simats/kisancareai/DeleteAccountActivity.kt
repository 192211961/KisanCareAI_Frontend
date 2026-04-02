package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

class DeleteAccountActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val etConfirmDelete = findViewById<EditText>(R.id.et_confirm_delete)
        val btnConfirmDelete = findViewById<Button>(R.id.btn_confirm_delete)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        
        // Find the text view in the info box
        val tvInfoBox = findViewById<TextView>(R.id.tv_info_text)
        tvInfoBox.text = Html.fromHtml("<b>Need a break instead?</b> You can logout and come back anytime without losing your data.", Html.FROM_HTML_MODE_LEGACY)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnCancel.setOnClickListener {
            onBackPressed()
        }

        btnConfirmDelete.setOnClickListener {
            if (etConfirmDelete.text.toString().trim() == "DELETE") {
                val prefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                Toast.makeText(this, "Account successfully deleted", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please type DELETE exactly to confirm", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
