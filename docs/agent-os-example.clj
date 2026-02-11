;; ============================================================================
;; AGENT OS - PRACTICAL EXAMPLE WITH CLAUDE API
;; ============================================================================
;; Ví dụ thực tế về cách Agent OS tự sửa đổi code của mình
;; Sử dụng Claude Sonnet API để phân tích và generate code
;; ============================================================================

(ns agent-os.example
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]))

;; ============================================================================
;; CLAUDE API CLIENT
;; ============================================================================

(def CLAUDE_API_URL "https://api.anthropic.com/v1/messages")
(def CLAUDE_MODEL "claude-sonnet-4-20250514")

(defn create-claude-request
  "Tạo request body cho Claude API"
  [system-prompt user-message]
  {:model CLAUDE_MODEL
   :max_tokens 4000
   :system system-prompt
   :messages [{:role "user"
               :content user-message}]})

(defn call-claude
  "Gọi Claude API và parse response"
  [api-key request-body]
  (try
    (let [response (http/post CLAUDE_API_URL
                     {:headers {"x-api-key" api-key
                               "anthropic-version" "2023-06-01"
                               "content-type" "application/json"}
                      :body (json/write-str request-body)
                      :throw-exceptions true})
          body (json/read-str (:body response) :key-fn keyword)
          content (-> body :content first :text)]
      {:success? true
       :content content
       :usage (:usage body)})
    (catch Exception e
      {:success? false
       :error (.getMessage e)})))

;; ============================================================================
;; SELF-REFLECTION WITH CLAUDE
;; ============================================================================

(defn generate-reflection-prompt
  "Tạo prompt cho Claude để tự phân tích code"
  [component-spec]
  (str 
    "Phân tích component này và đề xuất cải tiến:\n\n"
    "COMPONENT: " (:id component-spec) "\n"
    "PURPOSE: " (:purpose component-spec) "\n"
    "CURRENT CODE:\n"
    (pr-str (:code component-spec)) "\n\n"
    "Hãy phân tích:\n"
    "1. Code quality và readability\n"
    "2. Performance issues\n"
    "3. Potential bugs\n"
    "4. Cải tiến có thể làm\n\n"
    "Response format (JSON):\n"
    "{\n"
    "  \"quality_score\": 0-10,\n"
    "  \"issues\": [\"issue1\", \"issue2\"],\n"
    "  \"suggestions\": [\"suggestion1\", \"suggestion2\"],\n"
    "  \"priority\": \"low|medium|high\"\n"
    "}"))

(defn reflect-on-component
  "Agent tự phân tích một component bằng Claude"
  [api-key component-spec]
  (println "\n🔍 Reflecting on component:" (:id component-spec))
  (let [system-prompt "Bạn là AI Agent đang phân tích code của chính mình. Hãy objective và chi tiết."
        user-message (generate-reflection-prompt component-spec)
        request (create-claude-request system-prompt user-message)
        response (call-claude api-key request)]
    
    (if (:success? response)
      (do
        (println "✅ Reflection completed")
        (println "📊 Usage:" (:usage response))
        (try
          ;; Parse JSON response từ Claude
          (json/read-str (:content response) :key-fn keyword)
          (catch Exception e
            (println "⚠️ Could not parse response as JSON")
            {:raw-response (:content response)})))
      (do
        (println "❌ Reflection failed:" (:error response))
        nil))))

;; ============================================================================
;; CODE GENERATION WITH CLAUDE
;; ============================================================================

(defn generate-code-prompt
  "Tạo prompt cho Claude để generate code mới"
  [component-spec suggestion]
  (str
    "Generate improved code cho component này:\n\n"
    "COMPONENT: " (:id component-spec) "\n"
    "CURRENT CODE:\n"
    (pr-str (:code component-spec)) "\n\n"
    "IMPROVEMENT TO MAKE:\n"
    suggestion "\n\n"
    "Requirements:\n"
    "1. Code phải là valid Clojure S-expression\n"
    "2. Giữ nguyên function signature nếu có thể\n"
    "3. Code phải có thể eval được\n"
    "4. Thêm comments giải thích thay đổi\n\n"
    "Response format (Clojure code only, no markdown):\n"
    "(defn component-name\n"
    "  ;; Description of improvements\n"
    "  [args]\n"
    "  ;; implementation\n"
    "  ...)"))

