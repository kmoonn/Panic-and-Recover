# 面试深挖笔记 · 中国能建智能检索与问答平台（RAG，项目经历）· 索引/总览

> 用途：项目深挖轮的"抗追问"备稿**索引页**——承载项目级共享内容（定位/边界/STAR/主流程/选型攻防/分层陷阱/总结卡/主代码对照表）。
> 使用方式：先背 §1【60 秒总述】站稳开场 + §1.5 主流程；被问到某个技术点时翻对应**分点笔记**（见 §4 导航）逐层深挖。
> ⚠️ **技术栈现实（务必自己心里有数）**：最初落地版本是 **Java（Spring Boot 3.5.7，代号 Tiny-Ragent，用 SiliconFlow/BaiLian 云 API）**；简历写的 **Python + FastAPI + LangGraph + LangChain + Qdrant + 本地 Ollama** 版（代号 Nidus）是**重写版，核心问答链路已跑通、能本地演示**。Python 版全走**本地模型**，反而比 Java 版更契合"国企数据不出外网"——这是自洽加分点。口径：**"生产最初是 Java 云 API 版；我按数据不出网的要求重写了 Python 本地模型版，核心链路已跑通可演示。"** 别把"已跑通可演示"吹成"已在生产大规模上线"——被问部署规模就诚实降级。asyncio/FastAPI 内部实现必须能讲（见分点 [`rag-retrieval`](./01-rag-retrieval.md) / [`rag-model-routing`](./02-rag-model-routing.md)）。
> ⚠️ 真实性纪律：简历保留的量化数字（召回率 +30%、故障注入成功率 95%）均为**体感/自测口径**，深挖时主动降级为"体感估算、非严格对照实验"；旧口径里的 62%→87%、接入成本 -50% 不再作为简历主打。
> ⚠️ 核心机制（LangGraph 问答控制流、多路检索、模型路由、三态熔断、全链路追踪）**基本是我独立设计实现/编排的**，可放心讲；配置化摄入流水线保留为深挖备选，不再放在简历主线里。校企联培里合作方主要在需求和数据侧，工程核心归我。

---

## 0. 项目一句话定位

一个**面向企业内部知识库的私有化 RAG 检索问答平台**：把用户自然语言问题经**意图识别 → 多路并行检索（意图定向 + 向量全局）→ 去重 + Rerank → 模型路由（带三态熔断/流式首包探测的多模型高容错）→ 流式回答**这条链路打通，并配套**可编排的文档摄入流水线**和**异步全链路追踪**用于观测与排障。

**一句话电梯陈述**：
> "这是一个校企联培做的**企业私有化知识库问答平台**。我负责三块核心：一是**多路并行检索引擎**，用可扩展的 Channel + PostProcessor 框架把意图定向检索和向量全局检索并行跑再融合；二是**模型路由与高容错**，用三态熔断器 + 流式首包探测在多个大模型间自动故障切换；三是**异步全链路追踪**，解决线程池和流式回调场景下链路断裂、排障难的问题。"

**关联项目（顺势带出）**
- 呼应简历"专业技能 · AI"里的 **RAG / Prompt 工程 / Agent** 技术储备。
- 和字节 **API Testing Agent** 里"业务知识库 RAG"呼应——都是把文档切块检索补充上下文。

---

## 0.5 我的真实工作量 & 边界（诚实划界，反而可信）⭐

