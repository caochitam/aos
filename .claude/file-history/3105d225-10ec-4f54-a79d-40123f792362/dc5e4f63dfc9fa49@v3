;; ============================================================================
;; AGENT OS - Self-Modifying AI Architecture
;; ============================================================================
;; Kiến trúc cho phép AI Agent hiểu và chỉnh sửa chính nó
;; Được thiết kế để sử dụng với Claude Sonnet làm LLM engine
;;
;; Nguyên tắc thiết kế:
;; 1. Code as Data (Homoiconicity) - Mọi thứ đều là S-expressions
;; 2. Immutable State - Mọi thay đổi tạo version mới
;; 3. Self-Reflection - Agent có thể đọc và phân tích code của chính nó
;; 4. Safe Modification - Mọi thay đổi đều được validate và có thể rollback
;;
;; Tích hợp patterns từ OpenClaw:
;; 5. Autonomous Invocation - Agent tự kích hoạt (Heartbeat, Cron)
;; 6. Persistent State - File-first memory, files are source of truth
;; 7. Identity as Data - Soul, Identity, User context là data có thể evolve
;; 8. Dynamic Skills - Skills là EDN descriptors, load on demand
;; 9. Capability Security - Privilege separation, input sanitization
;; ============================================================================

(ns agent-os.core
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]))

;; ============================================================================
;; LAYER 1: CORE KERNEL (Immutable - Không thể sửa đổi)
;; ============================================================================
;; Đây là kernel tối thiểu, được bảo vệ khỏi self-modification

(def ^:const KERNEL_VERSION "0.1.0")

(defprotocol IKernel
  "Core kernel protocol - không thể sửa đổi"
  (boot [this] "Khởi động hệ thống")
  (shutdown [this] "Tắt hệ thống an toàn")
  (validate-modification [this change] "Kiểm tra tính hợp lệ của thay đổi")
  (apply-modification [this change] "Áp dụng thay đổi đã validate"))

;; ============================================================================
;; LAYER 2: ARCHITECTURE METADATA
;; ============================================================================
;; Mô tả kiến trúc của hệ thống dưới dạng data

(def architecture-schema
  "Schema mô tả kiến trúc hệ thống"
  {:system-id ::agent-os
   :version KERNEL_VERSION
   :components {}  ; Map of component-id -> component-spec
   :capabilities #{} ; Set of capabilities
   :constraints []}) ; List of constraints

(defn component-spec
  "Tạo spec cho một component"
  [id purpose interfaces dependencies & {:keys [modifiable? code]}]
  {:id id
   :purpose purpose
   :interfaces (set interfaces)
   :dependencies (set dependencies)
   :modifiable? (or modifiable? true)
   :code code  ; Actual Clojure code as data
   :created-at (System/currentTimeMillis)
   :version 1})

;; ============================================================================
;; LAYER 3: REFLECTION ENGINE
;; ============================================================================
;; Cho phép agent đọc và hiểu code của chính nó

(defn read-own-code
  "Đọc source code của component"
  [component-id architecture]
  (get-in architecture [:components component-id :code]))

