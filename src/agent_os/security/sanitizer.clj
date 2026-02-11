(ns agent-os.security.sanitizer
  "Security sanitization utilities to prevent API key leakage"
  (:require [clojure.string :as str]))

;; ============================================================================
;; API KEY PROTECTION
;; ============================================================================

(def ^:private sensitive-patterns
  "Patterns that might indicate API keys or sensitive data"
  [#"sk-ant-api03-[a-zA-Z0-9_-]+"     ; Anthropic API key pattern (specific)
   #"sk-ant-[a-zA-Z0-9_-]+"           ; General Anthropic key pattern
   #"ANTHROPIC_API_KEY"
   #":api-key"
   #"api\.anthropic\.com.*x-api-key"])

(defn redact-api-keys
  "Redact API keys from text using pattern matching"
  [text]
  (if (string? text)
    (reduce (fn [result pattern]
              (str/replace result pattern "[REDACTED_API_KEY]"))
            text
            sensitive-patterns)
    text))

(defn sanitize-data-structure
  "Recursively sanitize data structures to remove API keys"
  [data]
  (cond
    (string? data)
    (redact-api-keys data)

    (map? data)
    (into {}
          (map (fn [[k v]]
                 ;; Remove :api-key fields entirely
                 (if (or (= k :api-key)
                         (= k "api-key")
                         (= k "x-api-key")
                         (= k "ANTHROPIC_API_KEY"))
                   [k "[REDACTED]"]
                   [k (sanitize-data-structure v)]))
               data))

    (sequential? data)
    (mapv sanitize-data-structure data)

    :else
    data))

(defn sanitize-response
  "Sanitize LLM response to prevent API key leakage"
  [response]
  (-> response
      sanitize-data-structure
      (update :content redact-api-keys)))

(defn sanitize-error
  "Sanitize error messages to prevent API key leakage"
  [error-msg]
  (if (string? error-msg)
    (redact-api-keys error-msg)
    (str (redact-api-keys (str error-msg)))))

;; ============================================================================
;; VALIDATION
;; ============================================================================

(defn contains-sensitive-info?
  "Check if text contains sensitive information"
  [text]
  (when (string? text)
    (some #(re-find % text) sensitive-patterns)))

(defn validate-safe-output
  "Validate that output doesn't contain sensitive information"
  [output]
  (let [output-str (str output)]
    (when (contains-sensitive-info? output-str)
      (throw (ex-info "Output contains sensitive information"
                      {:sanitized (redact-api-keys output-str)})))))

;; ============================================================================
;; SECURE LOGGING
;; ============================================================================

(defn safe-log-data
  "Prepare data for logging by sanitizing sensitive fields"
  [data]
  (sanitize-data-structure data))
