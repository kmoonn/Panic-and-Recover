# 面试深挖笔记 · 中国能建 RAG · ① 多路并行检索引擎（bullet1）

> 用途：RAG 项目「多路并行检索」这一技术点的逐层深挖备稿。
> 使用方式：先看 §0 定位站稳，再背 ✅打磨版答法，吃透 ⭐加分锚点与追问预案。
> 📎 项目级背景（定位/选型攻防/STAR/端到端主流程/分层陷阱/主代码对照表）见索引 [`00-rag-platform.md`](./00-rag-platform.md)。
> ⚠️ 真实性纪律：`召回率 +30%` 为**体感/自测口径**，被问主动降级为"体感估算、非严格对照实验"。Java 版是原型，Python 版核心链路已跑通，讲的时候用「引擎/框架」这类语言中立词，asyncio 细节要能落地。

---

## 0. 一句话定位

一个问题进来**不只查一路**：**意图定向检索**（识别问题类别、去对应知识域精准捞）和**向量全局检索**（全库语义兜底）**并行**跑，跑完**先去重、再 Rerank** 融合成一份。用 **SearchChannel（一路检索源）+ PostProcessor（结果后处理）** 抽象成可扩展框架，新检索源即插即用。

## 0.5 我的边界

**独立设计实现**：SearchChannel / PostProcessor 抽象、`MultiChannelRetrievalEngine` 两阶段并行调度、去重与 Rerank 后处理链都是我写的；Rerank 复用本地 bge-reranker-v2-m3 模型（选型见索引 §选型）。

---

## 深挖 Q&A ⭐⭐

**💡 大白话（自己理解用，不用背）**
> 一个问题进来，我不只查一路。**意图定向检索**（先识别问题属于哪类，去对应知识域精准捞）和**向量全局检索**（全库语义兜底）**并行**跑，跑完把两路结果**先去重、再 Rerank** 融合成一份。好处：定向的准、全局的全，两条腿走路，还能随时插新的检索通道。

**✅ 打磨版答法（背这套）**
> "检索层我抽象成两个可扩展点：**SearchChannel**（一路检索源）和 **PostProcessor**（结果后处理）。
> - Channel 有两类：**意图定向 Channel**（priority=1，先做意图识别，命中就去对应知识域精准检索）和**向量全局 Channel**（priority=10，全库向量兜底）。它们实现同一个抽象接口，`MultiChannelRetrievalEngine` 把所有 channel **并行**发起——Java 版是 `CompletableFuture.allOf`，**Python 版是 `asyncio.gather` 把每个 channel 包成协程并发**，阻塞型的 SDK 调用再 `run_in_executor` 丢线程池，避免卡住 event loop。
> - 两阶段：**先并行跑完所有 channel，再顺序跑 PostProcessor 链**。后处理链按 `order` 排：**去重（order=1）→ Rerank（order=10）**，去重先做能减少 Rerank 的计算量。
> - 向量全局不是无脑全开，有**兜底触发条件**：意图为空、或定向结果 maxScore<0.6、或单一意图置信<0.8 时才补全局检索，省算力。
> 这套框架的价值是**新检索源即插即用**——加一路（比如全文检索 BM25）只要实现 Channel 接口、注册进去就行，不动主链路。"

**⭐ 加分锚点**：`asyncio.gather` 并发 + `run_in_executor` 处理阻塞 SDK 这句要主动说，证明你懂"IO 密集为什么用协程、CPU/阻塞调用为什么要丢线程池"；再补"去重在 Rerank 前"体现顺序是有工程考量的。

**追问预案**
- **Q：两路结果分数不在一个量纲，怎么融合排序？** → "正因为向量 cos 分和定向检索分不可直接比，我才在后处理链里放 **Rerank（bge-reranker-v2-m3）统一重排**——用 query+doc 的交叉打分给一个可比的相关性分，而不是硬拼两路的原始分。融合前的去重按 doc 唯一标识（来源+chunk id）做。"
- **Q：`asyncio.gather` 里一路检索超时或抛异常，整个挂了吗？** → "不会。`gather` 我带 `return_exceptions=True`，单路失败降级成空结果、记进 trace，其它路正常融合；只要还有一路有结果就能回答。这也呼应模型路由的高容错思路——**任何单点都不该阻塞主链路**。"
- **Q：为什么不直接一路向量检索就好，非要多路？** → "纯向量检索在**复杂/多意图问题**上召回不稳：语义相近但答非所问的 chunk 会挤进 topK。意图定向能把检索范围先收敛到对的知识域，召回率明显更稳——简历里'复杂自然语言场景召回率 +30%'主要来自这里（体感/自测口径）。"
- **Q：topK 取多少、Rerank 前后各留几条？** → "🔲 按实核对（Java 版记得是各路 topK≈10、融合去重后送 Rerank、Rerank 后取 top3~5 进上下文）；这个数是可调参数，面试前我对齐真实配置。"

---

## 总结卡（本点）

| 维度 | 一句话 |
|---|---|
| 机制 | SearchChannel + PostProcessor 可扩展框架，`asyncio.gather` 并行 + 阻塞调用丢线程池 |
| 后处理链 | 去重(order=1) → Rerank(order=10)，去重先做减少 Rerank 计算量 |
| 兜底触发 | 意图为空 / maxScore<0.6 / 单意图<0.8 才补向量全局检索 |
| 容错 | `gather(return_exceptions=True)`，单路失败降级、不阻塞主链路 |
| **红线** | `召回率 +30%` 是**体感/自测口径**，被问主动降级；topK/保留条数按实核对 |

## 代码对照（本点）

| 简历原文 | 落点 / 讲法 |
|---|---|
| `SearchChannel + PostProcessor` | 本文多路检索框架，语言中立类名 |
| `asyncio 协程与线程池` | 原 Java 是 `CompletableFuture`；Python 版 IO 密集走 asyncio，阻塞型 SDK 调用用 `run_in_executor` 丢线程池 |
| `去重、Rerank 优化召回` | 后处理链去重(order=1)→Rerank(order=10)；Rerank 复用本地 bge-reranker-v2-m3 |

## TODO（本点）
- [ ] 各路检索 topK、Rerank 前后保留条数的**真实配置值**（现为 🔲 按实核对）。
