---
tags:
  - AI
  - Agent
category: AI/Agent
---

# ReAct 模式

## 什么是 ReAct

**ReAct**（Reasoning + Acting）是一种让 LLM Agent 交替进行推理（Thought）和行动（Action）的模式，由 Yao et al. 2022 提出。它是当前最主流的 Agent 执行范式。

```
用户输入
    │
    ▼
┌─────────┐    ┌─────────┐    ┌─────────────┐
│ Thought │ →  │ Action  │ →  │ Observation │
│ (推理)   │    │ (行动)   │    │ (观察结果)   │
└─────────┘    └─────────┘    └─────────────┘
     ↑                               │
     └───────────────────────────────┘
              循环直到得出答案
```

---

## ReAct 流程示例

### 示例：查询城市人口排名

```
Question: 上海和北京哪个城市人口更多？多多少？

Thought 1: 我需要查询上海和北京的人口数据
Action 1: search[上海人口]
Observation 1: 上海常住人口约 2487 万人（2023年）

Thought 2: 现在查询北京的人口
Action 2: search[北京人口]
Observation 2: 北京常住人口约 2185 万人（2023年）

Thought 3: 上海 2487 万，北京 2185 万，上海更多，多 302 万
Action 3: finish[上海人口更多，约多 302 万人（上海 2487 万 vs 北京 2185 万）]
```

### 示例：代码调试

```
Question: 这段 Python 代码为什么报错？

Thought 1: 先看一下代码内容
Action 1: read_file[path="bug.py"]
Observation 1:
  def divide(a, b):
      return a / b
  print(divide(10, 0))

Thought 2: 代码在第2行做了除法，但调用时 b=0，会抛出 ZeroDivisionError
Action 2: finish[错误原因是除数为0。调用 divide(10, 0) 时 b=0，
           导致 a/b 抛出 ZeroDivisionError。修复方法：加除零判断]
```

---

## ReAct vs 纯推理 vs 纯行动

| 模式 | 做法 | 问题 |
|------|------|------|
| **纯推理**（CoT） | 只用 Thought 推理，不调用工具 | 无法获取外部信息，容易幻觉 |
| **纯行动** | 只调用工具，不推理 | 盲目调用，不知道为什么这么做 |
| **ReAct** | Thought 和 Action 交替 | ✅ 推理指导行动，行动验证推理 |

```
纯推理：Thought → Thought → Thought → Answer（可能幻觉）
纯行动：Action → Action → Action → Answer（可能盲目）
ReAct： Thought → Action → Observation → Thought → Action → ... → Answer
```

---

## Prompt 模板

### 基础模板

```
你是一个智能助手，可以通过推理和行动来回答问题。

可用工具：
- search[query]: 搜索信息
- lookup[term]: 在当前文档中查找术语
- finish[answer]: 给出最终答案

按照以下格式回答：

Question: 输入的问题
Thought: 推理你应该做什么
Action: 你要采取的行动
Observation: 行动的结果（系统返回）

... (Thought/Action/Observation 可以重复多次)

Thought: 我现在知道最终答案了
Action: finish[最终答案]
```

### 带工具描述的模板

```python
REACT_PROMPT = """你是一个智能助手，可以交替进行推理和行动来回答问题。

可用工具：
{tool_descriptions}

格式要求：
Question: 用户问题
Thought: 分析当前情况，决定下一步
Action: 工具名称[参数]
Observation: 工具返回结果

（Thought/Action/Observation 循环）

Thought: 我已经获得了足够的信息
Action: finish[最终答案]

开始！

Question: {input}
Thought: {agent_scratchpad}"""
```

---

## 代码实现

### 使用 LangChain 实现

```python
from langchain.agents import create_react_agent, AgentExecutor
from langchain.tools import tool
from langchain_openai import ChatOpenAI

# 定义工具
@tool
def search_weather(city: str) -> str:
    """查询城市天气"""
    # 实际调用天气 API
    return f"{city}: 晴，28°C"

@tool
def search_population(city: str) -> str:
    """查询城市人口"""
    return f"{city}: 约2400万"

tools = [search_weather, search_population]

# 创建 ReAct Agent
llm = ChatOpenAI(model="gpt-4", temperature=0)
agent = create_react_agent(llm, tools, REACT_PROMPT)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

# 执行
result = agent_executor.invoke({"input": "北京今天适合户外活动吗？"})
print(result["output"])
```

### 输出示例

```
> Entering new AgentExecutor chain...

Thought: 我需要查询北京今天的天气
Action: search_weather[北京]
Observation: 北京: 晴，28°C
Thought: 天气晴朗温度适宜，适合户外活动
Action: finish[北京今天晴天28°C，非常适合户外活动]

> Finished chain.
```

---

## ReAct 的局限性

| 局限 | 说明 | 缓解方案 |
|------|------|---------|
| **线性执行** | 一步步串行，无法并行 | 结合 Plan-and-Execute 做全局规划 |
| **长链偏移** | 步骤多了容易跑偏 | 限制最大步数 + 中间校验 |
| **Token 消耗** | 每步都要重复历史 | 压缩历史 Obs / 滑动窗口 |
| **无自修正** | 推理错误会传播 | 结合 Reflexion 加入反思 |
| **死循环** | 反复调用同一工具 | 检测重复 Action，强制退出 |

### 防死循环实践

```python
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    max_iterations=10,        # 最大迭代次数
    max_execution_time=60,    # 最大执行时间(秒)
    early_stopping_method="generate",  # 超限时强制生成答案
    handle_parsing_errors=True,  # Action 解析失败时不崩溃
)
```

---

## ReAct 的演进

```
ReAct (2022)
  │
  ├── Reflexion (2023) ── 加入自我反思
  │     Act → Evaluate → Reflect → Re-Act
  │
  ├── LATS (2023) ── 加入树搜索
  │     多条推理路径并行探索，选最优
  │
  └── ReAct + RAG ── 结合检索增强
        Action 中加入 retrieve 工具
        推理时参考检索到的文档
```

---

## 面试高频

### ReAct 为什么比纯 CoT 效果好？

CoT 只依赖模型内部知识推理，遇到知识不足或需要实时信息时会幻觉。ReAct 通过 Action 获取外部信息，Observation 验证推理，形成"推理→验证→调整"的闭环，减少幻觉。

### ReAct 的 Action 怎么设计？

| 设计原则 | 说明 |
|---------|------|
| 原子性 | 每个 Action 做一件事，便于组合 |
| 描述清晰 | 工具描述决定 LLM 会不会用 |
| 参数简单 | 避免复杂嵌套参数，LLM 容易填错 |
| 返回结构化 | 返回 JSON 而非长文本，便于解析 |

### ReAct 适合什么场景？不适合什么？

| 适合 | 不适合 |
|------|--------|
| 需要外部信息的问答 | 纯数学推理（CoT 更好） |
| 多步骤工具调用 | 简单分类/抽取任务 |
| 需要可追踪推理过程 | 超低延迟场景 |
| 调试/排错类任务 | 单步直接回答即可的场景 |

---

## 一句话总结

> ReAct 是最经典的 Agent 范式，通过 Thought→Action→Observation 循环让 LLM 交替推理与行动，推理指导行动方向，行动结果验证和补充推理，比纯推理（CoT）减少幻觉，比纯行动更有目标性，是大多数 Agent 应用的起点。
