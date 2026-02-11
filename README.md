# AOS - Agent Operating System

Self-modifying AI architecture built on Clojure, enabling Claude Sonnet to autonomously read and improve its own code.

## Architecture

12-layer self-modifying AI architecture with OpenClaw-inspired patterns:

- **Layer 1-3**: Immutable kernel + Reflection engine
- **Layer 4-5**: Multi-provider LLM interface + Safe modification engine
- **Layer 6**: File-first persistent memory
- **Layer 7**: Self-improvement loop
- **Layer 8**: Safety & constraints validation
- **Layer 9**: Identity & soul engine
- **Layer 10**: Heartbeat & proactive loop
- **Layer 12**: Admin CLI gateway

## Installation

### Prerequisites

- Java 17+ (OpenJDK)
- Leiningen 2.x

### Setup

```bash
# Clone repository
cd /root/aos

# Install dependencies
lein deps

# Run tests
lein test
```

## Configuration

### 🔒 Security: API Key Protection

**IMPORTANT**: AOS includes multiple security layers to protect your API key from prompt injection attacks.

Set your Anthropic API key via environment variable (RECOMMENDED):

```bash
# Method 1: Direct export
export ANTHROPIC_API_KEY=sk-ant-api03-...

# Method 2: Secure file storage
echo "sk-ant-api03-..." > ~/.anthropic_key
chmod 600 ~/.anthropic_key
export ANTHROPIC_API_KEY="$(cat ~/.anthropic_key)"
```

Configuration file `resources/config.edn`:

```clojure
{:llm {:provider :claude
       :api-key #env ANTHROPIC_API_KEY}  ; ✅ Reads from environment
 :kernel {:max-modifications-per-hour 10}
 :safety {:protected-namespaces ["agent-os.kernel"]}}
```

**Security Features:**
- ✅ Automatic API key sanitization in all responses
- ✅ Prompt injection protection
- ✅ Secure credential vault system
- ✅ Safe logging (API keys never logged)
- ✅ Error message sanitization

See [SECURITY.md](docs/SECURITY.md) for detailed security documentation.

## Running AOS

### 🎯 First Time Setup (Interactive)

**NEW!** AOS now auto-detects missing API key and guides you through setup:

```bash
# Just run AOS - it will help you set up API key if needed
lein run

# AOS will:
# 1. Detect if ANTHROPIC_API_KEY is missing
# 2. Guide you through interactive setup
# 3. Help you choose: bashrc, secure file, or temp
# 4. Automatically configure everything
# 5. Continue starting AOS
```

See [INTERACTIVE_SETUP.md](docs/guides/INTERACTIVE_SETUP.md) for detailed guide.

### CLI Mode (Default)

```bash
lein run
```

Available commands:
- `help` - Show available commands
- `status` - System status
- `components` - List all components
- `inspect <id>` - Inspect component details
- `memory [facts|decisions|patterns]` - View persistent memory
- `improve <component-id>` - Run self-improvement cycle
- `history [n]` - Show modification history
- `soul` - View agent personality
- `chat <message>` - Chat with AOS agent in natural language
- `restart` - Restart Agent OS (reload configuration and reinitialize)
- `exit` - Exit CLI

### REPL Mode

```bash
lein repl

;; Create and boot AOS
(require '[agent-os.core :as aos])
(require '[agent-os.config :as config])

(def cfg (config/load-config))
(def os (aos/create-agent-os cfg))

;; Explore system
(require '[agent-os.kernel.protocols :as kp])
(kp/status (:kernel os))
(kp/list-components (:kernel os))
```

## Testing

```bash
# Run all tests
lein test

# Run specific test namespace
lein test agent-os.kernel.core-test

# Run with coverage
lein cloverage
```

## Project Structure

```
aos/
├── src/agent_os/          # Source code
│   ├── kernel/            # Layer 1: Immutable kernel
│   ├── reflection/        # Layer 3: Self-analysis
│   ├── llm/               # Layer 4: LLM providers
│   ├── modification/      # Layer 5: Safe code modification
│   ├── memory/            # Layer 6: Persistent memory
│   ├── improvement/       # Layer 7: Self-improvement
│   ├── safety/            # Layer 8: Validation & security
│   ├── identity/          # Layer 9: Soul & personality
│   ├── heartbeat/         # Layer 10: Proactive loop
│   └── cli/               # Layer 12: CLI gateway
├── test/                  # Test suite
├── resources/             # Configuration & templates
├── data/                  # Runtime data (file-first memory)
└── docs/                  # Architecture documentation
```

## Development

### Adding New Components

```clojure
(require '[agent-os.kernel.core :as kernel])
(require '[agent-os.kernel.protocols :as kp])

(def my-component
  (kernel/create-component
    :user/my-component
    '(defn process [x] (* x 2))
    :purpose "Example component"
    :interfaces [:compute]
    :dependencies #{}))

(kp/register-component (:kernel os) my-component)
```

### Running Self-Improvement

```clojure
(require '[agent-os.improvement.loop :as improve])

(improve/improve-cycle
  (:kernel os)
  (:llm-registry os)
  (:history os)
  :user/my-component
  (:config os))
```

## Safety Features

- **Kernel Protection**: Core kernel cannot be modified
- **Syntax Validation**: All code validated before execution
- **Dependency Checking**: Ensures dependencies exist
- **Size Limits**: Code size restrictions
- **Dangerous Symbol Detection**: Blocks eval, shell execution
- **Rollback Capability**: All modifications can be reverted

## 🤖 Zalo Bot Integration

**NEW!** AOS can now communicate with users via Zalo Official Account:

```bash
# Quick start
./start-zalo-bot.sh
```

Features:
- ✅ Receive and reply to messages from Zalo users
- ✅ Full AI capabilities via Zalo chat
- ✅ Automatic webhook server
- ✅ Typing indicators & great UX
- ✅ Persistent memory of conversations

See **[Zalo Integration Guide](docs/guides/ZALO_INTEGRATION.md)** for complete setup instructions.

## 📚 Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[Quick Start](docs/QUICK_START.md)** - Get started in 5 minutes
- **[Architecture](docs/ARCHITECTURE.md)** - System design and components
- **[Zalo Integration](docs/guides/ZALO_INTEGRATION.md)** - Connect AOS to Zalo bot
- **[Security Guide](docs/SECURITY.md)** - Security features and best practices
- **[Dev Workflow](docs/DEV_WORKFLOW.md)** - Development workflow and auto-rebuild
- **[Command Guide](docs/guides/AOS_COMMAND_GUIDE.md)** - All AOS commands
- **[LLM Classification](docs/guides/LLM_BASED_CLASSIFICATION.md)** - Intelligent task routing
- **[Full Index](docs/INDEX.md)** - Complete documentation index

## OpenClaw Integration Patterns

1. **File-First Memory** - MEMORY.edn + daily logs
2. **Identity as Data** - Soul/Identity/User as EDN
3. **Provider Plugin System** - Multi-LLM with failover
4. **Capability-Based Security** - Permission per component
5. **Autonomous Invocation** - Heartbeat with standing instructions

## License

MIT

## Credits

- Inspired by OpenClaw (Peter Steinberger)
- Built with Clojure
- Powered by Claude Sonnet 4
