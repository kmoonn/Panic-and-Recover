# InnoDB 与 MyISAM 的区别

## InnoDB

每个表一个文件 .idb（数据文件本身就是索引文件）

支持事务，具有提交、回滚事务能力

默认使用可重复读隔离级别（解决幻读问题）

支持外键

支持行级锁（默认）、表级锁

支持XA事务

支持savepoints事务（支持部分回滚机制）

支持数据库异常崩溃后的安全恢复（依赖 redo log）

支持 MVCC（行级锁的升级）

InnoDB 使用缓冲池（Buffer Pool）缓存数据页和索引页

InnoDB 的性能比 MyISAM 更强大。

## MyISAM

每个表两个文件，一个数据文件 MYD 一个索引文件 MYI（索引文件和数据文件分离）

不支持事务

不支持行级锁、只有表级锁

MyISAM 使用键缓存（Key Cache）仅缓存索引页而不缓存数据页。
