(ns agent-os.core
  "Agent OS entry point"
  (:require [agent-os.config :as config]
            [agent-os.kernel.core :as kernel]
            [agent-os.kernel.protocols :refer [boot]]
            [agent-os.memory.store :as mem]
            [agent-os.modification.engine :as mod]
            [agent-os.identity.soul :as soul]
            [agent-os.llm.claude :as claude]
            [agent-os.llm.router :as router]
            [agent-os.cli.gateway :as cli]
            [agent-os.security.vault :as vault]
            [agent-os.security.sanitizer :as sanitizer]
            [agent-os.setup.interactive :as setup]
            [agent-os.integrations.zalo.server :as zalo-server]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:gen-class))

;; ============================================================================
;; LOGGING CONFIGURATION
;; ============================================================================

(log/merge-config!
  {:middleware [(fn [data]
                  (if (and (:?err data)
                           (instance? java.io.IOException (:?err data))
                           (or (str/includes? (str (:?err data)) "xdg-open")
                               (str/includes? (str (:?err data)) "security")))
                    nil
                    ;; Sanitize all log data to prevent API key leakage
                    (update data :vargs
                            (fn [args]
                              (mapv sanitizer/safe-log-data args)))))]
   :min-level :error})

;; ============================================================================
;; SYSTEM INITIALIZATION
;; ============================================================================

(defn create-agent-os
  "Create and initialize Agent OS"
  [config]
  (log/debug "Initializing Agent OS")

  (let [k (kernel/create-kernel config)
        booted-kernel (boot k config)

        ;; Create subsystems
        mem-system (mem/create-memory-system config)
        history (mod/create-history)

        ;; Create secure vault and LLM provider
        ;; Check both config (from env) and System properties (from interactive setup)
        api-key (or (get-in config [:llm :api-key])
                    (System/getProperty "ANTHROPIC_API_KEY"))
        providers (if api-key
                    (do (log/debug "Using Claude API key authentication"
                                   {:key-length (count api-key)
                                    :key-prefix (subs api-key 0 (min 10 (count api-key)))})
                        [(claude/create-claude-provider api-key)])
                    (do (log/debug "No ANTHROPIC_API_KEY set. Chat and improve commands will not work.")
                        []))

        llm-registry (router/create-registry providers :primary :claude)

        ;; Create identity
        agent-soul (or (soul/load-soul "data/SOUL.edn")
                       (soul/create-soul :aos-agent))
        agent-identity (soul/create-identity "AOS Agent" "Trí tuệ nhân tạo tự sửa đổi")

        ;; Create user context with Vietnamese preference
        user-context (soul/create-user-context :cli-user :language :vi)

        ;; Create CLI channel
        cli-channel (cli/create-cli-channel)]

    (log/debug "Agent OS initialized")

    {:kernel booted-kernel
     :config config
     :mem-system mem-system
     :history history
     :llm-registry llm-registry
     :soul agent-soul
     :identity agent-identity
     :user user-context
     :cli-channel cli-channel}))

;; ============================================================================
;; MAIN ENTRY POINT
;; ============================================================================

(defn -main
  "Main entry point"
  [& args]
  ;; Check and setup API key if needed (interactive)
  (setup/ensure-api-key-configured)

  ;; Start directly without banner
  (loop []
    (let [result (try
                   (let [cfg (config/load-config)
                         os (create-agent-os cfg)

                         ;; Start Zalo webhook server if enabled
                         zalo-server (when (get-in cfg [:zalo :enabled])
                                      (try
                                        (log/info "Starting Zalo webhook server...")
                                        (let [zalo-config (:zalo cfg)
                                              zalo-context {:kernel (:kernel os)
                                                          :llm-registry (:llm-registry os)
                                                          :memory (:mem-system os)
                                                          :config cfg}
                                              server (zalo-server/start-server zalo-context zalo-config)]
                                          (log/info "✓ Zalo bot integration started")
                                          server)
                                        (catch Exception e
                                          (log/error "Failed to start Zalo server:" (.getMessage e))
                                          (println "⚠ Warning: Zalo bot could not start. Continuing with CLI only.")
                                          nil)))]

                     ;; Store server in os context for restart handling
                     (cli/start-cli (assoc os :zalo-server zalo-server)))

                   (catch Exception e
                     (log/error e "Agent OS failed to start")
                     (println "Error:" (.getMessage e))
                     :error))]

      (cond
        (= result ::cli/restart)
        (recur)

        (= result :error)
        (System/exit 1)

        :else
        nil))))
