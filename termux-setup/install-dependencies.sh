#!/bin/bash

# Medical Translator - Termux Dependencies Installation Script
# This script installs all required packages for the offline translation system

echo "🏥 Medical Translator - Installing Dependencies"
echo "=============================================="

# Update package lists
echo "📦 Updating package lists..."
pkg update -y

# Upgrade existing packages
echo "⬆️ Upgrading existing packages..."
pkg upgrade -y

# Install basic packages
echo "🔧 Installing basic packages..."
pkg install -y python nodejs-lts wget curl git unzip

# Install Java for LanguageTool
echo "☕ Installing Java..."
pkg install -y openjdk-17

# Install build tools for llama.cpp
echo "🔨 Installing build tools..."
pkg install -y clang cmake make

# Install additional utilities
echo "🛠️ Installing additional utilities..."
pkg install -y termux-services

# Verify installations
echo "✅ Verifying installations..."

echo "Python version:"
python --version

echo "Node.js version:"
node --version

echo "Java version:"
java --version

echo "Git version:"
git --version

echo "CMake version:"
cmake --version

echo ""
echo "✅ All dependencies installed successfully!"
echo "📝 Next step: Run './setup-languagetool.sh' to install LanguageTool"
echo ""

