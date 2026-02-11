# Agent OS - Self-Modifying AI Architecture

## 🎯 Tổng Quan

Agent OS là một kiến trúc cho phép AI Agent (sử dụng Claude Sonnet) **tự đọc hiểu và chỉnh sửa code của chính nó**. Được xây dựng trên Clojure/Lisp để tận dụng tính chất **homoiconicity** (code = data).

### Tại Sao Chọn Clojure?

1. **Code as Data**: Mọi code đều là S-expressions, dễ dàng parse và manipulate
2. **Immutability**: An toàn hơn khi self-modifying
3. **REPL-Driven**: Feedback loop nhanh cho AI experimentation
4. **Simple Syntax**: AI chỉ cần hiểu 1 quy tắc: `(function arg1 arg2 ...)`

## 🏗️ Kien Truc 12 Layers

*Kien truc mo rong, tich hop cac pattern tu OpenClaw*

```
┌─────────────────────────────────────────────────────────────┐
│  LAYER 12: Admin CLI Gateway                                │
│  - Interactive command-line interface                        │
│  - Command router & dispatcher                              │
│  - Formatted output for humans                              │
│  - No Clojure knowledge required                            │
└─────────────────────────────────────────────────────────────┘
        ▲               ▲                ▲
        │               │                │
   [commands]     [status/output]   [chat/NL]
        │               │                │
        ▼               ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│  LAYER 11: Multi-Agent Orchestration          [OpenClaw]    │
│  - Session isolation per agent                              │
│  - Hub-and-spoke message routing                            │
│  - Agent workspace isolation                                │
│  - Channel-based inter-agent communication                  │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 10: Heartbeat & Proactive Loop         [OpenClaw]    │
│  - Periodic self-assessment (configurable interval)         │
│  - Standing instructions (HEARTBEAT.edn)                    │
│  - Autonomous invocation without user prompt                │
│  - Cron-based scheduled tasks                               │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 9: Identity & Soul Engine              [OpenClaw]    │
│  - SOUL.edn: Agent personality & behavioral framework       │
│  - IDENTITY.edn: Public-facing persona                      │
│  - USER.edn: User context & preferences                     │
│  - Dynamic identity evolution over time                     │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 8: Safety & Constraints                              │
│  - Safety checks before modification                        │
│  - Prevent kernel modification                              │
│  - Dependency validation                                    │
│  - Privilege separation & sandboxing          [OpenClaw]    │
│  - Input sanitization at trust boundary       [OpenClaw]    │
│  - Capability-based tool access control       [OpenClaw]    │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 7: Self-Improvement Loop                             │
│  - Reflection → Identify → Analyze → Propose →              │
│    Validate → Apply → Record                                │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 6: Learning & Persistent Memory                      │
│  - Modification history                                     │
│  - Pattern analysis                                         │
│  - Success/failure tracking                                 │
│  - File-first durable memory (EDN)            [OpenClaw]    │
│  - Daily append-only logs                     [OpenClaw]    │
│  - Hybrid retrieval (vector + FTS)            [OpenClaw]    │
│  - Context compaction with memory flush       [OpenClaw]    │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 5: Modification & Skill Engine                       │
│  - Create modification proposals                            │
│  - Validate new code                                        │
│  - Apply changes safely                                     │
│  - Rollback capability                                      │
│  - Dynamic skill loading (EDN descriptors)    [OpenClaw]    │
│  - Skill registry & discovery                 [OpenClaw]    │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 4: LLM Interface (Claude Sonnet)                     │
│  - API communication                                        │
│  - Prompt engineering                                       │
│  - Response parsing                                         │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 3: Reflection Engine                                 │
│  - Read own code                                            │
│  - Analyze components                                       │
│  - Understand system state                                  │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 2: Architecture Metadata                             │
│  - Component specifications                                 │
│  - Dependency graph                                         │
│  - Capabilities & constraints                               │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────────┐
│  LAYER 1: Core Kernel (IMMUTABLE)                           │
│  - Boot/Shutdown                                            │
│  - Validate modifications                                   │
│  - Apply modifications                                      │
│  - KHÔNG THỂ SỬA ĐỔI                                        │
└─────────────────────────────────────────────────────────────┘
```

### Two Fundamental Abstractions (Inspired by OpenClaw)

