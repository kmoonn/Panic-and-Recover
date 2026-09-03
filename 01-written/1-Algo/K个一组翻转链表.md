---
tags:
  - 笔试
  - 算法
  - 链表
category: 笔试/Algo
---

# K 个一组翻转链表（LeetCode 25）

> 一句话：**每 K 个一组，先数够 K 个，翻转这一组，再递归接上后面**。递归写法最好理解。

## 题目

每 K 个节点一组翻转链表，不足 K 个的保持原样。

## 递归解法（最好记）

思路分三步，每组都一样：

1. 先走 K 步，看**够不够 K 个**（不够就原样返回，作为最后一组）；
2. 翻转当前这 K 个节点；
3. 翻转后这组的尾（原来的头 `head`）接上"递归处理剩余部分"的结果。

```python
def reverseKGroup(head, k):
    # 1. 检查是否有 k 个节点
    node = head
    for _ in range(k):
        if not node:
            return head          # 不足 k 个，原样返回
        node = node.next
    # node 现在指向第 k+1 个（下一组的头）

    # 2. 翻转本组的 k 个节点（标准链表反转）
    prev = None
    cur = head
    for _ in range(k):
        cur.next, prev, cur = prev, cur, cur.next
    # 翻转后：prev 是本组新头，head 是本组新尾，cur 指向下一组头

    # 3. 本组尾 head 接上递归处理的下一组
    head.next = reverseKGroup(cur, k)
    return prev
```

## 三个记忆锚点

- **先数够 K 个再翻**：数不够直接原样返回（处理尾部不足 K 的组）。
- **翻转后 head 变尾**：原来的头 `head` 翻转后是本组末节点，用它接下一组。
- **返回 prev**：翻转后 `prev` 是本组新头。

## 复杂度

- 时间 O(n)（每节点访问常数次），空间 O(n/k) 递归栈

## 一句话总结

> 数够 K 个 → 翻转本组 → 原头变尾接递归结果 → 返回新头 prev；不足 K 个原样返回。
