---
tags:
  - Java
  - 并发
  - 锁
category: Java/并发/锁
---

# StampedLock

## 是什么

`StampedLock` 是 JDK 8 中 `java.util.concurrent.locks` 包引入的一种读写锁，核心创新是支持**乐观读（Optimistic Read）**模式。与传统的 `ReentrantReadWriteLock` 相比，乐观读不加锁，在读多写少的场景下性能显著提升。

## 三种锁模式

| 模式 | 获取方法 | 返回值 | 说明 |
|---|---|---|---|
| 写锁（Write Lock） | `writeLock()` | `long` stamp | 独占锁，同一时刻只有一个线程可获取 |
| 读锁（Read Lock） | `readLock()` | `long` stamp | 共享锁，允许多个线程同时读，与写锁互斥 |
| 乐观读（Optimistic Read） | `tryOptimisticRead()` | `long` stamp | 不加锁，返回一个版本戳，需通过 `validate` 校验 |

返回的 `stamp` 是一个长整型的"票据"，用于标识锁的状态版本，释放锁和校验乐观读都需要用到它。

## 乐观读的工作流程

乐观读的核心思想：先读数据，读完再检查期间是否有写操作发生。若没有，数据有效；若有，则转为悲观读锁重新读取。

```
tryOptimisticRead → 读取数据 → validate → 成功则使用数据
                                  ↓ 失败
                            获取 readLock → 重新读取 → 释放 readLock
```

## 代码示例：乐观读模板

```java
class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();

    // 乐观读：计算到原点的距离
    double distanceFromOrigin() {
        // 1. 尝试乐观读
        long stamp = sl.tryOptimisticRead();
        // 2. 读取数据到局部变量
        double currentX = x, currentY = y;
        // 3. 校验 stamp，检查读取期间是否有写操作
        if (!sl.validate(stamp)) {
            // 4. 校验失败，转为悲观读锁
            stamp = sl.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 写锁：移动坐标
    void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }
}
```

## 与 ReentrantReadWriteLock 的对比

| 对比维度 | StampedLock | ReentrantReadWriteLock |
|---|---|---|
| 乐观读 | 支持（不加锁） | 不支持 |
| 可重入 | 不支持 | 支持 |
| 锁降级 | 不支持 | 支持（写锁→读锁） |
| 性能 | 更高（乐观读无锁开销） | 较低（所有读操作都加锁） |
| 复杂度 | 高（需手动 validate） | 低（API 简单） |
| 可中断 | 不可中断（`writeLock` 不可响应 interrupt） | 可中断 |
| 适用场景 | 读多写少、追求极致性能 | 通用读写场景 |

## 注意事项

1. **不支持重入**：同一线程不能重复获取同一模式的锁，否则会死锁
2. **不可中断**：`writeLock()` 和 `readLock()` 不响应中断，不能在锁内做阻塞操作（如调用 `await()`、`sleep()` 等）
3. **不要在锁内做阻塞操作**：StampedLock 不是为条件变量设计的，没有 `newCondition()` 方法
4. **stamp 不能混用**：获取和释放锁必须使用同一个 stamp，否则抛 `IllegalMonitorStateException`
5. **乐观读中不要调用可能触发写锁的方法**：否则 validate 一定失败

## 面试视角

**Q：StampedLock 适用于什么场景？**

A：适用于**读多写少且对性能要求极高**的场景，如坐标计算、缓存读取等。乐观读模式下完全不加锁，性能远超传统读写锁。

**Q：为什么不推荐在日常开发中使用 StampedLock？**

A：使用复杂，容易出错（stamp 管理和 validate 逻辑），不支持重入，API 不友好。一般场景用 `ReentrantReadWriteLock` 即可，只有在对性能有明确瓶颈时才考虑 StampedLock。

## 一句话总结

StampedLock 通过乐观读模式实现无锁读取，在读多写少的高并发场景性能优异，但不支持重入且使用复杂，一般场景用 ReentrantReadWriteLock 足矣。
