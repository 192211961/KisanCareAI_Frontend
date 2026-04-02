package com.simats.kisancareai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class ImageAnalysisActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private var isTtsInitialized = false
    private lateinit var tvDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_analysis)

        textToSpeech = TextToSpeech(this, this)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val ivAnalyzedImage = findViewById<ImageView>(R.id.iv_analyzed_image)
        val tvQuery = findViewById<TextView>(R.id.tv_query)
        tvDescription = findViewById(R.id.tv_description)
        val btnPlayAudio = findViewById<Button>(R.id.btn_play_audio)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnLike = findViewById<ImageView>(R.id.btn_like)
        val btnDislike = findViewById<ImageView>(R.id.btn_dislike)
        val btnAskAnother = findViewById<Button>(R.id.btn_ask_another)
        val btnHome = findViewById<Button>(R.id.btn_home)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val queryText = intent.getStringExtra("QUERY_TEXT") ?: "Leaf disease analysis"

        tvQuery.text = queryText

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            ivAnalyzedImage.setImageURI(imageUri)
            
            tvDescription.text = getString(R.string.analyzing_image)
            lifecycleScope.launch {
                val response = KisanAiEngine.getDiseaseAnalysis(this@ImageAnalysisActivity, imageUri)
                tvDescription.text = response
                addToHistory(queryText, response, true)
            }
        } else {
            tvDescription.text = getString(R.string.upload_photo_desc)
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnPlayAudio.setOnClickListener {
            if (isTtsInitialized) {
                val text = tvDescription.text.toString()
                if (text.isNotEmpty() && text != getString(R.string.analyzing_image)) {
                    val cleanText = text.replace("*", "")
                    textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(this, getString(R.string.initializing_audio), Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            saveAdvice(queryText, tvDescription.text.toString(), true)
        }

        btnLike.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        btnDislike.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        btnAskAnother.setOnClickListener {
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
        val sharedPrefs = getSharedPreferences("kisanmitra_prefs", Context.MODE_PRIVATE)
        val historyData = sharedPrefs.getString("query_history", "[]")
        val jsonArray = org.json.JSONArray(historyData)

        if (jsonArray.length() > 0) {
            val lastItem = jsonArray.getJSONObject(jsonArray.length() - 1)
            if (lastItem.optString("query") == query) return
        }

        val newItem = org.json.JSONObject().apply {
            put("query", query)
            put("advice", advice)
            put("isDisease", isDisease)
            put("timestamp", System.currentTimeMillis())
        }

        jsonArray.put(newItem)
        sharedPrefs.edit().putString("query_history", jsonArray.toString()).apply()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            isTtsInitialized = !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
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
