---
tags:
  - Spring
  - 事务
category: Spring/事务
---

# @Transactional 原理

Spring 的事务注解是基于 AOP 实现的声明式事务。

Spring 在 Bean 初始化时为标注了 @Transactional 的类或方法创建代理对象，在方法执行前通过 PlatformTransactionManager 开启事务，方法正常结束则提交事务，出现符合回滚规则的异常则回滚事务。

事务是否生效依赖于代理机制，因此内部方法调用、非 public 方法、异常被捕获等场景会导致事务失效。

## 八股速记

**Q: Spring 事务 / `@Transactional` 失效**

- **传播行为**：`REQUIRED`（默认，有就加入没就新建）、`REQUIRES_NEW`（挂起当前、开新事务）、`NESTED`（嵌套/保存点）。
- **隔离级别**：委托数据库（呼应 `04-database.md` 的四种隔离级别）。
- **失效常见原因**：① **方法内部自调用**（不走代理）；② 方法非 `public`；③ 异常被 `catch` 吞掉没抛出；④ 抛的是**受检异常**（默认只回滚 `RuntimeException`，需 `rollbackFor`）；⑤ 数据库引擎不支持事务（如 MyISAM）。

**⭐ 加分/易错**：这是超高频追问题。记牢"**自调用 + 吞异常 + 非 public**"三大坑，能答出原理（基于代理）就是加分。

## 一句话总结

> @Transactional基于AOP代理实现，代理对象管理事务开启与回滚，内部调用等场景会导致事务失效