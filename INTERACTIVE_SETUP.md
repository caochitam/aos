# Interactive API Key Setup - Hướng Dẫn

## 🎯 Tính Năng Mới

AOS giờ đây **tự động kiểm tra API key** khi khởi động. Nếu chưa có, AOS sẽ:
1. ✅ Phát hiện thiếu API key
2. ✅ Hỏi bạn có muốn setup không
3. ✅ Hướng dẫn bạn nhập API key
4. ✅ Cho bạn chọn phương thức lưu
5. ✅ Tự động cấu hình cho bạn
6. ✅ Tiếp tục khởi động AOS

**Bạn KHÔNG CẦN làm gì thêm** - chỉ cần chạy `lein run` và làm theo hướng dẫn!

---

## 🚀 Cách Sử Dụng

### Lần Đầu Chạy AOS (Chưa có API key)

```bash
$ lein run
```

AOS sẽ tự động hiện ra:

```
==============================================
   AOS First-Time Setup
==============================================

Welcome to AOS! 🚀

I noticed you don't have an ANTHROPIC_API_KEY set.
Let's set that up now so AOS can use Claude API.

Don't have an API key yet?
→ Get one at: https://console.anthropic.com/settings/keys

Would you like to set up your API key now? (Y/n):
```

### Bước 1: Xác Nhận Setup

```
Would you like to set up your API key now? (Y/n): y
```

- Nhấn `y` hoặc `Enter` để setup ngay
- Nhấn `n` để bỏ qua (setup sau)

### Bước 2: Nhập API Key

```
Please enter your Anthropic API key:
(Format: sk-ant-api03-...)

API Key: [input hidden]
```

- Paste API key của bạn (input sẽ bị ẩn nếu có thể)
- Format phải là: `sk-ant-api03-...`
- AOS sẽ validate format trước khi lưu

### Bước 3: Chọn Phương Thức Lưu

```
How would you like to save the API key?

1. Add to ~/.bashrc or ~/.zshrc (Recommended)
   ✓ Permanent, loads automatically
   ✓ Simple and reliable

2. Use secure file ~/.anthropic_key
   ✓ Most secure (chmod 600)
   ✓ Easy to rotate keys
   ✓ Key stored separately

3. Current session only (Temporary)
   ⚠ Will be lost when you exit AOS
   ✓ Good for testing

4. Skip (I'll set it manually later)

Enter your choice (1-4) [default: 2]:
```

#### Option 1: Thêm vào ~/.bashrc
- ✅ Đơn giản, lâu dài
- ✅ Tự động load mỗi terminal
- API key được thêm trực tiếp vào file bashrc/zshrc

#### Option 2: Secure File (RECOMMENDED) ⭐
- ✅✅✅ **An toàn nhất**
- ✅ Key riêng file `~/.anthropic_key` với chmod 600
- ✅ Dễ rotate/thay key
- ✅ Không expose trong bashrc

#### Option 3: Session Only
- ⚠️ Tạm thời, mất khi thoát AOS
- ✅ Tốt cho testing
- Set cho JVM process hiện tại

#### Option 4: Skip
- Bỏ qua, setup thủ công sau

### Bước 4: Hoàn Thành

```
📝 Creating secure key file: ~/.anthropic_key
✓ Created ~/.anthropic_key with permissions 600
✓ Backed up to: ~/.bashrc.backup.1234567890
✓ Added loader to ~/.bashrc

✅ Setup complete!

⚠ IMPORTANT: Run this command to activate in current session:
   source ~/.bashrc

Or restart your terminal.
```

### Bước 5: Activate

```bash
# Activate trong session hiện tại
source ~/.bashrc

# Hoặc mở terminal mới - sẽ tự động load
```

### Bước 6: AOS Tiếp Tục Khởi Động

```
✓ ANTHROPIC_API_KEY is configured
Initializing Agent OS...
aos> _
```

---

## 🎬 Demo Flow Hoàn Chỉnh

```bash
# 1. Chạy AOS lần đầu (chưa có key)
$ lein run

# 2. AOS phát hiện thiếu key
==============================================
   AOS First-Time Setup
==============================================

Welcome to AOS! 🚀
[...]

# 3. Bạn chọn setup
Would you like to set up your API key now? (Y/n): y

# 4. Nhập API key
Please enter your Anthropic API key:
API Key: sk-ant-api03-[paste your key here]

✓ API key accepted
  Prefix: sk-ant-api03-xxxxx...

# 5. Chọn phương thức (recommend: 2)
Enter your choice (1-4) [default: 2]: 2

# 6. Auto setup
📝 Creating secure key file...
✓ Created ~/.anthropic_key with permissions 600
✓ Added loader to ~/.bashrc

✅ Setup complete!

# 7. Activate
$ source ~/.bashrc

# 8. Chạy lại AOS
$ lein run

✓ ANTHROPIC_API_KEY is configured
aos> status
[AOS running normally]
```

