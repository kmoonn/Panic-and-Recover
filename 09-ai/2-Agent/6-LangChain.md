---
tags:
  - AI
  - LLM
category: AI/Agent
---

# LangChain

## Q：LangChain 是什么？

LangChain 是一个用于开发 LLM（大语言模型）应用的框架，核心思想是**模块化组合**：将 LLM、外部工具、数据源以"链"的方式串联起来，构建复杂的 AI 应用。它提供了统一的接口来对接不同 LLM 提供商，并内置了 RAG、Agent、Memory 等常见模式的组件。

---

## 核心模块

### 1. Model I/O — 模型交互层

| 组件 | 说明 |
|---|---|
| LLM | 纯文本输入/输出的语言模型接口（如 GPT-4、Claude） |
| ChatModel | 聊天模型接口，输入/输出为消息列表（HumanMessage/AIMessage/SystemMessage） |
| Prompt Template | 提示词模板，支持变量注入，复用提示词 |
| 输出解析器 | 将 LLM 输出解析为结构化数据（JSON、列表等） |

```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 定义模型
llm = ChatOpenAI(model="gpt-4")

# 定义提示词模板
prompt = ChatPromptTemplate.from_template("用一句话解释{concept}")

# 构建链：prompt → llm → 输出解析
chain = prompt | llm | StrOutputParser()

result = chain.invoke({"concept": "量子计算"})
```

### 2. Retrieval — 检索增强生成（RAG）

| 组件 | 说明 |
|---|---|
| 文档加载器（Document Loader） | 从 PDF、网页、数据库等加载文档 |
| 文本分割器（Text Splitter） | 将长文档切分为合适大小的片段 |
| Embedding | 将文本转为向量表示 |
| Vector Store | 向量数据库（FAISS、Chroma、Pinecone 等） |
| 检索器（Retriever） | 根据查询检索相关文档片段 |

```python
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import FAISS

# 加载文档
loader = TextLoader("knowledge.txt")
docs = loader.load()

# 切分文档
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_documents(docs)

# 构建向量库
vectorstore = FAISS.from_documents(chunks, OpenAIEmbeddings())
retriever = vectorstore.as_retriever(search_kwargs={"k": 3})
```

### 3. Chains — 链

链是将多个组件串联执行的方式，是 LangChain 的核心编排机制。

| 类型 | 说明 |
|---|---|
| LLMChain | 最基础的链：Prompt → LLM → Output Parser |
| 顺序链（Sequential Chain） | 多个链按顺序执行，前一个的输出是后一个的输入 |
| 路由链（Router Chain） | 根据输入内容动态选择不同子链 |
| 转换链（Transform Chain） | 对输入/输出做自定义转换 |

> **LCEL（LangChain Expression Language）**：LangChain 推荐使用 `|` 管道操作符来组合链，即 `prompt | llm | parser`，替代旧版的 LLMChain 类。

### 4. Agents — 代理

Agent 可以根据用户输入动态选择并调用工具（Tool），实现自主决策。

| 组件 | 说明 |
|---|---|
| Tool | 工具定义（名称、描述、执行函数） |
| Agent 类型 | ReAct（推理+行动）、Function Calling（函数调用）、Plan-and-Execute 等 |
| AgentExecutor | 代理执行器，管理 Agent 的运行循环 |

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate

# 定义工具
@tool
def search_web(query: str) -> str:
    """搜索网页信息"""
    return f"搜索结果: {query} 的相关信息..."

# 创建 Agent
llm = ChatOpenAI(model="gpt-4")
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个有用的助手，可以使用工具回答问题。"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])
agent = create_tool_calling_agent(llm, [search_web], prompt)
agent_executor = AgentExecutor(agent=agent, tools=[search_web])

result = agent_executor.invoke({"input": "今天北京天气怎么样？"})
```

### 5. Memory — 记忆

| 类型 | 说明 | 适用场景 |
|---|---|---|
| ConversationBufferMemory | 保存完整对话历史 | 短对话 |
| ConversationBufferWindowMemory | 只保留最近 K 轮对话 | 限制 Token |
| ConversationSummaryMemory | 用 LLM 摘要对话历史 | 长对话 |
| ConversationEntityMemory | 提取并记忆对话中的实体 | 需要追踪实体的场景 |

---

## LangChain 与 LangGraph 的区别

| 对比维度 | LangChain | LangGraph |
|---|---|---|
| 定位 | LLM 应用开发框架 | 状态图/多步 Agent 框架 |
| 编排方式 | 线性链（Chain）、单步 Agent | 有向图，支持循环、分支、条件跳转 |
| 状态管理 | Memory 组件（简单） | 内置持久化状态，支持断点续跑 |
| 适用场景 | 简单 RAG、单步工具调用 | 复杂多步 Agent、人机协作、审批流 |
| 复杂度 | 较低 | 较高 |
| 关系 | LangChain 团队出品 | LangChain 团队出品，是 LangChain 的补充 |

> **简单理解**：LangChain 适合"输入→处理→输出"的线性流程；LangGraph 适合需要循环决策、状态回溯的复杂 Agent 场景（如多轮推理、人工审批、并行分支）。

---

## 代码示例：简单的 RAG 链

```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough

# 1. 加载并切分文档
loader = TextLoader("company_faq.txt")
docs = loader.load()
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_documents(docs)

# 2. 构建向量库和检索器
vectorstore = FAISS.from_documents(chunks, OpenAIEmbeddings())
retriever = vectorstore.as_retriever(search_kwargs={"k": 3})

# 3. 定义提示词模板
prompt = ChatPromptTemplate.from_template("""
根据以下上下文回答问题，如果上下文中没有答案，请说"我不知道"。

上下文：{context}

问题：{question}
""")

# 4. 构建 RAG 链
def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)

rag_chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | ChatOpenAI(model="gpt-4")
    | StrOutputParser()
)

# 5. 调用
answer = rag_chain.invoke("公司的年假政策是什么？")
print(answer)
```

---

## 适用场景与局限性

### 适用场景

| 场景 | 说明 |
|---|---|
| RAG 应用 | 企业知识库问答、文档检索 |
| Agent 应用 | 多工具协作、自主决策 |
| 聊天机器人 | 多轮对话、记忆管理 |
| 数据提取 | 从非结构化文本中提取结构化信息 |
| 内容生成 | 自动写作、摘要、翻译 |

### 局限性

| 局限性 | 说明 |
|---|---|
| 抽象层级多 | 代码可读性下降，调试困难 |
| 版本迭代快 | API 频繁变动，升级成本高 |
| 性能开销 | 框架层增加额外延迟，极致性能场景需绕过框架 |
| 复杂 Agent 有限 | 线性链难以表达复杂决策逻辑，需要 LangGraph 补充 |
| 过度封装 | 简单场景下直接调用 API 更轻量 |

---

## 一句话总结

LangChain 是 LLM 应用开发框架，核心模块包括 Model I/O（模型交互）、Retrieval（RAG检索）、Chains（链式编排）、Agents（自主代理）、Memory（对话记忆），适合快速搭建 RAG 和 Agent 应用，但版本迭代快、抽象层级多，复杂多步 Agent 场景建议使用同团队的 LangGraph。
