---
tags:
  - 笔试
  - 算法
  - 数组
  - 模拟
category: 笔试/Algo
---

# 螺旋矩阵 II（LeetCode 59）

> 一句话：**四个边界 + 转圈填数**——按 右→下→左→上 顺序填，填完一条边界就收缩一格。

## 题目

给定 n，生成 1~n² 按顺时针螺旋填充的 n×n 矩阵。

## 边界收缩法（最直观）

维护 `top / bottom / left / right` 四个边界，按顺时针填一圈、收缩一格，直到填满。

```python
def generateMatrix(n):
    m = [[0] * n for _ in range(n)]
    top, bottom, left, right = 0, n - 1, 0, n - 1
    num = 1
    while num <= n * n:
        for j in range(left, right + 1):   # 上边：从左到右
            m[top][j] = num; num += 1
        top += 1
        for i in range(top, bottom + 1):   # 右边：从上到下
            m[i][right] = num; num += 1
        right -= 1
        for j in range(right, left - 1, -1):  # 下边：从右到左
            m[bottom][j] = num; num += 1
        bottom -= 1
        for i in range(bottom, top - 1, -1):  # 左边：从下到上
            m[i][left] = num; num += 1
        left += 1
    return m
```

## 记忆锚点

- 顺序固定：**上→右→下→左**（右、下、左、上四个方向），每填完一条就把对应边界向内收一格。
- 循环条件 `num <= n*n`：填满 n² 个数就停。
- 四条边的遍历方向要对：下边和左边是**倒序** range（`-1` 步长）。

> 螺旋矩阵 I（54，读取而非生成）是同一套边界收缩，把"填 num"换成"读取 append"即可。

## 复杂度

- 时间 O(n²)（每格填一次），空间 O(1)（不计输出）

## 一句话总结

> 四边界转圈填：上→右→下→左，每条填完收缩一格，`num<=n²` 停——I 和 II 同一套模板。