| 我做了什么 | 说明 | 分点笔记 |
|---|---|---|
| **多路并行检索引擎架构** ⭐⭐ | SearchChannel（意图定向 / 向量全局）+ PostProcessor（去重 / Rerank）可扩展框架，多级并行调度——独立设计实现 | [`rag-retrieval`](./01-rag-retrieval.md) |
| **模型路由 + 三态熔断高容错** ⭐⭐ | Chat/Embedding/Rerank 统一路由层，候选优先级 + 三态熔断 + 失败阈值 + 流式首包探测——独立设计实现 | [`rag-model-routing`](./02-rag-model-routing.md) |
| **异步全链路追踪** ⭐ | TraceRun + TraceNode 建树，跨线程池/流式回调透传上下文，TTFT 埋点——独立设计实现 | [`rag-tracing`](./03-rag-tracing.md) |
| **LangGraph agentic 控制流** ⭐⭐ | 查询级状态图（检索→评分→重写→自检），调度检索/生成/追踪链路——我编排、不揽框架功 | [`rag-langgraph`](./05-rag-langgraph.md) |
| **配置化文档摄入流水线**（备选） | PipelineDefinition 把 解析/分块/增强/索引 抽象为可编排节点，支持条件执行；不再作为简历主打 | [`rag-ingestion`](./04-rag-ingestion.md) |
| **Java → Python 重写（Nidus）** | 简历技术栈版本（FastAPI + LangChain + Qdrant + 本地 Ollama）；**核心问答链路已跑通、能本地演示**，部署规模不吹 | — |

> **边界诚实划界**：这是校企联培课题，合作方/老师主要在**需求界定和知识库数据**侧；上面几块**工程核心机制基本是我独立设计实现的**，可以放心 own。被问"团队几个人、你负责哪块"就照这个讲——不揽数据/需求侧的功，但工程内核确实是我的。

**定位金句**：
> "这个项目我最想讲的不是'搭了个 RAG'，而是**三个'生产级'的工程细节**：检索不是单路而是**多路并行 + 后处理链**；模型不是单点而是**带熔断和首包探测的多模型路由**——保证坏模型的字节根本不会吐给用户；追踪不是简单打日志而是**能在线程池和流式回调里还原真实父子调用树**。"

---

## 1. 60 秒总述稿（STAR，开场必背）⭐⭐

**【背景 S】** 国企内部有大量制度/技术/项目文档，员工查资料靠人工翻找，效率低；又因**数据不能出外网**，不能用云端 RAG 服务。需要一个**私有化、全本地模型**的知识库问答平台，把自然语言问题变成可靠、可溯源的答案。

**【任务 T】** 我负责平台的**问答链路工程内核**：用 LangGraph 把检索、召回评估、Query 重写、答案生成与自检串起来，让检索又准又全、让多模型在故障下仍稳定出字、让异步链路可观测排障。

**【行动 A】** 三条简历主线：
① **LangGraph + 多路并行检索**——检索→评分→重写→生成→自检；SearchChannel + PostProcessor 框架，意图定向 + 向量全局**并行**跑（`asyncio.gather`），去重→Rerank 融合；
② **模型路由 + 三态熔断**——统一路由层 + CLOSED/OPEN/HALF_OPEN 熔断 + **流式首包探测**，坏模型字节永不吐给用户；
③ **异步全链路追踪**——TraceRun/TraceNode 隐式栈建树，跨线程池/流式回调透传，两种 TTFT 埋点。

**【结果 R】** Python 重写版**核心问答链路已跑通、能本地演示**（生产最初为 Java 云 API 版）；复杂自然语言场景下**召回率 +30%**、故障注入下**请求成功率 95%**（均为体感/自测口径，见各分点红线）。

---

## 1.5 端到端主流程（"讲下架构 / 一次问答怎么走"时用）⭐

```
[前端 React 18 · SSE 聊天页]
        │ 用户问题
        ▼
[FastAPI SSE 入口]
        ▼
[LangGraph 状态图]  ← 查询级 agentic 控制流（⑤）
   retrieve ─→ grade ─(召回差)→ rewrite ─┐
      ↑                                    │
      └────────────────────────────────────┘
   grade ─(召回好)→ generate ─→ self_check ─(疑似幻觉)→ 回 generate/rewrite
        │
        ├─ retrieve 节点 → 多路并行检索引擎（①，asyncio.gather 并行 → 去重 → Rerank）
        ├─ generate 节点 → 模型路由 + 三态熔断 + 流式首包探测（②）
        └─ 全程被 异步全链路追踪 包住（③，contextvars 建树 + TTFT）
        ▼
[流式回答 → SSE 推回前端]

[离线独立路径] 文档 → 配置化摄入流水线（④，fetch→parse→chunk(embed)→index 写 Qdrant）
```

