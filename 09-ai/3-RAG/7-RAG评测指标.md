---
tags:
  - AI
  - RAG
category: AI/RAG
---

# RAG 评测指标

## 指标体系总览

```
RAG 评测指标
├── 检索层指标（召回质量）
│   ├── Context Recall      ← 召回率
│   ├── Context Precision   ← 精确度
│   ├── Hit Rate            ← 命中率
│   ├── MRR                 ← 平均倒数排名
│   └── NDCG                ← 归一化折损累计增益
├── 生成层指标（答案质量）
│   ├── Faithfulness        ← 忠实度
│   ├── Answer Relevancy    ← 答案相关性
│   └── Answer Similarity   ← 答案相似度
└── 端到端指标（整体效果）
    └── 业务指标（用户满意度 / 采纳率）
```

---

## 一、检索层指标

### 1. Context Recall（上下文召回率）

**含义**：标准答案中的关键信息，有多少被检索内容覆盖。

**公式**：

```
Context Recall = 被检索内容覆盖的 GT 要点数 / GT 总要点数
```

**示例**：

```
问题：HashMap 的底层实现？
GT 要点：数组+链表、负载因子、扩容、红黑树转换
检索内容覆盖：数组+链表、扩容、红黑树转换（缺"负载因子"）

Context Recall = 3 / 4 = 0.75
```

| 分数 | 解读 |
|------|------|
| > 0.9 | 召回充分，检索质量好 |
| 0.6~0.9 | 部分信息缺失，需优化 Chunk/Embedding |
| < 0.6 | 召回严重不足，需排查 |

**计算方式**：用 LLM 将 GT 拆分为原子要点，逐条判断是否有检索内容支撑。

---

### 2. Context Precision（上下文精确度）

**含义**：检索结果中相关文档是否排名靠前（排在前面的噪声越少越好）。

**公式**：

```
Context Precision = Σ(precision@k × rel_k) / 相关文档总数

precision@k = 前 k 个结果中相关文档数 / k
rel_k = 第 k 个结果是否相关（0 或 1）
```

**示例**：

```
检索 5 个文档：[✓相关, ✗不相关, ✓相关, ✗不相关, ✓相关]

precision@1 = 1/1 = 1.0   rel_1 = 1  → 1.0 × 1 = 1.0
precision@2 = 1/2 = 0.5   rel_2 = 0  → 0.5 × 0 = 0
precision@3 = 2/3 = 0.67  rel_3 = 1  → 0.67 × 1 = 0.67
precision@4 = 2/4 = 0.5   rel_4 = 0  → 0.5 × 0 = 0
precision@5 = 3/5 = 0.6   rel_5 = 1  → 0.6 × 1 = 0.6

Context Precision = (1.0 + 0 + 0.67 + 0 + 0.6) / 3 = 0.76
```

| 分数 | 解读 |
|------|------|
| > 0.85 | 相关内容排名靠前，噪声少 |
| 0.5~0.85 | 有噪声混入，考虑加 ReRank |
| < 0.5 | 检索质量差，需换 Embedding 或优化 Query |

---

### 3. Hit Rate（命中率）

**含义**：检索结果中是否包含正确文档（不关注排名位置）。

**公式**：

```
Hit Rate@K = 包含正确文档的查询数 / 总查询数
```

| K 值 | 典型值 | 说明 |
|------|--------|------|
| Hit@1 | 30%~50% | 第一个结果就命中，要求极高 |
| Hit@5 | 60%~80% | 前 5 个有命中，常见评估标准 |
| Hit@10 | 70%~90% | 前 10 个有命中，门槛较低 |

**与 Context Recall 的区别**：Hit Rate 只看"有没有"（二值），Context Recall 看"覆盖了多少"（比例）。

---

### 4. MRR（Mean Reciprocal Rank）

**含义**：正确文档在检索结果中的排名倒数的均值，衡量排序质量。

**公式**：

```
MRR = 1/|Q| × Σ(1 / rank_i)

rank_i: 第 i 个查询中，第一个正确文档的排名
```

**示例**：

