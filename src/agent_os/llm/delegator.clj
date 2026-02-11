(ns agent-os.llm.delegator
  "Smart task delegation - simple tasks use AOS tools, complex tasks use Claude Code"
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [agent-os.llm.router :as router]
            [agent-os.protocols :refer [IProgressReporter report-start report-progress report-complete]]
            [taoensso.timbre :as log])
  (:import [java.lang ProcessBuilder Process]
           [java.io BufferedReader InputStreamReader]
           [java.util.concurrent TimeUnit]))

;; ============================================================================
;; OPENCLAW OPTIMIZATION: Three-Tier Model Routing
;; ============================================================================

(def model-tiers
  "Model tiers with cost information (per 1M tokens)"
  {:simple   {:model "claude-haiku-4-5-20251001"    :cost 0.25  :max-tokens 2000}
   :moderate {:model "claude-sonnet-4-5-20250929"   :cost 3.0   :max-tokens 4000}
   :complex  {:model "claude-opus-4-6"              :cost 15.0  :max-tokens 8000}})

;; ============================================================================
;; DEPRECATED: HARD-CODED TASK COMPLEXITY DETECTION
;; ============================================================================
;; These functions are NO LONGER USED. LLM-based classification is superior.
;; Kept for reference and potential fallback if LLM fails.
;;
;; Why LLM is better:
;; 1. Understands Vietnamese context ("bỏ" vs "thêm", "tắt" vs "bật")
;; 2. No keyword maintenance needed
;; 3. More accurate with minimal cost ($0.000025/request)
;; 4. Self-improving as models get better

;; DEPRECATED: Hard-coded keywords (reference only - kept in comment for history)
(comment
  (def complex-task-keywords
    #{"sua" "sửa" "modify" "refactor" "improve" "toi uu" "tối ưu" "debug" "fix"
      "them" "thêm" "add" "xoa" "xóa" "remove" "bo" "bỏ" "tat" "tắt" "disable"})

  (def simple-task-keywords
    #{"đọc" "read" "xem" "show" "list" "liệt kê" "hiển thị" "display"})

  (defn has-action-keyword? [lower-msg keywords]
    "DEPRECATED - use LLM classification instead"
    nil)

  (defn calculate-complexity-score [message]
    "DEPRECATED - use classify-task-with-llm instead"
    0.5))

;; ============================================================================
;; CLAUDE CODE DELEGATION
;; ============================================================================

(defn format-delegation-message
  "Format message to show user that task is being delegated"
  [message]
  (str "🔄 Đây là tác vụ phức tạp. Đang chuyển cho Claude Code xử lý...\n"
       "Yêu cầu: " message "\n"))

(defn format-completion-message
  "Format Claude Code output for display"
  [result]
  (if (:success? result)
    (str "✅ Claude Code đã hoàn thành!\n\n"
         (:output result))
    (str "❌ Claude Code gặp lỗi:\n"
         (:error result))))

(defn call-claude-code
  "Delegate complex task to Claude Code CLI with progress reporting

   Parameters:
   - message: Task description for Claude Code
   - working-dir: Working directory path
   - reporter: IProgressReporter implementation for progress updates

   Returns {:success? bool :output string :error string}"
  [message working-dir reporter]
  (log/debug "Delegating to Claude Code" {:message message :dir working-dir})

  ;; Report start
  (report-start reporter (format-delegation-message message))

  (try
    (let [;; Build process for Claude Code
          ;; NOTE: --print mode for non-interactive execution
          ;; NOTE: bypassPermissions blocked for root, so use acceptEdits instead
          ;; acceptEdits auto-approves file edits but may prompt for dangerous operations
          pb (ProcessBuilder. ["claude"
                               "--print"
                               "--permission-mode" "acceptEdits"
                               message])
          _ (.directory pb (io/file working-dir))
          process (.start pb)

          ;; Capture output
          output-buffer (StringBuilder.)
          reader (BufferedReader. (InputStreamReader. (.getInputStream process)))
          error-reader (BufferedReader. (InputStreamReader. (.getErrorStream process)))

          ;; Start time for progress tracking
          start-time (System/currentTimeMillis)

          ;; Background thread to monitor progress
          monitor-future
          (future
            (try
              (loop []
                (when (.isAlive process)
                  (Thread/sleep 5000)  ; Check every 5 seconds
                  (let [elapsed-seconds (quot (- (System/currentTimeMillis) start-time) 1000)]
                    (report-progress reporter
                                     (format "Claude Code đang xử lý... (%ds)" elapsed-seconds)))
                  (recur)))
              (catch InterruptedException _
                nil)))]

      ;; Read output line by line
      (loop []
        (when-let [line (.readLine reader)]
          (.append output-buffer line)
          (.append output-buffer "\n")
          (recur)))

      ;; Wait for process completion (max 5 minutes)
      (let [completed? (.waitFor process 300 TimeUnit/SECONDS)
            exit-code (.exitValue process)
            output (.toString output-buffer)
            success? (and completed? (zero? exit-code))]

        ;; Cancel monitor
        (future-cancel monitor-future)

        ;; Report completion
        (let [result (if success?
                       (do
                         (log/debug "Claude Code completed successfully")
                         {:success? true
                          :output output})
                       (do
                         (log/error "Claude Code failed" {:exit exit-code})
                         {:success? false
                          :error output}))]
          (report-complete reporter (format-completion-message result))
          result)))

    (catch Exception e
      (log/error e "Failed to call Claude Code")
      (let [error-msg (str "Không thể gọi Claude Code: " (.getMessage e)
                          "\nĐảm bảo Claude Code CLI đã được cài đặt.")
            result {:success? false
                    :error error-msg}]
        (report-complete reporter (format-completion-message result))
        result))))

