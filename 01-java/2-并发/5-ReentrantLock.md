---
tags:
  - Java
  - 并发
category: Java/并发
---

# ReentrantLock

## ReentrantLock 是什么？

ReentrantLock 是 `java.util.concurrent.locks` 包下基于 **AQS（AbstractQueuedSynchronizer）** 实现的**可重入独占锁**。它提供了比 synchronized 更灵活的锁控制能力，包括可响应中断、可超时获取、可公平/可非公平、可绑定多个条件变量等。

```java
ReentrantLock lock = new ReentrantLock();        // 默认非公平锁
ReentrantLock lock = new ReentrantLock(true);    // 公平锁
```

## 核心特性

| 特性 | 说明 |
|---|---|
| 可重入 | 同一线程可重复获取同一把锁，state 计数器 +1 |
| 可公平/可非公平 | 构造参数指定，默认非公平 |
| 可响应中断 | `lockInterruptibly()` 在等待锁时可响应中断 |
| 可超时获取 | `tryLock(timeout, unit)` 在指定时间内尝试获取锁 |
| 可绑定多个条件 | `newCondition()` 创建多个 Condition，精确唤醒 |
| 手动释放 | 必须在 finally 中调用 `unlock()` |

## AQS 原理简介

ReentrantLock 的核心是 AQS（AbstractQueuedSynchronizer），其关键设计：

### state 变量

- `volatile int state`：表示锁的状态
- 未加锁时 state = 0；加锁时通过 CAS 将 state 从 0 改为 1
- 可重入时 state 递增，释放时递减，减到 0 才真正释放锁
- 公平锁/非公平锁共用同一个 state

### CLH 双向队列

- 获取锁失败的线程被包装为 Node 节点，加入 CLH 双向队列尾部
- 队列中的线程以自旋 + LockSupport.park() 方式等待
- 前驱节点释放锁时唤醒后继节点（LockSupport.unpark()）

```
     head          Node1          Node2          tail
  (dummy)  <--  (thread-A)  <--  (thread-B)  <--  (tail)
```

### 非公平锁获取流程

```
1. CAS 尝试将 state 从 0 改为 1
   |-- 成功 --> 获取锁
   +-- 失败 --> 判断是否当前线程持有锁（可重入）
              |-- 是 --> state + 1
              +-- 否 --> 入 CLH 队列等待
```

### 公平锁获取流程

```
1. 检查 CLH 队列中是否有等待线程
   |-- 有 --> 当前线程入队等待（不抢锁）
   +-- 没有 --> CAS 尝试将 state 从 0 改为 1
              |-- 成功 --> 获取锁
              +-- 失败 --> 入 CLH 队列等待
```

## 公平锁 vs 非公平锁

| 对比维度 | 公平锁 | 非公平锁 |
|---|---|---|
| 构造方式 | `new ReentrantLock(true)` | `new ReentrantLock()` / `new ReentrantLock(false)` |
| 获取策略 | 先检查队列是否有等待线程，有则排队 | 先 CAS 抢锁，失败才入队 |
| 吞吐量 | 较低（线程切换多） | 较高（减少线程切换） |
| 饥饿问题 | 不会饥饿 | 可能饥饿（队列中线程可能一直等不到） |
| 适用场景 | 对公平性要求高 | 一般场景（默认推荐） |

> 非公平锁性能更好的原因：线程释放锁后，新来的线程可能直接获取锁，不需要唤醒队列中线程，减少了线程上下文切换开销。

## 与 synchronized 的对比

