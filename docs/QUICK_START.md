# AOS Quick Start Guide

Get AOS running in 5 minutes!

---

## 🚀 Quick Start (Recommended)

### Step 1: Clone & Setup

```bash
cd /root/aos
lein deps  # Install dependencies
```

### Step 2: Run AOS (Interactive Setup)

```bash
./aos
```

**NEW!** AOS automatically detects missing API key and guides you through setup:

```
⚠️  No ANTHROPIC_API_KEY found

AOS can help you set up your API key now.

Would you like to set up your API key now? (Y/n): y

Please enter your Anthropic API key:
API Key: sk-ant-api03-xxxxx...
         ^^^^^ YOU SEE THIS AS YOU TYPE ^^^^^

✓ API key accepted
  Prefix: sk-ant-api03-...

Where should AOS save this API key?

[1] ~/.bashrc (auto-load every terminal) ← RECOMMENDED
[2] Secure file (~/.anthropic_key, chmod 600)
[3] This session only (temporary)

Choice [1-3]: 1

✓ API key added to ~/.bashrc
✓ Configuration updated

Reload your shell or run: source ~/.bashrc

🚀 Starting AOS...
```

See [Interactive Setup Guide](guides/INTERACTIVE_SETUP.md) for detailed walkthrough.

### Step 3: Start Using AOS

```bash
# After interactive setup, just run:
./aos

# AOS CLI starts:
Type /help for commands, or just chat directly

aos>
```

---

## 🎯 Development Modes

### Production Mode (Fast Startup)

```bash
./aos
# → Runs from compiled JAR (~0.5s startup)
# → Auto-detects if rebuild needed
```

### Dev Mode (Always Latest Code)

```bash
./aos --dev
# → Runs from source via lein (~3-5s startup)
# → Code changes immediately reflected
```

### Rebuild After Code Changes

```bash
./aos --rebuild
# → Rebuilds JAR with latest code
# → Then starts AOS
```

**See:** [Dev Workflow Guide](DEV_WORKFLOW.md) for details on auto-rebuild detection.

---

## 💬 Basic Usage

### Chat Directly

```bash
aos> xin chào
# AOS responds in Vietnamese by default

aos> xem file README
# AOS reads and displays file content

aos> giải thích code trong core.clj
# AOS analyzes and explains code
```

### Slash Commands

```bash
aos> /help              # Show all commands
aos> /status            # System status
aos> /components        # List components
aos> /memory            # View memory
aos> /soul              # View agent personality
aos> /exit              # Exit
```

**See:** [Command Guide](guides/AOS_COMMAND_GUIDE.md) for complete command reference.

---

## 🔧 Manual API Key Setup (If Interactive Setup Skipped)

### Method 1: Environment Variable (Recommended)

```bash
# Add to ~/.bashrc
echo 'export ANTHROPIC_API_KEY="sk-ant-api03-YOUR-KEY-HERE"' >> ~/.bashrc
source ~/.bashrc

# Verify
echo $ANTHROPIC_API_KEY
```

### Method 2: Secure File

```bash
# Create secure key file
echo "sk-ant-api03-YOUR-KEY-HERE" > ~/.anthropic_key
chmod 600 ~/.anthropic_key

# Add loader to ~/.bashrc
cat >> ~/.bashrc << 'EOF'
# Load Anthropic API Key
if [ -f ~/.anthropic_key ]; then
    export ANTHROPIC_API_KEY="$(cat ~/.anthropic_key)"
fi
EOF

source ~/.bashrc
```

### Method 3: Temporary (Current Session Only)

```bash
export ANTHROPIC_API_KEY="sk-ant-api03-YOUR-KEY-HERE"
./aos
```

---

## ✅ Verification

```bash
# Check API key is set
[ -n "$ANTHROPIC_API_KEY" ] && echo "✅ API key is set" || echo "❌ Not set"

# Check key format
[[ "$ANTHROPIC_API_KEY" =~ ^sk-ant-api03- ]] && echo "✅ Valid format" || echo "❌ Invalid"

# Test AOS
./aos
# Should start without errors
```

---

## 🔍 Troubleshooting

### Issue: API key not found in new terminal

**Solution:**
```bash
# Reload bashrc
source ~/.bashrc

# Or close and reopen terminal
```

### Issue: "Code đã thay đổi - JAR cần rebuild!"

This is normal! AOS detected source code changes.

**Options:**
```
[1] Rebuild ngay (30s) ← Choose this
[2] Dev mode lần này
[3] Dùng JAR cũ (not recommended)
```

**See:** [Dev Workflow Guide](DEV_WORKFLOW.md)

### Issue: AOS starts but responses are slow

**Possible causes:**
- First message (loading model)
- Complex task (using Opus instead of Haiku)
- Network latency

**Check:**
```bash
aos> /status
# Shows which model tier is being used
```

---

## 🔒 Security Notes

- ✅ API key automatically sanitized in all responses
- ✅ Prompt injection protection enabled
- ✅ Safe logging (keys never logged)
- ✅ Secure file permissions (600)

**See:** [Security Guide](SECURITY.md) for detailed security features.

---

## 📚 Next Steps

**Getting Started:**
1. ✅ You're here! Quick Start
2. 📖 [Command Guide](guides/AOS_COMMAND_GUIDE.md) - Learn all commands
3. 🏗️ [Architecture](ARCHITECTURE.md) - Understand how AOS works

**Development:**
1. 🔧 [Dev Workflow](DEV_WORKFLOW.md) - Development best practices
2. 🤖 [LLM Classification](guides/LLM_BASED_CLASSIFICATION.md) - How task routing works
3. 📊 [Reports](reports/) - Technical reports and analysis

**Reference:**
- [Full Documentation Index](INDEX.md)
- [Security Audit Report](reports/SECURITY_AUDIT_REPORT.md)
- [GitHub Issues](https://github.com/your-repo/issues) - Report bugs

---

## 🎯 TL;DR (Too Long; Didn't Read)

```bash
# 1. Clone
cd /root/aos

# 2. Install deps
lein deps

# 3. Run (interactive setup)
./aos

# 4. Follow prompts to set API key

# 5. Start chatting!
aos> xin chào
```

**That's it!** 🎉

---

**Having issues?** Check [Troubleshooting](#troubleshooting) or open an issue.
