#!/bin/bash
# Test interactive setup without actual user input

echo "================================================"
echo "   TEST: Interactive API Key Setup"
echo "================================================"
echo ""

# Save current API key if exists
OLD_KEY="$ANTHROPIC_API_KEY"

# Temporarily unset API key to test interactive setup
unset ANTHROPIC_API_KEY

echo "1. Testing API key detection (should be empty):"
echo "   ANTHROPIC_API_KEY = '$ANTHROPIC_API_KEY'"
echo ""

if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "   ✅ API key is not set (good for testing)"
else
    echo "   ⚠ API key is still set, test may not work as expected"
fi

echo ""
echo "2. When you run 'lein run', AOS will:"
echo "   a) Detect missing API key"
echo "   b) Show welcome message"
echo "   c) Ask if you want to set up now (Y/n)"
echo "   d) Prompt for your API key"
echo "   e) Ask which method to use (1-4)"
echo "   f) Automatically configure it"
echo "   g) Continue starting AOS"
echo ""

echo "3. Setup methods available:"
echo "   [1] Add to ~/.bashrc (Simple, permanent)"
echo "   [2] Secure file ~/.anthropic_key (Recommended, most secure)"
echo "   [3] Current session only (Temporary, for testing)"
echo "   [4] Skip (Set manually later)"
echo ""

echo "4. Example flow:"
echo "   $ lein run"
echo ""
echo "   =============================================="
echo "      AOS First-Time Setup"
echo "   =============================================="
echo ""
echo "   Welcome to AOS! 🚀"
echo ""
echo "   I noticed you don't have an ANTHROPIC_API_KEY set."
echo "   Let's set that up now so AOS can use Claude API."
echo ""
echo "   Would you like to set up your API key now? (Y/n): y"
echo ""
echo "   Please enter your Anthropic API key:"
echo "   API Key: sk-ant-api03-xxxxx..."
echo ""
echo "   ✓ API key accepted"
echo ""
echo "   How would you like to save the API key?"
echo "   1. Add to ~/.bashrc (Recommended)"
echo "   2. Use secure file ~/.anthropic_key"
echo "   3. Current session only"
echo "   4. Skip"
echo ""
echo "   Enter your choice (1-4) [default: 2]: 2"
echo ""
echo "   📝 Creating secure key file..."
echo "   ✓ Created ~/.anthropic_key with permissions 600"
echo "   ✓ Added loader to ~/.bashrc"
echo ""
echo "   ✅ Setup complete!"
echo ""
echo "   ⚠ Run: source ~/.bashrc"
echo ""
echo "   ✓ ANTHROPIC_API_KEY is configured"
echo "   Starting AOS..."
echo ""

echo "5. Try it now:"
echo "   # Unset API key temporarily"
echo "   unset ANTHROPIC_API_KEY"
echo ""
echo "   # Run AOS - it will guide you through setup"
echo "   lein run"
echo ""

# Restore old key if it existed
if [ -n "$OLD_KEY" ]; then
    export ANTHROPIC_API_KEY="$OLD_KEY"
    echo "✓ Restored your original API key for this session"
fi

echo ""
echo "================================================"
echo "   Features"
echo "================================================"
echo "✅ Auto-detect missing API key"
echo "✅ Interactive prompts with clear instructions"
echo "✅ Multiple setup methods (bashrc, secure file, temp)"
echo "✅ API key validation (checks format)"
echo "✅ Auto backup of config files"
echo "✅ Secure file permissions (chmod 600)"
echo "✅ Works with bash and zsh"
echo "✅ Clear instructions for activation"
echo ""

echo "================================================"
echo "   Security"
echo "================================================"
echo "✅ Password input hidden (if console available)"
echo "✅ Validates API key format before saving"
echo "✅ Creates backup before modifying files"
echo "✅ Secure file permissions (600)"
echo "✅ Never logs actual API key"
echo ""
