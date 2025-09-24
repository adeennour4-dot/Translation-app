package com.medicaltranslator.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Simple pattern-based translation model for medical texts
 * This replaces the need for external AI services like llama.cpp
 */
class SimpleTranslationModel(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleTranslationModel"
    }
    
    private val medicalPatterns = mutableMapOf<Pattern, String>()
    private val commonPhrases = mutableMapOf<String, String>()
    private val grammarRules = mutableListOf<Pair<Pattern, String>>()
    
    init {
        initializePatterns()
        initializeCommonPhrases()
        initializeGrammarRules()
    }
    
    /**
     * Initialize medical translation patterns
     */
    private fun initializePatterns() {
        // Medical procedure patterns
        medicalPatterns[Pattern.compile("\\b(take|taking)\\s+(\\w+)\\s+(medication|medicine|drug|pill)\\b", Pattern.CASE_INSENSITIVE)] = 
            "تناول $2 (دواء)"
        
        medicalPatterns[Pattern.compile("\\b(patient|the patient)\\s+(has|have|is having)\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE)] = 
            "المريض يعاني من $3"
        
        medicalPatterns[Pattern.compile("\\b(diagnosis|diagnosed with)\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE)] = 
            "تشخيص $2"
        
        medicalPatterns[Pattern.compile("\\b(symptoms?|symptom)\\s+(include|includes?)\\s+([^.]+)\\b", Pattern.CASE_INSENSITIVE)] = 
            "الأعراض تشمل $3"
        
        medicalPatterns[Pattern.compile("\\b(treatment|therapy)\\s+(for|of)\\s+(\\w+)\\b", Pattern.CASE_INSENSITIVE)] = 
            "علاج $3"
        
        medicalPatterns[Pattern.compile("\\b(blood pressure|BP)\\s+(is|was)\\s+(\\d+/\\d+)\\b", Pattern.CASE_INSENSITIVE)] = 
            "ضغط الدم $3"
        
        medicalPatterns[Pattern.compile("\\b(heart rate|pulse)\\s+(is|was)\\s+(\\d+)\\s+bpm\\b", Pattern.CASE_INSENSITIVE)] = 
            "معدل ضربات القلب $3 نبضة في الدقيقة"
        
        medicalPatterns[Pattern.compile("\\b(temperature|temp)\\s+(is|was)\\s+(\\d+\\.?\\d*)\\s*°?[CF]?\\b", Pattern.CASE_INSENSITIVE)] = 
            "درجة الحرارة $3"
        
        // Body parts and systems
        medicalPatterns[Pattern.compile("\\b(chest pain|chest discomfort)\\b", Pattern.CASE_INSENSITIVE)] = "ألم في الصدر"
        medicalPatterns[Pattern.compile("\\b(abdominal pain|stomach pain)\\b", Pattern.CASE_INSENSITIVE)] = "ألم في البطن"
        medicalPatterns[Pattern.compile("\\b(headache|head pain)\\b", Pattern.CASE_INSENSITIVE)] = "صداع"
        medicalPatterns[Pattern.compile("\\b(back pain|backache)\\b", Pattern.CASE_INSENSITIVE)] = "ألم في الظهر"
        medicalPatterns[Pattern.compile("\\b(shortness of breath|difficulty breathing)\\b", Pattern.CASE_INSENSITIVE)] = "ضيق في التنفس"
        medicalPatterns[Pattern.compile("\\b(nausea|feeling sick)\\b", Pattern.CASE_INSENSITIVE)] = "غثيان"
        medicalPatterns[Pattern.compile("\\b(vomiting|throwing up)\\b", Pattern.CASE_INSENSITIVE)] = "قيء"
        medicalPatterns[Pattern.compile("\\b(diarrhea|loose stools)\\b", Pattern.CASE_INSENSITIVE)] = "إسهال"
        medicalPatterns[Pattern.compile("\\b(constipation|difficulty passing stools)\\b", Pattern.CASE_INSENSITIVE)] = "إمساك"
        medicalPatterns[Pattern.compile("\\b(fever|high temperature)\\b", Pattern.CASE_INSENSITIVE)] = "حمى"
        medicalPatterns[Pattern.compile("\\b(cough|coughing)\\b", Pattern.CASE_INSENSITIVE)] = "سعال"
        medicalPatterns[Pattern.compile("\\b(fatigue|tiredness|exhaustion)\\b", Pattern.CASE_INSENSITIVE)] = "إرهاق"
        medicalPatterns[Pattern.compile("\\b(dizziness|feeling dizzy)\\b", Pattern.CASE_INSENSITIVE)] = "دوخة"
        medicalPatterns[Pattern.compile("\\b(swelling|inflammation)\\b", Pattern.CASE_INSENSITIVE)] = "تورم"
        medicalPatterns[Pattern.compile("\\b(rash|skin irritation)\\b", Pattern.CASE_INSENSITIVE)] = "طفح جلدي"
    }
    
    /**
     * Initialize common medical phrases
     */
    private fun initializeCommonPhrases() {
        commonPhrases["medical history"] = "التاريخ الطبي"
        commonPhrases["family history"] = "التاريخ العائلي"
        commonPhrases["physical examination"] = "الفحص الجسدي"
        commonPhrases["laboratory tests"] = "الفحوصات المخبرية"
        commonPhrases["blood test"] = "فحص الدم"
        commonPhrases["urine test"] = "فحص البول"
        commonPhrases["x-ray"] = "أشعة سينية"
        commonPhrases["CT scan"] = "أشعة مقطعية"
        commonPhrases["MRI scan"] = "رنين مغناطيسي"
        commonPhrases["ultrasound"] = "موجات فوق صوتية"
        commonPhrases["prescription"] = "وصفة طبية"
        commonPhrases["medication"] = "دواء"
        commonPhrases["dosage"] = "الجرعة"
        commonPhrases["side effects"] = "الآثار الجانبية"
        commonPhrases["allergic reaction"] = "رد فعل تحسسي"
        commonPhrases["emergency room"] = "غرفة الطوارئ"
        commonPhrases["intensive care"] = "العناية المركزة"
        commonPhrases["surgery"] = "جراحة"
        commonPhrases["operation"] = "عملية"
        commonPhrases["recovery"] = "الشفاء"
        commonPhrases["follow-up"] = "متابعة"
        commonPhrases["appointment"] = "موعد"
        commonPhrases["consultation"] = "استشارة"
        commonPhrases["second opinion"] = "رأي ثاني"
        commonPhrases["chronic condition"] = "حالة مزمنة"
        commonPhrases["acute condition"] = "حالة حادة"
        commonPhrases["stable condition"] = "حالة مستقرة"
        commonPhrases["critical condition"] = "حالة حرجة"
        commonPhrases["vital signs"] = "العلامات الحيوية"
        commonPhrases["medical record"] = "السجل الطبي"
        commonPhrases["health insurance"] = "التأمين الصحي"
        commonPhrases["medical certificate"] = "شهادة طبية"
    }
    
    /**
     * Initialize basic grammar rules for Arabic
     */
    private fun initializeGrammarRules() {
        // Basic sentence structure rules
        grammarRules.add(Pattern.compile("\\bthe patient\\b", Pattern.CASE_INSENSITIVE) to "المريض")
        grammarRules.add(Pattern.compile("\\bthe doctor\\b", Pattern.CASE_INSENSITIVE) to "الطبيب")
        grammarRules.add(Pattern.compile("\\bthe nurse\\b", Pattern.CASE_INSENSITIVE) to "الممرضة")
        grammarRules.add(Pattern.compile("\\bthe hospital\\b", Pattern.CASE_INSENSITIVE) to "المستشفى")
        grammarRules.add(Pattern.compile("\\bthe clinic\\b", Pattern.CASE_INSENSITIVE) to "العيادة")
        
        // Time expressions
        grammarRules.add(Pattern.compile("\\btoday\\b", Pattern.CASE_INSENSITIVE) to "اليوم")
        grammarRules.add(Pattern.compile("\\byesterday\\b", Pattern.CASE_INSENSITIVE) to "أمس")
        grammarRules.add(Pattern.compile("\\btomorrow\\b", Pattern.CASE_INSENSITIVE) to "غداً")
        grammarRules.add(Pattern.compile("\\bnow\\b", Pattern.CASE_INSENSITIVE) to "الآن")
        grammarRules.add(Pattern.compile("\\blater\\b", Pattern.CASE_INSENSITIVE) to "لاحقاً")
        
        // Common verbs
        grammarRules.add(Pattern.compile("\\bis\\b", Pattern.CASE_INSENSITIVE) to "هو/هي")
        grammarRules.add(Pattern.compile("\\bare\\b", Pattern.CASE_INSENSITIVE) to "هم/هن")
        grammarRules.add(Pattern.compile("\\bwas\\b", Pattern.CASE_INSENSITIVE) to "كان")
        grammarRules.add(Pattern.compile("\\bwere\\b", Pattern.CASE_INSENSITIVE) to "كانوا")
        grammarRules.add(Pattern.compile("\\bhas\\b", Pattern.CASE_INSENSITIVE) to "لديه")
        grammarRules.add(Pattern.compile("\\bhave\\b", Pattern.CASE_INSENSITIVE) to "لديهم")
        grammarRules.add(Pattern.compile("\\bwill\\b", Pattern.CASE_INSENSITIVE) to "سوف")
        grammarRules.add(Pattern.compile("\\bcan\\b", Pattern.CASE_INSENSITIVE) to "يمكن")
        grammarRules.add(Pattern.compile("\\bshould\\b", Pattern.CASE_INSENSITIVE) to "يجب")
        grammarRules.add(Pattern.compile("\\bmust\\b", Pattern.CASE_INSENSITIVE) to "يجب")
    }
    
    /**
     * Main translation method
     */
    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        try {
            var translatedText = text
            
            // Step 1: Apply medical patterns
            for ((pattern, replacement) in medicalPatterns) {
                translatedText = pattern.matcher(translatedText).replaceAll(replacement)
            }
            
            // Step 2: Apply common phrases
            for ((phrase, translation) in commonPhrases) {
                translatedText = translatedText.replace(phrase, translation, ignoreCase = true)
            }
            
            // Step 3: Apply grammar rules
            for ((pattern, replacement) in grammarRules) {
                translatedText = pattern.matcher(translatedText).replaceAll(replacement)
            }
            
            // Step 4: Clean up annotations from dictionary lookups
            translatedText = cleanupAnnotations(translatedText)
            
            return@withContext translatedText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in translation", e)
            return@withContext text // Return original text on error
        }
    }
    
    /**
     * Clean up dictionary annotations and apply translations
     */
    private fun cleanupAnnotations(text: String): String {
        var cleanText = text
        
        // Replace [MED:translation] with just the translation
        val medPattern = Pattern.compile("\\w+\\s*\\[MED:([^\\]]+)\\]")
        var matcher = medPattern.matcher(cleanText)
        while (matcher.find()) {
            cleanText = cleanText.replace(matcher.group(), matcher.group(1))
            matcher = medPattern.matcher(cleanText)
        }
        
        // Replace [NORM:translation] with just the translation
        val normPattern = Pattern.compile("\\w+\\s*\\[NORM:([^\\]]+)\\]")
        matcher = normPattern.matcher(cleanText)
        while (matcher.find()) {
            cleanText = cleanText.replace(matcher.group(), matcher.group(1))
            matcher = normPattern.matcher(cleanText)
        }
        
        return cleanText.trim()
    }
    
    /**
     * Check if the model is ready
     */
    fun isReady(): Boolean {
        return medicalPatterns.isNotEmpty() && commonPhrases.isNotEmpty() && grammarRules.isNotEmpty()
    }
    
    /**
     * Get model statistics
     */
    fun getModelStats(): Map<String, Int> {
        return mapOf(
            "medical_patterns" to medicalPatterns.size,
            "common_phrases" to commonPhrases.size,
            "grammar_rules" to grammarRules.size
        )
    }
    
    /**
     * Add custom pattern
     */
    fun addCustomPattern(pattern: String, translation: String) {
        try {
            medicalPatterns[Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)] = translation
        } catch (e: Exception) {
            Log.e(TAG, "Error adding custom pattern: $pattern", e)
        }
    }
    
    /**
     * Add custom phrase
     */
    fun addCustomPhrase(phrase: String, translation: String) {
        commonPhrases[phrase.lowercase()] = translation
    }
}

