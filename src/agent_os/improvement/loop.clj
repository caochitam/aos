(ns agent-os.improvement.loop
  "Self-improvement cycle - 7 steps"
  (:require [agent-os.kernel.protocols :refer [get-component list-components]]
            [agent-os.reflection.engine :as reflect]
            [agent-os.modification.engine :as mod]
            [agent-os.llm.claude :as claude]
            [agent-os.llm.router :as router]
            [agent-os.llm.tools :as tools]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; ============================================================================
;; 7-STEP SELF-IMPROVEMENT CYCLE
;; ============================================================================

(defn improve-cycle
  "Run one complete improvement cycle on a component using tool-based LLM

   With tools, Claude can:
   - Read files directly
   - Make edits directly
   - Run tests/validation commands

   This is more powerful than passing code in prompts."
  [kernel llm-registry history component-id config]

  (try
    (log/debug "Starting improvement cycle" {:component component-id})

    ;; Step 1: Get component metadata (not code - Claude will read it)
    (let [component (get-component kernel component-id)]
      (when-not component
        (throw (ex-info "Component not found" {:component-id component-id})))

      ;; Step 2: Analyze component to find issues
      (let [analysis (reflect/analyze-component component)
            issues (reflect/find-issues component)

            ;; Skip if no issues found
            _ (when (empty? issues)
                (log/debug "No issues found, skipping" {:component component-id})
                (throw (ex-info "No improvement needed" {:skip true})))

            ;; Step 3: Create improvement request with tools
            improvement-messages
            [(claude/user-message
              (str "I need you to improve a component of Agent OS.\n\n"
                   "Component: " component-id "\n"
                   "Location: /root/aos/src/" (namespace component-id) "/" (name component-id) ".clj\n\n"
                   "Detected Issues:\n"
                   (clojure.string/join "\n" (map #(str "- " %) issues)) "\n\n"
                   "Analysis:\n" (pr-str analysis) "\n\n"
                   "Please:\n"
                   "1. Read the current file using read_file\n"
                   "2. Analyze the code and issues\n"
                   "3. Make improvements using edit_file\n"
                   "4. Optionally run tests using bash if appropriate\n\n"
                   "Focus on fixing the detected issues while maintaining code quality.\n"
                   "Reply with a summary of changes when done."))]

            ;; Step 4: Call LLM with tools enabled
            llm-response (router/chat-with-failover
                          llm-registry
                          improvement-messages
                          {:tools tools/available-tools
                           :max-tokens 8000})

            response-text (:response llm-response)]

        ;; Step 5: Return success
        ;; Note: With tools, Claude has already made the edits directly
        ;; We don't need to parse code or apply modifications
        (log/debug "Improvement cycle completed" {:component component-id})
        {:success? true
         :component component-id
         :summary response-text
         :provider (:provider llm-response)}))

    (catch clojure.lang.ExceptionInfo e
      (if (= true (-> e ex-data :skip))
        {:success? false :component component-id :skip? true :reason "No improvement needed"}
        (do
          (log/error e "Improvement cycle failed" {:component component-id})
          {:success? false :component component-id :error (.getMessage e)})))

    (catch Exception e
      (log/error e "Improvement cycle exception" {:component component-id})
      {:success? false :component component-id :error (.getMessage e)})))

;; ============================================================================
;; TARGET SELECTION
;; ============================================================================

(defn identify-improvement-targets
  "Find components that could benefit from improvement"
  [kernel]
  (let [comp-ids (list-components kernel)]
    (filter (fn [comp-id]
              (let [comp (get-component kernel comp-id)]
                (and (:modifiable? comp)
                     (not= "agent-os.kernel" (namespace comp-id)))))
            comp-ids)))

(defn prioritize-targets
  "Rank components by priority (most complex first)"
  [kernel targets]
  (->> targets
       (map (fn [comp-id]
              (let [comp (get-component kernel comp-id)
                    complexity (reflect/component-complexity comp)]
                [comp-id (:complexity-score complexity)])))
       (sort-by second >)
       (map first)))

;; ============================================================================
;; BATCH IMPROVEMENT
;; ============================================================================

(defn improve-all
  "Run improvement cycle on all eligible components (with safety limits)"
  [kernel llm-registry history config]
  (let [targets (identify-improvement-targets kernel)
        prioritized (prioritize-targets kernel targets)
        max-per-run (get-in config [:kernel :max-modifications-per-hour] 10)
        limited (take max-per-run prioritized)]

    (log/info "Starting batch improvement"
              {:targets (count targets)
               :prioritized (count prioritized)
               :will-improve (count limited)})

    (doall
     (map (fn [comp-id]
            (improve-cycle kernel llm-registry history comp-id config))
          limited))))
