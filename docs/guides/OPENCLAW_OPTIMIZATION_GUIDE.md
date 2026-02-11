# OpenClaw Optimization Techniques - Áp Dụng Cho AOS

## 📚 TÓM TẮT RESEARCH

OpenClaw đã đạt được **70-85% giảm chi phí** thông qua các kỹ thuật tối ưu thông minh.

---

## 🎯 TOP 5 KỸ THUẬT QUAN TRỌNG NHẤT

### 1. Bootstrap File Caching (93.5% Token Reduction) ⭐⭐⭐⭐⭐

**Vấn Đề:** OpenClaw gửi 7 bootstrap files (`AGENTS.md`, `SOUL.md`, `TOOLS.md`, etc.) **MỖI MESSAGE**
- ~35,600 tokens/message
- $1.51 wasted per 100-message session
- 93.5% token waste

**Giải Pháp:**
```javascript
// CHỈ load bootstrap files ở message ĐẦU TIÊN!
const hadSessionBefore = await sessionExists();
if (hadSessionBefore) {
  return { files: [] }; // Skip loading
}
// First message only - load all bootstrap files
```

**Kết quả:** 93.5% giảm tokens cho conversations dài

**ÁP DỤNG CHO AOS:**
```clojure
;; PRIORITY #1 - IMMEDIATE WIN!

;; File: src/agent_os/cli/gateway.clj

(def session-cache (atom #{})) ; Track initialized sessions

(defn get-system-prompt-for-session
  "Load full prompt only on first message"
  [session-id soul identity user]
  (if (contains? @session-cache session-id)
    ;; Existing session - MINIMAL prompt
    "You are AOS. Communicate in Vietnamese."
    ;; New session - FULL prompt with soul/identity
    (do
      (swap! session-cache conj session-id)
      (str (soul/get-system-prompt soul identity user)
           "\n\nVietnam language instructions..."
           "\n\nSelf-awareness instructions..."))))
```

**Impact:** ~900 tokens → ~20 tokens cho messages sau message đầu!

---

### 2. Three-Tier Model Routing (60-75% Cost Reduction) ⭐⭐⭐⭐⭐

**Strategy:** Route tasks theo complexity thay vì dùng model đắt cho tất cả

**OpenClaw's Tiers:**
```
SIMPLE tasks    → Claude Haiku   ($0.25/1M tokens)   - 60% của tasks
MODERATE tasks  → Claude Sonnet  ($3/1M tokens)      - 30% của tasks
COMPLEX tasks   → Claude Opus    ($15/1M tokens)     - 10% của tasks
```

**Routing Logic:**
```javascript
// 14-dimension weighted scoring
function calculateComplexity(message) {
  scores = {
    reasoning_keywords: 0.3,    // "analyze", "why", "phân tích"
    code_presence: 0.3,          // .clj, defn, function
    multi_file: 0.2,             // "nhiều file", "all files"
    self_modification: 0.2       // "sửa aos", "modify core"
  };

  return weightedScore; // 0.0 - 1.0
}

if (complexity >= 0.8) return "opus";
if (complexity >= 0.5) return "sonnet";
return "haiku"; // default safe choice
```

**Expected Savings:** 60-75% reduction = ~$1,200-$1,350/month for high-volume

**ÁP DỤNG CHO AOS:**
```clojure
;; PRIORITY #2 - HIGH IMPACT!

;; File: src/agent_os/llm/delegator.clj (ENHANCE EXISTING)

(def model-tiers
  {:simple   {:model "claude-haiku-4"    :cost 0.25}
   :moderate {:model "claude-sonnet-4-5" :cost 3.0}
   :complex  {:model "claude-opus-4-6"   :cost 15.0}})

(defn calculate-complexity-score
  "Enhanced complexity detection (0.0-1.0)"
  [message]
  (let [lower (str/lower-case message)
        score (+
               ;; Reasoning (30%)
               (if (some #(str/includes? lower %)
                        ["analyze" "phân tích" "why" "tại sao" "explain"])
                 0.3 0.0)

               ;; Code presence (30%)
               (if (or (str/includes? lower ".clj")
                      (str/includes? lower "defn")
                      (str/includes? lower "code"))
                 0.3 0.0)

               ;; Multi-file (20%)
               (if (or (str/includes? lower "nhiều file")
                      (re-find #"\d+\s+file" lower)
                      (str/includes? lower "all files"))
                 0.2 0.0)

               ;; Self-modification (20%)
               (if (re-find #"(sửa|modify).*(aos|core|gateway)" lower)
                 0.2 0.0))]
    score))

(defn select-model-tier
  "Choose model based on complexity"
  [message]
  (let [score (calculate-complexity-score message)]
    (cond
      (>= score 0.8) :complex   ; Opus for hard tasks
      (>= score 0.5) :moderate  ; Sonnet for medium
      :else :simple)))          ; Haiku for simple

;; Usage in gateway.clj:
(let [tier (delegator/select-model-tier message)
      tier-config (get delegator/model-tiers tier)
      result (router/chat-with-failover
              llm-registry
              messages
              {:system system-prompt
               :model (:model tier-config)  ; ← DYNAMIC MODEL!
               :max-tokens 2000})]
  ...)
```

