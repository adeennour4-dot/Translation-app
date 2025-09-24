package com.medicaltranslator.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Basic grammar checker for Arabic text
 * Replaces the need for external LanguageTool service
 */
class GrammarChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "GrammarChecker"
    }
    
    private val grammarRules = mutableListOf<GrammarRule>()
    private val commonMistakes = mutableMapOf<Pattern, String>()
    
    init {
        initializeGrammarRules()
        initializeCommonMistakes()
    }
    
    data class GrammarRule(
        val pattern: Pattern,
        val replacement: String,
        val description: String
    )
    
    /**
     * Initialize basic Arabic grammar rules
     */
    private fun initializeGrammarRules() {
        // Article agreement rules
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bال\\s+([أإآ])"),
            "الـ$1",
            "Article attachment"
        ))
        
        // Common spacing issues
        grammarRules.add(GrammarRule(
            Pattern.compile("\\s+([،؛؟!])"),
            "$1",
            "Remove space before punctuation"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("([،؛؟!])([^\\s])"),
            "$1 $2",
            "Add space after punctuation"
        ))
        
        // Number formatting
        grammarRules.add(GrammarRule(
            Pattern.compile("(\\d+)\\s*-\\s*(\\d+)"),
            "$1-$2",
            "Number range formatting"
        ))
        
        // Common word corrections
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bهاذا\\b"),
            "هذا",
            "Correct demonstrative pronoun"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bهاذه\\b"),
            "هذه",
            "Correct demonstrative pronoun"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bاللذي\\b"),
            "الذي",
            "Correct relative pronoun"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bاللتي\\b"),
            "التي",
            "Correct relative pronoun"
        ))
        
        // Medical terminology corrections
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bالمريضة\\s+الذكر\\b"),
            "المريض الذكر",
            "Gender agreement"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bالمريض\\s+الأنثى\\b"),
            "المريضة الأنثى",
            "Gender agreement"
        ))
        
        // Sentence structure improvements
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bيعاني من من\\b"),
            "يعاني من",
            "Remove duplicate preposition"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bفي في\\b"),
            "في",
            "Remove duplicate preposition"
        ))
        
        grammarRules.add(GrammarRule(
            Pattern.compile("\\bعلى على\\b"),
            "على",
            "Remove duplicate preposition"
        ))
    }
    
    /**
     * Initialize common mistakes and their corrections
     */
    private fun initializeCommonMistakes() {
        // Common spelling mistakes in medical Arabic
        commonMistakes[Pattern.compile("\\bالمرض\\b")] = "المريض"
        commonMistakes[Pattern.compile("\\bالدكتور\\b")] = "الطبيب"
        commonMistakes[Pattern.compile("\\bالمستشفا\\b")] = "المستشفى"
        commonMistakes[Pattern.compile("\\bالعلاج\\b")] = "العلاج"
        commonMistakes[Pattern.compile("\\bالفحص\\b")] = "الفحص"
        commonMistakes[Pattern.compile("\\bالتشخيص\\b")] = "التشخيص"
        commonMistakes[Pattern.compile("\\bالاعراض\\b")] = "الأعراض"
        commonMistakes[Pattern.compile("\\bالادوية\\b")] = "الأدوية"
        commonMistakes[Pattern.compile("\\bالاشعة\\b")] = "الأشعة"
        commonMistakes[Pattern.compile("\\bالعملية\\b")] = "العملية"
        
        // Common grammatical mistakes
        commonMistakes[Pattern.compile("\\bهو يعاني\\b")] = "يعاني"
        commonMistakes[Pattern.compile("\\bهي تعاني\\b")] = "تعاني"
        commonMistakes[Pattern.compile("\\bهم يعانون\\b")] = "يعانون"
        commonMistakes[Pattern.compile("\\bهن يعانين\\b")] = "يعانين"
        
        // Medical procedure corrections
        commonMistakes[Pattern.compile("\\bعملية جراحية\\b")] = "عملية جراحية"
        commonMistakes[Pattern.compile("\\bفحص طبي\\b")] = "فحص طبي"
        commonMistakes[Pattern.compile("\\bتحليل دم\\b")] = "فحص دم"
        commonMistakes[Pattern.compile("\\bصورة اشعة\\b")] = "صورة أشعة"
    }
    
    /**
     * Main grammar correction method
     */
    suspend fun correctBasicGrammar(text: String): String = withContext(Dispatchers.IO) {
        try {
            var correctedText = text
            
            // Step 1: Apply grammar rules
            for (rule in grammarRules) {
                correctedText = rule.pattern.matcher(correctedText).replaceAll(rule.replacement)
            }
            
            // Step 2: Fix common mistakes
            for ((pattern, correction) in commonMistakes) {
                correctedText = pattern.matcher(correctedText).replaceAll(correction)
            }
            
            // Step 3: Basic text cleanup
            correctedText = cleanupText(correctedText)
            
            return@withContext correctedText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in grammar correction", e)
            return@withContext text // Return original text on error
        }
    }
    
    /**
     * Clean up text formatting
     */
    private fun cleanupText(text: String): String {
        var cleanText = text
        
        // Remove extra whitespace
        cleanText = cleanText.replace(Regex("\\s+"), " ")
        
        // Fix punctuation spacing
        cleanText = cleanText.replace(Regex("\\s+([.،؛؟!])"), "$1")
        cleanText = cleanText.replace(Regex("([.،؛؟!])([^\\s])"), "$1 $2")
        
        // Fix parentheses spacing
        cleanText = cleanText.replace(Regex("\\s*\\(\\s*"), " (")
        cleanText = cleanText.replace(Regex("\\s*\\)\\s*"), ") ")
        
        // Fix quotation marks
        cleanText = cleanText.replace(Regex("\\s*\"\\s*"), " \"")
        cleanText = cleanText.replace(Regex("\"\\s*([^\"]+)\\s*\""), "\"$1\"")
        
        // Trim and return
        return cleanText.trim()
    }
    
    /**
     * Check if grammar checker is ready
     */
    fun isReady(): Boolean {
        return grammarRules.isNotEmpty() && commonMistakes.isNotEmpty()
    }
    
    /**
     * Get grammar checker statistics
     */
    fun getStats(): Map<String, Int> {
        return mapOf(
            "grammar_rules" to grammarRules.size,
            "common_mistakes" to commonMistakes.size
        )
    }
    
    /**
     * Add custom grammar rule
     */
    fun addCustomRule(pattern: String, replacement: String, description: String) {
        try {
            grammarRules.add(GrammarRule(
                Pattern.compile(pattern),
                replacement,
                description
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error adding custom rule: $pattern", e)
        }
    }
    
    /**
     * Add custom mistake correction
     */
    fun addCustomMistake(mistake: String, correction: String) {
        try {
            commonMistakes[Pattern.compile("\\b$mistake\\b", Pattern.CASE_INSENSITIVE)] = correction
        } catch (e: Exception) {
            Log.e(TAG, "Error adding custom mistake: $mistake", e)
        }
    }
    
    /**
     * Analyze text and return suggestions
     */
    suspend fun analyzeText(text: String): List<GrammarSuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<GrammarSuggestion>()
        
        try {
            // Check against grammar rules
            for (rule in grammarRules) {
                val matcher = rule.pattern.matcher(text)
                while (matcher.find()) {
                    suggestions.add(GrammarSuggestion(
                        start = matcher.start(),
                        end = matcher.end(),
                        original = matcher.group(),
                        suggestion = rule.replacement,
                        description = rule.description,
                        type = "grammar"
                    ))
                }
            }
            
            // Check against common mistakes
            for ((pattern, correction) in commonMistakes) {
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    suggestions.add(GrammarSuggestion(
                        start = matcher.start(),
                        end = matcher.end(),
                        original = matcher.group(),
                        suggestion = correction,
                        description = "Common mistake correction",
                        type = "spelling"
                    ))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing text", e)
        }
        
        return@withContext suggestions
    }
    
    data class GrammarSuggestion(
        val start: Int,
        val end: Int,
        val original: String,
        val suggestion: String,
        val description: String,
        val type: String
    )
}

