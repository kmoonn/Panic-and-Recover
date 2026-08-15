---
tags:
  - AI
  - LLM
category: AI/基础
---

# 大模型 QKV 机制

## Q1: 什么是 Q、K、V?

**Q (Query)、K (Key)、V (Value)** 是 Transformer 中 Self-Attention 机制的三个核心矩阵，由输入嵌入经线性变换得到。

| 符号 | 全称 | 直觉含义 | 类比 |
|------|------|----------|------|
| **Q** | Query | "我在找什么" | 搜索关键词 |
| **K** | Key | "我有什么特征" | 书的索引/关键词 |
| **V** | Value | "我携带的内容" | 书的正文内容 |

### 生成方式

```
输入 X ∈ R^(n×d)    (n 个 token，每个 d 维)

Q = X · W_Q    (W_Q ∈ R^(d×d_k)，查询变换矩阵)
K = X · W_K    (W_K ∈ R^(d×d_k)，键变换矩阵)
V = X · W_V    (W_V ∈ R^(d×d_v)，值变换矩阵)
```

三个变换矩阵 W_Q、W_K、W_V 是**可学习的参数**，在训练过程中更新。

---

## Q2: Self-Attention 的计算公式是什么?

### 核心公式

$$
\text{Attention}(Q, K, V) = \text{softmax}\left(\frac{QK^T}{\sqrt{d_k}}\right) V
$$

### 分步计算

```
Step 1: 计算注意力分数 (score)
  score = Q · K^T          ∈ R^(n×n)
  每个 token 的 Q 与所有 token 的 K 做点积，得到 n×n 的注意力矩阵

Step 2: 缩放 (scale)
  scaled_score = score / √d_k
  除以 √d_k 防止点积值过大

Step 3: 归一化 (softmax)
  attention_weight = softmax(scaled_score)    ∈ R^(n×n)
  每行归一化为概率分布，表示对每个 token 的关注程度

Step 4: 加权求和 (weighted sum)
  output = attention_weight · V    ∈ R^(n×d_v)
  用注意力权重对 V 加权求和，得到最终表示
```

### 数值示例 (简化)

```python
import torch
import torch.nn.functional as F

# 假设 3 个 token，d_k = 4
Q = torch.tensor([[1.0, 0.5, 0.3, 0.2],   # token 1 的 query
                   [0.4, 1.2, 0.1, 0.6],   # token 2 的 query
                   [0.7, 0.3, 1.1, 0.4]])  # token 3 的 query

K = torch.tensor([[0.9, 0.4, 0.5, 0.1],   # token 1 的 key
                   [0.3, 1.0, 0.2, 0.8],   # token 2 的 key
                   [0.6, 0.2, 0.9, 0.5]])  # token 3 的 key

V = torch.tensor([[1.1, 0.2, 0.7],         # token 1 的 value
                   [0.5, 0.9, 0.3],         # token 2 的 value
                   [0.8, 0.4, 1.0]])        # token 3 的 value

d_k = Q.shape[-1]  # 4

# Step 1: QK^T
scores = Q @ K.T  # shape: (3, 3)

# Step 2: Scale
scaled_scores = scores / (d_k ** 0.5)

# Step 3: Softmax
attn_weights = F.softmax(scaled_scores, dim=-1)

# Step 4: Weighted V
output = attn_weights @ V
```

---

## Q3: 为什么要除以 √d_k (缩放因子)?

这是面试高频问题！

### 问题

当 d_k 较大时，Q 和 K 的点积结果会很大（向量维度越高，点积值的方差越大）。

### 数学推导

```
假设 q 和 k 的各分量独立、均值为 0、方差为 1

则 q · k = Σ(q_i × k_i)，共 d_k 项

E[q · k] = 0
Var[q · k] = d_k        ← 方差与 d_k 成正比！

即点积的标准差 = √d_k
```

### 不缩放的后果

| 情况 | 点积值 | softmax 输出 | 问题 |
|------|--------|-------------|------|
| d_k 小 (如 64) | 较小 | 分布较平滑 | 正常 |
| d_k 大 (如 512) | 很大 | 极度尖锐 (接近 one-hot) | **梯度消失** |

```
softmax([10, 1, 1]) ≈ [0.9999, 0.00005, 0.00005]  ← 接近 one-hot
softmax([2, 0.2, 0.2]) ≈ [0.64, 0.18, 0.18]       ← 较平滑

→ 极端分布下，softmax 的梯度趋近于 0，训练几乎停滞
→ 除以 √d_k 将点积拉回合理范围，保持梯度有效
```

