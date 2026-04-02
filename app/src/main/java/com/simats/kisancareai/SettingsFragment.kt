package com.simats.kisancareai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfilePicture = view.findViewById(R.id.iv_settings_profile_picture)
        btnRemovePicture = view.findViewById(R.id.btn_remove_profile_picture)

        // Display saved user name and email
        val sharedPrefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("full_name", "Farmer")
        val userEmail = sharedPrefs.getString("email", "notset@example.com")
        val savedImagePath = sharedPrefs.getString("profile_image_path", null)
        
        view.findViewById<TextView>(R.id.tv_settings_user_name).text = userName
        view.findViewById<TextView>(R.id.tv_settings_user_email).text = userEmail

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

        // Navigation for options
        view.findViewById<View>(R.id.btn_personal_info).setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_language_voice).setOnClickListener {
            startActivity(Intent(requireContext(), LanguagePreferenceActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_notifications).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_privacy_security).setOnClickListener {
            startActivity(Intent(requireContext(), PrivacySecurityActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_weather).setOnClickListener {
            startActivity(Intent(requireContext(), WeatherForecastActivity::class.java))
        }

        // Logout
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_dialog_title))
                .setMessage(getString(R.string.logout_confirm_msg))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    val prefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        clear()
                        putString("app_language", "en")
                        apply()
                    }
                    
                    // Apply English locale immediately for the next activities
                    LocaleHelper.setLocale(requireContext(), "en")
                    
                    Toast.makeText(requireContext(), getString(R.string.logging_out), Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }
    }

    private fun showImagePickerDialog() {
        // Enforce Privacy & Security Setting
        val privacyPrefs = requireContext().getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE)
        val isCameraAllowed = privacyPrefs.getBoolean("camera", true)
        
        if (!isCameraAllowed) {
            Toast.makeText(requireContext(), "Please turn on camera access in privacy & security", Toast.LENGTH_LONG).show()
            return
        }

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = android.app.AlertDialog.Builder(requireContext())
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
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.simats.kisanmitra.fileprovider",
            photoFile
        )
        photoUri?.let { takePicture.launch(it) }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PROFILE_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveImageLocally(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "profile_pic_$timeStamp.jpg"
            val file = File(requireContext().filesDir, fileName)
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val localUri = Uri.fromFile(file)
            val sharedPrefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("profile_image_path", localUri.toString()).apply()
            
            loadProfileImage(localUri.toString())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
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
            showDefaultProfileImage()
        } catch (e: Exception) {
            e.printStackTrace()
            showDefaultProfileImage()
        }
    }

    private fun showDefaultProfileImage() {
        ivProfilePicture.setImageResource(R.drawable.ic_user)
        ivProfilePicture.setPadding(20, 20, 20, 20)
        ivProfilePicture.setColorFilter(ContextCompat.getColor(requireContext(), R.color.vibrant_green))
        btnRemovePicture.visibility = View.GONE
    }

    private fun removeProfileImage() {
        val sharedPrefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("profile_image_path").apply()
        showDefaultProfileImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        view?.findViewById<TextView>(R.id.tv_settings_user_name)?.text = sharedPrefs.getString("full_name", "Farmer")
        view?.findViewById<TextView>(R.id.tv_settings_user_email)?.text = sharedPrefs.getString("email", "notset@example.com")
        
        val savedImagePath = sharedPrefs.getString("profile_image_path", null)
        if (savedImagePath != null) {
            loadProfileImage(savedImagePath)
        } else {
            showDefaultProfileImage()
        }
    }
}
