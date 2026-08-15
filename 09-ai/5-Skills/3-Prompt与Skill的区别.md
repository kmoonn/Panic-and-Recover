---
tags:
  - AI
  - Skills
  - Prompt
category: AI/Skills
---

# Prompt 与 Skill 的区别

## 核心对比

| 维度 | Prompt | Skill |
|------|--------|-------|
| **本质** | 一段自然语言指令 | 结构化的"提示+工具"能力包 |
| **形式** | 自由文本 | Frontmatter + Prompt 模板 + 工具绑定 + I/O Schema |
| **工具** | 无绑定，手动指定 | 声明式绑定，自动注入 |
| **输入校验** | 无 | Input Schema 自动校验 |
| **输出约束** | 无 | Output Schema 格式约束 |
| **可复用** | 复制粘贴 | 模块化引用，参数化 |
| **可组合** | 无法链式调用 | 支持链式组合（A→B） |
| **可评估** | 难以量化 | 有 evals 测试数据 |
| **版本管理** | 无 | 可版本化迭代 |
| **类比** | 菜谱手写纸条 | 标准化菜谱卡 |

---

## 从 Prompt 到 Skill 的演进

```
Level 0: 原始 Prompt
  "帮我审查这段代码"

Level 1: 结构化 Prompt
  "你是一个代码审查员。请从安全性、规范性、性能三个维度审查代码。
   输出格式：[{severity, file, line, description, suggestion}]"

Level 2: Prompt + 工具
  "你是一个代码审查员。请用 read_file 读取代码，用 git_diff 查看变更，
   用 run_linter 运行检查。审查维度：安全性、规范性、性能。"

Level 3: Skill（完全结构化）
  name: code-review
  tools: [read_file, git_diff, run_linter]
  input:  {file_path: string, check_types: array}
  output: {issues: Issue[], summary: string}
  prompt: "..."
  evals: [{input: "...", expected: "..."}]
```

每一层解决上一层的痛点：

| 痛点 | Level 0 | Level 1 | Level 2 | Level 3 |
|------|---------|---------|---------|---------|
| 输出不一致 | ✗ | 部分解决 | 部分解决 | ✅ Schema 约束 |
| 无法获取信息 | ✗ | ✗ | ✅ 绑定工具 | ✅ 自动注入 |
| 输入不规范 | ✗ | ✗ | ✗ | ✅ Input 校验 |
| 不可复用 | ✗ | ✗ | ✗ | ✅ 参数化模板 |
| 无法评估 | ✗ | ✗ | ✗ | ✅ evals 数据 |
| 不可组合 | ✗ | ✗ | ✗ | ✅ 链式调用 |

---

## 具体区别展开

### 1. 工具绑定

**Prompt**：告诉模型"你可以用 XX 工具"，但模型不一定用、不一定用对。

```
请使用 read_file 工具读取代码文件，然后审查代码安全性。
```

**Skill**：声明式绑定，框架自动将工具注入 Agent 上下文，保证可用。

```yaml
tools:
  - read_file
  - git_diff
  - run_linter
```

### 2. 输入输出约束

**Prompt**：靠自然语言描述期望的输入输出格式，模型可能不遵守。

```
请传入文件路径，输出 JSON 格式的审查结果。
```

**Skill**：用 Schema 严格约束，不符合则校验失败。

```yaml
input:
  type: object
  properties:
    file_path:
      type: string
      description: 代码文件路径
    check_types:
      type: array
      items:
        type: enum
        values: [security, performance, style]
      default: [security]
  required: [file_path]

output:
  type: object
  properties:
    issues:
      type: array
      items:
        type: object
        properties:
          severity: {type: enum, values: [critical, warning, info]}
          message: {type: string}
    summary:
      type: string
  required: [issues, summary]
```

### 3. 可复用性

**Prompt**：每次手动复制粘贴，参数硬编码或手动替换。

```
# 每次要改文件路径
请审查 /src/auth/login.py 的代码安全性
请审查 /src/payment/checkout.py 的代码安全性
```

**Skill**：参数化模板，一次定义多次使用。

