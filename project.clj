(defproject agent-os "0.1.0-SNAPSHOT"
  :description "Self-modifying AI Agent Operating System"
  :url "https://github.com/your-org/agent-os"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/core.async "1.6.681"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/tools.reader "1.3.6"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [mount "0.1.17"]            ; component lifecycle
                 [aero "1.1.6"]              ; config management
                 [com.taoensso/timbre "6.3.1"]   ; logging
                 [ring/ring-core "1.10.0"]   ; HTTP server (future API)
                 [ring/ring-jetty-adapter "1.10.0"] ; Jetty adapter for Ring
                 [ring/ring-json "0.5.1"]    ; JSON middleware for Ring
                 [metosin/reitit "0.7.0"]    ; routing (future API)
                 [jline/jline "2.14.6"]      ; readline with history & completion
                 [io.aviso/pretty "1.4.4"]]  ; colored output

  :main ^:skip-aot agent-os.core
  :target-path "target/%s"

  :profiles {:dev {:dependencies [[midje "1.10.9"]
                                  [org.clojure/test.check "1.1.1"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
