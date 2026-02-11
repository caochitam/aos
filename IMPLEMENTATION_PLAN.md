# AOS (Agent OS) - Kế Hoạch Triển Khai Chi Tiết

> Tài liệu này là bản kế hoạch chi tiết để Claude Sonnet thực hiện code.
> Mỗi Phase là độc lập, hoàn thành Phase trước mới chuyển Phase sau.
> Đọc kỹ docs/ trước khi bắt đầu code.

---

## PHASE 0: Project Scaffolding (Ưu tiên cao nhất)

### Mục tiêu: Tạo cấu trúc dự án Clojure chuẩn

### 0.1 - Tạo project.clj
```clojure
(defproject agent-os "0.1.0-SNAPSHOT"
  :description "Self-modifying AI Agent Operating System"
  :url "https://github.com/your-org/agent-os"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/core.async "1.6.681"]
                 [org.clojure/tools.logging "1.2.4"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [mount "0.1.17"]            ; component lifecycle
                 [aero "1.1.6"]              ; config management
                 [taoensso/timbre "6.3.1"]   ; logging
                 [ring/ring-core "1.10.0"]   ; HTTP server (future API)
                 [metosin/reitit "0.7.0"]]   ; routing (future API)
  :main ^:skip-aot agent-os.core
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[midje "1.10.9"]
                                  [org.clojure/test.check "1.1.1"]]}
             :uberjar {:aot :all}})
```

### 0.2 - Tạo cấu trúc thư mục
```
aos/
├── project.clj
├── README.md
├── docs/                          # (giữ nguyên tài liệu hiện có)
│   ├── agent-os-architecture.clj
│   ├── agent-os-example.clj
│   ├── README.md
│   ├── DEPLOYMENT.md
│   ├── LANGUAGE_COMPARISON.md
│   └── OPENCLAW_INTEGRATION.md
├── src/
│   └── agent_os/
│       ├── core.clj               # Entry point, -main function
│       ├── config.clj              # Configuration loading (aero)
│       ├── kernel/
│       │   ├── core.clj            # Layer 1: Immutable kernel
│       │   └── protocols.clj       # IKernel, IComponent protocols
│       ├── metadata/
│       │   └── schema.clj          # Layer 2: Architecture metadata
│       ├── reflection/
│       │   └── engine.clj          # Layer 3: Self-analysis
│       ├── llm/
│       │   ├── protocols.clj       # ILLMProvider protocol (multi-LLM)
│       │   ├── claude.clj          # Layer 4: Claude Sonnet implementation
│       │   └── router.clj          # Provider selection & failover
│       ├── modification/
│       │   └── engine.clj          # Layer 5: Safe modification
│       ├── memory/
│       │   ├── store.clj           # Layer 6: File-first persistence
│       │   ├── search.clj          # Hybrid search (future)
│       │   └── compaction.clj      # Context compaction
│       ├── improvement/
│       │   └── loop.clj            # Layer 7: Self-improvement cycle
│       ├── safety/
│       │   ├── validator.clj       # Layer 8: Code validation
│       │   ├── permissions.clj     # Capability-based access
│       │   └── sanitizer.clj       # Input sanitization
│       ├── identity/
│       │   └── soul.clj            # Layer 9: Soul/Identity engine
│       ├── heartbeat/
│       │   └── loop.clj            # Layer 10: Proactive heartbeat
│       ├── orchestration/
│       │   ├── manager.clj         # Layer 11: Multi-agent (Phase 3)
│       │   └── queue.clj           # Lane Queue (inspired by OpenClaw)
│       └── cli/
│           └── gateway.clj         # Layer 12: Admin CLI
├── resources/
│   ├── config.edn                  # Default configuration
│   ├── templates/
│   │   ├── SOUL.edn                # Default soul template
│   │   ├── IDENTITY.edn            # Default identity template
│   │   └── USER.edn                # Default user preferences
│   └── skills/                     # Built-in skill descriptors
│       ├── code-review.edn
│       └── refactor.edn
├── data/                           # Runtime data (gitignored)
│   ├── MEMORY.edn
│   ├── SOUL.edn
│   ├── memory/                     # Daily logs
│   └── history/                    # Modification history
├── test/
│   └── agent_os/
│       ├── kernel/
│       │   └── core_test.clj
│       ├── reflection/
│       │   └── engine_test.clj
│       ├── modification/
│       │   └── engine_test.clj
│       ├── memory/
│       │   └── store_test.clj
│       ├── safety/
│       │   └── validator_test.clj
│       └── integration_test.clj
└── .gitignore
```

