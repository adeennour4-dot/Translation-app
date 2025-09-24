#!/bin/bash

# Medical Translator - LanguageTool Setup Script
# This script downloads and configures LanguageTool for offline grammar correction

echo "📝 Medical Translator - Setting up LanguageTool"
echo "==============================================="

# Create directory for LanguageTool
cd ~
LANGUAGETOOL_DIR="$HOME/LanguageTool-6.3"

# Check if LanguageTool is already installed
if [ -d "$LANGUAGETOOL_DIR" ]; then
    echo "⚠️ LanguageTool directory already exists. Removing old installation..."
    rm -rf "$LANGUAGETOOL_DIR"
fi

# Download LanguageTool
echo "⬇️ Downloading LanguageTool 6.3..."
wget -O LanguageTool-6.3.zip "https://languagetool.org/download/LanguageTool-6.3.zip"

if [ $? -ne 0 ]; then
    echo "❌ Failed to download LanguageTool. Please check your internet connection."
    exit 1
fi

# Extract LanguageTool
echo "📦 Extracting LanguageTool..."
unzip -q LanguageTool-6.3.zip

if [ $? -ne 0 ]; then
    echo "❌ Failed to extract LanguageTool."
    exit 1
fi

# Clean up zip file
rm LanguageTool-6.3.zip

# Create startup script for LanguageTool
echo "📝 Creating LanguageTool startup script..."
cat > "$LANGUAGETOOL_DIR/start-languagetool.sh" << 'EOF'
#!/bin/bash
cd ~/LanguageTool-6.3
echo "🚀 Starting LanguageTool server on port 8010..."
java -cp languagetool-server.jar org.languagetool.server.HTTPServer \
    --port 8010 \
    --allow-origin "*" \
    --languageModel ~/LanguageTool-6.3/org/languagetool/resource \
    --public
EOF

# Make startup script executable
chmod +x "$LANGUAGETOOL_DIR/start-languagetool.sh"

# Test LanguageTool installation
echo "🧪 Testing LanguageTool installation..."
cd "$LANGUAGETOOL_DIR"

# Start LanguageTool in background for testing
java -cp languagetool-server.jar org.languagetool.server.HTTPServer \
    --port 8010 \
    --allow-origin "*" &

LANGUAGETOOL_PID=$!
sleep 5

# Test if LanguageTool is responding
if curl -s "http://localhost:8010/v2/check" > /dev/null; then
    echo "✅ LanguageTool is working correctly!"
else
    echo "⚠️ LanguageTool test failed, but installation completed."
fi

# Stop test instance
kill $LANGUAGETOOL_PID 2>/dev/null

echo ""
echo "✅ LanguageTool setup completed!"
echo "📍 Installation location: $LANGUAGETOOL_DIR"
echo "🚀 To start LanguageTool: $LANGUAGETOOL_DIR/start-languagetool.sh"
echo "📝 Next step: Run './setup-llama.sh' to install llama.cpp"
echo ""

