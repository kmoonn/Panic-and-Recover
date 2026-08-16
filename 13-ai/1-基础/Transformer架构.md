---
category: AI/基础
tags:
  - AI
  - LLM
  - Transformer
  - 面试
---

# Transformer 架构

> 一句话总结：Transformer = 堆叠的(多头自注意力 + FFN + 残差 + LayerNorm)，用注意力替代 RNN 的递归实现并行计算，是所有现代 LLM 的骨架。

---

## Token → 向量（Embedding）

文本进入 Transformer 前必须变成数字，但纯编号（如 Unicode）不含语义。解决方案：

1. **分词**：将句子切分为 token（如"货拉拉拉不拉拉布拉多" → 「货拉拉」「拉不拉」「拉布拉多」）
2. **映射到高维向量空间**：每个 token 对应一个向量，维度通常数百到数千。训练后，**语义相近的词在空间中靠近，语义无关的词远离**
3. **加入位置编码**：同一词在不同位置含义不同，向量需同时编码"是什么"和"在哪个位置"

经典性质：**国王 − 男人 + 女人 ≈ 王后**（向量运算可捕捉语义关系）

多义词（如"拉"）在空间中只是一个模糊的平均位置，**结合上下文后才会被"拉"向正确的语义区域**——这正是注意力机制要做的事。

> 向量也常被称为 Embedding，两者在本语境下基本同义。

---

## 整体架构

```
输入序列 → [Embedding + Position Encoding] → Encoder ×N → 编码表示
                                                            ↓
目标序列 → [Embedding + Position Encoding] → Decoder ×N → 输出概率
```

原论文（Attention Is All You Need, 2017）：Encoder 6 层、Decoder 6 层、d_model=512、8 头注意力。

**现代变体**：
- **Encoder-Only**（BERT）：只保留 Encoder，双向注意力，适合理解任务（分类、NER）
- **Decoder-Only**（GPT/LLaMA/Qwen）：只保留 Decoder，单向注意力，适合生成任务
- **Encoder-Decoder**（T5/BART）：完整结构，适合 seq2seq 任务（翻译、摘要）

---

## Encoder 层

```
输入 x
  │
  ▼
Multi-Head Self-Attention ──→ 残差 + LayerNorm ──→ x₁
  │
  ▼
Feed-Forward Network (FFN) ──→ 残差 + LayerNorm ──→ x₂
  │
  ▼
输出 x₂（传入下一层 Encoder）
```

- **双向自注意力**：每个 token 可以看到输入序列中所有位置
- **残差连接**：`LayerNorm(x + Sublayer(x))`，保证梯度可直达浅层

## Decoder 层

```
输入 x
  │
  ▼
Masked Self-Attention ──→ 残差 + LayerNorm ──→ x₁     ← 只看当前位置及之前
  │
  ▼
Cross-Attention(Encoder输出) ──→ 残差 + LayerNorm ──→ x₂   ← 关注源序列
  │
  ▼
FFN ──→ 残差 + LayerNorm ──→ x₃
  │
  ▼
输出 x₃
```

### Decoder 为什么需要两种注意力？

Decoder 生成目标序列时需解决**两个问题**：

| 问题 | 解决方式 | Q 来源 | K/V 来源 |
|------|----------|--------|----------|
| 已生成的目标词之间如何衔接连贯？ | **Masked Self-Attention** | 已生成的目标序列 | 已生成的目标序列 |
| 当前翻译位置最该参考源序列哪部分？ | **Cross-Attention** | 已生成的目标序列 | Encoder 输出 |

### 自回归生成

生成过程是**逐词预测**：每一步在已有结果基础上预测下一个最合理的词，类似"成语接龙"。

```
<BOS> → Can → Can Huolala → Can Huolala carry → ... → Can Huolala carry a Labrador
```

### Masked Attention 的作用

防止**信息泄露**——预测第 t 个词时不能看到 t+1 及之后的词，保证自回归的一致性。实现：在注意力分数上加负无穷 mask，未来位置的 softmax 权重为 0。

### Encoder vs Decoder 对比

| 维度 | Encoder | Decoder |
|------|---------|---------|
| 注意力方向 | 双向（看全部） | 单向（只看过去） |
| 自注意力 | 普通 Self-Attention | Masked Self-Attention |
| 跨注意力? | ❌ 无 | ✅ 关注 Encoder 输出 |
| 典型模型 | BERT | GPT / LLaMA |
| 擅长 | 理解（分类、匹配） | 生成（对话、续写） |

---

## 多头注意力

> 详细的 Q/K/V 计算见 [大模型QKV](1-大模型QKV.md)

**为什么拆多个头？**

借鉴 CNN 多通道思想——每个头在独立的子空间学习不同类型的依赖关系：
- 有的头学近距离依赖（相邻词的语法关系）
- 有的头学长距离依赖（句首和句尾的指代关系）
- 有的头学语法特征、语义特征等

单头注意力只能学一种上下文表示，多头让模型**同时**关注多种模式。

