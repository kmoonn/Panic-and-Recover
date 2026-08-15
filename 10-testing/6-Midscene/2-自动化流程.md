---
tags:
  - 测试
  - AI
  - UI自动化
category: 测试/Midscene
---

# Midscene.js 自动化流程

## 整体 Pipeline

```
自然语言指令 → 截图捕获 → 多模态模型推理 → 元素定位 → 操作执行 → 结果校验 → 报告生成
```

## 逐步详解

### 1. 指令输入

用户通过 API 传入自然语言指令：

```typescript
// 执行操作
await aiAction("在搜索框中输入'helloworld'并点击搜索按钮");

// 查询数据
const items = await aiQuery<string[]>("列表中所有记录的标题");

// 断言
await aiAssert("页面中存在成功提示");

// 布尔判断
const isLoading = await aiBoolean("页面正在加载中");
```

### 2. 截图捕获

平台适配器自动获取当前界面截图，无需手动提供。支持多平台：
- Web：浏览器截图
- Android/iOS：设备屏幕截图
- 桌面端：窗口截图

### 3. 多模态模型推理

模型接收 **截图 + 指令**，完成两件事：
1. **UI 理解**：识别截图中所有可交互元素及其语义
2. **操作规划**：匹配用户描述的目标元素，规划具体操作

### 4. 元素定位（ID 映射缓存）

```
模型返回预测元素 ID → 查缓存映射 → 命中 → 直接定位 DOM 节点
                                  → 未命中 → 降级二次视觉定位
```

### 5. 操作执行

根据规划结果驱动浏览器/设备执行操作：

| 操作类型 | 示例 |
|---|---|
| 点击 | 点击按钮、链接 |
| 输入 | 在输入框键入文字 |
| 滚动 | 滚动到指定位置 |
| 悬停 | 鼠标悬停触发下拉 |

### 6. 结果校验

- `aiAssert`：自然语言断言（如"价格显示为¥99"）
- `aiBoolean`：布尔判断（如"弹窗是否出现"）
- `aiQuery`：提取结构化数据用于后续断言

### 7. 报告生成

每次运行生成**可视化报告**，包含：
- 逐步动画回放
- 每步截图 + 模型推理详情
- 在线调整提示词（调试用）

## 两种自动化风格

| | 自动规划（Auto Pilot） | 工作流风格（Workflow） |
|---|---|---|
| **控制权** | AI 自主规划执行 | 人工拆步、逐步明确 |
| **稳定性** | 较低（依赖模型判断） | 较高（每步指令明确） |
| **适用场景** | 简单一次性任务 | 复杂需稳定回归的测试 |
| **编写方式** | 一句自然语言描述完整流程 | 拆成多步 `aiAction` 逐步执行 |

```typescript
// 自动规划：一句话搞定
await aiAction("搜索helloworld并查看结果详情");

// 工作流风格：逐步明确，更稳定
await aiAction("在搜索框输入'helloworld'");
await aiAction("点击搜索按钮");
await aiAction("点击第一条搜索结果");
```

**推荐**：生产回归测试用工作流风格，探索/原型用自动规划。

## 四层项目架构实践

```
Case 层       →  编写具体测试用例
  ↑
Service 层    →  跨页面组合业务逻辑
  ↑
Page 层       →  封装单个页面的操作方法
  ↑
Component 层  →  封装可复用组件的定位逻辑（同一组件多页面复用）
```

```typescript
// Component 层：通用组件
const searchBox = {
  input: async (text: string) => aiAction(`在搜索框输入'${text}'`),
  submit: async () => aiAction("点击搜索按钮"),
};

// Page 层：组合组件
const homePage = {
  search: async (keyword: string) => {
    await searchBox.input(keyword);
    await searchBox.submit();
  },
};
```

## 与 Playwright / Puppeteer 集成

```typescript
// Playwright 集成
import { PlaywrightAiFixture } from "@midscene/web/playwright";
const ai = new PlaywrightAiFixture(page);
await ai.aiAction("点击登录按钮");

// Puppeteer 集成
import { PuppeteerAdapter } from "@midscene/web/puppeteer";
const ai = new PuppeteerAdapter(browser);
await ai.aiAction("点击登录按钮");
```

## 一句话总结

> 流程为截图到推理到定位到执行到校验到报告，生产用工作流风格逐步明确，探索用自动规划。
