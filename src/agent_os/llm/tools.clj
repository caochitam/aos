(ns agent-os.llm.tools
  "Tool system for LLM - enables Claude to read/edit files and run commands"
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; ============================================================================
;; TOOL DEFINITIONS (matches Anthropic API format)
;; ============================================================================

(def tool-read-file
  {:name "read_file"
   :description "Read the contents of a file with line numbers. Use this to examine code before making changes."
   :input_schema {:type "object"
                  :properties {:file_path {:type "string"
                                          :description "Absolute path to the file to read"}
                               :start_line {:type "integer"
                                           :description "Optional starting line number (1-indexed)"}
                               :end_line {:type "integer"
                                         :description "Optional ending line number (1-indexed)"}}
                  :required ["file_path"]}})

(def tool-edit-file
  {:name "edit_file"
   :description "Edit a file by replacing old text with new text. The old_string must match exactly (including whitespace). Always read the file first to see the exact content.

   OPTIMIZATION: For code changes, prefer unified diff format in your response:
   --- a/file.clj
   +++ b/file.clj
   @@ -line,count +line,count @@
   -old line
   +new line

   This saves ~90% tokens compared to showing full file content."
   :input_schema {:type "object"
                  :properties {:file_path {:type "string"
                                          :description "Absolute path to the file to edit"}
                               :old_string {:type "string"
                                           :description "The exact text to replace (must match exactly)"}
                               :new_string {:type "string"
                                           :description "The new text to insert"}}
                  :required ["file_path" "old_string" "new_string"]}})

(def tool-bash
  {:name "bash"
   :description "Execute a bash command. Use this to run tests, check syntax, or perform other operations. The command runs in the /root/aos directory."
   :input_schema {:type "object"
                  :properties {:command {:type "string"
                                        :description "The bash command to execute"}}
                  :required ["command"]}})

(def available-tools
  [tool-read-file
   tool-edit-file
   tool-bash])

;; ============================================================================
;; TOOL EXECUTION
;; ============================================================================

(defn execute-read-file
  "Read file contents with line numbers"
  [{:keys [file_path start_line end_line]}]
  (try
    (let [lines (str/split-lines (slurp file_path))
          start (dec (or start_line 1))
          end (or end_line (count lines))
          selected-lines (subvec (vec lines) start (min end (count lines)))
          numbered (map-indexed (fn [idx line]
                                 (str (format "%4d" (+ start idx 1)) "│" line))
                               selected-lines)]
      {:success true
       :output (str/join "\n" numbered)})
    (catch Exception e
      {:success false
       :error (str "Failed to read file: " (.getMessage e))})))

(defn execute-edit-file
  "Edit file by replacing exact string match"
  [{:keys [file_path old_string new_string]}]
  (try
    (let [content (slurp file_path)]
      (if (str/includes? content old_string)
        (do
          (spit file_path (str/replace content old_string new_string))
          {:success true
           :output (str "Successfully replaced text in " file_path)})
        {:success false
         :error (str "old_string not found in file. Make sure it matches exactly (including whitespace).")}))
    (catch Exception e
      {:success false
       :error (str "Failed to edit file: " (.getMessage e))})))

(defn execute-bash
  "Execute bash command safely"
  [{:keys [command]}]
  (try
    (log/debug "Executing bash command" {:command command})
    (let [result (shell/sh "bash" "-c" command :dir "/root/aos")
          output (str (:out result) (:err result))]
      (if (zero? (:exit result))
        {:success true
         :output output
         :exit_code 0}
        {:success false
         :error output
         :exit_code (:exit result)}))
    (catch Exception e
      {:success false
       :error (str "Failed to execute command: " (.getMessage e))
       :exit_code 1})))

(defn execute-tool
  "Execute a tool call and return the result"
  [tool-name tool-input]
  (log/debug "Executing tool" {:tool tool-name :input tool-input})
  (case tool-name
    "read_file" (execute-read-file tool-input)
    "edit_file" (execute-edit-file tool-input)
    "bash" (execute-bash tool-input)
    {:success false
     :error (str "Unknown tool: " tool-name)}))

(defn format-tool-result
  "Format tool execution result for Claude API"
  [tool-use-id result]
  {:type "tool_result"
   :tool_use_id tool-use-id
   :content (if (:success result)
              (or (:output result) "Success")
              (str "Error: " (or (:error result) "Unknown error")))})
