---
tags:
  - Java
  - 集合
category: Java/集合
---

# TreeMap

## 是什么

`TreeMap` 是基于**红黑树**（Red-Black Tree）实现的有序 Map，键值对按照键的自然顺序（`Comparable`）或自定义 `Comparator` 排序。查找、插入、删除操作的时间复杂度均为 **O(log n)**。

## 红黑树简介

红黑树是一种**自平衡二叉搜索树**，通过颜色规则和旋转操作维持平衡，保证最长路径不超过最短路径的 2 倍。

**红黑树五条性质：**

1. 每个节点是红色或黑色
2. 根节点是黑色
3. 所有叶子节点（NIL）是黑色
4. 红色节点的子节点必须是黑色（不能有连续红节点）
5. 从任一节点到其每个叶子的所有路径包含相同数目的黑色节点

**为什么用红黑树而不是 AVL 树？** 红黑树在插入/删除时旋转次数更少，更适合写操作较多的场景；AVL 树更严格平衡，查询略快但维护成本更高。TreeMap 兼顾读写，选择红黑树是合理的折中。

## 核心特点

| 特点 | 说明 |
|---|---|
| 有序性 | 按键自然排序或 Comparator 排序 |
| 时间复杂度 | get/put/remove 均为 O(log n) |
| 不允许 null 键 | 插入 null 键抛 `NullPointerException` |
| 允许 null 值 | 值可以为 null |
| 非线程安全 | 多线程访问需外部同步 |
| 实现 `NavigableMap` | 提供丰富的导航方法 |

## 关键 API

```java
TreeMap<Integer, String> map = new TreeMap<>();

// 导航方法
map.firstKey();            // 最小键
map.lastKey();             // 最大键
map.lowerKey(5);           // 小于 5 的最大键
map.higherKey(5);          // 大于 5 的最小键
map.floorKey(5);           // 小于等于 5 的最大键
map.ceilingKey(5);         // 大于等于 5 的最小键

// 范围视图
map.headMap(5);            // 键 < 5 的子映射
map.tailMap(5);            // 键 >= 5 的子映射
map.subMap(2, 8);          // 2 <= 键 < 8 的子映射

// 排序遍历
map.entrySet().forEach(e -> System.out.println(e.getKey() + "=" e.getValue()));
```

## 自定义排序

```java
// 自然排序：键实现 Comparable
TreeMap<String, Integer> map1 = new TreeMap<>();

// 自定义排序：传入 Comparator
TreeMap<String, Integer> map2 = new TreeMap<>(Comparator.reverseOrder());

// 按值排序（间接方式）
List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
list.sort(Map.Entry.comparingByValue());
```

## 与 HashMap 的对比

| 对比维度 | TreeMap | HashMap |
|---|---|---|
| 底层结构 | 红黑树 | 数组 + 链表/红黑树（JDK 8+） |
| 是否有序 | 有序（按键排序） | 无序 |
| null 键 | 不允许 | 允许 1 个 null 键 |
| 时间复杂度 | get/put/remove: O(log n) | 平均 O(1)，最坏 O(log n) |
| 内存开销 | 每个节点存左右子树指针 + 颜色 | 相对较小 |
| 范围查询 | 支持（subMap/headMap/tailMap） | 不支持 |
| 适用场景 | 需要有序遍历、范围查询 | 通用键值存储 |

## 线程安全替代

`TreeMap` 非线程安全，多线程场景下应使用：

```java
// 方式一：Collections 包装（全表锁，性能差）
SortedMap<K, V> syncMap = Collections.synchronizedSortedMap(new TreeMap<>());

// 方式二：ConcurrentSkipListMap（推荐，基于跳表，并发性能好）
ConcurrentSkipListMap<K, V> concurrentMap = new ConcurrentSkipListMap<>();
```

## 适用场景

- 需要按**键有序遍历**的 Map
- 需要**范围查询**（如"查找分数在 80-90 之间的学生"）
- 需要**排名/前 N /后 N** 查询
- 实现**一致性哈希**（如 Kafka 分区分配）

## 面试高频

**Q：TreeMap 的 put 操作时间复杂度？**

A：O(log n)。因为底层是红黑树，每次插入需要 O(log n) 查找位置，再通过旋转和变色维持平衡（旋转最多 O(log n) 次）。

**Q：TreeMap 为什么不允许 null 键？**

A：因为插入时需要调用 `compareTo` 或 `Comparator.compare` 进行比较排序，null 与任何对象比较都会抛 `NullPointerException`。

**Q：多线程环境下用什么替代 TreeMap？**

A：用 `ConcurrentSkipListMap`，基于跳表实现，同样支持有序性和范围查询，且并发性能远优于 `Collections.synchronizedSortedMap`。

## 一句话总结

TreeMap 基于红黑树实现按键排序的有序 Map，查找/插入 O(log n)，适用于有序遍历和范围查询场景，非线程安全，多线程环境用 ConcurrentSkipListMap 替代。
