# 🔍 int(1) 和 int(10) 区别是什么 ? int 类型到底需不需要设置长度 ?

## 📅 日期与标签

- 记录日期：2025-04-25
- 标签：`#MySQL` `#int类型` `#数据库`

## 1. 🧢 前言

- 使用场景

我们在开发项目时，数据库建立 user 用户表几乎是不可或缺的。
user 表常常需要添加主键 user_id 字段，作为用户唯一性标识。
有的时候用户量特别大，user_id 字段可能很大，就需要我们设计好 user_id 字段的数据类型和数据范围。

- 背景介绍

一般我们在 MySQL 中使用 int 类型作为 user_id 的字段类型，对于 `int(x)` 后面这个`x` 需不需要填，填多少一直是一个疑惑。不同于 `char` 字段类型，`char(x)` 就表示该字段最多存储 `x` 长度的字符。

## 2. 🛠 方法

- 数据说话

我们知道在MySQL中int占4个字节，对于无符号int，最大值是`2^32-1=4294967295`,将近40亿，可见使用int(1)就已经很够用了。

```sql
CREATE TABLE `user` (
  `id` int(1) unsigned NOT NULL AUTO_INCREMENT, -- 无符号int类型
   PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
```
id字段为无符号的int(1)，我们插入一个最大值试试看。
```sql
mysql> insert into user (id) values (4294967295);
Query OK, 1 row affected (0.02 sec)

mysql> select * from user;
+------------+
| id         |
+------------+
| 4294967295 |
+------------+
1 row in set (0.00 sec)
```
可以看到成功了，说明int后面的数字不影响int本身支持的大小，int(x)都一样。

## 3. 💡 总结

### 零填充

一般int后面的数字，配合 `zerofill` 一起使用才有效。
```sql
CREATE TABLE `user` (
  `id` int(4) unsigned zerofill NOT NULL AUTO_INCREMENT,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
```
分别插入四个不同大小的数字
```sql
mysql> INSERT INTO `user` (`id`) VALUES (1),(10),(100),(1000);
Query OK, 4 rows affected (0.00 sec)
Records: 4  Duplicates: 0  Warnings: 0
```
查询一下
```sql
mysql> select * from user;
+------+
| id   |
+------+
| 0001 |
| 0010 |
| 0100 |
| 1000 |
+------+
4 rows in set (0.00 sec)
```
### 版本
从 MySQL 8.0.17 开始，INT(n) 中的 n 对存储和排序没有实际意义，只有在配合 ZEROFILL 使用时才影响显示。

ZEROFILL 会自动将字段设为 UNSIGNED。

[参考文章](https://mp.weixin.qq.com/s?__biz=MzUxOTc4NjEyMw==&mid=2247591941&idx=3&sn=4e2e623c563d66ce6debe934d9388338&chksm=f8a7fc16b00d3facb2e2594e8eff2231863880267ea2152b380bab155b520ab123e1a5c3c6bd&mpshare=1&scene=23&srcid=0412ZI124qZp9wrMg9PKlzDG&sharer_shareinfo=d393bfc5045d4a07bce8cd35bda9a39b&sharer_shareinfo_first=d393bfc5045d4a07bce8cd35bda9a39b#rd)