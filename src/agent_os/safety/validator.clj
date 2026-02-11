(ns agent-os.safety.validator
  "Code validation and safety checks"
  (:require [clojure.string :as str]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [taoensso.timbre :as log]))

;; ============================================================================
;; DANGEROUS PATTERNS
;; ============================================================================

(def dangerous-namespaces
  "Namespaces that should not be required by untrusted code"
  #{"clojure.java.shell"
    "clojure.java.jdbc"
    "java.lang.Runtime"
    "java.lang.ProcessBuilder"})

(def dangerous-symbols
  "Symbols that indicate potentially dangerous operations"
  #{'eval
    'read-string  ; Can execute code
    'load-file
    'load-string
    'sh           ; Shell execution
    'System/exit})

;; ============================================================================
;; SYNTAX VALIDATION
;; ============================================================================

(defn validate-syntax
  "Validate Clojure syntax"
  [code-string]
  (try
    (let [code-str (if (string? code-string) code-string (pr-str code-string))
          reader (reader-types/indexing-push-back-reader code-str)]
      (loop [forms []]
        (let [form (reader/read {:eof ::eof} reader)]
          (if (= form ::eof)
            {:valid? true :forms forms}
            (recur (conj forms form))))))
    (catch Exception e
      {:valid? false
       :error (.getMessage e)})))

;; ============================================================================
;; SAFETY CHECKS
;; ============================================================================

(defn check-requires
  "Check for dangerous requires"
  [code allowed-requires]
  (let [code-str (if (string? code) code (pr-str code))
        forms (:forms (validate-syntax code-str))
        ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))
        requires (when ns-form
                   (->> ns-form
                        (filter #(and (seq? %) (= :require (first %))))
                        first
                        rest
                        (map (fn [req] (if (vector? req) (first req) req)))
                        (map str)))]
    (let [forbidden (filter dangerous-namespaces requires)
          allowed? (every? allowed-requires requires)]
      (cond
        (seq forbidden)
        {:valid? false
         :error (str "Dangerous namespaces: " (str/join ", " forbidden))}

        (not allowed?)
        {:valid? false
         :error "Contains non-whitelisted requires"}

        :else
        {:valid? true}))))

(defn check-dangerous-symbols
  "Check for dangerous symbol usage"
  [code]
  (let [code-str (if (string? code) code (pr-str code))
        forms (:forms (validate-syntax code-str))]
    (letfn [(find-symbols [form]
              (cond
                (symbol? form) [form]
                (coll? form) (mapcat find-symbols form)
                :else []))]
      (let [all-symbols (set (mapcat find-symbols forms))
            found-dangerous (filter dangerous-symbols all-symbols)]
        (if (seq found-dangerous)
          {:valid? false
           :error (str "Dangerous symbols found: " (str/join ", " found-dangerous))}
          {:valid? true})))))

(defn check-infinite-loops
  "Basic heuristic check for obvious infinite loops"
  [code]
  ;; Very basic check - look for (while true) patterns
  (let [code-str (str code)]
    (if (or (str/includes? code-str "(while true")
            (str/includes? code-str "(loop [] (recur"))
      {:valid? false
       :error "Potential infinite loop detected"}
      {:valid? true})))

(defn check-size
  "Check code size limits"
  [code max-bytes]
  (let [size (count (pr-str code))]
    (if (<= size max-bytes)
      {:valid? true :size size}
      {:valid? false
       :error (str "Code size " size " exceeds limit " max-bytes)
       :size size})))

(defn check-protected-namespace
  "Ensure component is not in protected namespace"
  [component-id protected-namespaces]
  (let [ns-str (namespace component-id)]
    (if (and ns-str (some #(str/starts-with? ns-str %) protected-namespaces))
      {:valid? false
       :error (str "Cannot modify protected namespace: " ns-str)}
      {:valid? true})))

;; ============================================================================
;; MASTER VALIDATION
;; ============================================================================

(defn validate-code
  "Run all safety checks on code"
  [code config & {:keys [component-id]}]
  (let [protected-ns (get-in config [:safety :protected-namespaces] [])
        max-size (get-in config [:safety :max-code-size-bytes] 50000)
        allowed-requires (get-in config [:safety :allowed-requires] #{})

        checks [(validate-syntax code)
                (check-size code max-size)
                (check-requires code allowed-requires)
                (check-dangerous-symbols code)
                (check-infinite-loops code)
                (when component-id
                  (check-protected-namespace component-id protected-ns))]

        checks (remove nil? checks)
        all-valid? (every? :valid? checks)
        errors (mapcat #(when-not (:valid? %) [(:error %)]) checks)]

    {:valid? all-valid?
     :checks (zipmap [:syntax :size :requires :symbols :loops :namespace]
                     checks)
     :errors (vec errors)}))
