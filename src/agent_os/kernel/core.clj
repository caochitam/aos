(ns agent-os.kernel.core
  "Core kernel implementation - IMMUTABLE, protected from self-modification"
  (:require [agent-os.kernel.protocols :refer [IKernel IComponent
                                                component-id component-version
                                                component-code component-metadata]]
            [mount.core :as mount]
            [taoensso.timbre :as log]))

;; ============================================================================
;; CONSTANTS
;; ============================================================================

(def ^:const KERNEL_VERSION "0.1.0")
(def ^:const KERNEL_NAMESPACE "agent-os.kernel")

;; ============================================================================
;; COMPONENT RECORD
;; ============================================================================

(defrecord Component [id version code metadata modifiable? created-at]
  IComponent
  (component-id [_] id)
  (component-version [_] version)
  (component-code [_] code)
  (component-metadata [_] metadata))

(defn create-component
  "Create a new component"
  [id code & {:keys [purpose interfaces dependencies modifiable?]
              :or {modifiable? true}}]
  (map->Component
    {:id id
     :version 1
     :code code
     :metadata {:purpose purpose
                :interfaces (set interfaces)
                :dependencies (set dependencies)}
     :modifiable? modifiable?
     :created-at (System/currentTimeMillis)}))

;; ============================================================================
;; KERNEL RECORD
;; ============================================================================

(defrecord Kernel [state config]
  IKernel
  (boot [this cfg]
    (log/debug "Booting Agent OS kernel" KERNEL_VERSION)
    (swap! state assoc
           :status :running
           :boot-time (System/currentTimeMillis)
           :components {}
           :config cfg)
    (log/debug "Kernel boot complete"
               {:components (count (:components @state))})
    this)

  (shutdown [this]
    (log/debug "Shutting down Agent OS kernel")
    (swap! state assoc
           :status :shutdown
           :shutdown-time (System/currentTimeMillis))
    (log/info "Kernel shutdown complete")
    nil)

  (status [_]
    (let [{:keys [status boot-time components]} @state]
      {:status status
       :version KERNEL_VERSION
       :uptime (when boot-time (- (System/currentTimeMillis) boot-time))
       :total-components (count components)
       :modifiable-components (count (filter :modifiable? (vals components)))}))

  (get-component [_ component-id]
    (get-in @state [:components component-id]))

  (list-components [_]
    (keys (:components @state)))

  (register-component [this component]
    (let [comp-id (component-id component)
          comp-ns (namespace comp-id)]
      ;; Safety: cannot register components in kernel namespace
      (when (and comp-ns (= comp-ns KERNEL_NAMESPACE))
        (throw (ex-info "Cannot register component in protected kernel namespace"
                        {:component-id comp-id
                         :namespace comp-ns})))

      ;; Check if component already exists
      (when (get-in @state [:components comp-id])
        (log/warn "Component already exists, will be overwritten" {:id comp-id}))

      (swap! state assoc-in [:components comp-id] component)
      (log/info "Component registered" {:id comp-id :version (component-version component)})
      this)))

;; ============================================================================
;; KERNEL CREATION
;; ============================================================================

(defn create-kernel
  "Create a new kernel instance"
  [config]
  (->Kernel (atom {:status :created}) config))

;; ============================================================================
;; KERNEL QUERIES
;; ============================================================================

(defn get-system-state
  "Get complete system state snapshot"
  [kernel]
  (let [state-snapshot @(:state kernel)
        components (:components state-snapshot)]
    {:status (:status state-snapshot)
     :version KERNEL_VERSION
     :boot-time (:boot-time state-snapshot)
     :uptime (when-let [bt (:boot-time state-snapshot)]
               (- (System/currentTimeMillis) bt))
     :total-components (count components)
     :modifiable-components (count (filter :modifiable? (vals components)))
     :component-ids (keys components)}))

(defn kernel-healthy?
  "Check if kernel is in healthy running state"
  [kernel]
  (= :running (get-in @(:state kernel) [:status])))
