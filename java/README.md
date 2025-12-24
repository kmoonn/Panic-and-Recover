# Java

## 基础

- [Java 语言特点]()
  - [Java 与 C++ 的区别]()
- [Java SE]()
- [Java EE]()
- [JMM]()
- [JDK、JRE、JVM、JIT]()
- [字节码]()
- [JIT]()
- [基本数据类型](basics/基本数据类型.md)
  - [包装类](basics/包装类.md)
  - [BigDecimal](basics/BigDecimal.md)
- [成员变量、局部变量、静态变量]()
- [静态方法、实例方法]()
- [重载与重写的区别]()
- [OOP、POP]()
- [封装、继承、多态]()
- [接口与抽象类的区别]()
- [深拷贝与浅拷贝的区别]()
- [Object 类]()
- [== 与 equals() 的区别]()
- [hashCode()]()
- [String 类]()
  - [String、StringBuffer、StringBuilder]()
  - [字符串常量池]()
- [异常]()
  - [Exception 与 Error 的区别]()
  - [Checked Exception 与 Unchecked Exception 的区别]()
  - [try-except-finally]()
  - [try-with-resources]()
- [泛型]()
  - [通配符]()
- ⭐[反射机制]()
- [代理]()
  - [动态代理、静态代理]()
- [注解]()
- [SPI]()
- [序列化、反序列化]()
- [语法糖]()

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
    - ⭐[HashMap 1.7 和 1.8 的区别](collections/HashMap1.7和1.8的区别.md)
      - [红黑树]()
    - [LinkedHashMap](collections/LinkedHashMap.md)
  - ⭐[ConcurrentHashMap](collections/ConcurrentHashMap.md)
  - [HashMap 和 ConcurrentHashMap 的区别](collections/HashMap和ConcurrentHashMap的区别.md)
  - [TreeMap](collections/TreeMap.md)
- [Collection 和 Map 的区别](collections/Collection和Map的区别.md)

## I/O

- [Java I/O 流]()
- ⭐[BIO、NIO、AIO 的区别]()

## 并发

- [进程](concurrency/Process.md)
- [线程](concurrency/Thread.md)
  - [创建方法]()
  - [生命周期]()
  - [调度算法](concurrency/线程调度算法.md)
  - [守护线程与用户线程的区别]()
  - 同步
    - ⭐[synchronized](concurrency/synchronized关键字.md)
    - ⭐[ReentrantLock](concurrency/ReentrantLock关键字.md)
    - ⭐[volatile](concurrency/volatile关键字.md)
    - [synchronized、ReentrantLock 与 volatile 的区别](concurrency/synchronized、ReentrantLock与volatile的区别.md)
- [协程](concurrency/协程.md)
- [进程、协程与线程的区别](concurrency/进程、协程与线程的区别.md)
- [并行与并发的区别](concurrency/并行与并发的区别.md)
- 线程池
  - [核心参数]()
  - [工作流程]()
  - [拒绝策略]()
- 锁
  - [乐观锁]()
  - [悲观锁]()
- ⭐[CAS]()

## JVM

- [内存模型](virtual_machine/内存模型.md)
- [垃圾回收]()
  - [垃圾回收算法]()
  - [垃圾回收器]()
- [类加载机制]()
- [JVM 参数]()
  - [性能调优]()
- [线上问题排查]()

## 新特性

- [Java 8]()
- [Java 9]()
  - [AOT]()
- [Java 10]()