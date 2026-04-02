package com.simats.kisancareai

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiseaseDetectionActivity : BaseActivity() {

    private lateinit var ivPlaceholder: ImageView
    private lateinit var ivPreview: ImageView
    private lateinit var layoutInitial: android.view.View
    private lateinit var layoutPreview: android.view.View
    private var photoUri: Uri? = null

    // Gallery selection
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            photoUri = it
            displayImage(it)
        }
    }

    // Camera capture
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            photoUri?.let { displayImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disease_detection)

        ivPlaceholder = findViewById(R.id.iv_placeholder)
        ivPreview = findViewById(R.id.iv_preview)
        layoutInitial = findViewById(R.id.layout_initial)
        layoutPreview = findViewById(R.id.layout_preview)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnTakePhoto = findViewById<Button>(R.id.btn_take_photo)
        val btnUpload = findViewById<Button>(R.id.btn_upload)
        val btnAnalyze = findViewById<Button>(R.id.btn_analyze)
        val btnSelectDifferent = findViewById<Button>(R.id.btn_select_different)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        btnUpload.setOnClickListener {
            val privacyPrefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)
            val isCameraAllowed = privacyPrefs.getBoolean("camera", true)
            
            if (!isCameraAllowed) {
                Toast.makeText(this, "Please turn on camera access in privacy & security", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            getContent.launch("image/*")
        }

        btnAnalyze.setOnClickListener {
            val intent = Intent(this, ImageAnalysisActivity::class.java)
            intent.putExtra("QUERY_TEXT", "Leaf disease analysis")
            intent.putExtra("IMAGE_URI", photoUri?.toString())
            startActivity(intent)
        }

        btnSelectDifferent.setOnClickListener {
            showInitialState()
        }
    }

    private fun displayImage(uri: Uri) {
        ivPreview.setImageURI(uri)
        layoutInitial.visibility = android.view.View.GONE
        layoutPreview.visibility = android.view.View.VISIBLE
    }

    private fun showInitialState() {
        layoutInitial.visibility = android.view.View.VISIBLE
        layoutPreview.visibility = android.view.View.GONE
        ivPreview.setImageDrawable(null)
    }

    private fun checkCameraPermissionAndLaunch() {
        val privacyPrefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)
        val isCameraAllowed = privacyPrefs.getBoolean("camera", true)
        
        if (!isCameraAllowed) {
            Toast.makeText(this, "Please turn on camera access in privacy & security", Toast.LENGTH_LONG).show()
            return
        }

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
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }
}