```
查询1: 正确文档排在第 1 位 → 1/1 = 1.0
查询2: 正确文档排在第 3 位 → 1/3 = 0.33
查询3: 正确文档排在第 2 位 → 1/2 = 0.5

MRR = (1.0 + 0.33 + 0.5) / 3 = 0.61
```

| MRR 值 | 解读 |
|--------|------|
| > 0.8 | 排序质量优秀 |
| 0.5~0.8 | 中等，有优化空间 |
| < 0.5 | 排序质量差，需 ReRank |

---

### 5. NDCG（Normalized Discounted Cumulative Gain）

**含义**：考虑位置权重的排序质量指标，排名越靠前权重越大。

**公式**：

```
DCG@K = Σ(rel_i / log2(i+1))     i = 1,2,...,K
NDCG@K = DCG@K / IDCG@K          (归一化到 [0,1])

rel_i: 第 i 个结果的相关度（0=不相关, 1=部分相关, 2=完全相关）
IDCG: 理想排序下的 DCG（所有相关文档排最前面）
```

**示例**：

```
检索 5 个文档，相关度：[2, 0, 1, 0, 2]

DCG@5 = 2/log2(2) + 0/log2(3) + 1/log2(4) + 0/log2(5) + 2/log2(6)
      = 2/1 + 0 + 1/2 + 0 + 2/2.58
      = 2 + 0 + 0.5 + 0 + 0.77 = 3.27

理想排序：[2, 2, 1, 0, 0]
IDCG@5 = 2/1 + 2/1.58 + 1/2 + 0 + 0 = 2 + 1.27 + 0.5 = 3.77

NDCG@5 = 3.27 / 3.77 = 0.87
```

| NDCG 值 | 解读 |
|---------|------|
| > 0.9 | 排序接近理想 |
| 0.7~0.9 | 较好 |
| < 0.7 | 排序偏差较大 |

**NDCG vs MRR**：MRR 只看第一个正确结果的位置；NDCG 考虑所有结果的位置和相关度，更全面但计算更复杂。

---

## 二、生成层指标

### 6. Faithfulness（忠实度）

**含义**：答案是否仅基于检索内容，有无幻觉。

**公式**：

```
Faithfulness = 被检索内容支撑的 claims 数 / 总 claims 数
```

**计算步骤**：
1. 将答案拆分为原子声明（claims）
2. 逐条判断每个 claim 是否有检索内容支撑
3. 计算支撑比例

**示例**：

```
检索内容："Redis 是单线程模型，使用 IO 多路复用"
生成答案："Redis 是单线程模型，使用 IO 多路复用，数据存储在磁盘上"

Claims:
  1. "Redis 是单线程模型"       → ✅ 有支撑
  2. "使用 IO 多路复用"         → ✅ 有支撑
  3. "数据存储在磁盘上"         → ❌ 幻觉（Redis 主要在内存）

Faithfulness = 2 / 3 = 0.67
```

| 分数 | 解读 |
|------|------|
| 1.0 | 完全忠实，零幻觉 |
| 0.8~0.99 | 轻微幻觉，可接受 |
| < 0.8 | 幻觉严重，需优化 Prompt 或检索 |

---

### 7. Answer Relevancy（答案相关性）

**含义**：答案是否切题，没有答非所问或冗余信息。

**公式**：

```
Answer Relevancy = 1/N × Σ(cos(E(q_i), E(original_q)))

E(q_i): 从答案反推出的第 i 个问题的 Embedding
E(original_q): 原始问题的 Embedding
N: 反推问题的数量（通常生成 3~5 个）
```

**直觉**：如果答案切题，从答案反推出来的问题应该和原始问题语义接近。

**示例**：

```
原始问题：什么是 RAG？
答案A："RAG 是检索增强生成技术"  → 反推问题接近原始 → 相关性高
答案B："LLM 是大语言模型"       → 反推问题偏离原始 → 相关性低
```

| 分数 | 解读 |
|------|------|
| > 0.85 | 答案切题 |
| 0.6~0.85 | 有偏题倾向 |
| < 0.6 | 严重跑题 |

---

### 8. Answer Similarity（答案相似度）

