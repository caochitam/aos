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
                    data))]
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

        ;; Create LLM provider
        api-key (get-in config [:llm :api-key])
        providers (if api-key
                    (do (log/debug "Using Claude API key authentication")
                        [(claude/create-claude-provider api-key)])
                    (do (log/debug "No ANTHROPIC_API_KEY set. Chat and improve commands will not work.")
                        []))

        llm-registry (router/create-registry providers :primary :claude)

        ;; Create identity
        agent-soul (or (soul/load-soul "data/SOUL.edn")
                       (soul/create-soul :aos-agent))
        agent-identity (soul/create-identity "AOS Agent" "Self-modifying AI")

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
     :cli-channel cli-channel}))

;; ============================================================================
;; MAIN ENTRY POINT
;; ============================================================================

(defn -main
  "Main entry point"
  [& args]
  (println "=== Agent OS - Self-Modifying AI Architecture ===")

  (loop []
    (let [result (try
                   (let [cfg (config/load-config)
                         os (create-agent-os cfg)]
                     (cli/start-cli os))

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
