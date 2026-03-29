# ConcurrentHashMap

## 数据结构

- JDK 1.7
  - 数组 + 链表
- JDK 1.8
  - 数组 + 链表 + 红黑树

哈希表

## 特点

- **线程安全**

通过分段锁机制实现线程安全：即对每个Segment独立加锁，小数组HashEntry用于存储键值对数据。