### 0.3 - Tạo .gitignore
```
/target
/data/
*.edn.bak
.nrepl-port
.lein-*
*.jar
*.class
.env
```

### 0.4 - Tạo resources/config.edn
```clojure
{:llm {:provider :claude
       :model "claude-sonnet-4-20250514"
       :max-tokens 4000
       ;; Hỗ trợ 2 authentication methods:
       ;; 1. Claude Max/Pro session (recommended) - use `login-claude` command
       ;; 2. API key - set env var ANTHROPIC_API_KEY
       :api-key #env ANTHROPIC_API_KEY}
 :kernel {:immutable true
          :max-modifications-per-hour 10}
 :memory {:base-path "data/"
          :memory-file "MEMORY.edn"
          :daily-log-dir "memory/"
          :history-dir "history/"
          :max-context-tokens 150000
          :flush-threshold 0.8}
 :heartbeat {:enabled false
             :interval-ms 1800000
             :standing-instructions []}
 :safety {:protected-namespaces ["agent-os.kernel"]
          :max-code-size-bytes 50000
          :allowed-requires #{"clojure.string" "clojure.set" "clojure.edn"}}
 :cli {:prompt "aos> "
       :history-file "data/.cli-history"}}
```

---

## PHASE 1: Core Kernel + Reflection (Layer 1-3)

### Mục tiêu: Agent có thể boot và đọc code của chính nó

### 1.1 - kernel/protocols.clj
Định nghĩa protocols:
```clojure
(ns agent-os.kernel.protocols)

(defprotocol IKernel
  (boot [this config] "Initialize kernel with config")
  (shutdown [this] "Graceful shutdown")
  (status [this] "Return kernel status map")
  (get-component [this component-id] "Get component by ID")
  (list-components [this] "List all component IDs")
  (register-component [this component] "Register a new component"))

(defprotocol IComponent
  (component-id [this] "Unique identifier")
  (component-version [this] "Current version number")
  (component-code [this] "Source code as string")
  (component-metadata [this] "Metadata map"))

(defprotocol IChannel
  "Kênh giao tiếp với AOS - bắt đầu chỉ có CLI, AOS tự thêm kênh sau"
  (receive [this] "Nhận message từ bên ngoài, trả về {:from :content :timestamp}")
  (send [this message] "Gửi message ra ngoài")
  (channel-id [this] "Identifier của kênh")
  (channel-status [this] "Trạng thái kênh :open/:closed"))
```

**Tại sao cần IChannel ngay Phase 1:** Đây là abstraction tối thiểu để AOS sau này có thể tự tạo channel mới (HTTP, WebSocket, Telegram...) bằng cách self-modify - chỉ cần implement protocol này. CLI là implementation đầu tiên và duy nhất ban đầu.

### 1.2 - kernel/core.clj
Implement kernel sử dụng atom cho state:
- Boot sequence: load config → init state → register core components → ready
- Kernel state là atom chứa map: `{:status :running, :components {}, :boot-time ...}`
- Components stored as immutable maps trong atom
- `register-component` chỉ cho phép add, KHÔNG cho phép overwrite kernel components
- Sử dụng `mount` library cho lifecycle management

