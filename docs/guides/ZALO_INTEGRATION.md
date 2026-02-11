# Hướng Dẫn Tích Hợp Zalo Bot với AOS

## Tổng Quan

AOS giờ đây có thể giao tiếp với người dùng qua Zalo Official Account (OA). Tích hợp này cho phép:

- ✅ Nhận và trả lời tin nhắn từ Zalo users
- ✅ Sử dụng toàn bộ khả năng AI của AOS qua Zalo
- ✅ Webhook server tự động xử lý events
- ✅ Typing indicator và UX tốt hơn
- ✅ Persistent memory về users và conversations

## Kiến Trúc

```
Zalo User → Zalo Platform → Webhook (AOS) → Chat Handler → Claude API
                                    ↓
                              Response Handler → Zalo Platform → User
```

### Components

1. **zalo/client.clj** - Zalo API client (send messages, get profile)
2. **zalo/handler.clj** - Process messages & events
3. **zalo/server.clj** - HTTP webhook server (Ring + Jetty)
4. **config.edn** - Zalo configuration

## Bước 1: Đăng Ký Zalo Official Account

### 1.1. Tạo Zalo OA

1. Truy cập: https://oa.zalo.me/
2. Đăng nhập với tài khoản Zalo
3. Chọn "Tạo Official Account mới"
4. Điền thông tin:
   - Tên OA: "AOS Bot" (hoặc tên bạn muốn)
   - Mô tả: "AI Agent tự cải tiến"
   - Danh mục: "Công nghệ"

### 1.2. Lấy Credentials

1. Vào **Cài đặt** → **Ứng dụng**
2. Tạo ứng dụng mới hoặc chọn ứng dụng có sẵn
3. Lấy thông tin:
   - **App ID**: (ví dụ: 1234567890123456789)
   - **Secret Key**: (giữ bí mật!)
   - **Access Token**: Tạo mới nếu chưa có
   - **Refresh Token**: Để refresh access token khi hết hạn

### 1.3. Cấu Hình Webhook

1. Vào **Cài đặt** → **Webhook**
2. Nhập **Webhook URL**: `https://your-domain.com/webhook`
   - Nếu chạy local: dùng **ngrok** (xem phần dưới)
3. Chọn events cần nhận:
   - ✅ `user_send_text` - User gửi tin nhắn text
   - ✅ `follow` - User follow OA
   - ✅ `unfollow` - User unfollow OA
4. Lưu cấu hình

## Bước 2: Cài Đặt Environment Variables

### 2.1. Tạo File Environment

```bash
# Tạo file .env (hoặc thêm vào ~/.bashrc)
cat >> ~/.bashrc << 'EOF'

# Zalo OA Configuration
export ZALO_APP_ID="your_app_id_here"
export ZALO_ACCESS_TOKEN="your_access_token_here"
export ZALO_REFRESH_TOKEN="your_refresh_token_here"
export ZALO_SECRET_KEY="your_secret_key_here"

EOF

# Reload bashrc
source ~/.bashrc
```

### 2.2. Verify Variables

```bash
echo $ZALO_APP_ID
echo $ZALO_ACCESS_TOKEN
# Should print your credentials (not empty)
```

## Bước 3: Cấu Hình AOS

### 3.1. Update config.edn

File `resources/config.edn` đã có sẵn cấu hình Zalo:

```clojure
:zalo {:enabled false                    ; Set true để bật
       :port 3000                        ; Port cho webhook server
       :app-id #env ZALO_APP_ID         ; Đọc từ environment
       :access-token #env ZALO_ACCESS_TOKEN
       :refresh-token #env ZALO_REFRESH_TOKEN
       :secret-key #env ZALO_SECRET_KEY
       :webhook-url ""                   ; URL public của webhook
       :auto-reply true                  ; Tự động reply
       :welcome-message "Xin chào! ..."}
```

### 3.2. Enable Zalo Integration

```bash
# Edit config.edn
nano resources/config.edn

# Change :enabled false → :enabled true
:zalo {:enabled true
       ...
```

## Bước 4: Setup Webhook URL (Local Development)

Nếu bạn đang develop local, bạn cần **expose localhost** ra internet để Zalo có thể gọi webhook.

### 4.1. Sử dụng ngrok

```bash
# Install ngrok
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# Sign up at https://ngrok.com and get auth token
ngrok config add-authtoken YOUR_NGROK_AUTH_TOKEN

# Start ngrok tunnel
ngrok http 3000
```

Output:
```
Forwarding  https://abcd-1234.ngrok-free.app -> http://localhost:3000
```

