# 多版本并发控制 MVCC

实现原理

- 快照读

普通的select语句，通过 MVCC 机制实现。

事务启动时创建一个数据快照，后续的快照读都读取这个版本的数据，从而避免了看到其他事务新插入的行（幻读）或修改的行（不可重复读）。

- 当前读

像 SELECT ... FOR UPDATE, SELECT ... LOCK IN SHARE MODE, INSERT, UPDATE, DELETE 这些操作。

InnoDB 使用 Next-Key Lock 来锁定扫描到的索引记录及其间的范围（间隙），防止其他事务在这个范围内插入新的记录，从而避免幻读。
