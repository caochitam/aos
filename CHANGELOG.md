# Changelog

All notable changes to Agent OS will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **restart command**: New CLI command to restart Agent OS without exiting the process
  - Reloads configuration from disk (config.edn, SOUL.edn, etc.)
  - Reinitializes all subsystems (kernel, memory, LLM providers)
  - Useful after adding Claude session token or updating configuration
  - Usage: `aos> restart`
  - File: `/root/aos/src/agent_os/cli/gateway.clj`
  - File: `/root/aos/src/agent_os/core.clj`

- **chat command**: Natural language chat interface with AOS agent
  - Chat with the AOS agent using natural language
  - Integrates with agent's soul, identity, and user context
  - Uses configured LLM provider (Claude session or API key)
  - Usage: `aos> chat <your message>`
  - File: `/root/aos/src/agent_os/cli/gateway.clj`

- **command-exists? helper**: Cross-platform command existence checking
  - Checks if a shell command exists before attempting to run it
  - Prevents noisy exceptions from missing commands
  - Used for browser launching and keychain access
  - File: `/root/aos/src/agent_os/llm/claude_session.clj`

### Changed
- **Improved error handling for OAuth login flow**
  - Browser launching now checks if command exists before attempting to open
  - Falls back to manual URL instruction if browser command unavailable
  - Cleaner output on headless servers
  - File: `/root/aos/src/agent_os/llm/claude_session.clj`

- **Improved keychain access error handling**
  - macOS keychain access only attempted when `security` command exists
  - Keychain warnings downgraded from WARN to DEBUG level
  - Automatic fallback to file-based storage on non-macOS systems
  - File: `/root/aos/src/agent_os/llm/claude_session.clj`

- **Logging improvements**
  - Added Timbre middleware to suppress noisy IOExceptions from shell commands
  - Shell command failures (xdg-open, security) no longer clutter logs
  - Configurable log level (default: :info)
  - File: `/root/aos/src/agent_os/core.clj`

### Fixed
- **Compilation errors resolved**
  - soul.clj: Fixed `catch` outside `try` block syntax error
  - claude.clj: Restructured retry logic to avoid `recur` inside `try` block
  - gateway.clj: Added missing protocol imports (`receive-message`, `send-message`)
  - gateway.clj: Fixed function arity for `cmd-help`, `cmd-status`, `cmd-components`

### Documentation
- Updated `/root/aos/README.md` with restart and chat commands
- Updated `/root/aos/docs/README.md` with restart and chat commands
- Updated `/root/aos/CLAUDE_MAX_LOGIN.md` with improved troubleshooting
- Updated `/root/aos/docs/DEPLOYMENT.md` with:
  - Runtime management section
  - Improved troubleshooting for shell command issues
  - Browser and keychain error handling guidance

## [0.1.0] - 2025-02-11

### Added
- Initial release of Agent OS
- Self-modifying AI architecture with 12 layers
- Claude Max/Pro session-based authentication
- CLI gateway for interactive administration
- File-first persistent memory system
- Identity and soul engine
- Self-improvement loop
- Safety mechanisms and kernel protection
- Multi-provider LLM support with failover