### 1.3 - reflection/engine.clj
Implement reflection engine:
- `read-own-source [namespace]` - Đọc source file từ classpath
- `analyze-component [component]` - Parse S-expressions, extract function signatures
- `list-dependencies [component]` - Tìm tất cả `require` dependencies
- `component-complexity [component]` - Đếm LOC, function count, nesting depth
- `find-issues [component]` - Phát hiện code smells (deep nesting, long functions, missing docstrings)
- Sử dụng `clojure.tools.reader` để parse Clojure code an toàn

### 1.4 - Tests cho Phase 1
- Test kernel boot/shutdown lifecycle
- Test component registration và retrieval
- Test reflection reads actual source files
- Test analysis produces correct metadata

---

## PHASE 2: LLM Interface + Modification Engine (Layer 4-5)

### Mục tiêu: Agent có thể gọi Claude API và thực hiện self-modification an toàn

### 2.1 - llm/protocols.clj (Học từ OpenClaw: Provider Plugin System)
```clojure
(defprotocol ILLMProvider
  (provider-name [this] "Provider identifier string")
  (chat [this messages opts] "Send messages, return response string")
  (supports-tools? [this] "Does provider support tool calling?")
  (available? [this] "Is provider currently available?"))
```
Thiết kế multi-provider thay vì lock-in Claude:
- Mỗi provider implement protocol
- Router chọn provider theo config + failover

### 2.2 - llm/claude.clj
Implement Claude provider thực tế:
- Sử dụng `clj-http` gọi Anthropic Messages API
- Endpoint: `https://api.anthropic.com/v1/messages`
- Headers: `x-api-key`, `anthropic-version: 2023-06-01`
- Model: đọc từ config, default `claude-sonnet-4-20250514`
- Handle rate limiting với exponential backoff
- Parse response body, extract text content
- Error handling: API errors, network errors, timeout

### 2.2.1 - llm/claude_session.clj (Claude Max/Pro Login - ADDED)
Session-based authentication cho Claude Max/Pro subscription:
- Browser-based OAuth login flow
- Multi-platform token storage:
  - macOS: Keychain (encrypted)
  - Linux/Windows: `~/.claude/session_token` (owner-only permissions)
  - Environment variable: `CLAUDE_SESSION_TOKEN`
- CLI commands: `login-claude`, `logout-claude`
- Auto-detect session token và ưu tiên session over API key
- Xem chi tiết: [CLAUDE_MAX_LOGIN.md](../CLAUDE_MAX_LOGIN.md)

### 2.3 - llm/router.clj
Provider router:
- Load providers từ config
- `select-provider` - chọn provider available đầu tiên
- `chat-with-failover` - thử provider chính, fallback sang backup
- Cooldown mechanism cho provider bị rate-limited (học từ OpenClaw)

### 2.4 - modification/engine.clj
Safe code modification:
- `create-proposal [component-id changes reason]` - Tạo modification proposal
- `validate-proposal [proposal]` - Validate:
  - Syntax check (parse S-expression)
  - Safety check (không modify kernel)
  - Dependency check (không break existing deps)
  - Size check (không exceed limit)
- `apply-modification [proposal]` - Apply nếu valid:
  - Backup current version
  - Write new code
  - Increment version
  - Record to history
- `rollback-modification [modification-id]` - Restore từ backup
- `modification-history [component-id]` - List all modifications

### 2.5 - Tests cho Phase 2
- Test Claude API call (mock HTTP cho unit test)
- Test provider failover
- Test modification proposal creation
- Test validation catches bad code
- Test apply + rollback cycle
- Integration test: reflect → propose → validate → apply

---

## PHASE 3: Memory + Safety + Identity (Layer 6, 8, 9)

### Mục tiêu: Agent có persistent memory, safety constraints, và personality

### 3.1 - memory/store.clj (Học từ OpenClaw: File-First Memory)
Implement file-first memory system:
- `remember-fact [category fact]` - Lưu vào MEMORY.edn
- `remember-decision [decision reasoning]` - Lưu quyết định
- `recall [query]` - Tìm kiếm trong memory
- `daily-log [entry]` - Append vào `data/memory/YYYY-MM-DD.edn`
- `load-memory []` - Load MEMORY.edn vào memory
- `save-memory [memory-state]` - Persist to disk

