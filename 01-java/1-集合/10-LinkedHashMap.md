---
tags:
  - Java
  - 集合
category: Java/集合
---

# LinkedHashMap

## 是什么

`LinkedHashMap` 是 `HashMap` 的子类，在 HashMap 的基础上额外维护了一条**双向链表**，用于记录键值对的插入顺序或访问顺序。迭代时的顺序与插入/访问顺序一致，而非 HashMap 的无序。

## 数据结构

```
HashMap（数组 + 链表/红黑树）
  + 双向链表（贯穿所有节点，维护顺序）

每个 Entry 包含 before/after 指针：
┌───┐    ┌───┐    ┌───┐
│ A │←──→│ B │←──→│ C │   ← 双向链表
└───┘    └───┘    └───┘
```

## 两种排序模式

| 模式 | accessOrder | 说明 |
|---|---|---|
| 插入顺序（默认） | `false` | 按插入顺序迭代，先插入的先遍历 |
| 访问顺序 | `true` | 按最近访问顺序迭代，最近访问的排在最后 |

```java
// 插入顺序（默认）
LinkedHashMap<String, Integer> map1 = new LinkedHashMap<>();

// 访问顺序
LinkedHashMap<String, Integer> map2 = new LinkedHashMap<>(16, 0.75f, true);
```

### 访问顺序示例

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
// 顺序：A → B → C

map.get("A");  // 访问 A
// 顺序变为：B → C → A（A 被移到链表尾部）

map.put("B", 20); // 更新 B 也是"访问"
// 顺序变为：C → A → B
```

## LRU 缓存实现

`LinkedHashMap` 天然适合实现 LRU（Least Recently Used）缓存，只需三步：

1. 构造函数设置 `accessOrder = true`
2. 重写 `removeEldestEntry()` 方法，控制最大容量
3. 继承 `LinkedHashMap` 并封装 get/put 方法

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // accessOrder = true
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 当元素数量超过容量时，自动移除最久未访问的元素（链表头部）
        return size() > capacity;
    }

    // 可选：synchronized 保证线程安全
    public synchronized V getCache(K key) {
        return super.get(key);
    }

    public synchronized void putCache(K key, V value) {
        super.put(key, value);
    }
}

// 使用
LRUCache<String, String> cache = new LRUCache<>(3);
cache.put("a", "1");
cache.put("b", "2");
cache.put("c", "3");     // 缓存: a → b → c
cache.get("a");           // 访问 a，缓存: b → c → a
cache.put("d", "4");     // 超出容量，移除最久未访问的 b，缓存: c → a → d
```

### removeEldestEntry 工作原理

每次 `put` 操作后，`LinkedHashMap` 会调用 `removeEldestEntry(eldest)`：
- 返回 `true`：移除链表头部（最老的/最久未访问的）元素
- 返回 `false`（默认）：不移除，Map 可无限增长

## 与 HashMap 的区别

| 对比维度 | LinkedHashMap | HashMap |
|---|---|---|
| 迭代顺序 | 有序（插入/访问顺序） | 无序 |
| 底层结构 | HashMap + 双向链表 | 数组 + 链表/红黑树 |
| 内存开销 | 每个 Entry 多 2 个指针（before/after） | 相对较小 |
| null 键 | 允许 1 个 null 键 | 允许 1 个 null 键 |
| 线程安全 | 否 | 否 |
| LRU 支持 | 天然支持（accessOrder + removeEldestEntry） | 不支持 |
| 性能 | 略慢于 HashMap（维护链表开销） | 略快 |

## 面试高频

**Q：如何用 LinkedHashMap 实现 LRU 缓存？**

A：(1) 构造时设置 `accessOrder = true`，使最近访问的元素排在链表尾部；(2) 重写 `removeEldestEntry()`，当 `size() > capacity` 时返回 `true` 自动移除链表头部（最久未访问的元素）；(3) 封装 get/put 并按需加锁保证线程安全。

**Q：LinkedHashMap 维护顺序的原理？**

A：在 HashMap 基础上，每个 Entry 增加了 `before` 和 `after` 指针，形成一条贯穿所有节点的双向链表。每次插入/访问/删除时，同时维护链表结构，保证迭代顺序与插入/访问顺序一致。

**Q：accessOrder=true 时，哪些操作算"访问"？**

A：`get()`、`put()`（更新已有 key）、`putAll()`（包含已有 key）都会将被操作的节点移到链表尾部。`containsKey()` 不算访问。

## 一句话总结

LinkedHashMap = HashMap + 双向链表，维护插入或访问顺序，通过 accessOrder=true + 重写 removeEldestEntry 可一行代码实现 LRU 缓存。
