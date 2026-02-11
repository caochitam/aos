# Agent OS - Deployment Guide & Best Practices

## 🚀 Quick Start

### Prerequisites
```bash
# Clojure 1.11+
brew install clojure  # macOS
sudo apt install clojure  # Ubuntu

# Leiningen (build tool)
brew install leiningen

# Required dependencies
# Thêm vào project.clj:
:dependencies [[org.clojure/clojure "1.11.1"]
               [org.clojure/data.json "2.4.0"]
               [clj-http "3.12.3"]
               [cheshire "5.11.0"]]
```

### Runtime Management

AOS provides convenient commands for managing the runtime:

**Restart Command**: Reload configuration and reinitialize without exiting the process
```bash
aos> restart
Restarting Agent OS...
=== Agent OS - Self-Modifying AI Architecture ===
=== Agent OS CLI v0.1.0 ===
Type 'help' for available commands
```

This is useful when:
- You've updated configuration files (config.edn, SOUL.edn, etc.)
- You've added a new session token via `login-claude`
- You want to refresh the system state without losing terminal context

**Exit Command**: Clean shutdown
```bash
aos> exit
Goodbye!
```

### Installation

```bash
# 1. Clone repository
git clone <your-repo>
cd agent-os

# 2. Install dependencies
lein deps

# 3. Test setup
lein repl
```

### First Run

```clojure
;; In REPL
(load-file "agent-os-architecture.clj")
(load-file "agent-os-example.clj")

;; Set API key
(def api-key "sk-ant-api03-your-key-here")

;; Run demo
(agent-os.example/run-demo api-key)
```

## 📋 Project Structure

```
agent-os/
├── agent-os-architecture.clj   # Core architecture
├── agent-os-example.clj        # Practical examples
├── README.md                   # Main documentation
├── DEPLOYMENT.md              # This file
├── project.clj                # Leiningen config
├── test/
│   └── agent-os/
│       └── core_test.clj      # Tests
└── resources/
    └── config.edn             # Configuration
```

## ⚙️ Configuration

### config.edn
```clojure
{:agent-os
 {:version "0.1.0"
  :kernel
  {:max-modifications-per-hour 10
   :safety-checks-enabled true
   :rollback-window-hours 24}
  
  :llm
  {:provider :anthropic
   :model "claude-sonnet-4-20250514"
   :max-tokens 4000
   :temperature 0.7
   :rate-limit {:requests-per-minute 50}}
  
  :components
  {:default-modifiable true
   :require-approval [:kernel :safety-engine]
   :auto-rollback-on-error true}
  
  :monitoring
  {:log-level :info
   :metrics-enabled true
   :alert-on-failed-modification true}}}
```

## 🛡️ Safety Best Practices

### 1. Always Enable Safety Checks

```clojure
(defn configure-safety
  [os]
  (-> os
      (assoc-in [:config :safety :enabled] true)
      (assoc-in [:config :safety :max-modifications-per-hour] 10)
      (assoc-in [:config :safety :require-human-approval] 
                #{:kernel :critical-components})))
```

### 2. Use Sandboxed Execution

```clojure
(defn execute-in-sandbox
  "Execute code in isolated environment"
  [code]
  (try
    ;; Create isolated namespace
    (binding [*ns* (create-ns (gensym "sandbox"))]
      ;; Limit permissions
      (with-limited-permissions
        (eval code)))
    (catch Exception e
      {:error (.getMessage e)
       :safe? false})))
```

### 3. Implement Rate Limiting

```clojure
(def modification-limiter
  (atom {:modifications []
         :window-start (System/currentTimeMillis)}))

(defn can-modify?
  "Check if modification is allowed"
  []
  (let [now (System/currentTimeMillis)
        window-ms (* 60 60 1000) ; 1 hour
        {:keys [modifications window-start]} @modification-limiter
        recent-mods (filter #(> % (- now window-ms)) modifications)]
    (< (count recent-mods) 10)))
```

### 4. Maintain Audit Trail

```clojure
(defn log-modification
  [modification result]
  (let [log-entry {:timestamp (System/currentTimeMillis)
                   :component (:component-id modification)
                   :success? (:success? result)
                   :hash (hash (:new-code modification))
                   :reason (:reason modification)}]
    ;; Write to persistent log
    (spit "logs/modifications.edn" 
          (pr-str log-entry)
          :append true)))
```

## 📊 Monitoring & Observability

### Metrics to Track

```clojure
(defrecord Metrics [modifications-total
                    modifications-successful
                    modifications-failed
                    avg-improvement-time
                    components-modified
                    rollbacks-performed])

(defn collect-metrics
  [history]
  (->Metrics
    (count (:modifications @history))
    (count (:successful @history))
    (count (:failed @history))
    (avg-time (:modifications @history))
    (count (distinct (map :component-id (:modifications @history))))
    (count (filter :rolled-back (:modifications @history)))))
```

### Health Checks