**讲解话术（约 40 秒）**：
> "一次问答从 FastAPI 的 SSE 入口进来，交给 **LangGraph 状态图**做控制流：先 `retrieve` 检索、`grade` 评召回质量，不好就 `rewrite` 重写 query 回环，够好才 `generate`、再 `self_check` 自检幻觉。关键是——**LangGraph 只当调度者**，`retrieve` 节点内部调的是我自研的**多路并行检索引擎**、`generate` 调的是**模型路由 + 熔断层**、全程被我的**全链路追踪**包住。文档摄入是**离线独立路径**，走**配置化流水线**写进 Qdrant，但这是备选深挖，不是简历主打。核心就是：**agentic 控制流兜质量，检索/路由/追踪三条工程主线兜稳定性**。"

---

## 2. 技术选型攻防（选型题高频，重点背）⭐⭐

> 面试官对 RAG / 基建项目最爱问"为什么选 X 不选 Y"。这一节把每个选型的**真实理由 + 追问预案 + 不能乱说的坑**固化下来。

### 2.1 向量数据库：为什么选 Qdrant（Docker 自部署）⭐⭐

**💡 大白话（自己理解用，不用背）**
> 国企数据不能出外网是**硬前提**，但这个前提 Milvus/Weaviate/PgVector 全都满足（都能私有化 Docker 部署），**不是 Qdrant 独有的优势**。真正选 Qdrant 的理由是"**在都能内网部署的前提下，哪个运维最省**"——Qdrant 单容器就能跑，Milvus 要拖 etcd + MinIO 一堆组件。

**❌ 千万别这么答（会被当场戳穿）**
> "因为国企数据不能出外网，所以选 Qdrant。"
> → 面试官一句"Milvus 不也能私有化内网部署吗？"就把你顶回来了。数据不出网是所有开源向量库的**共性**，不能当作选 Qdrant 的差异化理由。

**✅ 打磨版答法（背这套）**
> "数据不出外网是国企场景的**硬性前提**，但这个前提 Milvus、Weaviate、PgVector 都能满足，所以它不是我选型的决定因素。在'都能内网私有化部署'的前提下，我从三点选了 Qdrant：
> ① **运维成本**——这是核心。Qdrant 是单个 Rust 二进制，一个 Docker 容器就能起；Milvus 哪怕是 standalone 也要额外依赖 etcd 做元数据、MinIO 做对象存储，组件多、内网运维和排障成本明显更高。项目是个人/小团队维护，轻量部署很关键。
> ② **数据规模匹配**——项目的向量量级没到需要 Milvus 分布式、十亿级向量的程度，Qdrant 的性能和它的 payload filter（带元数据过滤的向量检索）完全覆盖需求。
> ③ **生态集成**——LangChain 官方有成熟的 Qdrant VectorStore，`qdrant-client` 接入简单。
> 所以结论是：**数据不出网是前提（大家都满足），在这个前提下按运维成本和实际数据规模，单容器的 Qdrant 比重组件的 Milvus 更划算。**"

