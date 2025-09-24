package com.medicaltranslator.services

import android.content.Context
import android.util.Log
import com.medicaltranslator.data.MedicalDictionary
import com.medicaltranslator.data.NormalDictionary
import com.medicaltranslator.ml.SimpleTranslationModel
import com.medicaltranslator.utils.GrammarChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enhanced offline translation service that doesn't rely on external dependencies
 * Replaces the Termux-dependent TranslationService with a fully standalone solution
 */
class OfflineTranslationService(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineTranslationService"
    }
    
    private lateinit var medicalDictionary: MedicalDictionary
    private lateinit var normalDictionary: NormalDictionary
    private lateinit var translationModel: SimpleTranslationModel
    private lateinit var grammarChecker: GrammarChecker
    
    init {
        initializeComponents()
    }
    
    private fun initializeComponents() {
        medicalDictionary = MedicalDictionary(context)
        normalDictionary = NormalDictionary(context)
        translationModel = SimpleTranslationModel(context)
        grammarChecker = GrammarChecker(context)
    }
    
    /**
     * Main translation pipeline: Dictionary → Pattern-based AI → Grammar correction
     * This is a fully offline implementation that doesn't require external services
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
                
                // Step 2: Pattern-based translation
                progressCallback(progress + 40f, "Translating page ${i + 1}...")
                val translated = translationModel.translate(dictionaryEnhanced)
                
                // Step 3: Basic grammar correction
                progressCallback(progress + 70f, "Grammar correction for page ${i + 1}...")
                val grammarCorrected = grammarChecker.correctBasicGrammar(translated)
                
                translatedTexts.add(grammarCorrected)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error translating page ${i + 1}", e)
                // Fallback: add dictionary-only translation
                translatedTexts.add(fallbackTranslation(text))
            }
        }
        
        return@withContext translatedTexts
    }
    
    /**
     * Enhance text with medical and normal dictionary lookups
     */
    private suspend fun enhanceWithDictionary(text: String): String = withContext(Dispatchers.IO) {
        val sentences = text.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        val enhancedSentences = mutableListOf<String>()
        
        for (sentence in sentences) {
            val words = sentence.trim().split(Regex("\\s+"))
            val enhancedWords = mutableListOf<String>()
            
            for (word in words) {
                val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
                val medicalTranslation = medicalDictionary.lookupWord(cleanWord)
                val normalTranslation = normalDictionary.lookupWord(cleanWord)
                
                when {
                    medicalTranslation != null -> {
                        // Prioritize medical dictionary
                        enhancedWords.add("$word [MED:$medicalTranslation]")
                    }
                    normalTranslation != null -> {
                        // Use normal dictionary if no medical translation
                        enhancedWords.add("$word [NORM:$normalTranslation]")
                    }
                    else -> {
                        enhancedWords.add(word)
                    }
                }
            }
            
            enhancedSentences.add(enhancedWords.joinToString(" "))
        }
        
        return@withContext enhancedSentences.joinToString(". ")
    }
    
    /**
     * Fallback translation using only dictionary lookups
     */
    private suspend fun fallbackTranslation(text: String): String = withContext(Dispatchers.IO) {
        val words = text.split(Regex("\\s+"))
        val translatedWords = mutableListOf<String>()
        
        for (word in words) {
            val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
            val medicalTranslation = medicalDictionary.lookupWord(cleanWord)
            val normalTranslation = normalDictionary.lookupWord(cleanWord)
            
            when {
                medicalTranslation != null -> translatedWords.add(medicalTranslation)
                normalTranslation != null -> translatedWords.add(normalTranslation)
                else -> translatedWords.add(word) // Keep original if no translation found
            }
        }
        
        return@withContext translatedWords.joinToString(" ")
    }
    
    /**
     * Get translation service status - always available since it's offline
     */
    suspend fun getServiceStatus(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext mapOf(
            "dictionary" to true,
            "translation_model" to translationModel.isReady(),
            "grammar_checker" to grammarChecker.isReady(),
            "offline_mode" to true
        )
    }
    
    /**
     * Check if all components are ready
     */
    suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        return@withContext translationModel.isReady() && grammarChecker.isReady()
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "ar" to "Arabic"
        )
    }
    
    /**
     * Get translation statistics
     */
    suspend fun getTranslationStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        return@withContext mapOf(
            "medical_terms" to medicalDictionary.getTermCount(),
            "normal_terms" to normalDictionary.getTermCount(),
            "total_translations" to 0 // Could be tracked in preferences
        )
    }
}