```clojure
(defn health-check
  [os]
  (let [arch (:architecture os)]
    {:status :healthy
     :components-count (count (:components arch))
     :modifiable-count (count (filter :modifiable? 
                                      (vals (:components arch))))
     :last-modification (last (:modifications @(:history os)))
     :memory-usage (memory-stats)
     :cpu-usage (cpu-stats)}))
```

### Alerting

```clojure
(defn setup-alerts
  []
  (add-watch modification-limiter :alert-watcher
    (fn [_ _ old-state new-state]
      (when (> (count (:modifications new-state)) 50)
        (send-alert {:level :warning
                    :message "High modification rate detected"
                    :count (count (:modifications new-state))})))))
```

## 🔧 Debugging Tips

### 1. Enable Verbose Logging

```clojure
(def debug-mode (atom false))

(defmacro debug-log
  [& body]
  `(when @debug-mode
     (println "DEBUG:" ~@body)))

;; Usage
(swap! debug-mode not)
```

### 2. Inspect Component State

```clojure
(defn inspect-component
  [os component-id]
  (let [comp (get-in (:architecture os) [:components component-id])]
    {:id (:id comp)
     :version (:version comp)
     :code-preview (take 100 (pr-str (:code comp)))
     :last-modified (:modified-at comp)
     :dependencies (:dependencies comp)
     :modification-count (count-modifications component-id (:history os))}))
```

### 3. Trace Execution

```clojure
(defn trace-modification
  [modification]
  (println "=== TRACE START ===")
  (println "Component:" (:component-id modification))
  (println "Old Code:" (pr-str (:old-code modification)))
  (println "New Code:" (pr-str (:new-code modification)))
  (println "Reason:" (:reason modification))
  (println "=== TRACE END ==="))
```

## 🧪 Testing

### Unit Tests

```clojure
(ns agent-os.core-test
  (:require [clojure.test :refer :all]
            [agent-os.core :refer :all]))

