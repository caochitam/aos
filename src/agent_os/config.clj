(ns agent-os.config
  "Configuration loading using aero"
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn load-config
  "Load configuration from resources/config.edn"
  ([]
   (load-config "config.edn"))
  ([filename]
   (if-let [resource (io/resource filename)]
     (aero/read-config resource)
     (throw (ex-info "Config file not found"
                     {:filename filename})))))

(defn get-in-config
  "Get value from config with path"
  [config & path]
  (get-in config path))
