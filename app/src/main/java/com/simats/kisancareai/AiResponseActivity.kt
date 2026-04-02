package com.simats.kisancareai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.launch
import java.util.Locale

class AiResponseActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_response)

        textToSpeech = TextToSpeech(this, this)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnHome = findViewById<Button>(R.id.btn_home)
        val btnPlay = findViewById<Button>(R.id.btn_play_audio)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnLike = findViewById<ImageView>(R.id.btn_like)
        val btnDislike = findViewById<ImageView>(R.id.btn_dislike)
        val tvQuery = findViewById<TextView>(R.id.tv_query)
        val tvDescription = findViewById<TextView>(R.id.tv_description)

        val isDiseaseAnalysis = intent.getBooleanExtra("IS_DISEASE_ANALYSIS", false)
        val queryText = intent.getStringExtra("QUERY_TEXT") ?: (if (isDiseaseAnalysis) "Leaf disease analysis" else "How to control aphids in cotton?")
        val precalculatedAdvice = intent.getStringExtra("PRECALCULATED_ADVICE")
        
        tvQuery.text = queryText
        
        if (precalculatedAdvice != null) {
            tvDescription.text = precalculatedAdvice
        } else if (isDiseaseAnalysis) {
            val imageUriString = intent.getStringExtra("IMAGE_URI")
            if (imageUriString != null) {
                val imageUri = android.net.Uri.parse(imageUriString)
                tvDescription.text = getString(R.string.analyzing_image)
                lifecycleScope.launch {
                    val response = KisanAiEngine.getDiseaseAnalysis(this@AiResponseActivity, imageUri)
                    tvDescription.text = response
                    addToHistory(queryText, response, isDiseaseAnalysis)
                }
            } else {
                tvDescription.text = getString(R.string.upload_photo_desc)
            }
        } else {
            // Call Gemini AI
            tvDescription.text = getString(R.string.consulting_ai)
            lifecycleScope.launch {
                val response = KisanAiEngine.getAiResponse(this@AiResponseActivity, queryText)
                tvDescription.text = response
                addToHistory(queryText, response, isDiseaseAnalysis)
            }
        }


        btnBack.setOnClickListener {
            onBackPressed()
        }

        findViewById<Button>(R.id.btn_ask_another).setOnClickListener {
            val intent = Intent(this, AskQuestionActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        btnPlay.setOnClickListener {
            if (isTtsInitialized) {
                val text = tvDescription.text.toString()
                if (text.isNotEmpty() && text != getString(R.string.consulting_ai)) {
                    // Remove asterisks to avoid reading them out loud
                    val cleanText = text.replace("*", "")
                    textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(this, getString(R.string.initializing_audio), Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            saveAdvice(queryText, tvDescription.text.toString(), isDiseaseAnalysis)
        }

        btnLike.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
        }

        btnDislike.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveAdvice(query: String, advice: String, isDisease: Boolean) {
        val sharedPrefs = getSharedPreferences("kisanmitra_prefs", Context.MODE_PRIVATE)
        val savedData = sharedPrefs.getString("saved_advice", "[]")
        val jsonArray = org.json.JSONArray(savedData)

        val newItem = org.json.JSONObject().apply {
            put("query", query)
            put("advice", advice)
            put("isDisease", isDisease)
            put("timestamp", System.currentTimeMillis())
        }

        jsonArray.put(newItem)
        sharedPrefs.edit().putString("saved_advice", jsonArray.toString()).apply()

        Toast.makeText(this, getString(R.string.advice_saved), Toast.LENGTH_SHORT).show()
    }

    private fun addToHistory(query: String, advice: String, isDisease: Boolean) {
        val sharedPrefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", "") ?: ""
        
        if (email.isEmpty()) return

        lifecycleScope.launch {
            try {
                val request = SaveHistoryRequest(email, query, advice, isDisease)
                RetrofitClient.backendApi.saveHistory(request)
            } catch (e: Exception) {
                android.util.Log.e("AiResponse", "Failed to save history to backend", e)
            }
        }

        // Optional: Keep local SharedPreferences as a fallback cache if needed, 
        // but the requirement is to store in database.
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsInitialized = false
                Toast.makeText(this, "Language not supported for audio", Toast.LENGTH_SHORT).show()
            } else {
                isTtsInitialized = true
            }
        } else {
            isTtsInitialized = false
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
