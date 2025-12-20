# InnoDB 引擎如何避免幻读现象

MySQL InnoDB 引擎的默认隔离级别虽然是「可重复读」，但是它很大程度上避免幻读现象。

解决方案分为两种：

- 针对**快照读**（普通 select 语句）

通过 MVCC 一致性试图机制避免幻读，新插入的数据的 `trx_id` 不在 Read View 可见范围内，这些新行对当前事务不可见。

所以查询结果集不会发生变化，不会出现幻读。

- 针对**当前读**（读取的是最新已经提交的数据版本）

```sql
SELECT ... FOR UPDATE
SELECT ... LOCK IN SHARE MODE
UPDATE
DELETE
```

通过 Next-Key Lock （记录锁 + 间隙锁）方式。

对当前读自动加 Next-Key Lock，其他事务在 Next-Key Lock 锁范围内插入记录时会被阻塞，无法成功插入，从而避免了幻读现象。

