# Midscene.js

**Midscene.js** 是字节跳动 Web Infra 团队开源的 **AI 视觉驱动 UI 自动化测试框架**。核心特点：**摒弃传统 DOM 选择器，仅凭截图 + 多模态模型理解界面**，用自然语言描述操作意图即可完成自动化。

覆盖场景：Web、Android、iOS、桌面端、Canvas、跨域 iframe。

| | 传统 UI 自动化 | Midscene.js |
|---|---|---|
| 定位方式 | CSS/XPath 选择器 | 自然语言 + 视觉理解 |
| 维护成本 | DOM 变更就挂 | 视觉不变就能定位 |
| Canvas 支持 | ❌ | ✅ |
| 跨域 iframe | ❌ | ✅ |
| 编写方式 | 写选择器代码 | 写自然语言描述 |

支持的多模态模型：Qwen3.x、Doubao-Seed-2.1、GLM-4.6V、Gemini-3.5-flash、UI-TARS 等，含可自托管开源选项。

适用场景：
- 页面频繁迭代、选择器维护成本高
- Canvas / WebView / 原生应用等传统工具难以覆盖
- 快速原型、验收测试、RPA 流程自动化
- 作为 AI Agent 的操作执行工具

## 笔记

- [Midscene架构](Midscene架构.md)
- [Midscene自动化流程](Midscene自动化流程.md)
- [Midscene组件定位失败处理](Midscene组件定位失败处理.md)
