---
tags:
  - AI
  - LLM
category: AI/Agent
---

# LangGraph 与 Python 链式脚本的区别

## LangGraph 是什么

LangGraph 是基于 LangChain 的**有状态图框架**，用于构建多步、多 Agent 的 LLM 应用。核心思想：**用图（Graph）定义控制流**，节点（Node）是函数/Agent，边（Edge）定义节点间的跳转逻辑。

## Python 链式脚本是什么

指用 `|` 管道符或函数嵌套将多个步骤串成**线性流水线**，如 LangChain 的 LCEL（LangChain Expression Language）：

```python
chain = prompt | llm | parser
result = chain.invoke({"question": "..."})
```

数据从左到右依次流过每个环节，**只能从前往后、不能回头**。

## 核心区别

| | LangGraph | Python 链式脚本 |
|---|---|---|
| **结构** | 图（有向图，支持环/分支/并行） | 链（线性管道） |
| **控制流** | 条件边、循环、分支、并行节点 | 只能 A → B → C 顺序执行 |
| **状态管理** | 内置状态对象，节点间共享 & 自动持久化 | 无内置状态，靠手动传参 |
| **循环/重试** | ✅ 支持（如 Agent 反复调用工具直到完成） | ❌ 不支持（管道无法回头） |
| **人机交互** | ✅ `interrupt` 暂停等人工输入后继续 | ❌ 无法中途暂停 |
| **并行** | ✅ 多节点并行执行 | ❌ 严格顺序 |
| **持久化** | ✅ 内置 checkpoint，可断点恢复 | 需自行实现 |
| **适用场景** | 复杂 Agent、多轮推理、需要循环/分支 | 简单 RAG、单次推理、线性流水线 |

## 代码对比

### 链式脚本：线性 RAG

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

# 简单线性链：prompt → llm → 输出
chain = ChatPromptTemplate.from_template("回答：{question}") | ChatOpenAI()
result = chain.invoke({"question": "什么是LangGraph"})
```

无法中途判断"答案不够好，重新检索"——因为没有回头路。

### LangGraph：带循环的 Agent

```python
from langgraph.graph import StateGraph, END

# 1. 定义状态
class State(TypedDict):
    messages: list
    next_action: str

# 2. 定义节点
def agent_node(state):
    # LLM 决定下一步：调用工具 or 直接回答
    response = llm.invoke(state["messages"])
    return {"messages": [response], "next_action": response.tool_calls[0].name if response.tool_calls else "finish"}

def tool_node(state):
    # 执行工具调用
    result = execute_tool(state["messages"][-1].tool_calls[0])
    return {"messages": [result]}

# 3. 定义条件边（循环关键）
def should_continue(state):
    return "tools" if state["next_action"] != "finish" else END

# 4. 构建图
graph = StateGraph(State)
graph.add_node("agent", agent_node)
graph.add_node("tools", tool_node)
graph.add_edge("agent", "tools")          # agent → tools
graph.add_conditional_edges("tools", should_continue)  # tools → agent or END

app = graph.compile()
result = app.invoke({"messages": [{"role": "user", "content": "查询北京天气"}]})
```

Agent 可以**反复调用工具直到满意**，链式脚本做不到。

## 什么时候用哪个

| 场景 | 推荐 |
|---|---|
| 单次 RAG（检索 → 生成 → 返回） | 链式脚本 |
| 需要循环的 Agent（ReAct、工具反复调用） | LangGraph |
| 多 Agent 协作 | LangGraph |
| 需要人工审核中间结果 | LangGraph |
| 需要断点恢复 / 长时间运行 | LangGraph |
| 简单 ETL 流水线 | 链式脚本 |

## 总结一句话

> 链式脚本是一条**直线**（A→B→C），简单高效但不能回头；LangGraph 是一张**图**（可分支、可循环、可暂停），为复杂 Agent 而生。简单任务用链，需要循环/状态/人机交互用图。
