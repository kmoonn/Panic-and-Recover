# 秋招备战索引 · 测开方向

> 目标：一个月内吃透简历每一段，做到"抗追问"。主投测试开发。
> 用法：每段实习经历一份深挖笔记；**项目经历改为"索引/总览 + 每个技术点一份分点笔记"**（索引承载 STAR/主流程/业务/数字/主代码对照表，分点笔记逐层深挖单个技术点）。结构统一 = 60 秒总述 + 逐层深挖 Q&A（大白话💡 + 打磨版答法 + ⭐加分点）+ 总结卡 + 代码对照表 + TODO。

---

## 一、笔记封板进度

| 模块 | 项目 | 笔记 | 状态 |
|---|---|---|---|
| 字节实习 | 实习背景（SDT + AI Testing 工具链索引） | [00-context.md](bytedance/00-context.md) | ✅ 封板（SDT=Spec-Driven Testing，落地=那三个项目） |
| 字节实习 | AI Data Gen（造数 Skill） | [01-ai-data-gen.md](bytedance/01-ai-data-gen.md) | ✅ 封板 |
| 字节实习 | Skills Pulse（运行时遥测） | [02-skills-pulse.md](bytedance/02-skills-pulse.md) | ✅ 封板 |
| 字节实习 | API Testing Agent | [03-api-testing-agent.md](bytedance/03-api-testing-agent.md) | ✅ 封板 |
| 快手实习 | 效能星盘（质效分析平台） | [01-xingpan.md](kuaishou/01-xingpan.md) | ✅ 封板 |
| 快手实习 | 星诉（客诉智能诊断） | [02-xingsu.md](kuaishou/02-xingsu.md) | ✅ 封板 |
| 快手实习 | 快聊移动端 UI 自动化巡检 | [03-kuaichat-ui.md](kuaishou/03-kuaichat-ui.md) | ✅ 封板 |
| 科大讯飞实习 | 作文答题卡自动渲染工具 | [01-essay-sheet-render.md](iflytek/01-essay-sheet-render.md) | ✅ 封板 |
| 科大讯飞实习 | 批阅机现网数据回流工具 | [02-data-backflow.md](iflytek/02-data-backflow.md) | ✅ 封板 |
| 科大讯飞实习 | 接口自动化测试场景建设 | [03-api-automation.md](iflytek/03-api-automation.md) | ✅ 封板 |
| 项目经历 | 中国能建 RAG · 索引/总览 | [00-rag-platform.md](ceec/00-rag-platform.md) | ✅ 封板 |
| 　└ 分点① | 多路并行检索 | [01-rag-retrieval.md](ceec/01-rag-retrieval.md) | ✅ 封板 |
| 　└ 分点② | 模型路由 + 三态熔断 | [02-rag-model-routing.md](ceec/02-rag-model-routing.md) | ✅ 封板 |
| 　└ 分点③ | 异步全链路追踪 | [03-rag-tracing.md](ceec/03-rag-tracing.md) | ✅ 封板 |
| 　└ 分点④ | 配置化摄入流水线 | [04-rag-ingestion.md](ceec/04-rag-ingestion.md) | ✅ 封板 |
| 　└ 分点⑤ | LangGraph 控制流 | [05-rag-langgraph.md](ceec/05-rag-langgraph.md) | ✅ 封板 |
| 项目经历 | 武理工学科竞赛系统 · 索引/总览 | [00-contest-system.md](whut/00-contest-system.md) | ✅ 封板 |
| 　└ 分点① | SSO 统一身份认证（Sa-Token） | [01-contest-sso.md](whut/01-contest-sso.md) | ✅ 封板 |
| 　└ 分点② | 动态数据权限（MyBatis 拦截器） | [02-contest-data-permission.md](whut/02-contest-data-permission.md) | ✅ 封板 |
| 　└ 分点③ | 高性能 Excel 读写（SAX 流式） | [03-contest-excel.md](whut/03-contest-excel.md) | ✅ 封板 |
| 教育背景 | 科研/竞赛/荣誉速查卡（国创/专利/软著/竞赛） | [00-background.md](education/00-background.md) | ✅ 封板（轻量速查卡；国创=竞赛系统，🔲 细节据实补） |
| 专业技能 | 八股速记（已整合至主知识库） | — | ✅ 已整合至各对应目录 |
| 　└ 设计模式 | 已整合至 14-design-pattern/ | — | ✅ 已整合 |

