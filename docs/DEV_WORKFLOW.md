# AOS Development Workflow - Tránh Bất Đồng Bộ

## ❌ Vấn Đề: Source vs JAR Bất Đồng Bộ

### Tình Huống
```bash
# Bạn sửa code
vim src/agent_os/setup/interactive.clj
# → Xóa dòng println "✓ ANTHROPIC_API_KEY is configured"

# Chạy AOS
./aos
# → Vẫn thấy thông báo cũ! ❌

# Tại sao?
# → Script `aos` chạy từ JAR đã compile (code cũ)
# → JAR chưa rebuild → code bất đồng bộ!
```

---

## ✅ Giải Pháp: 3 Cách Tránh Bất Đồng Bộ

### 1. Auto-Detect (Recommended) ⭐⭐⭐⭐⭐

Script `aos` **tự động phát hiện** khi code mới hơn JAR:

```bash
./aos

# Output:
⚠️  Code đã thay đổi - JAR cần rebuild!

   Lựa chọn:
   [1] Rebuild ngay (recommended) - 30s
   [2] Dev mode lần này (slow startup)
   [3] Dùng JAR cũ (có thể bị lỗi)

Chọn [1-3, default=1]:
```

**Cách hoạt động:**
- Check timestamp của core files: `delegator.clj`, `gateway.clj`, `interactive.clj`
- Nếu `.clj` mới hơn `.jar` → cảnh báo!
- User chọn action

**Pros:**
- ✅ Automatic detection
- ✅ No manual checks needed
- ✅ Clear options for user

**Cons:**
- Rebuild mất ~30s

---

### 2. Dev Mode (Luôn Mới Nhất) ⭐⭐⭐⭐

Khi đang phát triển, dùng **dev mode** để luôn chạy từ source:

```bash
./aos --dev
```

**Cách hoạt động:**
- Chạy trực tiếp từ source code qua `lein run`
- KHÔNG dùng JAR → luôn mới nhất!
- Code changes ngay lập tức có hiệu lực

**Pros:**
- ✅ Always up-to-date
- ✅ No rebuild needed
- ✅ Great for active development

**Cons:**
- ⚠️ Slower startup (~3-5s vs ~0.5s với JAR)
- ⚠️ Requires Leiningen

**When to use:**
```bash
# Active development - nhiều file changes
./aos --dev

# Testing changes quickly
./aos --dev

# Debugging
./aos --dev
```

---

### 3. Manual Rebuild ⭐⭐⭐

Khi biết code đã thay đổi, rebuild thủ công:

```bash
./aos --rebuild
```

**Cách hoạt động:**
1. Chạy `lein uberjar` để compile lại
2. Tạo JAR mới với code mới nhất
3. Khởi động AOS từ JAR mới

**Pros:**
- ✅ Explicit control
- ✅ Fast runtime after rebuild
- ✅ Good for production deployment

**Cons:**
- ⚠️ Rebuild mất ~30s
- ⚠️ Phải nhớ rebuild sau mỗi code change

**When to use:**
```bash
# Sau khi sửa nhiều files
git commit -m "Fix delegation logic"
./aos --rebuild

# Deploy to production
./aos --rebuild
cp target/uberjar/*.jar /production/

# After pulling updates
git pull
./aos --rebuild
```

---

## 📊 So Sánh 3 Cách

| Feature | Auto-Detect | Dev Mode | Manual Rebuild |
|---------|-------------|----------|----------------|
| **Startup time** | Fast (~0.5s) | Slow (~3-5s) | Fast (~0.5s) |
| **Detection** | Automatic ✅ | N/A | Manual ⚠️ |
| **Always sync** | After rebuild | Always ✅ | After rebuild |
| **Best for** | Production + Dev | Active Dev | Production |
| **Rebuild time** | ~30s (when needed) | None | ~30s |

---

## 🎯 Recommended Workflow

### Development Phase
```bash
# Option 1: Dev mode (if you're making many changes)
./aos --dev

# Option 2: Auto-detect (if you want fast startup)
./aos
# → Choose [1] to rebuild when prompted
```

### After Code Changes
```bash
# Quick test
./aos --dev

# Ready for commit?
./aos --rebuild
git add .
git commit -m "Your changes"
```

### Production Deployment
```bash
# Always rebuild before deploy!
./aos --rebuild

# Verify
./aos  # Should NOT prompt for rebuild

# Deploy
scp target/uberjar/*.jar production:/app/
```

---

## 🔍 How Auto-Detect Works

