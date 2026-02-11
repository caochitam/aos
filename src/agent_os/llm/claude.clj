(ns agent-os.llm.claude
  "Claude Sonnet provider implementation"
  (:require [agent-os.llm.protocols :refer [ILLMProvider]]
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
          attempt (atom 0)]
      (loop []
        (let [result (try
                       (let [response (http/post
                                       CLAUDE_API_ENDPOINT
                                       {:headers {"x-api-key" api-key
                                                  "anthropic-version" ANTHROPIC_VERSION
                                                  "content-type" "application/json"}
                                        :body (json/generate-string
                                               {:model (or (:model opts) model DEFAULT_MODEL)
                                                :max_tokens (or (:max-tokens opts) max-tokens DEFAULT_MAX_TOKENS)
                                                :messages messages})
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
                             {:success true :result (get-in body [:content 0 :text])})

                           ;; Rate limited or server error - retry with backoff
                           (should-retry? status @attempt max-retries)
                           (let [backoff-ms (exponential-backoff @attempt)]
                             (log/debug "Claude API error, retrying"
                                        {:status status
                                         :attempt @attempt
                                         :backoff-ms backoff-ms})
                             (Thread/sleep backoff-ms)
                             (swap! attempt inc)
                             {:retry true})

                           ;; Permanent error
                           :else
                           (do
                             (log/error "Claude API failed" {:status status :body body})
                             {:error true
                              :exception (ex-info "Claude API error"
                                                  {:status status
                                                   :error (:error body)})})))

                       (catch java.net.SocketTimeoutException e
                         (log/error e "Claude API timeout")
                         (if (< @attempt max-retries)
                           (do
                             (Thread/sleep (exponential-backoff @attempt))
                             (swap! attempt inc)
                             {:retry true})
                           {:error true
                            :exception (ex-info "Claude API timeout after retries"
                                                {:attempts @attempt})}))

                       (catch Exception e
                         (log/error e "Claude API exception")
                         {:error true :exception e}))]
          (cond
            (:success result) (:result result)
            (:retry result) (recur)
            (:error result) (throw (:exception result)))))))

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
