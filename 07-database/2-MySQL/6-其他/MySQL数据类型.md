---
tags:
  - MySQL
category: 数据库/MySQL/其他
---

# MySQL 数据类型

## Q: MySQL 有哪些主要数据类型分类？

| 分类 | 常用类型 | 说明 |
|------|---------|------|
| 数值类型 | TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, DECIMAL | 整数 + 浮点 + 定点 |
| 字符串类型 | CHAR, VARCHAR, TEXT, BLOB | 定长/变长/大文本/二进制 |
| 日期时间类型 | DATE, TIME, DATETIME, TIMESTAMP | 日期/时间/日期时间/时间戳 |
| JSON 类型 | JSON | MySQL 5.7+ 原生支持 |

---

## Q: 数值类型各占多少字节？范围是什么？

| 类型 | 字节数 | 有符号范围 | 无符号范围 | 常见用途 |
|------|--------|-----------|-----------|---------|
| TINYINT | 1 | -128 ~ 127 | 0 ~ 255 | 状态标志、布尔 |
| SMALLINT | 2 | -32768 ~ 32767 | 0 ~ 65535 | 小范围计数 |
| INT | 4 | -2^31 ~ 2^31-1 | 0 ~ 2^32-1 | 主键、常规整数 |
| BIGINT | 8 | -2^63 ~ 2^63-1 | 0 ~ 2^64-1 | 大ID、雪花ID |
| FLOAT | 4 | 约 ±3.4E38 | - | 近似小数，精度低 |
| DOUBLE | 8 | 约 ±1.8E308 | - | 近似小数，精度较高 |
| DECIMAL(M,D) | 变长 | 取决于M和D | - | 精确小数（金额！） |

> **关键点**：FLOAT/DOUBLE 是浮点近似值，有精度丢失；DECIMAL 是定点精确值，**存金额必须用 DECIMAL**。

```sql
-- FLOAT 精度丢失示例
CREATE TABLE t1 (f FLOAT);
INSERT INTO t1 VALUES (123456789.123456789);
SELECT f FROM t1;  -- 结果：123456792.0（精度丢失！）

-- DECIMAL 精确存储
CREATE TABLE t2 (d DECIMAL(18,6));
INSERT INTO t2 VALUES (123456789.123456789);
SELECT d FROM t2;  -- 结果：123456789.123457（精确到D位）
```

### INT(10) 中括号里的数字是什么意思？

- **不是长度限制**，是**显示宽度**（display width），仅搭配 `ZEROFILL` 时有效
- MySQL 8.0 已废弃 `ZEROFILL`，括号内数字无实际意义
- `INT(1)` 和 `INT(10)` 的存储空间和范围**完全相同**

---

## Q: CHAR 和 VARCHAR 的区别？

| 对比项 | CHAR | VARCHAR |
|--------|------|---------|
| 长度 | 定长，不足补空格 | 变长，按实际存储 |
| 最大长度 | 255 字符 | 65535 字节（受行大小限制） |
| 存储开销 | 固定 N 字节 | 实际长度 + 1~2 字节长度前缀 |
| 尾部空格 | INSERT 时补空格，SELECT 时自动截断 | 原样存储，原样取出 |
| 适用场景 | 短且定长（MD5、手机号、性别） | 长度变化大（姓名、地址、描述） |
| 性能 | 更新不产生碎片，检索快 | 更新可能产生碎片 |
| 空间利用 | 浪费空间（补空格） | 节省空间 |

```sql
CREATE TABLE char_test (
    c CHAR(10),      -- 存 'abc       '（补7个空格）
    v VARCHAR(10)    -- 存 'abc'（3字节 + 1字节长度前缀）
);

INSERT INTO char_test VALUES ('abc', 'abc');
SELECT LENGTH(c), LENGTH(v) FROM char_test;  -- 10, 3
```

### TEXT 和 BLOB

| 类型 | 最大长度 | 说明 |
|------|---------|------|
| TINYTEXT | 255 B | |
| TEXT | 64 KB | 常用于文章内容 |
| MEDIUMTEXT | 16 MB | |
| LONGTEXT | 4 GB | |
| TINYBLOB | 255 B | |
| BLOB | 64 KB | 二进制大对象（图片、文件） |
| MEDIUMBLOB | 16 MB | |
| LONGBLOB | 4 GB | |

> **注意**：TEXT/BLOB 不能设默认值，索引必须加前缀长度，可能产生溢出页存储。