Toàn bộ kiến trúc AOS được xây dựng trên 2 abstraction cốt lõi:

1. **Autonomous Invocation** - Agent có thể tự kích hoạt mà không cần user prompt (Heartbeat, Cron, Event-driven)
2. **Persistent State** - Mọi state đều được persist ra file, không phụ thuộc vào context window của LLM

## 📦 Cấu Trúc Dữ Liệu Chính

### 1. Component Specification

```clojure
{:id :memory-manager
 :purpose "Quản lý bộ nhớ của agent"
 :interfaces #{:store :retrieve :search}
 :dependencies #{:vector-db}
 :modifiable? true
 :code '(defn memory-manager [operation data] ...)
 :version 1
 :created-at 1234567890}
```

### 2. Architecture Schema

```clojure
{:system-id ::agent-os
 :version "0.1.0"
 :components {:memory-manager {...}
              :reflection-engine {...}
              :modification-engine {...}}
 :capabilities #{:self-reflection :self-modification :learning}
 :constraints [{:type :safety :rule "Cannot modify kernel"}]}
```

### 3. Modification Proposal

```clojure
{:id "mod-1234567890"
 :component-id :memory-manager
 :old-code '(defn old-fn [x] x)
 :new-code '(defn new-fn [x] (* x 2))
 :reason "Performance improvement"
 :metadata {:type :optimization}
 :timestamp 1234567890
 :status :proposed}
```

### 4. Identity & Soul (Inspired by OpenClaw)

```clojure
;; SOUL.edn - Agent personality & behavioral framework
{:soul-id ::agent-alpha
 :personality {:traits #{:analytical :cautious :creative}
               :communication-style :concise
               :risk-tolerance 0.3}
 :boundaries {:never-modify #{:kernel :safety-engine}
              :require-approval #{:critical-components}
              :max-autonomy-level :medium}
 :goals [:self-improvement :stability :efficiency]
 :evolution-history [{:timestamp 1234567890
                      :change "Increased caution after failed modification"
                      :trigger :failed-modification}]}

;; IDENTITY.edn - Public-facing persona
{:display-name "AOS Agent Alpha"
 :role "Self-Modifying System Agent"
 :capabilities-summary "Code analysis, self-modification, learning"}

;; USER.edn - User context & preferences
{:user-id "owner-1"
 :preferences {:approval-mode :critical-only
               :notification-level :important
               :language :vi}
 :interaction-history-ref "data/user-interactions.edn"}
```

### 5. Persistent Memory Structure (Inspired by OpenClaw)

```clojure
;; File-first memory - files are source of truth
;; data/memory/
;;   MEMORY.edn          - Durable facts, decisions, learned patterns
;;   2026-02-10.edn      - Daily append-only log
;;   2026-02-09.edn      - Yesterday's log (auto-loaded at session start)

;; MEMORY.edn format
{:facts [{:id :f1 :content "Vector caching improves retrieval 3x"
          :source :self-discovery :confidence 0.92 :timestamp 1234567890}]
 :decisions [{:id :d1 :decision "Always validate before apply"
              :reason "3 failed modifications without validation"
              :timestamp 1234567890}]
 :patterns [{:id :p1 :pattern "memory-manager modifications fail 40% of time"
             :action "Increase validation strictness for memory-manager"
             :timestamp 1234567890}]}

;; Daily log format (append-only)
[{:timestamp 1234567890 :type :modification
  :summary "Optimized reflection-engine with caching"
  :result :success :details {...}}
 {:timestamp 1234567891 :type :heartbeat
  :summary "All components healthy, no action needed"
  :result :no-action}]
```

### 6. Skill Descriptor (Inspired by OpenClaw)

```clojure
;; Skills are data (EDN), not compiled code
;; Loaded dynamically, injected into LLM prompt only when relevant
{:skill-id :web-scraper
 :name "Web Scraper"
 :description "Scrape and parse web content"
 :version 1
 :interfaces #{:scrape :parse :extract}
 :dependencies #{:clj-http :enlive}
 :triggers [:when-url-provided :when-web-data-needed]
 :code '(defn web-scraper [url options] ...)
 :permissions #{:network-access :file-write}
 :modifiable? true
 :auto-load? false}  ; Only load when needed (context window optimization)
```