---

## 🔐 Security Features

### Validation
- ✅ Check API key format (`sk-ant-*`)
- ✅ Validate length (> 20 chars)
- ✅ Reject empty/blank keys

### Input Protection
- ✅ Password hidden during input (if console available)
- ✅ Never echoed to terminal
- ✅ Never logged

### File Security
- ✅ Auto backup before modifying files
- ✅ Secure permissions (chmod 600)
- ✅ Timestamps on backups

### Runtime Protection
- ✅ Sanitization still active (prompt injection protection)
- ✅ Keys never exposed in logs
- ✅ Safe error messages

---

## 🛠 Advanced Usage

### Test Interactive Setup

```bash
# Temporarily unset API key to test
unset ANTHROPIC_API_KEY

# Run AOS - interactive setup will trigger
lein run
```

### Restore/Change API Key

```bash
# Method 1: Edit secure file
echo "sk-ant-NEW-KEY" > ~/.anthropic_key

# Method 2: Edit bashrc
nano ~/.bashrc
# Find and update ANTHROPIC_API_KEY line

# Reload
source ~/.bashrc
```

### Check Current Setup

```bash
# Check if key is set
echo $ANTHROPIC_API_KEY

# Check which method was used
if [ -f ~/.anthropic_key ]; then
    echo "Using secure file method"
    cat ~/.anthropic_key
fi

# Check bashrc
grep "ANTHROPIC_API_KEY" ~/.bashrc
```

### Manual Override

```bash
# Override with temporary key (just for this run)
ANTHROPIC_API_KEY="sk-ant-temp-key" lein run
```

---

## 📊 Comparison: Before vs After

### BEFORE (Old Way)
```bash
# User had to:
1. Know about ANTHROPIC_API_KEY
2. Manually export it
3. Remember to do it every session
4. Or manually edit bashrc
5. Deal with setup complexity

$ export ANTHROPIC_API_KEY="sk-ant-..."
$ lein run
```

### AFTER (New Way) ⭐
```bash
# User just needs to:
1. Run AOS
2. Follow interactive prompts
3. Done!

$ lein run
[Interactive setup guides you through everything]
```

---

## ❓ FAQ

### Q: Có bắt buộc phải setup không?
**A:** Không! Bạn có thể:
- Chọn "Skip" trong interactive setup
- Nhấn `n` khi được hỏi
- Setup thủ công sau bằng `export` hoặc edit bashrc

### Q: Setup một lần hay mỗi lần?
**A:** **Một lần duy nhất!** Sau khi setup:
- ✅ Mở terminal mới → API key tự động có
- ✅ Reboot máy → API key vẫn còn
- ✅ Chạy AOS → Không hỏi lại nữa

### Q: Nếu tôi đã có API key trong env?
**A:** AOS sẽ phát hiện và không hỏi setup nữa:
```
✓ ANTHROPIC_API_KEY is configured
Starting AOS...
```

### Q: Có thể đổi API key sau không?
**A:** Có! Dễ dàng:
```bash
# Nếu dùng secure file:
echo "NEW-KEY" > ~/.anthropic_key

# Nếu dùng bashrc:
nano ~/.bashrc  # Edit dòng ANTHROPIC_API_KEY

# Reload
source ~/.bashrc
```

### Q: Method nào an toàn nhất?
**A:** **Option 2 (Secure File)** - chmod 600, riêng file, dễ rotate

### Q: Có thể test mà không setup vĩnh viễn?
**A:** Có! Chọn **Option 3 (Session only)** - chỉ tồn tại khi AOS đang chạy

### Q: File backup ở đâu?
**A:** Backup files có format:
```
~/.bashrc.backup.1707654321000
~/.zshrc.backup.1707654321000
```

### Q: Có thể disable interactive setup không?
**A:** Có! Set key trước khi chạy:
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
lein run  # Sẽ không hỏi setup
```

---

## 🎉 Benefits

| Feature | Before | After |
|---------|--------|-------|
| **First-time setup** | Manual, confusing | ✅ Automated, guided |
| **User experience** | Error messages | ✅ Helpful prompts |
| **Security** | User's responsibility | ✅ Built-in best practices |
| **Documentation** | User must read docs | ✅ Interactive guide |
| **Mistakes** | Easy to misconfigure | ✅ Validated & safe |
| **Time to start** | 5-10 minutes | ✅ 30 seconds |

---

## 🔗 Related Docs

- [QUICK_START.md](QUICK_START.md) - Manual setup methods
- [SECURITY.md](SECURITY.md) - Security details
- [README.md](README.md) - General documentation

---

**Enjoy AOS with zero-hassle API key setup!** 🚀
