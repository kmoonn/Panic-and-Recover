# Java

## 集合

- Collection
  - List 接口
    - [Vector](collections/Vector.md)
    - ⭐[ArrayList](collections/ArrayList.md)
    - ⭐[LinkedList](collections/LinkedList.md)
      - [ArrayList 和 LinkedList 的区别](collections/ArrayList和LinkedList的区别.md)
  - Set 接口
    - [HashSet](collections/HashSet.md)
      - [LinkedHashSet](collections/LinkedHashSet.md)
    - [TreeSet](collections/TreeSet.md)
      - [compareTo 和 compare 的区别](collections/compareTo和compare的区别.md)
  - [List 和 Set 的区别](collections/List和Set的区别.md)
- Map
  - [HashTable](collections/HashTable.md)
  - ⭐[HashMap](collections/HashMap.md)
    - [HashMap 1.7 和 1.8 的区别](collections/HashMap1.7和1.8的区别.md)
    - [LinkedHashMap](collections/LinkedHashMap.md)
  - ⭐[ConcurrentHashMap](collections/ConcurrentHashMap.md)
  - [HashMap 和 ConcurrentHashMap 的区别](collections/HashMap和ConcurrentHashMap的区别.md)
  - [TreeMap](collections/TreeMap.md)
- [Collection 和 Map 的区别](collections/Collection和Map的区别.md)

## 并发

- 进程
- 协程
- 线程
  - [创建方法]()
  - [生命周期]()
  - [守护线程与用户线程的区别]()
  - 同步
    - ⭐[synchronized](concurrency/synchronized关键字.md)
    - ⭐[ReentrantLock](concurrency/ReentrantLock关键字.md)
    - [volatile](concurrency/volatile关键字.md)
    - [synchronized、ReentrantLock 与 volatile 的区别](concurrency/synchronized、ReentrantLock与volatile的区别.md)
- [进程、协程与线程的区别](concurrency/进程、协程与线程的区别.md)
- [并行与并发的区别](concurrency/并行与并发的区别.md)
- 线程池
  - [核心参数]()
  - [工作流程]()
  - [拒绝策略]()
- 锁
  - [乐观锁]()
  - [悲观锁]()

## JVM

- [内存模型](virtual_machine/内存模型.md)
- [垃圾回收]()
  - [垃圾回收算法]()
  - [垃圾回收器]()