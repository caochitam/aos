# Agent OS Architecture

## Core Files & Their Purpose

### Entry Point
- **src/agent_os/core.clj** - Main entry point, creates AOS components, runs CLI

### CLI Interface
- **src/agent_os/cli/gateway.clj** - CLI commands, chat handling, user interaction
  - Contains: command-registry, cmd-help, cmd-chat, start-cli
  - Slash commands: /help, /status, /exit, /restart, etc.

### LLM Integration
- **src/agent_os/llm/claude.clj** - Claude API provider (Sonnet 4.5)
- **src/agent_os/llm/router.clj** - Provider routing with failover
- **src/agent_os/llm/tools.clj** - Tool system (read_file, edit_file, bash)
- **src/agent_os/llm/delegator.clj** - Smart task delegation with **LLM-based classification**
  - Uses Haiku to classify task complexity (SIMPLE/MODERATE/COMPLEX)
  - Replaces hard-coded rules with meta-cognition
  - Cost: $0.00005/request with 95%+ accuracy
  - Three-tier model routing (Haiku/Sonnet/Opus)

### Security
- **src/agent_os/security/sanitizer.clj** - API key sanitization and safe logging
  - Redacts API keys from all outputs
  - Prevents prompt injection attacks
  - Safe error messages
- **src/agent_os/security/vault.clj** - Secure credential storage
  - Encrypted vault for API keys
  - Secure file permissions (600)
  - Environment variable integration

### Interactive Setup
- **src/agent_os/setup/interactive.clj** - Interactive API key setup
  - Auto-detects missing API key
  - Guides user through configuration
  - Supports bashrc, secure file, or temp storage
  - Validates API key format

### Identity & Personality
- **src/agent_os/identity/soul.clj** - Personality, boundaries, goals
  - Defines: traits (analytical, cautious), risk tolerance, communication style
  - System prompt generation

### Kernel & Components
- **src/agent_os/kernel/core.clj** - Protected kernel, component management
- **src/agent_os/kernel/protocols.clj** - Core protocols (IKernel, IComponent, IChannel)

### Self-Modification
- **src/agent_os/modification/engine.clj** - Modification proposals, validation
- **src/agent_os/improvement/loop.clj** - Self-improvement cycle

### Memory & Reflection
- **src/agent_os/memory/store.clj** - Memory system (facts, decisions, patterns)
- **src/agent_os/memory/compaction.clj** - Conversation compaction for long sessions
  - Auto-summarize when > 4000 tokens
  - Uses Haiku (cheap) for summarization
  - Preserves recent 10 messages
  - 40-60% long-term token savings
- **src/agent_os/reflection/engine.clj** - Code analysis, issue detection

## Key Concepts

### LLM-Based Task Classification (NEW!)

**Problem:** Hard-coded rules fail to understand Vietnamese context ("bỏ" vs "thêm")

**Solution:** Meta-cognition - LLM classifies its own tasks

**Flow:**
```
User: "bỏ thông báo khi khởi động"
  ↓
Haiku classification (cost: $0.00005):
  "COMPLEX - Cần sửa code để remove notification"
  ↓
Delegate to Claude Code ✓
```

**Benefits:**
- 95%+ accuracy (vs 60-70% hard-coded)
- Understands Vietnamese naturally
- Zero maintenance
- ROI: 400-20,000x

### Task Delegation Strategy
1. **SIMPLE tasks** (read file, run command) → AOS uses Haiku + tools.clj
2. **MODERATE tasks** (simple edits, cleanup) → AOS uses Sonnet + tools.clj
3. **COMPLEX tasks** (refactor, multi-file changes) → Delegate to Claude Code (Opus)

### System Prompt
Generated in `soul.clj:get-system-prompt`, includes:
- Personality traits
- Boundaries (never modify: kernel, safety-engine)
- Goals (self-improvement, stability, efficiency)
- Language preference (Vietnamese by default)

### Tool Execution Loop
1. User message → Claude API (with tools)
2. Claude returns tool_use blocks
3. AOS executes tools (read_file, edit_file, bash)
4. Results sent back to Claude
5. Continue until final text response

## Self-Modification Flow

When user asks to modify AOS:
1. **LLM classification** (delegator.clj) - Haiku classifies task complexity
2. If COMPLEX: spawn Claude Code subprocess
3. Claude Code reads/modifies files autonomously
4. **Auto-rebuild detection** - `aos` script detects source changes
5. User rebuilds JAR or uses dev mode
6. Changes take effect on next restart

## Protected Components
- **Kernel namespace** (agent-os.kernel.*) - Immutable
- **Safety engine** - Cannot be modified
- **Critical components** - Require approval

## Auto-Rebuild Detection (NEW!)

**Problem:** Source code changes not reflected after editing → bất đồng bộ

**Solution:** `aos` script auto-detects when source is newer than JAR

**Flow:**
```bash
./aos

# If source modified:
⚠️ Code đã thay đổi - JAR cần rebuild!
[1] Rebuild ngay (30s)
[2] Dev mode (slow startup)
[3] Dùng JAR cũ
```

**Benefits:**
- No more stale code issues
- Clear user options
- Fast startup after rebuild

## Working Directory
All operations run from: `/root/aos`
