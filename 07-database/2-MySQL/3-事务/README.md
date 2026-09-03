# 事务

事务是由 MySQL 引擎来实现的。

只有 InnoDB 引擎支持事务，原生的 MyISAM 引擎不支持事务。

DDL 语句会隐式提交事务
`ALTER TABLE、CREATE TABLE、DROP TABLE`

## 日志

- [redo日志](redo日志.md)
- [undo日志](undo日志.md)
- [binlog](binlog.md)

## 核心概念

- [事务四大特性ACID](事务四大特性ACID.md)
- [事务隔离级别](事务隔离级别.md)
- [脏读、不可重复读、幻读](脏读、不可重复读、幻读.md)
- [MVCC](MVCC.md)

## InnoDB 实现

- [InnoDB解决幻读](InnoDB解决幻读.md)
- [InnoDB如何保证事务的四大特性](InnoDB如何保证事务的四大特性.md)

## 使用场景

- 转账、扣款
- 订单创建 + 库存扣减
- 多表一致性更新
- 业务补偿机制（结合回滚）

## 长事务危害

- Undo Log 膨胀
- 锁占用时间长
- 主从延迟