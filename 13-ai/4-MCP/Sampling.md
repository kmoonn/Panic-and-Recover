---
category: AI/MCP
tags:
  - AI
  - MCP
  - Sampling
  - 面试
---

# Sampling

> 一句话总结：Sampling 让 MCP Server 反向借用 Client 侧 LLM 完成推理，Server 无需持有 API Key，但 2026 规范已 Deprecated。

---

## 是什么？

MCP Server 可以向 Client 发起 `sampling/createMessage` 请求，**借用 Client 侧的 LLM 完成推理**，Server 自身不需要持有 API Key。

```
正常流程：User → Client → Server（调用工具）
                        ← 返回结果

Sampling：User → Client → Server（调用工具）
                            ↓ 需要推理
                          Server → Client（"帮我运行这个 Prompt"）
                                    ↓
                                  LLM 推理
                                    ↓
                          Server ← Client（推理结果）
                        ← 返回最终结果
```

---

## 适用场景

| 场景 | 说明 |
|------|------|
| 公开 MCP Server | 避免 Server 承担 LLM 调用成本，借用 Client 侧模型 |
| 工具内需小范围推理 | 如 CSV 分析时让 LLM 总结趋势、文档摘要 |
| 利用 Client 专属模型 | 借用 Client 配置的特定模型能力或上下文 |

---

## 安全边界

Client 对 Sampling 有完全控制权：
- 可**拒绝**任何 Sampling 请求
- 可**限制**最大 token 数和模型选择
- 可**审查** Server 发来的 Prompt 内容

---

## 为什么 2026 规范 Deprecated？

增加了协议复杂度和安全边界问题。Server 直接集成 LLM Provider API 更简单可控，无需通过 Client 中转。保持至少 12 个月兼容期。

---

## 与简历项目的关联

| 知识点 | 项目关联 |
|--------|---------|
| Server 借用 Client LLM | API Testing Agent 中 MCP Server（造数/日志/Mock）不需要自己调 LLM，推理由 Client 侧模型完成 |
| 安全边界审查 | Skills Pulse 的 fallback 用户身份解析+匿名化哈希，本质是对外部调用的安全边界控制 |

---

## 面试高频问题

**Q：Sampling 是什么？为什么 Deprecated？**
A：Sampling 让 MCP Server 反向借用 Client 侧 LLM 完成推理，Server 无需 API Key。Deprecated 因为增加协议复杂度和安全边界问题，Server 直接调 LLM Provider API 更简单。保持 12 个月兼容期。

---

## 一句话总结

Sampling 让 MCP Server 反向借用 Client 侧 LLM，解决公开 Server 不持 API Key 的问题，但因复杂度和安全考量已于 2026 规范 Deprecated。
