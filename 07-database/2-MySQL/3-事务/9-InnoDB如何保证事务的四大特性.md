---
tags:
  - MySQL
  - 事务
category: 数据库/MySQL/事务
---

# InnoDB 引擎如何保证事务的四大特性

- 原子性
  - 通过 undo log 回滚日志保证
- 一致性
  - 通过持久性 + 原子性 + 隔离性来保证
- 隔离性
  - 通过 MVCC 多版本并发控制或锁机制保证
- 持久性
  - 通过 redo log 重做日志保证

## 一句话总结

> 原子性靠undo log回滚、隔离性靠MVCC和锁机制、持久性靠redo log刷盘，三者共同保证一致性。
