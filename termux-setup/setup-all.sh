#!/bin/bash

# Medical Translator - Complete Setup Script
# This script runs the entire setup process automatically

echo "🏥 Medical Translator - Complete Setup"
echo "======================================"
echo ""
echo "This script will:"
echo "1. Install all dependencies"
echo "2. Setup LanguageTool"
echo "3. Setup llama.cpp"
echo "4. Download AI models"
echo "5. Start all services"
echo ""

read -p "Continue with setup? (y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "Setup cancelled."
    exit 0
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Step 1: Install dependencies
echo ""
echo "🔧 Step 1/5: Installing dependencies..."
bash "$SCRIPT_DIR/install-dependencies.sh"

if [ $? -ne 0 ]; then
    echo "❌ Dependencies installation failed. Exiting."
    exit 1
fi

# Step 2: Setup LanguageTool
echo ""
echo "📝 Step 2/5: Setting up LanguageTool..."
bash "$SCRIPT_DIR/setup-languagetool.sh"

if [ $? -ne 0 ]; then
    echo "❌ LanguageTool setup failed. Exiting."
    exit 1
fi

# Step 3: Setup llama.cpp
echo ""
echo "🤖 Step 3/5: Setting up llama.cpp..."
bash "$SCRIPT_DIR/setup-llama.sh"

if [ $? -ne 0 ]; then
    echo "❌ llama.cpp setup failed. Exiting."
    exit 1
fi

# Step 4: Download models
echo ""
echo "📥 Step 4/5: Downloading AI models..."
echo "Recommended: Mistral 7B Instruct (option 1)"
bash "$SCRIPT_DIR/download-models.sh"

if [ $? -ne 0 ]; then
    echo "❌ Model download failed. Exiting."
    exit 1
fi

# Step 5: Start services
echo ""
echo "🚀 Step 5/5: Starting services..."
bash "$SCRIPT_DIR/start-services.sh"

if [ $? -ne 0 ]; then
    echo "❌ Service startup failed. Exiting."
    exit 1
fi

echo ""
echo "🎉 Setup completed successfully!"
echo ""
echo "📋 What's been installed:"
echo "   ✅ All required dependencies"
echo "   ✅ LanguageTool (Grammar correction)"
echo "   ✅ llama.cpp (AI translation)"
echo "   ✅ AI models for translation"
echo "   ✅ All services are running"
echo ""
echo "📱 Next steps:"
echo "1. Install the Medical Translator APK on your Android device"
echo "2. Keep Termux running in the background"
echo "3. Open the Medical Translator app"
echo "4. Select a PDF and start translating!"
echo ""
echo "🛠️ Useful commands:"
echo "   Check services: ~/check-services.sh"
echo "   Stop services: ~/stop-services.sh"
echo "   Restart services: $SCRIPT_DIR/start-services.sh"
echo ""

