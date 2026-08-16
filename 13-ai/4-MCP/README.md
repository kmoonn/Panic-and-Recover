# MCP 模型上下文协议

**MCP (Model Context Protocol)** 是 Anthropic 2024.11 开源的协议，用于连接 AI 模型与外部工具和数据源。

> 类比：MCP 是 AI 的 "USB-C" —— 统一接口标准，让任何模型都能通过同一协议访问任何工具。

| 特性 | 说明 |
|------|------|
| 提出者 | Anthropic (2024.11 开源) |
| 定位 | AI 模型与外部世界的标准接口协议 |
| 协议基础 | JSON-RPC 2.0 |
| 传输方式 | stdio（本地）/ Streamable HTTP（远程） |
| 开源状态 | 完全开源，已被 Anthropic、OpenAI、Google、Microsoft 采纳 |
| 核心理念 | 解耦模型与工具，一次接入，处处可用 |

## 笔记

- [三层架构](三层架构.md) — Host/Client/Server
- [核心概念](核心概念.md) — Tools / Resources / Prompts
- [通信生命周期](通信生命周期.md) — 初始化→正常通信→关闭
- [传输方式](传输方式.md) — stdio vs Streamable HTTP 对比选型
- [StreamableHTTP](StreamableHTTP.md) — 传输层演进与版本变迁
- [Sampling](Sampling.md) — Server 反向借用 Client 侧 LLM
- [MCP与FunctionCalling的区别](MCP与FunctionCalling的区别.md) — 协议标准 vs 模型能力
- [MCP与Skill的区别](MCP与Skill的区别.md) — 给全世界 vs 给自己人
