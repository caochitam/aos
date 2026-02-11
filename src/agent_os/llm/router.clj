(ns agent-os.llm.router
  "LLM provider router with failover - inspired by OpenClaw"
  (:require [agent-os.llm.protocols :refer [ILLMProvider provider-name chat
                                             available? create-provider-state]]
            [taoensso.timbre :as log]))

;; ============================================================================
;; PROVIDER REGISTRY
;; ============================================================================

(defrecord ProviderRegistry [providers        ; map of provider-name -> provider
                              provider-states  ; map of provider-name -> ProviderState
                              primary          ; keyword of primary provider
                              fallbacks])      ; vector of fallback provider keywords

(defn create-registry
  "Create a provider registry"
  [providers & {:keys [primary fallbacks]
                :or {primary :claude
                     fallbacks []}}]
  (let [provider-map (into {} (map (fn [p] [(provider-name p) p]) providers))
        states (into {} (map (fn [[k v]] [k (create-provider-state v)]) provider-map))]
    (map->ProviderRegistry
      {:providers provider-map
       :provider-states (atom states)
       :primary primary
       :fallbacks fallbacks})))

;; ============================================================================
;; COOLDOWN MANAGEMENT
;; ============================================================================

(defn mark-provider-failed
  "Mark a provider as failed and put it in cooldown"
  [registry provider-key]
  (swap! (:provider-states registry)
         update provider-key
         (fn [state]
           (let [new-failure-count (inc (:failure-count state))
                 cooldown-ms (* 60000 (Math/pow 2 (min new-failure-count 5)))] ; max 32 min
             (assoc state
                    :available? false
                    :failure-count new-failure-count
                    :cooldown-until (+ (System/currentTimeMillis) cooldown-ms)))))
  (log/warn "Provider marked as failed" {:provider provider-key}))

(defn mark-provider-success
  "Mark a provider as successful and reset failure count"
  [registry provider-key]
  (swap! (:provider-states registry)
         update provider-key
         (fn [state]
           (assoc state
                  :available? true
                  :failure-count 0
                  :cooldown-until nil
                  :last-request-at (System/currentTimeMillis)))))

(defn check-cooldown
  "Check if provider is still in cooldown"
  [provider-state]
  (if-let [cooldown-until (:cooldown-until provider-state)]
    (< (System/currentTimeMillis) cooldown-until)
    false))

(defn get-available-providers
  "Get list of currently available providers"
  [registry]
  (let [states @(:provider-states registry)]
    (->> states
         (filter (fn [[k state]]
                   (and (available? (get-in registry [:providers k]))
                        (not (check-cooldown state)))))
         (map first))))

;; ============================================================================
;; PROVIDER SELECTION
;; ============================================================================

(defn select-provider
  "Select best available provider, preferring primary then fallbacks"
  [registry]
  (let [available (set (get-available-providers registry))
        primary (:primary registry)
        fallbacks (:fallbacks registry)]
    (cond
      ;; Primary available
      (contains? available primary)
      primary

      ;; Try fallbacks in order
      :else
      (first (filter available fallbacks)))))

;; ============================================================================
;; CHAT WITH FAILOVER
;; ============================================================================

(defn chat-with-failover
  "Send chat request with automatic failover to backup providers"
  [registry messages opts]
  (let [provider-key (select-provider registry)]
    (when-not provider-key
      (throw (ex-info "No available providers"
                      {:available (get-available-providers registry)})))

    (let [provider (get-in registry [:providers provider-key])]
      (log/debug "Using provider" {:provider provider-key})
      (try
        (let [response (chat provider messages opts)]
          (mark-provider-success registry provider-key)
          {:provider provider-key
           :response response
           :success? true})

        (catch Exception e
          (log/error e "Provider failed" {:provider provider-key})
          (mark-provider-failed registry provider-key)

          ;; Try to failover
          (let [next-provider (select-provider registry)]
            (if next-provider
              (do
                (log/debug "Failing over to provider" {:from provider-key :to next-provider})
                (chat-with-failover registry messages opts))
              (throw (ex-info "All providers failed"
                              {:last-error (.getMessage e)
                               :provider provider-key})))))))))

;; ============================================================================
;; REGISTRY QUERIES
;; ============================================================================

(defn get-provider-status
  "Get status of all providers"
  [registry]
  (let [states @(:provider-states registry)]
    (into {}
          (map (fn [[k state]]
                 [k {:available? (and (available? (get-in registry [:providers k]))
                                      (not (check-cooldown state)))
                     :failure-count (:failure-count state)
                     :cooldown? (check-cooldown state)
                     :cooldown-until (:cooldown-until state)}])
               states))))