### 7. Heartbeat Configuration

```clojure
;; HEARTBEAT.edn - Standing instructions for proactive behavior
{:interval-ms 1800000  ; 30 minutes
 :checks [:component-health
          :pending-modifications
          :memory-usage
          :error-rate-spike
          :scheduled-tasks]
 :actions {:on-issue-found :notify-and-propose-fix
           :on-all-healthy :silent  ; No user interruption
           :on-scheduled-task :execute}
 :standing-instructions
 ["Check if any component has error rate > 20%"
  "Review pending modification proposals older than 1 hour"
  "Flush important observations to MEMORY.edn before context compaction"
  "Update daily log with current system state summary"]}
```

## 🔄 Workflow Self-Modification

### Bước 1: Reflection (Tự Phân Tích)

```clojure
;; Agent đọc code của chính nó
(def component-code (read-own-code :memory-manager arch))

;; Phân tích cấu trúc
(def analysis (analyze-component :memory-manager arch))
;; => {:id :memory-manager
;;     :structure {:type 'defn, :name 'memory-manager, ...}
;;     :dependencies #{:vector-db}}
```

### Bước 2: Identify Issues (Tìm Vấn Đề)

```clojure
;; Gọi Claude API để phân tích
(def claude-analysis 
  (call-claude-for-analysis 
    api-key 
    arch 
    :memory-manager))

;; Claude trả về:
;; {:analysis "Component performs well but could be optimized"
;;  :suggestions ["Add caching" "Use transducers"]
;;  :confidence 0.85}
```

### Bước 3: Propose Changes (Đề Xuất Thay Đổi)

```clojure
;; Gọi Claude để tạo code mới
(def new-code-proposal
  (call-claude-for-modification
    api-key
    arch
    :memory-manager
    "Add caching layer"))

;; Claude tạo code mới:
;; {:new-code '(defn memory-manager-v2 
;;               [operation data]
;;               (let [cache (atom {})]
;;                 (if-let [cached (@cache [operation data])]
;;                   cached
;;                   (let [result (compute operation data)]
;;                     (swap! cache assoc [operation data] result)
;;                     result))))
;;  :explanation "Added memoization for performance"
;;  :confidence 0.9}
```

### Bước 4: Validate (Kiểm Tra)

```clojure
;; Validate syntax
(def validation (validate-new-code new-code))
;; => {:valid? true, :code '(...)}

;; Safety check
(def safety (safety-check arch modification))
;; => {:safe? true}
```

### Bước 5: Apply (Áp Dụng)

```clojure
;; Apply modification
(def result (apply-modification-safe arch modification))

;; Nếu thành công:
;; {:success? true
;;  :architecture <new-arch>
;;  :rollback-data <old-component>}

;; Architecture mới có version tăng lên:
;; {:id :memory-manager
;;  :code '(defn memory-manager-v2 ...)
;;  :version 2
;;  :previous-version '(defn memory-manager ...)}
```

### Bước 6: Record & Learn (Ghi Nhận & Học)

```clojure
;; Ghi lại modification
(record-modification history modification result)

;; Phân tích patterns
(analyze-modification-patterns history)
;; => {:total-modifications 10
;;     :successful 8
;;     :failed 2
;;     :success-rate 0.8
;;     :frequent-modifications {:memory-manager 3, :reflection-engine 2}}
```

## 🚀 Cach Su Dung

### Setup

```bash
# Clone repository
git clone <repo>

# Install Clojure
# macOS: brew install clojure
# Linux: sudo apt install clojure

# Install dependencies
lein deps
```

### CLI Mode (Recommended for Admin)

```bash
# Start AOS with interactive CLI
lein run

# Or with API key
ANTHROPIC_API_KEY=sk-... lein run
```

