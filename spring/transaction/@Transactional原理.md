# @Transactional 原理

Spring 的事务注解是基于 AOP 实现的声明式事务。

Spring 在 Bean 初始化时为标注了 @Transactional 的类或方法创建代理对象，在方法执行前通过 PlatformTransactionManager 开启事务，方法正常结束则提交事务，出现符合回滚规则的异常则回滚事务。

事务是否生效依赖于代理机制，因此内部方法调用、非 public 方法、异常被捕获等场景会导致事务失效。