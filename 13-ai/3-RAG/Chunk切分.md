---
tags:
  - AI
  - RAG
  - Chunk
category: AI/RAG
---

# Chunk 切分

## 为什么切分很重要

Chunk 大小和质量直接决定检索效果：
- **太大**：检索不精准，噪声多，相似度高但无关内容多
- **太小**：语义不完整，上下文断裂，检索到片段拼不出完整答案

## 常见切分策略

| 策略 | 原理 | 优点 | 缺点 |
|---|---|---|---|
| **固定长度切分** | 按字符/Token 数切，配 overlap | 简单通用 | 可能切断句子/段落 |
| **递归字符切分** | 按分隔符优先级递归拆（`\n\n` → `\n` → `。` → 空格） | 语义尽量完整 | 长短不均 |
| **语义切分** | 用 Embedding 计算相邻句子相似度，低于阈值处断开 | 语义连贯 | 计算开销大 |
| **文档结构切分** | 按标题/章节/Markdown 标题层级切 | 保持结构完整 | 依赖文档格式 |
| **滑动窗口** | 固定窗口 + 步长滑动 | 覆盖全面 | 冗余度高 |

## 最常用：递归字符切分

LangChain 的 `RecursiveCharacterTextSplitter`，按分隔符优先级从大到小递归拆：

```python
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,       # 每个 chunk 最大 500 字符
    chunk_overlap=50,     # 相邻 chunk 重叠 50 字符，防止切断上下文
    separators=["\n\n", "\n", "。", "！", "？", "；", " ", ""]
)

chunks = splitter.split_text(document)
```

**为什么需要 overlap**：切分点附近的内容可能同时与查询相关，overlap 保证边界信息不被丢失。

## chunk_size 怎么选

| 场景 | 推荐 chunk_size | overlap |
|---|---|---|
| 事实型问答（精确检索） | 200–500 | 20–50 |
| 摘要/长文理解 | 800–1500 | 100–200 |
| 代码检索 | 500–1000 | 50–100 |
| 对话历史 | 300–600 | 30–60 |

经验法则：**chunk_size ≈ 模型可处理上下文的 1/5 ~ 1/3**，给检索到的多个 chunk 留出拼接空间。

## 进阶：元数据增强

每个 chunk 附加元数据，检索时可过滤：

```python
chunks = splitter.create_documents(
    [document],
    metadatas=[{"source": "doc_a", "chapter": "3", "page": 12}]
)
# 检索时可按 source/chapter 过滤
```

## 进阶：父子 Chunk

大 chunk（父）用于生成答案，小 chunk（子）用于精确检索：

```
父 chunk（1000字）
├── 子 chunk A（200字）  ← 检索命中
├── 子 chunk B（200字）
├── 子 chunk C（200字）
└── 子 chunk D（200字）

检索命中子 chunk A → 返回其父 chunk 整体作为上下文
```

好处：**检索精准（小 chunk），上下文完整（大 chunk）**。

---

## 八股速记

### Q3. 什么是 RAG，为什么要用 ⭐⭐

**答（要点式）**：
- **RAG（检索增强生成）**：回答前先从**外部知识库检索**相关片段，把片段拼进 prompt 再让大模型生成。= "开卷考试"。
- **为什么要用**：① 大模型知识**有截止时间**、不懂私有/内部数据；② 直接问会**幻觉**；③ 微调贵且更新慢。RAG 让模型基于**给定的、可追溯的资料**回答。

**⭐ 加分/易错**：RAG 的核心价值是**可溯源 + 可实时更新**——答案能标出处，知识库改了立即生效，不用重训模型。这正是中国能建"数据不出外网 + 内部文档问答"场景选 RAG 的原因（回 `ceec/00-rag-platform.md`）。

### Q4. RAG 完整链路 ⭐⭐

**答（主线，能串起大部分 RAG 知识）**：
1. **文档切块（Chunking）**：把长文档切成小片段（按段落/固定 token/语义），太大噪声多、太小丢上下文。
2. **向量化（Embedding）**：每个 chunk 过 embedding 模型转成向量，存进**向量数据库**。
3. **检索（Retrieval）**：用户问题也向量化，去库里找**最相似的 Top-K** chunk；常配**混合检索**（向量语义 + BM25 关键词）。
4. **重排（Rerank）**：用更精细的 rerank 模型对 Top-K 重新打分排序，提升相关性。
5. **生成（Generation）**：把命中的 chunk 拼进 prompt（"根据以下资料回答……"），大模型生成带出处的答案。

**⭐ 加分**：这是"钩子题"，每一步都能被深挖——切块策略、混合检索怎么融合、rerank 为啥需要、prompt 怎么防止模型不看资料乱答。项目里"多路并行检索 + 融合"见 `ceec/01-rag-retrieval.md`。

---

## 一句话总结

> Chunk 切分核心是选对策略与尺寸：递归字符切分最通用，chunk_size 按场景选，overlap 防断上下文，父子 chunk 兼顾精准与完整。
