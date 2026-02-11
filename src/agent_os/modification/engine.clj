(ns agent-os.modification.engine
  "Safe code modification engine with validation and rollback"
  (:require [agent-os.kernel.core :as kernel]
            [agent-os.kernel.protocols :refer [get-component component-id component-version
                                                component-code component-metadata]]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; ============================================================================
;; MODIFICATION PROPOSAL
;; ============================================================================

(defrecord ModificationProposal [id
                                  component-id
                                  old-code
                                  new-code
                                  reason
                                  metadata
                                  timestamp
                                  status])

(defn create-proposal
  "Create a modification proposal"
  [comp-id old-code new-code reason metadata]
  (map->ModificationProposal
    {:id (str "mod-" (System/currentTimeMillis))
     :component-id comp-id
     :old-code old-code
     :new-code new-code
     :reason reason
     :metadata metadata
     :timestamp (System/currentTimeMillis)
     :status :proposed}))

;; ============================================================================
;; CODE VALIDATION
;; ============================================================================

(defn validate-syntax
  "Validate Clojure syntax by attempting to read code"
  [code-string]
  (try
    (let [code-str (if (string? code-string)
                     code-string
                     (pr-str code-string))
          reader (reader-types/indexing-push-back-reader code-str)]
      (loop [forms []]
        (let [form (reader/read {:eof ::eof} reader)]
          (if (= form ::eof)
            {:valid? true :forms forms}
            (recur (conj forms form))))))
    (catch Exception e
      {:valid? false
       :error (.getMessage e)})))

(defn validate-size
  "Validate code size is within limits"
  [code max-size-bytes]
  (let [size (count (pr-str code))]
    (if (<= size max-size-bytes)
      {:valid? true :size size}
      {:valid? false
       :error (str "Code size " size " exceeds limit " max-size-bytes)
       :size size})))

(defn validate-not-kernel
  "Ensure component is not in protected kernel namespace"
  [comp-id protected-namespaces]
  (let [ns-str (namespace comp-id)]
    (if (and ns-str (some #(str/starts-with? ns-str %) protected-namespaces))
      {:valid? false
       :error (str "Cannot modify protected namespace: " ns-str)}
      {:valid? true})))

(defn validate-dependencies
  "Ensure all dependencies exist"
  [new-code existing-components]
  ;; Simplified - just return valid for now
  ;; Full implementation would parse requires and check existence
  {:valid? true})

(defn validate-proposal
  "Run all validation checks on a modification proposal"
  [proposal config existing-components]
  (let [new-code (:new-code proposal)
        comp-id (:component-id proposal)
        max-size (get-in config [:safety :max-code-size-bytes] 50000)
        protected-ns (get-in config [:safety :protected-namespaces] [])

        ;; Run all validations
        syntax-check (validate-syntax new-code)
        size-check (validate-size new-code max-size)
        kernel-check (validate-not-kernel comp-id protected-ns)
        dep-check (validate-dependencies new-code existing-components)

        all-checks [syntax-check size-check kernel-check dep-check]
        valid? (every? :valid? all-checks)
        errors (mapcat #(when-not (:valid? %) [(:error %)]) all-checks)]

    {:valid? valid?
     :checks {:syntax syntax-check
              :size size-check
              :kernel kernel-check
              :dependencies dep-check}
     :errors (vec errors)}))

;; ============================================================================
;; MODIFICATION APPLICATION
;; ============================================================================

(defn apply-modification
  "Apply a validated modification to kernel"
  [kernel proposal]
  (let [comp-id (:component-id proposal)
        new-code (:new-code proposal)
        old-component (get-component kernel comp-id)]

    (if-not old-component
      {:success? false
       :error "Component not found"
       :component-id comp-id}

      (let [new-component (assoc old-component
                                 :code new-code
                                 :version (inc (component-version old-component))
                                 :modified-at (System/currentTimeMillis)
                                 :previous-version (component-code old-component)
                                 :modification-id (:id proposal))]

        ;; Update component in kernel
        (swap! (:state kernel)
               assoc-in [:components comp-id] new-component)

        (log/info "Modification applied"
                  {:component comp-id
                   :new-version (:version new-component)
                   :mod-id (:id proposal)})

        {:success? true
         :component new-component
         :rollback-data old-component}))))

(defn rollback-modification
  "Rollback to previous version"
  [kernel comp-id]
  (let [component (get-component kernel comp-id)
        previous-code (:previous-version component)]

    (if-not previous-code
      {:success? false
       :error "No previous version available"}

      (let [rolled-back (assoc component
                               :code previous-code
                               :version (dec (component-version component))
                               :modified-at (System/currentTimeMillis)
                               :rollback-at (System/currentTimeMillis))]

        (swap! (:state kernel)
               assoc-in [:components comp-id] rolled-back)

        (log/info "Modification rolled back"
                  {:component comp-id
                   :version (:version rolled-back)})

        {:success? true
         :component rolled-back}))))

;; ============================================================================
;; MODIFICATION HISTORY
;; ============================================================================

(defn create-history
  "Create modification history tracker"
  []
  (atom {:modifications []
         :successful []
         :failed []
         :by-component {}}))

(defn record-modification
  "Record a modification attempt in history"
  [history proposal result]
  (swap! history
         (fn [h]
           (let [record (assoc proposal
                               :result result
                               :completed-at (System/currentTimeMillis))
                 comp-id (:component-id proposal)]
             (-> h
                 (update :modifications conj record)
                 (update (if (:success? result) :successful :failed) conj record)
                 (update-in [:by-component comp-id] (fnil conj []) record))))))

(defn get-component-history
  "Get modification history for a specific component"
  [history comp-id]
  (get-in @history [:by-component comp-id] []))

(defn get-recent-modifications
  "Get N most recent modifications"
  [history n]
  (->> @history
       :modifications
       (take-last n)
       reverse))

(defn modification-stats
  "Get statistics about modifications"
  [history]
  (let [h @history
        total (count (:modifications h))
        successful (count (:successful h))
        failed (count (:failed h))]
    {:total total
     :successful successful
     :failed failed
     :success-rate (if (pos? total) (double (/ successful total)) 0.0)
     :by-component (into {}
                         (map (fn [[k v]] [k (count v)])
                              (:by-component h)))}))
