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
import com.medicaltranslator.databinding.ActivityMainBinding
import com.medicaltranslator.services.TranslationService
import com.medicaltranslator.utils.PDFProcessor
import com.medicaltranslator.utils.PermissionHelper
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedPdfUri: Uri? = null
    private var selectedPdfPath: String? = null
    private lateinit var translationService: TranslationService
    private lateinit var pdfProcessor: PDFProcessor

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)

        binding.btnSelectPdf.setOnClickListener {
            selectPdf()
        }

        binding.btnTranslate.setOnClickListener {
            startTranslation()
        }

        binding.btnExport.setOnClickListener {
            exportPdf()
        }

        // Initially disable translate and export buttons
        binding.btnTranslate.isEnabled = false
        binding.btnExport.isEnabled = false

        // Setup progress indicators
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
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
        translationService = TranslationService(this)
        pdfProcessor = PDFProcessor(this)

        // Check if Termux services are available
        lifecycleScope.launch {
            val isTermuxReady = translationService.checkTermuxServices()
            if (!isTermuxReady) {
                showTermuxSetupDialog()
            }
        }
    }

    private fun selectPdf() {
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun handlePdfSelection(uri: Uri) {
        try {
            // Copy PDF to app's private directory for processing
            val inputStream = contentResolver.openInputStream(uri)
            
            // FIX: These two lines were joined together. I have separated them.
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

            // Preview the PDF
            binding.pdfView.fromFile(file)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .load()

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_LONG).show()
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

                    // Extract text from PDF
                    updateProgress("Extracting text from PDF...", 20)
                    val extractedText = pdfProcessor.extractTextFromPdf(pdfPath)

                    // Translate text using the pipeline
                    updateProgress("Translating text...", 40)
                    val translatedText = translationService.translateText(extractedText) { progress, stage ->
                        updateProgress(stage, 40 + (progress * 0.4).toInt())
                    }

                    // Generate new PDF with original and translated pages
                    updateProgress("Generating translated PDF...", 90)
                    val outputPath = generateOutputPdf(pdfPath, translatedText)

                    updateProgress("Translation complete!", 100)

                    // Load the translated PDF in viewer
                    binding.pdfView.fromFile(File(outputPath))
                        .defaultPage(0)
                        .load()

                    binding.btnExport.isEnabled = true

                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Translation error: ${e.message}", Toast.LENGTH_LONG).show()
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

    private suspend fun generateOutputPdf(originalPdfPath: String, translatedText: List<String>): String {
        val outputFile = File(filesDir, "translated_document.pdf")
        return pdfProcessor.createBilingualPdf(originalPdfPath, translatedText, outputFile.absolutePath)
    }

    private fun exportPdf() {
        val translatedFile = File(filesDir, "translated_document.pdf")
        if (translatedFile.exists()) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "translated_medical_document.pdf")
            }
            startActivityForResult(intent, EXPORT_REQUEST_CODE)
        }
    }

    private fun showTermuxSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.termux_setup_required))
            // FIX: These chained calls were joined together. I have put them on separate lines.
            .setMessage(getString(R.string.termux_not_configured))
            .setPositiveButton(getString(R.string.setup_instructions)) { _, _ ->
                // Show setup instructions
                showSetupInstructions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetupInstructions() {
        val intent = Intent(this, SetupInstructionsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_dark_mode -> {
                toggleDarkMode()
                true
            }
            R.id.action_settings -> {
                // Open settings
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
            .setTitle(getString(R.string.about))
            .setMessage("${getString(R.string.offline_medical_translator)}\n${getString(R.string.english_to_arabic)}\n\n${getString(R.string.version)}")
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

    companion object {
        private const val EXPORT_REQUEST_CODE = 1001
    }
}