**Cấu trúc MEMORY.edn:**
```clojure
{:facts [{:category :architecture
          :content "Kernel is immutable"
          :timestamp "2026-02-11T10:00:00Z"
          :confidence 1.0}]
 :decisions [{:decision "Use EDN for all config"
              :reasoning "Native Clojure, human-readable"
              :timestamp "2026-02-11T10:00:00Z"}]
 :patterns [{:pattern "Deep nesting indicates complexity"
             :learned-from "modification-123"
             :success-rate 0.85}]}
```

### 3.2 - memory/compaction.clj (Học từ OpenClaw: Context Compaction)
- `estimate-context-size [messages]` - Ước tính token count
- `should-compact? [messages config]` - Check threshold
- `compact-context [messages memory-store]` -
  1. Flush important facts to MEMORY.edn trước
  2. Summarize conversation history
  3. Return compacted messages
- `flush-before-compact [messages memory-store]` - Tự động lưu durable info trước khi compact (pattern từ OpenClaw)

### 3.3 - safety/validator.clj
Code validation pipeline:
- `validate-syntax [code-string]` - Parse Clojure code
- `validate-safety [code-string protected-ns]` - Check:
  - Không require dangerous namespaces (java.io.File delete, System/exit)
  - Không modify protected namespaces
  - Không chứa infinite loops (basic heuristic)
  - Không exceed size limits
- `validate-dependencies [code-string existing-deps]` - Verify deps exist

### 3.4 - safety/permissions.clj (Học từ OpenClaw: Capability-Based Security)
```clojure
;; Permission model
{:component-id "modification-engine"
 :permissions #{:read-code :write-code :read-memory :write-memory}
 :denied #{:modify-kernel :delete-component :system-exit}}
```
- `check-permission [component-id action]` - Verify permission
- `grant-permission [component-id permission]` - Add permission
- `revoke-permission [component-id permission]` - Remove permission
- Default deny policy - chỉ cho phép những gì explicitly granted

### 3.5 - safety/sanitizer.clj (Học từ OpenClaw: Input Sanitization)
- `sanitize-llm-output [output]` - Clean LLM response trước khi eval
- `detect-injection [input]` - Detect prompt injection patterns
- `trust-level [source]` - Classify: `:trusted` (kernel), `:semi-trusted` (agent), `:untrusted` (user)
- `validate-at-boundary [input trust-level]` - Validate input based on trust level

### 3.6 - identity/soul.clj
Identity system:
- `create-soul [template-path]` - Initialize từ template
- `load-soul [data-path]` - Load existing SOUL.edn
- `evolve-soul [soul experience]` - Update personality based on experience
- `get-system-prompt [soul identity user]` - Generate system prompt từ identity data
- `save-soul [soul path]` - Persist soul state

**SOUL.edn structure:**
```clojure
{:name "AOS Agent"
 :version 1
 :traits {:curiosity 0.8 :caution 0.7 :creativity 0.6}
 :values ["reliability" "safety" "continuous-improvement"]
 :communication-style :concise
 :experience-count 0
 :evolved-at nil}
```

### 3.7 - Tests cho Phase 3
- Test memory CRUD operations
- Test context compaction preserves important info
- Test safety validator catches dangerous code
- Test permission system denies unauthorized access
- Test input sanitization
- Test soul creation, loading, evolution

---

## PHASE 4: Self-Improvement Loop + Heartbeat (Layer 7, 10)

### Mục tiêu: Agent có thể tự cải thiện code và hoạt động proactive

### 4.1 - improvement/loop.clj
7-step self-improvement cycle:
```clojure
(defn improve-cycle [kernel llm-provider memory component-id]
  ;; Step 1: Reflect - đọc code hiện tại
  ;; Step 2: Analyze - gửi code cho LLM phân tích
  ;; Step 3: Decide - LLM quyết định có cần modify không
  ;; Step 4: Generate - LLM tạo improved code
  ;; Step 5: Validate - chạy safety checks
  ;; Step 6: Apply - apply modification an toàn
  ;; Step 7: Learn - record outcome, update patterns
  )
```

