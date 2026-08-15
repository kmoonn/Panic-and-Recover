---
tags:
  - Java
  - 集合
category: Java/集合
---

# ConcurrentHashMap

## Q1: 为什么需要 ConcurrentHashMap?

| 对比 | HashMap | Hashtable | ConcurrentHashMap |
|------|---------|-----------|-------------------|
| 线程安全 | ❌ 不安全 | ✅ 安全 | ✅ 安全 |
| 加锁方式 | 无锁 | **整个表一把锁** (`synchronized` 所有方法) | **分段锁 / 桶位锁** |
| 并发读 | 快（无锁） | 阻塞（读也要抢锁） | 快（volatile 读，无锁） |
| 并发写 | 不安全 | 串行化（所有写互斥） | **不同桶位并发写入不互斥** |
| null key/value | ✅ 允许 | ❌ 不允许 | ❌ 不允许 |
| 性能 | 单线程最优 | 多线程最差 | 多线程最优 |

**核心问题**：
- `HashMap` 多线程 put 可能导致**死循环**（JDK 1.7 扩容时链表成环）、数据丢失
- `Hashtable` 用一把锁锁整个表，读和写全部串行化，性能极差

---

## Q2: JDK 1.7 的实现（Segment 分段锁）

### 数据结构

```
ConcurrentHashMap
├── Segment[] (默认 16 个段)
│   ├── Segment 0  → HashEntry[] + ReentrantLock
│   ├── Segment 1  → HashEntry[] + ReentrantLock
│   ├── ...
│   └── Segment 15 → HashEntry[] + ReentrantLock
```

```java
// JDK 1.7 结构
static final class Segment<K,V> extends ReentrantLock {
    transient volatile HashEntry<K,V>[] table;
    // 每个 Segment 是一个小的 HashMap，自带一把 ReentrantLock
}

static final class HashEntry<K,V> {
    final int hash;
    final K key;
    volatile V value;
    volatile HashEntry<K,V> next;
}
```

| 特性 | 说明 |
|------|------|
| 分段数 | 默认 16（`-XX:ConcurrencyLevel`），一旦初始化不可扩容 |
| 锁粒度 | 每个 Segment 一把 `ReentrantLock` |
| 并发度 | 最多 16 个线程同时写（不同 Segment） |
| 定位流程 | hash → 定位 Segment → 再 hash → 定位 HashEntry |
| 读操作 | volatile 读 value + next，**不加锁** |
| 写操作 | lock Segment → 操作 → unlock |

### 1.7 的局限

- Segment 数量固定，扩容只扩 Segment 内的 HashEntry[]
- 并发度上限受 Segment 数量限制
- 锁粒度仍较粗（同一 Segment 内所有桶位互斥）

---

## Q3: JDK 1.8 的实现（Node[] + CAS + synchronized 桶位锁）

### 数据结构

```
ConcurrentHashMap
├── Node[] table  (与 HashMap 1.8 结构一致)
│   ├── index 0  → Node / TreeBin
│   ├── index 1  → null
│   ├── ...
│   └── index n  → Node 链表 / 红黑树
```

```java
// JDK 1.8 结构
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;       // volatile 保证可见性
    volatile Node<K,V> next;
}

static final class TreeBin<K,V> {
    // 红黑树节点，链表长度 >= 8 时树化
}
```

| 特性 | 说明 |
|------|------|
| 底层结构 | `Node[]` + 链表 + 红黑树（与 HashMap 1.8 一致） |
| 锁粒度 | **每个桶位（数组的单个槽位）** 一把锁 |
| 空桶位插入 | **CAS**（无锁） |
| 非空桶位更新 | `synchronized(head)` 锁住链表/红黑树首节点 |
| 读操作 | volatile 读，**全程无锁** |
| 扩容 | 支持**多线程并发扩容**（transfer 分段迁移） |

### put 流程（JDK 1.8）

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 1. 计算 hash
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        // 2. 初始化 table（lazy init）
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        // 3. 桶位为空 → CAS 插入（无锁）
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<>(hash, key, value, null)))
                break;
        }
        // 4. 桶位是 MOVED → 协助扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        // 5. 桶位非空 → synchronized 锁首节点
        else {
            synchronized (f) {
                // 遍历链表/红黑树，更新或插入
                // 链表长度 >= 8 → treeifyBin
            }
            // 检查是否需要扩容
        }
    }
    addCount(1L, binCount);  // 更新 size（CAS + CounterCell）
    return null;
}
```

---

## Q4: JDK 1.8 相比 1.7 的改进

| 改进点 | JDK 1.7 | JDK 1.8 |
|--------|---------|---------|
| 锁粒度 | Segment 级（最多 16 个并发写） | **桶位级**（理论上数组长度个并发写） |
| 锁实现 | `ReentrantLock` | `synchronized` + CAS（JDK 1.6+ synchronized 已优化） |
| 数据结构 | 数组 + 链表 | 数组 + 链表 + **红黑树**（查找 O(log n)） |
| 扩容 | 单线程扩容 Segment 内 HashEntry[] | **多线程并发扩容**（分段迁移，高效） |
| 查询复杂度 | O(n)（链表遍历） | O(log n)（红黑树，链表 >= 8 时树化） |
| 并发度 | 受 Segment 数量限制 | **随数组长度动态增长** |
| 内存占用 | Segment[] + HashEntry[][] | Node[]（更紧凑） |

### 1.8 为什么用 synchronized 而非 ReentrantLock?

1. **锁粒度更细**：每个桶位一把锁，JVM 可做更激进的优化（锁膨胀/偏向锁/轻量级锁）
2. **内存更省**：`ReentrantLock` 是对象，每个桶位创建一把锁开销大；`synchronized` 由 JVM 管理，无额外对象
3. **JDK 1.6+ synchronized 已高度优化**：偏向锁 → 轻量级锁 → 重量级锁膨胀机制

---

## Q5: 为什么不允许 null key 和 null value?

```java
// ConcurrentHashMap 源码校验
if (key == null || value == null) throw new NullPointerException();
```

**原因**：**二义性问题**

```java
// 如果允许 null value：
ConcurrentHashMap map = new ConcurrentHashMap();
map.put("key", null);

