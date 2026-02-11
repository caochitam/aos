(ns agent-os.integrations.zalo.server
  "HTTP server for Zalo webhook integration"
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [agent-os.integrations.zalo.handler :as handler]))

(defn- health-check-handler
  "Health check endpoint"
  [_request]
  (response/response {:status "ok" :service "AOS Zalo Bot"}))

(defn- webhook-verification-handler
  "Handle Zalo webhook verification (GET request)"
  [request]
  ;; Zalo sends a verification token during initial setup
  (let [params (:params request)
        challenge (get params "challenge")]
    (if challenge
      (do
        (log/info "Webhook verification request received")
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body challenge})
      {:status 400
       :body "Missing challenge parameter"})))

(defn- create-routes
  "Create Ring routes for Zalo webhook

  Parameters:
  - context: AOS context
  - zalo-config: Zalo configuration

  Returns:
  - Ring handler function"
  [context zalo-config]
  (fn [request]
    (let [uri (:uri request)
          method (:request-method request)]

      (cond
        ;; Health check endpoint
        (and (= method :get) (= uri "/health"))
        (health-check-handler request)

        ;; Webhook verification (GET)
        (and (= method :get) (= uri "/webhook"))
        (webhook-verification-handler request)

        ;; Webhook event handler (POST)
        (and (= method :post) (= uri "/webhook"))
        ((handler/create-handler context zalo-config) request)

        ;; 404 Not Found
        :else
        {:status 404
         :body "Not Found"}))))

(defn start-server
  "Start HTTP server for Zalo webhook

  Parameters:
  - context: AOS context (kernel, llm-registry, memory, config)
  - zalo-config: Zalo configuration map
    - :port - Server port (default: 3000)
    - :access-token - Zalo OA access token
    - :secret-key - App secret key

  Returns:
  - Server instance (can be stopped with .stop)"
  [context zalo-config]
  (let [port (get zalo-config :port 3000)
        routes (create-routes context zalo-config)
        app (-> routes
                wrap-params
                (wrap-json-body {:keywords? true})
                wrap-json-response)]

    (log/info (format "Starting Zalo webhook server on port %d..." port))

    (try
      (let [server (jetty/run-jetty app
                                    {:port port
                                     :join? false})]
        (log/info (format "✓ Zalo webhook server running at http://localhost:%d" port))
        (log/info "Webhook URL: /webhook")
        (log/info "Health check: /health")
        server)

      (catch Exception e
        (log/error "Failed to start webhook server:" (.getMessage e))
        (throw e)))))

(defn stop-server
  "Stop HTTP server

  Parameters:
  - server: Server instance returned by start-server"
  [server]
  (when server
    (try
      (.stop server)
      (log/info "Zalo webhook server stopped")
      (catch Exception e
        (log/error "Error stopping server:" (.getMessage e))))))

(defn restart-server
  "Restart HTTP server

  Parameters:
  - server: Current server instance
  - context: AOS context
  - zalo-config: Zalo configuration

  Returns:
  - New server instance"
  [server context zalo-config]
  (stop-server server)
  (Thread/sleep 1000) ; Wait for port to be released
  (start-server context zalo-config))

(comment
  ;; Example usage:

  ;; Start server
  (def server (start-server
                {:kernel kernel
                 :llm-registry llm-registry
                 :memory memory
                 :config config}
                {:port 3000
                 :access-token "YOUR_ACCESS_TOKEN"
                 :secret-key "YOUR_SECRET_KEY"}))

  ;; Stop server
  (stop-server server)

  ;; Test health check
  (require '[clj-http.client :as http])
  (http/get "http://localhost:3000/health")
  )