> 建议顺序：字节（✅）→ 快手（✅ 星盘/星诉/快聊）→ 讯飞（✅ 答题卡渲染/数据回流/接口自动化）→ 武理工学科竞赛（✅）→ 中国能建 RAG（✅）→ 八股（✅ 语言+测试/网络/操作系统/数据库/框架/中间件/AI/设计模式 八大主题均已封板）。

---

## 二、简历定稿 bullet 汇总（改动以此为准）

> 注：`.tex` 源文件不在本仓库（外部 moderncv 环境），仓库根 `resume.tex` 为**用户提供的权威副本**；此处为各笔记锁定版原文，供快速回贴与比对。

### 教育背景 · 武汉理工大学（速查卡，教育背景权重低）

- **可追问项**：国家级大学生创新创业训练计划项目负责人、受理发明专利 1 项、软著 3 项、中国软件杯全国二等奖、三创赛省级一等奖。✅ **国创项目 = 学科竞赛管理系统**（即简历项目经历那个，技术走 [`whut`](whut/00-contest-system.md)）；轻量速查卡 + 口径纪律（受理≠授权/省级≠国家级）见 [`education/00-background.md`](education/00-background.md)，🔲 专利/软著/竞赛作品的一句话主题据实补。

### 字节跳动 · 测试开发实习生

- **实习背景（SDT + AI Testing 工具链）**：TikTok One 三方创意质量保障，两条主线——后端 **SDT（Spec-Driven Testing，规格驱动测试）** + AI Testing 工具链。✅ **SDT 的落地就是下面三个项目**（AI Data Gen / Skills Pulse / API Testing Agent，三项目闭环：造数据→看数据→用数据测），API Testing Agent 即 AI 版 spec→用例→断言。详见 [`bytedance/00-context.md`](bytedance/00-context.md)。
- **AI Data Gen**：面向 AI Testing 场景沉淀 40+ 原子化造数 Skill，覆盖账号、合作、审核、投稿、达人 5 大核心业务模块，设计三级参数确定机制与流程编排能力，实现长链路复杂数据的自动化构造，累计服务 1000+ 人次，成功率 90%+，显著降低人工造数成本并提升测试准备效率。
- **Skills Pulse**：基于指令注入 + Bash 临时文件队列实现调用配对与异步上报，通过离线 JSONL 文件补发保障数据完整性，实现调用量、成功率、耗时、报错等核心指标可观测，适配 7+ 种主流 Coding Agent，设计 fallback 用户身份解析与匿名化哈希，兼顾可观测性与隐私合规性。
- **API Testing Agent**：设计并实现 PRD 与 MR 双驱动的渐进式 Checklist 生成机制，支持 HTML 标注评论与评审意见回流迭代，Agent 自主选择 Mock、日志检索、造数 Skill 三种数据生成方式补齐测试数据、编排用例、生成断言并自动重试，测试准备与执行耗时缩短约 40%。

### 快手 · 测试开发实习生（待深挖，简历原文）

- **效能星盘**：设计面向需求风险、QA 排期、团队效能、个人产出的 AI 质效分析工作流，打通数据查询、规则校验、AI 分析与结构化输出全链路；基于 Function Calling + Pydantic 实现稳定 JSON 输出，采用策略注册表 + Orchestrator 提升分析模块扩展性与维护效率。
- **星诉**：负责后端告警服务三级意图分流机制开发，实现规则应答、智能排查、人工干预的分层处理；基于 Redis 原子计数器实现 10 分钟同源异常聚合统计与 3 次阈值告警，打通参数校验、群组匹配、异常统计、Kim 群聊机器人推送全链路。
- **快聊移动端 UI 自动化巡检**：基于 KRunner 搭建核心链路 UI 自动化巡检体系，覆盖消息发送、营销卡片、潜客留资等关键场景，定位并推动修复机型兼容、弹窗层级等 4 类关键交互缺陷，单版本节省回归时间约 2-4 小时，回归效率提升约 30%。

