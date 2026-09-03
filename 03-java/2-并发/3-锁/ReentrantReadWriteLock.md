---
tags:
  - Java
  - 并发
  - 锁
category: Java/并发/锁
---

# ReentrantReadWriteLock

## 是什么

`ReentrantReadWriteLock` 是 `java.util.concurrent.locks` 包提供的可重入读写锁，实现了 `ReadWriteLock` 接口。核心规则：**读读共享、读写互斥、写写互斥**。适用于读多写少的场景，通过允许多个线程同时读来提升并发性能。

## 核心特性

| 特性 | 说明 |
|---|---|
| 可重入 | 同一线程可重复获取读锁或写锁 |
| 公平/非公平 | 构造函数可指定，默认非公平 |
| 锁降级 | 支持：写锁 → 读锁 |
| 锁升级 | 不支持：读锁 → 写锁（会死锁） |
| Condition | 写锁支持 `newCondition()`，读锁不支持 |

## 读写规则

```
     读锁    写锁
读锁  共享    互斥
写锁  互斥    互斥
```

- **读读共享**：多个线程可同时持有读锁，提高读并发
- **读写互斥**：有线程持有读锁时，写锁会被阻塞；有线程持有写锁时，读锁会被阻塞
- **写写互斥**：写锁是独占的，同一时刻只有一个线程可持有

## 锁降级

锁降级是指**在持有写锁的情况下，获取读锁，然后释放写锁**的过程。降级后线程仍持有读锁，可以继续读取数据。

**为什么需要锁降级？**

保证数据可见性：写操作完成后，如果先释放写锁再获取读锁，其他线程可能在这期间获取写锁修改数据，导致当前线程读取到不一致的数据。锁降级保证了从写到读的数据可见性。

```java
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

// 锁降级示例：缓存更新
public void updateCache(String key, Object value) {
    writeLock.lock();        // 1. 获取写锁
    try {
        // 更新数据
        cache.put(key, value);
        readLock.lock();     // 2. 获取读锁（降级开始）
    } finally {
        writeLock.unlock();  // 3. 释放写锁（降级完成）
    }
    try {
        // 继续读取数据，保证可见性
        Object v = cache.get(key);
    } finally {
        readLock.unlock();   // 4. 释放读锁
    }
}
```

## 为什么不支持锁升级

如果线程 A 持有读锁后尝试获取写锁（锁升级），而线程 B 也持有读锁后尝试获取写锁，两个线程都在等对方释放读锁，就会形成**死锁**。因此 ReentrantReadWriteLock 不允许在持有读锁时获取写锁。

```java
// 锁升级 —— 会导致死锁！
readLock.lock();
try {
    writeLock.lock();  // 阻塞！因为当前线程已持有读锁，写锁无法获取
    // ...
} finally {
    readLock.unlock();
}
```

## 适用场景

- **缓存系统**：读多写少，多个线程可并发读缓存，写缓存时独占
- **配置中心**：配置信息读远多于写
- **统计数据**：频繁读取统计指标，偶尔更新

## 与 StampedLock 的选择

| 场景 | 推荐 |
|---|---|
| 通用读写场景 | ReentrantReadWriteLock |
| 读多写少，追求极致性能 | StampedLock |
| 需要可重入 | ReentrantReadWriteLock |
| 需要锁降级 | ReentrantReadWriteLock |
| 需要公平性控制 | ReentrantReadWriteLock |
| 需要条件变量 | ReentrantReadWriteLock（写锁支持 Condition） |

## 面试高频

**Q：ReentrantReadWriteLock 的锁降级是什么？为什么要锁降级？**

A：锁降级是在持有写锁时获取读锁再释放写锁的过程。目的是保证写操作完成后读取数据的可见性，防止释放写锁后其他线程修改数据导致当前线程读到不一致的数据。

**Q：为什么 ReentrantReadWriteLock 不支持锁升级？**

A：因为多个线程同时持有读锁时，如果都尝试升级为写锁，会互相等待对方释放读锁，导致死锁。

## 一句话总结

ReentrantReadWriteLock 实现了读读共享、读写/写写互斥的可重入读写锁，支持锁降级但不支持锁升级，适用于读多写少的缓存等场景，是大多数读写锁场景的首选。