(deftest test-component-creation
  (testing "Component specification"
    (let [comp (component-spec
                :test-comp
                "Test component"
                #{:test}
                #{})]
      (is (= (:id comp) :test-comp))
      (is (true? (:modifiable? comp))))))

(deftest test-validation
  (testing "Code validation"
    (is (:valid? (validate-new-code '(defn test [x] x))))
    (is (not (:valid? (validate-new-code "invalid"))))))

(deftest test-safety
  (testing "Safety checks"
    (let [arch example-architecture
          mod {:component-id :kernel}]
      (is (not (:safe? (safety-check arch mod)))))))
```

### Integration Tests

```clojure
(deftest test-full-workflow
  (testing "Complete modification workflow"
    (let [os (create-agent-os)
          initial-version (get-in os [:architecture :components 
                                      :memory-manager :version])
          mod (create-modification
               :memory-manager
               '(defn old [x] x)
               '(defn new [x] (* x 2))
               "Test modification"
               {})
          result (apply-modification-safe (:architecture os) mod)]
      
      (is (:success? result))
      (is (= (inc initial-version)
             (get-in (:architecture result) 
                    [:components :memory-manager :version]))))))
```

### Load Testing

```clojure
(defn stress-test
  "Test system under load"
  [os iterations]
  (time
    (dotimes [i iterations]
      (let [target (rand-nth (keys (get-in os [:architecture :components])))
            mod (create-test-modification target)]
        (apply-modification-safe (:architecture os) mod))))
  (println "Completed" iterations "modifications"))
```

## 🔄 Continuous Integration

### GitHub Actions Example

```yaml
name: Agent OS CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install Clojure
        uses: DeLaGuardo/setup-clojure@master
        with:
          cli: 1.11.1.1165
      - name: Run tests
        run: lein test
      - name: Run integration tests
        run: lein test :integration
```

## 📦 Production Deployment

### Docker Setup

```dockerfile
FROM clojure:openjdk-11-lein

WORKDIR /app

COPY project.clj .
RUN lein deps

COPY . .

CMD ["lein", "run"]
```

### docker-compose.yml

```yaml
version: '3.8'
services:
  agent-os:
    build: .
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
    volumes:
      - ./logs:/app/logs
      - ./data:/app/data
    restart: unless-stopped
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agent-os
spec:
  replicas: 1
  selector:
    matchLabels:
      app: agent-os
  template:
    metadata:
      labels:
        app: agent-os
    spec:
      containers:
      - name: agent-os
        image: agent-os:latest
        env:
        - name: ANTHROPIC_API_KEY
          valueFrom:
            secretKeyRef:
              name: agent-os-secrets
              key: api-key
```

## 🔐 Security Hardening

### 1. API Key Management

```clojure
;; KHÔNG BAO GIỜ hardcode API key
;; Đọc từ environment variable
(defn get-api-key
  []
  (or (System/getenv "ANTHROPIC_API_KEY")
      (throw (ex-info "API key not found" {}))))
```

### 2. Input Validation

```clojure
(defn validate-modification-input
  [modification]
  (when-not (s/valid? ::modification-spec modification)
    (throw (ex-info "Invalid modification" 
                   {:spec-error (s/explain-data ::modification-spec 
                                                modification)}))))
```

### 3. Secure Communication

```clojure
(defn secure-api-call
  [api-key request]
  {:pre [(string? api-key)
         (map? request)]}
  (http/post url
    {:headers {"x-api-key" api-key
               "content-type" "application/json"}
     :body (json/write-str request)
     :socket-timeout 30000
     :conn-timeout 5000
     :throw-exceptions true
     :insecure? false}))  ; Enforce HTTPS
```

## 📈 Performance Optimization

### 1. Caching

```clojure
(def code-analysis-cache (atom {}))

(defn analyze-component-cached
  [component-id arch]
  (if-let [cached (@code-analysis-cache component-id)]
    (if (= (:version (get-in arch [:components component-id]))
           (:version cached))
      (:analysis cached)
      (let [analysis (analyze-component component-id arch)]
        (swap! code-analysis-cache assoc component-id
               {:version (:version (get-in arch [:components component-id]))
                :analysis analysis})
        analysis))
    (let [analysis (analyze-component component-id arch)]
      (swap! code-analysis-cache assoc component-id
             {:version (:version (get-in arch [:components component-id]))
              :analysis analysis})
      analysis)))
```

### 2. Async Processing

```clojure
(require '[clojure.core.async :as async])

(defn async-self-improvement
  [os]
  (let [ch (async/chan 10)]
    (async/go-loop []
      (when-let [component-id (async/<! ch)]
        (try
          (self-improve-component os component-id)
          (catch Exception e
            (println "Error improving" component-id (.getMessage e))))
        (recur)))
    ch))
```

## 🎓 Advanced Patterns

### 1. Multi-Agent System

```clojure
(defn create-agent-cluster
  [n api-key]
  (vec
    (for [i (range n)]
      {:id (keyword (str "agent-" i))
       :os (create-agent-os :llm-api-key api-key)
       :specialization (rand-nth [:optimization :bug-fixing :refactoring])})))

(defn coordinate-agents
  [cluster task]
  ;; Distribute task across agents based on specialization
  (let [agent (find-specialist cluster task)]
    (execute-task agent task)))
```

### 2. Evolutionary Algorithm

```clojure
(defn evolve-architecture
  [population generations]
  (loop [gen 0
         current-pop population]
    (if (>= gen generations)
      (best-individual current-pop)
      (let [evaluated (map #(assoc % :fitness (evaluate-fitness %)) 
                          current-pop)
            selected (selection evaluated)
            offspring (crossover-and-mutate selected)]
        (recur (inc gen) offspring)))))
```

## 📝 Troubleshooting

### Common Issues

**Issue 1: API Rate Limiting**
```clojure
;; Solution: Implement exponential backoff
(defn call-with-retry
  [api-fn max-retries]
  (loop [attempt 0]
    (let [result (try (api-fn) (catch Exception e e))]
      (cond
        (not (instance? Exception result)) result
        (>= attempt max-retries) (throw result)
        :else (do
                (Thread/sleep (* 1000 (Math/pow 2 attempt)))
                (recur (inc attempt)))))))
```

**Issue 2: Memory Leaks**
```clojure
;; Solution: Implement cleanup
(defn cleanup-old-modifications
  [history max-age-days]
  (let [cutoff (- (System/currentTimeMillis)
                  (* max-age-days 24 60 60 1000))]
    (swap! history update :modifications
           #(filter (fn [m] (> (:timestamp m) cutoff)) %))))
```

**Issue 3: Noisy Logs from Shell Commands**

AOS now automatically suppresses noisy IOExceptions from shell commands like `xdg-open`, `security`, and browser launching. This is handled by Timbre middleware in `/root/aos/src/agent_os/core.clj`.

If you need to debug shell command issues, you can temporarily enable debug logging:

```clojure
;; In core.clj, change :min-level
(log/merge-config!
  {:min-level :debug})  ; Change from :info to :debug
```

**Issue 4: Browser Not Opening for OAuth Login**

When running `login-claude`, if the browser doesn't automatically open:

1. AOS will detect if the browser command exists before attempting to open
2. If the command doesn't exist, you'll see: "Please open manually: https://claude.ai/login"
3. This is normal on headless servers or when browser commands are unavailable

**Issue 5: Keychain Access Warnings on Non-macOS Systems**

AOS now checks if the `security` command exists before attempting keychain operations. On Linux/Windows, AOS automatically falls back to file-based token storage at `~/.claude/session_token`.

## 📚 Resources

- [Clojure Documentation](https://clojure.org/reference/documentation)
- [Anthropic API Docs](https://docs.anthropic.com)
- [Agent OS GitHub](https://github.com/your-repo/agent-os)

## 🆘 Support

For issues or questions:
- GitHub Issues
- Discord Server
- Email: support@agent-os.dev
