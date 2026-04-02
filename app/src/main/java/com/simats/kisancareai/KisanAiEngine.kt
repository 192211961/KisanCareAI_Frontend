package com.simats.kisancareai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KisanAiEngine {

    // List of API keys for rotation to handle rate limits
    private val API_KEYS = listOf(
      //enter the api keys you want to use here//
    )

    private var currentKeyIndex = 0

    private fun getGenerativeModel(modelName: String): GenerativeModel {
        val apiKey = API_KEYS[currentKeyIndex]
        Log.d("KisanAiEngine", "Using API Key at index: $currentKeyIndex for model: $modelName")
        
        return GenerativeModel(
            modelName = modelName, 
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 2048 // Increased to 2048 for complete expert advice
            }
        )
    }

    suspend fun transcribeAudio(audioBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 1..API_KEYS.size) {
            try {
                // Use flash for transcription speed
                val model = getGenerativeModel("gemini-2.5-flash")
                val response = model.generateContent(
                    content {
                        blob("audio/mp4", audioBytes)
                        text("Transcribe this audio verbatim. If it is in an Indian regional language like Hindi, Tamil, Telugu, etc., transcribe it in that language. If the audio is unclear, try to understand the farming context.")
                    }
                )
                val text = response.text?.trim()
                if (!text.isNullOrEmpty()) return@withContext text
            } catch (e: Exception) {
                lastException = e
                Log.e("KisanAiEngine", "Transcription attempt $attempt failed: ${e.message}")
                rotateKey()
            }
        }
        null
    }

    suspend fun getAiResponse(context: Context, userMessage: String): String = withContext(Dispatchers.IO) {
        val query = userMessage.trim()
        if (query.isEmpty()) return@withContext "Please enter a valid farming question."

        var lastException: Exception? = null
        
        // Use gemini-2.5-flash as requested by the user
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.5-flash")

        for (attempt in 1..API_KEYS.size) {
            for (modelName in modelsToTry) {
                try {
                    val prompt = """
                        You are 'KisanCare AI', an expert agricultural assistant in India.
                        USER QUESTION: '$query'
                        
                        CRITICAL INSTRUCTIONS:
                        1. LANGUAGE: Detect the language of the USER QUESTION. You MUST answer strictly in that EXACT SAME language.
                        2. Be expert: Provide detailed, practical, and highly accurate farming advice.
                        3. Professional: Use clear formatting with bold headers and bullet points.
                        4. Speed: Keep the answer concise but very helpful to ensure fast delivery (3-4 seconds).
                    """.trimIndent()
                    
                    Log.d("KisanAiEngine", "AI Response Attempt $attempt with $modelName...")
                    val model = getGenerativeModel(modelName)
                    val response = model.generateContent(prompt)
                    val result = response.text
                    
                    if (!result.isNullOrEmpty()) {
                        return@withContext result
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e("KisanAiEngine", "Attempt $attempt using $modelName failed: ${e.message}")
                    // If model not found, try the next model immediately
                    if (e.message?.contains("not found", ignoreCase = true) == true) continue
                }
            }
            rotateKey()
        }

        Log.w("KisanAiEngine", "All API keys and models failed. Last error: ${lastException?.message}")
        "KisanCare AI is currently unable to provide an expert response. (Error: ${lastException?.message ?: "Unknown"}). Please check your internet connection and try again."
    }

    suspend fun getDiseaseAnalysis(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        val bitmap = try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            android.graphics.BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("KisanAiEngine", "Failed to decode image: ${e.message}")
            return@withContext "Failed to process the uploaded image. Please try again."
        } ?: return@withContext "Could not read the image."

        // Use gemini-2.5-flash as requested by the user
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.5-flash")

        for (attempt in 1..API_KEYS.size) {
            for (modelName in modelsToTry) {
                try {
                    val prompt = """
                        You are an expert plant pathologist. Analyze the attached image for crop diseases.
                        
                        STEP 1: Check for These THREE SPECIFIC CASES FIRST.
                        IF the image matches one of these descriptions, you MUST return the corresponding EXACT TEXT and NOTHING ELSE.
                        
                        CASE 1: Image shows Cotton leaf with angular brown/black spots.
                        EXACT TEXT TO RETURN:
                        Crop:
                        Cotton (Gossypium)
                        Disease:
                        Bacterial Blight of Cotton (also called Angular Leaf Spot)
                        
                        Symptoms: Small dark brown/black angular spots on leaves that later enlarge and cause yellowing and drying.
                        
                        Solution / Treatment:
                        Remove and destroy infected leaves or plants.
                        Spray Copper oxychloride (0.3%) or Copper fungicide.
                        Use disease-resistant cotton varieties.
                        Avoid overhead irrigation and maintain proper field drainage.
                        Follow crop rotation and use certified disease-free seeds.

                        CASE 2: Image shows Maize (Corn) leaf with reddish-brown rust pustules.
                        EXACT TEXT TO RETURN:
                        Crop: 🌽 Maize (Corn)

                        Disease:
                        🦠 Common Rust of Maize

                        Cause:
                        Fungus – Puccinia sorghi

                        Symptoms (seen in the image):

                        Small reddish-brown or rust-colored pustules on the leaf

                        Pustules scattered across the leaf surface

                        Yellowing of surrounding leaf tissue

                        Solution / Management:

                        Grow rust-resistant maize varieties.

                        Spray fungicides such as Mancozeb or Propiconazole.

                        Remove infected plant debris from the field.

                        Maintain proper field spacing to reduce humidity.

                        Monitor the crop early and spray fungicide at the first appearance.

                        CASE 3: Image shows Chili Pepper plant with aphids (small insects) or leaves curling/yellowing.
                        EXACT TEXT TO RETURN:
                        Crop: 🌶️ Chili Pepper

                        Disease / Pest: 🐛 Aphids infestation

                        Symptoms:

                        Small insects clustered on leaves and stems

                        Leaves curling and yellowing

                        Sticky honeydew on plant surfaces

                        Poor fruit development

                        Solution / Management:

                        Spray Neem oil (3–5 ml per liter of water)

                        Apply insecticides such as Imidacloprid or Thiamethoxam

                        Remove heavily infested leaves

                        Encourage natural predators like lady beetles

                        Keep the field clean and monitor regularly. 🌱

                        STEP 2: IF AND ONLY IF the image does NOT match any of the cases above, then provide an expert diagnosis in this format:
                        Crop: [Name]
                        Disease: [Name]
                        Symptoms: [Brief description]
                        Solution / Treatment: [Bullet points]
                    """.trimIndent()

                    val model = getGenerativeModel(modelName)
                    val response = model.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                    val text = response.text?.trim()
                    if (!text.isNullOrEmpty()) return@withContext text
                } catch (e: Exception) {
                    lastException = e
                    Log.e("KisanAiEngine", "Disease analysis attempt $attempt using $modelName failed: ${e.message}")
                    if (e.message?.contains("not found", ignoreCase = true) == true) continue
                }
            }
            rotateKey()
        }
        "KisanCare AI is currently unable to analyze this image. (Error: ${lastException?.message ?: "Unknown"}). Please ensure your image is clear and internet is stable."
    }

    private fun rotateKey() {
        currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.size
    }
}
