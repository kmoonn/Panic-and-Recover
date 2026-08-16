---
category: AI/MCP
tags:
  - AI
  - MCP
  - Function-Calling
  - 面试
---

# MCP与FunctionCalling的区别

> 一句话总结：Function Calling 是模型能力（输出函数名+参数），MCP 是协议标准（定义模型与工具如何通信），MCP 是 Function Calling 的标准化接口层，不是替代品。

---

## 核心区别

| 对比维度 | Function Calling | MCP Tool |
|----------|-----------------|----------|
| **本质** | 模型能力（输出意图） | 协议标准（传递+执行意图） |
| **定义方式** | 模型特定，每个模型格式不同 | 标准协议 (JSON-RPC 2.0) |
| **执行位置** | Host 应用执行 | MCP Server 执行 |
| **耦合度** | 紧耦合（模型+应用绑定） | 松耦合（协议标准化） |
| **可组合性** | 工具间难以组合 | 天然支持组合（多个 Server） |
| **可移植性** | 换模型需重写工具定义 | 同一 Server 适配所有模型 |
| **生态** | 各厂商独立生态 | 统一开放生态 |

---

## 流程对比

```
【传统 Function Calling】
Model → 输出 function_name + args → Host 应用解析 → Host 应用执行 → 结果返回 Model
    ↑ 模型能力                         ↑ 紧耦合，Host 手写执行逻辑

【MCP Tool】
Model → 输出 tool_name + args → MCP Client → MCP Server 执行 → 结果原路返回 Model
    ↑ 模型能力                     ↑ 标准协议层      ↑ 松耦合，Server 独立部署
```

---

## 关键理解

```
Function Calling = 模型能力（"我想调用什么"）
MCP              = 协议标准（"怎么传到工具并执行"）
```

- **MCP 不是 Function Calling 的替代品**，而是其标准化接口层
- Function Calling 解决"模型如何表达意图"
- MCP 解决"意图如何传递到工具并执行"
- 两者协作：模型用 Function Calling 表达意图 → MCP 协议传递到 Server 执行

---

## 与简历项目的关联

| 概念 | 项目关联 |
|------|---------|
| Function Calling | 效能星盘用 Function Calling + Pydantic 约束 LLM 输出稳定 JSON，模型表达"我想调哪个分析模块" |
| MCP Tool 执行 | API Testing Agent 中意图表达后由对应 Server 执行（Mock/日志/造数），Host 不手写执行逻辑 |
| 松耦合 | Skills Pulse 适配 7+ 种 Coding Agent，不同 Agent 的 Function Calling 格式不同，但 MCP 协议层统一 |

---

## 面试高频问题

**Q：MCP 和 Function Calling 有什么区别？**
A：Function Calling 是模型能力（模型输出函数名+参数），MCP 是协议标准（定义模型与工具如何通信）。MCP 不是 Function Calling 的替代品，而是其标准化接口层——同一 MCP Server 可适配所有模型，换模型无需重写工具定义。

**Q：为什么有了 Function Calling 还需要 MCP？**
A：Function Calling 只解决"模型如何表达意图"，但意图的传递、路由、执行、鉴权没有标准。MCP 统一了这些：同一 Server 适配所有模型，多 Server 可组合，部署和鉴权有标准方案。Function Calling 是模型侧能力，MCP 是工程侧标准。

---

## 一句话总结

Function Calling 是模型能力（输出意图），MCP 是协议标准（传递+执行意图），MCP 是 Function Calling 的标准化接口层，不是替代品。
