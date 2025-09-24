package com.medicaltranslator.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class NormalDictionary(private val context: Context) {

    private val dictionary = mutableMapOf<String, String>()

    companion object {
        private const val TAG = "NormalDictionary"
        private const val DICTIONARY_FILE = "en-ara.tei"
    }

    init {
        loadDictionary()
    }

    private fun loadDictionary() {
        try {
            val inputStream = context.assets.open(DICTIONARY_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("=", 2)
                    if (parts.size == 2) {
                        dictionary[parts[0].trim().lowercase()] = parts[1].trim()
                    }
                }
            }
            Log.d(TAG, "Successfully loaded ${dictionary.size} normal dictionary terms")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading normal dictionary from assets", e)
        }
    }

    fun lookupWord(englishWord: String): String? {
        return dictionary[englishWord.lowercase()]
    }
}