- `identify-improvement-targets [kernel]` - Tìm components cần improve
- `prioritize-targets [targets]` - Rank theo complexity, age, issue-count
- `improve-component [kernel llm component-id]` - Run full cycle cho 1 component
- `improve-all [kernel llm]` - Run cycle cho tất cả eligible components
- `improvement-report [results]` - Tạo report về changes

### 4.2 - heartbeat/loop.clj (Học từ OpenClaw: Autonomous Invocation)
Proactive heartbeat:
- `start-heartbeat [kernel config]` - Start background thread (core.async go-loop)
- `stop-heartbeat [heartbeat-state]` - Graceful stop
- `heartbeat-tick [kernel config]` - Single heartbeat iteration:
  1. Check standing instructions
  2. Review recent modifications
  3. Check memory for pending tasks
  4. Decide: improve something? log status? rest?
- `standing-instructions` - Configurable list of periodic checks

**Khác biệt với OpenClaw:** AOS heartbeat không chỉ check messages mà còn trigger self-improvement cycles.

### 4.3 - Tests cho Phase 4
- Test improvement cycle end-to-end (with mocked LLM)
- Test heartbeat starts/stops correctly
- Test heartbeat executes standing instructions
- Test improvement prioritization logic

---

## PHASE 5: Admin CLI Gateway (Layer 12)

### Mục tiêu: Người dùng non-technical có thể tương tác với AOS

### 5.1 - cli/gateway.clj
Interactive CLI:
- REPL-based interface sử dụng `clojure.main/repl`
- Command parsing: split input, dispatch to handler
- Commands registry (map of command → handler function):

```clojure
(def commands
  {"help"        cmd-help
   "status"      cmd-status
   "components"  cmd-components
   "inspect"     cmd-inspect        ; inspect <component-id>
   "analyze"     cmd-analyze        ; analyze <component-id>
   "improve"     cmd-improve        ; improve [component-id | all]
   "memory"      cmd-memory         ; memory [facts | decisions | patterns]
   "remember"    cmd-remember       ; remember <fact>
   "soul"        cmd-soul           ; soul [view | evolve]
   "heartbeat"   cmd-heartbeat      ; heartbeat [start | stop | status]
   "skills"      cmd-skills         ; skills [list | load | unload]
   "history"     cmd-history        ; history [component-id]
   "rollback"    cmd-rollback       ; rollback <modification-id>
   "chat"        cmd-chat           ; chat <message> (free-form LLM interaction)
   "permissions" cmd-permissions    ; permissions [component-id]
   "shutdown"    cmd-shutdown})
```

- Pretty-print output sử dụng `clojure.pprint`
- Command history (saved to file)
- Tab completion (bonus)
- Colored output (bonus)

### 5.2 - core.clj (Entry Point)
```clojure
(ns agent-os.core
  (:require [agent-os.kernel.core :as kernel]
            [agent-os.config :as config]
            [agent-os.cli.gateway :as cli]))

(defn -main [& args]
  (let [config (config/load-config)
        kernel (kernel/create-kernel config)]
    (kernel/boot kernel config)
    (println "Agent OS v0.1.0 - Ready")
    (cli/start-repl kernel config)))
```

### 5.3 - Tests cho Phase 5
- Test command parsing
- Test each command handler
- Test invalid command handling
- Test CLI output formatting

---

## PHASE 6: Integration & Polish

### Mục tiêu: Kết nối tất cả layers, end-to-end testing

### 6.1 - Integration Tests
- Boot kernel → register components → reflect → analyze → improve → verify
- Full self-modification cycle với real Claude API (optional, cần API key)
- Memory persistence across restarts
- Heartbeat trigger → self-improvement → memory update

