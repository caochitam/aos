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
- **src/agent_os/llm/delegator.clj** - Smart task delegation (simple → AOS tools, complex → Claude Code)

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
- **src/agent_os/reflection/engine.clj** - Code analysis, issue detection

## Key Concepts

### Task Delegation Strategy
1. **Simple tasks** (chat, read file, run command) → AOS uses tools.clj
2. **Complex tasks** (refactor, modify multiple files) → Delegate to Claude Code CLI

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
1. Complexity detection (delegator.clj)
2. If complex: spawn Claude Code subprocess
3. Claude Code reads/modifies files autonomously
4. Changes take effect on next restart (lein run)

## Protected Components
- **Kernel namespace** (agent-os.kernel.*) - Immutable
- **Safety engine** - Cannot be modified
- **Critical components** - Require approval

## Working Directory
All operations run from: `/root/aos`