### 科大讯飞 · 测试开发实习生（待深挖，简历原文）

- **作文答题卡自动渲染工具**：基于 PyPDF 和 ReportLab 设计并实现，接入星火 Spark 大模型实现语文、英语学科数智作业与自由作业场景的测试物料自动化生成，支持模拟多种手写笔迹，单班级造数从人工 8 小时+ 缩短至 5 分钟内。
- **批阅机现网数据回流工具**：针对线上业务故障缺少测试样本难以回归验证的痛点，实现线上批阅链路样本按需回流至测试环境，支持必要字段脱敏，支撑平台缺陷复测与批阅模型迭代，数据采集效率提升 70%+，全链路验证效率提升 90%。
- **接口自动化测试场景建设**：梳理智学网 APP 家长端学情报告、AI 错题本、AI 规划等 5 大核心业务共 100+ 接口，基于 JMeter 构建覆盖主流程与异常场景的 200+ 条冒烟与回归用例，并纳入版本发布前自动化验证流程，提升变更验证效率与回归覆盖度。

### 项目经历（待深挖，简历原文）

- **中国能建智能检索与问答平台**（RAG 开发 · Python + LangGraph + LangChain + FastAPI + PostgreSQL + Qdrant + Ollama）：基于 LangGraph 编排检索、召回评估、Query 改写、答案生成与自检流程，设计多路并行检索引擎（SearchChannel + PostProcessor，asyncio 协程 + 线程池并行）、模型路由与三态熔断高容错（流式首包探测，故障注入下成功率保持 95% 左右）、异步场景问题追踪定位（TraceRun + TraceNode）；复杂自然语言场景下召回率提升 30%+。⚠️ 真实项目为 Java（Tiny-Ragent，云 API），简历为 Python 本地模型重写版（Nidus，进行中）——选型攻防、LangGraph 分层与两个陷阱见 [`00-rag-platform.md`](ceec/00-rag-platform.md) §1。已砍 RocketMQ，embedding 走本地 bge-m3（维度 1024）。⚠️ 简历栈行精简省略了**实际在用的 Redis + React 18**（Redis 做会话记忆+熔断状态共享+限流+幂等，React 仅最小演示页）；配置化摄入流水线保留在深挖笔记中，非简历主打。
  - 分点深挖：[① 多路并行检索](ceec/01-rag-retrieval.md) · [② 模型路由+三态熔断](ceec/02-rag-model-routing.md) · [③ 异步全链路追踪](ceec/03-rag-tracing.md) · [④ 配置化摄入流水线](ceec/04-rag-ingestion.md) · [⑤ LangGraph 控制流](ceec/05-rag-langgraph.md)
- **武汉理工学科竞赛综合管理系统**（全栈 · Java 11 + Spring Boot 2 + MySQL + Redis + MyBatis + Sa-Token + FastExcel + Vue 3）：SSO 统一身份认证（Sa-Token，CAS 重定向+Ticket 验票、自动初始化身份、分布式 Token 无状态会话）、动态数据权限（MyBatis 拦截器，实体基类扩展属性 + "角色-部门-自定义"3 层 + 5 种权限粒度）、高性能 Excel 读写（FastExcel/SAX 流式 + 观察者模式 AnalysisEventListener，导入效率 2-3 倍）；上线稳定运行 1 年+，服务 5 万+ 校内师生用户。
  - 分点深挖：[① SSO（Sa-Token）](whut/01-contest-sso.md) · [② 动态数据权限（MyBatis 拦截器）](whut/02-contest-data-permission.md) · [③ 高性能 Excel（SAX 流式）](whut/03-contest-excel.md)；索引页 [`00-contest-system.md`](whut/00-contest-system.md) 载获奖审核业务/OOM 案例/测开视角。

---

## 三、目录结构

