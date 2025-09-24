package com.medicaltranslator.services

import android.content.Context
import android.util.Log
import com.medicaltranslator.data.MedicalDictionary
import com.medicaltranslator.data.NormalDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TranslationService(private val context: Context) {
    
    companion object {
        private const val TAG = "TranslationService"
        private const val LANGUAGETOOL_URL = "http://127.0.0.1:8010/v2/check"
        private const val LLAMA_CPP_URL = "http://127.0.0.1:8080/completion"
        private const val CONNECTION_TIMEOUT = 10L
        private const val READ_TIMEOUT = 30L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private lateinit var medicalDictionary: MedicalDictionary
    private lateinit var normalDictionary: NormalDictionary
    
    init {
        medicalDictionary = MedicalDictionary(context)
        normalDictionary = NormalDictionary(context)
    }
    
    /**
     * Main translation pipeline: Dictionary → AI → Grammar correction
     */
    suspend fun translateText(
        texts: List<String>,
        progressCallback: (Float, String) -> Unit = { _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        
        val translatedTexts = mutableListOf<String>()
        val totalTexts = texts.size
        
        for (i in texts.indices) {
            val text = texts[i]
            val progress = (i.toFloat() / totalTexts) * 100f
            
            try {
                // Step 1: Dictionary lookup and preprocessing
                progressCallback(progress + 10f, "Dictionary lookup for page ${i + 1}...")
                val dictionaryEnhanced = enhanceWithDictionary(text)
                
                // Step 2: AI translation with llama.cpp
                progressCallback(progress + 30f, "AI translation for page ${i + 1}...")
                val aiTranslated = translateWithAI(dictionaryEnhanced)
                
                // Step 3: Grammar correction with LanguageTool
                progressCallback(progress + 50f, "Grammar correction for page ${i + 1}...")
                val grammarCorrected = correctGrammar(aiTranslated)
                
                translatedTexts.add(grammarCorrected)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error translating page ${i + 1}", e)
                // Fallback: add original text with error note
                translatedTexts.add("Translation Error: ${e.message}\n\nOriginal text:\n$text")
            }
        }
        
        return@withContext translatedTexts
    }
    
    /**
     * Enhance text with medical dictionary lookups
     */
    private suspend fun enhanceWithDictionary(text: String): String = withContext(Dispatchers.IO) {
        val words = text.split(Regex("\\s+"))
        val enhancedWords = mutableListOf<String>()
        
        for (word in words) {
            val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
            val medicalTranslation = medicalDictionary.lookupWord(cleanWord)
            val normalTranslation = normalDictionary.lookupWord(cleanWord)
            
            if (medicalTranslation != null) {
                // Prioritize medical dictionary
                enhancedWords.add("$word [Medical: $medicalTranslation]")
            } else if (normalTranslation != null) {
                // Use normal dictionary if no medical translation
                enhancedWords.add("$word [Normal: $normalTranslation]")
            } else {
                enhancedWords.add(word)
            }
        }
        
        return@withContext enhancedWords.joinToString(" ")
    }
    
    /**
     * Translate text using llama.cpp AI model
     */
    private suspend fun translateWithAI(text: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildTranslationPrompt(text)
            val requestJson = JSONObject().apply {
                put("prompt", prompt)
                put("n_predict", 2048)
                put("temperature", 0.1)
                put("top_k", 40)
                put("top_p", 0.9)
                put("repeat_penalty", 1.1)
                put("stop", JSONArray().apply {
                    put("</translation>")
                    put("Human:")
                    put("Assistant:")
                })
            }
            
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(LLAMA_CPP_URL)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val content = jsonResponse.optString("content", "")
                
                // Extract translation from response
                return@withContext extractTranslation(content)
            } else {
                Log.e(TAG, "AI translation failed: ${response.code}")
                throw IOException("AI translation service unavailable")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI translation", e)
            // Fallback: return original text
            return@withContext text
        }
    }
    
    /**
     * Build translation prompt for AI model
     */
    private fun buildTranslationPrompt(text: String): String {
        return """
You are a professional medical translator specializing in English to Arabic translation. 
Translate the following English medical text to Arabic with high accuracy.

Instructions:
- Maintain medical terminology precision
- Preserve formatting and structure
- Use formal Arabic
- Keep medical terms accurate
- If you see [Medical: translation] annotations, use those for medical terms

<text>
$text
</text>

<translation>
""".trimIndent()
    }
    
    /**
     * Extract translation from AI response
     */
    private fun extractTranslation(response: String): String {
        // Remove the prompt and extract only the translation
        val lines = response.split("\n")
        val translationStart = lines.indexOfFirst { it.contains("<translation>") }
        
        return if (translationStart != -1) {
            lines.drop(translationStart + 1)
                .takeWhile { !it.contains("</translation>") }
                .joinToString("\n")
                .trim()
        } else {
            response.trim()
        }
    }
    
    /**
     * Correct grammar using LanguageTool
     */
    private suspend fun correctGrammar(text: String): String = withContext(Dispatchers.IO) {
        try {
            val formData = FormBody.Builder()
                .add("text", text)
                .add("language", "ar") // Arabic language code
                .add("enabledOnly", "false")
                .build()
            
            val request = Request.Builder()
                .url(LANGUAGETOOL_URL)
                .post(formData)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                
                return@withContext applyGrammarCorrections(text, jsonResponse)
            } else {
                Log.w(TAG, "Grammar correction service unavailable: ${response.code}")
                return@withContext text
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Grammar correction failed, using original text", e)
            return@withContext text
        }
    }
    
    /**
     * Apply grammar corrections from LanguageTool response
     */
    private fun applyGrammarCorrections(originalText: String, response: JSONObject): String {
        try {
            val matches = response.getJSONArray("matches")
            var correctedText = originalText
            
            // Apply corrections in reverse order to maintain positions
            val corrections = mutableListOf<Triple<Int, Int, String>>()
            
            for (i in 0 until matches.length()) {
                val match = matches.getJSONObject(i)
                val offset = match.getInt("offset")
                val length = match.getInt("length")
                val replacements = match.getJSONArray("replacements")
                
                if (replacements.length() > 0) {
                    val replacement = replacements.getJSONObject(0).getString("value")
                    corrections.add(Triple(offset, length, replacement))
                }
            }
            
            // Sort by offset in descending order
            corrections.sortByDescending { it.first }
            
            // Apply corrections
            for ((offset, length, replacement) in corrections) {
                correctedText = correctedText.substring(0, offset) + 
                               replacement + 
                               correctedText.substring(offset + length)
            }
            
            return correctedText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying grammar corrections", e)
            return originalText
        }
    }
    
    /**
     * Check if Termux services are running
     */
    suspend fun checkTermuxServices(): Boolean = withContext(Dispatchers.IO) {
        val languageToolAvailable = checkServiceAvailability(LANGUAGETOOL_URL)
        val llamaAvailable = checkServiceAvailability(LLAMA_CPP_URL)
        
        Log.d(TAG, "LanguageTool available: $languageToolAvailable")
        Log.d(TAG, "Llama.cpp available: $llamaAvailable")
        
        return@withContext languageToolAvailable && llamaAvailable
    }
    
    /**
     * Check if a service is available
     */
    private suspend fun checkServiceAvailability(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            val available = response.isSuccessful || response.code == 405 // Some services return 405 for GET
            response.close()
            
            return@withContext available
            
        } catch (e: Exception) {
            Log.d(TAG, "Service check failed for $url: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Get translation service status
     */
    suspend fun getServiceStatus(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext mapOf(
            "dictionary" to true, // Always available offline
            "languagetool" to checkServiceAvailability(LANGUAGETOOL_URL),
            "llama" to checkServiceAvailability(LLAMA_CPP_URL)
        )
    }
}