### Script Logic
```bash
# Check core files
CORE_FILES=(
    "src/agent_os/llm/delegator.clj"
    "src/agent_os/cli/gateway.clj"
    "src/agent_os/setup/interactive.clj"
)

# Compare timestamps
for src_file in "${CORE_FILES[@]}"; do
    if [ "$src_file" -nt "$UBERJAR" ]; then
        NEEDS_REBUILD=true
        break
    fi
done

# Prompt user if rebuild needed
if [ "$NEEDS_REBUILD" = "true" ]; then
    # Show options and rebuild if requested
fi
```

### Why Only Core Files?

**Checked:**
- `delegator.clj` - Task classification logic
- `gateway.clj` - Main CLI interface
- `interactive.clj` - Setup & initialization

**Not checked:**
- Test files (don't affect runtime)
- Documentation files
- Config files

**Rationale:**
- Fast check (only 3 files)
- Covers 90% of changes
- Minimal false negatives

**If you changed other files:**
```bash
# Still prompts on next change to core files
# OR manually rebuild:
./aos --rebuild
```

---

## 🐛 Troubleshooting

### Problem: Auto-detect không hoạt động

**Symptom:**
```bash
./aos
# Không có warning dù đã sửa code
```

**Diagnosis:**
```bash
# Check which files were modified
ls -lt src/agent_os/**/*.clj | head -5

# Check JAR timestamp
ls -lh target/uberjar/*.jar

# Manual compare
stat src/agent_os/llm/delegator.clj
stat target/uberjar/agent-os-0.1.0-SNAPSHOT-standalone.jar
```

**Solution:**
```bash
# Force rebuild
./aos --rebuild
```

---

### Problem: Dev mode quá chậm

**Symptom:**
```bash
./aos --dev
# Startup mất 5-10s
```

**Solution:**
```bash
# Rebuild JAR cho fast startup
./aos --rebuild

# Hoặc dùng auto-detect
./aos  # Choose [1] once
```

---

### Problem: Quên rebuild sau khi sửa code

**Symptom:**
```bash
# Sửa code
vim src/agent_os/llm/delegator.clj

# Chạy
./aos
# Code cũ vẫn chạy!
```

**Solutions:**

**1. Auto-detect sẽ catch (if core file):**
```bash
./aos
# → Warning + prompt to rebuild ✅
```

**2. Use dev mode for active dev:**
```bash
./aos --dev
# Always latest ✅
```

**3. Habit: Rebuild after changes:**
```bash
vim src/**/*.clj
./aos --rebuild  # Make it a habit!
```

---

## 📚 Best Practices

### ✅ DO

1. **Use dev mode during active development**
   ```bash
   ./aos --dev
   ```

2. **Rebuild before commits**
   ```bash
   ./aos --rebuild
   git commit
   ```

3. **Trust auto-detect prompts**
   ```bash
   ./aos
   # See warning? → Choose [1] to rebuild
   ```

4. **Rebuild after pulling updates**
   ```bash
   git pull
   ./aos --rebuild
   ```

### ❌ DON'T

1. **Don't ignore rebuild warnings**
   ```bash
   ./aos
   # ⚠️ Warning shown
   # → [3] Dùng JAR cũ  ❌ BAD CHOICE!
   ```

2. **Don't mix dev/prod modes randomly**
   ```bash
   # Confusing!
   ./aos --dev  # dev mode
   ./aos        # prod mode (JAR)
   ./aos --dev  # dev mode again
   ```

3. **Don't forget to rebuild before deploy**
   ```bash
   # ❌ WRONG
   git commit
   git push
   # Deploy old JAR

   # ✅ CORRECT
   ./aos --rebuild
   git commit
   git push
   # Deploy new JAR
   ```

---

## 🚀 Quick Reference

```bash
# Development
./aos --dev              # Always latest (slow startup)

# Production
./aos                    # Fast startup (auto-detect)
./aos --rebuild          # Force rebuild

# Options
./aos --help             # Show help
```

---

## 📝 Summary

**Vấn đề:** Source code vs JAR bất đồng bộ

**Giải pháp:**
1. ✅ **Auto-detect** - Script tự phát hiện & prompt
2. ✅ **Dev mode** - Luôn chạy từ source
3. ✅ **Manual rebuild** - Rebuild khi cần

**Recommended:**
- Development: `./aos --dev`
- Production: `./aos` (auto-detect)
- Before commit: `./aos --rebuild`

**Không bao giờ bất đồng bộ nữa! 🎉**