;; ============================================================================
;; LLM-BASED CLASSIFICATION (Meta-Cognition)
;; ============================================================================

(defn classify-task-with-llm
  "Use Haiku to classify task complexity - LLM understands context better than hard-coded rules!

  Cost analysis:
  - Classification call: ~100 tokens × $0.25/1M = $0.000025 per request
  - Wrong model choice: Could waste $0.01-0.50 if wrong model used
  - ROI: 400-20,000x return on investment!

  Returns: :simple | :moderate | :complex"
  [message llm-registry]

  (try
    (let [classification-prompt
          [{:role "user"
            :content (str "Bạn là trợ lý phân loại độ phức tạp của task. Phân loại task sau:\n\n"
                         "=== QUY TẮC PHÂN LOẠI ===\n\n"

                         "SIMPLE (Haiku - $0.25/1M tokens):\n"
                         "- Đọc/hiển thị file, code, hoặc info\n"
                         "- Chạy lệnh đơn giản (ls, git status, test)\n"
                         "- Trả lời câu hỏi về code/architecture\n"
                         "- Giải thích logic, function\n"
                         "Ví dụ: \"xem file README\", \"chạy test\", \"giải thích hàm X\"\n\n"

                         "MODERATE (Sonnet - $3/1M tokens):\n"
                         "- Sửa code đơn giản (1-2 files, logic rõ ràng)\n"
                         "- Refactor nhỏ, cleanup code\n"
                         "- Debug với context sẵn có\n"
                         "- Viết test cases đơn giản\n"
                         "Ví dụ: \"sửa typo trong code\", \"thêm validation\", \"cleanup imports\"\n\n"

                         "COMPLEX (Claude Code/Opus - $15/1M tokens - DELEGATE!):\n"
                         "- Sửa/thêm/bỏ/tắt features (cần hiểu codebase sâu)\n"
                         "- Thay đổi behavior của system\n"
                         "- Multi-file refactoring\n"
                         "- Debug phức tạp (cần trace qua nhiều files)\n"
                         "- Implement feature mới\n"
                         "Ví dụ: \"bỏ thông báo khi khởi động\", \"sửa bug authentication\", \"refactor module X\"\n\n"

                         "=== TASK CẦN PHÂN LOẠI ===\n"
                         "\"" message "\"\n\n"

                         "=== OUTPUT FORMAT ===\n"
                         "Trả lời theo format sau (CHÍNH XÁC):\n"
                         "CLASSIFICATION: [SIMPLE/MODERATE/COMPLEX]\n"
                         "REASON: [Giải thích ngắn gọn 1 câu tại sao]")}]

          result (router/chat-with-failover
                  llm-registry
                  classification-prompt
                  {:model "claude-haiku-4-5-20251001"  ; Use cheapest model!
                   :max-tokens 100                      ; Just need classification + reason
                   :temperature 0})                     ; Deterministic classification

          response (or (:response result) "")
          response-lower (str/lower-case response)]

      (log/debug "Task classification" {:message message :response response})

      ;; Parse classification from response
      (cond
        (str/includes? response-lower "complex") :complex
        (str/includes? response-lower "moderate") :moderate
        (str/includes? response-lower "simple") :simple

        ;; Fallback: if uncertain, default to MODERATE (safe choice)
        :else (do
                (log/warn "Could not parse classification, defaulting to MODERATE" {:response response})
                :moderate)))

    (catch Exception e
      (log/error e "Failed to classify task with LLM, defaulting to MODERATE")
      :moderate)))  ; Safe fallback

;; ============================================================================
;; SMART ROUTING
;; ============================================================================

(defn select-model-tier
  "Select model tier using LLM classification

  Uses Haiku to classify task as :simple/:moderate/:complex
  Returns matching tier for dynamic model routing

  Cost: $0.000025 per classification
  Benefit: Correct model selection = huge cost savings"
  [message llm-registry]
  (classify-task-with-llm message llm-registry))

(defn should-delegate?
  "Decide if task should be delegated to Claude Code using LLM classification

  NEW: Uses Haiku to classify task complexity instead of hard-coded rules!
  This solves the 'bỏ vs thêm' problem - LLM understands Vietnamese context."
  [message llm-registry]
  (= :complex (classify-task-with-llm message llm-registry)))