```
   ╔═══════════════════════════════════════════╗
   ║     AOS - Agent OS CLI v0.1.0             ║
   ║     Self-Modifying AI Architecture        ║
   ╚═══════════════════════════════════════════╝
   Type 'help' for available commands.

aos> status
  System Status
  ─────────────────────────────────────
  Version:        0.1.0
  Components:     3
  Modifiable:     3
  Modifications:  0
  Success Rate:   N/A
  Heartbeat:      STOPPED
  Soul:           default-agent
  Risk Tolerance: 0.3

aos> components
  Components (3)
  ─────────────────────────────────────
  memory-manager        v1  MODIFIABLE  Quan ly bo nho cua agent
  modification-engine   v1  MODIFIABLE  Thuc hien self-modification
  reflection-engine     v1  MODIFIABLE  Cho phep agent tu phan tich

aos> inspect memory-manager
  Component: memory-manager
  ─────────────────────────────────────
  Purpose:      Quan ly bo nho cua agent
  Version:      1
  Modifiable:   true
  Interfaces:   #{:store :retrieve :search}
  Dependencies: #{:vector-db}
  Code:
    (defn memory-manager [operation data] ...)

aos> soul
  Agent Soul
  ─────────────────────────────────────
  ID:              default-agent
  Traits:          #{:analytical :cautious}
  Style:           :concise
  Risk Tolerance:  0.3
  Goals:           [:self-improvement :stability :efficiency]
  Autonomy Level:  :medium
  Evolution Steps: 0

aos> memory
  Memory Summary
  ─────────────────────────────────────
  Facts:     0
  Decisions: 0
  Patterns:  0

aos> remember Vector caching improves retrieval 3x
  Remembered: Vector caching improves retrieval 3x
  ID: f-1707580800000

aos> heartbeat run-once
  Heartbeat check result:
  Status: :healthy
  Issues: 0

aos> permissions
  Permission Model
  ─────────────────────────────────────
  Components:
    kernel                #{}
    memory-manager        #{:file-read :file-write}
    modification-engine   #{:code-eval :code-write :file-write}
    reflection-engine     #{:code-read :system-inspect}

  Trust Levels:
    user        trusted
    api         semi-trusted
    file        semi-trusted
    web         untrusted

aos> shutdown
```

### CLI Command Reference

| Command | Description | Usage |
|---------|-------------|-------|
| **System** | | |
| `help` | Show available commands | `help [command]` |
| `status` | System status overview | `status` |
| `restart` | Restart Agent OS | `restart` |
| `shutdown` | Shutdown safely | `shutdown` |
| **Components** | | |
| `components` | List all components | `components` |
| `inspect` | Inspect component detail | `inspect <id>` |
| `analyze` | Analyze with Claude | `analyze <id>` |
| **Modifications** | | |
| `modify` | Propose modification | `modify <id> <desc>` |
| `improve` | Run self-improvement | `improve [id]` |
| `history` | Modification history | `history [count]` |
| `rollback` | Rollback to previous | `rollback <id>` |
| **Memory** | | |
| `memory` | View persistent memory | `memory [facts\|decisions\|patterns]` |
| `remember` | Save a fact | `remember <fact>` |
| `log` | View daily log | `log [today\|yesterday\|YYYY-MM-DD]` |
| **Identity** | | |
| `soul` | View agent soul | `soul [traits\|goals\|boundaries]` |
| `identity` | View public identity | `identity` |
| **Proactive** | | |
| `heartbeat` | Manage heartbeat | `heartbeat [status\|start\|stop\|run-once]` |
| **Skills** | | |
| `skills` | Manage skills | `skills [list\|load\|unload] [id]` |
| **Security** | | |
| `permissions` | View permissions | `permissions [component-id]` |
| **Interaction** | | |
| `chat` | Chat with AOS agent | `chat <message>` |

### REPL Mode (For Developers)

```bash
# Start in REPL mode
lein run --repl
```

```clojure
;; Load Agent OS
(load-file "agent-os-architecture.clj")

;; Tao Agent OS moi
(def os (create-agent-os :llm-api-key "your-anthropic-api-key"))

;; Xem system state
(get-system-state (:architecture os))

;; Phan tich mot component
(analyze-component :memory-manager (:architecture os))

;; Run self-improvement cycle
(def improved-os
  (->AgentOS
    (self-improvement-cycle
      (:architecture os)
      (:llm-client os)
      (:history os))
    (:history os)
    (:llm-client os)))
```

### Advanced: Custom Component

