package com.simats.kisancareai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VoiceQueryActivity : BaseActivity() {

    private var currentState = 0 // 0 = IDLE, 1 = LISTENING, 2 = TRANSCRIPT
    private var speechRecognizer: android.speech.SpeechRecognizer? = null

    private lateinit var btnMic: FrameLayout
    private lateinit var micIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvSpokenText: TextView
    private lateinit var btnRecordAgain: ImageView
    
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private lateinit var dot5: View
    
    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_query)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        btnMic = findViewById(R.id.btn_mic)
        micIcon = findViewById(R.id.mic_icon)
        tvTitle = findViewById(R.id.tv_title)
        tvSubtitle = findViewById(R.id.tv_subtitle)
        tvSpokenText = findViewById(R.id.tv_spoken_text)
        btnRecordAgain = findViewById(R.id.btn_record_again)
        
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        dot4 = findViewById(R.id.dot4)
        dot5 = findViewById(R.id.dot5)

        updateUI(currentState)

        btnMic.setOnClickListener {
            if (currentState == 0 || currentState == 2) {
                currentState = 1
                updateUI(currentState)
                startListening()
            } else if (currentState == 1) {
                stopListening()
            }
        }

        btnRecordAgain.setOnClickListener {
            currentState = 1 // Go back to listening
            updateUI(currentState)
            startListening()
        }
        
        // Auto-start listening when the activity opens
        btnMic.performClick()
    }

    private fun updateUI(state: Int) {
        when (state) {
            0 -> { // IDLE
                tvTitle.text = "Tap to speak"
                tvSubtitle.text = "Ask your farming question using voice"
                tvSpokenText.visibility = View.INVISIBLE
                stopDotAnimation()
            }
            1 -> { // LISTENING
                tvTitle.text = "Listening..."
                tvSubtitle.text = "Speak clearly into your microphone"
                tvSpokenText.visibility = View.INVISIBLE
                startDotAnimation()
            }
            2 -> { // TRANSCRIPT & ANSWER
                tvTitle.text = "Processing..."
                tvSubtitle.text = "Consulting KisanCare AI"
                startDotAnimation() // Keep animating while thinking
            }
        }
    }
    
    private fun startDotAnimation() {
        if (isAnimating) return
        isAnimating = true
        lifecycleScope.launch {
            val dots = listOf(dot1, dot2, dot3, dot4, dot5)
            var activeIndex = 0
            while (isAnimating) {
                for (i in dots.indices) {
                    if (i == activeIndex) {
                        dots[i].setBackgroundResource(R.drawable.bg_dot_active)
                        dots[i].layoutParams.width = resources.getDimensionPixelSize(R.dimen.active_dot_size)
                    } else {
                        dots[i].setBackgroundResource(R.drawable.bg_dot_inactive)
                        dots[i].layoutParams.width = resources.getDimensionPixelSize(R.dimen.inactive_dot_size)
                    }
                }
                activeIndex = (activeIndex + 1) % dots.size
                kotlinx.coroutines.delay(200)
            }
        }
    }
    
    private fun stopDotAnimation() {
        isAnimating = false
        val dots = listOf(dot1, dot2, dot3, dot4, dot5)
        for (dot in dots) {
            dot.setBackgroundResource(R.drawable.bg_dot_inactive)
            dot.layoutParams.width = resources.getDimensionPixelSize(R.dimen.inactive_dot_size)
        }
    }

    private fun startListening() {
        // Enforce Privacy & Security Setting
        val privacyPrefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)
        val isMicAllowed = privacyPrefs.getBoolean("mic", true)
        
        if (!isMicAllowed) {
            Toast.makeText(this, "Please turn on microphone access in privacy & security", Toast.LENGTH_LONG).show()
            currentState = 0
            updateUI(currentState)
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    tvTitle.text = "Processing..."
                    tvSubtitle.text = "Consulting KisanCare AI"
                }
                override fun onError(error: Int) {
                    Toast.makeText(this@VoiceQueryActivity, "Failed to recognize speech", Toast.LENGTH_SHORT).show()
                    currentState = 0
                    updateUI(currentState)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcribedText = matches?.get(0)
                    if (!transcribedText.isNullOrEmpty()) {
                        processQuery(transcribedText)
                    } else {
                        currentState = 0
                        updateUI(currentState)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun processQuery(transcribedText: String) {
        tvTitle.text = "Processing..."
        tvSubtitle.text = "Consulting KisanCare AI"
        tvSpokenText.text = "\"$transcribedText\""
        tvSpokenText.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            // Navigate directly to AiResponseActivity
            val intent = Intent(this@VoiceQueryActivity, AiResponseActivity::class.java)
            intent.putExtra("QUERY_TEXT", transcribedText)
            intent.putExtra("IS_DISEASE_ANALYSIS", false)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