```
Skill: code-review
调用1: {file_path: "/src/auth/login.py", check_types: [security]}
调用2: {file_path: "/src/payment/checkout.py", check_types: [security, performance]}
```

### 4. 可组合性

**Prompt**：两个 Prompt 之间无法直接串联。

**Skill**：支持链式组合，上一个 Skill 的输出作为下一个的输入。

```
用户: "Review and deploy the payment service"

Step 1: code-review Skill
  Input:  {file_path: "/services/payment"}
  Output: {issues: [...], can_deploy: true}

Step 2: deploy Skill
  Input:  {environment: "staging", version: "v2.1"}
  Output: {status: "success", url: "https://..."}

  → can_deploy=false 时，deploy Skill 自动拒绝
```

---

## 什么时候用 Prompt，什么时候用 Skill？

| 场景 | 推荐 | 原因 |
|------|------|------|
| 一次性简单问答 | Prompt | 不值得封装成 Skill |
| 快速实验/原型 | Prompt | 开发成本低，迭代快 |
| 需要调用工具 | Skill | 工具绑定是 Skill 的核心优势 |
| 多人复用 | Skill | 结构化、可分享、有版本 |
| 需要输入校验 | Skill | Schema 约束防错 |
| 需要输出可靠 | Skill | Output Schema 保证格式 |
| 需要链式组合 | Skill | Skill 间可串联 |
| 需要评估效果 | Skill | evals 数据可量化 |

**经验法则**：一个 Prompt 被复制粘贴超过 3 次 → 封装成 Skill。

---

## 与其他概念的关系

```
┌──────────────────────────────────────────────┐
│                 应用层                        │
│  Agent（自主决策，编排 Skills）                │
├──────────────────────────────────────────────┤
│               能力层                          │
│  Skill（Prompt + Tools + Schema）             │
│  ↑ 比 Prompt 多了：工具绑定、I/O 约束、评估    │
├──────────────────────────────────────────────┤
│               指令层                          │
│  Prompt（纯文本指令，无工具、无约束）           │
├──────────────────────────────────────────────┤
│               工具层                          │
│  MCP Tools（原子操作：read_file, kubectl...）  │
└──────────────────────────────────────────────┘
```

| 关系 | 说明 |
|------|------|
| **Skill ⊃ Prompt** | Skill 包含 Prompt，但不只是 Prompt |
| **Skill 使用 MCP** | Skill 调用 MCP 提供的原子工具 |
| **Agent 编排 Skill** | Agent 根据目标选择和组合 Skills |
| **Prompt → Skill → Agent** | 从简单到复杂的演进路径 |

---

## 面试高频

### Skill 能完全替代 Prompt 吗？

不能。Skill 有封装成本（写 Schema、绑定工具、写 evals），对于一次性简单任务，直接写 Prompt 更高效。Skill 适合**可复用、需要工具、需要可靠性**的场景。

### Prompt 工程和 Skill 工程的区别？

| 维度 | Prompt 工程 | Skill 工程 |
|------|-----------|-----------|
| 关注点 | 如何写好指令 | 如何封装可复用能力 |
| 核心技术 | 指令设计、Few-shot、CoT | Schema 设计、工具编排、评估 |
| 输出保证 | 靠 Prompt 质量引导 | 靠 Schema 强制约束 |
| 迭代方式 | 人工观察调整 | evals 自动化回归 |

### 为什么 Skill 需要评估（evals）？

Prompt 效果靠人眼看，不可量化、不可回归。Skill 的 evals 提供：
- **量化评估**：通过率是多少，有没有退化
- **回归保护**：改了 Prompt 后跑 evals，确认没有变差
- **持续优化**：每次改进都跑 evals 对比，数据驱动决策

---

## 一句话总结

> Prompt 是纯文本指令，Skill 是结构化的"Prompt+工具+Schema"能力包——比 Prompt 多了工具绑定（自动注入）、I/O 约束（输入校验+输出格式）、可组合性（链式调用）、可评估性（evals 数据），一个 Prompt 被复制超过 3 次就值得封装成 Skill。
