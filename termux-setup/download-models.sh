#!/bin/bash

# Medical Translator - AI Models Download Script
# This script downloads quantized AI models for offline translation

echo "🤖 Medical Translator - Downloading AI Models"
echo "=============================================="

# Create models directory
mkdir -p ~/models
cd ~/models

# Function to download with progress
download_model() {
    local url="$1"
    local filename="$2"
    local description="$3"
    
    echo ""
    echo "📥 Downloading $description..."
    echo "📁 File: $filename"
    echo "🔗 URL: $url"
    
    if [ -f "$filename" ]; then
        echo "⚠️ File already exists. Skipping download."
        return 0
    fi
    
    wget --progress=bar:force:noscroll -O "$filename" "$url"
    
    if [ $? -eq 0 ]; then
        echo "✅ Downloaded $filename successfully!"
        echo "📊 File size: $(du -h "$filename" | cut -f1)"
    else
        echo "❌ Failed to download $filename"
        rm -f "$filename"
        return 1
    fi
}

echo "🎯 Available models for download:"
echo "1. Mistral 7B Instruct (Recommended) - ~4GB"
echo "2. Llama 2 7B Chat - ~4GB"
echo "3. CodeLlama 7B Instruct - ~4GB"
echo "4. Phi-2 (Lightweight) - ~1.5GB"
echo "5. Download all models"
echo ""

read -p "Select model to download (1-5): " choice

case $choice in
    1)
        download_model \
            "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF/resolve/main/mistral-7b-instruct-v0.1.q4_0.gguf" \
            "mistral-7b-instruct-v0.1.q4_0.gguf" \
            "Mistral 7B Instruct (Q4_0)"
        ;;
    2)
        download_model \
            "https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.q4_0.gguf" \
            "llama-2-7b-chat.q4_0.gguf" \
            "Llama 2 7B Chat (Q4_0)"
        ;;
    3)
        download_model \
            "https://huggingface.co/TheBloke/CodeLlama-7B-Instruct-GGUF/resolve/main/codellama-7b-instruct.q4_0.gguf" \
            "codellama-7b-instruct.q4_0.gguf" \
            "CodeLlama 7B Instruct (Q4_0)"
        ;;
    4)
        download_model \
            "https://huggingface.co/microsoft/phi-2/resolve/main/model.gguf" \
            "phi-2.q4_0.gguf" \
            "Phi-2 (Q4_0)"
        ;;
    5)
        echo "📦 Downloading all models..."
        download_model \
            "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF/resolve/main/mistral-7b-instruct-v0.1.q4_0.gguf" \
            "mistral-7b-instruct-v0.1.q4_0.gguf" \
            "Mistral 7B Instruct (Q4_0)"
        
        download_model \
            "https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.q4_0.gguf" \
            "llama-2-7b-chat.q4_0.gguf" \
            "Llama 2 7B Chat (Q4_0)"
        
        download_model \
            "https://huggingface.co/TheBloke/CodeLlama-7B-Instruct-GGUF/resolve/main/codellama-7b-instruct.q4_0.gguf" \
            "codellama-7b-instruct.q4_0.gguf" \
            "CodeLlama 7B Instruct (Q4_0)"
        ;;
    *)
        echo "❌ Invalid selection. Exiting."
        exit 1
        ;;
esac

echo ""
echo "📊 Downloaded models:"
ls -lh ~/models/*.gguf 2>/dev/null || echo "No models found"

echo ""
echo "✅ Model download completed!"
echo "📝 Next step: Run './start-services.sh' to start all services"
echo ""

