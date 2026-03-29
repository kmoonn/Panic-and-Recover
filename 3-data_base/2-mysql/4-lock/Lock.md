# 锁机制

## 行锁（InnoDB）

- 共享锁（S Lock）
- 排他锁（X Lock）

## 间隙锁 / Next-Key Lock

- 防止**幻读**
- 主要在 **repeatable read** 隔离级别下生效