---

## Q: DATETIME 和 TIMESTAMP 的区别？

| 对比项 | DATETIME | TIMESTAMP |
|--------|----------|-----------|
| 格式 | YYYY-MM-DD HH:MM:SS | 同左 |
| 范围 | 1000-01-01 ~ 9999-12-31 | 1970-01-01 ~ 2038-01-19 |
| 存储空间 | 8 字节（MySQL 5.6.4+） | 4 字节 |
| 时区 | **不转换**，存什么取什么 | **自动转换**，存UTC，取时转当前时区 |
| 默认值 | NULL | CURRENT_TIMESTAMP（5.6+） |
| 自动更新 | 需手动指定 ON UPDATE | 支持 ON UPDATE CURRENT_TIMESTAMP |
| NULL | 允许 | 5.7+ 允许，默认 NOT NULL |
| 受影响 | 不受时区影响 | 受系统 time_zone 影响 |

```sql
CREATE TABLE time_test (
    dt DATETIME,                           -- 存入什么就是什么
    ts TIMESTAMP                           -- 存入时转UTC，取出时转当前时区
);

SET time_zone = '+08:00';
INSERT INTO time_test VALUES ('2024-01-01 12:00:00', '2024-01-01 12:00:00');

SET time_zone = '+00:00';
SELECT * FROM time_test;
-- dt: 2024-01-01 12:00:00（不变）
-- ts: 2024-01-01 04:00:00（自动转成UTC时区显示）
```

### DATE 和 TIME

- `DATE`：仅日期 `YYYY-MM-DD`，3 字节
- `TIME`：仅时间 `HH:MM:SS`，3 字节，范围 -838:59:59 ~ 838:59:59（可表示时间差）

---

## Q: MySQL 的 JSON 类型怎么用？什么时候该用 JSON？

### JSON 类型特性（MySQL 5.7+）

```sql
CREATE TABLE user_profile (
    id INT PRIMARY KEY,
    info JSON                           -- 原生JSON列
);

INSERT INTO user_profile VALUES (
    1,
    '{"name": "张三", "age": 25, "tags": ["java", "mysql"]}'
);
```

### 常用 JSON 函数

| 函数 | 作用 | 示例 |
|------|------|------|
| JSON_EXTRACT(doc, path) | 提取值 | `JSON_EXTRACT(info, '$.name')` → "张三" |
| JSON_CONTAINS(doc, val, path) | 是否包含 | `JSON_CONTAINS(info, '"java"', '$.tags')` → 1 |
| JSON_ARRAY() | 构造数组 | `JSON_ARRAY(1, 2, 3)` → [1,2,3] |
| JSON_OBJECT() | 构造对象 | `JSON_OBJECT('k', 'v')` → {"k":"v"} |
| JSON_SET(doc, path, val) | 设置值 | `JSON_SET(info, '$.age', 26)` |
| JSON_REMOVE(doc, path) | 删除键 | `JSON_REMOVE(info, '$.tags')` |
| -> | 等价 JSON_EXTRACT | `info->'$.name'` |
| ->> | 提取并去掉引号 | `info->>'$.name'` → 张三 |

```sql
-- 查询示例
SELECT info->>'$.name' AS name FROM user_profile WHERE id = 1;

-- JSON 索引（通过虚拟列 + 索引）
ALTER TABLE user_profile
ADD COLUMN name VARCHAR(50) GENERATED ALWAYS AS (info->>'$.name') STORED,
ADD INDEX idx_name (name);
```

### JSON 存储方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **JSON 列** | 灵活、schema-free、一个字段存复杂结构 | 难索引（需虚拟列）、查询性能差、校验弱 | 属性不固定、低频查询 |
| **拆分多列** | 可建索引、查询快、强类型校验 | 列数多、ALTER TABLE 加列麻烦 | 属性固定、高频查询 |
| **拆分关联表** | 规范化、灵活扩展、易索引 | JOIN 开销、查询复杂 | 属性多且需独立查询 |

> **原则**：高频查询 + 需索引 → 拆列/拆表；低频查询 + schema 灵活 → JSON 列

---

## 一句话总结

MySQL 数据类型选择核心：整数按范围选最小类型省空间，金额必须 DECIMAL，定长用 CHAR 变长用 VARCHAR，时间需时区转换用 TIMESTAMP 否则用 DATETIME，JSON 适合灵活 schema 但不宜高频查询索引场景。
