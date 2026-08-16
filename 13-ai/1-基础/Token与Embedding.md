---
tags:
  - AI
  - LLM
  - Token
  - Embedding
category: AI/基础
---

# Token与Embedding

> Token 是模型处理的最小单位，Embedding 是文本的语义向量表示——两者分别是 LLM 的"输入粒度"和"检索基石"。

## Token

- **Token**：模型处理的最小单位，不是字也不是词，是**子词（subword）**。中文一个字≈1~2 token，英文一个词可能拆成多个 token。计费、限长都按 token。

## 上下文窗口（Context Window）

- 一次能"看到"的最大 token 数（输入+输出共享）。超了要截断或靠 RAG/摘要补。

## Embedding（向量化）

- 把文本映射成一串定长浮点数（向量），**语义相近的文本向量距离近**。是 RAG 检索的基础。

## ⭐ 加分/易错

- Embedding 的**维度**由模型决定（如某模型 1024 维），全库必须用**同一个 embedding 模型**，换模型要全量重建索引——这点在 RAG 项目 `ceec/04-rag-ingestion.md` 摄入流水线里是硬约束。

## 一句话总结

> Token 是子词级的最小处理单位，上下文窗口决定一次能看多少 token，Embedding 把文本变成语义向量——换 embedding 模型必须全量重建索引。
