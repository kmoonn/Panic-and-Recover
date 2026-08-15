---
tags:
  - AI
  - Skills
category: AI/Skills
---

# Skill 结构

## Skill 的标准结构

一个完整的 Skill 由 **元数据 + Prompt 模板 + 工具绑定 + I/O Schema** 四部分组成：

```
Skill/
├── SKILL.md          ← 元数据 + Prompt 模板（核心）
├── references/       ← 参考文档（可选）
├── scripts/          ← 脚本工具（可选）
└── evals/            ← 评估数据（可选）
```

---

## SKILL.md 文件结构

### 1. Frontmatter（元数据）

```yaml
---
name: code-review          # Skill 名称（唯一标识）
description: 代码审查，检查安全性和规范性  # 一句话描述（用于触发匹配）
triggers:                  # 触发条件（什么情况下激活）
  - 代码审查
  - code review
  - review code
---
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `name` | ✅ | Skill 唯一标识，小写+连字符 |
| `description` | ✅ | 一句话描述，用于搜索引擎匹配 |
| `triggers` | ❌ | 触发关键词列表 |

### 2. Prompt 模板（指令区）

```markdown
# 代码审查

你是一个专业的代码审查员。请审查以下代码的安全性和规范性。

## 审查维度
1. 安全漏洞（SQL注入、XSS、敏感信息泄露）
2. 代码规范（命名、注释、格式）
3. 性能问题（N+1查询、内存泄漏）
4. 逻辑错误（边界条件、空指针）

## 输出格式
- 严重程度：Critical / Warning / Info
- 位置：文件名:行号
- 描述：问题说明
- 建议：修复方案
```

### 3. 工具绑定

Skill 通过声明方式指定需要的工具，执行时自动注入：

```markdown
## 工具

- read_file: 读取代码文件
- git_diff: 查看代码变更
- search_code: 搜索代码模式
```

### 4. 完整示例

```yaml
---
name: deploy
description: 部署服务到指定环境
triggers:
  - deploy
  - 部署
  - 发布
---

# 部署服务

请执行以下部署流程：

## 前置检查
1. 确认所有测试通过
2. 检查当前分支和版本号
3. 确认目标环境配置

## 部署步骤
1. 构建镜像
2. 推送到镜像仓库
3. 更新 K8s 配置
4. 滚动发布
5. 健康检查

## 回滚条件
- 健康检查连续失败 3 次
- 错误率超过 1%
- P99 延迟超过基线 2 倍

## 工具
- run_tests: 运行测试
- docker_build: 构建镜像
- kubectl_apply: 更新 K8s 配置
- check_health: 健康检查
```

---

## 参考文档（references/）

为 Skill 提供知识补充，不影响 Prompt 但在需要时可查阅：

```
references/
├── api-spec.md        ← API 规范文档
├── deploy-manual.md   ← 部署手册
└── style-guide.md     ← 代码风格指南
```

**用途**：当用户的问题涉及具体规范时，Agent 可以查阅参考文档获取细节，而不必把所有内容塞进 Prompt。

---

## 脚本工具（scripts/）

Skill 可自带脚本工具，扩展 Agent 的能力边界：

```
scripts/
├── run_linter.py      ← 运行代码检查
├── check_coverage.sh  ← 检查测试覆盖率
└── deploy.sh          ← 部署脚本
```

**特点**：
- 脚本由 Agent 调用执行，不是手动运行
- 需要有明确的输入/输出接口
- 错误处理要完善（Agent 可能传非法参数）

---

## 评估数据（evals/）

用于验证 Skill 的执行效果：

```json
{
  "test_cases": [
    {
      "input": "Review the auth module",
      "expected_contains": ["SQL injection", "password"],
      "expected_not_contains": ["LGTM without issues"]
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `input` | 测试输入 |
| `expected_contains` | 输出应包含的关键内容 |
| `expected_not_contains` | 输出不应包含的内容 |

---

## Skill 结构设计原则

| 原则 | 说明 | 反例 |
|------|------|------|
| **单一职责** | 一个 Skill 只做一件事 | "代码审查并部署"（两个职责） |
| **Prompt 精炼** | 指令清晰简短，不超过上下文的 10% | 3000 字的 Prompt 模板 |
| **工具最小化** | 只声明必要的工具 | 声明 20 个工具但只用 3 个 |
| **输入明确** | 明确输入参数和格式 | 模糊的"传入相关信息" |
| **输出结构化** | 指定输出格式，便于下游消费 | 自由文本输出 |
| **可评估** | 有 evals 数据验证效果 | 无法判断执行是否正确 |

---

## Skill 与 MCP 的分工

```
┌──────────────────────────────────────┐
│            Skill 层                   │
│  定义"做什么"：审查代码 / 部署服务     │
│  Prompt + 工具选择 + I/O 约束         │
├──────────────────────────────────────┤
│            MCP 层                     │
│  提供"怎么做"：read_file / kubectl    │
│  原子级工具 + 通信协议                │
└──────────────────────────────────────┘

Skill 调用 MCP 工具完成具体操作
MCP 工具不知道自己在被哪个 Skill 使用
```

| 维度 | Skill | MCP Tool |
|------|-------|----------|
| 粒度 | 任务级（代码审查） | 操作级（读取文件） |
| 包含 Prompt | 是 | 否 |
| 可组合 | Skill 可链式调用 | Tool 被组合使用 |
| 类比 | 菜谱（做什么菜） | 厨具（锅碗瓢盆） |

---

## 面试高频

### Skill 和 Prompt 模板有什么区别？

| 维度 | 普通 Prompt | Skill |
|------|------------|-------|
| 结构 | 纯文本 | Frontmatter + Prompt + 工具 + Schema |
| 工具 | 无绑定 | 声明式绑定 |
| 输入校验 | 无 | Input Schema 校验 |
| 输出约束 | 无 | Output Schema 约束 |
| 可评估 | 难 | 有 evals 评估数据 |
| 可复用 | 复制粘贴 | 模块化引用 |

### 多个 Skill 如何组合？

```
用户: "Review and deploy the payment service"

Step 1: code-review Skill
  → 输出: {issues: [...], can_deploy: true}

Step 2: deploy Skill（依赖 Step 1 结果）
  → 如果 can_deploy=false, 终止
  → 如果 can_deploy=true, 执行部署
```

组合方式：**链式**（A→B）、**条件式**（A 结果决定是否执行 B）、**并行式**（A 和 B 同时执行后合并）。

### Skill 的 triggers 怎么设计？

- 覆盖**中英文**关键词（"代码审查" + "code review"）
- 包含**同义词**（"部署" + "发布" + "deploy"）
- 不要太泛（"帮我"这种词不要放），避免误触发
- 3~5 个 trigger 词最佳

---

## 一句话总结

> Skill 结构由 Frontmatter（元数据+触发词）、Prompt 模板（核心指令）、工具绑定（声明式依赖）、I/O Schema（输入输出约束）四部分组成，遵循单一职责、Prompt 精炼、工具最小化原则，与 MCP 是"菜谱与厨具"的关系——Skill 定义做什么菜，MCP 提供锅碗瓢盆。