**追问预案**
- **Q：那数据规模大了、要上亿向量怎么办？** → "Qdrant 本身也支持分布式分片和 replica，能水平扩；如果真到需要强一致分布式、超大规模的量级，再评估迁移 Milvus。当前规模下没必要为用不到的扩展性付运维代价——不过早优化。"
- **Q：Qdrant 和 Milvus 的检索原理差别？** → "两者向量索引核心都是 **HNSW（分层可导航小世界图）**，召回原理一致。差别更多在工程形态：Qdrant 单体、内置存储；Milvus 存算分离、组件化。所以召回质量层面差异不大，主要差在部署运维。"
- **Q：为什么不用 PgVector？你们本来就有 PostgreSQL。** → "PgVector 适合数据量小、想省一个组件的场景。但它是 PG 的扩展，**向量检索和业务事务抢同一个库的资源**，且大规模 ANN 性能和过滤能力不如专用向量库。我们向量检索是核心链路、数据量和过滤需求都有要求，所以用专用的 Qdrant，PG 只存业务元数据。"
- **Q：距离度量选的什么？** → "COSINE（余弦相似度），向量维度 1024（对齐 embedding 模型 bge-m3 的输出维度）。"

**⭐ 加分锚点**：主动点破"数据不出网是共性前提、不是 Qdrant 优势"这一点，反而显得选型思考清醒；再补一句"不为用不到的扩展性付运维代价"体现工程判断。

### 2.2 其它选型速记（被问到再展开）

| 选型 | 一句话理由 | 别踩的坑 |
|---|---|---|
| **后端 FastAPI** | 异步原生（RAG 全是 IO 密集的检索/模型调用）、SSE 流式返回友好、Pydantic 契约清晰 | 别说"因为快"——要落到 async/SSE/类型契约 |
| **Agent 编排 LangGraph** | agentic RAG 需要有状态、可分支、可回路的控制流（检索→评分→重写→再检索→自检），比 Agent Executor/纯 Chain 更好测试、观测、复现、调试 | **只用在查询级控制流这一层**；检索/路由/追踪/摄入的内部机制**不是** LangGraph 做的，别混为一谈——见 §3 陷阱 |
| **Embedding bge-m3（本地）** | 数据不出外网 → 不能用云 API；bge-m3 中英双语强、开源可本地部署，Ollama 一行拉起，**维度 1024** | 维度是 **1024** 不是 1536（1536 是 OpenAI 口径）；别答成"用了 OpenAI embedding" |
| **Rerank bge-reranker-v2-m3（本地）** | 检索后处理的 Rerank 需要；与 bge-m3 同源、本地部署 | 说清 Rerank 在**去重之后**跑（order=10，见 [`rag-retrieval`](./01-rag-retrieval.md)） |
| **Chat 模型 Qwen2.5（本地多实例）** | 本地起 7b + 3b 两个实例，**才能演示路由/熔断/故障切换** | 单模型无法演示 failover；故障注入可再配一个指向坏端口的 provider |
| **LangChain** | 检索器、VectorStore、文本分块器等组件成熟，不重造轮子；但**核心编排（多路检索、模型路由）是自己写的，没直接用 LangChain 的 fallback** | 别把整个项目说成"LangChain 拼的"——路由/熔断/追踪都是自研 |
| **Redis** | **落地了**：会话/多轮记忆缓存、模型熔断状态共享（多实例时）、分布式限流、幂等键 | 用到就要答得出用在哪——见 [`rag-model-routing`](./02-rag-model-routing.md) 熔断状态 + 会话记忆 |
| **PostgreSQL** | 业务元数据、追踪记录（TraceRun/TraceNode）、会话持久化 | 向量**不**存 PG，存 Qdrant（见 2.1） |
| **前端 React 18** | 极简聊天页，演示 SSE 流式回答 + 链路追踪可视化 | 只做最小页面，别吹前端工程量；重点在后端 |

> ⚠️ **技术栈已砍 RocketMQ**：简历三条主线无一依赖 MQ；摄入的"异步执行"用 FastAPI `BackgroundTasks` 即可。写了不用会被追问"你 RocketMQ 怎么用的"而露馅——真实性红线。若被问"为何摄入不用 MQ"：**"当前规模用 BackgroundTasks 足够，MQ 是流量上来后的演进项，不为演示上不需要的组件。"**

