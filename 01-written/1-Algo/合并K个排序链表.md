---
tags:
  - 笔试
  - 算法
  - 链表
  - 堆
category: 笔试/Algo
---

# 合并 K 个排序链表（LeetCode 23）

> 一句话：**用最小堆同时管住 K 个链表的头结点，每次弹最小的接上并把它的下一个入堆**。

## 题目

合并 K 个升序链表为一个升序链表。如 `[[1,4,5],[1,3,4],[2,6]]` → `1→1→2→3→4→4→5→6`。

## 解法一：最小堆（好写、通用）

先把每个链表的头结点放进最小堆。每次弹出堆顶（当前全局最小），接到结果链表，再把它的 `next` 入堆。因为结点不能直接比较，堆里存 `(值, 序号, 结点)`，序号用于值相等时打破平局。

```python
import heapq

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next

def mergeKLists(lists):
    heap = []
    for i, node in enumerate(lists):
        if node:
            heapq.heappush(heap, (node.val, i, node))  # i 防止值相等时比较结点报错
    dummy = ListNode()
    cur = dummy
    while heap:
        val, i, node = heapq.heappop(heap)
        cur.next = node
        cur = cur.next
        if node.next:
            heapq.heappush(heap, (node.next.val, i, node.next))
    return dummy.next
```

## 解法二：分治两两合并

把 K 个链表两两合并（复用 LC21 合并两个有序链表），像归并一样 logK 轮合完，时间同为 O(Nlog K)。

## 记忆锚点

- 堆里存 **`(值, 序号, 结点)`**：序号是关键，否则值相等时 Python 会去比较 `ListNode` 而报错。
- 堆容量始终 ≤ K，弹一个补一个（补它的 next）。

## 复杂度

- 时间 O(N log K)（N 为总结点数），空间 O(K)

## 一句话总结

> 最小堆里放 K 个链表头（存 值,序号,结点），每次弹最小接上再把其 next 入堆——K 路归并标准解。
