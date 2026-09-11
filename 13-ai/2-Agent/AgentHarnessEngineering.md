---
tags:
  - 测试
  - AI
  - Agent
category: 测试/基础
---

# Agent Harness Engineering

## 什么是 Agent Harness

> **Agent = Model + Harness**

Harness 是 Agent 中**除模型之外的一切**——循环编排、工具调用、上下文管理、记忆持久化、沙箱隔离、权限控制、可观测性、恢复策略。它不是一层包装，而是多层结构。

核心论点：**Harness 的影响往往超过模型本身**——换 Harness 的收益可能比换模型更大。

| 证据 | 结果 |
|------|------|
| Vercel 去掉 80% 工具 | 成功率 80%→100%，延迟 724s→141s，同一模型 |
| LangChain 换 Harness | Terminal-Bench 52.8%→66.5%，同一模型 |
| Princeton CORE-Bench | 同一模型 42% vs 78%，不同 scaffold |
| Harvey 法律 Agent | 准确率翻倍，纯靠 Harness 优化 |
| Terminal-Bench 2.0 | Letta Code 59.1% vs Claude Code 41.6%，同一模型（Claude Opus 4.5） |

## 三层架构

```
┌─────────────────────────────────────┐
│  Assurance 层                       │
│  子 Agent 编排 · 验证循环 · 评估     │  ← Eval Harness 住这里
├─────────────────────────────────────┤
│  Capabilities 层                    │
│  工具 · 记忆 · 状态 · 上下文管理     │  ← Agent 能做什么
├─────────────────────────────────────┤
│  Runtime 层                         │
│  主循环 · Prompt 构造 · 输出解析     │  ← 让模型跑起来
│  · 错误处理                         │
└─────────────────────────────────────┘
```

## 四层栈

| 层 | 作用 | 例子 |
|---|---|---|
| **Model** | 推理 | GPT-5, Claude Opus |
| **Harness** | 跑一个 Agent（循环+工具+沙箱+记忆+上下文规则） | Claude Code, Codex |
| **Framework** | 编排多个 Agent（显式状态+分支） | LangGraph, CrewAI |
| **Platform** | 跑很多 Harness（持久执行+成本归属+治理） | Braintrust, LangFuse |

> 如果路径能画出来 → 用 Framework（规则引擎更便宜）；画不出来 → 用 Harness（Agent 自主探索）。

## 2026 主流 Agent Harness 对比

| Harness | 厂商/协议 | 栈 | 最适合 | 主要短板 |
|---------|----------|---|--------|---------|
| **Claude Code** | Anthropic, 闭源 | TS, 仅 Anthropic 模型 | Anthropic 模型；对抗对的半边 | 单厂商；持久记忆薄 |
| **Codex** | OpenAI, Apache-2.0 | Rust+TS, 仅 OpenAI 模型 | OpenAI 模型；对抗对的另半边 | 单厂商 |
| **OpenCode** | Anomaly, MIT | TS, 75+ provider 含本地 | 开源模型/自托管 | 曾默认调 Grok 免费 API 做会话命名 |
| **Goose** | Linux Foundation, Apache-2.0 | Rust, 15+ provider | 治理中立的跨厂商场景 | 基金会节奏慢于厂商 |
| **OpenHands** | All-Hands-AI, MIT | 自托管, Docker 沙箱 | 自托管沙箱；驱动其他 Agent | Docker-in-Docker 别扭；loop on 歧义 |
| **Pydantic AI Harness** | Pydantic, 开源 | Python, 嵌入已有服务 | 给 Python 服务加 Agent 行为 | 0.x 版本；库非应用 |

> Addy Osmani 观察：Harness 之间**长得比底层模型还像彼此**——比特性会过期，比假设才持久。

## Claude Code 的 Harness 组件

Claude Code 是理解 Harness 的绝佳实例——由命名扩展点构成，不同时机加载：

| 组件 | 作用 | 加载时机 |
|------|------|---------|
| **CLAUDE.md** | 项目约定和坑点，自动读入 | 每次会话 |
| **Hooks** | 确定性规则脚本（lint/format/test） | 事件触发 |
| **Skills** | 特定任务类型的打包指令 | 按需加载 |
| **Plugins** | Skill + Hook + MCP 打包 | 安装后常驻 |
| **MCP servers** | 外部工具/数据连接 | 配置后常驻 |

