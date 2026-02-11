(ns agent-os.protocols
  "Application-level protocols for AOS components")

;; ============================================================================
;; Progress Reporting Protocol
;; ============================================================================
;; Channel-agnostic progress reporting for long-running operations
;; Supports CLI, messaging bots, web interfaces, etc.

(defprotocol IProgressReporter
  "Abstraction for progress reporting across different communication channels.

   Different channels have different constraints:
   - CLI: Can update frequently (every 1-5s), real-time streaming OK
   - Messaging (Telegram/Slack): Must batch updates (10-15s), avoid spam
   - Web: Can use SSE/WebSocket for real-time, or polling for simple

   Implementations control update frequency and formatting."

  (report-start [this message]
    "Report start of operation. Called once at beginning.
     message: Description of what's starting")

  (report-progress [this status]
    "Report progress update. May be called frequently.
     Implementation should throttle based on channel constraints.
     status: Current status description or event")

  (report-complete [this result]
    "Report completion of operation. Called once at end.
     result: Final result or summary"))
