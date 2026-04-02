package com.simats.kisancareai

import android.content.Context
import android.os.Bundle
import android.view.View
import android.text.InputFilter
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        val pincodeApi = RetrofitClient.pincodeApi

        // Header Back Button
        findViewById<View>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Form Fields
        val etFullName = findViewById<EditText>(R.id.et_full_name)
        val etMobile = findViewById<EditText>(R.id.et_mobile)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPincode = findViewById<EditText>(R.id.et_pincode)
        val etState = findViewById<EditText>(R.id.et_state)
        val etDistrict = findViewById<EditText>(R.id.et_district)

        // Name Edit/Save Buttons
        val btnEditName = findViewById<TextView>(R.id.btn_edit_name)
        val btnSaveName = findViewById<TextView>(R.id.btn_save_name)
        
        // Input Filter to prevent numbers in name
        val nameFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (Character.isDigit(source[i])) {
                    Toast.makeText(this, "Numbers are not allowed in name", Toast.LENGTH_SHORT).show()
                    return@InputFilter ""
                }
            }
            null
        }
        etFullName.filters = arrayOf(nameFilter)

        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)

        // Load saved data (locally first for speed)
        val savedName = sharedPrefs.getString("full_name", "")
        val savedEmail = sharedPrefs.getString("email", "") ?: ""
        etFullName.setText(savedName)
        etMobile.setText(sharedPrefs.getString("mobile", ""))
        etEmail.setText(savedEmail)
        etPincode.setText(sharedPrefs.getString("pincode", ""))
        etState.setText(sharedPrefs.getString("state", ""))
        etDistrict.setText(sharedPrefs.getString("district", ""))

        // Fetch fresh data from backend
        if (savedEmail.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.backendApi.getProfile(savedEmail)
                    if (response.isSuccessful) {
                        val profile = response.body()
                        profile?.let {
                            etFullName.setText(it.full_name)
                            etMobile.setText(it.mobile)
                            etPincode.setText(it.pincode)
                            etState.setText(it.state)
                            etDistrict.setText(it.district)
                            
                            // Also update local storage
                            sharedPrefs.edit().apply {
                                putString("full_name", it.full_name)
                                putString("mobile", it.mobile)
                                putString("pincode", it.pincode)
                                putString("state", it.state)
                                putString("district", it.district)
                            }.apply()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProfile", "Error fetching profile", e)
                }
            }
        }

        // Edit Name Logic
        btnEditName.setOnClickListener {
            etFullName.isEnabled = true
            etFullName.requestFocus()
            // Move cursor to end
            etFullName.setSelection(etFullName.text.length)
            
            // Show keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etFullName, InputMethodManager.SHOW_IMPLICIT)
            
            btnEditName.visibility = View.GONE
            btnSaveName.visibility = View.VISIBLE
        }

        // Save Individual Name Logic (locally)
        btnSaveName.setOnClickListener {
            val newName = etFullName.text.toString().trim()
            if (newName.length < 3) {
                Toast.makeText(this, "Name must be at least 3 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (newName.any { it.isDigit() }) {
                Toast.makeText(this, "Name cannot contain numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPrefs.edit().putString("full_name", newName).apply()
            etFullName.isEnabled = false
            btnSaveName.visibility = View.GONE
            btnEditName.visibility = View.VISIBLE
            
            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etFullName.windowToken, 0)
            
            Toast.makeText(this, "Name updated locally", Toast.LENGTH_SHORT).show()
        }

        // Pincode Auto-fill Logic
        etPincode.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 6) {
                    val pin = s.toString()
                    fetchPincodeDetails(pin, pincodeApi, etState, etDistrict)
                } else {
                    etState.setText("")
                    etDistrict.setText("")
                }
            }
        })

        // Save All Changes Button (Network Call)
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val name = etFullName.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pincode = etPincode.text.toString().trim()
            val state = etState.text.toString().trim()
            val district = etDistrict.text.toString().trim()

            if (name.length < 3) {
                Toast.makeText(this, "Full Name must be at least 3 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.any { it.isDigit() }) {
                Toast.makeText(this, "Full Name cannot contain numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mobile.isNotEmpty() && mobile.length != 10) {
                Toast.makeText(this, "Mobile number must be 10 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profile = UserProfile(name, email, mobile, pincode, state, district)
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.backendApi.updateProfile(profile)
                    if (response.isSuccessful) {
                        // Save to SharedPreferences on success
                        val editor = sharedPrefs.edit()
                        editor.putString("full_name", name)
                        editor.putString("mobile", mobile)
                        editor.putString("email", email)
                        editor.putString("pincode", pincode)
                        editor.putString("state", state)
                        editor.putString("district", district)
                        editor.apply()

                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditProfile", "Update error", e)
                    Toast.makeText(this@EditProfileActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchPincodeDetails(pin: String, api: PincodeApiService, etState: EditText, etDistrict: EditText) {
        api.getPincodeDetails(pin).enqueue(object : Callback<List<PincodeResponse>> {
            override fun onResponse(call: Call<List<PincodeResponse>>, response: Response<List<PincodeResponse>>) {
                try {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val pincodeData = response.body()!![0]
                        if (pincodeData.Status == "Success" && !pincodeData.PostOffice.isNullOrEmpty()) {
                            val details = pincodeData.PostOffice[0]
                            etState.setText(details.State)
                            etDistrict.setText(details.District)

                            // Make them strictly non-editable after auto-fill
                            etState.isFocusable = false
                            etState.isClickable = false
                            etDistrict.isFocusable = false
                            etDistrict.isClickable = false
                            
                            Toast.makeText(this@EditProfileActivity, "Location updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EditProfileActivity, "Invalid pincode or no data found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@EditProfileActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@EditProfileActivity, "Error parsing location data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PincodeResponse>>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Network Error: Please check your internet", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
