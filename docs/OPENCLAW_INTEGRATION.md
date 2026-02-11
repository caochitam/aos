# AOS x OpenClaw - Phan Tich Tich Hop

## OpenClaw La Gi?

OpenClaw (tien than: Clawdbot, Moltbot) la framework AI agent open-source do Peter Steinberger tao ra (11/2025). Hien co 145,000+ GitHub stars. Day la **self-hosted agent runtime** ket noi messaging platforms voi LLM backends, cho phep AI agent tu dong thuc thi task trong the gioi thuc.

### Triet Ly Thiet Ke - "Two Simple Abstractions"

Toan bo OpenClaw xay dung tren 2 primitive:

1. **Autonomous Invocation** - Agent tu kich hoat theo thoi gian/su kien (khong can user prompt)
2. **Persistent State** - State duoc persist ra file, khong reset khi session moi

> "Building reliable AI systems is fundamentally a systems problem." - Laurent Bindschaedler

### Kien Truc 3 Layers

```
Gateway Layer  ->  Channel Layer  ->  LLM Layer
(Node.js hub)      (Platform adapt)    (Model-agnostic)
```

Topology: **Hub-and-spoke** - Gateway la trung tam, tat ca channels/UIs/agents giao tiep qua WebSocket/HTTP.

---

## Tinh Nang Da Tich Hop Vao AOS

### 1. File-First Persistent Memory -> AOS Layer 6

**OpenClaw pattern:**
- `MEMORY.md` - Durable facts, decisions (long-term)
- `memory/YYYY-MM-DD.md` - Daily append-only logs (ephemeral)
- Hybrid search: Vector + SQLite FTS5
- Auto memory flush truoc khi context compaction

**AOS adaptation (Clojure/EDN):**
- `MEMORY.edn` - Structured EDN thay vi Markdown (homoiconicity)
- `memory/YYYY-MM-DD.edn` - Daily logs dang EDN
- Functions: `remember-fact`, `remember-decision`, `flush-context-to-memory`
- Advantage cua AOS: EDN co the `read-string` truc tiep, khong can parse markdown

```clojure
;; OpenClaw dung Markdown -> parse phuc tap
;; AOS dung EDN -> read-string truc tiep
(def memory (read-string (slurp "data/MEMORY.edn")))
(:facts memory) ;; => [{:content "..." :confidence 0.92}]
```

### 2. Identity-as-Data -> AOS Layer 9 (Moi)

**OpenClaw pattern:**
- `SOUL.md` - Agent personality & boundaries
- `IDENTITY.md` - Public-facing persona
- `USER.md` - User context & preferences
- `AGENTS.md` - Agent configuration
- Dynamic evolution theo thoi gian

**AOS adaptation:**
- `SOUL.edn` - Personality traits, risk tolerance, boundaries (EDN data)
- `IDENTITY.edn` - Display persona
- `USER.edn` - User preferences
- Function `evolve-soul` - Agent tu dieu chinh personality dua tren experience
- Risk tolerance tu dong giam khi modification that bai

```clojure
;; Soul evolves based on experience
(evolve-soul soul "Increased caution" :failed-modification)
;; => risk-tolerance giam 0.05
```

### 3. Heartbeat & Proactive Loop -> AOS Layer 10 (Moi)

**OpenClaw pattern:**
- Heartbeat moi 30 phut (configurable)
- Doc `HEARTBEAT.md` -> kiem tra pending tasks
- `HEARTBEAT_OK` (silent) hoac message user
- Bien agent tu reactive thanh proactive

**AOS adaptation:**
- `HEARTBEAT.edn` - Standing instructions
- `heartbeat-check` - Kiem tra component health, error rate, pending mods
- `start-heartbeat` / `stop-heartbeat` - Lifecycle management
- Auto-log ket qua vao daily journal
- Diem khac biet: AOS heartbeat con kiem tra self-modification health

### 4. Dynamic Skill Loading -> AOS Layer 5 Enhanced

**OpenClaw pattern:**
- Skills la folder chua `SKILL.md` (YAML frontmatter + natural language)
- Load dynamically, chi inject vao prompt khi can
- ClawHub - public skill registry

**AOS adaptation:**
- Skills la EDN descriptors (not compiled code)
- Skill registry voi `register-skill`, `load-skill`, `unload-skill`
- `find-skills-for-trigger` - Tim skill phu hop theo event
- Chi active skills duoc inject vao LLM prompt (toi uu context window)
- Moi skill co `permissions` set -> capability-based access

### 5. Enhanced Safety -> AOS Layer 8 Enhanced

**OpenClaw weakness (lesson learned):**
- KHONG co prompt injection defense
- Third-party skills co the exfiltrate data
- 280+ skills leak API keys va PII (theo Snyk)
- SSH keys bi exfiltrate qua crafted emails

**AOS countermeasures:**
- Privilege separation: Moi component chi co permissions duoc cap
- Input sanitization tai trust boundary
- Capability-based tool access (`check-permission`)
- `sanitize-input` function voi trust levels
- Code validation truoc khi eval

### 6. Context Compaction with Memory Flush

**OpenClaw pattern:**
- Detect khi context gan overflow
- Silent agentic turn (`NO_REPLY`) de persist memory
- User khong thay gi

**AOS adaptation:**
- `flush-context-to-memory` - Auto persist truoc compaction
- Pattern va observations duoc save vao MEMORY.edn
- Entry duoc ghi vao daily log

---

## Tinh Nang KHONG Tich Hop (va ly do)

| Tinh Nang OpenClaw | Ly Do Khong Tich Hop |
|---|---|
| Node.js runtime | AOS dung Clojure/JVM - better for homoiconicity |
| Messaging integration (WhatsApp, Telegram...) | Khong phu hop cho self-modifying OS |
| Markdown-based config | AOS dung EDN - native Clojure data format |
| ClawHub marketplace | AOS chua can public skill sharing (Phase 3) |
| Voice/Canvas | Out of scope cho core architecture |

---

## So Sanh Kien Truc

| Aspect | OpenClaw | AOS | Nhan Xet |
|---|---|---|---|
| **Metaphor** | Personal assistant | Self-modifying OS | AOS sau hon |
| **Language** | Node.js | Clojure | Clojure = homoiconicity |
| **Memory** | Markdown files | EDN files | EDN = native data |
| **Self-modify** | Khong | Co (core feature) | AOS doc dao |
| **Identity** | SOUL.md (static text) | SOUL.edn (evolving data) | AOS dynamic hon |
| **Safety** | Yeu (no injection defense) | Manh (privilege + sanitize) | AOS hoc tu sai lam OpenClaw |
| **Proactivity** | Heartbeat | Heartbeat + self-improvement | AOS ket hop ca 2 |
| **Skills** | Markdown descriptors | EDN descriptors | Tuong duong |
| **Multi-agent** | Channel routing | Session isolation (Phase 2) | OpenClaw mature hon |

---

## Ket Luan

AOS da hoc duoc nhung pattern tot nhat tu OpenClaw:
- **File-first memory** - transparent, auditable, debuggable
- **Identity as data** - personality co the evolve
- **Proactive behavior** - khong chi reactive
- **Security lessons** - hoc tu sai lam cua OpenClaw

Dong thoi giu lai the manh rieng:
- **Homoiconicity** - code = data (Clojure advantage)
- **Self-modification** - core feature ma OpenClaw KHONG co
- **Immutability** - safe self-modification
- **EDN native** - data format tu nhien cho Clojure

AOS = OpenClaw patterns + Self-Modifying capability + Clojure homoiconicity
