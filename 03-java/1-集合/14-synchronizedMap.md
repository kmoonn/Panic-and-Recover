---
tags:
  - Java
  - 集合
category: Java/集合
---

# synchronizedMap

## 是什么

`synchronizedMap` 是 `Collections.synchronizedMap()` 返回的线程安全包装 Map。它使用**装饰器模式**，在普通 Map 的每个方法外层加 `synchronized` 互斥锁，将非线程安全的 Map 变为线程安全的。

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
```

## 实现原理

查看 JDK 源码，`synchronizedMap` 的每个方法都用 `synchronized(mutex)` 包裹：

```java
// JDK 源码（简化）
private static class SynchronizedMap<K,V> implements Map<K,V> {
    private final Map<K,V> m;     // 被装饰的底层 Map
    final Object mutex;           // 锁对象

    SynchronizedMap(Map<K,V> m) {
        this.m = m;
        this.mutex = this;        // 锁对象就是 synchronizedMap 自身
    }

    public int size() {
        synchronized (mutex) { return m.size(); }
    }
    public V get(Object key) {
        synchronized (mutex) { return m.get(key); }
    }
    public V put(K key, V value) {
        synchronized (mutex) { return m.put(key, value); }
    }
    public V remove(Object key) {
        synchronized (mutex) { return m.remove(key); }
    }
    // ... 所有方法都是 synchronized(mutex)
}
```

**关键点：所有操作共用同一把锁（`mutex = this`），即对整个 Map 加锁，同一时刻只有一个线程能操作 Map。**

## 遍历时必须手动同步

`synchronizedMap` 的 `size()`、`get()`、`put()` 等单个方法是线程安全的，但**复合操作和遍历不是**，必须手动加锁：

```java
// 错误：遍历可能抛 ConcurrentModificationException
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}

// 正确：遍历时必须手动加锁
synchronized (map) {
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
        System.out.println(entry.getKey() + "=" + entry.getValue());
    }
}

// 正确：复合操作也需手动加锁
synchronized (map) {
    if (!map.containsKey(key)) {
        map.put(key, value);
    }
}
```

## 与 ConcurrentHashMap 的对比

| 对比维度 | synchronizedMap | ConcurrentHashMap |
|---|---|---|
| 锁粒度 | 整个 Map（一把锁） | 桶级别（JDK 7: 分段锁；JDK 8: CAS + synchronized） |
| 并发性能 | 差（所有操作串行） | 好（不同桶可并发操作） |
| 遍历安全 | 需手动加锁 | 弱一致性迭代器，无需手动加锁 |
| null 键/值 | 允许 | 不允许（null 键和 null 值都抛 NPE） |
| 复合操作 | 需手动加锁 | 提供 `putIfAbsent`、`computeIfAbsent` 等原子方法 |
| 实现复杂度 | 简单（装饰器包装） | 复杂（分段锁/CAS + 内部优化） |
| 推荐程度 | 不推荐 | 推荐 |

## 性能对比示意

```
synchronizedMap：
Thread1: [---put(A)---]          [---get(B)---]
Thread2:          [---put(C)---]          [---get(D)---]
         ↑ 全部串行，任何时刻只有一个线程操作

ConcurrentHashMap：
Thread1: [---put(A)---]    [---get(B)---]
Thread2: [---put(C)---]    [---get(D)---]
         ↑ 不同桶可并行，吞吐量更高
```

## 为什么不推荐使用

1. **全表锁，性能差**：所有操作串行，高并发下成为瓶颈
2. **遍历需手动同步**：容易遗漏，导致 `ConcurrentModificationException`
3. **复合操作不安全**：`containsKey` + `put` 不是原子的，需手动加锁
4. **ConcurrentHashMap 更好**：几乎在所有方面都优于 synchronizedMap

## 什么时候可以考虑使用

- 极低并发场景（几乎无竞争）
- 需要允许 null 键/值时（ConcurrentHashMap 不允许）
- 快速将非线程安全 Map 变为线程安全（临时方案）

## 面试高频

**Q：synchronizedMap 和 ConcurrentHashMap 有什么区别？**

A：核心区别在锁粒度——synchronizedMap 对整个 Map 加一把锁，所有操作串行；ConcurrentHashMap 在 JDK 7 用分段锁、JDK 8 用 CAS + 桶级别 synchronized，不同桶可并发操作，性能远优于 synchronizedMap。此外 ConcurrentHashMap 提供原子复合方法（如 `putIfAbsent`），遍历时弱一致性无需手动加锁。

**Q：synchronizedMap 遍历为什么要手动加锁？**

A：因为 `synchronizedMap` 只保证单个方法的原子性，遍历涉及多次调用 `next()`，两次调用之间其他线程可能修改 Map，导致 `ConcurrentModificationException` 或行为不一致。必须在遍历期间持有锁才能保证一致性。

## 一句话总结

synchronizedMap 通过装饰器模式给 Map 的每个方法加全表 synchronized 锁，线程安全但性能差，遍历和复合操作还需手动加锁，实际开发中应优先使用 ConcurrentHashMap。
