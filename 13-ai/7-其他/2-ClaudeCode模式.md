---
tags:
  - AI
  - LLM
category: AI/其他
---

# Claude Code 模式

## 什么是 Claude Code？

Claude Code 是 Anthropic 官方推出的 **CLI（命令行）编程工具**，基于 Claude 大模型，能够在终端中直接完成代码搜索、编辑、执行、Git 操作等开发任务。它不是一个 IDE 插件，而是一个独立的命令行 Agent。

核心定位：**Agentic Coding** —— 不仅能生成代码片段，还能自主理解项目上下文、规划任务、多步执行、并行修改多个文件。

## 核心模式

### Agent 模式（自主执行）

Claude Code 的默认模式，Agent 会自主规划和执行多步任务：

```
用户：把这个项目的日志框架从 Log4j 迁移到 SLF4J

Agent 自主执行：
1. 搜索所有使用 Log4j 的文件
2. 分析 import 语句和 API 调用
3. 逐文件替换 import 和 API
4. 更新构建配置（pom.xml / build.gradle）
5. 运行编译验证
6. 提交修改摘要
```

特点：
- 自主规划任务步骤，无需逐步指挥
- 可并行处理多个文件
- 遇到错误会自动分析并调整策略
- 执行前会展示计划，用户可确认或修改

### 交互模式（逐步确认）

用户可以逐步确认每一步操作：

- 每次文件修改、命令执行前都会征求用户确认
- 用户可以拒绝、修改或补充 Agent 的提议
- 适合对精确性要求高的场景（如生产环境修改）

## 关键能力

| 能力 | 说明 |
|------|------|
| 代码搜索 | 理解语义地搜索代码，不限于字符串匹配 |
| 代码编辑 | 精准的行级编辑，支持多文件并行修改 |
| 命令执行 | 在沙箱中执行 shell 命令，运行测试、构建等 |
| Git 集成 | 原生支持 commit、diff、PR 创建等 Git 操作 |
| MCP 工具集成 | 通过 Model Context Protocol 接入外部工具（数据库、API 等） |
| 多文件并行 | 同时修改多个文件，保持一致性 |
| 上下文理解 | 自动读取项目结构、CLAUDE.md、相关文件 |

## 与 Cursor / Copilot 的对比

| 对比维度 | Claude Code | Cursor | GitHub Copilot |
|---------|-------------|--------|----------------|
| 形态 | CLI 命令行 | IDE（基于 VS Code） | IDE 插件 |
| 交互方式 | 自然语言对话 | 自然语言 + 编辑器内联 | 代码补全 + Chat |
| 自主性 | 高（Agent 自主执行多步任务） | 中（需逐步确认） | 低（逐行补全） |
| 多文件修改 | 支持（并行修改） | 支持 | 有限 |
| 项目理解 | 深度（CLAUDE.md + 自动索引） | 中等（Codebase Indexing） | 弱（基于当前文件） |
| 外部工具 | MCP 协议扩展 | 插件系统 | 有限 |
| 适用场景 | 复杂重构、批量修改 | 日常编码 | 代码补全 |
| 运行环境 | 终端 | 桌面 GUI | IDE 内 |
| 价格 | Claude 订阅 | 订阅制 | 订阅制 |

> 简单来说：Copilot 是"打字加速器"，Cursor 是"智能编辑器"，Claude Code 是"编程 Agent"。

## 使用场景

| 场景 | 说明 |
|------|------|
| 代码重构 | 大规模重命名、API 迁移、模式替换 |
| Bug 修复 | 自动定位问题、分析调用链、生成修复 |
| 项目搭建 | 从零生成项目结构、配置文件、基础代码 |
| 代码审查 | 分析变更影响、检查潜在问题 |
| 文档生成 | 根据代码生成注释和文档 |
| 测试编写 | 为现有代码生成单元测试 |

## 最佳实践

### CLAUDE.md 配置

在项目根目录放置 `CLAUDE.md` 文件，为 Claude Code 提供项目上下文：

```markdown
# CLAUDE.md

## 项目概述
- Java 17 + Spring Boot 3.x 项目
- 使用 Gradle 构建

## 代码规范
- 使用 SLF4J 而非 Log4j
- 所有公共方法必须有 Javadoc
- 测试覆盖率不低于 80%

## 目录结构
- src/main/java/...  业务代码
- src/test/java/...   测试代码
```

CLAUDE.md 的层级：
- `~/.claude/CLAUDE.md` —— 全局配置（所有项目共享）
- `项目根/CLAUDE.md` —— 项目级配置
- `项目根/子目录/CLAUDE.md` —— 目录级配置

### 权限管理

Claude Code 执行操作需要用户授权，权限粒度可控：

```json
// .claude/settings.json
{
  "permissions": {
    "allow": [
      "Bash(git *)",
      "Bash(gradle *)",
      "Read(*)"
    ],
    "deny": [
      "Bash(rm -rf *)"
    ]
  }
}
```

- **allow**：白名单，自动放行无需确认
- **deny**：黑名单，始终拒绝
- 未配置的操作：每次询问用户

### Hooks 机制

Hooks 允许在 Claude Code 执行操作前后运行自定义脚本：

```json
// .claude/settings.json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "command": "echo '即将执行命令'"
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit",
        "command": "./scripts/lint-changed-files.sh"
      }
    ]
  }
}
```

| Hook 类型 | 触发时机 | 用途 |
|-----------|---------|------|
| PreToolUse | 工具执行前 | 拦截危险操作、添加前置检查 |
| PostToolUse | 工具执行后 | 自动 lint、格式化、验证 |
| Notification | 通知事件 | 集成告警系统 |

## 一句话总结

Claude Code 是 Anthropic 官方的 CLI 编程 Agent，以自主执行多步任务为核心模式，通过 CLAUDE.md 配置项目上下文、MCP 集成外部工具、Hooks 实现流程自动化，适合代码重构、Bug 修复、项目搭建等复杂开发场景。
