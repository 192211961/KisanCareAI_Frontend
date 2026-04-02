package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Real login logic
            val request = LoginRequest(email, password)
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.backendApi.login(request)
                    if (response.isSuccessful) {
                        val loginBody = response.body()
                        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
                        val editor = sharedPrefs.edit()
                        editor.putBoolean("is_logged_in", true)
                        editor.putString("full_name", loginBody?.fullName ?: "Farmer")
                        editor.putString("email", loginBody?.email ?: email)
                        editor.apply()

                        navigateToSubscription()
                    } else {
                        val errorStr = response.errorBody()?.string() ?: ""
                        val message = try {
                            org.json.JSONObject(errorStr).getString("error")
                        } catch (e: Exception) {
                            "Login Failed"
                        }
                        android.widget.Toast.makeText(this@LoginActivity, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LoginActivity", "Login error", e)
                    android.widget.Toast.makeText(this@LoginActivity, "Connection Error", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<TextView>(R.id.btn_forgot_password).setOnClickListener {
            navigateToResetPassword()
        }

        findViewById<TextView>(R.id.btn_register).setOnClickListener {
            navigateToRegister()
        }
    }

    private fun navigateToResetPassword() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRegister() {
        val intent = Intent(this, CreateAccountActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSubscription() {
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
        
        val intent = Intent(this, SubscriptionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
