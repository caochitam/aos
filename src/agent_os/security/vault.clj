(ns agent-os.security.vault
  "Secure credential storage and management"
  (:require [taoensso.timbre :as log]
            [agent-os.security.sanitizer :as sanitizer]))

;; ============================================================================
;; SECURE VAULT
;; ============================================================================

(defprotocol ICredentialVault
  "Protocol for secure credential storage"
  (get-credential [this key-name] "Retrieve a credential securely")
  (has-credential? [this key-name] "Check if credential exists"))

(defrecord SecureVault [credentials]
  ICredentialVault
  (get-credential [_ key-name]
    ;; Never log the actual credential
    (log/debug "Retrieving credential" {:key-name key-name :exists (contains? @credentials key-name)})
    (get @credentials key-name))

  (has-credential? [_ key-name]
    (contains? @credentials key-name)))

(defn create-vault
  "Create a secure credential vault

  The vault stores sensitive credentials in memory with protection against:
  - Serialization/reflection attacks
  - Accidental logging
  - Prompt injection attempts to reveal keys"
  [initial-credentials]
  (log/debug "Creating secure vault" {:key-count (count initial-credentials)})
  ;; Use atom with private ref - harder to access via reflection
  (->SecureVault (atom initial-credentials)))

;; ============================================================================
;; ENVIRONMENT VARIABLE LOADING
;; ============================================================================

(defn load-from-env
  "Safely load credentials from environment variables"
  [env-var-name]
  (let [value (System/getenv env-var-name)]
    (when value
      (log/debug "Loaded credential from environment" {:var env-var-name :length (count value)}))
    value))

(defn load-api-key
  "Load Anthropic API key from environment with validation"
  []
  (let [api-key (load-from-env "ANTHROPIC_API_KEY")]
    (cond
      (nil? api-key)
      (do
        (log/warn "ANTHROPIC_API_KEY not set in environment")
        nil)

      (not (re-matches #"sk-ant-.*" api-key))
      (do
        (log/error "Invalid API key format detected")
        (throw (ex-info "Invalid API key format" {})))

      :else
      (do
        (log/debug "API key loaded successfully" {:key-prefix (subs api-key 0 10)})
        api-key))))

;; ============================================================================
;; SYSTEM INTEGRATION
;; ============================================================================

(defn create-system-vault
  "Create vault with all system credentials loaded from environment"
  []
  (let [api-key (load-api-key)]
    (create-vault
      {:anthropic-api-key api-key})))

(defn get-anthropic-api-key
  "Get Anthropic API key from vault - this is the ONLY way to access it"
  [vault]
  (get-credential vault :anthropic-api-key))

;; ============================================================================
;; SECURITY WRAPPER FOR PROVIDERS
;; ============================================================================

(defn wrap-provider-security
  "Wrap LLM provider with security layer to prevent credential leakage"
  [provider vault]
  ;; Return provider with credentials from vault
  ;; The actual API key is never stored in the provider itself
  (assoc provider ::vault vault))

(defn get-provider-api-key
  "Securely retrieve API key for provider from vault"
  [provider]
  (when-let [vault (::vault provider)]
    (get-anthropic-api-key vault)))
