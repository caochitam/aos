#!/usr/bin/env lein exec
;; Test script to demonstrate AOS using tools through Claude API

(require '[agent-os.llm.claude :as claude]
         '[agent-os.llm.tools :as tools]
         '[agent-os.llm.protocols :as protocols])

(def api-key (System/getenv "ANTHROPIC_API_KEY"))

(when-not api-key
  (println "ERROR: ANTHROPIC_API_KEY not set")
  (System/exit 1))

(println "=== Testing AOS Self-Modification ===\n")

;; Create Claude provider
(def provider (claude/create-claude-provider api-key))

;; Test: Ask Claude to create a file using tools
(println "Sending request to Claude API with tools...")
(println "Request: 'Create a file /tmp/aos-test.txt with content: AOS is working!'\n")

(def messages
  [{:role "user"
    :content "Create a file /tmp/aos-test.txt with the text 'AOS is working!' using the bash tool."}])

(try
  (def response
    (protocols/chat provider messages
                    {:tools tools/available-tools
                     :max-tokens 2000}))

  (println "✅ Claude API Response:")
  (println response)
  (println)

  ;; Verify file was created
  (println "Verifying file was created by AOS tools:")
  (def file-content (slurp "/tmp/aos-test.txt"))
  (println "File content:" file-content)
  (println)
  (println "✅ SUCCESS! AOS used tools through Claude API to create the file!")

  (catch Exception e
    (println "❌ ERROR:" (.getMessage e))
    (println "Make sure ANTHROPIC_API_KEY is set correctly")
    (.printStackTrace e)))
