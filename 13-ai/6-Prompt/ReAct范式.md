---
category: AI/Prompt
tags:
  - AI
  - LLM
  - Prompt
  - ReAct
  - Agent
  - 面试
---

# ReAct范式

> 一句话总结：ReAct = Reasoning + Acting，推理与行动交替进行，模型可调用外部工具获取真实信息，是 Agent 系统的基础范式。

---

## 是什么？

将**推理**与**行动**结合——模型在推理过程中可以调用外部工具获取真实信息。

```
问：2024年奥斯卡最佳影片的导演是谁？

思考1：我需要先查 2024 年奥斯卡最佳影片是哪部
行动1：search("2024年奥斯卡最佳影片")
观察1：Oppenheimer（奥本海默）

思考2：现在查奥本海默的导演
行动2：search("Oppenheimer导演")
观察2：Christopher Nolan

思考3：我有了答案
回答：2024年奥斯卡最佳影片《奥本海默》的导演是 Christopher Nolan
```

---

## 解决了 CoT 什么问题？

CoT 只能基于模型内部知识推理，遇到需要最新信息或精确数据时容易幻觉。ReAct 通过工具调用获取**真实世界信息**，推理建立在事实之上。

| 维度 | 纯 CoT | ReAct |
|------|--------|-------|
| 信息来源 | 仅模型内部知识 | 内部知识 + 外部工具 |
| 时效性 | 无法获取最新信息 | 可搜索、可查 API |
| 准确性 | 事实可能错误（幻觉） | 工具返回真实数据 |
| 成本 | 仅推理 token | 推理 + 工具调用 |
| 适用 | 纯逻辑推理 | 需要外部信息的任务 |

---

## ReAct 是 Agent 的基础范式

所有 Agent 框架的核心循环都是 ReAct 的变体：

| 框架 | ReAct 变体 |
|------|-----------|
| LangChain | Tool → Observe → Reason 循环 |
| AutoGPT | Goal → Action → Observation → Reflection |
| Claude Code | Think → Tool → Result → Continue |

---

## 与简历项目的关联

| 技术点 | 项目关联 |
|--------|---------|
| ReAct 循环 | API Testing Agent 的核心循环：推理选择数据生成方式（Mock/日志/造数 Skill）→调用工具→观察结果→继续 |
| Acting | 效能星盘用 Function Calling 让 LLM 调用分析模块，策略注册表 + Orchestrator 提升扩展性 |
| 工具调用 | 字节 AI Testing 工具链三项目闭环：造数→遥测→测试，本质是 ReAct 中多工具协作 |

---

## 面试高频问题

**Q：ReAct 相比纯 CoT 解决了什么核心问题？**
A：CoT 只能基于模型内部知识推理，遇到需要最新信息或精确数据时容易幻觉。ReAct 通过工具调用获取真实世界信息，让推理建立在事实之上。这也是 Agent 系统的基础范式。

---

## 一句话总结

ReAct = Reasoning + Acting，推理与工具调用交替，解决 CoT 无法获取外部信息的缺陷，是所有 Agent 框架的基础范式。
