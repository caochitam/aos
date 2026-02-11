# Vấn Đề: AOS Tốn Quá Nhiều Tokens

## 🔴 VẤN ĐỀ

Mỗi lần chat với AOS, hệ thống gửi **CỰC NHIỀU DATA** đến Claude API:

### 1. System Prompt (~800-1000 tokens)

```
You are AOS Agent, Trí tuệ nhân tạo tự sửa đổi.

Your personality traits: [:analytical :curious :helpful]
Communication style: friendly-technical
Risk tolerance: moderate
Goals: ["Help users effectively" "Learn continuously" "Maintain system integrity"]

You NEVER modify: ["agent-os.kernel"]
You require approval for: ["System config changes"]
Maximum autonomy level: supervised

User preferences: {:approval-mode :critical-only, :notification-level :important, :language :vi}

IMPORTANT: Always communicate in Vietnamese (tiếng Việt) by default...

SELF-AWARENESS: You are Agent OS running from /root/aos...

You have access to tools that allow you to read files, edit files, and run bash commands...
```

**~800-1000 tokens chỉ riêng system prompt!**

### 2. Tools Schema (~600-800 tokens)

```json
[
  {
    "name": "read_file",
    "description": "Read the contents of a file...",
    "input_schema": { ... }
  },
  {
    "name": "edit_file",
    "description": "Edit a file by replacing...",
    "input_schema": { ... }
  },
  {
    "name": "bash",
    "description": "Execute a bash command...",
    "input_schema": { ... }
  }
]
```

**~600-800 tokens cho tool definitions!**

### 3. Max Output Tokens

```clojure
:max-tokens 8000  ; Request 8000 tokens output
```

### 4. TỔNG CỘNG

```
System prompt:    ~900 tokens
Tools schema:     ~700 tokens
User message:     ~100 tokens (average)
Max output:      8000 tokens
─────────────────────────────
TOTAL:          ~9700 tokens PER CHAT!
```

## ⚠️ HẬU QUẢ

### Rate Limit của Anthropic:
```
30,000 tokens per minute
```

### Với AOS (9700 tokens/chat):
```
30,000 / 9,700 = ~3 chats per minute
```

**Bạn chỉ chat được 3 lần trong 1 phút!**

Nếu gửi 4-5 messages liên tục → **429 Rate Limit Error**

## 🎯 SO SÁNH VỚI CLAUDE.AI

### Claude.ai (Website)
- System prompt: Rất ngắn (~50-100 tokens)
- No tools: 0 tokens
- Max output: Thường 1000-2000 tokens
- **Total: ~1200 tokens/chat**
- **Can chat: 25+ times/minute**

### AOS (Hiện tại)
- System prompt: RẤT DÀI (~900 tokens)
- Tools: 3 tools (~700 tokens)
- Max output: 8000 tokens
- **Total: ~9700 tokens/chat**
- **Can chat: chỉ 3 times/minute!**

## 📊 Chi Tiết Token Usage

| Component | Tokens | % of Total |
|-----------|--------|------------|
| System Prompt | ~900 | 9% |
| Soul/Identity | ~400 | 4% |
| Tools Schema | ~700 | 7% |
| Vietnamese Instructions | ~100 | 1% |
| Self-awareness | ~100 | 1% |
| Tool Instructions | ~100 | 1% |
| User Message | ~100 | 1% |
| **Max Output** | **8000** | **82%** |
| **TOTAL** | **~9700** | **100%** |

## 🔧 GIẢI PHÁP

### Giải Pháp 1: Giảm System Prompt ⭐⭐⭐⭐⭐

**Trước (VERBOSE):**
```clojure
"Your personality traits: [:analytical :curious :helpful]
Communication style: friendly-technical
Risk tolerance: moderate
Goals: [\"Help users effectively\" \"Learn continuously\"]
You NEVER modify: [\"agent-os.kernel\"]
You require approval for: [\"System config changes\"]
Maximum autonomy level: supervised
User preferences: {:approval-mode :critical-only...}"
```

**Sau (COMPACT):**
```clojure
"You are AOS, an AI agent. Communicate in Vietnamese by default.
You can read/edit files and run commands using tools."
```

**Tiết kiệm: ~700 tokens!**

### Giải Pháp 2: Giảm Max Tokens ⭐⭐⭐⭐

**Trước:**
```clojure
:max-tokens 8000  ; Too much!
```

**Sau:**
```clojure
:max-tokens 2000  ; Enough for chat
```

**Tiết kiệm: 6000 tokens!**

### Giải Pháp 3: Lazy Load Tools ⭐⭐⭐

Chỉ gửi tools khi cần:

**Simple chat → No tools**
```clojure
;; User: "chào bạn"
;; Don't need tools!
:tools []  ; 0 tokens
```

**Complex task → Include tools**
```clojure
;; User: "sửa file gateway.clj"
;; Need tools!
:tools [read_file edit_file bash]  ; ~700 tokens
```

**Tiết kiệm: ~700 tokens cho simple chats!**

### Giải Pháp 4: Optimize Tool Descriptions ⭐⭐

**Trước (VERBOSE):**
```clojure
{:name "read_file"
 :description "Read the contents of a file with line numbers. Use this to examine code before making changes."
 :input_schema {...}}  ; ~200 tokens per tool
```

**Sau (COMPACT):**
```clojure
{:name "read_file"
 :description "Read file contents"
 :input_schema {...}}  ; ~100 tokens per tool
```

**Tiết kiệm: ~300 tokens!**

## 📈 KẾT QUẢ SAU KHI TỐI ƯU

### Before Optimization:
```
System prompt:     900 tokens
Tools:             700 tokens
User message:      100 tokens
Max output:       8000 tokens
─────────────────────────────
TOTAL:           9700 tokens/chat
Max chats/min:      3 chats ❌
```

### After Optimization:
```
System prompt:     200 tokens  (↓700)
Tools:               0 tokens  (lazy load)
User message:      100 tokens
Max output:       2000 tokens  (↓6000)
─────────────────────────────
TOTAL:           2300 tokens/chat
Max chats/min:     13 chats ✅
```

**Tăng gấp 4 lần!**

## 🚀 HÀNH ĐỘNG

### Priority 1: Giảm Max Tokens (NHANH)
```clojure
;; In gateway.clj line 186
:max-tokens 2000  ; Change from 8000 to 2000
```

### Priority 2: Compact System Prompt (QUAN TRỌNG)
```clojure
;; In gateway.clj line 172-179
;; Simplify system prompt, remove verbose soul/identity data
```

### Priority 3: Lazy Load Tools (MEDIUM)
```clojure
;; Detect if user needs tools before including them
(if (needs-tools? message)
  {:tools available-tools}
  {:tools []})
```

## 💡 TẠI SAO CLAUDE.AI KHÔNG BỊ?

**Claude.ai:**
- Minimal system prompt
- No tools (unless Code mode)
- Smart token management
- Optimized for conversation

**AOS (trước khi tối ưu):**
- HUGE system prompt with soul/identity
- ALWAYS send 3 tools
- Max 8000 tokens output
- Not optimized

## 📝 NEXT STEPS

1. ✅ Identify problem (DONE)
2. ⬜ Reduce max-tokens to 2000
3. ⬜ Simplify system prompt
4. ⬜ Implement lazy tool loading
5. ⬜ Test and verify improvements

---

**BOTTOM LINE:**

AOS hiện tại tốn **9700 tokens/chat** → Rate limit sau 3 chats!

Sau tối ưu: **2300 tokens/chat** → 13 chats/minute ✅

**Improvement: 4x better! 🚀**
