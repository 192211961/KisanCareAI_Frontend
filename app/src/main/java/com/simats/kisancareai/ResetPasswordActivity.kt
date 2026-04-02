package com.simats.kisancareai

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ResetPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        findViewById<TextView>(R.id.btn_back_to_login).setOnClickListener {
            finish()
        }

        val etEmail = findViewById<android.widget.EditText>(R.id.et_email)
        findViewById<android.widget.Button>(R.id.btn_send_otp).setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                android.widget.Toast.makeText(this, "Please enter your email", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressBar = findViewById<android.widget.ProgressBar>(R.id.progress_bar)
            progressBar.visibility = android.view.View.VISIBLE

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.backendApi.forgotPassword(ForgotPasswordRequest(email))
                    if (response.isSuccessful) {
                        android.widget.Toast.makeText(this@ResetPasswordActivity, "OTP sent to your email", android.widget.Toast.LENGTH_SHORT).show()
                        navigateToOtpSent(email)
                    } else {
                        val errorStr = response.errorBody()?.string() ?: ""
                        val message = try {
                            org.json.JSONObject(errorStr).getString("error")
                        } catch (e: Exception) {
                            "Failed to send OTP"
                        }
                        android.widget.Toast.makeText(this@ResetPasswordActivity, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ResetPassword", "Error", e)
                    android.widget.Toast.makeText(this@ResetPasswordActivity, "Connection Error", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun navigateToOtpSent(email: String) {
        val intent = android.content.Intent(this, OtpVerificationActivity::class.java)
        intent.putExtra("email", email)
        intent.putExtra("isResetPassword", true)
        startActivity(intent)
    }
}