```clojure
;; Tạo component mới
(def my-component
  (component-spec
    :task-planner
    "AI task planning và scheduling"
    #{:plan :schedule :optimize}
    #{:memory-manager}
    :modifiable? true
    :code '(defn task-planner
             [tasks]
             (sort-by :priority tasks))))

;; Thêm vào architecture
(def arch-with-planner
  (assoc-in (:architecture os)
            [:components :task-planner]
            my-component))

;; Update Agent OS
(def os-v2 (->AgentOS arch-with-planner (:history os) (:llm-client os)))
```

## 🛡️ Safety Mechanisms

### 1. Kernel Protection
```clojure
;; Kernel KHÔNG THỂ được sửa đổi
(validate-modification os {:component-id :kernel ...})
;; => {:safe? false, :reason "Cannot modify kernel"}
```

### 2. Dependency Validation
```clojure
;; Phải maintain dependency graph
;; Nếu component A depends on B, không được xóa B
```

### 3. Code Validation
```clojure
;; Code mới phải là valid Clojure S-expression
(validate-new-code '(defn valid-fn [x] x))  ;; ✅
(validate-new-code "invalid string")        ;; ❌
```

### 4. Rollback Capability
```clojure
;; Mọi modification đều có thể rollback
(def rolled-back
  (rollback-modification
    (:architecture os)
    :memory-manager))
```

### 5. Privilege Separation & Sandboxing (Inspired by OpenClaw)
```clojure
;; Mỗi skill/component chỉ có quyền được cấp rõ ràng
(def permission-model
  {:web-scraper #{:network-access :file-read}
   :memory-manager #{:file-read :file-write}
   :modification-engine #{:code-eval :file-write}
   :kernel #{}})  ; Kernel không cần permission vì immutable

;; Sandbox execution - isolate untrusted code
(defn execute-sandboxed [code permissions]
  (binding [*ns* (create-ns (gensym "sandbox"))]
    (with-permissions permissions
      (eval code))))
```

### 6. Input Sanitization at Trust Boundary (Learned from OpenClaw)
```clojure
;; OpenClaw's biggest weakness: no prompt injection defense
;; AOS giải quyết bằng cách sanitize tại trust boundary
(defn sanitize-llm-response [response]
  (-> response
      (strip-system-prompt-leaks)
      (validate-code-structure)
      (check-forbidden-operations)
      (limit-scope-to-target-component)))

;; Validate external data before feeding to agent
(defn sanitize-external-input [input source]
  {:pre [(contains? #{:user :api :file :web} source)]}
  (case source
    :user input  ; Trusted
    :api (validate-api-response input)
    :file (validate-file-content input)
    :web (strip-injection-patterns input)))
```

### 7. Capability-Based Tool Access (Inspired by OpenClaw)
```clojure
;; Mỗi agent chỉ access tools được grant
(defn check-capability [agent-id tool-id]
  (let [agent-caps (get-in @system [:agents agent-id :capabilities])
        tool-reqs (get-in @system [:tools tool-id :required-permissions])]
    (clojure.set/subset? tool-reqs agent-caps)))
```

## 🧠 Integration với Claude Sonnet

### Prompt Engineering cho Self-Reflection

```clojure
;; System prompt cho Claude
"Bạn là một AI Agent đang phân tích kiến trúc của chính mình.

SYSTEM STATE:
{:architecture {...}
 :components [:memory-manager :reflection-engine :modification-engine]
 :total-components 3}

ANALYZING COMPONENT: :memory-manager
{:id :memory-manager
 :purpose \"Quản lý bộ nhớ của agent\"
 :structure {:type defn, :name memory-manager, ...}
 :dependencies #{:vector-db}}

Hãy phân tích component này và đề xuất cải tiến nếu cần.
Response format:
{:analysis \"...\",
 :suggestions [...],
 :confidence 0.0-1.0}"
```

### Prompt Engineering cho Code Generation

```clojure
"Bạn đang sửa đổi component: :memory-manager

CURRENT CODE:
(defn memory-manager [operation data]
  (case operation
    :store (store-memory data)
    :retrieve (retrieve-memory data)))

PROPOSED CHANGE:
Add caching layer for frequently accessed data

REASON:
Improve performance for repeated queries

Hãy tạo NEW CODE dưới dạng Clojure S-expression.
Response phải là VALID Clojure code có thể eval.
Format: {:new-code '(...), :explanation \"...\"}"
```

### API Call Example

