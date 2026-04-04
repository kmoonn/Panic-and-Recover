# 事务

事务是由 MySQL 引擎来实现的。

只有 InnoDB 引擎支持事务，原生的 MyISAM 引擎不支持事务。

DDL 语句会隐式提交事务
`ALTER TABLE、CREATE TABLE、DROP TABLE`

## 使用场景

- 转账、扣款
- 订单创建 + 库存扣减
- 多表一致性更新
- 业务补偿机制（结合回滚）

## 长事务危害

- Undo Log 膨胀
- 锁占用时间长
- 主从延迟