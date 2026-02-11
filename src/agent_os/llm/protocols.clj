(ns agent-os.llm.protocols
  "LLM provider protocols - multi-provider support inspired by OpenClaw")

;; ============================================================================
;; LLM PROVIDER PROTOCOL
;; ============================================================================

(defprotocol ILLMProvider
  "Protocol for LLM providers - allows failover between Claude, OpenAI, Ollama, etc."
  (provider-name [this] "Unique identifier for this provider (e.g., :claude, :openai)")
  (chat [this messages opts] "Send messages and return response string. Messages format: [{:role :content}]")
  (supports-tools? [this] "Does this provider support tool/function calling?")
  (available? [this] "Is provider currently available? Check cooldown, rate limits, etc."))

;; ============================================================================
;; PROVIDER STATE
;; ============================================================================

(defrecord ProviderState [provider
                          available?
                          last-request-at
                          failure-count
                          cooldown-until])

(defn create-provider-state
  "Create initial state for a provider"
  [provider]
  (map->ProviderState
    {:provider provider
     :available? true
     :last-request-at nil
     :failure-count 0
     :cooldown-until nil}))
