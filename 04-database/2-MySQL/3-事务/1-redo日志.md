---
tags:
  - MySQL
  - 事务
category: 数据库/MySQL/事务
---

# redo 日志

## Q：什么是 redo log？

redo log（重做日志）是 InnoDB 存储引擎的**物理日志**，记录的是"**某个数据页上做了什么修改**"。

- 记录形式：`表空间号 + 页号 + 偏移量 + 修改的数据`
- 是 WAL(Write-Ahead Logging) 原则的核心实现
- 保证了事务的**持久性(Durability)**

```sql
-- redo log 记录的是物理修改，类似：
"把表空间 67 的第 8 号页偏移 100 处的值从 0x01 改为 0x02"
```

---

## Q：为什么需要 redo log？

### 问题背景

事务提交后，修改的数据页（脏页）在 Buffer Pool 中，不一定立刻刷盘。如果在刷盘前 MySQL 崩溃，已提交事务的数据就丢失了，违反持久性。

### WAL(Write-Ahead Logging) 原则

**先写日志，再写磁盘**：

1. 事务修改数据时，先将修改记录写入 redo log（顺序写，很快）
2. 脏页可以在之后合适的时间异步刷盘（随机写，较慢）
3. 崩溃后通过 redo log 重放恢复数据

### 核心收益

| 收益 | 说明 |
|---|---|
| **保证持久性** | 崩溃后通过 redo log 重放恢复已提交事务的修改 |
| **性能优化** | 将随机写（刷脏页）转化为顺序写（写 redo log），大幅提升性能 |
| **延迟刷盘** | 脏页不必每次提交就刷盘，可批量异步刷，减少 IO |

---

## Q：redo log 的写入过程？

redo log 的写入经过三个阶段：

```
事务修改数据
     │
     ▼
┌──────────────┐
│ redo log     │  ← 内存中，事务执行过程中不断写入
│ buffer       │
└──────┬───────┘
       │ 刷写到 OS 缓存
       ▼
┌──────────────┐
│ OS buffer    │  ← 操作系统缓存，可能尚未 fsync 到磁盘
│ (page cache) │
└──────┬───────┘
       │ fsync 刷盘
       ▼
┌──────────────┐
│ redo log     │  ← 磁盘上的 redo log 文件
│ file         │
└──────────────┘
```

---

## Q：redo log 的刷盘策略？

由参数 `innodb_flush_log_at_trx_commit` 控制：

| 值 | 行为 | 安全性 | 性能 |
|---|---|---|---|
| **0** | 每秒将 redo log buffer 刷到 OS buffer 并 fsync | 最低（崩溃可能丢 1 秒数据） | 最高 |
| **1** | 每次事务提交都将 redo log buffer 刷到 OS buffer 并 fsync | 最高（不丢数据） | 最低 |
| **2** | 每次事务提交将 redo log buffer 刷到 OS buffer，每秒 fsync | 中等（OS 崩溃丢数据，MySQL 崩溃不丢） | 中等 |

> 生产环境推荐设为 **1**，保证 ACID 的持久性。

---

## Q：redo log 是固定大小、循环写的？

是的。redo log 采用**固定大小 + 循环写入**的方式：

- InnoDB 的 redo log 是一组固定大小的文件（如 `ib_logfile0`、`ib_logfile1`）
- 两个关键指针：

```
┌──────────────────────────────────────────────┐
│  ib_logfile0          ib_logfile1             │
│  ┌──────────────────────────────────────┐    │
│  │ checkpoint →  [可覆盖]  write pos →  │    │
│  │                [已写入未刷盘的数据]    │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘

write pos：当前写入位置，循环移动
checkpoint：当前要擦除的位置，循环移动
```

- write pos 追上 checkpoint 时需要先推进 checkpoint（将脏页刷盘），才能继续写入
- 整个 redo log 文件组可用空间 = 总大小 - (write pos - checkpoint)

---

## Q：redo log 如何实现 crash-safe？

### crash-safe 原理

1. 事务提交时，redo log 已按 `innodb_flush_log_at_trx_commit=1` 刷到磁盘
2. MySQL 崩溃重启后，扫描 redo log 文件
3. 从 checkpoint 位置开始**重放(redo)**所有已提交事务的修改
4. 对于未提交事务的修改，通过 undo log 回滚

### 恢复流程

```
MySQL 崩溃重启
     │
     ▼
从 checkpoint 位置开始扫描 redo log
     │
     ▼
┌─────────────────────────────┐
│ redo log 记录的修改          │
│ ├── 已提交事务 → 重放(redo)  │
│ └── 未提交事务 → 回滚(undo)  │
└─────────────────────────────┘
     │
     ▼
数据恢复到崩溃前的一致状态
```

> 一句话总结：redo log 是 InnoDB 的物理日志，记录数据页的物理修改，通过 WAL 原则（先写日志再写磁盘）将随机写转为顺序写来提升性能，同时保证崩溃后可通过重放恢复已提交事务的数据，实现 crash-safe。
