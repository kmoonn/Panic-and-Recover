---
tags:
  - 笔试
  - 算法
  - 堆
  - 快速选择
category: 笔试/Algo
---

# 数组中的第 K 个最大元素（LeetCode 215）

> 一句话：**维护一个大小为 K 的最小堆，堆顶就是第 K 大**。

## 题目

返回未排序数组中第 `k` 个最大的元素。如 `[3,2,1,5,6,4], k=2` → `5`。

## 解法一：小顶堆（面试最好写）

维护容量为 `k` 的最小堆：遍历所有数，堆不满就进，满了就和堆顶比——比堆顶大才替换。最终堆里是最大的 K 个数，**堆顶（最小的那个）就是第 K 大**。

```python
import heapq

def findKthLargest(nums, k):
    heap = []                      # 最小堆
    for x in nums:
        heapq.heappush(heap, x)
        if len(heap) > k:          # 超过 k 个就弹掉最小的
            heapq.heappop(heap)
    return heap[0]                 # 堆顶 = 第 K 大
```

> 一行速记版：`heapq.nlargest(k, nums)[-1]`。

## 解法二：快速选择（平均 O(n)）

借用快排 partition：一次划分后基准落到最终位置 `p`，若 `p` 正好是目标下标就返回，否则只递归一侧。

```python
import random

def findKthLargest(nums, k):
    target = len(nums) - k         # 第 k 大 = 升序第 (n-k) 位
    lo, hi = 0, len(nums) - 1
    while True:
        p = partition(nums, lo, hi)
        if p == target:
            return nums[p]
        elif p < target:
            lo = p + 1
        else:
            hi = p - 1

def partition(nums, lo, hi):
    r = random.randint(lo, hi)     # 随机基准防退化
    nums[r], nums[hi] = nums[hi], nums[r]
    pivot = nums[hi]
    i = lo
    for j in range(lo, hi):
        if nums[j] < pivot:
            nums[i], nums[j] = nums[j], nums[i]
            i += 1
    nums[i], nums[hi] = nums[hi], nums[i]
    return i
```

## 记忆锚点

- 求第 K **大**用**小顶堆**（堆顶是这 K 个里最小的，即第 K 大）；求第 K 小则反过来用大顶堆。
- 快速选择只递归一侧，平均 O(n)；面试时间紧就写堆。

## 复杂度

- 堆：时间 O(n log k)，空间 O(k)
- 快速选择：平均 O(n)，最坏 O(n²)，空间 O(1)

## 一句话总结

> 求第 K 大就维护容量 K 的小顶堆、堆顶即答案；要 O(n) 就用随机基准的快速选择只递归一侧。
