---
tags:
  - MyBatis
  - ORM
category: 开发工具/MyBatis
---

# 动态sql标签

```text
<if>：条件判断，非空拼接
<where>：自动去掉多余 and/or
<choose>/<when>/<otherwise>：多分支选择
<foreach>：遍历集合，in 批量查询、批量插入
<set>：动态更新，去除多余逗号
<trim>：自定义前后缀、去除多余符号
<include>：复用 SQL 片段
<bind>：绑定变量
```

## 一句话总结

> MyBatis动态SQL标签包括if条件、where去AND、choose分支、foreach遍历、set去逗号、trim自定义等