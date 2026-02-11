(ns agent-os.improvement.loop
  "Self-improvement cycle - 7 steps"
  (:require [agent-os.kernel.protocols :refer [get-component list-components]]
            [agent-os.reflection.engine :as reflect]
            [agent-os.modification.engine :as mod]
            [agent-os.llm.claude :as claude]
            [agent-os.llm.router :as router]
            [taoensso.timbre :as log]))

;; ============================================================================
;; 7-STEP SELF-IMPROVEMENT CYCLE
;; ============================================================================

(defn improve-cycle
  "Run one complete improvement cycle on a component
   Steps:
   1. Reflect - read current code
   2. Analyze - send to LLM for analysis
   3. Decide - LLM decides if modification needed
   4. Generate - LLM generates improved code
   5. Validate - run safety checks
   6. Apply - apply modification
   7. Learn - record outcome, update patterns"
  [kernel llm-registry history component-id config]

  (try
    (log/info "Starting improvement cycle" {:component component-id})

    ;; Step 1: Reflect - read current code
    (let [component (get-component kernel component-id)]
      (when-not component
        (throw (ex-info "Component not found" {:component-id component-id})))

      ;; Step 2: Analyze - get LLM analysis
      (let [analysis (reflect/analyze-component component)
            issues (reflect/find-issues component)

            ;; Skip if no issues found
            _ (when (empty? issues)
                (log/info "No issues found, skipping" {:component component-id})
                (throw (ex-info "No improvement needed" {:skip true})))

            ;; Step 3: Decide - create analysis prompt
            analysis-prompt [(claude/user-message
                              (str "Analyze this component and suggest improvements:\n\n"
                                   "Component ID: " component-id "\n"
                                   "Code: " (pr-str (:code component)) "\n\n"
                                   "Detected issues: " (pr-str issues) "\n\n"
                                   "Analysis: " (pr-str analysis) "\n\n"
                                   "Provide improved code as a Clojure S-expression."))]

            ;; Step 4: Generate - call LLM
            llm-response (router/chat-with-failover llm-registry analysis-prompt {})
            improved-code-str (:response llm-response)

            ;; Parse LLM response to get code
            improved-code (read-string improved-code-str)

            ;; Step 5: Validate - create proposal and validate
            proposal (mod/create-proposal
                      component-id
                      (:code component)
                      improved-code
                      (str "Auto-improvement: " (first issues))
                      {:issues issues :analysis analysis})

            validation (mod/validate-proposal proposal config {})

            _ (when-not (:valid? validation)
                (log/warn "Validation failed" {:errors (:errors validation)})
                (throw (ex-info "Validation failed"
                                {:validation validation})))]

        ;; Step 6: Apply - apply the modification
        (let [result (mod/apply-modification kernel proposal)]
          (if (:success? result)
            (do
              ;; Step 7: Learn - record success
              (mod/record-modification history proposal result)
              (log/info "Improvement cycle completed"
                        {:component component-id
                         :new-version (get-in result [:component :version])})
              {:success? true
               :component component-id
               :result result})

            (do
              (mod/record-modification history proposal result)
              (log/warn "Modification failed" {:component component-id})
              {:success? false
               :component component-id
               :error (:error result)})))))

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