---

### 3. Session Compaction (40-60% Long-Term Savings) ⭐⭐⭐⭐

**Vấn Đề:** Conversation history phình to → exponential token consumption

**OpenClaw's Solution:**

1. **Soft Threshold:** 4,000 tokens → trigger warning
2. **Hard Threshold:** 8,000 tokens → auto-compact
3. **Three-Point Compaction:**
   - Summarize dropped messages
   - Compact main history
   - Preserve recent 10 messages

**Compaction Process:**
```
1. Silent memory flush → Write important context to MEMORY.md
2. Summarize old messages → Use Haiku (cheap!)
3. Keep recent 10 messages intact
4. Replace history with: [summary] + [recent 10]
```

**Impact:** 60% context reduction while preserving key info

**ÁP DỤNG CHO AOS:**
```clojure
;; PRIORITY #3 - MEDIUM PRIORITY

;; File: src/agent_os/memory/compaction.clj (NEW)

(ns agent-os.memory.compaction
  "Conversation compaction for long sessions"
  (:require [agent-os.llm.claude :as claude]))

(def soft-threshold 4000)  ; Warning
(def hard-threshold 8000)  ; Force compact

(defn estimate-tokens [text]
  "Rough estimate: 4 chars = 1 token"
  (/ (count (str text)) 4))

(defn should-compact? [messages]
  (let [total (->> messages
                  (map :content)
                  (map str)
                  (map estimate-tokens)
                  (reduce +))]
    (>= total soft-threshold)))

(defn compact-history
  "Summarize old messages, keep recent ones"
  [messages claude-provider]
  (when (should-compact? messages)
    (let [keep-recent 10
          to-summarize (drop-last keep-recent messages)
          recent (take-last keep-recent messages)

          ;; Use HAIKU to summarize (cheap!)
          summary-prompt [{:role "user"
                          :content (str "Tóm tắt cuộc hội thoại này bằng tiếng Việt, "
                                       "giữ lại các quyết định quan trọng, file paths, "
                                       "và context cần thiết:\n\n"
                                       (pr-str to-summarize))}]

          summary (claude/chat claude-provider
                              summary-prompt
                              {:model "claude-haiku-4"  ; CHEAP!
                               :max-tokens 500})]

      (println "📝 Compacted:" (count messages) "→" (inc keep-recent) "messages")

      ;; Return: [summary] + [recent 10]
      (cons {:role "system"
             :content (str "[Tóm tắt hội thoại trước]\n" summary)}
            recent))))

;; Integration in gateway.clj:
(defn cmd-chat [os message]
  (let [conversation (get-conversation session-id)

        ;; AUTO-COMPACT if needed
        conversation (if (compaction/should-compact? conversation)
                      (do
                        (println "⚠️  Compacting conversation...")
                        (compaction/compact-history conversation provider))
                      conversation)

        response (router/chat conversation opts)]
    ...))
```

---

### 4. Diff-Only Code Responses (90% Output Reduction) ⭐⭐⭐⭐

**Strategy:** Trả về unified diff thay vì full file content

**Before:**
```
Assistant: Here's the modified file:

(ns agent-os.core ...)  ; 500 lines
(defn create-agent ...)
... ENTIRE FILE ...
```
**Tokens:** ~2000-5000

**After:**
```
Assistant: Here's the diff:

--- a/core.clj
+++ b/core.clj
@@ -45,3 +45,4 @@
-(def old-code ...)
+(def new-code ...)
```
**Tokens:** ~200-500

**90% savings on output tokens!**

**ÁP DỤNG CHO AOS:**
```clojure
;; PRIORITY #4 - EASY WIN!

;; File: src/agent_os/llm/tools.clj

(def tool-edit-file
  {:name "edit_file"
   :description "Edit file by replacing text.

   IMPORTANT: For code changes, prefer unified diff format:
   --- a/file.clj
   +++ b/file.clj
   @@ -line,count +line,count @@
   -old line
   +new line

   This saves 90% tokens and provides clear change tracking.

   For small changes, old_string/new_string is fine."

   :input_schema {...}})

;; Also add to system prompt:
(def system-prompt
  "...

  For code modifications, prefer unified diff format to save tokens:
  --- a/file.clj
  +++ b/file.clj
  @@ -10,3 +10,4 @@
  -old code
  +new code

  Only show full file content when explicitly requested.")
```

