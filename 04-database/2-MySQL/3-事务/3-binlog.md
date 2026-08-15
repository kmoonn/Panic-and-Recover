---
tags:
  - MySQL
  - 事务
category: 数据库/MySQL/事务
---

# binlog

## Q：什么是 binlog？

binlog（二进制日志）是 MySQL **Server 层**的逻辑日志，记录了所有对数据库执行更改的 DDL 和 DML 操作（不含 SELECT 和 SHOW）。

- 以事件形式记录，如"对某表执行了一条 UPDATE"
- 主要用途：**主从复制** 和 **数据恢复(PITR)**
- 不是 InnoDB 引擎特有的，所有存储引擎的修改都会记录

```sql
-- binlog 记录的是逻辑操作，类似：
"对 test_db.user 表执行 UPDATE user SET age=30 WHERE id=1"
```

---

## Q：binlog 与 redo log 的区别？

| 对比维度 | binlog | redo log |
|---|---|---|
| **所属层级** | Server 层 | InnoDB 引擎层 |
| **日志内容** | 逻辑日志（记录 SQL 语句或行数据变更） | 物理日志（记录页的物理修改） |
| **核心用途** | 主从复制、数据恢复 | 崩溃恢复（crash-safe） |
| **写入方式** | 追加写入，文件写满换新文件，不会覆盖 | 循环写入，固定大小，会覆盖旧数据 |
| **生命周期** | 持久保存（可配置过期时间） | 只保留最近一段，用于崩溃恢复 |
| **事务相关** | 事务提交时一次性写入 | 事务执行过程中持续写入 |
| **引擎通用性** | 所有引擎通用 | 仅 InnoDB 有 |

---

## Q：binlog 的三种格式？

| 格式 | 记录内容 | 优点 | 缺点 |
|---|---|---|---|
| **STATEMENT** | 记录 SQL 语句 | 日志量小 | 可能主从不一致（如 `NOW()`、`UUID()`、`LIMIT` 无序等） |
| **ROW** | 记录行数据的变更（修改前后的值） | 数据一致性有保证 | 日志量大（批量 UPDATE 会记录每行变化） |
| **MIXED** | 默认 STATEMENT，遇到不安全语句自动切 ROW | 折中方案 | 仍有不一致风险，判断逻辑复杂 |

> 推荐使用 **ROW** 格式，保证主从数据一致性。

---

## Q：binlog 的用途？

### 1. 主从复制

```
主库(Master)                    从库(Slave)
    │                               │
    │  写入 binlog                   │
    │                               │
    │  ──── binlog dump ────────►   │
    │                               │  写入 relay log
    │                               │
    │                               │  SQL 线程回放 relay log
    │                               │
```

- 主库将 binlog 发送给从库
- 从库 IO 线程写入 relay log，SQL 线程回放执行
- 实现数据同步

### 2. 数据恢复（PITR，Point-In-Time Recovery）

```bash
# 先恢复全量备份
mysql < full_backup.sql

# 再通过 binlog 增量恢复到指定时间点
mysqlbinlog --start-datetime="2024-01-01 00:00:00" \
            --stop-datetime="2024-01-01 12:00:00" \
            mysql-bin.000001 | mysql
```

- 可以精确恢复到某个时间点
- 前提是保留了完整的 binlog 文件

---

## Q：什么是两阶段提交？

MySQL 在事务提交时，需要同时写 redo log 和 binlog，为保证两者一致性，采用**两阶段提交(2PC)**：

### 流程

```
                    ┌──────────────────┐
                    │  事务执行过程中    │
                    │  写入 redo log    │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
              ┌─────│   第一阶段：       │
              │     │   redo log 状态    │
              │     │   设为 prepare    │
              │     └────────┬─────────┘
              │              │
              │     ┌────────▼─────────┐
              │     │   写入 binlog     │
              │     └────────┬─────────┘
              │              │
              │     ┌────────▼─────────┐
  事务提交 ───┤     │   第二阶段：       │
              │     │   redo log 状态    │
              │     │   设为 commit     │
              │     └──────────────────┘
```

### 为什么需要两阶段提交？

如果不用两阶段提交，可能出现 redo log 和 binlog 不一致：

| 场景 | 后果 |
|---|---|
| 先写 redo log 后写 binlog，redo log 写完崩溃 | 主库有数据，但 binlog 没记录 → 从库丢失该数据 → 主从不一致 |
| 先写 binlog 后写 redo log，binlog 写完崩溃 | 从库有数据，但主库没有 → 从库多数据 → 主从不一致 |

两阶段提交保证：**即使崩溃，也能通过 redo log 和 binlog 的状态判断是否需要提交或回滚**。

### 崩溃恢复规则

| redo log 状态 | binlog 状态 | 处理方式 |
|---|---|---|
| prepare + binlog 完整 | 有对应记录 | 提交事务 |
| prepare + binlog 不完整 | 无对应记录 | 回滚事务 |
| commit | — | 已提交，无需处理 |

> 一句话总结：binlog 是 MySQL Server 层的逻辑日志，用于主从复制和数据恢复，与 InnoDB 的 redo log 通过两阶段提交保证一致性——先写 redo log(prepare)，再写 binlog，最后 commit，确保崩溃恢复时两者不矛盾。
