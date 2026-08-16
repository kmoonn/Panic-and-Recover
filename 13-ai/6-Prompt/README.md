# Prompt 提示词

Prompt 工程是通过设计和优化输入提示来引导 LLM 输出更好结果的技术，核心范式从简单到复杂：Zero-shot → Few-shot → CoT → ReAct → 自动化。

```
Zero-shot  →  Few-shot  →  CoT  →  Self-Consistency  →  ReAct
 简单指令      加示例       加推理    多路径投票            加工具调用
                                                            ↓
                                                        Agent 系统
```

从左到右：**能力递增、成本递增、复杂度递增**。

## 笔记

- [Zero-shot与Few-shot](Zero-shot与Few-shot.md) — 基础范式 + Prompt 基本结构
- [CoT思维链](CoT思维链.md) — CoT / Self-Consistency / Tree of Thought
- [ReAct范式](ReAct范式.md) — Reasoning + Acting，Agent 基础范式
- [Prompt自动化优化](Prompt自动化优化.md) — APE / OPRO / DSPy + System Prompt 设计