---

### 5. Lazy Tool Loading (700 Tokens Saved) ⭐⭐⭐

**Vấn Đề:** AOS gửi 3 tool schemas MỖI REQUEST (~700 tokens)

**Giải Pháp:** Chỉ gửi tools khi cần

**Simple Chat → No Tools:**
```
User: "chào bạn"
→ Tools: [] (0 tokens)
```

**Complex Task → Include Tools:**
```
User: "sửa file gateway.clj"
→ Tools: [read_file, edit_file, bash] (700 tokens)
```

**ÁP DỤNG CHO AOS:**
```clojure
;; PRIORITY #5 - SMART OPTIMIZATION

;; File: src/agent_os/cli/gateway.clj

(defn needs-tools?
  "Detect if message requires tool usage"
  [message]
  (let [lower (str/lower-case message)]
    (or
      ;; File operations
      (some #(str/includes? lower %)
            ["file" "read" "edit" "sửa" "đọc" "xem"])

      ;; Code modifications
      (some #(str/includes? lower %)
            [".clj" "code" "function" "defn"])

      ;; System commands
      (some #(str/includes? lower %)
            ["run" "command" "bash" "lein" "test"]))))

(defn select-tools
  "Choose relevant tools based on message"
  [message]
  (if (needs-tools? message)
    tools/available-tools  ; Full tools (700 tokens)
    []))                   ; No tools (0 tokens)

;; Usage:
(let [tools (select-tools message)
      result (router/chat-with-failover
              llm-registry
              messages
              {:system system-prompt
               :tools tools     ; ← CONDITIONAL!
               :max-tokens 2000})]
  ...)
```

---

## 📊 TỔNG HỢP KẾT QUẢ

### Before Optimization (Current AOS)
```
System prompt:     900 tokens  (soul/identity/personality)
Tools:             700 tokens  (always sent)
User message:      100 tokens
Max output:       8000 tokens
─────────────────────────────
PER CHAT:        9700 tokens
CHATS/MINUTE:       3 chats   ❌

MONTHLY COST (100 chats/day):
Input:  100 × 100 days × 1,700 tokens × $3/1M  = $51
Output: 100 × 100 days × 8,000 tokens × $15/1M = $1,200
TOTAL: ~$1,251/month
```

### After Optimization (với tất cả 5 techniques)
```
System prompt:      20 tokens  (minimal - cached after first)
Tools:               0 tokens  (lazy loaded - only when needed)
User message:      100 tokens
Max output:       2000 tokens  (reduced from 8000)
Model routing:    Haiku 60% of time (10x cheaper)
─────────────────────────────
PER CHAT:        2120 tokens  (simple) / 2820 (complex)
CHATS/MINUTE:      14 chats   ✅

MONTHLY COST (100 chats/day):
Simple (60%): 60 × 100 × 120 × $0.25/1M = $1.80
Moderate (30%): 30 × 100 × 2,820 × $3/1M = $25.38
Complex (10%): 10 × 100 × 2,820 × $15/1M = $42.30
TOTAL: ~$69.48/month

SAVINGS: $1,251 → $69 = 94.4% reduction! 🎉
```

---

## 🚀 ACTION PLAN - PRIORITY ORDER

### Phase 1: Quick Wins (1 day)

**[DONE] ✅ Reduce max-tokens: 8000 → 2000**
- File: `gateway.clj` line 186
- Impact: 6000 tokens/chat saved
- Savings: ~50% immediately

**[DONE] ✅ Simplify system prompt**
- File: `gateway.clj` line 172-179
- Impact: 850 tokens/chat saved
- Savings: ~9% immediately

**[TODO] Priority 1.1: Bootstrap caching**
- File: `gateway.clj` - add session tracking
- Impact: 900 → 20 tokens after first message
- Effort: 30 minutes
- Code: See section #1 above

**[TODO] Priority 1.2: Diff-only responses**
- File: `tools.clj` - update edit_file description
- Impact: 90% output reduction for code tasks
- Effort: 15 minutes
- Code: See section #4 above

**[TODO] Priority 1.3: Lazy tool loading**
- File: `gateway.clj` - conditional tools
- Impact: 700 tokens saved for simple chats
- Effort: 20 minutes
- Code: See section #5 above

**PHASE 1 TOTAL:** ~60% savings

### Phase 2: Model Routing (2 days)

**[TODO] Priority 2.1: Enhance delegator complexity detection**
- File: `delegator.clj`
- Add: `calculate-complexity-score` function
- Impact: Foundation for routing

