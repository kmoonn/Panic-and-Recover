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

## 八股速记

### Q14. LangGraph / LangChain 框架深挖 ⭐ B

> 这张卡对应简历项目栈行的 `LangGraph + LangChain`，也是被问"框架具体怎么用"时的补充。**项目里的用法/分层/选型攻防在 `ceec/05-rag-langgraph.md` 和 `ceec/00-rag-platform.md` §3**，本卡只补**框架本身的 API/概念**这层八股，避免被追到 API 名字时卡壳。诚实纪律：**用过的坦白讲用过，只了解的说"了解、项目里没深用"**，别硬凹。

**答（要点式）**：

**① LangChain vs LangGraph 一句话分工**
- **LangChain**：大模型应用的"**组件库 + 链式编排**"——提供 LLM/Embedding 封装、VectorStore、Retriever、文本分块器、Prompt 模板等现成组件，用 **LCEL**（`|` 管道）把它们串成**线性链**。擅长"一条道走到底"的流程。
- **LangGraph**：LangChain 生态里的"**有状态图编排**"——把流程建成**图（节点 + 边 + 共享状态）**，能表达 LangChain 链式做不到的**分支、循环、条件路由、回环**。擅长 agentic（要判断、要重试、要回头）的流程。
- 关系：LangGraph 不是替代 LangChain，而是**上层控制流**；节点内部照样可以调 LangChain 组件。

**② LangGraph 核心概念（被追 API 时能说出名字）**
- **State（状态）**：图里所有节点共享的一个状态对象，一般用 `TypedDict` 定义；字段可配 **reducer**（如 `Annotated[list, add]`）决定"节点返回的值是覆盖还是累加"——多节点写同一字段（如 messages 追加）靠它。
- **Node（节点）**：一个函数，入参是当前 State、返回要更新的字段。
- **Edge（边）**：普通边固定跳下一节点；**条件边 `add_conditional_edges`** 根据一个路由函数的返回值决定跳哪个节点——这就是"召回差就回 rewrite、够好才 generate"的实现。
- **`StateGraph` → `compile()`**：定义好节点和边后编译成可执行图。
- **checkpointer（持久化）**：给 compile 传一个 checkpointer（如内存 / 数据库），图能**按 thread_id 存断点**，实现**多轮会话记忆、断点续跑、human-in-the-loop（`interrupt` 中断等人工介入）**。

**③ LCEL（LangChain Expression Language）**
- 用 `|` 把组件串起来：`prompt | llm | output_parser`，本质是 **Runnable 接口**的组合。
- Runnable 统一提供 `invoke / batch / stream / ainvoke`（同步/批量/流式/异步）；`RunnableParallel` 可并行跑多个分支。好处是**流式和异步开箱即用、组合清晰**。

**⭐ 加分/易错**：
- **别把 LangGraph 说成"另一个框架"**——它是 LangChain 官方的图编排库，考的是"你知道什么时候该从链式升级到图"。
- **一句选型话术**：**"线性流程 LCEL 链就够；一旦要条件分支、回环重试、跨轮状态，就上 LangGraph 状态图。"** 我项目里 agentic RAG（检索→评分→重写回环→自检）正是后者。
- **checkpoint 是高频延伸点**：能说出"LangGraph 靠 checkpointer 按 thread_id 做持久化/多轮记忆"就显深度。🔲 **诚实降级**：我项目里多轮会话记忆是**用 Redis 自己管的**（见 `ceec/00-rag-platform.md`），LangGraph 原生 checkpointer **了解机制、项目里没深用**——被问就这么讲，别假装用过。
- **易混**：LangGraph 的"tool routing / 条件边"是**应用层**控制流，别和 RAG 里"模型路由/熔断"（infra 层选哪个 LLM 实例）混（见 `ceec/02-rag-model-routing.md` 红线）。

---

## 总结一句话

> 链式脚本是一条**直线**（A→B→C），简单高效但不能回头；LangGraph 是一张**图**（可分支、可循环、可暂停），为复杂 Agent 而生。简单任务用链，需要循环/状态/人机交互用图。