**含义**：生成答案与参考答案的语义相似度。

**公式**：

```
Answer Similarity = cos(E(answer), E(ground_truth))
```

| 分数 | 解读 |
|------|------|
| > 0.9 | 语义高度一致 |
| 0.7~0.9 | 核心意思对，表述不同 |
| < 0.7 | 偏差较大 |

---

## 三、指标选型指南

### 按 RAG 优化阶段选指标

| 优化阶段 | 关注指标 | 目标 |
|---------|---------|------|
| 基线建立 | 全部跑一遍 | 记录基准值 |
| 优化检索 | Context Recall ↑, Hit Rate ↑, MRR ↑ | 召回更多相关文档 |
| 加 ReRank | Context Precision ↑, NDCG ↑ | 相关文档排更前 |
| 优化 Prompt | Faithfulness ↑ | 减少幻觉 |
| 端到端验证 | Answer Relevancy ↑, 业务指标 ↑ | 整体效果提升 |

### 指标组合诊断

| 症状 | 诊断 | 优化方向 |
|------|------|---------|
| Recall 高 + Faithfulness 低 | 检索到了但生成不忠实 | 优化 Prompt（加防幻觉指令） |
| Recall 低 + Faithfulness 高 | 检索不够但没幻觉 | 优化检索（换 Embedding / 加 ReRank） |
| Recall 高 + Precision 低 | 召回了但噪声多 | 加 ReRank / 减少 TopK |
| Answer Relevancy 低 + Faithfulness 高 | 忠实但偏题 | 优化 Prompt（强调切题） |

### 快速参考卡

| 指标 | 范围 | 越高越好？ | 关键输入 | 最关注阶段 |
|------|------|----------|---------|-----------|
| Context Recall | 0~1 | ✅ | GT + Contexts | 检索优化 |
| Context Precision | 0~1 | ✅ | GT + Contexts | ReRank |
| Hit Rate | 0~1 | ✅ | GT + Contexts | 检索基线 |
| MRR | 0~1 | ✅ | GT + Contexts | 排序质量 |
| NDCG | 0~1 | ✅ | GT + Contexts | 排序质量 |
| Faithfulness | 0~1 | ✅ | Answer + Contexts | 生成优化 |
| Answer Relevancy | 0~1 | ✅ | Question + Answer | 端到端 |
| Answer Similarity | -1~1 | ✅ | Answer + GT | 端到端 |

---

## 面试高频

### 哪个指标最重要？

**Faithfulness**。RAG 的核心价值是减少幻觉，如果答案不忠实于检索内容，RAG 就失去了意义。其次是 Context Recall——检索不到正确的文档，再好的生成也无用。

### 这些指标需要人工标注吗？

| 指标 | 是否需要 GT | 说明 |
|------|-----------|------|
| Context Recall | 需要 | 需要标准答案要点 |
| Context Precision | 不严格需要 | LLM 可判断相关性 |
| Hit Rate / MRR / NDCG | 需要 | 需要标注相关文档 |
| Faithfulness | 不需要 | LLM 可判断 claims 是否有支撑 |
| Answer Relevancy | 不严格需要 | LLM 可反推问题 |
| Answer Similarity | 需要 | 需要参考答案 |

实践做法：**少量人工标注（50~100 条）+ LLM 自动评估其余**。

### MRR 和 NDCG 怎么选？

| 场景 | 推荐 | 原因 |
|------|------|------|
| 只关心第一个正确结果 | MRR | 更简单直观 |
| 需要评估整体排序质量 | NDCG | 考虑所有位置 |
| 文档有多级相关度 | NDCG | 支持相关度分级（0/1/2） |
| 文档只分相关/不相关 | MRR | 够用了 |

---

## 一句话总结

> RAG 评测指标分三层：检索层看 Context Recall（召回率）、Context Precision（精确度）、Hit Rate/MRR/NDCG（排序质量），生成层看 Faithfulness（忠实度，最重要）、Answer Relevancy（相关性），组合诊断定位瓶颈——Recall 低优化检索，Faithfulness 低优化 Prompt，Precision 低加 ReRank。
