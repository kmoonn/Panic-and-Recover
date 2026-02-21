# 线程池拒绝策略

## 4 种 JDK 原生拒绝策略

- AbortPolicy 默认策略
  - 直接抛出RejectedExecutionException运行时异常，拒绝执行新任务
  - 直接报错，不处理新任务
- CallerRunsPolicy
  - 由提交任务的主线程直接执行新任务，线程池不处理
  - “线程池忙不过来，提交任务的人自己干”
- DiscardPolicy
  - 直接丢弃新任务，无任何提示和异常
  - “默默扔掉，假装没收到”
- DiscardOldestPolicy
  - 丢弃任务队列中最旧的任务（队列头部的任务），将新任务加入队列等待执行
  - “扔掉最早的任务，给新任务腾位置”

## 自定义拒绝策略

实现 RejectedExecutionHandler 接口，重写 rejectedExecution 方法。