#!/bin/bash

# Medical Translator - Services Startup Script
# This script starts both LanguageTool and llama.cpp services

echo "🚀 Medical Translator - Starting Services"
echo "=========================================="

# Function to check if a port is in use
check_port() {
    local port=$1
    if netstat -tuln 2>/dev/null | grep -q ":$port "; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to wait for service to start
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for $service_name to start..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "✅ $service_name is ready!"
            return 0
        fi
        
        echo "   Attempt $attempt/$max_attempts..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name failed to start within timeout"
    return 1
}

# Check if services are already running
echo "🔍 Checking existing services..."

if check_port 8010; then
    echo "⚠️ Port 8010 (LanguageTool) is already in use"
    read -p "Kill existing process? (y/n): " kill_lt
    if [ "$kill_lt" = "y" ]; then
        pkill -f "languagetool-server.jar"
        sleep 2
    fi
fi

if check_port 8080; then
    echo "⚠️ Port 8080 (llama.cpp) is already in use"
    read -p "Kill existing process? (y/n): " kill_llama
    if [ "$kill_llama" = "y" ]; then
        pkill -f "llama.cpp.*server"
        sleep 2
    fi
fi

# Start LanguageTool
echo ""
echo "📝 Starting LanguageTool..."
if [ -f "$HOME/LanguageTool-6.3/start-languagetool.sh" ]; then
    cd "$HOME/LanguageTool-6.3"
    nohup ./start-languagetool.sh > languagetool.log 2>&1 &
    LANGUAGETOOL_PID=$!
    echo "🆔 LanguageTool PID: $LANGUAGETOOL_PID"
    
    # Wait for LanguageTool to start
    wait_for_service "http://localhost:8010/v2/check" "LanguageTool"
else
    echo "❌ LanguageTool startup script not found. Please run './setup-languagetool.sh' first."
    exit 1
fi

# Start llama.cpp
echo ""
echo "🤖 Starting llama.cpp..."
if [ -f "$HOME/llama.cpp/start-llama.sh" ]; then
    cd "$HOME/llama.cpp"
    nohup ./start-llama.sh > llama.log 2>&1 &
    LLAMA_PID=$!
    echo "🆔 llama.cpp PID: $LLAMA_PID"
    
    # Wait for llama.cpp to start
    wait_for_service "http://localhost:8080/health" "llama.cpp"
else
    echo "❌ llama.cpp startup script not found. Please run './setup-llama.sh' first."
    exit 1
fi

# Create status check script
cat > ~/check-services.sh << 'EOF'
#!/bin/bash
echo "🔍 Medical Translator - Service Status"
echo "====================================="

# Check LanguageTool
if curl -s "http://localhost:8010/v2/check" > /dev/null 2>&1; then
    echo "✅ LanguageTool: Running (Port 8010)"
else
    echo "❌ LanguageTool: Not responding"
fi

# Check llama.cpp
if curl -s "http://localhost:8080/health" > /dev/null 2>&1; then
    echo "✅ llama.cpp: Running (Port 8080)"
else
    echo "❌ llama.cpp: Not responding"
fi

echo ""
echo "📊 Process information:"
ps aux | grep -E "(languagetool|llama)" | grep -v grep
EOF

chmod +x ~/check-services.sh

# Create stop services script
cat > ~/stop-services.sh << 'EOF'
#!/bin/bash
echo "🛑 Medical Translator - Stopping Services"
echo "=========================================="

echo "📝 Stopping LanguageTool..."
pkill -f "languagetool-server.jar"

echo "🤖 Stopping llama.cpp..."
pkill -f "llama.cpp.*server"

sleep 2

echo "✅ Services stopped"
EOF

chmod +x ~/stop-services.sh

echo ""
echo "✅ All services started successfully!"
echo ""
echo "📋 Service URLs:"
echo "   LanguageTool: http://localhost:8010"
echo "   llama.cpp: http://localhost:8080"
echo ""
echo "🛠️ Management scripts created:"
echo "   Check status: ~/check-services.sh"
echo "   Stop services: ~/stop-services.sh"
echo ""
echo "📱 You can now use the Medical Translator app!"
echo "⚠️ Keep Termux running in the background for the app to work."
echo ""

