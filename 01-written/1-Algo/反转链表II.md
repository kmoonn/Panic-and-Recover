---
tags:
  - 笔试
  - 算法
  - 链表
category: 笔试/Algo
---

# 反转链表 II（LeetCode 92）

> 一句话：**先走到 left 前一个位置，再用"头插法"把 left~right 段逐个翻到前面**。

## 题目

反转链表中从位置 `left` 到 `right` 的部分（1 开始计数）。如 `1→2→3→4→5, left=2, right=4` → `1→4→3→2→5`。

## 哑结点 + 头插法

设哑结点 `dummy`，先让 `prev` 走到 `left` 的**前一个**结点。此后固定 `prev`，把 `cur`（`left` 位置）后面的结点一个个"摘下来插到 `prev` 后面"，做 `right-left` 次，这段就翻转了。

```python
class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next

def reverseBetween(head, left, right):
    dummy = ListNode(0, head)
    prev = dummy
    for _ in range(left - 1):     # 走到 left 前一个
        prev = prev.next
    cur = prev.next               # cur 是要反转段的第一个（始终不动，逐渐后移）
    for _ in range(right - left): # 头插 right-left 次
        nxt = cur.next            # 摘下 cur 的下一个
        cur.next = nxt.next       # cur 跳过它
        nxt.next = prev.next      # nxt 插到 prev 后面
        prev.next = nxt
    return dummy.next
```

## 记忆锚点

- **哑结点**处理 `left=1` 的边界，`prev` 停在反转段前一位后就不动了。
- 头插法核心三行：摘下 `nxt`、`cur` 跳过、`nxt` 插到 `prev` 后。`cur` 会自然沉到段尾。

## 复杂度

- 时间 O(n)（一趟），空间 O(1)

## 一句话总结

> 哑结点起手，prev 停在 left 前一位，把后续结点逐个头插到 prev 后共 right-left 次——一趟完成局部反转。