```
resume-parser/
├── README.md                       # 本文件：进度看板 + bullet 汇总 + backlog
├── AGENTS.md                       # 给 agent 的项目规范
├── resume.tex                      # ⭐ 仓库内权威简历源（moderncv/LaTeX，唯一事实来源）
├── resume-notes/
│   ├── bytedance/                  # ✅ 三份已封板 + 实习背景索引
│   │   ├── 00-context.md           #   ✅ 实习背景（SDT=Spec-Driven Testing + AI Testing 工具链 + 三项目闭环）
│   │   ├── 01-ai-data-gen.md
│   │   ├── 02-skills-pulse.md
│   │   └── 03-api-testing-agent.md
│   ├── kuaishou/                   # ✅ 三份已封板（效能星盘 / 星诉 / 快聊 UI）
│   │   ├── 01-xingpan.md
│   │   ├── 02-xingsu.md
│   │   └── 03-kuaichat-ui.md
│   ├── iflytek/                    # ✅ 三份已封板（答题卡渲染 / 数据回流 / 接口自动化）
│   │   ├── 01-essay-sheet-render.md
│   │   ├── 02-data-backflow.md
│   │   └── 03-api-automation.md
│   ├── ceec/                       # ✅ 中国能建 RAG · 索引 + 5 份分点笔记
│   │   ├── 00-rag-platform.md         #   索引/总览（STAR/主流程/选型攻防/主代码对照表）
│   │   ├── 01-rag-retrieval.md        #   ① 多路并行检索
│   │   ├── 02-rag-model-routing.md    #   ② 模型路由 + 三态熔断
│   │   ├── 03-rag-tracing.md          #   ③ 异步全链路追踪
│   │   ├── 04-rag-ingestion.md        #   ④ 配置化摄入流水线
│   │   └── 05-rag-langgraph.md        #   ⑤ LangGraph 控制流
│   ├── whut/                       # ✅ 学科竞赛系统 · 索引 + 3 份分点笔记
│   │   ├── 00-contest-system.md       #   索引/总览（STAR/主流程/获奖审核业务/OOM 案例/主代码对照表）
│   │   ├── 01-contest-sso.md          #   ① SSO 统一身份认证（Sa-Token）
│   │   ├── 02-contest-data-permission.md #   ② 动态数据权限（MyBatis 拦截器）
│   │   └── 03-contest-excel.md        #   ③ 高性能 Excel 读写（SAX 流式）
│   └── education/                  # ✅ 教育背景速查卡（轻量；国创=竞赛系统）
│       └── 00-background.md           #   国创项目/发明专利/软著/学科竞赛（一句话速查 + 口径纪律）
├── fundamentals/                   # ✅ 已整合至主知识库各对应目录（原八股速记）
└── job-targets/                    # 目标岗位：按公司分，每家下分 笔试(written-exam) / 面试(interview)
    └── baidu/                       # 百度（校招）
        ├── written-exam/           #   笔试
        │   ├── 01-overview.md      #     结构(赛码网)+选择题高频+编程真题库+I/O 模板+两天计划
        │   ├── 02-coding-solutions.md #  硬核题解：构造回文串/异或数组/树染色(含纠错)
        │   └── 03-coding-dp.md     #     DP 题解：上楼梯变体/交替加减取模
        └── interview/              #   面试
            └── 01-ai-test-dev-jd.md #    上海 AI 测试开发工程师（校招 J101057）JD↔KB 差距分析
```

> 新增笔记命名：`resume-notes/<公司>/<NN>-<项目短名>.md`（两位序号前缀 + 小写连字符）。序号在**各公司文件夹内**从 `01` 递增；**项目经历的索引页用 `00-`**（排最前），分点笔记依次 `01/02/03…`（如 `00-contest-system.md` + `01-contest-sso.md`）。八股已整合至主知识库各对应目录。目标岗位放 `job-targets/<公司>/{written-exam,interview}/<NN>-<短名>.md`。

---

## 四、贯穿全局的红线（每份笔记都遵守）

- **真实性第一**：技术细节绝不编造；推测/记忆来源的内容显式标注"待确认"。
- **数字必被追**：`40%`、`90%+`、`1000+`、`30%` 等都是**体感/上报口径**，被问必主动降级说明来源，不吹成精确实验值。
- **项目互串**：造数 Skill（AI Data Gen）是 API Testing Agent 的数据源、其指标由 Skills Pulse 采集——三者成闭环，能互相带出。
- **诚实划界**：团队项目讲清自己 own 的部分，平台基建（ATE 等）说明是"复用 + 编排",不越界吹。
