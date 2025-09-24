# Medical Translator - Offline Android App

A fully offline Android application that translates medical documents from English to Arabic with professional accuracy. The app processes PDF files while preserving their original layout and formatting, creating bilingual documents with alternating original and translated pages.

## 🏥 Features

### Core Functionality
- **PDF Input/Output**: Accepts PDF files and maintains original layout, formatting, colors, and fonts
- **Bilingual Output**: Creates documents with alternating pages (Original → Translation → Original → Translation...)
- **Professional Medical Translation**: 99% accuracy target with specialized medical terminology
- **Completely Offline**: No internet required after initial setup
- **Android Native**: Built with Java/Kotlin for Android Studio and AIDE compatibility

### Translation Pipeline
1. **Medical Dictionary Lookup**: SQLite database with 50+ medical terms for precise terminology
2. **AI Translation**: Integration with llama.cpp using quantized models (Mistral, LLaMA 2)
3. **Grammar Correction**: LanguageTool integration for Arabic grammar refinement
4. **Layout Preservation**: High-quality PDF rendering maintains visual fidelity

### User Interface
- **Material Design 3**: Modern, clean interface with dark/light mode support
- **PDF Viewer**: Built-in preview for original and translated documents
- **Progress Tracking**: Real-time translation progress with stage indicators
- **File Management**: Easy PDF selection and export functionality

## 📋 Requirements

### Android Device
- Android 7.0 (API level 24) or higher
- Minimum 4GB RAM (8GB recommended for larger models)
- 10GB free storage space
- ARM64 processor (most modern Android devices)

### Termux Setup
- Termux app (from F-Droid recommended)
- Internet connection for initial setup only
- Background app permissions for Termux

## 🚀 Quick Start

### 1. Install Termux Services
```bash
# Download and run the complete setup script
cd /data/data/com.termux/files/home
wget https://your-server.com/medical-translator/termux-setup.zip
unzip termux-setup.zip
cd termux-setup
./setup-all.sh
```

### 2. Install Android App
1. Download `MedicalTranslator.apk`
2. Enable "Install from unknown sources" in Android settings
3. Install the APK file
4. Grant required permissions (storage, network)

### 3. Start Translation
1. Open Termux and run `~/start-services.sh`
2. Keep Termux running in background
3. Open Medical Translator app
4. Select PDF file and translate

## 📖 Detailed Setup Instructions



### Step-by-Step Termux Setup