---

## 3. LangGraph 分层架构 & 两个必答陷阱 ⭐⭐

> LangGraph 的定位必须讲清楚，否则会和追踪（[`rag-tracing`](./03-rag-tracing.md)）、摄入流水线（[`rag-ingestion`](./04-rag-ingestion.md)）**语义打架**，反而把自己的活说成"调框架"。分点笔记 [`rag-langgraph`](./05-rag-langgraph.md) 的深挖以本节分层为前提。

**💡 大白话（自己理解用）**
> LangGraph 当"**大脑/控制流**"（问答时决定：先检索→评召回好不好→不好就重写 query 再检索→回答后自检有没有幻觉）；简历主打的三块工程能力是**多路检索、模型路由、全链路追踪**，摄入流水线作为备选深挖。LangGraph 在上层调度，**每块的核心机制仍然是我写的**。

**分层架构（画图/讲架构时用）**
```
[FastAPI SSE 入口]
        ↓
[LangGraph 状态图]  ← 查询级 agentic 控制流（技术点⑤）
   retrieve ─→ grade ─(召回差)→ rewrite ─┐
      ↑                                    │
      └────────────────────────────────────┘
   grade ─(召回好)→ generate ─→ self_check ─(疑似幻觉)→ 回 generate/rewrite
        ↓
[自研引擎层，LangGraph 只当节点调用它，不感知内部]
   retrieve 节点 → 多路并行检索引擎           (技术点①)
   generate 节点 → 模型路由 + 三态熔断 + 首包探测 (技术点②)
   全程被 contextvars 追踪包住                 (技术点③)
[摄入：自研配置化流水线，离线独立路径]          (技术点④)
```

### ⚠️ 陷阱 1：追踪——别让 LangSmith 顶替全链路追踪
LangGraph 配 LangSmith 能"免费"给节点级追踪。面试官会问："**LangSmith 不就把链路追踪做了吗，你的 TraceRun/TraceNode 还有啥用？**"
**打磨版答法**：
> "LangSmith 是外部 SaaS，**内网私有化部署不能依赖它**；而且我要的是**自定义 TTFT 语义（用户可见首字节 vs 模型首包两种）、跨线程池/流式回调透传、以及落到自己 PG 表的自定义节点类型**——这些 LangSmith 给不了。好在 LangGraph 节点大多在同一 event loop 里跑，**contextvars 天然透传**，我的追踪能顺势把图节点也包住。"

**⭐ 加分锚点**：能主动说出"为什么不用 LangSmith"反而证明你懂追踪的本质诉求，不是无脑造轮子。

### ⚠️ 陷阱 2：摄入流水线——别和 LangGraph 撞车
LangGraph 本身就是"条件边 + 图执行引擎"。面试官会问："**LangGraph 不就是图引擎吗，你的 PipelineDefinition 编排引擎不是重复造轮子？**"
**打磨版答法**：
> "两者设计目标不同。LangGraph 的图是**代码里写死**的；我的摄入流水线是**数据/配置驱动**的——PipelineDefinition 以 JSON 存 DB、运行时可改、带条件 DSL，目标是让**非开发也能重排节点、新节点即插即用**。这是产品化的配置诉求，和'代码定义图'是两个层面的东西。所以摄入这条我坚持用自研的轻量引擎，不套 LangGraph。"

### ⚠️ 词要咬清：两个"routing"不是一回事
- **模型路由** = 选哪个 **LLM 供应商/实例**做故障切换（**infra 层**，见 [`rag-model-routing`](./02-rag-model-routing.md)）。
- LangGraph 的 **"tool routing"** = agent 决定调哪个**工具/检索器**（**应用层**）。
- 同名不同物，面试时别让面试官把两者混在一起追问。

---

## 4. 五个技术点 · 分点深挖导航 ⭐⭐

