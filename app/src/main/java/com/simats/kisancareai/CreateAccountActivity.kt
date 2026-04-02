package com.simats.kisancareai

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class CreateAccountActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)

        val tilFullName = findViewById<TextInputLayout>(R.id.til_full_name)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_confirm_password)
        
        val etFullName = findViewById<EditText>(R.id.et_full_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)

        // Block numbers in Full Name field
        etFullName.filters = arrayOf(android.text.InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (Character.isDigit(source[i])) {
                    return@InputFilter ""
                }
            }
            null
        })

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            navigateToLogin()
        }

        findViewById<Button>(R.id.btn_send_otp).setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            
            tilFullName.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            if (fullName.isEmpty()) {
                tilFullName.error = "Full Name is required"
                return@setOnClickListener
            }

            if (fullName.length < 3) {
                tilFullName.error = "Full Name must be at least 3 characters"
                return@setOnClickListener
            }

            if (fullName.any { it.isDigit() }) {
                tilFullName.error = "Full Name cannot contain numbers"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                tilEmail.error = "Email address is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }

            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$"
            if (!password.matches(Regex(passwordPattern))) {
                tilPassword.error = "Password must be at least 8 characters, include one uppercase, one lowercase, one number, and one special symbol (@$!%*?&)"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerUser(fullName, email, password, confirmPassword)
        }

        findViewById<TextView>(R.id.btn_have_account).setOnClickListener {
            navigateToLogin()
        }
    }

    private fun registerUser(fullName: String, email: String, p: String, cp: String) {
        val request = RegisterRequest(fullName, email, p, cp)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.backendApi.register(request)
                if (response.isSuccessful) {
                    // Save info to SharedPreferences before proceeding
                    getSharedPreferences("UserProfilePrefs", MODE_PRIVATE).edit()
                        .putString("full_name", fullName)
                        .putString("email", email)
                        .apply()
                        
                    Toast.makeText(this@CreateAccountActivity, response.body()?.message ?: "OTP Sent", Toast.LENGTH_SHORT).show()
                    navigateToOtp(email)
                } else {
                    val errorStr = response.errorBody()?.string() ?: ""
                    Log.e("BackendError", "Response code: ${response.code()}, Body: $errorStr")
                    val message = try {
                        JSONObject(errorStr).getString("error")
                    } catch (e: Exception) {
                        if (errorStr.contains("<!doctype html>", true)) {
                            "Server error (HTML). Check backend logs."
                        } else {
                            "Error: $errorStr"
                        }
                    }
                    Toast.makeText(this@CreateAccountActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Log.e("NetworkError", "Connection failed to ${RetrofitClient.BACKEND_BASE_URL}", e)
                Toast.makeText(this@CreateAccountActivity, "Network Error. Checked: ${RetrofitClient.BACKEND_BASE_URL}\nCheck if your IP is correct and server is running.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("NetworkError", "Unexpected error", e)
                Toast.makeText(this@CreateAccountActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun navigateToOtp(email: String) {
        val intent = android.content.Intent(this, OtpVerificationActivity::class.java).apply {
            putExtra("email", email)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = android.content.Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