| 对比维度 | synchronized | ReentrantLock |
|---|---|---|
| 实现层面 | JVM 内置关键字 | API 层面（基于 AQS） |
| 锁获取释放 | 自动（进入/退出同步块） | 手动（`lock()` / `unlock()`） |
| 可中断 | 不可 | 可（`lockInterruptibly()`） |
| 超时获取 | 不支持 | 支持（`tryLock(timeout, unit)`） |
| 公平性 | 仅非公平 | 可公平/可非公平 |
| 条件变量 | 单个（`wait` / `notify`） | 多个（`Condition`） |
| 锁绑定 | 不支持 | 支持多个 Condition 绑定 |
| 可重入 | 是 | 是 |
| 死锁风险 | 较低（自动释放） | 较高（忘记 unlock） |
| JVM 优化 | 锁升级/锁消除/锁粗化 | 无 JVM 层面优化 |
| 使用场景 | 简单同步、锁持有时间短 | 需要高级功能（中断/超时/公平/多条件） |

## 常用 API

| 方法 | 说明 |
|---|---|
| `lock()` | 获取锁，阻塞等待 |
| `unlock()` | 释放锁（必须在 finally 中调用） |
| `tryLock()` | 尝试获取锁，成功返回 true，失败返回 false（不阻塞） |
| `tryLock(timeout, unit)` | 在指定时间内尝试获取锁 |
| `lockInterruptibly()` | 获取锁，等待期间可响应中断 |
| `newCondition()` | 创建一个 Condition 对象 |
| `isHeldByCurrentThread()` | 判断当前线程是否持有锁 |
| `getHoldCount()` | 查询当前线程持有锁的次数 |
| `getQueueLength()` | 估计等待获取锁的线程数量 |
| `hasQueuedThreads()` | 是否有线程在等待获取锁 |

## 使用模板（try-finally 释放锁）

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // 同步代码
} finally {
    lock.unlock();  // 保证锁一定被释放
}
```

> **注意**：`lock()` 必须在 try 之前调用，不要放在 try 里面。如果在 try 里面调用且 lock() 抛异常，finally 中 unlock() 会抛 IllegalMonitorStateException。

### tryLock 使用模板

```java
if (lock.tryLock()) {
    try {
        // 获取锁成功，执行同步代码
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败，执行其他逻辑
}
```

### lockInterruptibly 使用模板

```java
try {
    lock.lockInterruptibly();
    try {
        // 同步代码
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    // 等待锁的过程中被中断
    Thread.currentThread().interrupt();
}
```

## Condition 的使用（替代 wait/notify）

Condition 提供了比 `wait()/notify()` 更精确的线程等待/唤醒机制：
- 支持多个等待队列
- 可以精确唤醒指定条件上的线程
- `await()` 对应 `wait()`，`signal()` 对应 `notify()`，`signalAll()` 对应 `notifyAll()`

```java
ReentrantLock lock = new ReentrantLock();
Condition notEmpty = lock.newCondition();  // 非空条件
Condition notFull = lock.newCondition();   // 未满条件

// 生产者
lock.lock();
try {
    while (queue.size() == capacity) {
        notFull.await();   // 队列满，等待非满条件
    }
    queue.add(item);
    notEmpty.signal();     // 通知消费者：队列非空
} finally {
    lock.unlock();
}

// 消费者
lock.lock();
try {
    while (queue.isEmpty()) {
        notEmpty.await();  // 队列空，等待非空条件
    }
    Item item = queue.remove();
    notFull.signal();      // 通知生产者：队列非满
} finally {
    lock.unlock();
}
```

**Condition vs wait/notify**：

| 对比维度 | wait/notify | Condition |
|---|---|---|
| 所属 | Object 的方法 | Lock 接口的方法 |
| 等待队列数量 | 一个 | 多个 |
| 精确唤醒 | 不支持（notify 随机唤醒） | 支持（signal 唤醒指定条件上的线程） |
| 超时等待 | 支持 | 支持 |
| 不响应中断 | 不支持 | 支持（awaitUninterruptibly） |
| 前提 | 必须在 synchronized 块中 | 必须在 lock() 后 |

## 一句话总结

ReentrantLock 是基于 AQS 的可重入独占锁，相比 synchronized 提供了可中断、可超时、可公平、多条件等高级功能，但需要手动释放锁，适合需要精细控制的高级同步场景。