(defn analyze-component
  "Phân tích cấu trúc của một component"
  [component-id architecture]
  (let [component (get-in architecture [:components component-id])
        code (:code component)]
    {:id component-id
     :purpose (:purpose component)
     :structure (if code
                  {:type (first code)
                   :name (second code)
                   :params (if (= 'defn (first code))
                             (nth code 2)
                             nil)
                   :body (if (= 'defn (first code))
                           (drop 3 code)
                           (rest code))}
                  nil)
     :dependencies (:dependencies component)
     :interfaces (:interfaces component)}))

(defn get-system-state
  "Lấy toàn bộ state hiện tại của hệ thống"
  [architecture]
  {:architecture architecture
   :components (keys (:components architecture))
   :total-components (count (:components architecture))
   :modifiable-components (count (filter :modifiable? 
                                          (vals (:components architecture))))})

;; ============================================================================
;; LAYER 4: LLM INTERFACE (Claude Sonnet)
;; ============================================================================
;; Interface để giao tiếp với Claude API

(defn llm-request-schema
  "Schema cho request tới Claude"
  []
  {:model "claude-sonnet-4-20250514"  ; Sử dụng Sonnet 4 mới nhất
   :max_tokens 4000
   :messages []})

(defn prepare-self-reflection-prompt
  "Chuẩn bị prompt cho agent tự phân tích"
  [architecture component-id]
  (let [component (analyze-component component-id architecture)
        system-state (get-system-state architecture)]
    {:role "user"
     :content (str 
               "Bạn là một AI Agent đang phân tích kiến trúc của chính mình.\n\n"
               "SYSTEM STATE:\n"
               (pr-str system-state) "\n\n"
               "ANALYZING COMPONENT: " component-id "\n"
               (pr-str component) "\n\n"
               "Hãy phân tích component này và đề xuất cải tiến nếu cần.")}))

(defn prepare-modification-prompt
  "Chuẩn bị prompt để agent tự sửa đổi code"
  [architecture component-id proposed-change reason]
  (let [current-code (read-own-code component-id architecture)]
    {:role "user"
     :content (str
               "Bạn đang sửa đổi component: " component-id "\n\n"
               "CURRENT CODE:\n"
               (pr-str current-code) "\n\n"
               "PROPOSED CHANGE:\n"
               proposed-change "\n\n"
               "REASON:\n"
               reason "\n\n"
               "Hãy tạo NEW CODE dưới dạng Clojure S-expression."
               "Response phải là VALID Clojure code có thể eval.")}))

;; ============================================================================
;; LAYER 5: MODIFICATION ENGINE
;; ============================================================================
;; Cho phép agent sửa đổi code của chính nó một cách an toàn

(defn create-modification
  "Tạo một modification proposal"
  [component-id old-code new-code reason metadata]
  {:id (str "mod-" (System/currentTimeMillis))
   :component-id component-id
   :old-code old-code
   :new-code new-code
   :reason reason
   :metadata metadata
   :timestamp (System/currentTimeMillis)
   :status :proposed})

(defn validate-new-code
  "Validate code mới trước khi apply"
  [new-code]
  (try
    ;; Kiểm tra syntax bằng cách read code
    (read-string (pr-str new-code))
    ;; Kiểm tra xem có phải valid S-expression không
    (when-not (list? new-code)
      (throw (ex-info "Code must be a valid S-expression" {:code new-code})))
    {:valid? true :code new-code}
    (catch Exception e
      {:valid? false 
       :error (.getMessage e)
       :code new-code})))

(defn apply-modification-safe
  "Áp dụng modification với rollback capability"
  [architecture modification]
  (let [{:keys [component-id new-code]} modification
        validation (validate-new-code new-code)]
    (if (:valid? validation)
      (let [old-component (get-in architecture [:components component-id])
            new-component (assoc old-component
                                :code new-code
                                :version (inc (:version old-component))
                                :modified-at (System/currentTimeMillis)
                                :previous-version (:code old-component))]
        {:success? true
         :architecture (assoc-in architecture 
                                 [:components component-id] 
                                 new-component)
         :rollback-data old-component})
      {:success? false
       :error (:error validation)
       :architecture architecture})))

(defn rollback-modification
  "Rollback về version trước"
  [architecture component-id]
  (let [component (get-in architecture [:components component-id])
        previous-code (:previous-version component)]
    (if previous-code
      (assoc-in architecture 
                [:components component-id :code] 
                previous-code)
      architecture)))

;; ============================================================================
;; LAYER 6: LEARNING & MEMORY
;; ============================================================================
;; Lưu trữ lịch sử modifications và học từ chúng

(defn modification-history
  "Lưu trữ lịch sử các modifications"
  []
  (atom {:modifications []
         :successful []
         :failed []}))

(defn record-modification
  "Ghi lại một modification"
  [history modification result]
  (swap! history update 
         (if (:success? result) :successful :failed)
         conj
         (assoc modification :result result)))

(defn analyze-modification-patterns
  "Phân tích patterns từ modification history"
  [history]
  (let [all-mods (:modifications @history)]
    {:total-modifications (count all-mods)
     :successful (count (:successful @history))
     :failed (count (:failed @history))
     :success-rate (if (pos? (count all-mods))
                     (/ (count (:successful @history)) 
                        (count all-mods))
                     0)
     :frequent-modifications (frequencies 
                              (map :component-id all-mods))}))

;; ============================================================================
;; LAYER 7: SELF-IMPROVEMENT LOOP
;; ============================================================================
;; Main loop cho phép agent tự cải thiện

(defn self-improvement-cycle
  "Một cycle của self-improvement"
  [architecture llm-client history]
  ;; 1. REFLECTION: Phân tích state hiện tại
  (let [system-state (get-system-state architecture)
        
        ;; 2. IDENTIFY: Tìm component cần cải thiện
        target-component (first (keys (:components architecture)))
        
        ;; 3. ANALYZE: Phân tích component
        analysis (analyze-component target-component architecture)
        
        ;; 4. PROPOSE: Đề xuất modification (gọi Claude API)
        ;; (Simplified - trong thực tế sẽ gọi Claude API thật)
        proposed-change {:type :optimization
                        :description "Improve performance"}
        
        ;; 5. VALIDATE: Kiểm tra modification
        modification (create-modification 
                      target-component
                      (:code analysis)
                      '(defn improved-fn [x] (* x 2)) ; Example new code
                      "Performance improvement"
                      proposed-change)
        
        ;; 6. APPLY: Áp dụng nếu valid
        result (apply-modification-safe architecture modification)]
    
    ;; 7. RECORD: Ghi lại kết quả
    (record-modification history modification result)
    
    ;; 8. RETURN: Trả về architecture mới
    (if (:success? result)
      (:architecture result)
      architecture)))

;; ============================================================================
;; LAYER 8: SAFETY & CONSTRAINTS
;; ============================================================================
;; Đảm bảo agent không tự phá hủy hoặc vi phạm constraints

(defn safety-check
  "Kiểm tra safety constraints"
  [architecture modification]
  (let [{:keys [component-id]} modification
        component (get-in architecture [:components component-id])]
    (cond
      ;; Không được sửa kernel
      (= component-id :kernel)
      {:safe? false :reason "Cannot modify kernel"}
      
      ;; Không được sửa component non-modifiable
      (false? (:modifiable? component))
      {:safe? false :reason "Component is not modifiable"}
      
      ;; Kiểm tra dependencies
      (not (every? #(contains? (:components architecture) %)
                   (:dependencies component)))
      {:safe? false :reason "Missing dependencies"}
      
      :else
      {:safe? true})))

;; ============================================================================
;; LAYER 9: IDENTITY & SOUL ENGINE (Inspired by OpenClaw)
;; ============================================================================
;; Agent personality, behavioral framework, và user context
;; Triết lý: Identity là DATA, có thể evolve theo thời gian

(defn load-edn-file
  "Đọc EDN file từ disk - file is source of truth"
  [filepath]
  (try
    (read-string (slurp filepath))
    (catch Exception e
      (println "Warning: Could not load" filepath)
      nil)))

(defn save-edn-file
  "Ghi EDN data ra file"
  [filepath data]
  (spit filepath (pr-str data)))

(defn create-soul
  "Tạo Soul cho agent - personality & behavioral framework"
  [soul-id & {:keys [traits communication-style risk-tolerance goals]}]
  {:soul-id soul-id
   :personality {:traits (set (or traits [:analytical :cautious]))
                 :communication-style (or communication-style :concise)
                 :risk-tolerance (or risk-tolerance 0.3)}
   :boundaries {:never-modify #{:kernel :safety-engine}
                :require-approval #{:critical-components}
                :max-autonomy-level :medium}
   :goals (or goals [:self-improvement :stability :efficiency])
   :evolution-history []
   :created-at (System/currentTimeMillis)})

(defn evolve-soul
  "Cập nhật soul dựa trên experience - identity evolves over time"
  [soul change-description trigger]
  (-> soul
      (update :evolution-history conj
              {:timestamp (System/currentTimeMillis)
               :change change-description
               :trigger trigger})
      (update-in [:personality :risk-tolerance]
                 (fn [rt]
                   (case trigger
                     :failed-modification (max 0.1 (- rt 0.05))
                     :successful-modification (min 0.9 (+ rt 0.02))
                     rt)))))

(defn create-identity
  "Public-facing persona"
  [display-name role]
  {:display-name display-name
   :role role
   :created-at (System/currentTimeMillis)})

(defn create-user-context
  "User preferences và context"
  [user-id & {:keys [approval-mode notification-level language]}]
  {:user-id user-id
   :preferences {:approval-mode (or approval-mode :critical-only)
                 :notification-level (or notification-level :important)
                 :language (or language :vi)}
   :interaction-count 0
   :first-interaction (System/currentTimeMillis)})

;; ============================================================================
;; LAYER 6 ENHANCED: FILE-FIRST PERSISTENT MEMORY (Inspired by OpenClaw)
;; ============================================================================
;; Pattern từ OpenClaw: Files are source of truth, not the model's context
;; Sử dụng EDN thay vì Markdown (phù hợp Clojure homoiconicity)

(defn create-persistent-memory
  "Tạo persistent memory system - file-first approach"
  [data-dir]
  {:data-dir data-dir
   :memory-file (str data-dir "/MEMORY.edn")
   :daily-log-fn (fn [] (str data-dir "/memory/"
                             (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
                                      (java.util.Date.))
                             ".edn"))})

(defn load-durable-memory
  "Load MEMORY.edn - long-term facts, decisions, patterns"
  [mem-system]
  (or (load-edn-file (:memory-file mem-system))
      {:facts [] :decisions [] :patterns []}))

(defn save-durable-memory
  "Persist memory to file"
  [mem-system memory-data]
  (save-edn-file (:memory-file mem-system) memory-data))

(defn append-daily-log
  "Append entry to today's daily log (append-only)"
  [mem-system entry]
  (let [log-file ((:daily-log-fn mem-system))
        existing (or (load-edn-file log-file) [])]
    (save-edn-file log-file
                   (conj existing
                         (assoc entry :timestamp (System/currentTimeMillis))))))

(defn remember-fact
  "Lưu một fact vào durable memory"
  [mem-system fact-content source confidence]
  (let [memory (load-durable-memory mem-system)
        new-fact {:id (keyword (str "f-" (System/currentTimeMillis)))
                  :content fact-content
                  :source source
                  :confidence confidence
                  :timestamp (System/currentTimeMillis)}]
    (save-durable-memory mem-system
                         (update memory :facts conj new-fact))
    new-fact))

(defn remember-decision
  "Lưu một decision vào durable memory"
  [mem-system decision reason]
  (let [memory (load-durable-memory mem-system)
        new-decision {:id (keyword (str "d-" (System/currentTimeMillis)))
                      :decision decision
                      :reason reason
                      :timestamp (System/currentTimeMillis)}]
    (save-durable-memory mem-system
                         (update memory :decisions conj new-decision))
    new-decision))

(defn flush-context-to-memory
  "Flush important context to MEMORY.edn before context compaction
   Pattern từ OpenClaw: silent agentic turn to persist before compaction"
  [mem-system context-summary]
  (let [memory (load-durable-memory mem-system)]
    (save-durable-memory
      mem-system
      (update memory :patterns conj
              {:id (keyword (str "p-" (System/currentTimeMillis)))
               :pattern context-summary
               :action "Auto-flushed before context compaction"
               :timestamp (System/currentTimeMillis)}))
    (append-daily-log mem-system
                      {:type :context-flush
                       :summary context-summary
                       :result :flushed})))

;; ============================================================================
;; LAYER 10: HEARTBEAT & PROACTIVE LOOP (Inspired by OpenClaw)
;; ============================================================================
;; Agent tự động kiểm tra và hành động theo interval
;; Biến agent từ reactive thành proactive

(defn create-heartbeat-config
  "Tạo cấu hình heartbeat"
  [& {:keys [interval-ms checks]}]
  {:interval-ms (or interval-ms 1800000) ; 30 phút default
   :checks (or checks [:component-health
                        :pending-modifications
                        :memory-usage
                        :error-rate-spike
                        :scheduled-tasks])
   :actions {:on-issue-found :notify-and-propose-fix
             :on-all-healthy :silent
             :on-scheduled-task :execute}
   :standing-instructions
   ["Check if any component has error rate > 20%"
    "Review pending modification proposals older than 1 hour"
    "Flush important observations to MEMORY.edn before context compaction"
    "Update daily log with current system state summary"]})

(defn heartbeat-check
  "Một heartbeat cycle - kiểm tra system health"
  [architecture mem-system history]
  (let [system-state (get-system-state architecture)
        mod-patterns (analyze-modification-patterns history)
        health-result {:timestamp (System/currentTimeMillis)
                       :components-count (:total-components system-state)
                       :success-rate (:success-rate mod-patterns)
                       :issues []}]
    ;; Check error rate
    (let [issues (cond-> []
                   (< (:success-rate mod-patterns) 0.8)
                   (conj {:type :high-error-rate
                          :detail (str "Success rate: " (:success-rate mod-patterns))
                          :severity :warning})

                   (zero? (:total-components system-state))
                   (conj {:type :no-components
                          :detail "No components loaded"
                          :severity :critical}))]
      ;; Log to daily journal
      (append-daily-log mem-system
                        {:type :heartbeat
                         :summary (if (empty? issues)
                                    "All components healthy"
                                    (str (count issues) " issues found"))
                         :result (if (empty? issues) :healthy :issues-found)
                         :details issues})
      (assoc health-result :issues issues
                           :status (if (empty? issues) :healthy :needs-attention)))))

(defn start-heartbeat
  "Khởi động heartbeat loop"
  [architecture mem-system history heartbeat-config]
  (let [running (atom true)]
    (future
      (while @running
        (try
          (let [result (heartbeat-check architecture mem-system history)]
            (when (= (:status result) :needs-attention)
              (println "HEARTBEAT: Issues detected -" (:issues result))))
          (catch Exception e
            (println "HEARTBEAT ERROR:" (.getMessage e))))
        (Thread/sleep (:interval-ms heartbeat-config))))
    ;; Return control atom to stop heartbeat
    running))

(defn stop-heartbeat
  "Dừng heartbeat loop"
  [heartbeat-atom]
  (reset! heartbeat-atom false))

;; ============================================================================
;; LAYER 5 ENHANCED: DYNAMIC SKILL LOADING (Inspired by OpenClaw)
;; ============================================================================
;; Skills as data (EDN descriptors), loaded dynamically
;; Chỉ inject vào LLM prompt khi cần (tối ưu context window)

(defn skill-descriptor
  "Tạo skill descriptor - skills are data, not compiled code"
  [skill-id name description interfaces dependencies
   & {:keys [triggers permissions code auto-load?]}]
  {:skill-id skill-id
   :name name
   :description description
   :version 1
   :interfaces (set interfaces)
   :dependencies (set dependencies)
   :triggers (or triggers [])
   :permissions (or permissions #{})
   :code code
   :auto-load? (or auto-load? false)
   :modifiable? true
   :created-at (System/currentTimeMillis)})

(defn create-skill-registry
  "Tạo skill registry"
  []
  (atom {:skills {}
         :loaded #{}}))

(defn register-skill
  "Đăng ký skill vào registry"
  [registry skill]
  (swap! registry assoc-in [:skills (:skill-id skill)] skill))

(defn load-skill
  "Load skill vào runtime (mark as active)"
  [registry skill-id]
  (swap! registry update :loaded conj skill-id))

(defn unload-skill
  "Unload skill khỏi runtime (free context window)"
  [registry skill-id]
  (swap! registry update :loaded disj skill-id))

(defn get-active-skills
  "Lấy danh sách skills đang active - để inject vào LLM prompt"
  [registry]
  (let [{:keys [skills loaded]} @registry]
    (select-keys skills loaded)))

(defn find-skills-for-trigger
  "Tìm skills phù hợp cho trigger event"
  [registry trigger]
  (let [{:keys [skills]} @registry]
    (filter (fn [[_ skill]]
              (some #(= % trigger) (:triggers skill)))
            skills)))

;; ============================================================================
;; LAYER 8 ENHANCED: CAPABILITY-BASED SECURITY (Inspired by OpenClaw)
;; ============================================================================
;; Privilege separation + input sanitization + capability-based access

(defn create-permission-model
  "Tạo permission model cho hệ thống"
  []
  (atom {:component-permissions
         {:memory-manager #{:file-read :file-write}
          :reflection-engine #{:code-read :system-inspect}
          :modification-engine #{:code-eval :code-write :file-write}
          :kernel #{}}  ; Kernel immutable, không cần permission
         :trust-levels
         {:user :trusted
          :api :semi-trusted
          :web :untrusted
          :file :semi-trusted}}))

(defn check-permission
  "Kiểm tra permission trước khi thực thi"
  [perm-model component-id required-permission]
  (let [granted (get-in @perm-model [:component-permissions component-id])]
    (contains? granted required-permission)))

(defn sanitize-input
  "Sanitize input tại trust boundary - lesson from OpenClaw security issues"
  [input source trust-levels]
  (let [trust-level (get trust-levels source :untrusted)]
    (case trust-level
      :trusted input
      :semi-trusted {:sanitized true :data input :source source}
      :untrusted {:sanitized true
                  :data (str input) ; Force string conversion
                  :source source
                  :warning "Untrusted source - validate before use"})))

;; ============================================================================
;; EXAMPLE ARCHITECTURE
;; ============================================================================
;; Ví dụ về một architecture cụ thể

(def example-architecture
  (-> architecture-schema
      (assoc-in [:components :memory-manager]
                (component-spec
                 :memory-manager
                 "Quản lý bộ nhớ của agent"
                 #{:store :retrieve :search}
                 #{:vector-db}
                 :modifiable? true
                 :code '(defn memory-manager
                         [operation data]
                         (case operation
                           :store (store-memory data)
                           :retrieve (retrieve-memory data)
                           :search (search-memory data)))))
      
      (assoc-in [:components :reflection-engine]
                (component-spec
                 :reflection-engine
                 "Cho phép agent tự phân tích"
                 #{:analyze :reflect :introspect}
                 #{:memory-manager}
                 :modifiable? true
                 :code '(defn reflection-engine
                         [target]
                         (analyze-component target @architecture))))
      
      (assoc-in [:components :modification-engine]
                (component-spec
                 :modification-engine
                 "Thực hiện self-modification"
                 #{:validate :apply :rollback}
                 #{:reflection-engine :memory-manager}
                 :modifiable? true
                 :code '(defn modification-engine
                         [change]
                         (when (validate-modification change)
                           (apply-modification change)))))
      
      (assoc-in [:capabilities]
                #{:self-reflection
                  :self-modification
                  :learning
                  :rollback})
      
      (assoc-in [:constraints]
                [{:type :safety
                  :rule "Cannot modify kernel"}
                 {:type :dependencies
                  :rule "Must maintain dependency graph"}
                 {:type :validation
                  :rule "All modifications must pass validation"}])))

;; ============================================================================
;; USAGE EXAMPLES
;; ============================================================================

(comment
  ;; Initialize system
  (def arch example-architecture)
  (def history (modification-history))
  
  ;; Xem system state
  (get-system-state arch)
  ;; => {:architecture {...}, :components [...], :total-components 3, ...}
  
  ;; Phân tích một component
  (analyze-component :memory-manager arch)
  ;; => {:id :memory-manager, :purpose "...", :structure {...}, ...}
  
  ;; Tạo modification proposal
  (def mod (create-modification
            :memory-manager
            '(defn old-fn [x] x)
            '(defn new-fn [x] (* x 2))
            "Improve performance"
            {:type :optimization}))
  
  ;; Validate và apply
  (def result (apply-modification-safe arch mod))
  (:success? result) ;; => true
  
  ;; Record result
  (record-modification history mod result)
  
  ;; Analyze patterns
  (analyze-modification-patterns history)
  
  ;; Rollback nếu cần
  (def rolled-back (rollback-modification (:architecture result) 
                                          :memory-manager))
  
  ;; Run self-improvement cycle
  (def improved-arch (self-improvement-cycle arch nil history))
  )

;; ============================================================================
;; CLAUDE API INTEGRATION
;; ============================================================================
;; Integration với Claude Sonnet API (cần API key)

(defn call-claude-for-analysis
  "Gọi Claude API để phân tích component"
  [api-key architecture component-id]
  ;; Trong thực tế, sẽ gọi API thật
  ;; Đây là placeholder
  {:analysis "Component performs well"
   :suggestions ["Consider caching"]
   :confidence 0.85})

(defn call-claude-for-modification
  "Gọi Claude API để tạo code mới"
  [api-key architecture component-id change-description]
  ;; Trong thực tế, sẽ gọi API và parse response
  {:new-code '(defn improved-function [x] (* x 2))
   :explanation "Optimized for performance"
   :confidence 0.9})

;; ============================================================================
;; ADMIN CLI GATEWAY
;; ============================================================================
;; Giao diện dòng lệnh cho admin tương tác với AOS
;; Admin không cần biết Clojure - chỉ cần gõ commands
;;
;; Thiết kế: Command Router pattern
;; Input: "aos> status" -> parse -> dispatch -> execute -> format output

(def cli-banner
  "
   ╔═══════════════════════════════════════════╗
   ║     AOS - Agent OS CLI v0.1.0             ║
   ║     Self-Modifying AI Architecture        ║
   ╚═══════════════════════════════════════════╝
   Type 'help' for available commands.
  ")

(def cli-commands
  "Registry of all CLI commands"
  {:help
   {:description "Show available commands"
    :usage "help [command]"
    :category :system}

   :status
   {:description "Show system status overview"
    :usage "status"
    :category :system}

   :components
   {:description "List all components"
    :usage "components"
    :category :component}

   :inspect
   {:description "Inspect a component in detail"
    :usage "inspect <component-id>"
    :category :component}

   :analyze
   {:description "Analyze a component with Claude"
    :usage "analyze <component-id>"
    :category :component}

   :modify
   {:description "Propose a modification"
    :usage "modify <component-id> <description>"
    :category :modification}

   :history
   {:description "Show modification history"
    :usage "history [count]"
    :category :modification}

   :rollback
   {:description "Rollback a component to previous version"
    :usage "rollback <component-id>"
    :category :modification}

   :memory
   {:description "View or manage persistent memory"
    :usage "memory [facts|decisions|patterns|today]"
    :category :memory}

   :remember
   {:description "Save a fact to durable memory"
    :usage "remember <fact>"
    :category :memory}

   :soul
   {:description "View or edit agent soul"
    :usage "soul [view|traits|goals|boundaries]"
    :category :identity}

   :identity
   {:description "View agent identity"
    :usage "identity"
    :category :identity}

   :heartbeat
   {:description "Manage heartbeat"
    :usage "heartbeat [status|start|stop|run-once]"
    :category :proactive}

   :skills
   {:description "Manage skills"
    :usage "skills [list|load|unload] [skill-id]"
    :category :skill}

   :chat
   {:description "Chat with agent using natural language"
    :usage "chat <message>"
    :category :interaction}

   :improve
   {:description "Run one self-improvement cycle"
    :usage "improve [component-id]"
    :category :modification}

   :log
   {:description "View daily log"
    :usage "log [today|yesterday|YYYY-MM-DD]"
    :category :memory}

   :permissions
   {:description "View permission model"
    :usage "permissions [component-id]"
    :category :security}

   :shutdown
   {:description "Shutdown Agent OS safely"
    :usage "shutdown"
    :category :system}})

;; --- CLI Command Parsers ---

(defn parse-command
  "Parse raw input string into command + args"
  [input]
  (let [parts (clojure.string/split (clojure.string/trim input) #"\s+" 2)
        cmd (keyword (first parts))
        args (second parts)]
    {:command cmd :args args :raw input}))

;; --- CLI Command Handlers ---

(defn cli-help
  "Show help for commands"
  [args]
  (if args
    ;; Help for specific command
    (let [cmd (keyword args)]
      (if-let [info (get cli-commands cmd)]
        (str "  " (name cmd) " - " (:description info) "\n"
             "  Usage: " (:usage info))
        (str "  Unknown command: " args)))
    ;; General help
    (let [categories (group-by (fn [[_ v]] (:category v)) cli-commands)]
      (str
        "  Available Commands:\n"
        "  ─────────────────────────────────────\n"
        (clojure.string/join "\n"
          (for [[category cmds] (sort-by first categories)]
            (str "  [" (name category) "]\n"
                 (clojure.string/join "\n"
                   (for [[cmd info] (sort-by first cmds)]
                     (str "    " (format "%-14s" (name cmd))
                          (:description info)))))))))))

(defn cli-status
  "Show system status"
  [os-state]
  (let [{:keys [architecture mem-system history soul heartbeat-atom]} os-state
        sys (get-system-state architecture)
        patterns (analyze-modification-patterns history)]
    (str
      "  System Status\n"
      "  ─────────────────────────────────────\n"
      "  Version:        " KERNEL_VERSION "\n"
      "  Components:     " (:total-components sys) "\n"
      "  Modifiable:     " (:modifiable-components sys) "\n"
      "  Modifications:  " (:total-modifications patterns) "\n"
      "  Success Rate:   " (if (pos? (:total-modifications patterns))
                              (str (int (* 100 (:success-rate patterns))) "%")
                              "N/A") "\n"
      "  Heartbeat:      " (if (and heartbeat-atom @heartbeat-atom)
                              "RUNNING" "STOPPED") "\n"
      "  Soul:           " (name (or (:soul-id soul) :none)) "\n"
      "  Risk Tolerance: " (get-in soul [:personality :risk-tolerance] "N/A"))))

(defn cli-components
  "List all components"
  [architecture]
  (let [comps (:components architecture)]
    (str
      "  Components (" (count comps) ")\n"
      "  ─────────────────────────────────────\n"
      (clojure.string/join "\n"
        (for [[id comp] (sort-by first comps)]
          (str "  " (format "%-22s" (name id))
               "v" (:version comp) "  "
               (if (:modifiable? comp) "MODIFIABLE" "LOCKED")
               "  " (:purpose comp)))))))

(defn cli-inspect
  "Inspect a specific component"
  [architecture args]
  (if-not args
    "  Usage: inspect <component-id>"
    (let [comp-id (keyword args)
          analysis (analyze-component comp-id architecture)
          comp (get-in architecture [:components comp-id])]
      (if-not comp
        (str "  Component not found: " args)
        (str
          "  Component: " (name comp-id) "\n"
          "  ─────────────────────────────────────\n"
          "  Purpose:      " (:purpose comp) "\n"
          "  Version:      " (:version comp) "\n"
          "  Modifiable:   " (:modifiable? comp) "\n"
          "  Interfaces:   " (pr-str (:interfaces comp)) "\n"
          "  Dependencies: " (pr-str (:dependencies comp)) "\n"
          "  Created:      " (:created-at comp) "\n"
          "  Code:\n"
          "    " (pr-str (:code comp)))))))

(defn cli-memory
  "View persistent memory"
  [mem-system args]
  (let [memory (load-durable-memory mem-system)
        sub (or args "summary")]
    (case sub
      "facts"     (str "  Facts (" (count (:facts memory)) ")\n"
                       "  ─────────────────────────────────────\n"
                       (clojure.string/join "\n"
                         (for [f (:facts memory)]
                           (str "  [" (name (:id f)) "] " (:content f)
                                " (confidence: " (:confidence f) ")"))))
      "decisions" (str "  Decisions (" (count (:decisions memory)) ")\n"
                       "  ─────────────────────────────────────\n"
                       (clojure.string/join "\n"
                         (for [d (:decisions memory)]
                           (str "  [" (name (:id d)) "] " (:decision d)
                                "\n    Reason: " (:reason d)))))
      "patterns"  (str "  Patterns (" (count (:patterns memory)) ")\n"
                       "  ─────────────────────────────────────\n"
                       (clojure.string/join "\n"
                         (for [p (:patterns memory)]
                           (str "  [" (name (:id p)) "] " (:pattern p)
                                "\n    Action: " (:action p)))))
      ;; default: summary
      (str "  Memory Summary\n"
           "  ─────────────────────────────────────\n"
           "  Facts:     " (count (:facts memory)) "\n"
           "  Decisions: " (count (:decisions memory)) "\n"
           "  Patterns:  " (count (:patterns memory)) "\n"
           "  Use: memory [facts|decisions|patterns] for details"))))

(defn cli-remember
  "Save a fact to memory"
  [mem-system args]
  (if-not args
    "  Usage: remember <fact>"
    (let [fact (remember-fact mem-system args :admin 0.8)]
      (str "  Remembered: " (:content fact) "\n"
           "  ID: " (name (:id fact))))))

(defn cli-soul-view
  "View agent soul"
  [soul args]
  (let [sub (or args "view")]
    (case sub
      "traits"     (str "  Traits: " (pr-str (get-in soul [:personality :traits])))
      "goals"      (str "  Goals: " (pr-str (:goals soul)))
      "boundaries" (str "  Boundaries:\n"
                        "    Never modify: " (pr-str (get-in soul [:boundaries :never-modify])) "\n"
                        "    Require approval: " (pr-str (get-in soul [:boundaries :require-approval])) "\n"
                        "    Max autonomy: " (get-in soul [:boundaries :max-autonomy-level]))
      ;; default: full view
      (str "  Agent Soul\n"
           "  ─────────────────────────────────────\n"
           "  ID:              " (name (:soul-id soul)) "\n"
           "  Traits:          " (pr-str (get-in soul [:personality :traits])) "\n"
           "  Style:           " (get-in soul [:personality :communication-style]) "\n"
           "  Risk Tolerance:  " (get-in soul [:personality :risk-tolerance]) "\n"
           "  Goals:           " (pr-str (:goals soul)) "\n"
           "  Autonomy Level:  " (get-in soul [:boundaries :max-autonomy-level]) "\n"
           "  Evolution Steps: " (count (:evolution-history soul))))))

(defn cli-heartbeat
  "Manage heartbeat"
  [os-state args]
  (let [sub (or args "status")]
    (case sub
      "status" (str "  Heartbeat: "
                    (if (and (:heartbeat-atom os-state)
                             @(:heartbeat-atom os-state))
                      "RUNNING" "STOPPED"))
      "start"  (do (when (and (:heartbeat-atom os-state)
                              @(:heartbeat-atom os-state))
                     (stop-heartbeat (:heartbeat-atom os-state)))
                   "  Heartbeat started.")
      "stop"   (if (:heartbeat-atom os-state)
                 (do (stop-heartbeat (:heartbeat-atom os-state))
                     "  Heartbeat stopped.")
                 "  Heartbeat not running.")
      "run-once" (let [result (heartbeat-check (:architecture os-state)
                                                (:mem-system os-state)
                                                (:history os-state))]
                   (str "  Heartbeat check result:\n"
                        "  Status: " (:status result) "\n"
                        "  Issues: " (count (:issues result))))
      (str "  Unknown: " sub ". Use: heartbeat [status|start|stop|run-once]"))))

(defn cli-skills
  "Manage skills"
  [skill-registry args]
  (let [parts (when args (clojure.string/split args #"\s+" 2))
        sub (or (first parts) "list")
        skill-arg (second parts)]
    (case sub
      "list" (let [{:keys [skills loaded]} @skill-registry]
               (str "  Skills (" (count skills) " registered, "
                    (count loaded) " loaded)\n"
                    "  ─────────────────────────────────────\n"
                    (if (empty? skills)
                      "  No skills registered."
                      (clojure.string/join "\n"
                        (for [[id skill] (sort-by first skills)]
                          (str "  " (format "%-18s" (name id))
                               (if (contains? loaded id) "[LOADED]" "[      ]")
                               "  " (:description skill)))))))
      "load"   (if skill-arg
                 (do (load-skill skill-registry (keyword skill-arg))
                     (str "  Loaded: " skill-arg))
                 "  Usage: skills load <skill-id>")
      "unload" (if skill-arg
                 (do (unload-skill skill-registry (keyword skill-arg))
                     (str "  Unloaded: " skill-arg))
                 "  Usage: skills unload <skill-id>")
      (str "  Unknown: " sub ". Use: skills [list|load|unload]"))))

(defn cli-permissions
  "View permissions"
  [perm-model args]
  (let [perms @perm-model]
    (if args
      (let [comp-id (keyword args)
            granted (get-in perms [:component-permissions comp-id])]
        (if granted
          (str "  Permissions for " args ": " (pr-str granted))
          (str "  No permissions found for: " args)))
      (str "  Permission Model\n"
           "  ─────────────────────────────────────\n"
           "  Components:\n"
           (clojure.string/join "\n"
             (for [[id perms] (sort-by first (:component-permissions perms))]
               (str "    " (format "%-22s" (name id)) (pr-str perms))))
           "\n\n  Trust Levels:\n"
           (clojure.string/join "\n"
             (for [[src level] (:trust-levels perms)]
               (str "    " (format "%-12s" (name src)) (name level))))))))

(defn cli-history
  "Show modification history"
  [history args]
  (let [n (if args (Integer/parseInt args) 10)
        h @history
        recent (take-last n (concat (:successful h) (:failed h)))]
    (str "  Modification History (last " n ")\n"
         "  ─────────────────────────────────────\n"
         (if (empty? recent)
           "  No modifications yet."
           (clojure.string/join "\n"
             (for [m (reverse (sort-by :timestamp recent))]
               (str "  [" (if (get-in m [:result :success?]) "OK" "FAIL") "] "
                    (name (:component-id m)) " - " (:reason m))))))))

;; --- Main CLI Dispatcher ---

(defn dispatch-command
  "Route parsed command to appropriate handler"
  [os-state {:keys [command args]}]
  (case command
    :help        (cli-help args)
    :status      (cli-status os-state)
    :components  (cli-components (:architecture os-state))
    :inspect     (cli-inspect (:architecture os-state) args)
    :memory      (cli-memory (:mem-system os-state) args)
    :remember    (cli-remember (:mem-system os-state) args)
    :soul        (cli-soul-view (:soul os-state) args)
    :identity    (str "  " (pr-str (:identity os-state)))
    :heartbeat   (cli-heartbeat os-state args)
    :skills      (cli-skills (:skill-registry os-state) args)
    :permissions (cli-permissions (:perm-model os-state) args)
    :history     (cli-history (:history os-state) args)
    :log         (str "  Daily log: " (or args "today"))
    :chat        (if args
                   (str "  [Chat] Processing: " args "\n"
                        "  (Requires LLM client - set API key with create-agent-os)")
                   "  Usage: chat <message>")
    :improve     (str "  Running self-improvement cycle"
                      (when args (str " on " args)) "...")
    :shutdown    "  SHUTDOWN"
    ;; Unknown command
    (str "  Unknown command: " (name command) "\n"
         "  Type 'help' for available commands.")))

;; --- CLI REPL Loop ---

(defn start-cli
  "Start interactive CLI loop"
  [os-state]
  (println cli-banner)
  (loop [state os-state]
    (print "aos> ")
    (flush)
    (let [input (read-line)]
      (when (and input (not (clojure.string/blank? input)))
        (let [parsed (parse-command input)
              output (dispatch-command state parsed)]
          (println output)
          (when-not (= output "  SHUTDOWN")
            (recur state)))))))

;; ============================================================================
;; MAIN AGENT OS
;; ============================================================================

(defrecord AgentOS [architecture history llm-client]
  IKernel
  (boot [this]
    (println "Agent OS booting...")
    (println "Version:" KERNEL_VERSION)
    (println "Components:" (count (:components architecture)))
    this)
  
  (shutdown [this]
    (println "Agent OS shutting down safely...")
    (println "Final state:" (get-system-state architecture))
    nil)
  
  (validate-modification [this change]
    (let [safety (safety-check architecture change)
          validation (validate-new-code (:new-code change))]
      (and (:safe? safety) (:valid? validation))))
  
  (apply-modification [this change]
    (let [result (apply-modification-safe architecture change)]
      (when (:success? result)
        (record-modification history change result))
      (->AgentOS (:architecture result) history llm-client))))

(defn create-agent-os
  "Tạo instance mới của Agent OS - enhanced with OpenClaw patterns
   Trả về full OS state map cho cả REPL và CLI sử dụng"
  [& {:keys [custom-architecture llm-api-key data-dir soul-config]}]
  (let [arch (or custom-architecture example-architecture)
        hist (modification-history)
        llm-client (when llm-api-key
                     {:api-key llm-api-key})
        ;; OpenClaw-inspired: persistent memory
        mem-system (create-persistent-memory (or data-dir "data"))
        ;; OpenClaw-inspired: soul & identity
        soul (or soul-config (create-soul ::default-agent))
        agent-identity (create-identity "AOS Agent" "Self-Modifying System Agent")
        ;; OpenClaw-inspired: skill registry
        skills (create-skill-registry)
        ;; OpenClaw-inspired: permission model
        perms (create-permission-model)
        ;; OpenClaw-inspired: heartbeat config
        heartbeat-cfg (create-heartbeat-config)
        ;; Boot core OS
        core-os (boot (->AgentOS arch hist llm-client))]
    ;; Return full state map (CLI Gateway cần tất cả subsystems)
    {:core-os core-os
     :architecture arch
     :history hist
     :llm-client llm-client
     :mem-system mem-system
     :soul soul
     :identity agent-identity
     :skill-registry skills
     :perm-model perms
     :heartbeat-config heartbeat-cfg
     :heartbeat-atom (atom false)}))

;; ============================================================================
;; STARTUP
;; ============================================================================

(defn -main
  "Entry point - hỗ trợ cả CLI mode và REPL mode
   Usage:
     lein run              -> CLI interactive mode
     lein run --repl       -> REPL mode (print instructions)"
  [& args]
  (println "=== Agent OS - Self-Modifying AI Architecture ===")
  (println "Starting Agent OS...")
  (let [os (create-agent-os)]
    (if (some #{"--repl"} args)
      ;; REPL mode
      (do
        (println "\nAgent OS is ready! (REPL mode)")
        (println "Try: (get-system-state (:architecture os))")
        os)
      ;; CLI mode (default)
      (do
        (println "\nAgent OS is ready!")
        (start-cli os)))))