### 一句话

**√d_k 缩放 = 把点积值除以其标准差，使 softmax 输入保持在合理范围，防止梯度消失。**

---

## Q4: 什么是 Multi-Head Attention (多头注意力)?

### 核心思想

将 Q、K、V 分成 h 个头，每个头独立计算注意力，最后拼接结果。

> 单头注意力只能捕捉一种关联模式，多头可以同时关注不同子空间的不同关系（语法、语义、位置等）。

### 计算流程

```
输入 X ∈ R^(n×d)

1. 线性变换得到 Q, K, V
   Q = X · W_Q,  K = X · W_K,  V = X · W_V

2. 按 head 拆分
   Q = [Q_1, Q_2, ..., Q_h]    每个 Q_i ∈ R^(n×d_k/h)
   K = [K_1, K_2, ..., K_h]
   V = [V_1, V_2, ..., V_h]

3. 每个 head 独立计算 Scaled Dot-Product Attention
   head_i = Attention(Q_i, K_i, V_i) = softmax(Q_i K_i^T / √(d_k/h)) V_i

4. 拼接所有 head 的输出
   MultiHead = Concat(head_1, head_2, ..., head_h) · W_O
   W_O ∈ R^(d×d) 是输出的线性变换矩阵
```

### 图示

```
         Q ──┬── Q_1 ──→ head_1 ──┐
             ├── Q_2 ──→ head_2 ──┤
             ├── ...               ├── Concat ──→ W_O ──→ Output
             └── Q_h ──→ head_h ──┘

         K, V 同理拆分
```

### 参数对比

| 模型 | d_model | h (头数) | d_k per head |
|------|---------|---------|-------------|
| BERT-base | 768 | 12 | 64 |
| BERT-large | 1024 | 16 | 64 |
| GPT-2 | 768 | 12 | 64 |
| GPT-3 | 12288 | 96 | 128 |

> 注意：d_k/h 通常保持为 64 或 128，即每个头的维度相对固定，增加模型宽度时增加头数。

---

## Q5: QKV 的直觉理解——图书馆搜索类比

| 概念 | 图书馆类比 | 在注意力中的作用 |
|------|-----------|----------------|
| **Q (Query)** | 你在搜索框输入的关键词 | 当前 token 想要寻找什么样的信息 |
| **K (Key)** | 每本书的标签/索引词 | 其他 token 有什么样的特征可供匹配 |
| **Q · K^T** | 搜索关键词与书标签的匹配程度 | 两个 token 的相关程度 (注意力分数) |
| **V (Value)** | 书的正文内容 | 匹配到的 token 实际携带的信息 |
| **softmax** | 按匹配程度分配阅读时间 | 将分数归一化为概率分布 |
| **加权 V** | 按比例吸收各书内容 | 融合相关 token 的信息得到新表示 |

### 完整类比流程

```
你 (Query) 到图书馆找资料:
  1. 你带着问题 "深度学习入门"        ← Q: 我在找什么
  2. 每本书有标签 [AI, 入门, 进阶...]  ← K: 书有什么特征
  3. 你的问题与各标签匹配打分          ← QK^T: 注意力分数
  4. 按匹配程度决定每本书的阅读比重    ← softmax: 归一化
  5. 按比重吸收各书内容               ← 加权V: 得到综合信息
```

---

## Q6: Self-Attention vs Cross-Attention

| 类型 | Q 来源 | K,V 来源 | 用途 |
|------|--------|---------|------|
| **Self-Attention** | 同一序列 | 同一序列 | 捕捉序列内部依赖 (如 BERT) |
| **Cross-Attention** | 一个序列 | 另一个序列 | 跨序列信息交互 (如 Decoder 关注 Encoder 输出) |

```
Self-Attention:
  Q = X · W_Q,  K = X · W_K,  V = X · W_V    (X 为同一输入)

Cross-Attention:
  Q = X · W_Q                                (X 为当前序列)
  K = Y · W_K,  V = Y · W_V                  (Y 为另一个序列，如 Encoder 输出)
```

---

## 一句话总结

**QKV 是 Transformer 注意力机制的核心——Q 是 "我在找什么"，K 是 "我有什么特征"，V 是 "我携带的内容"，通过 QK^T/√d_k 计算相关性分数再对 V 加权求和，多头机制让模型同时关注不同子空间的不同关系。**
