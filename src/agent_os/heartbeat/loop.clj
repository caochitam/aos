(ns agent-os.heartbeat.loop
  "Heartbeat & proactive loop - autonomous invocation inspired by OpenClaw"
  (:require [agent-os.kernel.protocols :refer [status]]
            [agent-os.memory.store :as mem]
            [agent-os.modification.engine :as mod]
            [taoensso.timbre :as log]
            [clojure.core.async :as async :refer [go-loop <! >! timeout chan]]))

;; ============================================================================
;; HEARTBEAT CONFIGURATION
;; ============================================================================

(defn create-heartbeat-config
  "Create heartbeat configuration"
  [& {:keys [interval-ms standing-instructions enabled?]
      :or {interval-ms 1800000  ; 30 minutes
           standing-instructions []
           enabled? false}}]
  {:interval-ms interval-ms
   :standing-instructions standing-instructions
   :enabled? enabled?})

;; ============================================================================
;; HEALTH CHECKS
;; ============================================================================

(defn check-system-health
  "Perform system health checks"
  [kernel history]
  (let [kernel-status (status kernel)
        mod-stats (mod/modification-stats history)
        issues []]

    ;; Check modification success rate
    (let [issues (if (and (pos? (:total mod-stats))
                          (< (:success-rate mod-stats) 0.5))
                   (conj issues {:type :low-success-rate
                                 :severity :warning
                                 :detail (str "Success rate: " (:success-rate mod-stats))})
                   issues)]

      ;; Check if kernel is running
      (let [issues (if (not= :running (:status kernel-status))
                     (conj issues {:type :kernel-not-running
                                   :severity :critical
                                   :detail "Kernel is not in running state"})
                     issues)]

        {:status (if (empty? issues) :healthy :needs-attention)
         :issues issues
         :timestamp (System/currentTimeMillis)}))))

;; ============================================================================
;; HEARTBEAT TICK
;; ============================================================================

(defn heartbeat-tick
  "Execute one heartbeat iteration"
  [kernel mem-system history heartbeat-config]
  (try
    (log/debug "Heartbeat tick")

    ;; 1. Check standing instructions
    (doseq [instruction (:standing-instructions heartbeat-config)]
      (log/debug "Processing standing instruction" {:instruction instruction}))

    ;; 2. Run health checks
    (let [health (check-system-health kernel history)]

      ;; 3. Log to daily journal
      (mem/append-daily-log mem-system
                            {:type :heartbeat
                             :status (:status health)
                             :issues (:issues health)})

      ;; 4. Return health status
      health)

    (catch Exception e
      (log/error e "Heartbeat tick failed")
      {:status :error
       :error (.getMessage e)
       :timestamp (System/currentTimeMillis)})))

;; ============================================================================
;; HEARTBEAT LOOP
;; ============================================================================

(defn start-heartbeat
  "Start heartbeat loop using core.async"
  [kernel mem-system history heartbeat-config]
  (let [control-ch (chan)
        running? (atom true)]

    (log/info "Starting heartbeat" {:interval-ms (:interval-ms heartbeat-config)})

    (go-loop []
      (let [timeout-ch (timeout (:interval-ms heartbeat-config))
            [val port] (async/alts! [control-ch timeout-ch])]

        (cond
          ;; Stop signal received
          (= port control-ch)
          (do
            (log/info "Heartbeat stopped")
            (reset! running? false))

          ;; Timeout - run heartbeat tick
          (= port timeout-ch)
          (do
            (heartbeat-tick kernel mem-system history heartbeat-config)
            (when @running?
              (recur))))))

    ;; Return control channel for stopping
    {:control-ch control-ch
     :running? running?}))

(defn stop-heartbeat
  "Stop heartbeat loop"
  [heartbeat-state]
  (when-let [ch (:control-ch heartbeat-state)]
    (log/info "Stopping heartbeat")
    (async/>!! ch :stop)
    (async/close! ch)))
