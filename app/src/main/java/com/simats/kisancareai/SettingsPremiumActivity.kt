package com.simats.kisancareai

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsPremiumActivity : BaseActivity() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnRemovePicture: View
    private var photoUri: Uri? = null

    // Gallery selection
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveImageLocally(it)
        }
    }

    // Camera capture
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            photoUri?.let { saveImageLocally(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_premium)
        setupBottomNavigation()

        ivProfilePicture = findViewById(R.id.iv_settings_profile_picture)
        btnRemovePicture = findViewById(R.id.btn_remove_profile_picture)

        // Display saved user name and email
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
        val userName = sharedPrefs.getString("full_name", "Farmer")
        val userEmail = sharedPrefs.getString("email", "notset@example.com")
        val savedImagePath = sharedPrefs.getString("profile_image_path", null)
        
        findViewById<TextView>(R.id.tv_settings_user_name).text = userName
        findViewById<TextView>(R.id.tv_settings_user_email).text = userEmail

        if (savedImagePath != null) {
            loadProfileImage(savedImagePath)
        } else {
            showDefaultProfileImage()
        }

        // Profile Picture click to change
        ivProfilePicture.setOnClickListener {
            showImagePickerDialog()
        }

        // Remove Button
        btnRemovePicture.setOnClickListener {
            removeProfileImage()
        }

        // Back Button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Navigation for options
        findViewById<View>(R.id.btn_personal_info).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_language_voice).setOnClickListener {
            startActivity(Intent(this, LanguagePreferenceActivity::class.java))
        }

        findViewById<View>(R.id.btn_notifications).setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btn_privacy_security).setOnClickListener {
            startActivity(Intent(this, PrivacySecurityActivity::class.java))
        }

        findViewById<View>(R.id.btn_weather).setOnClickListener {
            startActivity(Intent(this, WeatherForecastActivity::class.java))
        }

    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Change Profile Picture")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermissionAndLaunch()
                1 -> getContent.launch("image/*")
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "com.simats.kisanmitra.fileprovider",
            photoFile
        )
        photoUri?.let { takePicture.launch(it) }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PROFILE_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveImageLocally(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "profile_pic_$timeStamp.jpg"
            val file = File(filesDir, fileName)
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val localUri = Uri.fromFile(file)
            val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
            sharedPrefs.edit().putString("profile_image_path", localUri.toString()).apply()
            
            loadProfileImage(localUri.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val path = if (uri.scheme == "file") {
                uri.path
            } else {
                uriString // fallback
            }

            if (path != null) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    ivProfilePicture.setImageBitmap(bitmap)
                    ivProfilePicture.setPadding(0, 0, 0, 0)
                    ivProfilePicture.clearColorFilter()
                    ivProfilePicture.imageTintList = null
                    btnRemovePicture.visibility = View.VISIBLE
                    return
                }
            }
            // If bitmap loading fails, fallback to default
            showDefaultProfileImage()
        } catch (e: Exception) {
            e.printStackTrace()
            showDefaultProfileImage()
        }
    }

    private fun showDefaultProfileImage() {
        ivProfilePicture.setImageResource(R.drawable.ic_user)
        ivProfilePicture.setPadding(20, 20, 20, 20)
        ivProfilePicture.setColorFilter(ContextCompat.getColor(this, R.color.vibrant_green))
        btnRemovePicture.visibility = View.GONE
    }

    private fun removeProfileImage() {
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
        sharedPrefs.edit().remove("profile_image_path").apply()
        showDefaultProfileImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh name and email if they were changed in EditProfileActivity
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", MODE_PRIVATE)
        findViewById<TextView>(R.id.tv_settings_user_name).text = sharedPrefs.getString("full_name", "Farmer")
        findViewById<TextView>(R.id.tv_settings_user_email).text = sharedPrefs.getString("email", "notset@example.com")
        
        val savedImagePath = sharedPrefs.getString("profile_image_path", null)
        if (savedImagePath != null) {
            loadProfileImage(savedImagePath)
        } else {
            showDefaultProfileImage()
        }
        setupBottomNavigation()
    }
}
