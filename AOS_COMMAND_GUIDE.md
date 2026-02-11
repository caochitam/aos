# AOS Command Guide

## 🚀 Quick Start

```bash
# Just run AOS - that's it!
./aos

# First time: Interactive setup will help you configure API key
# Next times: Direct to AOS with your saved configuration
```

---

## 📖 Command Options

### Basic Usage

```bash
# Start AOS (recommended)
./aos

# This will:
# 1. Use fast uberjar if available (instant startup)
# 2. Or fall back to lein run (slower, for development)
# 3. Auto-detect missing API key and offer setup
```

### Development Mode

```bash
# Force dev mode (always use lein run)
./aos --dev

# Useful when:
# - You're actively developing/modifying code
# - You want latest code without rebuilding uberjar
# - You're debugging
```

### Rebuild Uberjar

```bash
# Rebuild uberjar with latest code and start
./aos --rebuild

# When to use:
# - After pulling new code from git
# - After modifying source files
# - After adding new dependencies
# - To get fast startup back after using --dev
```

### Help

```bash
# Show help message
./aos --help
./aos -h
```

---

## 🔍 Behind the Scenes

### Script Logic

The `aos` script intelligently chooses the best startup method:

```
                    ./aos
                      │
                      ▼
           ┌──────────────────────┐
           │  Check arguments     │
           └──────────┬───────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
    --help        --dev        --rebuild
        │             │             │
        ▼             ▼             ▼
    Show help   lein run    Rebuild + run

                      │ (no args)
                      ▼
           ┌──────────────────────┐
           │ Check if uberjar     │
           │ exists?              │
           └──────────┬───────────┘
                      │
              ┌───────┴───────┐
              │               │
           YES│               │NO
              │               │
              ▼               ▼
        ┌─────────┐     ┌──────────┐
        │ java -jar│     │ lein run │
        │ (fast)   │     │ (dev)    │
        └─────────┘     └──────────┘
              │               │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │ Interactive   │
              │ API key check │
              └───────────────┘
                      │
                      ▼
              ┌───────────────┐
              │ Start AOS     │
              └───────────────┘
```

---

## ⚡ Performance Comparison

| Method | Startup Time | Use Case |
|--------|-------------|----------|
| **./aos** (uberjar) | ~1-2 seconds | ✅ Normal use |
| **./aos --dev** | ~10-15 seconds | 🔧 Development |
| **lein run** | ~10-15 seconds | 🔧 Development |

---

## 🔄 Common Workflows

### First Time Setup

```bash
# 1. Clone and setup
git clone <repo>
cd aos
lein deps

# 2. Run AOS - interactive setup will guide you
./aos

# 3. Follow prompts to configure API key

# 4. Done! AOS is now configured permanently
```

### Daily Usage

```bash
# Just run it - configuration persists
./aos
```

### After Code Changes

```bash
# Option A: Quick dev test (slower)
./aos --dev

# Option B: Rebuild for speed (recommended)
./aos --rebuild
```

### Development Cycle

```bash
# 1. Make code changes
vim src/agent_os/some_file.clj

# 2. Test quickly
./aos --dev

# 3. When satisfied, rebuild for speed
./aos --rebuild

# 4. Normal usage with fast uberjar
./aos
```

---

## 🛠 Troubleshooting

### Problem: "No ANTHROPIC_API_KEY" error

**Solution 1:** Let interactive setup handle it
```bash
./aos
# Follow prompts - it will set up API key for you
```

**Solution 2:** Set manually before running
```bash
export ANTHROPIC_API_KEY="sk-ant-api03-..."
./aos
```

### Problem: Changes not reflected when running `./aos`

**Cause:** Using old uberjar

**Solution:**
```bash
# Rebuild uberjar with latest code
./aos --rebuild

# Or use dev mode temporarily
./aos --dev
```

### Problem: Slow startup

**Cause:** Using lein run (dev mode) or uberjar not built

**Solution:**
```bash
# Build uberjar for fast startup
./aos --rebuild

# Then normal runs will be fast
./aos
```

### Problem: "UberJAR not found"

**This is normal on first run!** The script will use lein run automatically.

**To build uberjar for speed:**
```bash
./aos --rebuild
```

---

## 📊 Startup Modes Comparison

### Uberjar Mode (Fast)

**Pros:**
- ⚡ Fast startup (~1-2 sec)
- ✅ Single JAR file
- ✅ No lein overhead
- ✅ Production-ready

**Cons:**
- ⚠️ Must rebuild after code changes
- ⚠️ Build time (~30-60 sec)

**When to use:** Normal usage, production, demos

### Lein Run Mode (Dev)

**Pros:**
- ✅ Always latest code
- ✅ No build step needed
- ✅ Good for development

**Cons:**
- ⚠️ Slower startup (~10-15 sec)
- ⚠️ Requires lein
- ⚠️ More memory usage

**When to use:** Development, testing, debugging

---

## 🎯 Best Practices

### For Regular Use
```bash
./aos                    # Use this 99% of the time
```

### For Development
```bash
./aos --dev             # When actively coding
./aos --rebuild         # After finishing changes
```

### For Sharing/Demo
```bash
./aos --rebuild         # Build fresh uberjar first
./aos                   # Then demo with fast startup
```

---

## 🔐 Security Features (Automatic)

All startup methods include:
- ✅ Interactive API key setup
- ✅ Prompt injection protection
- ✅ Automatic sanitization
- ✅ Secure credential storage
- ✅ Persistent configuration

No extra configuration needed!

---

## 📝 Environment Variables

The aos script respects these environment variables:

```bash
# API key (auto-detected)
export ANTHROPIC_API_KEY="sk-ant-..."

# JVM options (optional)
export JAVA_OPTS="-Xmx2g -Xms512m"

# Use with aos:
JAVA_OPTS="-Xmx2g" ./aos
```

---

## 🚀 Advanced Usage

### Custom JVM Options

```bash
# Run with custom memory settings
java -Xmx4g -jar target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar
```

### Background Mode

```bash
# Run in background
./aos &

# Or with nohup
nohup ./aos > aos.log 2>&1 &
```

### With Custom Config

```bash
# Set custom config file
CONFIG_FILE=/path/to/config.edn ./aos
```

---

## 📋 Checklist for New Users

- [ ] Clone repository
- [ ] Run `lein deps`
- [ ] Run `./aos` - follow interactive setup
- [ ] API key configured automatically
- [ ] Run `./aos --rebuild` for fast startup
- [ ] Done! Use `./aos` normally

---

## 🎉 That's It!

**99% of the time, just use:**
```bash
./aos
```

Everything else is automatic! 🚀

---

## 📚 Related Docs

- [INTERACTIVE_SETUP.md](INTERACTIVE_SETUP.md) - API key setup guide
- [SECURITY.md](SECURITY.md) - Security details
- [README.md](README.md) - Full documentation