### 会话生命周期

1. 会话启动 → CLAUDE.md 加载
2. Start Hook 触发 → 注入团队/模块上下文
3. 用户 Prompt → 匹配 Skill 按需加载
4. Agent 工作 → LSP + MCP 提供符号级精度和外部数据
5. 操作执行 → Hook 确定性检查（lint/format/test）
6. 会话结束 → Stop Hook 反思 → 提议 CLAUDE.md 更新（自进化）

### 关键缺口：没有原生 Eval

Claude Code 的 Harness 解决的是"把正确上下文送进模型并塑造行为"，但**没有评估机制**——对确定性代码（测试红绿）够用，对非确定性 Agent 输出不够。这正是 Eval Harness 填补的缺口。

## Eval Harness（评估 Harness）

### 与 Guardrail 的区别

| | Eval | Guardrail |
|---|---|---|
| **时机** | 离线/开发时 | 在线/运行时 |
| **数据** | 固定黄金数据集 | 实际请求/响应 |
| **动作** | 度量、不干预 | 可拦截/重试/降级 |
| **归属层** | Assurance 层（验证循环） | Runtime 层（错误处理） |

### DeepEval 填补 Claude Code 的 Eval 缺口

| CC 组件 | DeepEval 放什么 | 类别 |
|--------|----------------|------|
| **Skill** | 模板、50+ 指标目录、迭代循环护栏 | Eval — 离线，Agent 驱动 |
| **Hook** | `deepeval test run` 接入 stop/pre-commit hook | Eval — 离线，强制 |
| **Plugin** | Skill + eval hook + dataset 打包，组织级安装 | Eval — 分发 |

## 六层测试栈

Agent Harness 的测试与标准软件完全不同——概率性输出、工具链式失败、数据漂移。RAND 报告 Agent 生产故障率 80-90%。

| 层 | 名称 | 做什么 | 关键指标 | 主要工具 |
|---|------|--------|---------|---------|
| **0** | 数据认证门 | 认证每个数据源的质量和新鲜度 | Precision@5≥0.7, null率<0.15 | — |
| **1** | 单元测试 | 每个工具调用类型独立测 | 工具选择准确率、参数正确性 | DeepEval, Promptfoo |
| **2** | 集成测试 | 多步链路+中间状态验证 | 步骤效率、上下文召回 | Braintrust, LangFuse |
| **3** | E2E 模拟 | 完整 Agent 循环+故障注入 | pass@k, pass^k, 任务成功率 | 沙箱环境 |
| **4** | 对抗/红队 | 越狱+PII 泄露+安全违规 | 覆盖 5 类安全场景×10 用例 | Maxim AI |
| **5** | 生产 CI/CD | PR 门禁+软失败阈值+持续监控 | 软失败率<33%, 成本/任务 | LangFuse, Arize |

> **Layer 0 是最常跳过的层，也是 eval 不稳定的最常见根因。**

### 关键概念

- **pass@k**：k 次尝试中至少成功一次的概率 → 揭示**上限**
- **pass^k**：k 次尝试全部成功的概率 → 揭示**下限**
- pass@5=0.9 但 pass^5=0.4 → **demo-ready 但不是 production-ready**
- **0.5-0.8 软失败带**：比二元阈值更可靠；≥33% 落入此带 → 停下来查
- **对抗测试是永久回归测试**：每个发现的越狱向量 → 锁定用例
- **元评估**：每季度用人工标注集校准 LLM-as-judge（~10% 假阴性率）

## Harness-Bench：专门度量 Harness 效应的基准

- 106 个沙箱化离线 Agent 任务，8 个工作流类别
- **核心发现**：可配置 Harness 中，最高分 76.2%（NanoBot），最低 52.4%（OpenClaw），**23.8 分差距**——同一任务集、同一模型池
- **强模型对 Harness 不敏感，弱模型对 Harness 高度敏感**
- 失败分布：合约违反 36.4% → 工具/恢复 24.6% → 证据/接地 14.6% → 产出提交 11.1% → 状态/续行 9.3%
- **结论**：Agent 能力应报告在 **模型-Harness 配置级别**，而非仅归因于基座模型