(defn generate-improved-code
  "Agent tự generate code mới bằng Claude"
  [api-key component-spec suggestion]
  (println "\n🛠️ Generating improved code for:" (:id component-spec))
  (println "📝 Suggestion:" suggestion)
  
  (let [system-prompt "Bạn là AI code generator. Generate valid, production-ready Clojure code."
        user-message (generate-code-prompt component-spec suggestion)
        request (create-claude-request system-prompt user-message)
        response (call-claude api-key request)]
    
    (if (:success? response)
      (do
        (println "✅ Code generated")
        (println "📊 Usage:" (:usage response))
        (try
          ;; Parse Clojure code từ response
          (let [code-str (:content response)
                ;; Remove markdown code blocks nếu có
                clean-code (-> code-str
                              (clojure.string/replace #"```clojure\n?" "")
                              (clojure.string/replace #"```\n?" "")
                              clojure.string/trim)]
            (read-string clean-code))
          (catch Exception e
            (println "⚠️ Could not parse as Clojure code")
            (println "Raw response:" (:content response))
            nil)))
      (do
        (println "❌ Code generation failed:" (:error response))
        nil))))

;; ============================================================================
;; COMPLETE SELF-MODIFICATION WORKFLOW
;; ============================================================================

(defn self-modify-workflow
  "Complete workflow: Reflect → Identify Issue → Generate Fix → Apply"
  [api-key component-spec]
  (println "\n" (apply str (repeat 60 "=")))
  (println "🚀 STARTING SELF-MODIFICATION WORKFLOW")
  (println (apply str (repeat 60 "=")))
  
  ;; Step 1: Reflection
  (println "\n📍 STEP 1: SELF-REFLECTION")
  (let [reflection (reflect-on-component api-key component-spec)]
    
    (if-not reflection
      (do
        (println "❌ Workflow failed: Could not reflect on component")
        nil)
      
      (do
        (println "\n📊 Reflection Results:")
        (println "  Quality Score:" (:quality_score reflection))
        (println "  Issues:" (:issues reflection))
        (println "  Suggestions:" (:suggestions reflection))
        (println "  Priority:" (:priority reflection))
        
        ;; Step 2: Decide if modification is needed
        (println "\n📍 STEP 2: DECISION")
        (if (or (< (:quality_score reflection) 7)
                (= (:priority reflection) "high"))
          
          (do
            (println "✅ Modification needed")
            
            ;; Step 3: Generate improved code
            (println "\n📍 STEP 3: CODE GENERATION")
            (let [main-suggestion (first (:suggestions reflection))
                  new-code (generate-improved-code 
                            api-key 
                            component-spec 
                            main-suggestion)]
              
              (if-not new-code
                (do
                  (println "❌ Workflow failed: Could not generate code")
                  nil)
                
                (do
                  (println "\n📍 STEP 4: VALIDATION")
                  (println "Generated code:")
                  (println (pr-str new-code))
                  
                  ;; Step 4: Validate
                  (try
                    ;; Validate syntax
                    (eval new-code) ;; Try to evaluate
                    (println "✅ Code validation passed")
                    
                    ;; Return modification proposal
                    {:status :success
                     :component-id (:id component-spec)
                     :old-code (:code component-spec)
                     :new-code new-code
                     :reflection reflection
                     :improvement main-suggestion}
                    
                    (catch Exception e
                      (println "❌ Code validation failed:" (.getMessage e))
                      {:status :failed
                       :error (.getMessage e)}))))))
          
          (do
            (println "✅ No modification needed - component quality is good")
            {:status :no-change-needed
             :reflection reflection}))))))

;; ============================================================================
;; EXAMPLE COMPONENTS TO TEST
;; ============================================================================

(def example-simple-component
  {:id :calculator
   :purpose "Simple calculator"
   :code '(defn calculator
            [op a b]
            (if (= op :add)
              (+ a b)
              (if (= op :subtract)
                (- a b)
                (if (= op :multiply)
                  (* a b)
                  (if (= op :divide)
                    (/ a b)
                    :unknown-operation)))))
   :modifiable? true})

(def example-memory-component
  {:id :memory-store
   :purpose "Store and retrieve values"
   :code '(defn memory-store
            [operation data]
            (let [storage (atom {})]
              (case operation
                :set (swap! storage assoc (:key data) (:value data))
                :get (get @storage (:key data))
                :delete (swap! storage dissoc (:key data))
                nil)))
   :modifiable? true})

(def example-buggy-component
  {:id :fibonacci
   :purpose "Calculate fibonacci number"
   :code '(defn fibonacci
            ;; Bug: Inefficient recursive implementation
            [n]
            (if (< n 2)
              n
              (+ (fibonacci (- n 1))
                 (fibonacci (- n 2)))))
   :modifiable? true})

;; ============================================================================
;; INTERACTIVE DEMO
;; ============================================================================