> 每个技术点一份独立深挖笔记（💡大白话 + ✅打磨版 + ⭐加分锚点 + 追问预案 + mini 总结卡）。被追某点时直接翻对应文件。

| # | 技术点 | 分点笔记 | 一句话 |
|---|---|---|---|
| ① | 多路并行检索引擎 | [`01-rag-retrieval.md`](./01-rag-retrieval.md) | SearchChannel + PostProcessor 框架，`asyncio.gather` 并行 + 去重→Rerank 融合 |
| ② | 模型路由 + 三态熔断 | [`02-rag-model-routing.md`](./02-rag-model-routing.md) | 三态熔断 + **流式首包探测**，坏模型字节永不吐给用户 |
| ③ | 异步全链路追踪 | [`03-rag-tracing.md`](./03-rag-tracing.md) | TraceRun/TraceNode 隐式栈建树，contextvars 跨线程池/流式透传，两种 TTFT |
| ④ | LangGraph agentic 控制流 | [`05-rag-langgraph.md`](./05-rag-langgraph.md) | 检索→评分→重写→自检回环，调度检索/生成/追踪链路——不揽框架功 |
| ⑤ | 配置化摄入流水线（备选） | [`04-rag-ingestion.md`](./04-rag-ingestion.md) | PipelineDefinition 配置驱动、条件执行，保留深挖但不再作为简历主打 |

---

## 5. 总结卡（一分钟收尾用）⭐⭐

| 维度 | 一句话 |
|---|---|
| 一句话定位 | 企业私有化知识库 RAG 问答平台，核心是**LangGraph 问答控制流 + 多路检索 + 多模型高容错 + 全链路追踪**三条简历主线 |
| LangGraph + 多路检索（①） | LangGraph 编排检索→评分→重写→生成→自检；SearchChannel + PostProcessor 可扩展框架，`asyncio.gather` 并行 + 阻塞调用丢线程池，去重(order=1)→Rerank(order=10) 融合 |
| 模型高容错（②） | 三态熔断（CLOSED/OPEN/HALF_OPEN）+ **流式首包探测**——坏模型的字节永不吐给用户，无感切下一个 |
| 全链路追踪（③） | TraceRun/TraceNode 隐式栈建树，contextvars 跨线程池/流式透传，区分 USER_TTFT 与 LLM_TTFT |
| 摄入流水线（备选） | PipelineDefinition 配置驱动、运行时可改、条件执行——保留深挖，非简历主打 |
| 我的边界 | 三条简历主线**基本独立设计实现/编排**；校企联培合作方在需求/数据侧；Python 重写版核心链路**已跑通可演示** |
| 测开视角 | LangGraph 状态图让 agentic 流程**可断言、可复现、能定位到具体节点和轮次** |
| **红线** | 数字（+30%、95%）全是**体感/自测口径**，被问主动降级；旧版 62%→87%、-50% 不再作为简历主打；真实生产最初是 Java 云 API 版，Python 本地版是重写、**可演示不吹已大规模上线**；LangSmith/OTel 不用是有理由的、不是无脑造轮子 |

---

## 6. 代码对照表（简历每句 ↔ 真实设计）· 单一事实来源

