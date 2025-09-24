package com.medicaltranslator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.apache.pdfbox.android.PDFBoxResourceLoader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PDFProcessor(private val context: Context) {

    companion object {
        private const val TAG = "PDFProcessor"
        private const val DPI = 150f
    }

    init {
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extract text from PDF while preserving page structure
     */
    suspend fun extractTextFromPdf(pdfPath: String): List<String> {
        val pageTexts = mutableListOf<String>()
        
        try {
            val document = PDDocument.load(File(pdfPath))
            val textStripper = PDFTextStripper()
            
            for (pageNum in 1..document.numberOfPages) {
                textStripper.startPage = pageNum
                textStripper.endPage = pageNum
                
                val pageText = textStripper.getText(document)
                pageTexts.add(pageText.trim())
                
                Log.d(TAG, "Extracted text from page $pageNum: ${pageText.length} characters")
            }
            
            document.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
            throw e
        }
        
        return pageTexts
    }

    /**
     * Create a bilingual PDF with original and translated pages alternating
     */
    suspend fun createBilingualPdf(
        originalPdfPath: String,
        translatedTexts: List<String>,
        outputPath: String
    ): String {
        try {
            val writer = PdfWriter(outputPath)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            
            // Load original PDF for page images
            val originalDoc = PDDocument.load(File(originalPdfPath))
            val renderer = PDFRenderer(originalDoc)
            
            // Set up fonts for Arabic text
            val arabicFont = createArabicFont()
            val englishFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            
            for (pageIndex in 0 until originalDoc.numberOfPages) {
                // Add original page as image
                addOriginalPageAsImage(document, renderer, pageIndex, pdfDoc)
                
                // Add translated page
                if (pageIndex < translatedTexts.size) {
                    addTranslatedPage(
                        document, 
                        translatedTexts[pageIndex], 
                        arabicFont, 
                        pageIndex + 1
                    )
                }
            }
            
            originalDoc.close()
            document.close()
            
            Log.d(TAG, "Bilingual PDF created successfully: $outputPath")
            return outputPath
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bilingual PDF", e)
            throw e
        }
    }

    /**
     * Add original page as high-quality image to preserve layout
     */
    private fun addOriginalPageAsImage(
        document: Document,
        renderer: PDFRenderer,
        pageIndex: Int,
        pdfDoc: PdfDocument
    ) {
        try {
            // Render page as bitmap
            val bitmap = renderer.renderImageWithDPI(pageIndex, DPI)
            
            // Convert bitmap to byte array
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            
            // Create new page and add image
            pdfDoc.addNewPage(PageSize.A4)
            val image = Image(imageData)
            
            // Scale image to fit page while maintaining aspect ratio
            val pageWidth = PageSize.A4.width - 40 // 20pt margins on each side
            val pageHeight = PageSize.A4.height - 40
            
            image.scaleToFit(pageWidth, pageHeight)
            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
            
            document.add(image)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding original page as image", e)
            // Fallback: add a placeholder page
            pdfDoc.addNewPage(PageSize.A4)
            document.add(Paragraph("Original Page ${pageIndex + 1}").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)))
        }
    }

    /**
     * Add translated page with proper Arabic text formatting
     */
    private fun addTranslatedPage(
        document: Document,
        translatedText: String,
        arabicFont: PdfFont,
        pageNumber: Int
    ) {
        try {
            // Add new page
            document.add(com.itextpdf.layout.element.AreaBreak())
            
            // Add header
            val header = Paragraph("Translated Page $pageNumber")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            
            document.add(header)
            
            // Add translated text with proper Arabic formatting
            val textParagraph = Paragraph()
                .setFont(arabicFont)
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.RIGHT) // Arabic is RTL
                .setMarginLeft(20f)
                .setMarginRight(20f)
            
            // Split text into lines and add with proper spacing
            val lines = translatedText.split("\n")
            for (line in lines) {
                if (line.trim().isNotEmpty()) {
                    textParagraph.add(Text(line.trim() + "\n"))
                }
            }
            
            document.add(textParagraph)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding translated page", e)
            // Fallback: add simple text
            document.add(Paragraph("Translation Error: ${e.message}"))
        }
    }

    /**
     * Create or load Arabic font for proper text rendering
     */
    private fun createArabicFont(): PdfFont {
        return try {
            // Try to load Arabic font from assets
            val fontStream = context.assets.open("fonts/NotoSansArabic-Regular.ttf")
            val fontBytes = fontStream.readBytes()
            fontStream.close()
            
            PdfFontFactory.createFont(fontBytes, "Identity-H")
        } catch (e: Exception) {
            Log.w(TAG, "Arabic font not found, using default font")
            // Fallback to standard font
            PdfFontFactory.createFont(StandardFonts.HELVETICA)
        }
    }

    /**
     * Get PDF page count
     */
    fun getPdfPageCount(pdfPath: String): Int {
        return try {
            val document = PDDocument.load(File(pdfPath))
            val pageCount = document.numberOfPages
            document.close()
            pageCount
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF page count", e)
            0
        }
    }

    /**
     * Validate PDF file
     */
    fun validatePdf(pdfPath: String): Boolean {
        return try {
            val document = PDDocument.load(File(pdfPath))
            val isValid = document.numberOfPages > 0
            document.close()
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "PDF validation failed", e)
            false
        }
    }

    /**
     * Extract images from PDF pages
     */
    suspend fun extractPageImages(pdfPath: String): List<Bitmap> {
        val images = mutableListOf<Bitmap>()
        
        try {
            val document = PDDocument.load(File(pdfPath))
            val renderer = PDFRenderer(document)
            
            for (pageIndex in 0 until document.numberOfPages) {
                val bitmap = renderer.renderImageWithDPI(pageIndex, DPI)
                images.add(bitmap)
            }
            
            document.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting page images", e)
            throw e
        }
        
        return images
    }

    /**
     * Create a preview bitmap of the first page
     */
    fun createPreviewBitmap(pdfPath: String): Bitmap? {
        return try {
            val document = PDDocument.load(File(pdfPath))
            val renderer = PDFRenderer(document)
            val bitmap = renderer.renderImageWithDPI(0, 100f) // Lower DPI for preview
            document.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating preview bitmap", e)
            null
        }
    }
}

