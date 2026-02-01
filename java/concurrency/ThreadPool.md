# 线程池

## 创建线程池

创建线程池的核心方法 / 入口，按使用场景优先级总结：

- 开发调试 / 快速实现
  - java.util.concurrent.Executors 的静态工厂方法
    - newFixedThreadPool
    - newSingleThreadExecutor
- 生产环境 / 线上项目
  - 必须直接使用 ThreadPoolExecutor的 7 参数构造方法
  - 手动指定有界任务队列、合理的核心 / 最大线程数，避免 OOM
- 定时 / 延时任务
  - 底层用 ScheduledThreadPoolExecutor 构造
  - 快捷方式用 Executors.newScheduledThreadPool()

## 快速创建入口

java.util.concurrent.Executors 类的静态工厂方法

5 种静态方法
- newFixedThreadPool(int nThreads)
  - 固定核心线程数
  - 核心线程 = 最大线程
- newCachedThreadPool()
  - 可缓存
  - 核心线程 = 0
  - 最大线程 = Integer.MAX_VALUE
  - 空闲线程 60 s 回收
- newSingleThreadExecutor()
  - 单线程
  - 只有一个核心线程
  - 任务串行执行
- newScheduledThreadPool(int corePoolSize)
  - 定时/延时执行
  - 支持定时任务、周期性任务
- newWorkStealingPool()
  - 并行工作窃取

## 底层构造方法

ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)

Executors的快捷方法存在参数硬编码问题（如 newCachedThreadPool 的最大线程数是Integer.MAX_VALUE，大量任务会创建无数线程导致 OOM；newFixedThreadPool 的任务队列是无界队列，任务堆积会撑爆内存）。

```java
public ThreadPoolExecutor(
    int corePoolSize,        // 核心线程数：线程池常驻的最小线程数，即使空闲也不会回收
    int maximumPoolSize,     // 最大线程数：线程池允许创建的最大线程数（核心+非核心）
    long keepAliveTime,      // 非核心线程空闲超时时间：超时后回收非核心线程
    TimeUnit unit,           // keepAliveTime的时间单位（如TimeUnit.SECONDS、TimeUnit.MILLISECONDS）
    BlockingQueue<Runnable> workQueue,  // 任务队列：核心线程忙时，新任务进入队列等待
    ThreadFactory threadFactory,        // 线程工厂：自定义线程的创建（如设置线程名、优先级）
    RejectedExecutionHandler handler    // 拒绝策略：任务队列满+最大线程数达上限时，如何处理新任务
);
```