// 此时 get("key") 返回 null，有两种可能：
// 1. key 存在，value 就是 null
// 2. key 不存在，get 返回 null

// 在单线程下可用 containsKey 区分，但在并发下：
// containsKey 和 get 之间可能有其他线程修改 → 竞态条件
// 所以直接禁止 null，消除二义性
```

| Map | null key | null value | 原因 |
|-----|----------|------------|------|
| HashMap | ✅ | ✅ | 单线程，可用 containsKey 消除二义性 |
| Hashtable | ❌ | ❌ | 并发下 null 有二义性 |
| ConcurrentHashMap | ❌ | ❌ | 并发下 null 有二义性 |

---

## Q6: 常用方法与使用场景

### 原子复合操作方法

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// 1. putIfAbsent：不存在才放入（原子操作，避免竞态）
map.putIfAbsent("key", 1);  // 如果 key 不存在则放入

// 2. computeIfAbsent：不存在时计算并放入（常用于缓存）
map.computeIfAbsent("user:1001", key -> loadUserFromDB(1001));

// 3. compute：存在时重新计算
map.compute("counter", (k, v) -> v == null ? 1 : v + 1);

// 4. merge：合并新值
map.merge("counter", 1, Integer::sum);

// 5. forEach / search / reduce（JDK 8+ 批量操作）
map.forEach(3, (k, v) -> System.out.println(k + "=" + v));  // 并行度 3
```

### 常见使用场景

| 场景 | 代码示例 | 说明 |
|------|---------|------|
| **本地缓存** | `cache.computeIfAbsent(key, k -> queryDB(k))` | 线程安全缓存，避免重复计算 |
| **计数器** | `map.merge(key, 1, Integer::sum)` | 并发计数，比 `synchronized` + HashMap 高效 |
| **配置中心** | `configMap.put("timeout", "5000")` | 运行时动态更新配置，读无锁 |
| **注册表** | `registry.putIfAbsent(serviceName, instance)` | 服务注册，避免重复注册 |
| **限流器** | `rateLimiter.compute(key, (k, v) -> ...)` | 滑动窗口限流 |

### 经典模式：缓存 + 自动加载

```java
// 最常用模式：computeIfAbsent 做本地缓存
ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();

public User getUser(long userId) {
    return userCache.computeIfAbsent(
        String.valueOf(userId),
        id -> userDao.findById(Long.parseLong(id))  // 缓存未命中时查库
    );
}
```

> **注意**：`computeIfAbsent` 的 mappingFunction 是**原子的**（在计算期间桶位锁定），但 mappingFunction 中**不应有耗时操作**，否则会阻塞该桶位的其他操作。

---

## Q7: ConcurrentHashMap 的常见面试陷阱

| 陷阱 | 正解 |
|------|------|
| "ConcurrentHashMap 是绝对线程安全的" | **不保证复合操作原子性**：先 get 再 put 不是原子的，需用 `putIfAbsent` / `computeIfAbsent` |
| "读操作完全无锁" | 读 Node 的 val/next 是 volatile 读，无锁；但 size() 等方法需遍历，是**弱一致性** |
| "size() 返回精确值" | 返回的是**近似值**（1.8 用 CounterCell[] 分散计数 CAS 汇总，类似 LongAdder） |
| "迭代器是强一致性的" | **弱一致性**：迭代器遍历的是创建时的快照，迭代期间其他线程的修改可能看不到 |
| "1.8 去掉了分段锁" | 锁粒度从 Segment 变为桶位，本质上更细的分段，不是"无锁" |

---

## 一句话总结

ConcurrentHashMap 是线程安全的高并发 Map，JDK 1.7 用 Segment[] + ReentrantLock 分段锁（默认 16 段），JDK 1.8 改为 Node[] + CAS + synchronized 桶位锁（锁粒度更细、支持并发扩容、红黑树优化查询），不允许 null key/value 以消除并发二义性，常用 `computeIfAbsent` / `putIfAbsent` 做原子复合操作。
