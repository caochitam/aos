(ns agent-os.llm.claude
  "Claude Sonnet provider implementation"
  (:require [agent-os.llm.protocols :refer [ILLMProvider]]
            [agent-os.llm.tools :as tools]
            [agent-os.security.sanitizer :as sanitizer]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [taoensso.timbre :as log]))

;; ============================================================================
;; CONSTANTS
;; ============================================================================

(def ^:const CLAUDE_API_ENDPOINT "https://api.anthropic.com/v1/messages")
(def ^:const ANTHROPIC_VERSION "2023-06-01")
(def ^:const DEFAULT_MODEL "claude-sonnet-4-20250514")
(def ^:const DEFAULT_MAX_TOKENS 4000)
(def ^:const DEFAULT_TIMEOUT 30000) ; 30 seconds

;; ============================================================================
;; RATE LIMITING & BACKOFF
;; ============================================================================

(defn exponential-backoff
  "Calculate backoff time in ms for retry attempt"
  [attempt]
  (min 60000 (* 1000 (Math/pow 2 attempt)))) ; max 60s

(defn should-retry?
  "Check if we should retry based on error"
  [status-code attempt max-retries]
  (and (< attempt max-retries)
       (or (= 429 status-code)  ; Rate limit
           (= 500 status-code)  ; Server error
           (= 503 status-code)))) ; Service unavailable

;; ============================================================================
;; CLAUDE PROVIDER
;; ============================================================================

(defrecord ClaudeProvider [api-key model max-tokens timeout]
  ILLMProvider
  (provider-name [_] :claude)

  (chat [this messages opts]
    (let [max-retries (get opts :max-retries 3)
          tools (get opts :tools)
          conversation (atom messages)]
      ;; Tool execution loop
      (loop [iteration 0]
        (when (> iteration 20)
          (throw (ex-info "Too many tool iterations" {:iterations iteration})))

        ;; Make API call with retry logic
        (let [api-result
              (loop [attempt 0]
                (let [result (try
                               (let [system-prompt (get opts :system)
                                     ;; OPENCLAW OPTIMIZATION: Prompt Caching
                                     ;; Mark system prompt for Anthropic caching (90% discount)
                                     system-with-cache (when system-prompt
                                                        [{:type "text"
                                                          :text system-prompt
                                                          :cache_control {:type "ephemeral"}}])
                                     request-body (cond-> {:model (or (:model opts) model DEFAULT_MODEL)
                                                           :max_tokens (or (:max-tokens opts) max-tokens DEFAULT_MAX_TOKENS)
                                                           :messages @conversation}
                                                    tools (assoc :tools tools)
                                                    system-with-cache (assoc :system system-with-cache))
                                     response (http/post
                                               CLAUDE_API_ENDPOINT
                                               {:headers {"x-api-key" api-key
                                                          "anthropic-version" ANTHROPIC_VERSION
                                                          "content-type" "application/json"}
                                                :body (json/generate-string request-body)
                                                :socket-timeout (or timeout DEFAULT_TIMEOUT)
                                                :conn-timeout 10000
                                                :throw-exceptions false})
                                     body (json/parse-string (:body response) true)
                                     status (:status response)]

                                 (cond
                                   ;; Success
                                   (= 200 status)
                                   (do
                                     (log/debug "Claude API success" {:model (:model body)})
                                     ;; Sanitize response to prevent API key leakage
                                     {:success true :result (sanitizer/sanitize-response body)})

                                   ;; Rate limited or server error - retry with backoff
                                   (should-retry? status attempt max-retries)
                                   (let [backoff-ms (exponential-backoff attempt)]
                                     (log/debug "Claude API error, retrying"
                                                {:status status
                                                 :attempt attempt
                                                 :backoff-ms backoff-ms})
                                     (Thread/sleep backoff-ms)
                                     {:retry true})

                                   ;; Permanent error
                                   :else
                                   (do
                                     ;; Sanitize error logs to prevent API key leakage
                                     (log/error "Claude API failed"
                                                {:status status
                                                 :body (sanitizer/safe-log-data body)})
                                     {:error true
                                      :exception (ex-info "Claude API error"
                                                          {:status status
                                                           :error (sanitizer/safe-log-data (:error body))})})))

                               (catch java.net.SocketTimeoutException e
                                 (log/error e "Claude API timeout")
                                 (if (< attempt max-retries)
                                   (do
                                     (Thread/sleep (exponential-backoff attempt))
                                     {:retry true})
                                   {:error true
                                    :exception (ex-info "Claude API timeout after retries"
                                                        {:attempts attempt})}))

                               (catch Exception e
                                 ;; Sanitize exception messages
                                 (let [sanitized-msg (sanitizer/sanitize-error (.getMessage e))]
                                   (log/error sanitized-msg "Claude API exception")
                                   {:error true
                                    :exception (ex-info sanitized-msg
                                                       {:original-type (class e)})})))]

                  (cond
                    (:retry result) (recur (inc attempt))
                    :else result)))]

          ;; Handle API result
          (cond
            (:error api-result)
            (throw (:exception api-result))

            (:success api-result)
            (let [response-body (:result api-result)
                  content (:content response-body)
                  stop-reason (:stop_reason response-body)]

              ;; Add assistant response to conversation
              (swap! conversation conj {:role "assistant" :content content})

              (cond
                ;; Check if there are tool uses
                (some #(= "tool_use" (:type %)) content)
                (let [tool-uses (filter #(= "tool_use" (:type %)) content)
                      tool-results (mapv (fn [tool-use]
                                          (let [tool-name (:name tool-use)
                                                tool-input (:input tool-use)
                                                tool-id (:id tool-use)]
                                            (log/debug "Executing tool" {:tool tool-name :id tool-id})
                                            (let [result (tools/execute-tool tool-name tool-input)]
                                              (tools/format-tool-result tool-id result))))
                                        tool-uses)]
                  ;; Add tool results to conversation
                  (swap! conversation conj {:role "user" :content tool-results})
                  ;; Continue outer loop to get final response
                  (recur (inc iteration)))

                ;; Final text response
                :else
                (let [text-content (first (filter #(= "text" (:type %)) content))
                      response-text (:text text-content "")]
                  ;; Final sanitization check before returning to user
                  (sanitizer/redact-api-keys response-text)))))))))

  (supports-tools? [_] true)

  (available? [_]
    ;; Check if API key is present
    (boolean api-key)))

;; ============================================================================
;; FACTORY
;; ============================================================================

(defn create-claude-provider
  "Create a Claude provider instance"
  [api-key & {:keys [model max-tokens timeout]
              :or {model DEFAULT_MODEL
                   max-tokens DEFAULT_MAX_TOKENS
                   timeout DEFAULT_TIMEOUT}}]
  (when-not api-key
    (log/warn "Claude API key not provided, provider will be unavailable"))
  (map->ClaudeProvider
    {:api-key api-key
     :model model
     :max-tokens max-tokens
     :timeout timeout}))

;; ============================================================================
;; PROMPT HELPERS
;; ============================================================================

(defn system-message
  "Create a system message for Claude"
  [content]
  {:role "system" :content content})

(defn user-message
  "Create a user message for Claude"
  [content]
  {:role "user" :content content})

(defn assistant-message
  "Create an assistant message for Claude"
  [content]
  {:role "assistant" :content content})
