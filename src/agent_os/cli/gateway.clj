(ns agent-os.cli.gateway
  "Admin CLI Gateway - implements IChannel protocol"
  (:require [agent-os.kernel.protocols :refer [IChannel receive-message send-message status list-components get-component]]
            [agent-os.reflection.engine :as reflect]
            [agent-os.modification.engine :as mod]
            [agent-os.memory.store :as mem]
            [agent-os.identity.soul :as soul]
            [agent-os.improvement.loop :as improve]
            [agent-os.llm.router :as router]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [io.aviso.ansi :as ansi])
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter]
           [java.io File]))

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
    (println (:content message)))

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
    (let [llm-registry (:llm-registry os-state)
          soul (:soul os-state)
          identity (:identity os-state)
          user (:user os-state)
          system-prompt (soul/get-system-prompt soul identity user)
          messages [{:role "user" :content message}]
          result (router/chat-with-failover llm-registry messages {})]
      (if (:success? result)
        (:response result)
        (ansi/red (str "Chat failed: " (:error result)))))
    (catch Exception e
      (ansi/red (str "Chat error: " (.getMessage e))))))

;; ============================================================================
;; COMMAND DISPATCHER
;; ============================================================================

(defn cmd-restart [_ _]
  ::restart)

(def command-registry
  {"/help"        cmd-help
   "/status"      cmd-status
   "/components"  cmd-components
   "/inspect"     cmd-inspect
   "/memory"      cmd-memory
   "/improve"     cmd-improve
   "/history"     cmd-history
   "/soul"        cmd-soul
   "/restart"     cmd-restart})

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
  (println (ansi/bold-cyan "=== Agent OS - Self-Modifying AI ==="))
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

                  :else
                  (do
                    (send-message (:cli-channel os-state) {:content output})
                    (recur new-history))))))
          :exit)))))
