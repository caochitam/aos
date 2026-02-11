# AOS Smart Task Delegation

## Kiến trúc mới:

```
User → AOS → Phân tích độ phức tạp
              │
              ├─ SIMPLE → AOS tools (read_file, edit_file, bash)
              │
              └─ COMPLEX → Claude Code CLI (full autonomous agent)
```

## Ví dụ:

### ✅ Simple Tasks (AOS tự xử lý):

```
aos> đọc file README.md
→ AOS dùng tool read_file

aos> chạy lệnh ls -la
→ AOS dùng tool bash

aos> xin chào
→ AOS chat thường
```

### 🚀 Complex Tasks (Delegate to Claude Code):

```
aos> sửa bản thân để nói tiếng việt tốt hơn
→ 🔄 Delegating to Claude Code...
→ Claude Code: reads files, makes edits, tests
→ ✅ Completed!

aos> refactor code trong src/agent_os/llm/
→ 🔄 Delegating to Claude Code...
→ Claude Code: analyzes, refactors, runs tests
→ ✅ Completed!

aos> tạo một component mới cho logging
→ 🔄 Delegating to Claude Code...
→ Claude Code: creates files, implements features
→ ✅ Completed!
```

## Từ khóa phức tạp (trigger delegation):

- **Modification**: sửa, modify, refactor, improve, tối ưu
- **Creation**: tạo, create, viết, implement
- **Analysis**: phân tích, analyze, debug, fix
- **Self-modification**: bản thân, chính mình, yourself
- **Multiple files**: nhiều file, files

## Lợi ích:

1. ✅ **Hiệu quả chi phí**: Complex tasks ít API calls hơn
2. ✅ **Tools mạnh hơn**: Claude Code có Edit/Write/Read tốt hơn
3. ✅ **Autonomy**: AOS vẫn tự làm được tasks đơn giản
4. ✅ **Best of both worlds**: Kết hợp self-awareness + execution power

## Yêu cầu:

- Claude Code CLI phải được cài đặt: `npm install -g @anthropic/claude-code`
- Hoặc set env var để skip delegation nếu không có Claude Code
