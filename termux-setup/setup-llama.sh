#!/bin/bash

# Medical Translator - llama.cpp Setup Script
# This script downloads, compiles, and configures llama.cpp for AI translation

echo "🤖 Medical Translator - Setting up llama.cpp"
echo "============================================="

cd ~

# Check if llama.cpp directory exists
if [ -d "llama.cpp" ]; then
    echo "⚠️ llama.cpp directory already exists. Updating..."
    cd llama.cpp
    git pull
    cd ~
else
    # Clone llama.cpp repository
    echo "📥 Cloning llama.cpp repository..."
    git clone https://github.com/ggerganov/llama.cpp.git
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to clone llama.cpp repository."
        exit 1
    fi
fi

# Build llama.cpp
echo "🔨 Building llama.cpp..."
cd llama.cpp

# Clean previous build
make clean 2>/dev/null

# Build with optimizations for mobile devices
make -j$(nproc) LLAMA_OPENBLAS=0 LLAMA_METAL=0

if [ $? -ne 0 ]; then
    echo "❌ Failed to build llama.cpp."
    exit 1
fi

# Create models directory
echo "📁 Creating models directory..."
mkdir -p ~/models

# Create startup script for llama.cpp
echo "📝 Creating llama.cpp startup script..."
cat > ~/llama.cpp/start-llama.sh << 'EOF'
#!/bin/bash

# Configuration
MODEL_PATH="$HOME/models"
LLAMA_PATH="$HOME/llama.cpp"

# Check available models
echo "🔍 Available models in $MODEL_PATH:"
ls -la "$MODEL_PATH"/*.gguf 2>/dev/null || echo "No .gguf models found"

# Find the first available model
MODEL_FILE=$(ls "$MODEL_PATH"/*.gguf 2>/dev/null | head -n1)

if [ -z "$MODEL_FILE" ]; then
    echo "❌ No model files found in $MODEL_PATH"
    echo "📥 Please download a model first using './download-models.sh'"
    exit 1
fi

echo "🚀 Starting llama.cpp server with model: $(basename "$MODEL_FILE")"
echo "🌐 Server will be available at http://localhost:8080"

cd "$LLAMA_PATH"
./server \
    -m "$MODEL_FILE" \
    -c 2048 \
    --port 8080 \
    --host 0.0.0.0 \
    -t $(nproc) \
    --mlock
EOF

# Make startup script executable
chmod +x ~/llama.cpp/start-llama.sh

echo ""
echo "✅ llama.cpp setup completed!"
echo "📍 Installation location: ~/llama.cpp"
echo "🚀 To start llama.cpp: ~/llama.cpp/start-llama.sh"
echo "📝 Next step: Run './download-models.sh' to download AI models"
echo ""

