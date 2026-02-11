(ns agent-os.memory.compaction
  "OPENCLAW OPTIMIZATION: Session compaction for long conversations
  Automatically summarizes old messages to reduce context window usage."
  (:require [agent-os.llm.protocols :refer [chat]]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; ============================================================================
;; CONFIGURATION
;; ============================================================================

(def soft-threshold-tokens 4000)
(def hard-threshold-tokens 8000)
(def keep-recent-messages 10)

;; ============================================================================
;; TOKEN ESTIMATION
;; ============================================================================

(defn estimate-tokens
  "Rough token estimation: 4 characters ≈ 1 token"
  [text]
  (/ (count (str text)) 4))

(defn estimate-message-tokens
  "Estimate tokens for a single message"
  [message]
  (estimate-tokens (:content message)))

(defn estimate-total-tokens
  "Estimate total tokens for all messages"
  [messages]
  (->> messages
       (map estimate-message-tokens)
       (reduce + 0)))

;; ============================================================================
;; COMPACTION LOGIC
;; ============================================================================

(defn should-compact?
  "Check if conversation needs compaction based on token count"
  [messages]
  (let [total-tokens (estimate-total-tokens messages)]
    (>= total-tokens soft-threshold-tokens)))

(defn compact-history
  "Summarize old messages, keep recent ones intact.
  Uses Haiku (cheap) for summarization.

  Returns new message list: [summary-message] + [recent N messages]"
  [messages claude-provider]
  (when (should-compact? messages)
    (let [to-summarize (drop-last keep-recent-messages messages)
          recent (take-last keep-recent-messages messages)]

      (if (empty? to-summarize)
        ;; Nothing to compact
        messages

        ;; Summarize old messages
        (do
          (log/info "Compacting conversation"
                   {:old-count (count messages)
                    :new-count (inc keep-recent-messages)
                    :tokens-before (estimate-total-tokens messages)})

          (try
            (let [summary-prompt [{:role "user"
                                  :content (str "Tóm tắt cuộc hội thoại sau bằng tiếng Việt. "
                                               "Giữ lại các quyết định quan trọng, file paths, "
                                               "context cần thiết, và thông tin then chốt:\n\n"
                                               (str/join "\n\n"
                                                        (map #(str (:role %) ": " (:content %))
                                                             to-summarize)))}]

                  ;; Use Haiku for cheap summarization
                  summary-response (chat claude-provider
                                        summary-prompt
                                        {:model "claude-haiku-4-5-20251001"
                                         :max-tokens 500})]

              (log/info "Compaction successful"
                       {:tokens-after (estimate-total-tokens
                                      (cons {:role "system" :content summary-response}
                                            recent))})

              ;; Return: [summary] + [recent N]
              (cons {:role "system"
                     :content (str "📝 [Tóm tắt hội thoại trước]\n\n" summary-response)}
                    recent))

            (catch Exception e
              (log/error e "Compaction failed")
              ;; If summarization fails, just keep everything
              messages)))))))

(defn maybe-compact
  "Compact conversation if needed. Returns updated messages."
  [messages claude-provider]
  (if (should-compact? messages)
    (do
      (println "⚠️  Conversation getting long, compacting history...")
      (compact-history messages claude-provider))
    messages))

;; ============================================================================
;; MONITORING
;; ============================================================================

(defn get-conversation-stats
  "Get statistics about conversation token usage"
  [messages]
  (let [total-tokens (estimate-total-tokens messages)
        percentage (/ total-tokens soft-threshold-tokens)]
    {:message-count (count messages)
     :total-tokens total-tokens
     :soft-threshold soft-threshold-tokens
     :hard-threshold hard-threshold-tokens
     :percentage (int (* percentage 100))
     :needs-compaction? (should-compact? messages)}))