### 4.2. Update Webhook URL

1. Copy ngrok URL: `https://abcd-1234.ngrok-free.app`
2. Vào Zalo OA dashboard → Webhook settings
3. Paste: `https://abcd-1234.ngrok-free.app/webhook`
4. Zalo sẽ gửi verification request (AOS tự động xử lý)

### 4.3. Alternative: Deploy to Cloud

Hoặc deploy AOS lên server/cloud với public IP:

- **VPS**: DigitalOcean, Linode, AWS EC2
- **Heroku**: Free tier có sẵn HTTPS
- **Railway**: Deploy từ Git repo

## Bước 5: Install Dependencies và Build

```bash
# Install dependencies
lein deps

# Rebuild JAR (nếu cần)
lein uberjar

# Hoặc run dev mode
lein run
```

## Bước 6: Start Zalo Bot

### 6.1. Start từ Code

Thêm vào `src/agent_os/core.clj`:

```clojure
(ns agent-os.core
  (:require ...
            [agent-os.integrations.zalo.server :as zalo-server]))

(defn -main [& args]
  ;; ... existing code ...

  ;; Start Zalo webhook server if enabled
  (when (get-in config [:zalo :enabled])
    (let [zalo-config (:zalo config)
          zalo-context {:kernel kernel
                       :llm-registry llm-registry
                       :memory memory
                       :config config}]
      (zalo-server/start-server zalo-context zalo-config)))

  ;; ... rest of code ...
  )
```

### 6.2. Start AOS

```bash
# Run AOS
./aos

# AOS sẽ khởi động cả CLI và Zalo webhook server
# Output:
# ✓ Zalo webhook server running at http://localhost:3000
# Webhook URL: /webhook
# Health check: /health
```

### 6.3. Test Webhook

```bash
# Test health check
curl http://localhost:3000/health

# Expected output:
# {"status":"ok","service":"AOS Zalo Bot"}
```

## Bước 7: Test với Zalo

### 7.1. Test trên Mobile

1. Mở app Zalo trên điện thoại
2. Search tên OA của bạn
3. Click "Quan tâm" (Follow)
4. Gửi tin nhắn: "Xin chào"
5. AOS sẽ reply với câu trả lời thông minh!

### 7.2. Monitor Logs

```bash
# Terminal 1: Run AOS
./aos

# Terminal 2: Tail logs
tail -f logs/aos.log

# Bạn sẽ thấy:
# INFO - Received message from User (ID: 1234567890): Xin chào
# INFO - Generated response for User: Xin chào! Tôi là AOS...
# INFO - Sent message to Zalo user: 1234567890
```

## Troubleshooting

### Issue 1: Webhook không nhận được requests

**Nguyên nhân:**
- ngrok tunnel chưa start
- Webhook URL sai
- Firewall block port 3000

**Giải pháp:**
```bash
# Check ngrok
curl https://your-ngrok-url.ngrok-free.app/health

# Check local server
curl http://localhost:3000/health

# Check port listening
netstat -tulpn | grep 3000
```

### Issue 2: Access token hết hạn

**Nguyên nhân:** Zalo access token có thời hạn (thường 90 ngày)

**Giải pháp:**
```clojure
;; Refresh token trong REPL
(require '[agent-os.integrations.zalo.client :as zalo])

(zalo/refresh-access-token
  (System/getenv "ZALO_APP_ID")
  (System/getenv "ZALO_REFRESH_TOKEN")
  (System/getenv "ZALO_SECRET_KEY"))

;; Update ZALO_ACCESS_TOKEN với token mới
```

### Issue 3: Messages không được reply

**Nguyên nhân:**
- Claude API key không đúng
- LLM service lỗi
- Handler bị exception

**Giải pháp:**
```bash
# Check Claude API key
echo $ANTHROPIC_API_KEY

# Check logs
tail -f logs/aos.log | grep ERROR

# Test chat trực tiếp trong CLI
./aos
aos> chat Xin chào
```

### Issue 4: Signature validation failed

**Nguyên nhân:** Secret key không khớp hoặc payload bị modify

**Giải pháp:**
- Double-check `ZALO_SECRET_KEY`
- Temporarily disable signature validation để debug
- Check webhook payload format

## Advanced Configuration

### Custom Welcome Message

Edit `resources/config.edn`:

```clojure
:zalo {:welcome-message "Chào mừng bạn! Tôi là AOS - AI agent có khả năng tự cải tiến code của chính mình. Hãy hỏi tôi bất cứ điều gì!"}
```

### Multi-Language Support

AOS tự động detect ngôn ngữ từ tin nhắn user:

