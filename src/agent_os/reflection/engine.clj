(ns agent-os.reflection.engine
  "Reflection engine - allows agent to read and analyze its own code"
  (:require [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; ============================================================================
;; SOURCE CODE READING
;; ============================================================================

(defn namespace->file-path
  "Convert namespace to file path (e.g., agent-os.kernel.core -> agent_os/kernel/core.clj)"
  [ns-symbol]
  (-> (str ns-symbol)
      (str/replace #"\." "/")
      (str/replace #"-" "_")
      (str ".clj")))

(defn read-own-source
  "Read source code from namespace - returns source as string"
  [ns-symbol]
  (try
    (let [file-path (namespace->file-path ns-symbol)
          resource (io/resource file-path)]
      (if resource
        (slurp resource)
        (do
          (log/warn "Source file not found" {:namespace ns-symbol :path file-path})
          nil)))
    (catch Exception e
      (log/error e "Failed to read source" {:namespace ns-symbol})
      nil)))

(defn parse-source
  "Parse Clojure source code into S-expressions"
  [source-string]
  (try
    (let [reader (reader-types/indexing-push-back-reader source-string)]
      (loop [forms []]
        (let [form (reader/read {:eof ::eof} reader)]
          (if (= form ::eof)
            forms
            (recur (conj forms form))))))
    (catch Exception e
      (log/error e "Failed to parse source")
      [])))

;; ============================================================================
;; CODE ANALYSIS
;; ============================================================================

(defn extract-functions
  "Extract all function definitions from parsed forms"
  [forms]
  (filter #(and (seq? %)
                (= 'defn (first %)))
          forms))

(defn function-signature
  "Extract function signature from defn form"
  [defn-form]
  (when (and (seq? defn-form)
             (= 'defn (first defn-form)))
    (let [[_ fn-name & rest] defn-form
          docstring (when (string? (first rest)) (first rest))
          params (if docstring (second rest) (first rest))]
      {:name fn-name
       :params params
       :docstring docstring})))

(defn count-lines
  "Count lines of code (excluding blank lines and comments)"
  [source-string]
  (when source-string
    (->> (str/split-lines source-string)
         (remove str/blank?)
         (remove #(str/starts-with? (str/trim %) ";"))
         count)))

(defn nesting-depth
  "Calculate maximum nesting depth in form"
  [form]
  (cond
    (not (coll? form)) 0
    (empty? form) 1
    :else (inc (apply max 0 (map nesting-depth form)))))

(defn analyze-component
  "Analyze a component's structure and complexity"
  [component]
  (let [code (when component (:code component))
        code-str (if (string? code) code (pr-str code))
        parsed (when code-str (parse-source code-str))
        functions (extract-functions parsed)
        signatures (map function-signature functions)]
    {:id (:id component)
     :version (:version component)
     :modifiable? (:modifiable? component)
     :code-size (count code-str)
     :line-count (count-lines code-str)
     :function-count (count functions)
     :function-signatures signatures
     :max-nesting-depth (if (seq? code) (nesting-depth code) 0)
     :metadata (:metadata component)}))

;; ============================================================================
;; DEPENDENCY ANALYSIS
;; ============================================================================

(defn extract-requires
  "Extract all :require dependencies from namespace form"
  [parsed-forms]
  (let [ns-form (first (filter #(and (seq? %) (= 'ns (first %))) parsed-forms))]
    (when ns-form
      (->> ns-form
           (filter #(and (seq? %) (= :require (first %))))
           first
           rest
           (map (fn [req]
                  (if (vector? req)
                    (first req)
                    req)))
           (remove nil?)))))

(defn list-dependencies
  "List all dependencies of a component"
  [component]
  (let [code (:code component)
        code-str (if (string? code) code (pr-str code))
        parsed (parse-source code-str)
        requires (extract-requires parsed)
        explicit-deps (get-in component [:metadata :dependencies])]
    {:requires (set requires)
     :declared-dependencies explicit-deps
     :all-dependencies (into (set requires) explicit-deps)}))

;; ============================================================================
;; ISSUE DETECTION
;; ============================================================================

(defn find-issues
  "Detect potential code smells and issues"
  [component]
  (let [analysis (analyze-component component)
        issues []]
    (cond-> issues
      ;; Deep nesting
      (> (:max-nesting-depth analysis) 5)
      (conj {:type :deep-nesting
             :severity :warning
             :message (str "Deep nesting detected: " (:max-nesting-depth analysis))})

      ;; Long code
      (> (:line-count analysis) 200)
      (conj {:type :long-code
             :severity :info
             :message (str "Component has " (:line-count analysis) " lines")})

      ;; No functions (might be just data)
      (zero? (:function-count analysis))
      (conj {:type :no-functions
             :severity :info
             :message "Component contains no function definitions"})

      ;; Missing docstrings
      (and (pos? (:function-count analysis))
           (some #(nil? (:docstring %)) (:function-signatures analysis)))
      (conj {:type :missing-docstrings
             :severity :info
             :message "Some functions lack docstrings"}))))

;; ============================================================================
;; COMPONENT COMPLEXITY METRICS
;; ============================================================================

(defn component-complexity
  "Calculate complexity metrics for a component"
  [component]
  (let [analysis (analyze-component component)]
    {:lines-of-code (:line-count analysis)
     :function-count (:function-count analysis)
     :max-nesting (:max-nesting-depth analysis)
     :code-size-bytes (:code-size analysis)
     :complexity-score (+ (* 0.3 (min 100 (:line-count analysis)))
                          (* 0.3 (* 10 (:max-nesting-depth analysis)))
                          (* 0.4 (* 5 (:function-count analysis))))}))
