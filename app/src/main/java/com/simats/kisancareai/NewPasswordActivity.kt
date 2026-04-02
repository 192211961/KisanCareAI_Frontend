package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class NewPasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_password)

        val tilNewPassword = findViewById<TextInputLayout>(R.id.til_new_password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_confirm_password)
        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnResetPassword = findViewById<Button>(R.id.btn_reset_password)

        btnBack.setOnClickListener { finish() }

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            tilNewPassword.error = null
            tilConfirmPassword.error = null

            if (newPassword.isEmpty()) {
                tilNewPassword.error = "Password is required"
                return@setOnClickListener
            }

            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$"
            if (!newPassword.matches(Regex(passwordPattern))) {
                tilNewPassword.error = "Password must be at least 8 characters, include one uppercase, one lowercase, one number, and one special symbol (@$!%*?&)"
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val email = intent.getStringExtra("email") ?: ""
            val otp = intent.getStringExtra("otp") ?: ""

            if (email.isEmpty() || otp.isEmpty()) {
                Toast.makeText(this, "Session expired. Please try again.", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val request = ResetPasswordRequest(email, otp, newPassword, confirmPassword)
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.backendApi.resetPassword(request)
                    if (response.isSuccessful) {
                        Toast.makeText(this@NewPasswordActivity, "Password reset successfully!", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@NewPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorStr = response.errorBody()?.string() ?: ""
                        val message = try {
                            JSONObject(errorStr).getString("error")
                        } catch (e: Exception) {
                            "Failed to reset password"
                        }
                        Toast.makeText(this@NewPasswordActivity, message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NewPassword", "Error", e)
                    Toast.makeText(this@NewPasswordActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