## Harness 工作是否会被更好的模型淘汰？

| 类型 | 会被淘汰？ | 举例 |
|------|----------|------|
| 会被淘汰 | ✅ | 上下文压缩、重试逻辑、输出裁剪 |
| 不会被淘汰 | ❌ | 沙箱、权限、限额、审计轨迹 → 编码的是**组织允许什么**，不是**模型能做什么** |

> **Osmani 棘轮**：每次 Agent 犯错，工程化方案使它永不再犯——每条规则可追溯到真实故障，绝不预设。这比预测式 Harness 更能跨模型升级存活。

## 实际经验（结合自己的项目）

### ATE 平台：复用 + 编排 Harness

- **不是自己造 Harness**——复用团队 ATE 平台做执行引擎
- 自己补的是 **AI 编排层**：Agent 调用 ATE 的配套 Skill 写用例、组参数、发请求、收结果
- 对应四层栈：ATE = Platform 层，Agent + Skill = Harness 层，PRD→Checklist = 数据层

### Skills Pulse：寄生式可观测性 Harness

- 团队已有 VSK 脚手架（只能统计静态指标）
- 设计**指令注入机制**：向 SKILL.md 注入遥测钩子，不修改原 Skill 代码 → 寄生式 Harness
- Bash 临时文件队列做跨进程调用配对，nohup + disown 做异步非阻塞上报
- 离线 JSONL 补发保证网络故障时数据不丢
- 8 平台适配器（claude/codex/trae/cursor/coco/gemini/opencode/openclaw）
- 本质：**Harness 的 Harness**——给任意 Agent Harness 织入运行时可观测性

### JMeter + CI 门禁：从零搭建 Harness

- 100+ API × 5 业务域 → 200+ 用例（正常+边界+异常）
- 冒烟 vs 回归分层 → CI Pipeline 自动触发做质量门禁
- Harness 结构：JMeter 脚本（Driver）+ 参数化数据（Test Data）+ CI（Fixture）

> **一致原则**：复用现有基建 + 补自己这层，不重造轮子。面试时明确区分"我用/编排了什么"和"我造了什么"。

## 面试回答结构

1. **定义**：Agent = Model + Harness，Harness 是除模型外的一切
2. **三层架构**：Runtime / Capabilities / Assurance
3. **为什么重要**：换 Harness 的收益可能比换模型更大（附数据）
4. **自己搭过什么**：ATE 编排层 / Skills Pulse 遥测 / JMeter+CI 门禁
5. **怎么评估**：六层测试栈 + Eval vs Guardrail 区分
6. **趋势**：Harness 间在收敛（ACP 协议互操作），选择看假设不看特性，对齐 18 个月不是 5 年

## 一句话总结

> Agent Harness 是模型之外的运行层（循环+工具+记忆+沙箱+权限+可观测），其影响常超过模型本身；评估用六层测试栈+Eval Harness（如 DeepEval），实战原则是复用基建+补编排层+不重造轮子，选择 Harness 比假设不比特性能持久。

---

## 八股速记

**问：对 Harness Engineering 的理解？有没有自己搭过？ ⭐⭐⭐**

**答（要点式）**：
- **定义**：Agent = Model + Harness，Harness 是循环编排+工具调用+上下文管理+记忆+沙箱+权限+可观测的一切。
- **核心论点**：换 Harness 的收益常比换模型大（Vercel 去工具成功率 80%→100% 同模型）。
- **三层**：Runtime（主循环）→ Capabilities（工具/记忆）→ Assurance（验证/Eval）。
- **自己搭的**：ATE 平台复用+AI编排层 / Skills Pulse 寄生式遥测 Harness / JMeter+CI 门禁从零搭。原则：复用基建+补编排层，不重造轮子。
- **评估**：六层测试栈（数据认证→单元→集成→E2E→对抗→生产CI/CD），Eval 离线度量 vs Guardrail 在线拦截。

**⭐ 加分**：能区分"我用了什么基建"和"我造了什么"——ATE/KRunner/VSK 是复用，AI 编排层和 Skills Pulse 是自己补的。能讲 Harness-Bench 23.8 分差的数据。能讲 Osmani 棘轮——每次犯错工程化使永不再犯，比预测式 Harness 更抗模型升级。