```clojure
;; Trong handler.clj, thêm language detection
(defn detect-language [text]
  (if (re-find #"[À-ỹ]" text)
    :vietnamese
    :english))
```

### Rate Limiting

Để tránh spam, thêm rate limiting:

```clojure
;; Trong handler.clj
(def user-message-count (atom {}))

(defn rate-limit? [user-id]
  (let [count (get @user-message-count user-id 0)]
    (if (> count 10) ; Max 10 messages/minute
      true
      (do
        (swap! user-message-count update user-id (fnil inc 0))
        false))))
```

### Analytics

Track usage statistics:

```clojure
;; Store metrics in memory
(memory/store-fact memory
  {:type :zalo-analytics
   :user-id user-id
   :message-length (count message-text)
   :response-length (count response)
   :processing-time-ms (- end start)
   :timestamp (System/currentTimeMillis)})
```

## Production Deployment

### 1. Security Checklist

- [ ] Enable signature validation
- [ ] Use HTTPS (không dùng HTTP)
- [ ] Store credentials an toàn (vault/secret manager)
- [ ] Enable rate limiting
- [ ] Set up monitoring & alerts
- [ ] Log sanitization (không log sensitive data)

### 2. Deploy to VPS

```bash
# SSH to server
ssh user@your-server.com

# Clone repo
git clone https://github.com/your-org/aos.git
cd aos

# Set environment variables
nano ~/.bashrc  # Add ZALO_* and ANTHROPIC_API_KEY

# Build
lein uberjar

# Run with systemd
sudo nano /etc/systemd/system/aos-zalo.service
```

**aos-zalo.service:**
```ini
[Unit]
Description=AOS Zalo Bot
After=network.target

[Service]
Type=simple
User=aos
WorkingDirectory=/home/aos/aos
ExecStart=/usr/bin/java -jar target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Start service
sudo systemctl enable aos-zalo
sudo systemctl start aos-zalo
sudo systemctl status aos-zalo
```

### 3. Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /webhook {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## API Reference

### Zalo Client Functions

```clojure
;; Send text message
(zalo/send-text-message access-token user-id "Hello!")

;; Send typing indicator
(zalo/send-typing-indicator access-token user-id)

;; Get user profile
(zalo/get-user-profile access-token user-id)

;; Refresh access token
(zalo/refresh-access-token app-id refresh-token secret-key)
```

### Server Functions

```clojure
;; Start server
(def server (zalo-server/start-server context config))

;; Stop server
(zalo-server/stop-server server)

;; Restart server
(zalo-server/restart-server server context config)
```

## Examples

### Example 1: Custom Command Handler

Thêm commands đặc biệt:

```clojure
(defn process-message [event context]
  (let [text (extract-message-text event)]
    (cond
      (= text "/help")
      "Commands: /status, /memory, /improve"

      (= text "/status")
      (str "System status: " (kernel/get-status (:kernel context)))

      :else
      (gateway/process-chat-message text context))))
```

### Example 2: Scheduled Messages

Gửi tin nhắn định kỳ:

```clojure
(require '[clojure.core.async :refer [go-loop timeout]])

(defn start-daily-reminder [access-token user-id]
  (go-loop []
    (<! (timeout (* 24 60 60 1000))) ; 24 hours
    (zalo/send-text-message
      access-token
      user-id
      "Nhắc nhở hàng ngày: Đã tự cải tiến code hôm nay chưa? 😊")
    (recur)))
```

## FAQ

**Q: Có thể handle tin nhắn hình ảnh không?**
A: Có, extend handler để xử lý `user_send_image` event.

**Q: Cost bao nhiêu?**
A: Zalo OA free. Bạn chỉ trả phí Claude API (~$3/1M tokens).

**Q: Có thể tích hợp nhiều OA không?**
A: Có, chạy multiple instances với configs khác nhau.

**Q: Làm sao để bot reply nhanh hơn?**
A: Dùng Haiku cho simple queries, cache frequent responses.

## Kết Luận

Bây giờ AOS của bạn đã có thể giao tiếp với users qua Zalo! 🎉

Người dùng có thể:
- Chat với AI agent trực tiếp trên Zalo
- Hỏi về bất kỳ topic nào
- Nhận responses thông minh từ Claude
- Tận dụng toàn bộ khả năng self-improvement của AOS

## Support

Nếu gặp vấn đề:
1. Check logs: `logs/aos.log`
2. Test webhook: `curl http://localhost:3000/health`
3. Verify credentials trong environment variables
4. Check Zalo OA dashboard for webhook status

Happy coding! 🚀