> **简历定稿（锁定版）**
> **中国能建智能检索与问答平台**（RAG 开发）· Python + **LangGraph** + LangChain + FastAPI + **PostgreSQL** + **Qdrant** + **Ollama**
> - 多路并行检索引擎架构：基于 LangGraph 编排检索、召回评估、Query 改写、答案生成与自检流程，设计 SearchChannel + PostProcessor 可扩展检索框架，基于 **asyncio 协程与独立线程池**实现多级并行调度，并结合去重、Rerank 优化召回结果，复杂自然语言场景下召回率提升 30%+。
> - 模型路由与高容错机制：统一封装 Chat/Embedding/Rerank 模型路由层，结合候选优先级、三态熔断器、失败阈值控制与流式首包探测实现故障自动切换，避免单一模型故障直接阻塞用户问答主链路，故障注入场景下核心问答请求成功率保持在 95% 左右。
> - 异步场景问题追踪定位：设计 TraceRun + TraceNode 追踪模型，通过装饰器埋点记录节点输入输出、耗时、状态与父子关系，并补充首 Token 耗时指标，解决问答链路中检索、重排、模型调用、流式返回等异步环节难以定位耗时与失败原因的问题，提升复杂链路问题定位效率。
>
> ⚠️ **简历栈行是精简版**（`…+ PostgreSQL + Qdrant + Ollama`），**省略了实际在用的 Redis + React 18**——被问"为何没写"答："栈行精简，**PG 存业务元数据/结构化数据，Redis 做会话记忆+熔断状态共享+限流+幂等，React 做最小聊天演示页**，只是没占简历版面"。
> 注：技术栈已**砍 RocketMQ**（摄入异步走 BackgroundTasks）；**LangGraph 已并入第一条检索主线**；**Milvus→Qdrant**。配置化摄入流水线保留为深挖备选，不再作为简历主打。

| 简历原文 | 落点 / 讲法 | 分点笔记 |
|---|---|---|
| `LangGraph` | 查询级 agentic 控制流：检索→评分→重写→生成→自检；别和自研的检索/追踪/摄入混为一谈 | [`rag-langgraph`](./05-rag-langgraph.md) |
| `asyncio 协程与线程池` | 原 Java 是 `CompletableFuture`；Python 版 IO 密集走 asyncio，阻塞型 SDK 调用用 `run_in_executor` 丢线程池 | [`rag-retrieval`](./01-rag-retrieval.md) |
| `SearchChannel + PostProcessor` | 多路检索框架，语言中立类名 | [`rag-retrieval`](./01-rag-retrieval.md) |
| `Embedding/Rerank`（本地 bge-m3 / bge-reranker-v2-m3，维度 1024） | §2.2；数据不出外网走本地，别答成云 API、别说 1536 | [`rag-ingestion`](./04-rag-ingestion.md) |
| `三态熔断器 + 流式首包探测` | 模型路由，坏模型字节永不吐给用户是最大亮点 | [`rag-model-routing`](./02-rag-model-routing.md) |
| `TraceRun + TraceNode + @RagTraceNode + TTFT` | 追踪，Python 用 contextvars；别用 LangSmith 顶替（§3 陷阱1） | [`rag-tracing`](./03-rag-tracing.md) |
| `PipelineDefinition + 条件执行` | 摄入流水线；保留深挖备选，区别于 LangGraph 代码定义图（§3 陷阱2） | [`rag-ingestion`](./04-rag-ingestion.md) |
| `Qdrant` | §2.1 选型攻防，别把"不出网"当 Qdrant 的独有优势 | — |
| `Redis` | 会话/多轮记忆缓存 + 熔断状态共享 + 限流 + 幂等；被问答得出具体用途（§2.2） | [`rag-model-routing`](./02-rag-model-routing.md) |

---

## 12. TODO / 待确认（面试前敲定，低风险占位）

> 各技术点的细粒度 🔲（topK、幂等键等）已下沉到对应分点笔记的「TODO（本点）」。这里只留**项目级**待确认。

- [ ] Qdrant 实际部署形态（单容器 / 是否配 replica）、数据规模量级（文档数 / 向量数），准备被问部署时的诚实回答。
- [ ] `+30% / 95%` 的口径来源与样本量（怎么测的），降级话术已在 §5 红线备好；旧口径 `62%→87% / -50%` 只作为历史备选，不主动讲。
- [ ] "校企联培课题"的角色边界：合作方/老师具体 own 哪几块（需求/数据侧），面试前对齐一句话。
- [ ] Python 重写进度：投递时能说到"核心链路已跑通可演示"，被追问再诚实对齐当时状态。
