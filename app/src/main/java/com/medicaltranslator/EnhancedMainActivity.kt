package com.medicaltranslator

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.medicaltranslator.databinding.ActivityEnhancedMainBinding
import com.medicaltranslator.services.OfflineTranslationService
import com.medicaltranslator.utils.PDFProcessor
import com.medicaltranslator.utils.PermissionHelper
import com.medicaltranslator.utils.OCRProcessor
import com.medicaltranslator.utils.TextToSpeechManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Enhanced MainActivity with offline capabilities and advanced features
 * No longer depends on external Termux services
 */
class EnhancedMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnhancedMainBinding
    private var selectedPdfUri: Uri? = null
    private var selectedPdfPath: String? = null
    private lateinit var translationService: OfflineTranslationService
    private lateinit var pdfProcessor: PDFProcessor
    private lateinit var ocrProcessor: OCRProcessor
    private lateinit var ttsManager: TextToSpeechManager
    
    private var translatedTexts: List<String> = emptyList()
    private var originalTexts: List<String> = emptyList()
    private var currentViewMode = ViewMode.ORIGINAL

    enum class ViewMode {
        ORIGINAL, TRANSLATED, SIDE_BY_SIDE
    }

    // File picker launcher
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            handlePdfSelection(it)
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeServices()
        } else {
            Toast.makeText(this, "Permissions required for app to function", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnhancedMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)

        // File selection
        binding.btnSelectPdf.setOnClickListener {
            selectPdf()
        }

        // Translation
        binding.btnTranslate.setOnClickListener {
            startTranslation()
        }

        // Export
        binding.btnExport.setOnClickListener {
            exportPdf()
        }

        // View mode toggle
        binding.btnToggleView.setOnClickListener {
            toggleViewMode()
        }

        // Text-to-Speech controls
        binding.btnSpeakOriginal.setOnClickListener {
            speakCurrentText(false)
        }

        binding.btnSpeakTranslated.setOnClickListener {
            speakCurrentText(true)
        }

        binding.btnStopSpeech.setOnClickListener {
            ttsManager.stop()
        }

        // OCR toggle
        binding.switchOcrMode.setOnCheckedChangeListener { _, isChecked ->
            updateOCRModeUI(isChecked)
        }

        // Initially disable certain buttons
        binding.btnTranslate.isEnabled = false
        binding.btnExport.isEnabled = false
        binding.btnToggleView.isEnabled = false
        binding.btnSpeakOriginal.isEnabled = false
        binding.btnSpeakTranslated.isEnabled = false

        // Setup progress indicators
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
        
        // Setup stats display
        updateStatsDisplay()
    }

    private fun checkPermissions() {
        val permissions = PermissionHelper.getRequiredPermissions()
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeServices()
        }
    }

    private fun initializeServices() {
        lifecycleScope.launch {
            try {
                // Initialize offline services
                translationService = OfflineTranslationService(this@EnhancedMainActivity)
                pdfProcessor = PDFProcessor(this@EnhancedMainActivity)
                ocrProcessor = OCRProcessor(this@EnhancedMainActivity)
                ttsManager = TextToSpeechManager(this@EnhancedMainActivity)

                // Initialize TTS
                val ttsInitialized = ttsManager.initialize()
                if (!ttsInitialized) {
                    Toast.makeText(this@EnhancedMainActivity, "Text-to-Speech not available", Toast.LENGTH_SHORT).show()
                }

                // Check service status
                val serviceStatus = translationService.getServiceStatus()
                updateServiceStatusUI(serviceStatus)

                Toast.makeText(this@EnhancedMainActivity, "Enhanced Medical Translator Ready", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@EnhancedMainActivity, "Error initializing services: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun selectPdf() {
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun handlePdfSelection(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Copy PDF to app's private directory for processing
                val inputStream = contentResolver.openInputStream(uri)
                val fileName = "selected_document.pdf"
                val file = File(filesDir, fileName)

                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                selectedPdfPath = file.absolutePath
                binding.tvSelectedFile.text = "Selected: $fileName"
                binding.btnTranslate.isEnabled = true

                // Check if PDF needs OCR
                val isScanned = ocrProcessor.isScannedPdf(file.absolutePath)
                if (isScanned) {
                    binding.switchOcrMode.isChecked = true
                    binding.tvOcrStatus.text = "Scanned document detected - OCR enabled"
                } else {
                    binding.tvOcrStatus.text = "Text-based document"
                }

                // Preview the PDF
                binding.pdfView.fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load()

                binding.btnSpeakOriginal.isEnabled = true

            } catch (e: Exception) {
                Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTranslation() {
        selectedPdfPath?.let { pdfPath ->
            binding.btnTranslate.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            binding.tvProgress.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    updateProgress("Starting translation...", 10)

                    // Extract text from PDF (with or without OCR)
                    val useOCR = binding.switchOcrMode.isChecked
                    originalTexts = if (useOCR) {
                        updateProgress("Extracting text with OCR...", 20)
                        ocrProcessor.extractTextFromPdfWithOCR(pdfPath) { progress, stage ->
                            updateProgress(stage, 20 + (progress * 0.2).toInt())
                        }
                    } else {
                        updateProgress("Extracting text from PDF...", 20)
                        listOf(pdfProcessor.extractTextFromPdf(pdfPath))
                    }

                    // Translate text using offline service
                    updateProgress("Translating text...", 40)
                    translatedTexts = translationService.translateText(originalTexts) { progress, stage ->
                        updateProgress(stage, 40 + (progress * 0.4).toInt())
                    }

                    // Generate new PDF with original and translated pages
                    updateProgress("Generating translated PDF...", 90)
                    val outputPath = generateOutputPdf(pdfPath, translatedTexts)

                    updateProgress("Translation complete!", 100)

                    // Load the translated PDF in viewer
                    binding.pdfView.fromFile(File(outputPath))
                        .defaultPage(0)
                        .load()

                    // Enable additional features
                    binding.btnExport.isEnabled = true
                    binding.btnToggleView.isEnabled = true
                    binding.btnSpeakTranslated.isEnabled = true

                    // Update stats
                    updateStatsDisplay()

                    Toast.makeText(this@EnhancedMainActivity, "Translation completed successfully!", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(this@EnhancedMainActivity, "Translation error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnTranslate.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                }
            }
        }
    }

    private fun updateProgress(message: String, progress: Int) {
        runOnUiThread {
            binding.tvProgress.text = message
            binding.progressBar.progress = progress
        }
    }

    private suspend fun generateOutputPdf(originalPdfPath: String, translatedTexts: List<String>): String {
        val outputFile = File(filesDir, "translated_document.pdf")
        return pdfProcessor.createBilingualPdf(originalPdfPath, translatedTexts, outputFile.absolutePath)
    }

    private fun toggleViewMode() {
        currentViewMode = when (currentViewMode) {
            ViewMode.ORIGINAL -> ViewMode.TRANSLATED
            ViewMode.TRANSLATED -> ViewMode.SIDE_BY_SIDE
            ViewMode.SIDE_BY_SIDE -> ViewMode.ORIGINAL
        }

        updateViewModeUI()
    }

    private fun updateViewModeUI() {
        val modeText = when (currentViewMode) {
            ViewMode.ORIGINAL -> "Original"
            ViewMode.TRANSLATED -> "Translated"
            ViewMode.SIDE_BY_SIDE -> "Side by Side"
        }

        binding.btnToggleView.text = "View: $modeText"
        binding.tvViewMode.text = "Current view: $modeText"

        // Here you would implement the actual view switching logic
        // For now, we'll just update the UI indicators
    }

    private fun speakCurrentText(isTranslated: Boolean) {
        lifecycleScope.launch {
            try {
                val texts = if (isTranslated) translatedTexts else originalTexts
                val language = if (isTranslated) "ar" else "en"

                if (texts.isNotEmpty()) {
                    val combinedText = texts.joinToString("\n\n")
                    val success = ttsManager.speakMedicalText(combinedText, language)
                    
                    if (!success) {
                        Toast.makeText(this@EnhancedMainActivity, "Text-to-Speech failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EnhancedMainActivity, "No text available to speak", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EnhancedMainActivity, "Speech error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOCRModeUI(isEnabled: Boolean) {
        binding.tvOcrStatus.text = if (isEnabled) {
            "OCR Mode: Enabled (for scanned documents)"
        } else {
            "OCR Mode: Disabled (for text-based documents)"
        }
    }

    private fun updateServiceStatusUI(status: Map<String, Boolean>) {
        val statusText = StringBuilder("Service Status:\n")
        status.forEach { (service, isReady) ->
            val indicator = if (isReady) "✓" else "✗"
            statusText.append("$indicator $service\n")
        }
        binding.tvServiceStatus.text = statusText.toString()
    }

    private fun updateStatsDisplay() {
        lifecycleScope.launch {
            try {
                val stats = translationService.getTranslationStats()
                val statsText = "Medical Terms: ${stats["medical_terms"]}\n" +
                              "Normal Terms: ${stats["normal_terms"]}\n" +
                              "Pages Translated: ${translatedTexts.size}"
                binding.tvStats.text = statsText
            } catch (e: Exception) {
                binding.tvStats.text = "Stats unavailable"
            }
        }
    }

    private fun exportPdf() {
        val translatedFile = File(filesDir, "translated_document.pdf")
        if (translatedFile.exists()) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "enhanced_medical_translation.pdf")
            }
            startActivityForResult(intent, EXPORT_REQUEST_CODE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.enhanced_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_dark_mode -> {
                toggleDarkMode()
                true
            }
            R.id.action_settings -> {
                // Open enhanced settings
                true
            }
            R.id.action_history -> {
                // Show translation history
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enhanced Medical Translator")
            .setMessage("Version 2.0\n\nFeatures:\n• Offline translation\n• OCR for scanned documents\n• Text-to-Speech\n• Multiple view modes\n• No external dependencies")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EXPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                exportToUri(uri)
            }
        }
    }

    private fun exportToUri(uri: Uri) {
        try {
            val translatedFile = File(filesDir, "translated_document.pdf")
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                translatedFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this, "PDF exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        ttsManager.shutdown()
        ocrProcessor.cleanup()
    }

    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
    }
}

