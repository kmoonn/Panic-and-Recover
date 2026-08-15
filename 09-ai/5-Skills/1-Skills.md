---
tags:
  - AI
  - Skills
category: AI/Skills
---

# Skills 介绍

## Q1: 什么是 Skills?

**Skills** 是可复用、可组合的 **提示+工具** 打包单元，赋予 AI 特定的能力。

> 类比：如果 MCP 是 "USB-C 接口"，那 Skills 就是 "USB-C 设备" —— 每个 Skill 是一个即插即用的能力模块。

| 特性 | 说明 |
|------|------|
| 本质 | prompt 模板 + 工具绑定 + 输入/输出 schema |
| 目标 | 让 AI 获得某个特定领域的完整能力 |
| 特点 | 结构化、可复用、可组合、可验证 |
| 类比 | 类似微服务中的 "能力包"，每个 Skill 封装一个完整功能 |

---

## Q2: Skills 的工作原理是什么?

### Skill 的组成结构

```
Skill = Prompt Template + Tool Bindings + I/O Schema
```

| 组件 | 说明 | 示例 |
|------|------|------|
| **Prompt Template** | 指导 AI 如何执行任务的提示模板 | "Review the following code for security issues: {{code}}" |
| **Tool Bindings** | 该 Skill 需要使用的工具列表 | `read_file`, `run_test`, `git_diff` |
| **Input Schema** | 输入参数的定义与校验 | `{code_path: string, severity: enum[low,medium,high]}` |
| **Output Schema** | 输出结果的格式定义 | `{issues: Issue[], summary: string}` |

### 执行流程

```
用户调用 Skill
    │
    ▼
┌─────────────────────────┐
│ 1. 校验输入 (Input Schema) │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ 2. 渲染 Prompt Template   │
│    (将输入填入模板变量)    │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ 3. 绑定 Tools             │
│    (加载 Skill 所需工具)  │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ 4. AI 执行                │
│    (带着 prompt + tools   │
│     调用 LLM)             │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ 5. 校验输出 (Output Schema)│
└────────────┬────────────┘
             │
             ▼
        返回结果
```

---

## Q3: Skills 与普通 Prompts 有什么区别?

| 对比维度 | 普通 Prompt | Skill |
|----------|------------|-------|
| **结构化** | 自由文本，无固定格式 | 结构化定义，有 schema 约束 |
| **可复用性** | 复制粘贴，难以管理 | 模板化，参数化，一次定义多次使用 |
| **工具绑定** | 无，需手动指定 | 自动绑定所需工具 |
| **可组合性** | 无法链式调用 | 支持链式组合 (Skill A → Skill B) |
| **输入校验** | 无 | Input Schema 自动校验 |
| **输出校验** | 无 | Output Schema 自动校验 |
| **可观测性** | 难以追踪 | 可追踪执行过程与结果 |
| **版本管理** | 无 | 可版本化，可迭代 |

### 代码示例对比

```yaml
# 普通 Prompt (非结构化)
prompt: |
  Review this code for bugs and security issues.
  Make sure to check for SQL injection, XSS, and common errors.
  The code is in file: /src/auth/login.py

# Skill (结构化)
name: code-review
description: Comprehensive code review with security and quality checks
input:
  type: object
  properties:
    file_path:
      type: string
      description: Path to the file to review
    check_types:
      type: array
      items:
        type: enum
        values: [security, performance, style, bugs]
      default: [security, bugs]
tools:
  - read_file
  - git_diff
  - run_linter
prompt_template: |
  Review the code at {{file_path}} for the following aspects:
  {{#each check_types}}- {{this}}{{/each}}

  Use the available tools to:
  1. Read the file content
  2. Check recent changes (git diff)
  3. Run linter if available
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
          line: {type: integer}
    summary:
      type: string
```

---

## Q4: Skills 的使用示例有哪些?

### 示例 1: Code Review Skill

```
Skill: code-review
├── Prompt: "审查代码的安全性和规范性..."
├── Tools:  read_file, git_diff, run_linter, search_code
├── Input:  {file_path: string, check_types: array}
└── Output: {issues: Issue[], summary: string}
```

### 示例 2: Deploy Skill

```
Skill: deploy
├── Prompt: "执行部署流程，检查前置条件..."
├── Tools:  kubectl_apply, docker_build, run_tests, check_health
├── Input:  {environment: enum[dev,staging,prod], version: string}
└── Output: {deploy_status: string, rollout_url: string}
```

### 示例 3: Data Analysis Skill

```
Skill: data-analysis
├── Prompt: "分析数据集，生成洞察和可视化建议..."
├── Tools:  query_database, read_csv, generate_chart, statistical_test
├── Input:  {data_source: string, analysis_type: enum[descriptive,inferential,predictive]}
└── Output: {insights: Insight[], charts: Chart[], summary: string}
```

### Skills 链式组合

```
用户: "Review and deploy the auth service"

Step 1: code-review skill
  Input:  {file_path: "/services/auth", check_types: [security, bugs]}
  Output: {issues: [...], summary: "2 warnings found"}

Step 2: deploy skill (依赖 Step 1 的结果)
  Input:  {environment: "staging", version: "v2.1.0"}
  Output: {deploy_status: "success", rollout_url: "https://..."}

  (如果有 critical issues，deploy skill 可选择拒绝部署)
```

---

## Q5: Skills 与 MCP 的关系是什么?

| 维度 | MCP | Skills |
|------|-----|--------|
| 定位 | 协议层 (怎么通信) | 能力层 (做什么事) |
| 粒度 | 单个工具/资源 | 完整能力包 (prompt + tools) |
| 类比 | USB-C 接口 | USB-C 设备 |
| 关系 | Skills 使用 MCP Tools 作为构建块 | MCP 是 Skills 的基础设施 |

### 关系图

```
┌──────────────────────────────────────┐
│             Skill Layer               │
│  ┌──────────┐  ┌──────────┐         │
│  │code-review│  │  deploy  │         │
│  │  Skill    │  │  Skill   │         │
│  └─────┬────┘  └────┬─────┘         │
│        │             │               │
│        │  使用 MCP   │               │
│        │  Tools 作为  │               │
│        │  构建块     │               │
│        ▼             ▼               │
├──────────────────────────────────────┤
│           MCP Protocol Layer          │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐      │
│  │read│ │git │ │lint│ │k8s │      │
│  │file│ │diff│ │    │ │ctl │      │
│  └────┘ └────┘ └────┘ └────┘      │
└──────────────────────────────────────┘
```

**MCP 提供原子级工具 (read_file, git_commit)，Skills 将这些工具组合成更高层的能力 (code-review, deploy)。**

---

## 一句话总结

**Skills 是结构化的 "提示+工具" 能力包——通过 Prompt Template + Tool Bindings + I/O Schema 将零散的 MCP 工具组装成可复用、可组合、可校验的 AI 能力模块，是 MCP 协议之上的能力编排层。**