#### 1. Install Termux
- **Recommended**: Download from [F-Droid](https://f-droid.org/packages/com.termux/)
- **Alternative**: Google Play Store (may have limitations)
- Open Termux and allow storage permissions

#### 2. Update Termux
```bash
pkg update && pkg upgrade
```

#### 3. Run Automated Setup
```bash
# Download setup scripts
curl -L https://github.com/your-repo/medical-translator/archive/main.zip -o setup.zip
unzip setup.zip
cd medical-translator-main/termux-setup

# Run complete setup (recommended)
./setup-all.sh

# Or run individual steps:
./install-dependencies.sh
./setup-languagetool.sh
./setup-llama.sh
./download-models.sh
./start-services.sh
```

#### 4. Manual Setup (Alternative)

**Install Dependencies:**
```bash
pkg install python nodejs-lts wget curl git unzip openjdk-17 clang cmake make
```

**Setup LanguageTool:**
```bash
cd ~
wget https://languagetool.org/download/LanguageTool-6.3.zip
unzip LanguageTool-6.3.zip
cd LanguageTool-6.3
```

**Setup llama.cpp:**
```bash
cd ~
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
make -j$(nproc)
mkdir -p ~/models
```

**Download AI Models:**
```bash
cd ~/models
# Mistral 7B (Recommended)
wget https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF/resolve/main/mistral-7b-instruct-v0.1.q4_0.gguf

# Or Llama 2 7B
wget https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.q4_0.gguf
```

### Android App Installation

#### Building from Source (AIDE)
1. Copy the project folder to your Android device
2. Open AIDE (Android IDE)
3. Open the project folder
4. Build → Make Project
5. Install the generated APK

#### Building with Android Studio
1. Import the project in Android Studio
2. Sync Gradle files
3. Build → Generate Signed Bundle/APK
4. Transfer APK to Android device and install

## 🔧 Configuration

### Model Selection
The app supports multiple AI models with different performance characteristics:

| Model | Size | RAM Required | Speed | Quality |
|-------|------|--------------|-------|---------|
| Mistral 7B Q4_0 | ~4GB | 6GB | Medium | High |
| Llama 2 7B Q4_0 | ~4GB | 6GB | Medium | High |
| Phi-2 Q4_0 | ~1.5GB | 3GB | Fast | Medium |
| CodeLlama 7B Q4_0 | ~4GB | 6GB | Medium | High |

### Service Configuration

**LanguageTool Settings:**
- Port: 8010
- Language: Arabic (ar)
- Grammar rules: Enabled
- Style checking: Enabled

**llama.cpp Settings:**
- Port: 8080
- Context length: 2048 tokens
- Temperature: 0.1 (for consistency)
- Threads: Auto-detected CPU cores

### Medical Dictionary
The app includes a comprehensive medical dictionary with:
- 50+ common medical terms
- Anatomical terminology
- Symptoms and conditions
- Treatment procedures
- Medical equipment
- Expandable via SQLite database

## 📱 Usage Guide


### Using the Medical Translator App

#### 1. Start Services
```bash
# In Termux
~/start-services.sh
```
Keep Termux running in the background.

#### 2. Open the App
- Launch "Medical Translator" from your app drawer
- The app will automatically check for running services
- If services are not detected, setup instructions will be shown

#### 3. Select PDF Document
- Tap "Select PDF" button
- Choose a PDF file from your device storage
- The first page will be displayed in the preview

#### 4. Start Translation
- Tap "Translate" button
- Monitor progress through the progress bar
- Translation stages: Dictionary → AI → Grammar → PDF Generation

#### 5. Review and Export
- Preview the translated document in the PDF viewer
- Tap "Export PDF" to save the bilingual document
- Choose save location and filename

### Translation Process Details

The app follows a sophisticated three-stage translation pipeline:

**Stage 1: Dictionary Enhancement**
- Scans text for medical terminology
- Replaces common medical terms with precise Arabic equivalents
- Provides context annotations for AI processing

**Stage 2: AI Translation**
- Sends enhanced text to local llama.cpp server
- Uses specialized prompts for medical translation
- Maintains formatting and structure

**Stage 3: Grammar Correction**
- Processes translated text through LanguageTool
- Corrects Arabic grammar and syntax
- Ensures proper diacritical marks and word order

**Stage 4: PDF Generation**
- Creates new PDF with alternating pages
- Preserves original page layout as high-resolution images
- Adds translated pages with proper Arabic text rendering

## 🛠️ Troubleshooting

### Common Issues

#### App Shows "Termux Setup Required"
**Cause**: Services are not running or not accessible
**Solution**:
1. Open Termux
2. Run `~/check-services.sh` to verify status
3. If services are down, run `~/start-services.sh`
4. Ensure Termux has background app permissions

#### Translation Fails with Network Error
**Cause**: Local services are not responding
**Solution**:
1. Check if Termux is running: `ps aux | grep -E "(languagetool|llama)"`
2. Restart services: `~/stop-services.sh && ~/start-services.sh`
3. Check port availability: `netstat -tuln | grep -E "(8010|8080)"`

#### PDF Processing Error
**Cause**: Insufficient memory or corrupted PDF
**Solution**:
1. Close other apps to free memory
2. Try with a smaller PDF file
3. Ensure PDF is not password-protected or corrupted

#### Poor Translation Quality
**Cause**: Model not optimized for medical content
**Solution**:
1. Try different AI model (Mistral vs Llama 2)
2. Ensure medical dictionary is populated
3. Check if LanguageTool is running for grammar correction

#### App Crashes on Large PDFs
**Cause**: Memory limitations
**Solution**:
1. Close background apps
2. Process smaller sections of the document
3. Use a device with more RAM
4. Consider using a lighter AI model (Phi-2)

### Service Management

**Check Service Status:**
```bash
~/check-services.sh
```

**Restart All Services:**
```bash
~/stop-services.sh
~/start-services.sh
```

**View Service Logs:**
```bash
# LanguageTool logs
tail -f ~/LanguageTool-6.3/languagetool.log

# llama.cpp logs
tail -f ~/llama.cpp/llama.log
```

**Test Services Manually:**
```bash
# Test LanguageTool
curl -X POST "http://localhost:8010/v2/check" \
  -d "text=Hello world" \
  -d "language=en"

# Test llama.cpp
curl -X POST "http://localhost:8080/completion" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Hello","n_predict":10}'
```

### Performance Optimization

#### For Low-End Devices
1. Use Phi-2 model instead of larger models
2. Reduce context length in llama.cpp settings
3. Process smaller PDF sections
4. Close unnecessary background apps

#### For High-End Devices
1. Use larger models for better quality
2. Increase context length for better coherence
3. Enable more CPU threads for faster processing

### Storage Management

**Check Storage Usage:**
```bash
du -h ~/LanguageTool-6.3
du -h ~/llama.cpp
du -h ~/models
```

**Clean Up Temporary Files:**
```bash
rm -f ~/LanguageTool-6.3/*.log
rm -f ~/llama.cpp/*.log
```

## 🔒 Privacy and Security


### Privacy Features

**Complete Offline Operation**
- No data sent to external servers
- All processing happens locally on your device
- Medical documents never leave your device
- No internet connection required after setup

**Data Security**
- Documents processed in app's private storage
- Temporary files automatically cleaned up
- No logging of sensitive medical content
- Local SQLite database for dictionary storage

**Permissions**
- Storage: Required for PDF file access
- Network: Only for local Termux services (127.0.0.1)
- No camera, microphone, or location permissions required

## 🏗️ Technical Architecture

### App Components

**Core Classes:**
- `MainActivity`: Main UI and orchestration
- `PDFProcessor`: PDF parsing and generation using PDFBox/iText
- `TranslationService`: Translation pipeline coordination
- `MedicalDictionary`: SQLite-based medical terminology lookup

**Translation Pipeline:**
```
PDF Input → Text Extraction → Dictionary Lookup → AI Translation → Grammar Correction → PDF Generation
```

**Dependencies:**
- PDFBox Android: PDF processing
- iText 7: PDF generation with Arabic support
- OkHttp: HTTP client for local services
- Room: SQLite database management
- Material Components: UI framework

### Service Architecture

**LanguageTool Server:**
- Java-based grammar checking service
- Runs on port 8010
- Supports Arabic language rules
- RESTful API for grammar correction

**llama.cpp Server:**
- C++ inference engine for LLM models
- Runs on port 8080
- Supports GGUF quantized models
- Optimized for mobile ARM processors

### File Structure
```
MedicalTranslator/
├── app/
│   ├── src/main/
│   │   ├── java/com/medicaltranslator/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SetupInstructionsActivity.kt
│   │   │   ├── data/
│   │   │   │   └── MedicalDictionary.kt
│   │   │   ├── services/
│   │   │   │   └── TranslationService.kt
│   │   │   └── utils/
│   │   │       ├── PDFProcessor.kt
│   │   │       └── PermissionHelper.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── assets/
│   │       ├── fonts/
│   │       └── medical_dictionary.json
│   └── build.gradle
├── termux-setup/
│   ├── setup-all.sh
│   ├── install-dependencies.sh
│   ├── setup-languagetool.sh
│   ├── setup-llama.sh
│   ├── download-models.sh
│   └── start-services.sh
└── README.md
```

## 🤝 Contributing

### Development Setup

**Prerequisites:**
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK API 34
- Git

**Clone and Build:**
```bash
git clone https://github.com/your-repo/medical-translator.git
cd medical-translator
./gradlew build
```

### Adding Medical Terms

**Via Code:**
1. Edit `app/src/main/assets/medical_dictionary.json`
2. Add new terms in the format:
```json
{
  "english": "term",
  "arabic": "ترجمة",
  "category": "anatomy",
  "definition": "Medical definition"
}
```

**Via Database:**
```kotlin
medicalDictionary.addWord("english_term", "arabic_translation", "category", "definition")
```

### Improving Translation Quality

**Model Fine-tuning:**
- Collect medical translation pairs
- Fine-tune models using medical corpus
- Test with medical professionals

**Dictionary Expansion:**
- Add specialized medical terminology
- Include pharmaceutical terms
- Add medical procedure descriptions

### Testing

**Unit Tests:**
```bash
./gradlew test
```

**Integration Tests:**
```bash
./gradlew connectedAndroidTest
```

**Manual Testing Checklist:**
- [ ] PDF selection and preview
- [ ] Translation pipeline execution
- [ ] Bilingual PDF generation
- [ ] Service connectivity
- [ ] Error handling
- [ ] Memory usage optimization

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **PDFBox**: Apache License 2.0
- **iText**: AGPL/Commercial License
- **LanguageTool**: LGPL 2.1
- **llama.cpp**: MIT License
- **Material Components**: Apache License 2.0

## 🙏 Acknowledgments

- **LanguageTool Team**: Grammar correction engine
- **llama.cpp Contributors**: Efficient LLM inference
- **PDFBox Community**: PDF processing library
- **Google Fonts**: Noto Sans Arabic font
- **Medical Terminology Sources**: Open medical dictionaries

## 📞 Support

### Getting Help

**Documentation:**
- Read this README thoroughly
- Check the troubleshooting section
- Review setup instructions

**Community Support:**
- GitHub Issues: Bug reports and feature requests
- Discussions: General questions and usage help

**Professional Support:**
- For commercial use or custom modifications
- Contact: [your-email@domain.com]

### Reporting Issues

When reporting bugs, please include:
1. Android version and device model
2. App version and build number
3. Termux version and setup details
4. Error messages or logs
5. Steps to reproduce the issue
6. Sample PDF (if not confidential)

### Feature Requests

We welcome suggestions for:
- Additional language pairs
- New medical specialties
- UI/UX improvements
- Performance optimizations
- Integration with other tools

---

**Version**: 1.0.0  
**Last Updated**: December 2024  
**Minimum Android**: 7.0 (API 24)  
**Target Android**: 14 (API 34)  

For the latest updates and releases, visit our [GitHub repository](https://github.com/your-repo/medical-translator).

