---
tags:
  - AI
  - MCP
category: AI/MCP
---

# MCP (Model Context Protocol) 介绍

## Q1: 什么是 MCP?

**MCP (Model Context Protocol)** 是 Anthropic 推出的开放协议，用于连接 AI 模型与外部工具和数据源。

> 类比：MCP 是 AI 的 "USB-C" —— 统一接口标准，让任何 AI 模型都能通过同一协议访问各种工具和数据。

| 特性 | 说明 |
|------|------|
| 提出者 | Anthropic (2024.11 开源) |
| 定位 | AI 模型与外部世界的标准接口协议 |
| 协议基础 | JSON-RPC 2.0 |
| 开源状态 | 完全开源，社区驱动 |
| 核心理念 | 解耦模型与工具，一次接入，处处可用 |

---

## Q2: MCP 在 AI 系统中的角色是什么?

MCP 是 LLM 与外部世界之间的**标准接口层**：

```
┌─────────────────────────────────────────────────┐
│                   Host (宿主)                     │
│         Claude Desktop / IDE / AI 应用            │
│                                                   │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐   │
│  │MCP Client│    │MCP Client│    │MCP Client│   │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘   │
└───────┼───────────────┼───────────────┼─────────┘
        │               │               │
        ▼               ▼               ▼
  ┌──────────┐   ┌──────────┐   ┌──────────┐
  │MCP Server│   │MCP Server│   │MCP Server│
  │ (文件系统) │   │ (数据库)  │   │ (Git)    │
  └──────────┘   └──────────┘   └──────────┘
```

- **Host**：运行 AI 模型的应用程序（如 Claude Desktop、VS Code）
- **MCP Client**：Host 内部的协议客户端，负责与 Server 建立连接
- **MCP Server**：提供具体工具/资源/提示的轻量级服务程序

---

## Q3: MCP 底层协议是什么?

MCP 基于 **JSON-RPC 2.0** 协议，支持两种传输方式：

### 传输方式对比

| 传输方式 | stdio (标准输入输出) | HTTP+SSE |
|----------|---------------------|----------|
| 适用场景 | 本地工具、单机部署 | 远程服务、共享服务器 |
| 连接方式 | Host 启动 Server 进程，通过 stdin/stdout 通信 | Client 连接远程 URL |
| 请求发送 | 写入 Server 的 stdin | HTTP POST 请求 |
| 响应接收 | 从 Server 的 stdout 读取 | SSE (Server-Sent Events) 流 |
| 安全性 | 本地进程隔离，较安全 | 需要网络鉴权 (OAuth/API Key) |
| 典型用途 | 文件系统、本地数据库、Git 操作 | 云端 API、共享工具服务 |

### stdio 模式示例

```json
// Host → Server (通过 stdin 写入)
{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}

// Server → Host (通过 stdout 返回)
{"jsonrpc": "2.0", "id": 1, "result": {"tools": [{"name": "read_file", ...}]}}
```

### HTTP+SSE 模式示例

```
Client                              Server
  │                                   │
  │─── HTTP POST /mcp (请求) ────────▶│
  │                                   │
  │◀─── SSE stream (响应流) ─────────│
```

---

## Q4: MCP 的三大核心概念是什么?

| 概念 | 说明 | 类比 | 示例 |
|------|------|------|------|
| **Tools** | 模型可调用的函数 | API 接口 | `read_file`, `query_db`, `git_commit` |
| **Resources** | 模型可读取的数据 | 数据源/文件 | 文件内容、数据库记录、日志 |
| **Prompts** | 可复用的提示模板 | 模板引擎 | `code_review_prompt`, `summarize_prompt` |

### Tool 示例

```json
{
  "name": "query_database",
  "description": "Execute a SQL query against the database",
  "inputSchema": {
    "type": "object",
    "properties": {
      "sql": {"type": "string", "description": "SQL query to execute"}
    },
    "required": ["sql"]
  }
}
```

### Resource 示例

```json
{
  "uri": "file:///project/README.md",
  "name": "Project README",
  "mimeType": "text/markdown"
}
```

