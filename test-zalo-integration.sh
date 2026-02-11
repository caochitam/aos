#!/bin/bash
# Test script for Zalo integration

echo "Testing Zalo Integration..."
echo ""

# Test 1: Check if Zalo namespaces compile
echo "Test 1: Checking Zalo namespace compilation..."
lein check 2>&1 | grep -i "error" && {
    echo "❌ Compilation failed!"
    exit 1
} || {
    echo "✓ All namespaces compile successfully"
}

echo ""

# Test 2: Check dependencies
echo "Test 2: Checking dependencies..."
grep -q "ring/ring-jetty-adapter" project.clj && {
    echo "✓ ring-jetty-adapter found"
} || {
    echo "❌ ring-jetty-adapter missing!"
    exit 1
}

grep -q "ring/ring-json" project.clj && {
    echo "✓ ring-json found"
} || {
    echo "❌ ring-json missing!"
    exit 1
}

echo ""

# Test 3: Check config
echo "Test 3: Checking config.edn..."
grep -q ":zalo" resources/config.edn && {
    echo "✓ Zalo configuration found"
} || {
    echo "❌ Zalo configuration missing!"
    exit 1
}

echo ""

# Test 4: Check if Zalo files exist
echo "Test 4: Checking Zalo integration files..."
FILES=(
    "src/agent_os/integrations/zalo/client.clj"
    "src/agent_os/integrations/zalo/handler.clj"
    "src/agent_os/integrations/zalo/server.clj"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "❌ $file missing!"
        exit 1
    fi
done

echo ""

# Test 5: Test health check endpoint (if server is running)
echo "Test 5: Testing webhook server (if running)..."
if curl -s http://localhost:3000/health > /dev/null 2>&1; then
    RESPONSE=$(curl -s http://localhost:3000/health)
    if echo "$RESPONSE" | grep -q "ok"; then
        echo "✓ Health check endpoint working"
        echo "  Response: $RESPONSE"
    else
        echo "⚠️  Health check returned unexpected response"
    fi
else
    echo "⚠️  Server not running (this is OK if you haven't started it yet)"
    echo "  To start: ./start-zalo-bot.sh"
fi

echo ""
echo "================================"
echo "All basic checks passed! ✓"
echo "================================"
echo ""
echo "Next steps:"
echo "1. Set up Zalo OA credentials (see docs/guides/ZALO_INTEGRATION.md)"
echo "2. Configure environment variables (.env.example)"
echo "3. Start bot: ./start-zalo-bot.sh"
echo "4. Test with Zalo mobile app"
echo ""
