#!/bin/bash
# AOS API Key Setup Script
# This script helps you set ANTHROPIC_API_KEY permanently

set -e

echo "================================================"
echo "   AOS - API Key Permanent Setup"
echo "================================================"
echo ""

# Check if API key is already set
if [ -n "$ANTHROPIC_API_KEY" ]; then
    echo "✓ ANTHROPIC_API_KEY is already set in current session"
    echo "  Current value: ${ANTHROPIC_API_KEY:0:20}..."
    echo ""
fi

# Function to check if API key is in file
check_key_in_file() {
    local file=$1
    if [ -f "$file" ]; then
        if grep -q "ANTHROPIC_API_KEY" "$file"; then
            echo "✓ Found in $file"
            return 0
        fi
    fi
    return 1
}

echo "Checking existing configuration..."
echo ""

# Check various config files
FOUND=false
for file in ~/.bashrc ~/.bash_profile ~/.profile ~/.zshrc; do
    if check_key_in_file "$file"; then
        FOUND=true
    fi
done

if [ "$FOUND" = false ]; then
    echo "⚠ ANTHROPIC_API_KEY not found in any config file"
    echo ""
fi

echo "================================================"
echo "   Setup Options"
echo "================================================"
echo ""
echo "Choose a method to set your API key permanently:"
echo ""
echo "1. Add to ~/.bashrc (Recommended)"
echo "   - Loads automatically for all bash sessions"
echo "   - Best for interactive use"
echo ""
echo "2. Use secure file with chmod 600"
echo "   - API key stored in separate file"
echo "   - More secure, can be backed up separately"
echo ""
echo "3. Add to ~/.profile (System-wide)"
echo "   - Loads for all shells (bash, sh, dash)"
echo "   - Good for system services"
echo ""
echo "4. Use systemd environment.d (Advanced)"
echo "   - Available to all user processes"
echo "   - Best for system-wide deployment"
echo ""

read -p "Enter your choice (1-4) or 'q' to quit: " choice

case $choice in
    1)
        echo ""
        echo "Method 1: Adding to ~/.bashrc"
        echo "================================"
        read -sp "Enter your Anthropic API key: " api_key
        echo ""

        if [ -z "$api_key" ]; then
            echo "❌ No API key provided. Exiting."
            exit 1
        fi

        # Backup existing bashrc
        cp ~/.bashrc ~/.bashrc.backup.$(date +%Y%m%d_%H%M%S)
        echo "✓ Backed up ~/.bashrc"

        # Add to bashrc
        echo "" >> ~/.bashrc
        echo "# AOS - Anthropic API Key (added $(date))" >> ~/.bashrc
        echo "export ANTHROPIC_API_KEY=\"$api_key\"" >> ~/.bashrc

        echo "✓ Added to ~/.bashrc"
        echo ""
        echo "To activate in current session, run:"
        echo "  source ~/.bashrc"
        echo ""
        echo "Or start a new terminal session."
        ;;

    2)
        echo ""
        echo "Method 2: Secure File Storage"
        echo "================================"
        read -sp "Enter your Anthropic API key: " api_key
        echo ""

        if [ -z "$api_key" ]; then
            echo "❌ No API key provided. Exiting."
            exit 1
        fi

        # Create secure key file
        KEY_FILE=~/.anthropic_key
        echo "$api_key" > "$KEY_FILE"
        chmod 600 "$KEY_FILE"
        echo "✓ Created secure key file: $KEY_FILE"
        echo "✓ Set permissions to 600 (owner read/write only)"

        # Backup bashrc
        cp ~/.bashrc ~/.bashrc.backup.$(date +%Y%m%d_%H%M%S)

        # Add to bashrc to load from file
        echo "" >> ~/.bashrc
        echo "# AOS - Load Anthropic API Key from secure file (added $(date))" >> ~/.bashrc
        echo "if [ -f ~/.anthropic_key ]; then" >> ~/.bashrc
        echo "    export ANTHROPIC_API_KEY=\"\$(cat ~/.anthropic_key)\"" >> ~/.bashrc
        echo "fi" >> ~/.bashrc

        echo "✓ Added loader to ~/.bashrc"
        echo ""
        echo "To activate in current session, run:"
        echo "  source ~/.bashrc"
        ;;

    3)
        echo ""
        echo "Method 3: System-wide ~/.profile"
        echo "================================"
        read -sp "Enter your Anthropic API key: " api_key
        echo ""

        if [ -z "$api_key" ]; then
            echo "❌ No API key provided. Exiting."
            exit 1
        fi

        # Backup profile
        if [ -f ~/.profile ]; then
            cp ~/.profile ~/.profile.backup.$(date +%Y%m%d_%H%M%S)
        fi

        # Add to profile
        echo "" >> ~/.profile
        echo "# AOS - Anthropic API Key (added $(date))" >> ~/.profile
        echo "export ANTHROPIC_API_KEY=\"$api_key\"" >> ~/.profile

        echo "✓ Added to ~/.profile"
        echo ""
        echo "To activate, logout and login again, or run:"
        echo "  source ~/.profile"
        ;;

    4)
        echo ""
        echo "Method 4: systemd environment.d"
        echo "================================"
        read -sp "Enter your Anthropic API key: " api_key
        echo ""

        if [ -z "$api_key" ]; then
            echo "❌ No API key provided. Exiting."
            exit 1
        fi

        # Create environment.d directory
        ENV_DIR=~/.config/environment.d
        mkdir -p "$ENV_DIR"

        # Create env file
        ENV_FILE="$ENV_DIR/anthropic.conf"
        echo "ANTHROPIC_API_KEY=$api_key" > "$ENV_FILE"
        chmod 600 "$ENV_FILE"

        echo "✓ Created $ENV_FILE"
        echo "✓ Set permissions to 600"
        echo ""
        echo "⚠ Note: This requires systemd user session support"
        echo "You may need to logout/login for changes to take effect"
        ;;

    q|Q)
        echo "Exiting without changes."
        exit 0
        ;;

    *)
        echo "❌ Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
echo "================================================"
echo "   Setup Complete!"
echo "================================================"
echo ""
echo "✓ API key configured for permanent use"
echo ""
echo "Verification:"
echo "  1. Open a new terminal"
echo "  2. Run: echo \$ANTHROPIC_API_KEY"
echo "  3. Should show: sk-ant-api03-..."
echo ""
echo "Security Tips:"
echo "  - Never commit .bashrc/.profile to git if they contain keys"
echo "  - Rotate your API key regularly (every 30-90 days)"
echo "  - Check Anthropic dashboard for unusual activity"
echo ""
echo "Start AOS with:"
echo "  cd /root/aos && lein run"
echo ""
