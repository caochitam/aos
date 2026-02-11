# AOS Quick Start - Setup API Key Lâu Dài

## 🚀 Setup Nhanh (2 phút)

### Bước 1: Chạy Script Tự Động

```bash
cd /root/aos
./setup_api_key.sh
```

Script sẽ hỏi bạn chọn phương thức và tự động setup!

### Bước 2: Chọn Phương Thức

```
1. Add to ~/.bashrc (Recommended) ← CHỌN CÁI NÀY
2. Use secure file with chmod 600
3. Add to ~/.profile (System-wide)
4. Use systemd environment.d (Advanced)
```

**Khuyến nghị:** Chọn số **1** hoặc **2** (an toàn nhất)

### Bước 3: Nhập API Key

Khi được hỏi, paste API key của bạn:
```
Enter your Anthropic API key: sk-ant-api03-xxxxx...
```

### Bước 4: Reload Config

```bash
source ~/.bashrc
```

### Bước 5: Kiểm Tra

```bash
# Xem API key đã được set chưa
echo $ANTHROPIC_API_KEY

# Nếu thấy: sk-ant-api03-... thì OK! ✅
```

### Bước 6: Chạy AOS

```bash
lein run
```

---

## 📝 Setup Thủ Công (Nếu không dùng script)

### Phương Pháp 1: Thêm trực tiếp vào ~/.bashrc

```bash
# 1. Mở file
nano ~/.bashrc

# 2. Thêm vào cuối file:
export ANTHROPIC_API_KEY="sk-ant-api03-YOUR-KEY-HERE"

# 3. Save: Ctrl+O, Enter, Ctrl+X

# 4. Reload
source ~/.bashrc

# 5. Test
echo $ANTHROPIC_API_KEY
```

### Phương Pháp 2: Dùng File Riêng (An toàn hơn)

```bash
# 1. Tạo file key (chỉ owner đọc được)
echo "sk-ant-api03-YOUR-KEY-HERE" > ~/.anthropic_key
chmod 600 ~/.anthropic_key

# 2. Thêm loader vào ~/.bashrc
cat >> ~/.bashrc << 'EOF'

# Load Anthropic API Key
if [ -f ~/.anthropic_key ]; then
    export ANTHROPIC_API_KEY="$(cat ~/.anthropic_key)"
fi
EOF

# 3. Reload
source ~/.bashrc

# 4. Test
echo $ANTHROPIC_API_KEY
```

---

## ✅ Verification Checklist

```bash
# 1. Check environment variable
[ -n "$ANTHROPIC_API_KEY" ] && echo "✅ API key is set" || echo "❌ API key NOT set"

# 2. Check key format
[[ "$ANTHROPIC_API_KEY" =~ ^sk-ant- ]] && echo "✅ Valid format" || echo "❌ Invalid format"

# 3. Check persistence (open new terminal and run)
echo $ANTHROPIC_API_KEY
# Should still show your key

# 4. Test AOS
cd /root/aos && lein run
# Should start without "No ANTHROPIC_API_KEY" error
```

---

## 🔍 Troubleshooting

### Vấn Đề: Terminal mới không thấy API key

**Nguyên nhân:** File config chưa được reload

**Giải pháp:**
```bash
# Kiểm tra file nào được load
echo $SHELL  # Nếu là /bin/bash, dùng .bashrc

# Reload thủ công
source ~/.bashrc

# Hoặc đóng terminal và mở lại
```

### Vấn Đề: API key có nhưng AOS báo lỗi

**Kiểm tra:**
```bash
# 1. Key có đúng format không?
echo $ANTHROPIC_API_KEY | grep "^sk-ant"

# 2. Key có bị thừa khoảng trắng không?
echo "$ANTHROPIC_API_KEY" | wc -c  # Should be ~96 characters

# 3. Trim whitespace
export ANTHROPIC_API_KEY=$(echo $ANTHROPIC_API_KEY | tr -d '[:space:]')
```

### Vấn Đề: Muốn đổi API key

**Giải pháp:**
```bash
# Option 1: Edit trực tiếp
nano ~/.bashrc
# Tìm dòng ANTHROPIC_API_KEY và sửa

# Option 2: Nếu dùng file riêng
echo "sk-ant-NEW-KEY" > ~/.anthropic_key

# Reload
source ~/.bashrc
```

---

## 🔒 Security Best Practices

✅ **DO (NÊN):**
- Set qua environment variables
- Dùng chmod 600 cho key files
- Rotate keys định kỳ (30-90 ngày)
- Thêm `~/.anthropic_key` vào `.gitignore`
- Backup key file (encrypted) nếu cần

❌ **DON'T (KHÔNG NÊN):**
- Hard-code trong source code
- Commit vào git
- Share qua Slack/email
- Set qua /etc/environment (system-wide)
- Log API key ra console

---

## 🎯 TL;DR (Quá Dài Không Đọc)

```bash
# One-liner setup (paste cái này và chạy):
echo "export ANTHROPIC_API_KEY='sk-ant-api03-YOUR-KEY-HERE'" >> ~/.bashrc && source ~/.bashrc && echo "✅ Done! Test: \$ANTHROPIC_API_KEY"

# Verify
echo $ANTHROPIC_API_KEY

# Run AOS
cd /root/aos && lein run
```

---

## 📚 Đọc Thêm

- [SECURITY.md](SECURITY.md) - Chi tiết về bảo mật
- [README.md](README.md) - Hướng dẫn đầy đủ
- Script: `./setup_api_key.sh` - Setup tự động

---

**Có vấn đề?** Open issue hoặc check logs tại `/tmp/aos-setup.log`
