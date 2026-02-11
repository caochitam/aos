(ns agent-os.kernel.protocols
  "Core protocols for Agent OS - IMMUTABLE, part of protected kernel")

;; ============================================================================
;; KERNEL PROTOCOL
;; ============================================================================

(defprotocol IKernel
  "Core kernel protocol - cannot be modified by self-modification"
  (boot [this config] "Initialize kernel with configuration")
  (shutdown [this] "Graceful shutdown of the system")
  (status [this] "Return current kernel status map")
  (get-component [this component-id] "Get component by ID")
  (list-components [this] "List all component IDs")
  (register-component [this component] "Register a new component"))

;; ============================================================================
;; COMPONENT PROTOCOL
;; ============================================================================

(defprotocol IComponent
  "Protocol that all components must implement"
  (component-id [this] "Unique identifier for the component")
  (component-version [this] "Current version number")
  (component-code [this] "Source code as string or S-expression")
  (component-metadata [this] "Metadata map describing the component"))

;; ============================================================================
;; CHANNEL PROTOCOL
;; ============================================================================

(defprotocol IChannel
  "Communication channel protocol - allows AOS to add new channels via self-modification
   Initial implementation: CLI only
   Future implementations (self-created): HTTP, WebSocket, Telegram, etc."
  (receive-message [this] "Receive message from external source, returns {:from :content :timestamp}")
  (send-message [this message] "Send message to external destination")
  (channel-id [this] "Unique identifier for this channel")
  (channel-status [this] "Current status: :open, :closed, :error"))
