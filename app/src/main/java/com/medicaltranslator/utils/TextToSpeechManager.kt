package com.medicaltranslator.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

/**
 * Text-to-Speech manager for reading translated content aloud
 * Supports both English and Arabic languages
 */
class TextToSpeechManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TextToSpeechManager"
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = Locale.ENGLISH
    
    /**
     * Initialize Text-to-Speech engine
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        return@withContext suspendCancellableCoroutine { continuation ->
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    setupTTS()
                    continuation.resume(true)
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                    continuation.resume(false)
                }
            }
        }
    }
    
    /**
     * Setup TTS with default settings
     */
    private fun setupTTS() {
        textToSpeech?.let { tts ->
            // Set default language to English
            val result = tts.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "English language not supported")
            }
            
            // Set speech rate and pitch
            tts.setSpeechRate(0.9f) // Slightly slower for medical content
            tts.setPitch(1.0f)
            
            // Set utterance progress listener
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started for utterance: $utteranceId")
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed for utterance: $utteranceId")
                }
                
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error for utterance: $utteranceId")
                }
            })
        }
    }
    
    /**
     * Speak text in the specified language
     */
    suspend fun speak(
        text: String,
        language: String = "en",
        utteranceId: String = "medical_translation"
    ): Boolean = withContext(Dispatchers.Main) {
        
        if (!isInitialized || textToSpeech == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext false
        }
        
        try {
            // Set language
            val locale = when (language.lowercase()) {
                "ar", "arabic" -> Locale("ar")
                "en", "english" -> Locale.ENGLISH
                else -> Locale.ENGLISH
            }
            
            if (currentLanguage != locale) {
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language $language not supported, using default")
                } else {
                    currentLanguage = locale
                }
            }
            
            // Speak the text
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            return@withContext result == TextToSpeech.SUCCESS
            
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            return@withContext false
        }
    }
    
    /**
     * Speak English text (original)
     */
    suspend fun speakEnglish(text: String): Boolean {
        return speak(text, "en", "english_original")
    }
    
    /**
     * Speak Arabic text (translated)
     */
    suspend fun speakArabic(text: String): Boolean {
        return speak(text, "ar", "arabic_translation")
    }
    
    /**
     * Stop current speech
     */
    fun stop() {
        textToSpeech?.stop()
    }
    
    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
    
    /**
     * Set speech rate (0.5 to 2.0)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Set speech pitch (0.5 to 2.0)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): Set<Locale> {
        return textToSpeech?.availableLanguages ?: emptySet()
    }
    
    /**
     * Check if a language is supported
     */
    fun isLanguageSupported(language: String): Boolean {
        val locale = when (language.lowercase()) {
            "ar", "arabic" -> Locale("ar")
            "en", "english" -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
        
        val result = textToSpeech?.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }
    
    /**
     * Get TTS engine info
     */
    fun getTTSInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        
        textToSpeech?.let { tts ->
            info["isInitialized"] = isInitialized
            info["isSpeaking"] = tts.isSpeaking
            info["currentLanguage"] = currentLanguage.toString()
            info["availableLanguages"] = getAvailableLanguages().map { it.toString() }
            info["englishSupported"] = isLanguageSupported("en")
            info["arabicSupported"] = isLanguageSupported("ar")
        }
        
        return info
    }
    
    /**
     * Speak text with custom parameters
     */
    suspend fun speakWithParams(
        text: String,
        language: String = "en",
        rate: Float = 0.9f,
        pitch: Float = 1.0f,
        utteranceId: String = "custom_speech"
    ): Boolean = withContext(Dispatchers.Main) {
        
        // Set parameters
        setSpeechRate(rate)
        setPitch(pitch)
        
        // Speak
        return@withContext speak(text, language, utteranceId)
    }
    
    /**
     * Speak medical text with appropriate settings
     */
    suspend fun speakMedicalText(
        text: String,
        language: String = "en"
    ): Boolean {
        // Use slower rate and normal pitch for medical content
        return speakWithParams(
            text = text,
            language = language,
            rate = 0.8f, // Slower for clarity
            pitch = 1.0f,
            utteranceId = "medical_content"
        )
    }
    
    /**
     * Speak text in chunks for long content
     */
    suspend fun speakLongText(
        text: String,
        language: String = "en",
        chunkSize: Int = 4000 // TTS has character limits
    ): Boolean {
        if (text.length <= chunkSize) {
            return speak(text, language)
        }
        
        val chunks = text.chunked(chunkSize)
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val utteranceId = "chunk_$i"
            
            val success = speak(chunk, language, utteranceId)
            if (!success) {
                Log.e(TAG, "Failed to speak chunk $i")
                return false
            }
            
            // Wait for current chunk to finish before starting next
            while (isSpeaking()) {
                kotlinx.coroutines.delay(100)
            }
        }
        
        return true
    }
    
    /**
     * Release resources
     */
    fun shutdown() {
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
        isInitialized = false
    }
}