(defn run-demo
  "Chạy demo với API key của bạn"
  [api-key]
  (println "╔════════════════════════════════════════════════════════╗")
  (println "║         AGENT OS - SELF-MODIFICATION DEMO              ║")
  (println "╚════════════════════════════════════════════════════════╝")
  
  (println "\n🎯 Testing self-modification on 3 different components...")
  
  ;; Test 1: Simple component with nested ifs
  (println "\n" (apply str (repeat 60 "=")))
  (println "TEST 1: SIMPLE CALCULATOR")
  (println (apply str (repeat 60 "=")))
  (let [result1 (self-modify-workflow api-key example-simple-component)]
    (when (= (:status result1) :success)
      (println "\n✅ SUCCESS: Component improved!")
      (println "Old code used nested ifs")
      (println "New code:" (pr-str (:new-code result1)))))
  
  (Thread/sleep 2000) ;; Wait to avoid rate limiting
  
  ;; Test 2: Memory component with atom in function
  (println "\n" (apply str (repeat 60 "=")))
  (println "TEST 2: MEMORY STORE")
  (println (apply str (repeat 60 "=")))
  (let [result2 (self-modify-workflow api-key example-memory-component)]
    (when (= (:status result2) :success)
      (println "\n✅ SUCCESS: Component improved!")
      (println "Old code created atom inside function")
      (println "New code:" (pr-str (:new-code result2)))))
  
  (Thread/sleep 2000)
  
  ;; Test 3: Buggy fibonacci
  (println "\n" (apply str (repeat 60 "=")))
  (println "TEST 3: FIBONACCI (BUGGY)")
  (println (apply str (repeat 60 "=")))
  (let [result3 (self-modify-workflow api-key example-buggy-component)]
    (when (= (:status result3) :success)
      (println "\n✅ SUCCESS: Bug fixed!")
      (println "Old code was inefficient O(2^n)")
      (println "New code:" (pr-str (:new-code result3)))))
  
  (println "\n╔════════════════════════════════════════════════════════╗")
  (println "║              DEMO COMPLETED                            ║")
  (println "╚════════════════════════════════════════════════════════╝"))

;; ============================================================================
;; USAGE INSTRUCTIONS
;; ============================================================================

(comment
  ;; 1. Set up your Anthropic API key
  (def my-api-key "sk-ant-api03-...")
  
  ;; 2. Test reflection on a single component
  (reflect-on-component my-api-key example-simple-component)
  
  ;; 3. Test code generation
  (generate-improved-code 
    my-api-key 
    example-simple-component
    "Replace nested if statements with case or cond")
  
  ;; 4. Run full workflow on one component
  (def result (self-modify-workflow my-api-key example-simple-component))
  
  ;; 5. Run complete demo
  (run-demo my-api-key)
  
  ;; 6. Apply the modification to actual architecture
  ;; (requires agent-os-architecture.clj loaded)
  ;; (def os (create-agent-os :llm-api-key my-api-key))
  ;; (def mod (create-modification 
  ;;            (:component-id result)
  ;;            (:old-code result)
  ;;            (:new-code result)
  ;;            (:improvement result)
  ;;            {:reflection (:reflection result)}))
  ;; (def new-os (apply-modification os mod))
  )

;; ============================================================================
;; CONTINUOUS SELF-IMPROVEMENT LOOP
;; ============================================================================

(defn continuous-improvement
  "Loop liên tục tự cải thiện các components"
  [api-key architecture max-iterations]
  (println "🔄 Starting continuous self-improvement loop...")
  (println "Max iterations:" max-iterations)
  
  (loop [current-arch architecture
         iteration 0
         improvements []]
    
    (if (>= iteration max-iterations)
      (do
        (println "\n✅ Continuous improvement completed")
        (println "Total improvements:" (count improvements))
        {:final-architecture current-arch
         :improvements improvements})
      
      (do
        (println "\n" (apply str (repeat 60 "-")))
        (println "Iteration" (inc iteration) "/" max-iterations)
        
        ;; Pick a random component to improve
        (let [components (vals (:components current-arch))
              modifiable (filter :modifiable? components)
              target (rand-nth modifiable)
              result (self-modify-workflow api-key target)]
          
          (Thread/sleep 3000) ;; Rate limiting
          
          (if (= (:status result) :success)
            (do
              (println "✅ Improvement applied to" (:component-id result))
              (recur 
                (update-in current-arch 
                          [:components (:component-id result)]
                          assoc :code (:new-code result))
                (inc iteration)
                (conj improvements result)))
            
            (do
              (println "⏭️ No improvement needed, trying next component")
              (recur current-arch (inc iteration) improvements))))))))

;; ============================================================================
;; MAIN
;; ============================================================================

(defn -main
  "Main entry point"
  [& args]
  (if-let [api-key (first args)]
    (do
      (println "Agent OS Example - Starting...")
      (run-demo api-key))
    (println "Usage: lein run <anthropic-api-key>")))
