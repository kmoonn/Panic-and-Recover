---
tags:
  - 中间件
  - 安全
category: 安全
---

# SQL 注入

## Q: 什么是 SQL 注入？举例说明

**SQL 注入**：攻击者通过输入参数拼接进 SQL 语句，改变原 SQL 语义，执行恶意 SQL。

### 经典示例

```sql
-- 正常登录查询
SELECT * FROM users WHERE username = 'admin' AND password = '123456';

-- 攻击者输入用户名：' OR 1=1 --
-- 拼接后 SQL：
SELECT * FROM users WHERE username = '' OR 1=1 --' AND password = 'xxx';
-- 1=1 永真，-- 注释掉后面，直接返回所有用户！无需密码即可登录

-- 删库攻击：输入用户名：'; DROP TABLE users; --
-- 拼接后 SQL：
SELECT * FROM users WHERE username = ''; DROP TABLE users; --' AND password = 'xxx';
```

### 危害

| 危害 | 说明 |
|------|------|
| 绕过认证 | 无密码登录 |
| 数据泄露 | 读取任意表数据 |
| 数据篡改 | 修改/删除数据 |
| 提权 | 执行系统命令（MySQL `INTO OUTFILE` 写 WebShell） |

---

## Q: SQL 注入如何防御？

| 防御方式 | 原理 | 安全级别 |
|---------|------|---------|
| **参数化查询（PreparedStatement）** | 参数用占位符 `?`，驱动自动转义，不拼接进 SQL | 最高（首选） |
| **ORM 框架** | Hibernate/MyBatis 默认用参数化 | 高 |
| **输入校验** | 白名单验证参数格式（长度、类型、字符集） | 辅助 |
| **最小权限** | 应用 DB 用户只授予必要权限，禁止 DROP/FILE | 纵深防御 |

```java
// 安全：PreparedStatement 参数化
String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, username);   // 驱动自动转义，不会拼接进SQL
ps.setString(2, password);
ResultSet rs = ps.executeQuery();

// 危险：字符串拼接
String sql = "SELECT * FROM users WHERE username = '" + username
           + "' AND password = '" + password + "'";  // 直接注入！
```

---

## Q: MyBatis 中 `${}` 和 `#{}` 的区别？（高频面试题！）

| 对比项 | `#{}` | `${}` |
|--------|-------|-------|
| 处理方式 | **PreparedStatement 参数化**，占位符 `?` | **字符串直接拼接**进 SQL |
| SQL 注入 | **安全**（自动转义） | **危险**（注入风险） |
| 性能 | 预编译缓存，可复用执行计划 | 每次拼接不同 SQL，无法复用 |
| 适用场景 | 参数值（WHERE 条件值、INSERT 值） | 动态 SQL 结构（表名、列名、ORDER BY） |

```xml
<!-- #{} → PreparedStatement 占位符 -->
<select id="getUser" resultType="User">
    SELECT * FROM users WHERE id = #{id}
    <!-- 实际执行：SELECT * FROM users WHERE id = ?  →  参数：id值 -->
</select>

<!-- ${} → 字符串直接拼接 -->
<select id="getUser" resultType="User">
    SELECT * FROM users WHERE id = ${id}
    <!-- 实际执行：SELECT * FROM users WHERE id = 1  （直接替换） -->
    <!-- 如果 id 传入 "1 OR 1=1"，则变成 WHERE id = 1 OR 1=1  注入！ -->
</select>

<!-- ${} 合法用途：动态表名、列名（#{} 不能用在这些位置） -->
<select id="queryByColumn" resultType="User">
    SELECT * FROM ${tableName} ORDER BY ${columnName}
    <!-- 表名和列名不能用占位符，只能拼接，但必须做白名单校验！ -->
</select>
```

> **口诀**：能用 `#{}` 就用 `#{}`，`${}` 只用于表名/列名/ORDER BY 等结构场景，且必须白名单校验。

---

## 一句话总结

SQL 注入利用参数拼接改变 SQL 语义，防御首选 PreparedStatement 参数化查询，MyBatis 中 `#{}` 安全 `${}` 危险（仅用于动态表名/列名且需白名单校验）。
