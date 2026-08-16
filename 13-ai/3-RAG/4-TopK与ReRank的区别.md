---
tags:
  - AI
  - RAG
category: AI/RAG
---

# TopK 与 ReRank 的区别

## 检索流程中的位置

```
用户 Query
    │
    ▼
向量检索（粗筛） ──→ 返回 TopK 条结果（如 Top-20）
    │
    ▼
ReRank（精排） ──→ 从 TopK 中重排序，取最终 Top-N（如 Top-5）
    │
    ▼
送入 LLM 生成答案
```

**TopK 是召回阶段，ReRank 是精排阶段**，二者串行配合，而非替代关系。

---

## 核心对比

| 维度 | TopK（粗筛/召回） | ReRank（精排/重排） |
|------|-------------------|---------------------|
| **阶段** | 检索阶段（Retrieval） | 后处理阶段（Post-retrieval） |
| **目标** | 从海量文档中快速捞出候选集 | 从候选集中精确排序，提升最相关结果到前面 |
| **模型** | 向量 Embedding 模型（如 bge-large） | 交叉编码器 Cross-Encoder（如 bge-reranker） |
| **计算方式** | 余弦相似度（Query 向量 vs 文档向量，独立编码） | Query + Document 拼接后联合编码，输出相关性分数 |
| **速度** | 快（向量检索毫秒级，ANN 近似检索） | 慢（每对 Q-D 需完整前向推理） |
| **精度** | 中等（双塔结构，Query 与 Doc 不交互） | 高（交叉编码，捕捉细粒度语义交互） |
| **输入规模** | 全量文档库（百万~亿级） | TopK 候选集（通常 20~100 条） |
| **典型 K 值** | Top-20 ~ Top-100 | 最终取 Top-3 ~ Top-5 |

---

## 为什么需要 ReRank？

### 双塔模型的局限

TopK 阶段用的 Embedding 检索是**双塔结构**（Bi-Encoder）：

```
Query  → Encoder → Q向量 ─┐
                            ├→ cos(Q, D) → 相似度
Doc    → Encoder → D向量 ─┘
```

Query 和 Document **独立编码**，只在最后算点积/余弦。优点是可预计算文档向量、检索速度快；缺点是 **Query 和 Doc 之间没有交互**，无法捕捉细粒度语义匹配（如否定词、条件限定、多跳推理）。

### ReRank 的交叉编码

ReRank 用 **Cross-Encoder**（交叉编码器）：

```
[CLS] Query [SEP] Document [SEP] → Encoder → 相关性分数
```

Query 和 Document **拼接后联合编码**，每一层 Attention 都有交互，能捕捉：
- 否定关系（"不推荐" vs "推荐"）
- 条件限定（"Java 8" vs "Java 17"）
- 细粒度语义匹配（同义改写、指代消解）

### 典型效果提升

| 指标 | 仅 TopK | TopK + ReRank | 提升 |
|------|---------|---------------|------|
| Hit Rate@5 | ~65% | ~82% | +17% |
| MRR | ~0.55 | ~0.74 | +0.19 |
| NDCG@10 | ~0.60 | ~0.78 | +0.18 |

> 数据来源：MTEB/BEIR 基准上 bge-reranker 实验结果，具体数值因场景而异。

---

## 常用 ReRank 模型

| 模型 | 特点 | 适用场景 |
|------|------|---------|
| **bge-reranker-base** | BAAI 开源，中英文支持好 | 通用场景 |
| **bge-reranker-large** | 更大参数，精度更高 | 精度优先 |
| **cohere-rerank** | API 服务，无需本地部署 | 快速接入 |
| **ms-marco-MiniLM** | 英文场景经典模型 | 英文为主 |
| **jina-reranker-v2** | 支持长文档，8k 上下文 | 长文档检索 |

---

## 代码示例

```python
from sentence_transformers import CrossEncoder

# 加载 ReRank 模型
reranker = CrossEncoder("BAAI/bge-reranker-base")

query = "什么是 RAG？"
documents = [
    "RAG 是检索增强生成技术...",
    "GPT 是生成式预训练模型...",
    "RAG 通过检索外部知识减少幻觉...",
    "向量数据库用于存储 Embedding...",
]

# 对 (query, doc) 对打分
pairs = [[query, doc] for doc in documents]
scores = reranker.predict(pairs)

# 按分数降序排列
ranked = sorted(zip(scores, documents), reverse=True)
for score, doc in ranked:
    print(f"[{score:.4f}] {doc}")
```

### 结合 LangChain 使用

```python
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CrossEncoderReranker
from langchain_community.cross_encoders import HuggingFaceCrossEncoder

# 基础向量检索器（TopK 粗筛）
base_retriever = vectorstore.as_retriever(search_kwargs={"k": 20})

# ReRank 压缩器（精排）
compressor = CrossEncoderReranker(
    model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-base"),
    top_n=5,
)

# 组合检索器
compression_retriever = ContextualCompressionRetriever(
    base_compressor=compressor,
    base_retriever=base_retriever,
)

docs = compression_retriever.invoke("什么是 RAG？")
```

---

## 面试高频

### TopK 的 K 值怎么选？

| K 值 | 特点 | 适用场景 |
|------|------|---------|
| 太小（如 5） | 召回不足，可能遗漏相关文档 | 精确匹配场景 |
| 适中（20~50） | 兼顾召回率和 ReRank 开销 | 大多数场景 |
| 太大（如 200+） | 召回充分但 ReRank 计算量大 | 高精度要求场景 |

经验：**TopK 取 20~50，ReRank 后取 Top-3~5 送入 LLM**。

### 可以不用 ReRank 吗？

可以，但效果会下降。以下场景可以跳过 ReRank：
- 文档量小（< 1000），Embedding 检索已足够精准
- 对延迟极度敏感，ReRank 耗时不可接受
- 简单的 FAQ 匹配场景，答案唯一明确

### ReRank 和 RRF（倒数秩融合）的区别？

| 维度 | ReRank | RRF |
|------|--------|-----|
| 原理 | 用模型重新打分 | 多路检索结果按排名加权融合 |
| 需要模型 | 是（Cross-Encoder） | 否（纯排序算法） |
| 计算开销 | 高 | 低 |
| 适用场景 | 单路检索精排 | 多路检索融合（向量+关键词+结构化） |

---

## 一句话总结

> TopK 是粗筛阶段用向量相似度从海量文档中快速召回候选集，ReRank 是精排阶段用交叉编码器对候选集重排序以提升精度，二者串行配合——先粗后精，兼顾效率与效果。
