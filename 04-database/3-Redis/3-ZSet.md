---
tags:
  - Redis
  - 缓存
category: 数据库/Redis
---

# ZSet 底层实现

ZSet 底层由两种数据结构实现

1. ziplist（压缩列表）
2. skiplist（跳表） + 哈希表

- 元素少、值小 → 用 ziplist，省内存
- 元素多、值大 → 自动转成 skiplist + 哈希表


## 一句话总结

> ZSet底层小数据用ziplist省内存，大数据自动转skiplist+哈希表支持高效范围查询与排序。