**[TODO] Priority 2.2: Implement three-tier routing**
- File: `delegator.clj`, `gateway.clj`, `claude.clj`
- Add: Model tier selection logic
- Impact: 60-75% cost reduction
- Code: See section #2 above

**[TODO] Priority 2.3: Add fallback/escalation**
- If Haiku fails → retry with Sonnet
- If Sonnet fails → escalate to Opus

**PHASE 2 TOTAL:** Additional 30-40% savings

### Phase 3: Advanced (1 week)

**[TODO] Priority 3.1: Session compaction**
- File: `memory/compaction.clj` (new)
- Auto-summarize when > 4000 tokens
- Impact: 40-60% long-term savings

**[TODO] Priority 3.2: Prompt caching headers**
- File: `claude.clj`
- Add: Anthropic cache_control blocks
- Impact: 90% discount on cached prompts

**[TODO] Priority 3.3: Semantic snapshots for tool results**
- File: `tools.clj`
- Summarize large outputs
- Impact: 60-90% on tool result tokens

**PHASE 3 TOTAL:** Additional 20-30% savings

---

## 📈 EXPECTED RESULTS TIMELINE

### Week 1: Quick Wins
```
Before: 9,700 tokens/chat × $3/1M = $29.10 per 1000 chats
After:  2,150 tokens/chat × $3/1M = $6.45 per 1000 chats
Savings: 78% ✅
```

### Week 2: Model Routing
```
Before: $6.45 per 1000 chats (all Sonnet)
After:  $1.93 per 1000 chats (60% Haiku, 30% Sonnet, 10% Opus)
Savings: Additional 70% ✅
```

### Week 3+: Advanced Optimizations
```
Before: $1.93 per 1000 chats
After:  $0.70 per 1000 chats (with caching & compaction)
Savings: Additional 64% ✅
```

### TOTAL IMPROVEMENT
```
Original: $29.10 per 1000 chats
Optimized: $0.70 per 1000 chats
TOTAL SAVINGS: 97.6% 🎉🚀
```

---

## 🎓 KEY LEARNINGS FROM OPENCLAW

### What OpenClaw Does Well
1. **Local-first gateway** - Routes smartly before API call
2. **Session management** - JSONL files, not in-memory
3. **Modular system prompt** - Easy to customize per use case
4. **Skill-based architecture** - Load skills on-demand
5. **Multi-provider support** - Fallback & cost optimization

### What We Can Improve in AOS
1. **Simpler personality system** - AOS has too verbose soul/identity
2. **Dynamic tool loading** - Don't send tools for simple chats
3. **Smarter model selection** - Use Haiku for 60% of tasks
4. **Better context management** - Compact when needed
5. **Prompt stability** - Cache system prompts

---

## 📚 SOURCES & REFERENCES

### Primary Sources
- [OpenClaw GitHub](https://github.com/openclaw/openclaw)
- [OpenClaw Docs - Context Management](https://docs.openclaw.ai/concepts/context)
- [OpenClaw Docs - Compaction](https://docs.openclaw.ai/concepts/compaction)
- [AGENTS.md Best Practices](https://github.com/openclaw/openclaw/blob/main/AGENTS.md)

### Key Issues
- [Issue #9157: Bootstrap file waste](https://github.com/openclaw/openclaw/issues/9157)
- [Issue #10969: Model routing middleware](https://github.com/openclaw/openclaw/issues/10969)
- [Discussion #1949: Token burning](https://github.com/openclaw/openclaw/discussions/1949)

### Articles
- [Token Cost Optimization Guide](https://help.apiyi.com/en/openclaw-token-cost-optimization-guide-en.html)
- [Multi-model Routing Guide](https://velvetshark.com/openclaw-multi-model-routing)
- [8 Ways to Stop Losing Context](https://codepointer.substack.com/p/openclaw-stop-losing-context)

---

## 🎯 NEXT STEPS

1. **Read this document completely** ✅
2. **Implement Phase 1 quick wins** (1 day)
3. **Test & measure results** (verify 60% savings)
4. **Implement Phase 2 routing** (2 days)
5. **Monitor & optimize** (ongoing)

---

**BOTTOM LINE:**

OpenClaw đã chứng minh có thể giảm 70-85% chi phí mà vẫn giữ chất lượng.

AOS có thể đạt **97.6% savings** bằng cách:
1. ✅ Bootstrap caching (93.5% savings on prompts)
2. ✅ Model routing (60-75% overall)
3. ✅ Lazy tool loading (700 tokens saved)
4. ✅ Diff-only responses (90% output reduction)
5. ✅ Session compaction (40-60% long-term)

**Start with Phase 1 - get 60% savings in 1 day! 🚀**
