(ns agent-os.setup.interactive
  "Interactive setup for API keys and configuration"
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io Console]))

;; ============================================================================
;; CONSOLE I/O
;; ============================================================================

(defn read-line-safe
  "Read a line from stdin safely"
  []
  (try
    (read-line)
    (catch Exception _ nil)))

(defn read-password
  "Read password/API key without echoing (if console available)"
  []
  (if-let [console (System/console)]
    (String. (.readPassword console))
    (do
      (println "⚠ Warning: Input will be visible (no console available)")
      (read-line-safe))))

(defn print-header
  "Print a formatted header"
  [text]
  (println)
  (println "==============================================")
  (println (str "   " text))
  (println "==============================================")
  (println))

;; ============================================================================
;; API KEY VALIDATION
;; ============================================================================

(defn valid-api-key?
  "Check if API key has valid Anthropic format"
  [key]
  (and (string? key)
       (not (str/blank? key))
       (str/starts-with? key "sk-ant-")
       (> (count key) 20)))

(defn get-home-dir
  "Get user home directory"
  []
  (System/getProperty "user.home"))

(defn get-shell-rc-file
  "Detect which shell RC file to use"
  []
  (let [home (get-home-dir)
        shell (or (System/getenv "SHELL") "/bin/bash")]
    (cond
      (str/includes? shell "zsh") (str home "/.zshrc")
      (str/includes? shell "bash") (str home "/.bashrc")
      :else (str home "/.bashrc"))))

;; ============================================================================
;; SETUP METHODS
;; ============================================================================

(defn backup-file
  "Create a backup of a file"
  [file-path]
  (let [backup-path (str file-path ".backup." (System/currentTimeMillis))]
    (try
      (io/copy (io/file file-path) (io/file backup-path))
      (println (str "✓ Backed up to: " backup-path))
      backup-path
      (catch Exception e
        (println (str "⚠ Could not backup file: " (.getMessage e)))
        nil))))

(defn append-to-file
  "Append text to a file"
  [file-path text]
  (try
    (spit file-path text :append true)
    true
    (catch Exception e
      (println (str "❌ Error writing to file: " (.getMessage e)))
      false)))

(defn setup-bashrc-method
  "Add API key directly to .bashrc/.zshrc"
  [api-key]
  (let [rc-file (get-shell-rc-file)]
    (println (str "\n📝 Adding to " rc-file))

    ;; Backup first
    (when (.exists (io/file rc-file))
      (backup-file rc-file))

    ;; Append API key
    (let [content (str "\n"
                       "# AOS - Anthropic API Key (added " (java.time.Instant/now) ")\n"
                       "export ANTHROPIC_API_KEY=\"" api-key "\"\n")]
      (if (append-to-file rc-file content)
        (do
          (println "✅ Successfully added to" rc-file)

          ;; IMPORTANT: Also set for CURRENT session so AOS can start immediately!
          (System/setProperty "ANTHROPIC_API_KEY" api-key)
          (println "✅ Activated for current session")
          (println)
          (println "🎉 Setup complete! Starting AOS now...")
          (println)
          (println "💡 Next time you open a terminal, API key will be automatically loaded.")
          true)
        false))))

(defn setup-secure-file-method
  "Create secure key file and load from .bashrc/.zshrc"
  [api-key]
  (let [key-file (str (get-home-dir) "/.anthropic_key")
        rc-file (get-shell-rc-file)]
    (println (str "\n📝 Creating secure key file: " key-file))

    ;; Create key file
    (try
      (spit key-file api-key)
      ;; Set permissions to 600 (owner read/write only)
      (.setReadable (io/file key-file) false false)
      (.setReadable (io/file key-file) true true)
      (.setWritable (io/file key-file) false false)
      (.setWritable (io/file key-file) true true)
      (println (str "✓ Created " key-file " with permissions 600"))

      ;; Backup bashrc
      (when (.exists (io/file rc-file))
        (backup-file rc-file))

      ;; Add loader to bashrc
      (let [content (str "\n"
                         "# AOS - Load Anthropic API Key from secure file (added " (java.time.Instant/now) ")\n"
                         "if [ -f ~/.anthropic_key ]; then\n"
                         "    export ANTHROPIC_API_KEY=\"$(cat ~/.anthropic_key)\"\n"
                         "fi\n")]
        (if (append-to-file rc-file content)
          (do
            (println (str "✓ Added loader to " rc-file))

            ;; IMPORTANT: Also set for CURRENT session so AOS can start immediately!
            (System/setProperty "ANTHROPIC_API_KEY" api-key)
            (println "✅ Activated for current session")
            (println)
            (println "🎉 Setup complete! Starting AOS now...")
            (println)
            (println "💡 Next time you open a terminal, API key will be automatically loaded.")
            true)
          false))

      (catch Exception e
        (println (str "❌ Error creating secure file: " (.getMessage e)))
        false))))

