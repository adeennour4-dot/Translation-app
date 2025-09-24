# Build Instructions - Medical Translator

This document provides detailed instructions for building the Medical Translator Android app from source code.

## 🛠️ Development Environment Setup

### Prerequisites

**Required Software:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or JDK 17
- Android SDK API 34
- Git 2.0+

**System Requirements:**
- Windows 10/11, macOS 10.14+, or Ubuntu 18.04+
- 8GB RAM minimum (16GB recommended)
- 10GB free disk space
- Internet connection for initial setup

### Android Studio Setup

1. **Download Android Studio:**
   - Visit [developer.android.com](https://developer.android.com/studio)
   - Download the latest stable version
   - Install with default settings

2. **Configure SDK:**
   ```
   Tools → SDK Manager → SDK Platforms
   ✓ Android 14 (API 34) - Target
   ✓ Android 7.0 (API 24) - Minimum
   
   Tools → SDK Manager → SDK Tools
   ✓ Android SDK Build-Tools 34.0.0
   ✓ Android Emulator
   ✓ Android SDK Platform-Tools
   ✓ Android SDK Tools
   ```

3. **Set JDK:**
   ```
   File → Project Structure → SDK Location
   JDK Location: [Path to JDK 11 or 17]
   ```

## 📥 Source Code Setup

### Clone Repository

```bash
# Clone the project
git clone https://github.com/your-repo/medical-translator.git
cd medical-translator

# Verify project structure
ls -la
```

### Import in Android Studio

1. **Open Project:**
   - File → Open
   - Select the `MedicalTranslator` folder
   - Wait for Gradle sync to complete

2. **Verify Configuration:**
   - Check `local.properties` file exists
   - Verify SDK path is correct
   - Ensure Gradle wrapper is downloaded

## 🔧 Building the Project

### Command Line Build

```bash
# Navigate to project directory
cd MedicalTranslator

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing)
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### Android Studio Build

1. **Debug Build:**
   - Build → Make Project (Ctrl+F9)
   - Build → Build Bundle(s)/APK(s) → Build APK(s)
   - APK location: `app/build/outputs/apk/debug/`

2. **Release Build:**
   - Build → Generate Signed Bundle/APK
   - Select APK
   - Create or select keystore
   - Build signed APK

## 📱 Building for AIDE (Android IDE)

### AIDE Compatibility

The project is designed to be compatible with AIDE for on-device development.

**AIDE Setup:**
1. Install AIDE from Google Play Store
2. Copy project folder to device storage
3. Open project in AIDE
4. Build directly on Android device

**AIDE Build Steps:**
```
1. Open AIDE app
2. File → Open Project
3. Navigate to MedicalTranslator folder
4. Wait for project analysis
5. Build → Make Project
6. Install generated APK
```

**AIDE Limitations:**
- Limited debugging capabilities
- Slower build times
- May require manual dependency resolution
- Some advanced features not supported

### Optimizing for AIDE

**Reduce Build Complexity:**
```gradle
// In app/build.gradle, simplify dependencies
dependencies {
    // Use only essential dependencies
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    // Remove complex annotation processors if needed
}
```

**Minimize Resource Usage:**
- Remove unused resources
- Optimize images and assets
- Simplify layouts if necessary

## 🔐 Code Signing

### Debug Signing

Debug builds are automatically signed with the debug keystore.

**Debug Keystore Location:**
- Windows: `%USERPROFILE%\.android\debug.keystore`
- macOS/Linux: `~/.android/debug.keystore`

### Release Signing

**Create Release Keystore:**
```bash
keytool -genkey -v -keystore medical-translator-release.keystore \
  -alias medical-translator -keyalg RSA -keysize 2048 -validity 10000
```

**Configure Signing in build.gradle:**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('path/to/medical-translator-release.keystore')
            storePassword 'your-store-password'
            keyAlias 'medical-translator'
            keyPassword 'your-key-password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 🧪 Testing

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.medicaltranslator.PDFProcessorTest"

# Generate test report
./gradlew test jacocoTestReport
```

### Instrumentation Tests

```bash
# Run on connected device/emulator
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.medicaltranslator.MainActivityTest
```

### Manual Testing

**Test Checklist:**
- [ ] App launches successfully
- [ ] PDF file selection works
- [ ] Translation pipeline executes
- [ ] Progress indicators function
- [ ] PDF viewer displays content
- [ ] Export functionality works
- [ ] Dark/light mode switching
- [ ] Error handling scenarios

## 🚀 Deployment

### APK Distribution

**Debug APK:**
- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Use for testing and development
- Automatically signed with debug key

**Release APK:**
- Location: `app/build/outputs/apk/release/app-release.apk`
- Use for production distribution
- Requires proper code signing

### Installation Methods

**ADB Installation:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Manual Installation:**
1. Enable "Unknown Sources" in Android settings
2. Transfer APK to device
3. Open APK file to install

**F-Droid Distribution:**
- Follow F-Droid submission guidelines
- Ensure reproducible builds
- Include metadata and descriptions

## 🔧 Troubleshooting Build Issues

### Common Problems

**Gradle Sync Failed:**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Re-download dependencies
./gradlew build --refresh-dependencies
```

**OutOfMemoryError:**
```gradle
// In gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m
```

**SDK Not Found:**
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

**Dependency Conflicts:**
```gradle
// Force specific versions
configurations.all {
    resolutionStrategy {
        force 'androidx.appcompat:appcompat:1.6.1'
    }
}
```

### Build Optimization

**Faster Builds:**
```gradle
// In gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
android.enableBuildCache=true
```

**Reduce APK Size:**
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        }
    }
}
```

## 📋 Build Variants

### Debug Variant
- Debuggable
- No obfuscation
- Includes debug symbols
- Faster build times

### Release Variant
- Optimized
- Code obfuscation
- Smaller APK size
- Production-ready

### Custom Variants
```gradle
android {
    flavorDimensions "version"
    productFlavors {
        lite {
            dimension "version"
            applicationIdSuffix ".lite"
            versionNameSuffix "-lite"
        }
        full {
            dimension "version"
            // Full feature set
        }
    }
}
```

## 📊 Build Metrics

### APK Analysis
```bash
# Analyze APK size and content
./gradlew analyzeDebugBundle

# Generate build scan
./gradlew build --scan
```

### Performance Monitoring
- Monitor build times
- Track APK size changes
- Analyze dependency impact
- Review ProGuard reports

---

**Build Environment Tested:**
- Android Studio Hedgehog 2023.1.1
- Gradle 8.0
- Android Gradle Plugin 8.1.0
- JDK 17
- Android SDK 34

For build issues or questions, please check the troubleshooting section or create an issue on GitHub.

