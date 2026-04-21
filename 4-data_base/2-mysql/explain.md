# EXPLAIN

EXPLAIN（也可写为 DESCRIBE/DESC）是 SQL 优化核心命令，用于查看数据库的查询执行计划

字段名	核心含义（优化必看）
id	查询执行的顺序编号
select_type	查询类型（简单 / 子查询 / 联合查询）
table	操作的表（别名 / 真实表名）
type	最重要！ 连接类型（性能从好到坏）
possible_keys	可能会用到的索引
key	实际用到的索引
key_len	索引使用的字节长度
ref	索引匹配的列 / 常量
rows	预估要扫描的行数
filtered	结果过滤百分比（越高越好）
Extra	关键补充信息（Using index/Using filesort 等）
partitions	匹配的分区（分区表才用）
cost	执行计划预估成本

1. id
查询的执行序号，数字越大越先执行；id 相同则从上到下执行。
单表查询：id=1
子查询 / 关联查询：会出现多个 id

2. select_type
标记查询的类型，判断是否为复杂查询：
SIMPLE：简单查询（无子查询 / UNION）
SUBQUERY：子查询
DERIVED：派生表（FROM 里的子查询）
UNION：UNION 中的第二个及以后的查询

3. table
当前行操作的表（显示表名或别名）。

4. type（⭐ 最核心优化指标）
表示表的连接 / 查询方式，性能从优到劣排序：system > const > eq_ref > ref > range > index > ALL
const/system：最优，根据主键 / 唯一索引精准匹配一行
eq_ref：多表关联，主键 / 唯一索引匹配
ref：普通索引匹配，返回多行
range：索引范围查询（>、<、BETWEEN、IN）
index：扫描全索引树
ALL：全表扫描（最坏！需要优化）
✅ 优化目标：至少达到 range，优先 ref 及以上

5. possible_keys
理论上可能用到的索引，只是候选，不一定真用。

6. key（⭐ 必看）
实际使用的索引
为 NULL = 没走索引
显示索引名 = 命中索引

7. key_len
索引使用的字节长度，数值越小，索引效率越高。

8. ref
显示与索引比较的列 / 常量，例如：库名.表名.列名 或 const。

9. rows
MySQL 预估需要扫描的行数
数值越小，查询越快
全表扫描时会等于表总行数

10. filtered
符合查询条件的行百分比，越高越好（100 为最优）。

11. Extra（⭐ 关键优化信号）
补充执行细节，出现以下值必须优化：
Using index：最优，使用覆盖索引，无需回表
Using where：用 WHERE 过滤数据
Using filesort：严重警告，MySQL 额外排序（无法用索引排序，性能差）
Using temporary：严重警告，使用临时表（GROUP BY/ORDER BY 不合理）
Using join buffer：关联查询未用索引，性能差

12. partitions
分区表匹配的分区，普通表为 NULL。

13. cost
查询执行的预估成本，数值越小越好。

## 判断SQL好坏

type 不能是 ALL（全表扫描 = 慢查询）
key 不能是 NULL（必须命中索引）
Extra 无 filesort/temporary（无额外排序 / 临时表）
rows 越小越好