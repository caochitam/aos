(ns agent-os.identity.soul
  "Identity and soul engine - personality as data, inspired by OpenClaw"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]))

;; ============================================================================
;; SOUL - PERSONALITY & BEHAVIORAL FRAMEWORK
;; ============================================================================

(defrecord Soul [soul-id
                 personality
                 boundaries
                 goals
                 evolution-history
                 created-at])

(defn create-soul
  "Create a new soul with personality traits"
  [soul-id & {:keys [traits communication-style risk-tolerance goals boundaries]
              :or {traits #{:analytical :cautious}
                   communication-style :concise
                   risk-tolerance 0.3
                   goals [:self-improvement :stability :efficiency]
                   boundaries {:never-modify #{:kernel :safety-engine}
                               :require-approval #{:critical-components}
                               :max-autonomy-level :medium}}}]
  (map->Soul
    {:soul-id soul-id
     :personality {:traits (set traits)
                   :communication-style communication-style
                   :risk-tolerance risk-tolerance}
     :boundaries boundaries
     :goals (vec goals)
     :evolution-history []
     :created-at (System/currentTimeMillis)}))

(defn evolve-soul
  "Update soul based on experience - identity evolves over time"
  [soul change-description trigger]
  (let [new-risk (case trigger
                   :failed-modification
                   (max 0.1 (- (get-in soul [:personality :risk-tolerance]) 0.05))

                   :successful-modification
                   (min 0.9 (+ (get-in soul [:personality :risk-tolerance]) 0.02))

                   ;; Default: no change
                   (get-in soul [:personality :risk-tolerance]))

        evolution-entry {:timestamp (System/currentTimeMillis)
                         :change change-description
                         :trigger trigger
                         :old-risk-tolerance (get-in soul [:personality :risk-tolerance])
                         :new-risk-tolerance new-risk}]

    (-> soul
        (update :evolution-history conj evolution-entry)
        (assoc-in [:personality :risk-tolerance] new-risk))))

(defn get-system-prompt
  "Generate system prompt from soul/identity/user context"
  [soul identity user]
  (str "You are " (:display-name identity) ", " (:role identity) ".\n\n"
       "Your personality traits: " (pr-str (get-in soul [:personality :traits])) "\n"
       "Communication style: " (get-in soul [:personality :communication-style]) "\n"
       "Risk tolerance: " (get-in soul [:personality :risk-tolerance]) "\n"
       "Goals: " (pr-str (:goals soul)) "\n\n"
       "You NEVER modify: " (pr-str (get-in soul [:boundaries :never-modify])) "\n"
       "You require approval for: " (pr-str (get-in soul [:boundaries :require-approval])) "\n"
       "Maximum autonomy level: " (get-in soul [:boundaries :max-autonomy-level]) "\n\n"
       "User preferences: " (pr-str (:preferences user))))

;; ============================================================================
;; IDENTITY - PUBLIC PERSONA
;; ============================================================================

(defrecord Identity [display-name
                     role
                     created-at])

(defn create-identity
  "Create public-facing identity"
  [display-name role]
  (map->Identity
    {:display-name display-name
     :role role
     :created-at (System/currentTimeMillis)}))

;; ============================================================================
;; USER CONTEXT - USER PREFERENCES
;; ============================================================================

(defrecord UserContext [user-id
                        preferences
                        interaction-count
                        first-interaction])

(defn create-user-context
  "Create user context with preferences"
  [user-id & {:keys [approval-mode notification-level language]
              :or {approval-mode :critical-only
                   notification-level :important
                   language :vi}}]
  (map->UserContext
    {:user-id user-id
     :preferences {:approval-mode approval-mode
                   :notification-level notification-level
                   :language language}
     :interaction-count 0
     :first-interaction (System/currentTimeMillis)}))

;; ============================================================================
;; FILE PERSISTENCE
;; ============================================================================

(defn save-soul
  "Save soul to EDN file"
  [soul filepath]
  (try
    (let [file (io/file filepath)]
      (.mkdirs (.getParentFile file))
      (spit file (pr-str soul))
      (log/info "Soul saved" {:file filepath})
      true)
    (catch Exception e
      (log/error e "Failed to save soul" {:file filepath})
      false)))

(defn load-soul
  "Load soul from EDN file"
  [filepath]
  (try
    (when (.exists (io/file filepath))
      (let [data (edn/read-string (slurp filepath))]
        (map->Soul data)))
    (catch Exception e
      (log/error e "Failed to load soul" {:file filepath})
      nil)))

(defn save-identity
  "Save identity to EDN file"
  [identity filepath]
  (try
    (spit filepath (pr-str identity))
    (log/info "Identity saved" {:file filepath})
    true
    (catch Exception e
      (log/error e "Failed to save identity")
      false)))

(defn load-identity
  "Load identity from EDN file"
  [filepath]
  (try
    (when (.exists (io/file filepath))
      (map->Identity (edn/read-string (slurp filepath))))
    (catch Exception e
      (log/error e "Failed to load identity")
      nil)))
