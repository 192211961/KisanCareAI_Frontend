package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class OtpVerificationActivity : BaseActivity() {

    private lateinit var tvResend: TextView
    private var countDownTimer: CountDownTimer? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)

        email = intent.getStringExtra("email")
        if (email == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvResend = findViewById(R.id.tv_resend)
        val otpInputs = arrayOf(
            findViewById<EditText>(R.id.et_otp1),
            findViewById<EditText>(R.id.et_otp2),
            findViewById<EditText>(R.id.et_otp3),
            findViewById<EditText>(R.id.et_otp4),
            findViewById<EditText>(R.id.et_otp5),
            findViewById<EditText>(R.id.et_otp6)
        )
        val btnVerify = findViewById<Button>(R.id.btn_verify)

        setupOtpLogic(otpInputs, btnVerify)
        startResendTimer()

        btnVerify.setOnClickListener {
            val otp = otpInputs.joinToString("") { it.text.toString() }
            verifyOtp(otp)
        }

        tvResend.setOnClickListener {
            if (tvResend.tag == "RESEND_ENABLED") {
                val progressBar = findViewById<android.widget.ProgressBar>(R.id.progress_bar)
                progressBar.visibility = android.view.View.VISIBLE
                
                lifecycleScope.launch {
                    try {
                        val request = ResendOtpRequest(email!!)
                        val response = RetrofitClient.backendApi.resendOtp(request)
                        if (response.isSuccessful) {
                            Toast.makeText(this@OtpVerificationActivity, response.body()?.message ?: "OTP Resent", Toast.LENGTH_SHORT).show()
                            startResendTimer()
                        } else {
                            val errorStr = response.errorBody()?.string() ?: ""
                            val message = try { JSONObject(errorStr).getString("error") } catch (e: Exception) { "Resend Failed" }
                            Toast.makeText(this@OtpVerificationActivity, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@OtpVerificationActivity, "Network Error", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        val request = VerifyOtpRequest(email!!, otp)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.backendApi.verifyOtp(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@OtpVerificationActivity, "Verified Successfully", Toast.LENGTH_SHORT).show()
                    
                    val isResetPassword = intent.getBooleanExtra("isResetPassword", false)
                    if (isResetPassword) {
                        val resetIntent = Intent(this@OtpVerificationActivity, NewPasswordActivity::class.java)
                        resetIntent.putExtra("email", email)
                        resetIntent.putExtra("otp", otp)
                        startActivity(resetIntent)
                        finish()
                    } else {
                        val loginIntent = Intent(this@OtpVerificationActivity, LoginActivity::class.java)
                        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(loginIntent)
                        finish()
                    }
                } else {
                    val errorStr = response.errorBody()?.string() ?: ""
                    val message = try {
                        JSONObject(errorStr).getString("error")
                    } catch (e: Exception) {
                        "Verification Failed"
                    }
                    Toast.makeText(this@OtpVerificationActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("NetworkError", "Verification failed at ${RetrofitClient.BACKEND_BASE_URL}", e)
                Toast.makeText(this@OtpVerificationActivity, "Network Error. Checked: ${RetrofitClient.BACKEND_BASE_URL}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun startResendTimer() {
        tvResend.isEnabled = false
        tvResend.tag = "RESEND_DISABLED"
        tvResend.setTextColor(ContextCompat.getColor(this, R.color.grey))
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResend.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                tvResend.text = "Resend OTP"
                tvResend.isEnabled = true
                tvResend.tag = "RESEND_ENABLED"
                tvResend.setTextColor(ContextCompat.getColor(this@OtpVerificationActivity, R.color.vibrant_green))
            }
        }.start()
    }

    private fun setupOtpLogic(inputs: Array<EditText>, button: Button) {
        for (i in inputs.indices) {
            inputs[i].addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s?.length == 1 && i < inputs.size - 1) inputs[i + 1].requestFocus()
                    validateOtp(inputs, button)
                }
            })
            inputs[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (inputs[i].text.isEmpty() && i > 0) {
                        inputs[i - 1].requestFocus()
                        inputs[i - 1].text?.clear()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun validateOtp(inputs: Array<EditText>, button: Button) {
        val isValid = inputs.all { it.text.length == 1 }
        button.isEnabled = isValid
        if (isValid) {
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.vibrant_green)
            )
        } else {
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.light_grey)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
