---
tags:
  - MySQL
  - 事务
category: 数据库/MySQL/事务
---

# undo 日志

## Q：什么是 undo log？

undo log（回滚日志）是 InnoDB 存储引擎的**逻辑日志**，记录的是**数据修改前的旧值**。

- 当执行 INSERT 时，undo log 记录主键值（用于 DELETE 回滚）
- 当执行 DELETE 时，undo log 记录整行旧数据（用于 INSERT 回滚）
- 当执行 UPDATE 时，undo log 记录修改前的旧值（用于反向 UPDATE 回滚）

```sql
-- 原始数据：id=1, name='张三', age=25
UPDATE user SET age = 30 WHERE id = 1;

-- undo log 记录：id=1 的旧 age=25（回滚时执行 UPDATE user SET age = 25 WHERE id = 1）
```

---

## Q：undo log 的两大作用？

| 作用 | 说明 |
|---|---|
| **回滚(Rollback)** | 事务回滚时，利用 undo log 中的旧值将数据恢复到修改前的状态 |
| **MVCC（快照读）** | 事务执行快照读时，通过 undo log 版本链找到对应版本的可见数据 |

### 回滚示例

```sql
BEGIN;
UPDATE user SET age = 30 WHERE id = 1;  -- undo log 记录 age=25
UPDATE user SET age = 40 WHERE id = 1;  -- undo log 记录 age=30
ROLLBACK;                                -- 利用 undo log 依次回滚：40→30→25
```

### MVCC 中的使用

- 每行数据的隐藏列 `roll_pointer` 指向 undo log 中的上一版本
- 多次修改形成**版本链**
- 快照读时通过 ReadView + 版本链判断哪个版本对当前事务可见

---

## Q：undo log 的类型？

| 类型 | 对应操作 | 生命周期 |
|---|---|---|
| **insert undo log** | INSERT 操作产生 | 事务提交后即可删除（INSERT 的行对其他事务不可见，无需用于 MVCC） |
| **update undo log** | UPDATE / DELETE 操作产生 | 需要保留到没有更早的快照读需要访问它（即没有更早的 ReadView 引用它） |

- insert undo log 事务提交后可立即清理
- update undo log 需要等待所有活跃事务都不再需要该版本才能清理
- 长时间运行的事务会导致 update undo log 堆积，影响性能

---

## Q：undo log 与 redo log 的区别？

| 对比维度 | undo log | redo log |
|---|---|---|
| **日志类型** | 逻辑日志（记录旧值） | 物理日志（记录页的物理修改） |
| **记录内容** | 数据修改前的值 | 数据页的物理修改操作 |
| **核心作用** | 回滚 + MVCC | 崩溃恢复（crash-safe） |
| **保证特性** | 原子性(Atomicity) | 持久性(Durability) |
| **写入方式** | 随着数据修改产生 | WAL，先写日志再写磁盘 |
| **存储位置** | 系统表空间或 undo 表空间 | redo log 文件（ib_logfile） |
| **是否覆盖** | 事务提交后可清理（update undo log 需等 MVCC 不再引用） | 循环写入，checkpoint 后可覆盖 |
| **产生关系** | undo log 也会产生 redo log | — |

---

## Q：undo log 如何支持 MVCC？

### 版本链

每行数据有两个隐藏列：
- `trx_id`：最后修改该行的事务 ID
- `roll_pointer`：指向 undo log 中该行的上一个版本

```
当前行：{id=1, name='张三', age=30, trx_id=103, roll_pointer → ──┐}
                                                                   │
Undo log 版本2：{age=25, trx_id=101, roll_pointer → ──┐}          │
                                                       │          │
Undo log 版本1：{age=20, trx_id=99, roll_pointer=null}  ◄────────┘
```

### 快照读流程

1. 事务执行快照读时，先创建 ReadView（记录当前活跃事务列表）
2. 从当前行开始，比较 `trx_id` 与 ReadView 判断可见性
3. 如果不可见，沿 `roll_pointer` 到 undo log 中的上一个版本
4. 重复判断直到找到可见版本或返回空

---

## Q：undo log 也会产生 redo log？

是的。undo log 本身也是数据页的修改，也需要持久化：

1. 写入 undo log（修改 undo 页）
2. 修改 undo 页会产生对应的 redo log
3. 所以 **undo log 的持久化靠 redo log 保证**

这意味着崩溃恢复时：
- 先通过 redo log 恢复 undo log（重放 undo 页的修改）
- 再通过恢复好的 undo log 回滚未提交事务

> 一句话总结：undo log 是逻辑日志，记录数据修改前的旧值，核心作用是回滚(保证原子性)和 MVCC(快照读)，通过版本链 + ReadView 实现多版本并发读，其持久化依赖 redo log 保证。