### Prompt 示例

```json
{
  "name": "code_review",
  "description": "Review code for best practices",
  "arguments": [
    {"name": "code", "required": true, "description": "Code to review"}
  ]
}
```

---

## Q5: MCP Tool 与传统 Function Calling 有什么区别?

这是面试高频问题！

| 对比维度 | Function Calling | MCP Tool |
|----------|-----------------|----------|
| **定义方式** | 模型特定 (每个模型格式不同) | 标准协议 (JSON-RPC 2.0) |
| **执行位置** | Host 应用执行 | MCP Server 执行 |
| **耦合度** | 紧耦合 (模型+应用绑定) | 松耦合 (协议标准化) |
| **可组合性** | 工具间难以组合 | 天然支持组合 (多个 Server) |
| **可移植性** | 换模型需重写工具定义 | 同一 Server 适配所有模型 |
| **生态** | 各厂商独立生态 | 统一开放生态 |

### 关键理解

```
Function Calling = 模型能力 (模型输出函数名+参数)
MCP              = 协议标准 (定义模型与工具如何通信)
```

- **MCP 不是 Function Calling 的替代品**，而是 Function Calling 的标准化接口层
- Function Calling 解决的是"模型如何表达意图"
- MCP 解决的是"意图如何传递到工具并执行"

### 流程对比

```
【传统 Function Calling】
Model → 输出 function_name + args → Host 应用解析 → Host 应用执行 → 结果返回 Model

【MCP Tool】
Model → 输出 tool_name + args → MCP Client → MCP Server 执行 → 结果原路返回 Model
                                    ↑
                              标准协议层
```

---

## Q6: MCP 的架构是怎样的?

### 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                        Host 应用                              │
│                  (Claude Desktop / IDE)                       │
│                                                              │
│    ┌─────────┐   ┌─────────┐   ┌─────────┐                 │
│    │ LLM     │   │MCP      │   │MCP      │                 │
│    │ (Claude)│   │Client 1 │   │Client 2 │                 │
│    └────┬────┘   └────┬────┘   └────┬────┘                 │
│         │             │             │                        │
│         └──────┬──────┘             │                        │
│                │                    │                        │
└────────────────┼────────────────────┼────────────────────────┘
                 │  JSON-RPC 2.0     │
                 ▼                    ▼
          ┌─────────────┐    ┌─────────────┐
          │ MCP Server 1│    │ MCP Server 2│
          │ ┌─────────┐ │    │ ┌─────────┐ │
          │ │ Tools   │ │    │ │ Tools   │ │
          │ │Resources│ │    │ │Resources│ │
          │ │Prompts  │ │    │ │Prompts  │ │
          │ └─────────┘ │    │ └─────────┘ │
          └─────────────┘    └─────────────┘
```

### 通信生命周期

```
1. 初始化 (Initialize)
   Client → Server: initialize 请求
   Server → Client: 返回能力声明 (capabilities)

2. 正常通信
   - tools/list:    发现可用工具
   - tools/call:    调用工具
   - resources/list: 发现可用资源
   - resources/read: 读取资源
   - prompts/list:  发现可用提示
   - prompts/get:   获取提示内容

3. 关闭 (Shutdown)
   任一方发送关闭信号，清理连接
```

---

## Q7: MCP 的实际应用场景有哪些?

| 场景 | MCP Server | 提供的能力 |
|------|-----------|-----------|
| 代码开发 | filesystem, git | 读写文件、Git 操作 |
| 数据库操作 | postgres, sqlite | 执行 SQL、查询表结构 |
| Web 交互 | fetch, puppeteer | HTTP 请求、浏览器自动化 |
| 知识检索 | brave-search, memory | 搜索引擎、知识图谱 |
| 云服务 | aws, gcp | 云>云资源管理 |

---

## 一句话总结

**MCP 是 AI 世界的 USB-C 标准——用 JSON-RPC 2.0 协议统一了模型与工具之间的通信接口，让任何模型都能通过同一协议调用任何工具，实现松耦合、可组合、可移植的 AI 工具生态。**
