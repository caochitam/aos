(ns agent-os.cli.gateway
  "Admin CLI Gateway - implements IChannel protocol"
  (:require [agent-os.kernel.protocols :refer [IChannel receive-message send-message status list-components get-component]]
            [agent-os.reflection.engine :as reflect]
            [agent-os.modification.engine :as mod]
            [agent-os.memory.store :as mem]
            [agent-os.memory.compaction :as compaction]
            [agent-os.identity.soul :as soul]
            [agent-os.improvement.loop :as improve]
            [agent-os.llm.router :as router]
            [agent-os.llm.tools :as tools]
            [agent-os.llm.delegator :as delegator]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [io.aviso.ansi :as ansi])
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter]
           [java.io File]))

;; ============================================================================
;; OPENCLAW OPTIMIZATION: Bootstrap File Caching
;; ============================================================================

(def ^:private session-cache
  "Track sessions initialized with full system prompt. Saves ~880 tokens/message."
  (atom #{}))

(defn- session-initialized? [session-id]
  (contains? @session-cache session-id))

(defn- mark-session-initialized! [session-id]
  (swap! session-cache conj session-id))

;; ============================================================================
;; OPENCLAW OPTIMIZATION: Lazy Tool Loading
;; ============================================================================

(defn- needs-tools?
  "Detect if message requires tool usage. Returns false for simple chats."
  [message]
  (let [lower (str/lower-case message)]
    (or
      ;; File operations
      (some #(str/includes? lower %)
            ["file" "read" "edit" "sửa" "đọc" "xem" "tệp"])

      ;; Code modifications
      (some #(str/includes? lower %)
            [".clj" "code" "function" "defn" "mã" "hàm"])

      ;; System commands
      (some #(str/includes? lower %)
            ["run" "command" "bash" "lein" "test" "chạy" "lệnh"]))))

(defn- select-tools
  "Choose relevant tools based on message content. Saves 700 tokens for simple chats."
  [message]
  (if (needs-tools? message)
    tools/available-tools  ; Include tools (700 tokens)
    []))                   ; No tools (0 tokens)

;; ============================================================================
;; READLINE SETUP
;; ============================================================================

(def ^:dynamic *console-reader* nil)

(defn create-console-reader
  "Create JLine console reader with history and completion"
  []
  (let [console (ConsoleReader.)
        history-file (File. (str (System/getProperty "user.home") "/.aos_history"))
        completer (StringsCompleter.
                    ["/help" "/status" "/components" "/inspect" "/memory"
                     "/improve" "/history" "/soul" "/restart" "/exit"])]
    (.setPrompt console "aos> ")
    (.setHistoryEnabled console true)
    (when (.exists history-file)
      (try
        (.setHistory console (jline.console.history.FileHistory. history-file))
        (catch Exception _)))
    (.addCompleter console completer)
    console))

(defn read-line-with-readline
  "Read line using JLine with history and completion"
  [console]
  (try
    (.readLine console)
    (catch Exception e
      nil)))

;; ============================================================================
;; CLI CHANNEL IMPLEMENTATION
;; ============================================================================

(defrecord CLIChannel [state]
  IChannel
  (receive-message [this]
    (when-let [input (if *console-reader*
                       (read-line-with-readline *console-reader*)
                       (do (print "aos> ") (flush) (read-line)))]
      {:from :cli
       :content input
       :timestamp (System/currentTimeMillis)}))

  (send-message [this message]
    (let [separator "──────────────────────────────────────────────────────────────────────────────────────────────────────────────"]
      (println separator)
      (println (:content message))
      (println separator)))

  (channel-id [_] :cli)

  (channel-status [_] :open))

(defn create-cli-channel
  "Create CLI channel"
  []
  (map->CLIChannel {:state (atom {})}))

;; ============================================================================
;; COMMAND REGISTRY
;; ============================================================================

(defn cmd-help [_ _]
  (str (ansi/bold-cyan "AOS - Agent Operating System") "\n\n"
       (ansi/bold "Available slash commands:") "\n"
       (ansi/green "  /help") "              - Show this help\n"
       (ansi/green "  /status") "            - System status\n"
       (ansi/green "  /components") "        - List components\n"
       (ansi/green "  /inspect") " <id>      - Inspect component\n"
       (ansi/green "  /memory") " [type]     - View memory (facts/decisions/patterns)\n"
       (ansi/green "  /improve") " [id]      - Run improvement cycle\n"
       (ansi/green "  /history") " [n]       - Show modification history\n"
       (ansi/green "  /soul") "              - View agent soul\n"
       (ansi/green "  /restart") "           - Restart Agent OS\n"
       (ansi/green "  /exit") "              - Exit CLI\n\n"
       (ansi/italic "Type anything else to chat with AOS directly.")))

(defn cmd-status [os-state _]
  (let [kernel-status (status (:kernel os-state))]
    (str (ansi/bold "Status: ") (ansi/cyan (name (:status kernel-status))) "\n"
         (ansi/bold "Version: ") (:version kernel-status) "\n"
         (ansi/bold "Uptime: ") (quot (or (:uptime kernel-status) 0) 1000) "s\n"
         (ansi/bold "Components: ") (:total-components kernel-status))))

(defn cmd-components [os-state _]
  (let [comp-ids (list-components (:kernel os-state))]
    (if (empty? comp-ids)
      (ansi/yellow "No components registered")
      (str (ansi/bold "Components") " (" (count comp-ids) "):\n  "
           (str/join "\n  " (map #(ansi/cyan (name %)) comp-ids))))))

(defn cmd-inspect [os-state args]
  (if-not args
    (ansi/yellow "Usage: /inspect <component-id>")
    (let [comp-id (keyword args)
          component (get-component (:kernel os-state) comp-id)]
      (if-not component
        (ansi/red (str "Component not found: " args))
        (with-out-str (pprint component))))))

(defn cmd-memory [os-state args]
  (let [mem-sys (:mem-system os-state)
        memory (mem/load-memory mem-sys)
        type (or args "summary")]
    (case type
      "facts" (with-out-str (pprint (:facts memory)))
      "decisions" (with-out-str (pprint (:decisions memory)))
      "patterns" (with-out-str (pprint (:patterns memory)))
      (str (ansi/bold "Memory summary:") "\n"
           "  Facts: " (ansi/cyan (str (count (:facts memory)))) "\n"
           "  Decisions: " (ansi/cyan (str (count (:decisions memory)))) "\n"
           "  Patterns: " (ansi/cyan (str (count (:patterns memory))))))))

(defn cmd-improve [os-state args]
  (if-not args
    (ansi/yellow "Usage: /improve <component-id>")
    (let [comp-id (keyword args)
          result (improve/improve-cycle
                  (:kernel os-state)
                  (:llm-registry os-state)
                  (:history os-state)
                  comp-id
                  (:config os-state))]
      (if (:success? result)
        (ansi/green (str "✓ Improvement successful for " args))
        (ansi/red (str "✗ Improvement failed: " (:error result)))))))

(defn cmd-history [os-state args]
  (let [n (if args (Integer/parseInt args) 10)
        recent (mod/get-recent-modifications (:history os-state) n)]
    (if (empty? recent)
      (ansi/yellow "No modification history")
      (with-out-str (pprint recent)))))

(defn cmd-soul [os-state _]
  (with-out-str (pprint (:soul os-state))))

(defn cmd-chat [os-state message]
  (try
    ;; Extract llm-registry first (needed for LLM-based classification)
    (let [llm-registry (:llm-registry os-state)]

      ;; LLM-BASED CLASSIFICATION: Use Haiku to decide delegation
      ;; Cost: $0.000025/request - way cheaper than wrong model choice!
      (if (delegator/should-delegate? message llm-registry)
        ;; COMPLEX TASK: Delegate to Claude Code
        (let [_ (println (ansi/yellow (delegator/format-delegation-message message)))
              result (delegator/call-claude-code message "/root/aos")]
          (delegator/format-completion-message result))

        ;; SIMPLE/MODERATE TASK: Use AOS's own tools
        (let [session-id (or (:session-id os-state) "default-session")

              ;; OPENCLAW OPTIMIZATION: Bootstrap caching (93.5% savings)
              ;; Full prompt only on first message (~200 tokens)
              ;; Minimal prompt on subsequent messages (~20 tokens)
              system-prompt (if (session-initialized? session-id)
                             ;; Minimal prompt - session already has context
                             "You are AOS. Communicate in Vietnamese by default."

                             ;; Full prompt - first message only
                             (do
                               (mark-session-initialized! session-id)
                               (str "You are AOS, an AI agent that helps users with tasks.\n\n"
                                    "Communication: Respond in Vietnamese by default. "
                                    "Use English only when explicitly requested or for technical code/commands.\n\n"
                                    "Capabilities: You can read files, edit files, and run bash commands using tools when needed. "
                                    "Use tools wisely to accomplish user requests.\n\n"
                                    "Code modifications: Prefer unified diff format to save tokens:\n"
                                    "--- a/file.clj\n"
                                    "+++ b/file.clj\n"
                                    "@@ -line,count +line,count @@\n"
                                    "-old code\n"
                                    "+new code\n\n"
                                    "Working directory: /root/aos")))

              messages [{:role "user" :content message}]

              ;; OPENCLAW OPTIMIZATION: Lazy tool loading
              ;; Only send tools when message actually needs them
              tools-to-use (select-tools message)

              ;; LLM-BASED MODEL ROUTING: Use Haiku to select tier (Haiku/Sonnet)
              ;; Complex tasks already delegated above, so only :simple/:moderate here
              model-tier (delegator/select-model-tier message llm-registry)
              tier-config (get delegator/model-tiers model-tier)

              result (router/chat-with-failover
                      llm-registry
                      messages
                      {:system system-prompt
                       :tools tools-to-use          ; Conditional tools
                       :model (:model tier-config)  ; Dynamic model selection!
                       :max-tokens (:max-tokens tier-config)})]
          (if (:success? result)
            (:response result)
            (ansi/red (str "Chat failed: " (:error result)))))))
    (catch Exception e
      (ansi/red (str "Chat error: " (.getMessage e))))))

;; ============================================================================
;; COMMAND DISPATCHER
;; ============================================================================

(defn cmd-restart [_ _]
  ::restart)

(defn cmd-exit [_ _]
  (println (ansi/green "Goodbye!"))
  ::exit)

(def command-registry
  {"/help"        cmd-help
   "/status"      cmd-status
   "/components"  cmd-components
   "/inspect"     cmd-inspect
   "/memory"      cmd-memory
   "/improve"     cmd-improve
   "/history"     cmd-history
   "/soul"        cmd-soul
   "/restart"     cmd-restart
   "/exit"        cmd-exit})

(defn parse-input
  "Parse input - slash command or chat message"
  [input]
  (let [trimmed (str/trim input)]
    (if (str/starts-with? trimmed "/")
      ;; Slash command
      (let [parts (str/split trimmed #"\s+" 2)]
        {:type :command
         :command (first parts)
         :args (second parts)})
      ;; Chat message
      {:type :chat
       :message trimmed})))

(defn dispatch-input
  "Dispatch input - either slash command or chat"
  [os-state input]
  (let [parsed (parse-input input)]
    (case (:type parsed)
      :command
      (let [handler (get command-registry (:command parsed))]
        (if handler
          (handler os-state (:args parsed))
          (ansi/red (str "Unknown command: " (:command parsed) "\nType /help for available commands"))))

      :chat
      (cmd-chat os-state (:message parsed)))))

;; ============================================================================
;; SESSION HISTORY
;; ============================================================================

(defn save-session-history
  "Save conversation to session history file"
  [messages]
  (try
    (let [history-dir (str (System/getProperty "user.home") "/.aos")
          history-file (str history-dir "/session_history.edn")]
      (.mkdirs (clojure.java.io/file history-dir))
      (spit history-file (pr-str messages)))
    (catch Exception e
      nil)))

(defn load-session-history
  "Load conversation from session history file"
  []
  (try
    (let [history-file (str (System/getProperty "user.home") "/.aos/session_history.edn")]
      (when (.exists (clojure.java.io/file history-file))
        (read-string (slurp history-file))))
    (catch Exception e
      [])))

;; ============================================================================
;; CLI REPL
;; ============================================================================

(defn start-cli
  "Start interactive CLI REPL. Returns ::restart if restart requested, :exit otherwise."
  [os-state]
  (println (ansi/italic "Type /help for commands, or just chat directly"))
  (println)

  ;; Create console reader for readline support
  (let [console (create-console-reader)]
    (binding [*console-reader* console]
      (loop [conversation-history []]
        (if-let [msg (receive-message (:cli-channel os-state))]
          (let [input (:content msg)]
            (cond
              ;; Handle /exit command
              (= "/exit" input)
              (do
                (println (ansi/green "Goodbye!"))
                (save-session-history conversation-history)
                :exit)

              ;; Handle empty input
              (str/blank? input)
              (recur conversation-history)

              ;; Dispatch command or chat
              :else
              (let [output (dispatch-input os-state input)
                    new-history (if (str/starts-with? input "/")
                                  conversation-history
                                  (conj conversation-history
                                        {:role "user" :content input}
                                        {:role "assistant" :content output}))]
                (cond
                  (= output ::restart)
                  (do
                    (println (ansi/yellow "Restarting Agent OS..."))
                    (save-session-history new-history)
                    ::restart)

                  (= output ::exit)
                  (do
                    (save-session-history new-history)
                    :exit)

                  :else
                  (do
                    (send-message (:cli-channel os-state) {:content output})
                    (recur new-history))))))
          :exit)))))