**缩放因子 √d_k**：防止 Q·K 内积过大导致 softmax 饱和（梯度趋零）。除以 √d_k 使方差归一化到 1 附近。

---

## 位置编码

### 为什么需要位置编码？

Self-Attention 是**置换不变的**——打乱输入顺序，输出只是对应打乱，模型无法感知位置。而"我爱你"和"我爱你"顺序不同语义不同，必须注入位置信息。

### 三种主流位置编码

| 类型 | 代表 | 原理 | 优点 | 缺点 |
|------|------|------|------|------|
| 正弦/余弦 | 原论文 | 固定三角函数，不同频率编码不同位置 | 无需学习、可外推到更长序列 | 外推能力有限 |
| 可学习 | BERT | 位置 embedding 作为参数训练 | 灵活、适配数据 | 无法外推到训练时未见的位置 |
| **RoPE** | LLaMA/Qwen/ChatGLM | 旋转矩阵作用于 Q/K，内积只依赖相对位置 | 绝对位置实现相对位置感知、外推性强 | 实现稍复杂 |

### RoPE 核心思想

对 Q 和 K 向量施加**位置相关的旋转**，旋转角度与位置成正比。两个位置的内积只取决于它们的**相对位置差**，天然具备远程衰减性质。

一句话：**RoPE 用绝对位置编码的方式实现了相对位置感知，兼具两者优势。**

---

## 前馈网络（FFN）

每个 token **独立**经过两层全连接网络：

```
FFN(x) = W₂ · activation(W₁ · x + b₁) + b₂
```

- 中间维度通常是 d_model 的 4 倍（如 512 → 2048 → 512）
- 原论文用 ReLU，现代模型用 **SwiGLU**（LLaMA/Gemma）

### FFN 的作用

注意力层完成 token 间的**信息交互**，FFN 让每个 token **独立消化**整合到的信息——提取特征、非线性变换，为下一层注意力交互做准备。

**类比**：注意力是"开会交流"，FFN 是"会后独立思考消化"。

---

## 残差连接 + LayerNorm

### 残差连接

```
输出 = LayerNorm(x + Sublayer(x))
                ↑     ↑
              直通    本层变换
```

- **作用**：梯度可经直通路径直接回传到浅层，解决深度网络的**梯度消失**问题
- **约束**：所有子层输出维度 = d_model，保证残差加法维度一致
- 每个 Transformer 块有**两处**残差连接（注意力后 + FFN 后）

### LayerNorm vs BatchNorm

| 维度 | LayerNorm | BatchNorm |
|------|-----------|-----------|
| 归一化方向 | 沿**特征维度**（每个 token 独立） | 沿**batch 维度**（每个特征跨样本） |
| 依赖 batch 大小? | ❌ 不依赖 | ✅ 依赖 |
| 变长序列 | 适合 | 不适合 |
| Transformer 选择 | ✅ LayerNorm | ❌ |

### Pre-LN vs Post-LN

| 方式 | 公式 | 说明 |
|------|------|------|
| Post-LN（原论文） | `LayerNorm(x + Sublayer(x))` | 训练不稳定，需 warmup |
| **Pre-LN（现代）** | `x + Sublayer(LayerNorm(x))` | 训练更稳定，无需 warmup，LLaMA/GPT 采用 |

---

### 为什么 GPT 不需要 Encoder？

原始 Transformer 用于翻译——有"源语言"和"目标语言"的区分，Encoder 专职理解源语言，Decoder 专职生成目标语言，Cross-Attention 是两者桥梁。

GPT 的场景是对话/续写，**输入和输出是同一条连续文本**，没有两种语言的区分：

```
[用户问题：什么是WAL？] [模型接着写：WAL即预写日志，____]
 ↑ 输入部分                ↑ 生成部分
 └─────── 同一条文本，从左到右 ───────┘
```

Masked Self-Attention **同时完成了理解和生成**：
- 处理输入部分时，每个 token 关注前面的输入 token → 等效于"理解输入"
- 生成回复时，新 token 能看到所有输入 + 已生成内容 → 等效于"带着上下文生成"

原本需要 Encoder + Cross-Attention 完成的事，一个 Masked Self-Attention 就全干了。

**一句话：GPT 不需要 Encoder，因为输入和输出是同一条连续文本，Decoder 在处理输入时就已经"理解"了输入，生成时又能看到输入，不需要额外的 Encoder 单独理解。**

### 为什么 BERT 不需要 Decoder？

理解任务只需要"看完整个输入做判断"，不需要"逐词生成"。

Encoder 的**双向注意力**天然适合理解——每个 token 能看左右两侧所有词，理解更充分。Decoder 的单向注意力只能看左半边，对纯理解任务是不必要的信息损失。

Decoder 做单向是为了"生成时不泄露未来信息"，但理解任务根本不生成，没有信息泄露问题，**放开双向反而更合理**。

**一句话：BERT 不需要 Decoder，因为理解任务不需要生成；Encoder 双向注意力看全句，比单向理解更充分，且没有信息泄露的顾虑。**

