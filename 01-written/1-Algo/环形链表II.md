---
tags:
  - 笔试
  - 算法
  - 链表
  - 双指针
category: 笔试/Algo
---

# 环形链表 II（LeetCode 142）

> 一句话：**快慢指针找相遇点，再让一个指针从头走，与慢指针同速再相遇即环入口**。

## 题目

判断链表是否有环，若有则返回**环的入口结点**，否则返回 `None`。

## 快慢指针（Floyd 判圈）

- 第一步：`slow` 每次走 1 步，`fast` 每次走 2 步。若相遇说明有环；`fast` 到头（`None`）说明无环。
- 第二步：相遇后，让 `p` 从**头结点**出发，`slow` 留在相遇点，两者都每次走 1 步，**再次相遇处就是环入口**（数学可证）。

```python
class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next

def detectCycle(head):
    slow = fast = head
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
        if slow == fast:                # 相遇，有环
            p = head
            while p != slow:            # 一个从头，一个从相遇点，同速
                p = p.next
                slow = slow.next
            return p                    # 环入口
    return None                         # fast 到头，无环
```

## 记忆锚点

- 两步走：**先快慢相遇判环，再头结点与相遇点同速找入口**。
- 为什么成立：头到入口的距离 = 相遇点绕到入口的距离（设环长 L、头到入口 a，可推导）。
- 循环条件 `fast and fast.next` 防止走两步时空指针。

## 复杂度

- 时间 O(n)，空间 O(1)

## 一句话总结

> 快慢指针相遇判有环，再让新指针从头与慢指针同速走，交点就是环入口——Floyd 判圈经典两段式。
