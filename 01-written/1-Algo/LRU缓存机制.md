---
tags:
  - 笔试
  - 算法
  - 链表
  - 哈希表
  - 设计
category: 笔试/Algo
---

# LRU 缓存机制（LeetCode 146）

> 一句话：**哈希表 + 双向链表**——哈希表 O(1) 定位，双向链表维护"最近使用"顺序（头=最新，尾=最久）。

## 题目

设计 LRU 缓存，`get` 和 `put` 都要 O(1)。容量满时淘汰最久未使用的。

## 为什么是"哈希表 + 双向链表"

| 需求 | 谁解决 |
|---|---|
| O(1) 按 key 查值 | 哈希表 |
| O(1) 把某节点移到"最新" | 双向链表（有 prev 指针才能 O(1) 删任意节点） |
| O(1) 淘汰最久未用 | 双向链表尾部 |

> 单链表删节点要找前驱是 O(n)，所以必须**双向**链表。

## Python 偷懒写法：OrderedDict

`collections.OrderedDict` 本身就是"哈希表 + 双向链表"，面试可直接用：

```python
from collections import OrderedDict

class LRUCache:
    def __init__(self, capacity):
        self.cap = capacity
        self.cache = OrderedDict()

    def get(self, key):
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)      # 访问后移到末尾（标记为最新）
        return self.cache[key]

    def put(self, key, value):
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.cap:
            self.cache.popitem(last=False)  # 弹出最前（最久未用）
```

## 记忆锚点

- `move_to_end(key)`：访问/更新后把它标记为最新（放末尾）。
- `popitem(last=False)`：淘汰最久未用（队首）。
- get 命中要 move_to_end；put 已存在也要 move_to_end，再判断是否超容淘汰。

> 若面试要求手写双向链表：用一个带 `head`/`tail` 哨兵节点的双向链表，封装 `_remove(node)` 和 `_add_to_head(node)` 两个私有方法即可。

## 复杂度

- get / put 均 O(1)，空间 O(capacity)

## 一句话总结

> 哈希表定位 + 双向链表排序，头新尾旧；Python 用 OrderedDict 的 move_to_end / popitem(last=False) 两招搞定。
