---
tags:
  - AI
  - RAG
category: AI/RAG
---

# 评估 RAG 的效果

## 为什么需要评估 RAG

RAG 系统不是"检索+生成"就完事了，需要量化评估：
- 检索是否找对了文档？（召回质量）
- 生成是否忠实于检索内容？（防幻觉）
- 答案是否有用？（用户体验）

没有评估，就无法迭代优化——你不知道是 Chunk 切分的问题、Embedding 模型的问题、还是 Prompt 的问题。

---

## 评估框架：RAGAS

**RAGAS**（Retrieval Augmented Generation Assessment）是最主流的 RAG 评估框架，定义了核心指标：

| 指标 | 全称 | 评估什么 | 依赖 |
|------|------|---------|------|
| **Faithfulness** | 忠实度 | 答案是否仅基于检索内容，无幻觉 | Answer + Contexts |
| **Answer Relevancy** | 答案相关性 | 答案是否切题 | Question + Answer |
| **Context Precision** | 上下文精确度 | 检索到的内容中有多少是相关的 | Question + Contexts |
| **Context Recall** | 上下文召回率 | 相关内容是否都被检索到了 | Question + Ground Truth |

### 指标详解

#### 1. Faithfulness（忠实度）

衡量答案是否**忠实于检索内容**，不编造信息。

```
检索内容：Python 由 Guido van Rossum 于 1991 年发布
生成答案：Python 由 Guido van Rossum 于 1991 年发布    → 忠实度 = 1.0

检索内容：Python 由 Guido van Rossum 于 1991 年发布
生成答案：Python 由 Dennis Ritchie 于 1972 年发布      → 忠实度 = 0.0
```

**计算方式**：将答案拆分为多个声明（claims），逐条判断是否有检索内容支撑：

```
Faithfulness = 被支撑的 claims 数 / 总 claims 数
```

#### 2. Answer Relevancy（答案相关性）

衡量答案是否**切题**，不答非所问。

```
问题：什么是 RAG？
答案：RAG 是检索增强生成，通过检索外部知识辅助 LLM 生成答案  → 相关性高
答案：LLM 是大语言模型，基于 Transformer 架构              → 相关性低
```

**计算方式**：用 LLM 从答案反推可能的问题，计算反推问题与原始问题的语义相似度。

#### 3. Context Precision（上下文精确度）

衡量检索结果中**相关文档的排名是否靠前**。

```
检索了 4 个文档：[相关, 不相关, 相关, 不相关]
→ 相关文档排在第 1、3 位 → 精确度中等

检索了 4 个文档：[相关, 相关, 不相关, 不相关]
→ 相关文档排在第 1、2 位 → 精确度高
```

#### 4. Context Recall（上下文召回率）

衡量标准答案中的信息**是否都被检索内容覆盖**。

```
标准答案要点：A、B、C
检索内容覆盖：A、B
→ Context Recall = 2/3 = 0.67
```

---

## 代码示例

```python
from ragas import evaluate
from ragas.metrics import (
    faithfulness,
    answer_relevancy,
    context_precision,
    context_recall,
)
from datasets import Dataset

# 准备评估数据
data = {
    "question": [
        "什么是 RAG？",
        "RAG 如何减少幻觉？",
    ],
    "answer": [
        "RAG 是检索增强生成，通过检索外部知识辅助 LLM 生成答案",
        "RAG 让 LLM 基于检索到的真实文档生成答案，而非仅依赖参数化记忆",
    ],
    "contexts": [
        ["RAG 是 Retrieval-Augmented Generation 的缩写..."],
        ["RAG 通过检索外部文档为 LLM 提供事实依据，减少幻觉..."],
    ],
    "ground_truth": [
        "RAG 通过检索外部知识库增强 LLM 生成能力",
        "RAG 为 LLM 提供检索到的事实依据，避免编造",
    ],
}

dataset = Dataset.from_dict(data)

# 评估
result = evaluate(
    dataset,
    metrics=[
        faithfulness,
        answer_relevancy,
        context_precision,
        context_recall,
    ],
)

print(result)
# {'faithfulness': 0.92, 'answer_relevancy': 0.88, ...}
```

---

## 其他评估指标

| 指标 | 说明 | 适用场景 |
|------|------|---------|
| **Hit Rate** | 检索结果中包含正确文档的比例 | 检索阶段评估 |
| **MRR** | 正确文档在检索结果中的排名倒数均值 | 检索排序质量 |
| **NDCG** | 考虑位置的排序质量指标 | 检索排序质量 |
| **BLEU** | 生成答案与参考答案的 n-gram 重合度 | 翻译/摘要场景 |
| **ROUGE** | 生成答案与参考答案的召回率 | 摘要场景 |
| **LLM-as-Judge** | 用强模型评判弱模型输出质量 | 通用评估 |

### 检索 vs 生成 分层评估

```
┌─────────────────────────────────────┐
│         端到端评估（整体效果）         │
│  Answer Relevancy / 用户满意度       │
├─────────────────┬───────────────────┤
│   检索层评估     │    生成层评估       │
│ Context Recall  │  Faithfulness     │
│ Context Prec.   │  Answer Relevancy │
│ Hit Rate / MRR  │  BLEU / ROUGE     │
├─────────────────┴───────────────────┤
│         组件级评估（定位瓶颈）         │
│  Chunk 策略 / Embedding / ReRank    │
│  Prompt 模板 / LLM 选择             │
└─────────────────────────────────────┘
```

**定位瓶颈的思路**：如果 Context Recall 低 → 检索召回不足，优化 Chunk/Embedding；如果 Faithfulness 低 → 生成幻觉，优化 Prompt 或换模型；如果 Context Recall 高但 Answer Relevancy 低 → Prompt 问题。

---

## 面试高频

### RAG 评估需要标注数据吗？

RAGAS 的 Faithfulness 和 Context Precision **不需要人工标注**，用 LLM 自动评估；但 Context Recall 和 Answer Relevancy 最好有参考答案（Ground Truth）。实际中可以用 LLM 生成伪标注 + 人工抽检的方式降低成本。

### 如何做 A/B 实验对比两个 RAG 方案？

| 步骤 | 说明 |
|------|------|
| 1. 固定测试集 | 准备 50~200 条 Q-A 对 |
| 2. 控制变量 | 每次只改一个组件（如 Embedding 模型） |
| 3. 跑评估 | 用 RAGAS 四指标 + Hit Rate |
| 4. 统计显著性 | 同一测试集上对比分数差异 |

### 评估发现 Faithfulness 低怎么办？

| 可能原因 | 优化方向 |
|---------|---------|
| Prompt 未强调"仅根据检索内容回答" | 加防幻觉指令 |
| 检索内容与问题不相关 | 优化检索（换 Embedding / 加 ReRank） |
| LLM 参数化记忆过强（"太聪明"） | 降 temperature / 换更听话的模型 |
| 检索内容太短，信息不足 | 增大 chunk_size / 返回更多 chunk |

---

## 一句话总结

> RAG 评估核心框架是 RAGAS 的四大指标：Faithfulness（防幻觉）、Answer Relevancy（切题）、Context Precision（检索精确度）、Context Recall（检索召回率），分检索层和生成层分层评估以定位瓶颈，无需全部人工标注可用 LLM 自动评估。
