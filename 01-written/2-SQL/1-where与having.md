---
tags:
  - 数据库
  - SQL
category: 数据库/SQL
---

# where 与 having 的区别

## Q: where 和 having 有什么区别？

| 对比维度 | WHERE | HAVING |
|---------|-------|--------|
| 作用时机 | 分组**之前**过滤 | 分组**之后**过滤 |
| 过滤对象 | 原始行 | 分组后的结果集 |
| 能否用聚合函数 | ❌ 不能 | ✅ 能 |
| 是否依赖 GROUP BY | 不依赖 | 一般与 GROUP BY 连用 |
| 性能 | ✅ 优先用，先过滤再分组，减少分组计算量 | ⚠️ 先分组再过滤，分组数据量更大 |

---

## 执行顺序

```sql
SELECT column, aggregate_func()
FROM table
WHERE condition        -- ① 先过滤原始行
GROUP BY column
HAVING condition       -- ② 再过滤分组
ORDER BY column;
```

```
FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY
         ↑                      ↑
      分组前过滤              分组后过滤
```

## 示例

```sql
-- 查询订单金额 > 100 的用户，且其总消费 > 1000
SELECT user_id, SUM(amount) AS total
FROM orders
WHERE amount > 100          -- 先过滤掉小订单行
GROUP BY user_id
HAVING SUM(amount) > 1000;  -- 再过滤总消费不足的分组
```

### 典型错误

```sql
-- ❌ WHERE 中不能使用聚合函数
WHERE SUM(amount) > 1000    -- 报错：无效使用聚合函数

-- ✅ 聚合过滤用 HAVING
HAVING SUM(amount) > 1000   -- 正确
```

## 性能原则

> **过滤原始行优先用 WHERE，性能更好。**

- WHERE 在 GROUP BY 之前执行，先缩小数据量再分组 → 分组计算量小
- HAVING 在 GROUP BY 之后执行，先全量分组再过滤 → 分组计算量大
- 能用 WHERE 过滤的条件不要放到 HAVING 中

```sql
-- ✅ 好的写法：先用 WHERE 缩小范围
SELECT dept, AVG(salary)
FROM employees
WHERE salary > 3000      -- 先过滤低薪行
GROUP BY dept
HAVING AVG(salary) > 5000;

-- ❌ 差的写法：把行级过滤放到 HAVING
SELECT dept, AVG(salary)
FROM employees
GROUP BY dept
HAVING AVG(salary) > 5000 AND salary > 3000;  -- salary 行级条件不应在这里
```

## 一句话总结

> WHERE 分组前过滤原始行（不能用聚合函数），HAVING 分组后过滤分组结果（能用聚合函数）；过滤原始行优先用 WHERE，性能更好。
