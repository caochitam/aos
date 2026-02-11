(ns agent-os.integrations.zalo.client
  "Zalo API client for sending messages and interacting with Zalo OA"
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [agent-os.security.sanitizer :as sanitizer]))

(def ^:private zalo-api-base "https://openapi.zalo.me/v2.0/oa")

(defn- make-headers
  "Create headers with access token"
  [access-token]
  {"access_token" access-token
   "Content-Type" "application/json"})

(defn send-text-message
  "Send a text message to a user

  Parameters:
  - access-token: Zalo OA access token
  - user-id: Recipient's Zalo user ID
  - message: Text message to send

  Returns:
  - Response map with :success and :data/:error"
  [access-token user-id message]
  (try
    (let [url (str zalo-api-base "/message")
          payload {:recipient {:user_id user-id}
                   :message {:text message}}
          response (http/post url
                              {:headers (make-headers access-token)
                               :body (json/generate-string payload)
                               :content-type :json
                               :as :json})]
      (log/info "Sent message to Zalo user:" user-id)
      {:success true
       :data (:body response)})
    (catch Exception e
      (log/error "Failed to send Zalo message:" (.getMessage e))
      {:success false
       :error (sanitizer/sanitize-error (.getMessage e))})))

(defn send-typing-indicator
  "Show typing indicator to user

  Parameters:
  - access-token: Zalo OA access token
  - user-id: Recipient's Zalo user ID"
  [access-token user-id]
  (try
    (let [url (str zalo-api-base "/message")
          payload {:recipient {:user_id user-id}
                   :sender_action "typing_on"}
          response (http/post url
                              {:headers (make-headers access-token)
                               :body (json/generate-string payload)
                               :content-type :json
                               :as :json})]
      (log/debug "Sent typing indicator to:" user-id)
      {:success true})
    (catch Exception e
      (log/warn "Failed to send typing indicator:" (.getMessage e))
      {:success false})))

(defn get-user-profile
  "Get user profile information

  Parameters:
  - access-token: Zalo OA access token
  - user-id: Zalo user ID

  Returns:
  - User profile map with :name, :avatar, etc."
  [access-token user-id]
  (try
    (let [url (str zalo-api-base "/getprofile")
          response (http/get url
                             {:headers (make-headers access-token)
                              :query-params {:data (json/generate-string
                                                     {:user_id user-id})}
                              :as :json})]
      (log/debug "Retrieved profile for user:" user-id)
      {:success true
       :data (:body response)})
    (catch Exception e
      (log/error "Failed to get user profile:" (.getMessage e))
      {:success false
       :error (sanitizer/sanitize-error (.getMessage e))})))

(defn refresh-access-token
  "Refresh Zalo OA access token using refresh token

  Parameters:
  - app-id: Zalo app ID
  - refresh-token: Current refresh token
  - secret-key: App secret key

  Returns:
  - New access token and refresh token"
  [app-id refresh-token secret-key]
  (try
    (let [url "https://oauth.zaloapp.com/v4/oa/access_token"
          response (http/post url
                              {:form-params {:app_id app-id
                                            :refresh_token refresh-token
                                            :grant_type "refresh_token"}
                               :headers {"secret_key" secret-key}
                               :as :json})]
      (log/info "Successfully refreshed Zalo access token")
      {:success true
       :data (:body response)})
    (catch Exception e
      (log/error "Failed to refresh access token:" (.getMessage e))
      {:success false
       :error (sanitizer/sanitize-error (.getMessage e))})))

(defn validate-webhook-signature
  "Validate webhook signature from Zalo

  Parameters:
  - payload: Raw request body
  - signature: X-ZEvent-Signature header
  - secret-key: App secret key

  Returns:
  - true if signature is valid"
  [payload signature secret-key]
  (try
    (let [computed-sig (-> (str payload secret-key)
                          (.getBytes "UTF-8")
                          (java.security.MessageDigest/getInstance "SHA-256")
                          (.digest)
                          (javax.xml.bind.DatatypeConverter/printHexBinary)
                          (.toLowerCase))]
      (= computed-sig signature))
    (catch Exception e
      (log/error "Failed to validate webhook signature:" (.getMessage e))
      false)))
