package com.medicaltranslator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * OCR processor for extracting text from scanned PDFs and images
 * Uses Google ML Kit for on-device text recognition
 */
class OCRProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "OCRProcessor"
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extract text from PDF using OCR (for scanned documents)
     */
    suspend fun extractTextFromPdfWithOCR(
        pdfPath: String,
        progressCallback: (Float, String) -> Unit = { _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        
        val extractedTexts = mutableListOf<String>()
        
        try {
            val file = File(pdfPath)
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            
            val pageCount = pdfRenderer.pageCount
            progressCallback(0f, "Starting OCR for $pageCount pages...")
            
            for (i in 0 until pageCount) {
                try {
                    val progress = (i.toFloat() / pageCount) * 100f
                    progressCallback(progress, "Processing page ${i + 1} of $pageCount...")
                    
                    // Render page to bitmap
                    val page = pdfRenderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2, // Higher resolution for better OCR
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    // Extract text from bitmap using OCR
                    val extractedText = extractTextFromBitmap(bitmap)
                    extractedTexts.add(extractedText)
                    
                    // Clean up bitmap
                    bitmap.recycle()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing page $i", e)
                    extractedTexts.add("Error processing page ${i + 1}: ${e.message}")
                }
            }
            
            pdfRenderer.close()
            fileDescriptor.close()
            
            progressCallback(100f, "OCR completed for all pages")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in PDF OCR processing", e)
            throw IOException("Failed to process PDF with OCR: ${e.message}")
        }
        
        return@withContext extractedTexts
    }
    
    /**
     * Extract text from a single bitmap using ML Kit
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            
            val extractedText = StringBuilder()
            
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    extractedText.append(line.text).append("\n")
                }
                extractedText.append("\n") // Add extra line break between blocks
            }
            
            return@withContext extractedText.toString().trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in bitmap OCR", e)
            return@withContext "Error extracting text from image: ${e.message}"
        }
    }
    
    /**
     * Extract text from image file
     */
    suspend fun extractTextFromImage(imagePath: String): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
            val result = textRecognizer.process(image).await()
            
            val extractedText = StringBuilder()
            
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    extractedText.append(line.text).append(" ")
                }
                extractedText.append("\n")
            }
            
            return@withContext extractedText.toString().trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from image", e)
            return@withContext "Error extracting text from image: ${e.message}"
        }
    }
    
    /**
     * Check if PDF contains mostly images (scanned document)
     */
    suspend fun isScannedPdf(pdfPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            
            // Check first few pages
            val pagesToCheck = minOf(3, pdfRenderer.pageCount)
            var totalTextLength = 0
            
            for (i in 0 until pagesToCheck) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Quick OCR check
                val text = extractTextFromBitmap(bitmap)
                totalTextLength += text.length
                
                bitmap.recycle()
            }
            
            pdfRenderer.close()
            fileDescriptor.close()
            
            // If average text per page is very low, it's likely a scanned document
            val averageTextPerPage = totalTextLength / pagesToCheck
            return@withContext averageTextPerPage < 100 // Threshold for scanned documents
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if PDF is scanned", e)
            return@withContext false // Assume it's not scanned if we can't check
        }
    }
    
    /**
     * Get OCR confidence for extracted text
     */
    suspend fun getOCRConfidence(bitmap: Bitmap): Float = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            
            var totalConfidence = 0f
            var elementCount = 0
            
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        // Note: ML Kit doesn't provide confidence scores in the free version
                        // This is a placeholder for potential future enhancement
                        totalConfidence += 1.0f
                        elementCount++
                    }
                }
            }
            
            return@withContext if (elementCount > 0) totalConfidence / elementCount else 0f
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating OCR confidence", e)
            return@withContext 0f
        }
    }
    
    /**
     * Preprocess image for better OCR results
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // This could include operations like:
        // - Contrast enhancement
        // - Noise reduction
        // - Skew correction
        // - Binarization
        
        // For now, return the original bitmap
        // In a production app, you might want to implement image preprocessing
        return bitmap
    }
    
    /**
     * Clean up OCR text (remove common OCR errors)
     */
    fun cleanupOCRText(text: String): String {
        var cleanText = text
        
        // Remove common OCR artifacts
        cleanText = cleanText.replace(Regex("[|\\\\/_]"), "")
        cleanText = cleanText.replace(Regex("\\s+"), " ")
        cleanText = cleanText.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        
        // Fix common character misrecognitions
        cleanText = cleanText.replace("0", "O") // Zero to O in medical contexts
        cleanText = cleanText.replace("1", "I") // One to I in some contexts
        cleanText = cleanText.replace("5", "S") // Five to S in some contexts
        
        return cleanText.trim()
    }
    
    /**
     * Release resources
     */
    fun cleanup() {
        try {
            textRecognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up OCR processor", e)
        }
    }
}