### Decoder 也能做语义理解吗？

可以，靠的是单向注意力的**多层信息累积**：
- 后面的词能回头看到前面的词，信息从左到右逐步累积
- 多层堆叠后，信息间接传递，单向也能间接获取全局信息
- 模型够大、训练数据够多，单向理解力也够用（GPT-4 的理解能力没人质疑）

但同等参数量下不如双向精确——双向一步到位看全句，单向靠间接传递有信息损失。小模型纯理解任务上 BERT 仍比同等 GPT 强，但大模型时代差距被规模抹平。

**一句话：Decoder 也能理解语义，靠多层信息累积。同等规模不如双向精确，但模型够大时理解力也够用——这就是 GPT 放弃 Encoder 的底气。**

### 三种架构选择速查

| 架构 | 看的方向 | 适合 | 不适合 |
|------|---------|------|--------|
| Encoder-Only（BERT） | 双向 | 理解（分类、匹配、NER） | 生成（无生成能力） |
| Decoder-Only（GPT） | 单向 | 生成（对话、续写、代码） | 纯理解（丢失后文信息） |
| Encoder-Decoder（T5） | 双向+单向 | 翻译、摘要（理解源+生成目标） | 单一任务时架构冗余 |

---

## 与简历项目的关联

| 知识点 | 项目关联 |
|--------|---------|
| Decoder-Only 生成 | RAG 平台用 Qwen（Decoder-Only）做答案生成与自检，单模型同时理解检索结果并生成回答 |
| Embedding 向量 | RAG 平台用 bge-m3（1024 维）做 Embedding，全库必须同模型，换模型需全量重建索引（见 `ceec/04-rag-ingestion.md`） |
| 多头注意力 | API Testing Agent 中 LLM 需同时关注 PRD 文档多个段落 + MR 评论，多头自然分工 |
| 位置编码 RoPE | Qwen 系列默认用 RoPE，支持长上下文外推，RAG 场景检索片段拼接后不易位置溢出 |
| Pre-LN 稳定性 | RAG 平台模型路由三态熔断中，流式首包探测依赖模型稳定输出，Pre-LN 比原论文 Post-LN 更不易崩 |

---

## 面试高频问题

**Q：Transformer 和 RNN 的本质区别？**
A：RNN 通过递归隐状态传递信息，必须逐步计算，无法并行；Transformer 用注意力直接建立任意两个位置间的联系，整个序列并行计算。代价是注意力是 O(n²) 复杂度，RNN 是 O(n)。

**Q：为什么 Transformer 用 LayerNorm 不用 BatchNorm？**
A：LayerNorm 沿特征维度归一化，不依赖 batch 大小，适合变长序列和 batch_size=1 的推理场景。BatchNorm 依赖 batch 统计量，batch 小时不稳定。

**Q：Decoder-Only 为什么成为主流？**
A：三点原因：(1) 生成任务（对话、代码）是最大应用场景，Decoder-Only 天然适配；(2) 输入和输出是同一条文本，Masked Self-Attention 同时完成理解和生成，不需要 Encoder；(3) 架构更简单，训练更稳定，更容易扩展。GPT/LLaMA/Qwen/DeepSeek 都用 Decoder-Only。

**Q：BERT 为什么不需要 Decoder？**
A：理解任务不需要生成，Encoder 的双向注意力能看全句，理解比单向更充分。Decoder 做单向是为防止生成时信息泄露，理解任务没有这个问题，放开双向更合理。

**Q：Decoder 也能做语义理解吗？**
A：能，靠多层信息累积——单向注意力下信息从左到右逐步传递，多层堆叠后间接获取全局信息。同等规模不如双向精确，但模型够大时差距被规模抹平。

**Q：Decoder 为什么有两种注意力？**
A：Masked Self-Attention 解决目标序列自身的衔接连贯（只看已生成的词），Cross-Attention 解决当前翻译位置应参考源序列哪部分信息（看 Encoder 输出）。两种注意力各司其职。

**Q：残差连接为什么重要？**
A：保证梯度有直通路径回传到浅层，解决深度网络梯度消失。没有残差连接，6 层以上 Transformer 几乎无法训练。

**Q：FFN 在 Transformer 中的角色？**
A：注意力负责 token 间的信息交互（"开会"），FFN 负责每个 token 独立的特征变换和非线性映射（"会后消化"）。两者交替堆叠构成 Transformer 层。

**Q：Embedding 向量为什么能表达语义？**
A：通过海量语料训练，语义相近的词在向量空间中被优化到相近位置，语义无关的词远离。向量维度越高，能表达的语义关系越精细。经典例子：国王 − 男人 + 女人 ≈ 王后。

---

## 一句话总结

Transformer = 堆叠的(多头自注意力 + FFN + 残差 + LayerNorm)，用注意力替代 RNN 的递归实现并行计算，Decoder-Only 成为现代 LLM 主流架构，RoPE 和 Pre-LN 是当前最佳实践。