(defn setup-current-session-only
  "Set API key only for current session (temporary)"
  [api-key]
  (println "\n⚠ Setting API key for CURRENT SESSION ONLY")
  (println "   This will NOT persist after you close the terminal!")
  (println)
  ;; Set environment variable for current JVM process
  ;; Note: This doesn't actually set it for the shell, just for this process
  (System/setProperty "ANTHROPIC_API_KEY" api-key)
  (println "✅ API key set for current AOS session")
  (println "   (Will be lost when AOS exits)")
  true)

;; ============================================================================
;; INTERACTIVE SETUP FLOW
;; ============================================================================

(defn prompt-setup-method
  "Ask user which setup method they prefer"
  []
  (println "How would you like to save the API key?")
  (println)
  (println "1. Add to ~/.bashrc or ~/.zshrc (Recommended)")
  (println "   ✓ Permanent, loads automatically")
  (println "   ✓ Simple and reliable")
  (println)
  (println "2. Use secure file ~/.anthropic_key")
  (println "   ✓ Most secure (chmod 600)")
  (println "   ✓ Easy to rotate keys")
  (println "   ✓ Key stored separately")
  (println)
  (println "3. Current session only (Temporary)")
  (println "   ⚠ Will be lost when you exit AOS")
  (println "   ✓ Good for testing")
  (println)
  (println "4. Skip (I'll set it manually later)")
  (println)
  (print "Enter your choice (1-4) [default: 2]: ")
  (flush)

  (let [choice (or (read-line-safe) "2")]
    (case (str/trim choice)
      "1" :bashrc
      "2" :secure-file
      "3" :session-only
      "4" :skip
      "" :secure-file
      :secure-file)))

(defn prompt-api-key
  "Ask user for their API key"
  []
  (println)
  (println "Please enter your Anthropic API key:")
  (println "(Format: sk-ant-api03-...)")
  (println)
  (print "API Key: ")
  (flush)

  ;; Use read-line instead of read-password to show input
  (let [api-key (str/trim (read-line-safe))]
    (cond
      (str/blank? api-key)
      (do
        (println "❌ No API key provided")
        nil)

      (not (valid-api-key? api-key))
      (do
        (println "❌ Invalid API key format")
        (println "   Expected format: sk-ant-api03-...")
        nil)

      :else
      api-key)))

(defn run-interactive-setup
  "Run interactive API key setup"
  []
  (print-header "AOS First-Time Setup")

  (println "Welcome to AOS! 🚀")
  (println)
  (println "I noticed you don't have an ANTHROPIC_API_KEY set.")
  (println "Let's set that up now so AOS can use Claude API.")
  (println)
  (println "Don't have an API key yet?")
  (println "→ Get one at: https://console.anthropic.com/settings/keys")
  (println)

  ;; Ask if user wants to set up now
  (print "Would you like to set up your API key now? (Y/n): ")
  (flush)
  (let [response (or (read-line-safe) "y")]
    (when (contains? #{"y" "Y" "yes" "Yes" ""} (str/trim response))
      ;; Get API key
      (when-let [api-key (prompt-api-key)]
        (println)
        (println "✓ API key accepted")
        (println (str "  Prefix: " (subs api-key 0 20) "..."))

        ;; Get setup method
        (let [method (prompt-setup-method)]
          (case method
            :bashrc
            (setup-bashrc-method api-key)

            :secure-file
            (setup-secure-file-method api-key)

            :session-only
            (setup-current-session-only api-key)

            :skip
            (do
              (println)
              (println "⚠ Setup skipped")
              (println "  You can set it manually later with:")
              (println "  export ANTHROPIC_API_KEY=\"sk-ant-api03-...\"")
              (println)
              false)))))))

;; ============================================================================
;; CHECK AND SETUP
;; ============================================================================

(defn check-api-key
  "Check if API key is set, return it or nil"
  []
  (or (System/getenv "ANTHROPIC_API_KEY")
      (System/getProperty "ANTHROPIC_API_KEY")))

(defn ensure-api-key-configured
  "Ensure API key is configured, run interactive setup if not"
  []
  (if-let [existing-key (check-api-key)]
    existing-key
    (do
      (when (run-interactive-setup)
        ;; Check again after setup
        (check-api-key)))))
