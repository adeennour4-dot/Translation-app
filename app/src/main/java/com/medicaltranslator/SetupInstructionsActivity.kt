package com.medicaltranslator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medicaltranslator.databinding.ActivitySetupInstructionsBinding

class SetupInstructionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupInstructionsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupInstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Setup Instructions"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Load setup instructions
        loadSetupInstructions()
    }
    
    private fun loadSetupInstructions() {
        val instructions = """
# Termux Setup Instructions

Follow these steps to set up the required services for offline translation:

## 1. Install Termux
- Download Termux from F-Droid (recommended) or Google Play Store
- Open Termux and update packages: `pkg update && pkg upgrade`

## 2. Install Required Packages
```bash
# Install basic packages
pkg install python nodejs-lts wget curl git

# Install LanguageTool dependencies
pkg install openjdk-17

# Install build tools for llama.cpp
pkg install clang cmake make
```

## 3. Setup LanguageTool
```bash
# Download LanguageTool
cd ~
wget https://languagetool.org/download/LanguageTool-6.3.zip
unzip LanguageTool-6.3.zip
cd LanguageTool-6.3

# Start LanguageTool server
java -cp languagetool-server.jar org.languagetool.server.HTTPServer --port 8010 --allow-origin "*"
```

## 4. Setup llama.cpp
```bash
# Clone and build llama.cpp
cd ~
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
make

# Create models directory
mkdir -p ~/models
```

## 5. Download AI Models
```bash
# Download a quantized model (example: Mistral 7B)
cd ~/models
wget https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF/resolve/main/mistral-7b-instruct-v0.1.q4_0.gguf

# Or download Llama 2 7B Chat
wget https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.q4_0.gguf
```

## 6. Start llama.cpp Server
```bash
cd ~/llama.cpp
./server -m ~/models/mistral-7b-instruct-v0.1.q4_0.gguf -c 2048 --port 8080 --host 0.0.0.0
```

## 7. Create Startup Scripts

Create `~/start-services.sh`:
```bash
#!/bin/bash
# Start LanguageTool
cd ~/LanguageTool-6.3
java -cp languagetool-server.jar org.languagetool.server.HTTPServer --port 8010 --allow-origin "*" &

# Start llama.cpp
cd ~/llama.cpp
./server -m ~/models/mistral-7b-instruct-v0.1.q4_0.gguf -c 2048 --port 8080 --host 0.0.0.0 &

echo "Services started. Keep Termux running in background."
```

Make it executable:
```bash
chmod +x ~/start-services.sh
```

## 8. Usage
1. Run `~/start-services.sh` in Termux
2. Keep Termux running in the background
3. Open Medical Translator app
4. The app will automatically connect to local services

## Troubleshooting
- Ensure Termux has storage permissions
- Keep Termux running in background (disable battery optimization)
- Check that ports 8010 and 8080 are not blocked
- Verify model files are downloaded completely

## Alternative Models
You can use different models by changing the model path in the startup script:
- Llama 2 7B: `llama-2-7b-chat.q4_0.gguf`
- Mistral 7B: `mistral-7b-instruct-v0.1.q4_0.gguf`
- CodeLlama 7B: `codellama-7b-instruct.q4_0.gguf`

Choose models based on your device's RAM capacity.
        """.trimIndent()
        
        binding.tvInstructions.text = instructions
    }
}

