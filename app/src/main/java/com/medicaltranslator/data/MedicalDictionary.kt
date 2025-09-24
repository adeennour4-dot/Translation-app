package com.medicaltranslator.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MedicalDictionary(private val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    
    companion object {
        private const val TAG = "MedicalDictionary"
        private const val DATABASE_NAME = "medical_dictionary.db"
        private const val DATABASE_VERSION = 1
        
        // Table structure
        private const val TABLE_DICTIONARY = "dictionary"
        private const val COLUMN_ID = "id"
        private const val COLUMN_ENGLISH = "english"
        private const val COLUMN_ARABIC = "arabic"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_DEFINITION = "definition"
        
        // SQL statements
        private const val CREATE_TABLE_DICTIONARY = """
            CREATE TABLE $TABLE_DICTIONARY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ENGLISH TEXT NOT NULL UNIQUE,
                $COLUMN_ARABIC TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_DEFINITION TEXT
            )
        """
        
        private const val CREATE_INDEX_ENGLISH = """
            CREATE INDEX idx_english ON $TABLE_DICTIONARY($COLUMN_ENGLISH)
        """
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_DICTIONARY)
        db.execSQL(CREATE_INDEX_ENGLISH)
        
        // Populate with initial medical terms
        populateInitialData(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DICTIONARY")
        onCreate(db)
    }
    
    /**
     * Look up a word in the medical dictionary
     */
    fun lookupWord(englishWord: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DICTIONARY,
            arrayOf(COLUMN_ARABIC),
            "$COLUMN_ENGLISH = ? COLLATE NOCASE",
            arrayOf(englishWord.lowercase()),
            null, null, null
        )
        
        return if (cursor.moveToFirst()) {
            val translation = cursor.getString(0)
            cursor.close()
            translation
        } else {
            cursor.close()
            null
        }
    }
    
    /**
     * Add a new word to the dictionary
     */
    fun addWord(english: String, arabic: String, category: String? = null, definition: String? = null): Boolean {
        val db = writableDatabase
        return try {
            val values = android.content.ContentValues().apply {
                put(COLUMN_ENGLISH, english.lowercase())
                put(COLUMN_ARABIC, arabic)
                put(COLUMN_CATEGORY, category)
                put(COLUMN_DEFINITION, definition)
            }
            
            val result = db.insertWithOnConflict(
                TABLE_DICTIONARY, 
                null, 
                values, 
                SQLiteDatabase.CONFLICT_REPLACE
            )
            
            result != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error adding word to dictionary", e)
            false
        }
    }
    
    /**
     * Search for words by category
     */
    fun getWordsByCategory(category: String): List<Pair<String, String>> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DICTIONARY,
            arrayOf(COLUMN_ENGLISH, COLUMN_ARABIC),
            "$COLUMN_CATEGORY = ?",
            arrayOf(category),
            null, null, COLUMN_ENGLISH
        )
        
        val words = mutableListOf<Pair<String, String>>()
        while (cursor.moveToNext()) {
            words.add(Pair(cursor.getString(0), cursor.getString(1)))
        }
        cursor.close()
        
        return words
    }
    
    /**
     * Get all categories
     */
    fun getCategories(): List<String> {
        val db = readableDatabase
        val cursor = db.query(
            true, // distinct
            TABLE_DICTIONARY,
            arrayOf(COLUMN_CATEGORY),
            "$COLUMN_CATEGORY IS NOT NULL",
            null, null, null, COLUMN_CATEGORY, null
        )
        
        val categories = mutableListOf<String>()
        while (cursor.moveToNext()) {
            categories.add(cursor.getString(0))
        }
        cursor.close()
        
        return categories
    }
    
    /**
     * Get dictionary statistics
     */
    fun getStatistics(): Map<String, Int> {
        val db = readableDatabase
        val totalCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_DICTIONARY", null)
        val total = if (totalCursor.moveToFirst()) totalCursor.getInt(0) else 0
        totalCursor.close()
        
        val categoryCursor = db.rawQuery(
            "SELECT COUNT(DISTINCT $COLUMN_CATEGORY) FROM $TABLE_DICTIONARY WHERE $COLUMN_CATEGORY IS NOT NULL", 
            null
        )
        val categories = if (categoryCursor.moveToFirst()) categoryCursor.getInt(0) else 0
        categoryCursor.close()
        
        return mapOf(
            "total_words" to total,
            "categories" to categories
        )
    }
    
    /**
     * Populate database with initial medical terms
     */
    private fun populateInitialData(db: SQLiteDatabase) {
        try {
            // Load medical terms from JSON file in assets
            val inputStream = context.assets.open("medical_dictionary.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            val jsonObject = JSONObject(jsonString)
            val terms = jsonObject.getJSONArray("terms")
            
            db.beginTransaction()
            try {
                for (i in 0 until terms.length()) {
                    val term = terms.getJSONObject(i)
                    val english = term.getString("english")
                    val arabic = term.getString("arabic")
                    val category = term.optString("category", null)
                    val definition = term.optString("definition", null)
                    
                    val values = android.content.ContentValues().apply {
                        put(COLUMN_ENGLISH, english.lowercase())
                        put(COLUMN_ARABIC, arabic)
                        put(COLUMN_CATEGORY, category)
                        put(COLUMN_DEFINITION, definition)
                    }
                    
                    db.insert(TABLE_DICTIONARY, null, values)
                }
                
                db.setTransactionSuccessful()
                Log.d(TAG, "Successfully loaded ${terms.length()} medical terms")
                
            } finally {
                db.endTransaction()
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not load medical dictionary from assets, using fallback data", e)
            populateFallbackData(db)
        }
    }
    
    /**
     * Populate with fallback medical terms if JSON file is not available
     */
    private fun populateFallbackData(db: SQLiteDatabase) {
        val fallbackTerms = listOf(
            Triple("heart", "قلب", "anatomy"),
            Triple("blood", "دم", "anatomy"),
            Triple("brain", "دماغ", "anatomy"),
            Triple("lung", "رئة", "anatomy"),
            Triple("liver", "كبد", "anatomy"),
            Triple("kidney", "كلية", "anatomy"),
            Triple("stomach", "معدة", "anatomy"),
            Triple("bone", "عظم", "anatomy"),
            Triple("muscle", "عضلة", "anatomy"),
            Triple("nerve", "عصب", "anatomy"),
            
            Triple("pain", "ألم", "symptoms"),
            Triple("fever", "حمى", "symptoms"),
            Triple("headache", "صداع", "symptoms"),
            Triple("nausea", "غثيان", "symptoms"),
            Triple("dizziness", "دوخة", "symptoms"),
            Triple("fatigue", "إرهاق", "symptoms"),
            Triple("cough", "سعال", "symptoms"),
            Triple("breathing", "تنفس", "symptoms"),
            
            Triple("medicine", "دواء", "treatment"),
            Triple("surgery", "جراحة", "treatment"),
            Triple("therapy", "علاج", "treatment"),
            Triple("injection", "حقنة", "treatment"),
            Triple("prescription", "وصفة طبية", "treatment"),
            Triple("treatment", "علاج", "treatment"),
            Triple("diagnosis", "تشخيص", "treatment"),
            Triple("examination", "فحص", "treatment"),
            
            Triple("doctor", "طبيب", "medical_staff"),
            Triple("nurse", "ممرض", "medical_staff"),
            Triple("patient", "مريض", "medical_staff"),
            Triple("hospital", "مستشفى", "medical_staff"),
            Triple("clinic", "عيادة", "medical_staff"),
            
            Triple("diabetes", "السكري", "diseases"),
            Triple("hypertension", "ارتفاع ضغط الدم", "diseases"),
            Triple("cancer", "سرطان", "diseases"),
            Triple("infection", "عدوى", "diseases"),
            Triple("inflammation", "التهاب", "diseases"),
            Triple("allergy", "حساسية", "diseases"),
            Triple("asthma", "الربو", "diseases"),
        )
        
        db.beginTransaction()
        try {
            for ((english, arabic, category) in fallbackTerms) {
                val values = android.content.ContentValues().apply {
                    put(COLUMN_ENGLISH, english)
                    put(COLUMN_ARABIC, arabic)
                    put(COLUMN_CATEGORY, category)
                }
                
                db.insert(TABLE_DICTIONARY, null, values)
            }
            
            db.setTransactionSuccessful()
            Log.d(TAG, "Successfully loaded ${fallbackTerms.size} fallback medical terms")
            
        } finally {
            db.endTransaction()
        }
    }
}

