---
tags:
  - AI
  - Agent
category: AI/Agent
---

# Agent 五大核心要素

```
[目标规划器] → [工具执行引擎] → [记忆系统]
       ↑            ↓            ↓
[自省自修正] ←─ [观测反馈闭环] ←─
```


1. Goal-Oriented Planner（目标导向规划器）

不是简单prompt engineering，而是将高层目标（如“帮我订一张下周从北京到上海的高铁票并同步到日历”）自动分解为带依赖关系的子任务图（Task Graph）：[查询12306余票] → [选择车次] → [调用支付接口] → [写入Google Calendar API]。

典型实现：Tree-of-Thought（ToT）或Chain-of-Thought（CoT）+ LLM-based task decomposition，配合结构化输出约束（如JSON Schema）确保可解析性。

2. Tool Integration & Execution Engine（工具集成与执行引擎）

必须支持动态工具注册、参数校验、沙箱化调用、超时熔断、重试策略。

工具描述需遵循标准化协议（如OpenAI Function Calling格式或LangChain Tool Interface），底层通过反射机制（Python inspect）或AST解析实现函数元信息提取。关键点：工具调用不是字符串拼接，而是类型安全的函数对象绑定与异步IO调度（如asyncio.run_in_executor避免阻塞LLM线程）。

3. Memory System（记忆系统）

分为短期记忆（Short-term Memory，即当前session的token-aware context window，需做RAG式压缩或summary truncation防止overflow）和长期记忆（Long-term Memory，如向量数据库Chroma/Weaviate存储用户偏好、历史行为，支持HyDE或Self-RAG检索）。注意：Memory不是缓存，而是带时间戳、来源标签、置信度评分的结构化知识图谱（Knowledge Graph），支持反事实查询（“上次我拒绝的酒店类型是什么？”）。

4. Observation & Feedback Loop（观测与反馈闭环）

Agent必须能接收非文本反馈：API返回的HTTP status code、JSON error字段、CLI命令的stderr、甚至浏览器自动化中的DOM变更事件。典型设计模式是“Action-Observation Cycle”：每执行一个tool_call后，强制插入observation parser（正则/Schema校验/LLM classifier）判断是否成功；失败时触发replan（而非重试），例如支付失败→切换支付方式→重新生成订单号。该循环在代码层面体现为while loop + state machine（如使用transitions库建模状态转移）。

5. Self-Reflection & Self-Correction（自省与自修正）

超越简单retry，包含三层次机制：① 内省层（Introspection）：LLM对自身上一步action生成critique（“我未检查身份证号格式，导致12306接口报错400”）；② 规划层修正（Replanning）：根据critique重构task graph，插入格式校验子步骤；③ 元策略层（Meta-policy）：记录高频失败模式，生成新rule加入memory（如“所有身份证字段必须经re.match(r'^\d{17}[\dXx]$')验证”）。这要求Agent具备可追溯的execution trace（如LangGraph的checkpoints或自研log schema）。

## 八股速记

### Q9. Agent / Function Calling / LangGraph ⭐ B

**答（要点式）**：
- **Agent（智能体）**：让大模型**自主决策 + 调工具 + 多步执行**去完成任务，而非一问一答。核心循环：思考→选工具→执行→看结果→再思考。
- **Function Calling / Tool Use**：模型按预定义 schema 输出"要调哪个函数、传什么参数"，由程序执行后把结果回喂——是 Agent 调工具的底层机制。
- **LangGraph**：用**图（节点+边+状态）**来编排 Agent 流程，相比链式（LangChain）能表达**分支、循环、条件路由**，控制流更清晰可控。

**⭐ 加分/易错**：LangGraph 的价值是**把不可控的自由 Agent 收敛成可控状态机**——每个节点职责单一、可测、可重试。项目里 LangGraph 控制流见 `ceec/05-rag-langgraph.md`；**框架 API 层（StateGraph/条件边/checkpoint/LCEL）深挖见 Q14**。

**⭐ 延伸：怎么测一个 Agent？**（AI 测开加分，百度 JD"AI Agent 测试"）
> Agent 比单轮 LLM 更难测——它**多步、调工具、有状态**，错误会跨步累积。分层测：
> - **① 工具调用正确性**：该调工具时有没有调、**选对工具没**、**参数填对没**（Function Calling 的 schema 是否守住）——这是 Agent 最常错的地方。
> - **② 单步 vs 端到端**：既测**单个节点**（给定输入，这步决策/输出对不对，可 mock 掉工具），也测**整条任务**（最终有没有达成目标）。呼应 LangGraph"每个节点可测"。
> - **③ 任务成功率 + 步数**：多次跑同一任务看**成功率**（非确定性，要多次采样）、**平均步数/是否绕路**、有没有陷入**死循环**（要设最大步数兜底）。
> - **④ 鲁棒性/安全**：工具**返回报错/超时**时 Agent 会不会崩、会不会瞎编结果；**越权**——会不会调不该调的工具、访问不该访问的数据。
> - **扣回我的经验**：**API Testing Agent 本身就是个 Agent**（自主选 Mock/日志/造数补数据、编排用例、失败重试），我做的"每步后置校验 + 失败区分环境抖动/真实缺陷"正是 Agent 可靠性设计（回 `bytedance/03-api-testing-agent.md`）；造数 Skill 的"每步后置校验不对就中断"也是同一思想（`bytedance/01-ai-data-gen.md`）。

---

## 一句话总结

> Agent五大要素：目标规划器、工具执行引擎、记忆系统、观测反馈闭环、自省自修正