```clojure
(require '[clj-http.client :as http])
(require '[cheshire.core :as json])

(defn call-claude-api
  [api-key messages]
  (let [response (http/post "https://api.anthropic.com/v1/messages"
                   {:headers {"x-api-key" api-key
                             "anthropic-version" "2023-06-01"
                             "content-type" "application/json"}
                    :body (json/generate-string
                            {:model "claude-sonnet-4-20250514"
                             :max_tokens 4000
                             :messages messages})})]
    (-> response :body (json/parse-string true))))
```

## 📊 Monitoring & Analytics

### Modification Statistics

```clojure
;; Xem statistics
(analyze-modification-patterns history)
;; => {:total-modifications 50
;;     :successful 42
;;     :failed 8
;;     :success-rate 0.84
;;     :frequent-modifications 
;;       {:memory-manager 15
;;        :reflection-engine 12
;;        :modification-engine 10
;;        :task-planner 8}}
```

### Component Health

```clojure
(defn component-health
  [arch component-id]
  (let [component (get-in arch [:components component-id])
        mod-count (count-modifications component-id history)]
    {:version (:version component)
     :last-modified (:modified-at component)
     :total-modifications mod-count
     :stability (/ 1.0 (inc mod-count))}))
```

## 🎯 Use Cases

### 1. Performance Optimization
Agent tự động phát hiện bottlenecks và optimize code

### 2. Bug Fixing
Agent phát hiện lỗi trong runtime và tự fix

### 3. Feature Addition
Agent tự động thêm capabilities mới khi cần

### 4. Architecture Refactoring
Agent cải thiện structure của chính nó theo best practices

## 🔮 Roadmap

### Phase 1.5: OpenClaw-Inspired Foundations (Current)
- [x] File-first persistent memory (MEMORY.edn + daily logs)
- [x] Identity-as-Data (SOUL.edn, IDENTITY.edn, USER.edn)
- [x] Heartbeat & proactive agent loop
- [x] Dynamic skill loading with EDN descriptors
- [x] Context compaction with automatic memory flush
- [x] Enhanced safety: privilege separation, input sanitization
- [x] Capability-based tool access control
- [x] Admin CLI Gateway (interactive command-line interface)

### Phase 2: Advanced Features
- [ ] Multi-agent collaboration with session isolation (OpenClaw pattern)
- [ ] Hub-and-spoke message routing between agents
- [ ] Genetic algorithms for code evolution
- [ ] A/B testing for modifications
- [ ] Automatic benchmark va performance tracking
- [ ] Hybrid memory retrieval (vector search + FTS)

### Phase 3: Distribution
- [ ] Distributed Agent OS cluster
- [ ] Code sharing giua cac agents
- [ ] Collective learning
- [ ] Shared skill registry (like OpenClaw's ClawHub)

### Phase 4: Meta-Learning
- [ ] Agent hoc cach hoc tot hon
- [ ] Meta-optimization cua optimization strategies
- [ ] Emergent behaviors
- [ ] Soul evolution - agent tu phat trien personality qua thoi gian

## 🔒 Security Considerations

1. **Sandboxing**: Chạy modifications trong isolated environment
2. **Rate Limiting**: Giới hạn số modifications per time period
3. **Human Approval**: Critical modifications cần approval
4. **Audit Trail**: Log tất cả modifications
5. **Kill Switch**: Emergency shutdown mechanism

## 📚 References

### Papers & Research
- "Self-Modifying Systems—AI Security"
- Darwin Godel Machine (Sakana AI)
- Claude Code Architecture (Anthropic)
- "Decoding OpenClaw: Two Simple Abstractions" - Laurent Bindschaedler

### Inspiration
- LISP homoiconicity
- Smalltalk self-modifying environments
- Synthesis kernel (Alexia Massalin)
- **OpenClaw** - File-first memory, identity-as-data, heartbeat pattern, skill system
  - Autonomous invocation + persistent state as two fundamental abstractions
  - Security lessons: privilege separation, input sanitization at trust boundary

## 🤝 Contributing

Đóng góp ý tưởng về:
- Safety mechanisms
- Optimization strategies
- Use cases
- Integration với other AI models

## 📝 License

MIT License - Free to use and modify

---

**Agent OS** - Where AI becomes its own architect 🏗️🤖
