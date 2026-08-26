# 面试深挖笔记 · 中国能建 RAG · ③ 异步全链路追踪（bullet3）

> 用途：RAG 项目「异步场景全链路追踪」这一技术点的逐层深挖备稿。
> 使用方式：先看 §0 定位站稳，再背 ✅打磨版答法，吃透 ⭐加分锚点与追问预案。
> 📎 项目级背景（定位/选型攻防/STAR/端到端主流程/分层陷阱/主代码对照表）见索引 [`00-rag-platform.md`](./00-rag-platform.md)。
> ⚠️ 真实性纪律：为什么不用 LangSmith/OTel 是**有理由的选择**、不是无脑造轮子（见索引 §1.3 陷阱1）。追踪落库 PG（PG 用途见索引 §选型）。

---

## 0. 一句话定位

RAG 一次问答跨很多步（检索、Rerank、模型），还有**线程池**和**流式回调**——普通打日志会**断链**。我做的是把这些步骤**自动还原成一棵父子调用树**（TraceRun + TraceNode），并埋 **TTFT**，排障时能看清每次问答慢在哪、错在哪。

## 0.5 我的边界

**独立设计实现**：TraceRun/TraceNode 模型、隐式栈建树（Java 用 ThreadLocal + AOP / Python 用 contextvars）、跨线程池透传、流式 span 交接、两种 TTFT 语义都是我设计的。

---

## 深挖 Q&A ⭐⭐

**💡 大白话（自己理解用，不用背）**
> RAG 一次问答跨了好多步（检索、Rerank、模型），还有**线程池**和**流式回调**——普通打日志会**断链**：子线程里的日志不知道属于哪次请求，流式回调触发时主请求早返回了。我做的是**自动把这些步骤还原成一棵父子调用树**，并且埋了 **TTFT（首字节耗时）**，排障时能看清每次问答到底慢在哪、错在哪。

**✅ 打磨版答法（背这套）**
> "痛点是异步场景下链路会断：线程池里的子任务、流式的异步回调，都脱离了原始请求上下文。我设计了 **TraceRun（一次问答=一棵树）+ TraceNode（每一步=一个节点）** 模型：
> - **隐式建树**：不手动传 traceId，而是靠**上下文 + 调用栈**。Java 版用 ThreadLocal 存 `traceId/taskId/nodeStack`，`@RagTraceRoot`/`@RagTraceNode` 注解 + AOP 在方法进出时 push/pop 节点栈，栈顶就是当前节点的父节点——**父子关系天然建出来**。**Python 版用 `contextvars.ContextVar` 替代 ThreadLocal**，装饰器/上下文管理器建树。
> - **跨线程池透传**：ThreadLocal 不会自动传到线程池子线程，所以提交任务时**深拷贝**当前 nodeStack 带过去（`nodeStack.copy()`），防止并发串栈；Python 版对应 `copy_context()` 把上下文快照带进 executor。
> - **流式 span 交接**：流式回答时主方法先返回、字节在异步回调里陆续吐，我用 `StreamSpan.detach()` 把这个 span 从主栈摘下、**交给异步流去 close**，等流真正结束才记完耗时。
> - **两种 TTFT**：区分 **USER_TTFT（用户可见首字节）和 LLM_TTFT（模型自身首包）**——因为中间还隔着我的首包探测缓冲（bullet2），这两个值不一样，分开埋才能定位'慢在模型还是慢在我的中间层'。
> 落库到 PG 的 TraceRun/TraceNode 表，前端能可视化成调用树。"

**⭐ 加分锚点**：三个点任一都能加分——① "深拷贝 nodeStack 防并发串栈"体现你踩过并发的坑；② "两种 TTFT 分开埋"体现你懂流式性能归因；③ "为什么不用 LangSmith"（见索引 §1.3 陷阱1）体现你懂追踪本质诉求。

**追问预案**
- **Q：为什么不直接上 OpenTelemetry / Jaeger？** → "OTel 是通用分布式追踪、偏 span 打点和跨服务传播，我这里是**单体内的 RAG 语义追踪**，要的是**领域化的节点类型（检索/Rerank/模型/摄入）+ 自定义 TTFT 语义 + 落自己 PG 表给业务前端看**，用 OTel 反而要写一堆适配。规模真大了、要跨服务了，我会把这套 TraceNode 往 OTel span 上映射，而不是一开始就背通用框架的复杂度。"
- **Q：contextvars 在 `asyncio.gather` 的并发协程里会互相污染吗？** → "不会。每个 task 在创建时会**拷贝**创建时刻的 context，协程之间的 ContextVar 修改互不可见——这正是它比 ThreadLocal 更适合 asyncio 的原因。要跨 `run_in_executor` 到线程池才需要手动 `copy_context()` 带过去。"
- **Q：追踪本身有性能开销吗？** → "节点 push/pop 是内存操作，开销极小；重的是落库，所以 trace 写 PG 是**异步批量**的，不阻塞问答主链路。高频场景可加采样。"

---

## 总结卡（本点）

| 维度 | 一句话 |
|---|---|
| 模型 | TraceRun（一次问答=树）+ TraceNode（每步=节点），落 PG 可视化调用树 |
| 隐式建树 | Java: ThreadLocal + AOP push/pop 栈；Python: contextvars 装饰器/上下文管理器 |
| 跨线程池 | `nodeStack.copy()` 深拷贝防并发串栈 / `copy_context()` 带进 executor |
| 流式交接 | `StreamSpan.detach()` 把 span 交给异步流去 close |
| 两种 TTFT | USER_TTFT（用户可见首字节）vs LLM_TTFT（模型自身首包），分开埋定位慢在哪 |
| **红线** | 不用 LangSmith/OTel 是有理由的选择（私有化+领域语义），不是无脑造轮子 |

## 代码对照（本点）

| 简历原文 | 落点 / 讲法 |
|---|---|
| `TraceRun + TraceNode + @RagTraceNode` | 本文追踪模型，Python 用 contextvars 替代 ThreadLocal |
| `异步上下文透传 + 父子节点建树` | 隐式栈建树 + 跨线程池 `copy_context()` + 流式 `StreamSpan.detach()` |
| `TTFT 埋点` | 区分 USER_TTFT 与 LLM_TTFT；别用 LangSmith 顶替（索引 §1.3 陷阱1） |