### 6.2 - Logging (Học từ OpenClaw: JSONL Audit Trail)
- Sử dụng `taoensso/timbre` cho structured logging
- Log mọi modification với full context
- Log mọi LLM API call với prompt/response
- Daily log rotation

### 6.3 - Error Handling
- Wrap tất cả LLM calls trong try-catch
- Graceful degradation khi API unavailable
- Circuit breaker cho repeated failures

### 6.4 - Documentation
- Docstrings cho tất cả public functions
- `lein doc` để generate API docs
- Quick-start guide trong README.md

---

## QUY TẮC CHO CLAUDE SONNET KHI VIẾT CODE

1. **Đọc docs/ trước** - Tất cả thiết kế chi tiết nằm trong `docs/agent-os-architecture.clj` và `docs/agent-os-example.clj`. Đọc kỹ trước khi viết bất kỳ code nào.

2. **Clojure idioms** - Sử dụng:
   - Immutable data structures (maps, vectors, sets)
   - Pure functions wherever possible
   - `atom` chỉ khi cần mutable state
   - Threading macros (`->`, `->>`)
   - Destructuring
   - `defprotocol` / `defrecord` cho polymorphism

3. **EDN everywhere** - Config, data, memory đều dùng EDN format. KHÔNG dùng JSON hay YAML.

4. **Safety first** - Mọi code modification phải qua validation pipeline. KHÔNG skip safety checks.

5. **Test mỗi function** - Viết test cùng lúc với implementation. Dùng `clojure.test` hoặc `midje`.

6. **Error messages rõ ràng** - Khi validation fail, trả về message giải thích tại sao.

7. **Không over-engineer** - Phase 1 focus vào hoạt động được. Optimize sau.

8. **File-first** - Memory luôn persist ra file. KHÔNG chỉ giữ trong memory.

9. **Kernel immutable** - KHÔNG BAO GIỜ cho phép modify kernel namespace.

10. **Log everything** - Mọi action quan trọng đều phải log.

---

## THỨ TỰ ƯU TIÊN

```
Phase 0 (Scaffolding)     → 1-2 sessions
Phase 1 (Kernel+Reflect)  → 2-3 sessions
Phase 2 (LLM+Modify)      → 2-3 sessions
Phase 3 (Memory+Safety)   → 3-4 sessions
Phase 4 (Improve+Heart)   → 2-3 sessions
Phase 5 (CLI)             → 1-2 sessions
Phase 6 (Integration)     → 2-3 sessions
```

**Milestone quan trọng:** Sau Phase 2, agent phải có thể:
1. Boot lên
2. Đọc code của chính nó
3. Gọi Claude API phân tích code
4. Tự đề xuất và apply modification an toàn

Đây là MVP - đủ để demo "self-modifying AI".

---

## THAM KHẢO TỪ OPENCLAW

Những pattern cụ thể cần implement (đã mark trong từng phase):

1. **Provider Plugin System** (Phase 2) - Multi-LLM support qua protocol
2. **Lane Queue** (Phase 3+) - Concurrency control cho multi-agent
3. **File-First Memory** (Phase 3) - MEMORY.edn + daily logs
4. **Context Compaction** (Phase 3) - Flush before compact
5. **Capability-Based Security** (Phase 3) - Permission per component
6. **Input Sanitization** (Phase 3) - Trust boundaries
7. **Autonomous Invocation** (Phase 4) - Heartbeat with standing instructions
8. **JSONL Audit Trail** (Phase 6) - Structured logging
9. **Cooldown/Failover** (Phase 2) - Provider health management
10. **Dynamic Skill Loading** (Future) - On-demand skill registration

**Điểm khác biệt AOS vs OpenClaw:**
- AOS có **self-modification** - OpenClaw không có
- AOS dùng **Clojure/homoiconicity** - OpenClaw dùng TypeScript
- AOS là **OS cho agents** - OpenClaw là **personal assistant framework**
- AOS có **code evolution** - OpenClaw chỉ execute tools
