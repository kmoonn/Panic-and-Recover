---
tags:
  - MySQL
  - 事务
category: 数据库/MySQL/事务
---

# MVCC 多版本并发控制

## 是什么

MVCC（Multi-Version Concurrency Control）是 InnoDB 的并发控制机制：**读不阻塞写，写不阻塞读**。每行数据保留多个历史版本，不同事务读取各自可见的版本。

## 核心依赖

MVCC 依赖三个组件：

| 组件 | 作用 |
|---|---|
| **隐藏字段 trx_id** | 记录最近修改该行的事务 ID |
| **隐藏字段 roll_pointer** | 指向 Undo Log 中该行的上一个版本 |
| **Undo Log** | 存储行的历史版本链 |

```
当前行（trx_id=5, roll_pointer →）
  → Undo Log 版本2（trx_id=3, roll_pointer →）
    → Undo Log 版本1（trx_id=1, roll_pointer → NULL）
```

## Read View

事务执行**快照读**时创建 Read View，决定能看到哪些版本：

### Read View 四个字段

| 字段 | 含义 |
|---|---|
| `m_ids` | 创建 Read View 时，当前所有**活跃事务** ID 列表 |
| `min_trx_id` | 活跃事务中最小 ID |
| `max_trx_id` | 下一个将分配的事务 ID（最大活跃 ID + 1） |
| `creator_trx_id` | 创建该 Read View 的事务 ID |

### 可见性判断规则

对某版本的 `trx_id`：

```
if trx_id == creator_trx_id:
    → 自己修改的，可见 ✅
elif trx_id < min_trx_id:
    → 事务已提交，可见 ✅
elif trx_id >= max_trx_id:
    → 事务在 Read View 之后才开启，不可见 ❌
elif trx_id in m_ids:
    → 事务仍在活跃（未提交），不可见 ❌
else:
    → 事务已提交，可见 ✅
```

沿版本链从最新往老遍历，**第一个可见的版本就是该事务应读到的数据**。

## 快照读 vs 当前读

| | 快照读 | 当前读 |
|---|---|---|
| **SQL** | 普通 `SELECT` | `SELECT ... FOR UPDATE` / `LOCK IN SHARE MODE` / `INSERT` / `UPDATE` / `DELETE` |
| **读取内容** | MVCC 历史版本（不一定最新） | 最新已提交数据 + 加锁 |
| **加锁** | 不加锁 | 加 Next-Key Lock（记录锁 + 间隙锁） |
| **用途** | 普通查询，高并发读 | 更新/删除/加锁查询 |

## RC vs RR 隔离级别下的 MVCC

| | RC（Read Committed） | RR（Repeatable Read） |
|---|---|---|
| **Read View 生成时机** | **每次 SELECT** 都新建 Read View | **事务首次 SELECT** 时创建，后续复用 |
| **效果** | 每次 SELECT 可能读到新提交的数据 | 整个事务内读到的数据一致 |
| **能否防止不可重复读** | ❌ | ✅ |
| **能否防止幻读** | ❌ | ✅（快照读靠 MVCC，当前读靠 Next-Key Lock） |

## RR 隔离级别如何防止幻读

- **快照读**：MVCC 保证事务内始终读到同一快照版本，天然防幻读
- **当前读**：Next-Key Lock 锁住记录 + 间隙，阻止其他事务在范围内插入新行

## MVCC 能完全解决幻读吗

**不能**。存在一个经典例外：

```sql
-- 事务A
SELECT * FROM t WHERE id = 5;        -- 快照读，返回空
UPDATE t SET name='x' WHERE id = 5;  -- 当前读！会读到其他事务已提交的最新数据
SELECT * FROM t WHERE id = 5;        -- 快照读，但此时能读到了（因为 UPDATE 修改了行的 trx_id）
```

原因：`UPDATE` 是当前读，会读到其他事务新插入的行并修改它（更新 trx_id），后续快照读就能看到了。

## 版本链清理（Purge）

Undo Log 中的历史版本不能永远保留，需要 **Purge 线程**定期清理：

- 当没有更老的 Read View 需要访问某版本时，该版本可被清理
- 长事务会阻止 Purge，导致 Undo Log 膨胀 → 影响性能

## 总结

| 要点 | 说明 |
|---|---|
| 核心 | 行保留多版本 + Read View 判断可见性 |
| 依赖 | 隐藏字段（trx_id, roll_pointer）+ Undo Log |
| 快照读 | 普通 SELECT，不加锁，读历史版本 |
| 当前读 | FOR UPDATE / UPDATE / DELETE，加锁，读最新数据 |
| RC vs RR | RC 每次 SELECT 新建 Read View，RR 复用首次的 |
| 幻读 | 快照读靠 MVCC 防，当前读靠 Next-Key Lock 防，但存在例外 |


## 八股速记

- **MVCC（多版本并发控制）**：读不加锁、读写不阻塞，靠**版本链 + ReadView** 实现。
- 每行有隐藏字段 `trx_id`（事务 id）、`roll_pointer`（指向 undo log 版本链）。
- **ReadView** 判断某版本对当前事务是否可见（比较 trx_id 与活跃事务列表）。
- **快照读**：普通 `SELECT`，读的是历史快照（MVCC），不加锁。
- **当前读**：`SELECT ... FOR UPDATE`/`LOCK IN SHARE MODE`、`INSERT/UPDATE/DELETE`，读最新版本并加锁。

**⭐ 加分/易错**：RC 每次 select 都生成新 ReadView（所以不可重复读）；RR 只在**第一次** select 生成 ReadView 并复用（所以可重复读）。

## 一句话总结

> MVCC通过隐藏字段(trx_id/roll_pointer)+UndoLog+ReadView实现快照读，RC每次SELECT新建RV、RR复用首次RV。
