#!/bin/bash
# Quick start script for AOS Zalo Bot

set -e

echo "================================"
echo "AOS Zalo Bot Launcher"
echo "================================"
echo ""

# Check if environment variables are set
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "❌ ANTHROPIC_API_KEY is not set!"
    echo ""
    echo "Please set your Anthropic API key:"
    echo "  export ANTHROPIC_API_KEY='sk-ant-api03-...'"
    echo ""
    echo "Or add it to ~/.bashrc for persistence."
    exit 1
fi

if [ -z "$ZALO_ACCESS_TOKEN" ]; then
    echo "⚠️  ZALO_ACCESS_TOKEN is not set!"
    echo ""
    echo "Zalo bot will not work without credentials."
    echo "Please see docs/guides/ZALO_INTEGRATION.md for setup instructions."
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check if config.edn has Zalo enabled
if grep -q ':enabled false' resources/config.edn 2>/dev/null; then
    echo "⚠️  Zalo is disabled in resources/config.edn"
    echo ""
    echo "To enable Zalo bot, edit resources/config.edn:"
    echo "  :zalo {:enabled true  ; <- Change false to true"
    echo ""
    read -p "Do you want me to enable it now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Enable Zalo in config
        sed -i 's/:enabled false/:enabled true/' resources/config.edn
        echo "✓ Zalo enabled in config.edn"
    else
        echo "Please manually enable Zalo in resources/config.edn"
        exit 1
    fi
fi

echo "✓ Environment variables configured"
echo "✓ Zalo bot enabled"
echo ""

# Check if JAR needs rebuild
if [ -f "target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar" ]; then
    # Check if source is newer than JAR
    NEWEST_SOURCE=$(find src -name "*.clj" -type f -printf '%T@\n' | sort -n | tail -1)
    JAR_TIME=$(stat -c %Y "target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar")

    if (( $(echo "$NEWEST_SOURCE > $JAR_TIME" | bc -l) )); then
        echo "⚠️  Source code is newer than JAR - rebuild recommended"
        echo ""
        read -p "Rebuild JAR now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Building JAR..."
            lein uberjar
            echo "✓ JAR rebuilt"
        else
            echo "⚠️  Using old JAR - new Zalo code may not be included!"
        fi
    fi
fi

echo ""
echo "Starting AOS with Zalo bot integration..."
echo ""
echo "Webhook will be available at: http://localhost:3000/webhook"
echo "Health check: http://localhost:3000/health"
echo ""
echo "For ngrok tunnel (if testing locally):"
echo "  ngrok http 3000"
echo ""
echo "Press Ctrl+C to stop"
echo ""
echo "================================"
echo ""

# Start AOS
if [ -f "target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar" ]; then
    java -jar target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar
else
    lein run
fi
