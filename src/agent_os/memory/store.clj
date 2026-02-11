(ns agent-os.memory.store
  "File-first persistent memory - inspired by OpenClaw"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import [java.text SimpleDateFormat]
           [java.util Date]))

;; ============================================================================
;; FILE-FIRST MEMORY SYSTEM
;; ============================================================================

(defrecord MemorySystem [base-path memory-file daily-log-dir history-dir])

(defn create-memory-system
  "Create file-first memory system"
  [config]
  (let [base (get-in config [:memory :base-path] "data/")
        mem-file (get-in config [:memory :memory-file] "MEMORY.edn")
        daily-dir (get-in config [:memory :daily-log-dir] "memory/")
        hist-dir (get-in config [:memory :history-dir] "history/")]

    ;; Ensure directories exist
    (.mkdirs (io/file base))
    (.mkdirs (io/file base daily-dir))
    (.mkdirs (io/file base hist-dir))

    (map->MemorySystem
      {:base-path base
       :memory-file (str base mem-file)
       :daily-log-dir (str base daily-dir)
       :history-dir (str base hist-dir)})))

;; ============================================================================
;; EDN FILE I/O
;; ============================================================================

(defn read-edn-file
  "Read EDN file safely"
  [filepath]
  (try
    (when (.exists (io/file filepath))
      (with-open [r (io/reader filepath)]
        (edn/read-string (slurp r))))
    (catch Exception e
      (log/warn e "Failed to read EDN file" {:file filepath})
      nil)))

(defn write-edn-file
  "Write data to EDN file atomically"
  [filepath data]
  (try
    (let [file (io/file filepath)
          temp-file (io/file (str filepath ".tmp"))]
      (.mkdirs (.getParentFile file))
      (spit temp-file (pr-str data))
      (.renameTo temp-file file)
      true)
    (catch Exception e
      (log/error e "Failed to write EDN file" {:file filepath})
      false)))

;; ============================================================================
;; DURABLE MEMORY (MEMORY.edn)
;; ============================================================================

(defn load-memory
  "Load MEMORY.edn - long-term facts, decisions, patterns"
  [mem-system]
  (or (read-edn-file (:memory-file mem-system))
      {:facts []
       :decisions []
       :patterns []}))

(defn save-memory
  "Save MEMORY.edn to disk"
  [mem-system memory-data]
  (write-edn-file (:memory-file mem-system) memory-data))

(defn remember-fact
  "Store a fact in durable memory"
  [mem-system category content source confidence]
  (let [memory (load-memory mem-system)
        fact {:id (keyword (str "fact-" (System/currentTimeMillis)))
              :category category
              :content content
              :source source
              :confidence confidence
              :timestamp (System/currentTimeMillis)}
        updated (update memory :facts conj fact)]
    (save-memory mem-system updated)
    (log/info "Fact remembered" {:id (:id fact) :category category})
    fact))

(defn remember-decision
  "Store a decision in durable memory"
  [mem-system decision reasoning]
  (let [memory (load-memory mem-system)
        dec-record {:id (keyword (str "decision-" (System/currentTimeMillis)))
                    :decision decision
                    :reasoning reasoning
                    :timestamp (System/currentTimeMillis)}
        updated (update memory :decisions conj dec-record)]
    (save-memory mem-system updated)
    (log/info "Decision remembered" {:id (:id dec-record)})
    dec-record))

(defn remember-pattern
  "Store a learned pattern in durable memory"
  [mem-system pattern learned-from success-rate]
  (let [memory (load-memory mem-system)
        pat-record {:id (keyword (str "pattern-" (System/currentTimeMillis)))
                    :pattern pattern
                    :learned-from learned-from
                    :success-rate success-rate
                    :timestamp (System/currentTimeMillis)}
        updated (update memory :patterns conj pat-record)]
    (save-memory mem-system updated)
    (log/info "Pattern remembered" {:id (:id pat-record)})
    pat-record))

(defn recall
  "Search memory for matching entries"
  [mem-system query-fn]
  (let [memory (load-memory mem-system)]
    {:facts (filter query-fn (:facts memory))
     :decisions (filter query-fn (:decisions memory))
     :patterns (filter query-fn (:patterns memory))}))

;; ============================================================================
;; DAILY LOGS (Append-only)
;; ============================================================================

(defn today-log-file
  "Get today's log file path"
  [mem-system]
  (let [date-format (SimpleDateFormat. "yyyy-MM-dd")
        today (.format date-format (Date.))]
    (str (:daily-log-dir mem-system) today ".edn")))

(defn append-daily-log
  "Append entry to today's daily log (append-only)"
  [mem-system entry]
  (let [log-file (today-log-file mem-system)
        existing (or (read-edn-file log-file) [])
        entry-with-ts (assoc entry :timestamp (System/currentTimeMillis))
        updated (conj existing entry-with-ts)]
    (write-edn-file log-file updated)
    (log/debug "Daily log entry appended" {:file log-file})
    entry-with-ts))

(defn read-daily-log
  "Read a specific daily log"
  [mem-system date-string]
  (let [log-file (str (:daily-log-dir mem-system) date-string ".edn")]
    (read-edn-file log-file)))

;; ============================================================================
;; CONTEXT COMPACTION (Flush before compaction)
;; ============================================================================

(defn estimate-context-size
  "Estimate token count (very rough approximation)"
  [messages]
  ;; Rough estimate: 1 token ~= 4 characters
  (quot (reduce + (map #(count (str %)) messages)) 4))

(defn should-compact?
  "Check if context should be compacted"
  [messages config]
  (let [max-tokens (get-in config [:memory :max-context-tokens] 150000)
        threshold (get-in config [:memory :flush-threshold] 0.8)
        current-tokens (estimate-context-size messages)
        limit (* max-tokens threshold)]
    (> current-tokens limit)))

(defn flush-context-to-memory
  "Flush important context to MEMORY.edn before compaction - OpenClaw pattern"
  [mem-system context-summary]
  (let [memory (load-memory mem-system)
        pattern-record {:id (keyword (str "ctx-flush-" (System/currentTimeMillis)))
                        :pattern context-summary
                        :action "Auto-flushed before context compaction"
                        :timestamp (System/currentTimeMillis)}
        updated (update memory :patterns conj pattern-record)]
    (save-memory mem-system updated)
    (append-daily-log mem-system
                      {:type :context-flush
                       :summary context-summary
                       :result :flushed})
    (log/info "Context flushed to memory before compaction")
    pattern-record))

(defn compact-context
  "Compact context by summarizing - placeholder for LLM-based summarization"
  [messages mem-system]
  ;; In real implementation, this would call LLM to summarize
  ;; For now, just keep recent messages
  (let [keep-count (quot (count messages) 2)]
    (take-last keep-count messages)))
