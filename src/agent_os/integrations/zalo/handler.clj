(ns agent-os.integrations.zalo.handler
  "Message handler for Zalo bot integration with AOS"
  (:require [clojure.tools.logging :as log]
            [agent-os.integrations.zalo.client :as zalo-client]
            [agent-os.cli.gateway :as gateway]))

(defn- extract-message-text
  "Extract text from Zalo message event"
  [event]
  (get-in event [:message :text] ""))

(defn- extract-user-id
  "Extract user ID from Zalo event"
  [event]
  (get-in event [:sender :id]))

(defn- extract-user-name
  "Extract user name from Zalo event"
  [event]
  (get-in event [:sender :display_name] "User"))

(defn process-message
  "Process incoming message from Zalo and generate response

  Parameters:
  - event: Zalo webhook event
  - context: AOS context (kernel, llm-registry, memory, config)

  Returns:
  - Response text to send back to user"
  [event context]
  (try
    (let [user-id (extract-user-id event)
          user-name (extract-user-name event)
          message-text (extract-message-text event)

          ;; Log incoming message
          _ (log/info (format "Received message from %s (ID: %s): %s"
                             user-name user-id message-text))

          ;; Process message through AOS chat system
          ;; Note: This uses the same chat handler as CLI
          response (gateway/process-chat-message
                     message-text
                     context)]

      (log/info (format "Generated response for %s: %s"
                       user-name (subs response 0 (min 100 (count response)))))
      response)

    (catch Exception e
      (log/error "Error processing Zalo message:" (.getMessage e))
      "Xin lỗi, tôi gặp lỗi khi xử lý tin nhắn của bạn. Vui lòng thử lại sau.")))

(defn handle-webhook-event
  "Handle incoming Zalo webhook event

  Parameters:
  - event: Parsed webhook event JSON
  - context: AOS context
  - config: Zalo configuration (access-token, etc.)

  Returns:
  - {:success true/false, :message string}"
  [event context config]
  (try
    (let [event-name (:event_name event)
          access-token (:access-token config)]

      (case event-name
        ;; User sends message
        "user_send_text"
        (let [user-id (extract-user-id event)]
          ;; Show typing indicator
          (zalo-client/send-typing-indicator access-token user-id)

          ;; Process message and get response
          (let [response-text (process-message event context)]
            ;; Send response back to Zalo
            (let [result (zalo-client/send-text-message
                          access-token
                          user-id
                          response-text)]
              (if (:success result)
                {:success true :message "Message sent successfully"}
                {:success false :message (str "Failed to send: " (:error result))}))))

        ;; User follows OA
        "follow"
        (let [user-id (extract-user-id event)
              welcome-msg (str "Xin chào! Tôi là AOS (Agent Operating System) - "
                              "một AI agent tự cải tiến. "
                              "Bạn có thể chat với tôi về bất kỳ điều gì!")]
          (zalo-client/send-text-message access-token user-id welcome-msg)
          {:success true :message "Welcome message sent"})

        ;; User unfollows OA
        "unfollow"
        (do
          (log/info "User unfollowed:" (extract-user-id event))
          {:success true :message "User unfollowed"})

        ;; Unknown event type
        (do
          (log/warn "Unknown Zalo event type:" event-name)
          {:success true :message "Event ignored"})))

    (catch Exception e
      (log/error "Error handling webhook event:" (.getMessage e))
      {:success false :message (.getMessage e)})))

(defn create-handler
  "Create a Ring handler for Zalo webhook

  Parameters:
  - context: AOS context (kernel, llm-registry, memory, config)
  - zalo-config: Zalo configuration map

  Returns:
  - Ring handler function"
  [context zalo-config]
  (fn [request]
    (try
      ;; Validate webhook signature
      (let [signature (get-in request [:headers "x-zevent-signature"])
            body (:body request)
            secret-key (:secret-key zalo-config)

            ;; Note: In production, validate signature
            ;; valid? (zalo-client/validate-webhook-signature body signature secret-key)

            event (if (string? body)
                   (cheshire.core/parse-string body true)
                   body)]

        ;; Process the event
        (let [result (handle-webhook-event event context zalo-config)]
          {:status (if (:success result) 200 500)
           :headers {"Content-Type" "application/json"}
           :body (cheshire.core/generate-string result)}))

      (catch Exception e
        (log/error "Webhook handler error:" (.getMessage e))
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (cheshire.core/generate-string
                 {:success false
                  :message "Internal server error"})}))))
