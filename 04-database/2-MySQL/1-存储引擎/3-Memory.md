---
tags:
  - MySQL
  - 存储引擎
category: 数据库/MySQL/存储引擎
---

# Memory

## Memory 存储引擎是什么？

Memory（原名 HEAP）是 MySQL 提供的**内存表存储引擎**，数据全部存储在内存中，访问速度极快，但服务器重启后数据会丢失。

## 核心特点

| 特点 | 说明 |
|---|---|
| 纯内存存储 | 数据存在内存中，不写磁盘 |
| 极快访问速度 | 无磁盘 I/O，读写速度极高 |
| 表级锁 | 写操作锁整张表 |
| 不支持事务 | 无 ACID 保证 |
| 不支持 BLOB/TEXT | 内存表不支持大字段类型 |
| 重启后数据丢失 | MySQL 重启后数据消失，表结构保留 |
| 固定行长度 | 使用固定长度行格式，VARCHAR 按定长存储 |

## 索引类型

Memory 引擎支持两种索引，默认使用 Hash 索引：

| 索引类型 | 特点 | 适用场景 |
|---|---|---|
| Hash 索引（默认） | 等值查询 O(1)，不支持范围查询和排序 | 等值查找（WHERE key = ?） |
| B-Tree 索引 | 支持范围查询和排序，等值查询 O(log n) | 范围查询（WHERE key > ?） |

```sql
-- 默认 Hash 索引
CREATE TABLE cache_table (
    id INT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE = Memory;

-- 指定 B-Tree 索引
CREATE TABLE lookup_table (
    id INT PRIMARY KEY,
    score INT,
    INDEX idx_score (score) USING BTREE
) ENGINE = Memory;
```

### Hash 索引 vs B-Tree 索引

| 对比项 | Hash 索引 | B-Tree 索引 |
|---|---|---|
| 等值查询 | O(1)，极快 | O(log n) |
| 范围查询 | 不支持 | 支持 |
| 排序 | 不支持 | 支持 |
| 最左前缀 | 不支持 | 支持 |
| 冲突处理 | 链地址法 | — |
| 存储空间 | 较小 | 较大 |

## Memory 表与临时表（TEMPORARY TABLE）的区别

| 对比项 | Memory 表 | TEMPORARY TABLE |
|---|---|---|
| 可见性 | 所有会话可见 | 仅创建会话可见 |
| 生命周期 | MySQL 重启后数据丢失 | 会话结束自动删除（结构和数据） |
| 存储引擎 | 固定使用 Memory 引擎 | 可指定任意引擎（默认 InnoDB） |
| 表名冲突 | 不能与已有表同名 | 可以同名（会话隔离） |
| 删除方式 | DROP TABLE | 会话结束自动删除或 DROP TEMPORARY TABLE |

```sql
-- Memory 表：所有会话可见，重启丢数据
CREATE TABLE mem_cache (id INT, val VARCHAR(100)) ENGINE = Memory;

-- 临时表：仅当前会话可见，会话结束自动删除
CREATE TEMPORARY TABLE temp_result (id INT, val VARCHAR(100));
```

## 适用场景

- **临时缓存**：缓存中间计算结果
- **Lookup 表**：快速等值查找的映射表（如地区代码映射）
- **会话级临时数据**：分析时的中间结果集
- **高频读取的小表**：数据量小、读取极频繁的配置表

## 注意事项

1. Memory 表的数据在 MySQL 重启后**全部丢失**，只保留表结构
2. 不支持 BLOB/TEXT 等大字段，VARCHAR 存储时按定长分配空间
3. 表级锁限制了并发写入性能
4. 服务器内存不足时可能触发交换，反而降低性能
5. 不适合存储重要数据，仅用于临时/缓存场景

## 一句话总结

Memory 是 MySQL 的纯内存存储引擎，以极快的访问速度为优势，但受限于表级锁、不支持事务、不支持 BLOB/TEXT、重启后数据丢失，适合用作临时缓存和 Lookup 表，不适合存储持久化数据。
