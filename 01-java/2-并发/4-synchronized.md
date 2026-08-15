---
tags:
  - Java
  - 并发
category: Java/并发
---

# synchronized

## synchronized 是什么？

synchronized 是 Java 内置的互斥锁机制，用于实现线程同步，保证同一时刻只有一个线程可以执行被保护的代码块或方法。它是 JVM 层面的锁，不需要手动释放，退出同步代码块时自动释放锁。

## 三种使用方式

### 1. 修饰实例方法——锁当前实例对象

```java
public synchronized void method() {
    // 同一实例对象的此方法互斥
}
```

锁的是当前实例对象 `this`，不同实例之间不互斥。

### 2. 修饰静态方法——锁 Class 对象

```java
public static synchronized void method() {
    // 同一 Class 的此方法互斥
}
```

锁的是当前类的 `Class` 对象（如 `MyClass.class`），所有实例共享同一把锁。

### 3. 修饰代码块——锁指定对象

```java
// 锁当前实例
synchronized (this) { /* ... */ }

// 锁 Class 对象
synchronized (MyClass.class) { /* ... */ }

// 锁任意对象
synchronized (lockObject) { /* ... */ }
```

比修饰整个方法更灵活，可以精确控制同步范围，减少锁持有时间。

### 三种方式对比

| 使用方式 | 锁对象 | 作用范围 | 粒度 |
|---|---|---|---|
| 实例方法 | 当前实例 this | 整个方法体 | 粗 |
| 静态方法 | Class 对象 | 整个方法体 | 粗 |
| 代码块 | 指定对象 | 代码块内 | 细（推荐） |

## 底层原理

### 同步代码块：monitorenter / monitorexit

```java
synchronized (obj) {
    // 同步代码
}
```

编译后字节码：

```
monitorenter      // 获取 monitor 锁，计数器 +1
// 同步代码
monitorexit       // 释放 monitor 锁，计数器 -1
monitorexit       // 异常路径的释放（保证异常时也能释放锁）
```

- `monitorenter`：每个对象关联一个 Monitor，尝试获取该对象的 Monitor 所有权。成功则将计数器设为 1；如果当前线程已拥有则计数器 +1（可重入）；如果被其他线程占用则阻塞。
- `monitorexit`：将计数器 -1，减为 0 时释放 Monitor。编译器会自动生成一个异常处理的 monitorexit，确保即使抛出异常也能释放锁。

### 同步方法：ACC_SYNCHRONIZED 标志位

```java
public synchronized void method();
```

方法的 `access_flags` 中会设置 `ACC_SYNCHRONIZED` 标志位。JVM 调用该方法时，若为实例方法则获取当前对象的 Monitor，若为静态方法则获取对应 Class 对象的 Monitor。

### Monitor 对象（ObjectMonitor）

每个对象在 JVM 中关联一个 `ObjectMonitor`（C++ 实现），核心字段：

| 字段 | 含义 |
|---|---|
| `_owner` | 持有锁的线程 |
| `_EntryList` | 阻塞等待锁的线程队列 |
| `_WaitSet` | 调用 wait() 后等待的线程集合 |
| `_count` | 重入计数器 |

获取锁失败时线程进入 `_EntryList` 排队；调用 `wait()` 时线程释放锁并进入 `_WaitSet`，被 `notify()` 唤醒后进入 `_EntryList` 重新竞争。

## 锁升级过程（JDK 1.6+）

JDK 1.6 引入了锁升级机制，synchronized 不再一上来就使用重量级锁，而是根据竞争情况逐步升级。锁信息存储在对象头的 **Mark Word** 中。

### 升级方向

```
无锁 → 偏向锁 → 轻量级锁 → 重量级锁
```

升级是单向的（不可降级，GC 时可以批量撤销偏向锁）。

### 无锁

- 对象刚创建、尚未被任何线程加锁时的状态
- Mark Word 存储对象的哈希码、GC 分代年龄等信息

### 偏向锁

- **适用场景**：只有一个线程访问同步代码（无竞争）
- **原理**：在 Mark Word 中记录获取锁的线程 ID，后续该线程再进入同步块时，只需判断线程 ID 是否一致，无需任何同步操作（甚至不需要 CAS）
- **获取过程**：
  1. 检查 Mark Word 中线程 ID 是否为当前线程
  2. 是 → 直接进入同步块（偏向成功）
  3. 否 → 通过 CAS 尝试将线程 ID 设为当前线程
  4. CAS 成功 → 偏向成功；CAS 失败 → 说明存在竞争，触发**偏向锁撤销**
- **偏向锁撤销**：
  - 需要等待全局安全点（STW）
  - 检查持有偏向锁的线程是否存活、是否已退出同步块
  - 若已退出 → 重置为无锁；若未退出 → 升级为轻量级锁
  - 撤销开销较大，因此适合无竞争场景
- **批量重偏向 / 批量撤销**：当某个类的对象撤销偏向锁次数达到阈值时，JVM 会批量重偏向或直接批量撤销该类的偏向锁

### 轻量级锁

- **适用场景**：少量线程交替执行同步代码，虽有竞争但不激烈
- **原理**：通过 CAS 操作竞争锁，竞争失败的线程自旋等待（不阻塞）
- **获取过程**：
  1. 在当前线程栈帧中创建 Lock Record，拷贝 Mark Word（称为 Displaced Mark Word）
  2. CAS 尝试将对象头 Mark Word 指向 Lock Record
  3. 成功 → 获取轻量级锁
  4. 失败 → 自旋重试；自旋超过阈值 → 升级为重量级锁
- **释放过程**：CAS 将 Displaced Mark Word 替换回 Mark Word，成功则释放；失败说明有竞争，升级为重量级锁并唤醒被阻塞的线程

### 重量级锁

- **适用场景**：多线程激烈竞争、持有锁时间长
- **原理**：依赖 Monitor（ObjectMonitor），竞争失败的线程调用操作系统互斥量进入阻塞状态
- **缺点**：涉及用户态与内核态切换，线程阻塞/唤醒需要操作系统介入，开销大
- **优点**：线程阻塞不消耗 CPU，适合锁持有时间长的场景

### 锁升级对比

| 锁类型 | 适用场景 | 获取方式 | 优点 | 缺点 |
|---|---|---|---|---|
| 偏向锁 | 单线程重入 | 检查线程 ID | 几乎无开销 | 有竞争时撤销代价大 |
| 轻量级锁 | 少量交替竞争 | CAS + 自旋 | 避免阻塞 | 自旋消耗 CPU |
| 重量级锁 | 激烈竞争 | Monitor 互斥 | 不消耗 CPU（阻塞等待） | 用户态内核态切换开销大 |

## synchronized 是可重入的

同一个线程可以重复获取同一把 synchronized 锁，不会自己阻塞自己：

```java
public synchronized void methodA() {
    methodB(); // 可以调用，不会死锁
}

public synchronized void methodB() {
    // 同一把锁，可重入
}
```

底层通过 Monitor 的计数器实现：每次 monitorenter 计数器 +1，每次 monitorexit 计数器 -1，减到 0 才真正释放锁。

## synchronized 与 volatile 的区别

| 特性 | synchronized | volatile |
|---|---|---|
| 原子性 | 保证（互斥访问） | 不保证（仅保证可见性） |
| 可见性 | 保证（释放锁时刷新到主内存） | 保证（强制读写主内存） |
| 有序性 | 保证（内部相当于有内存屏障） | 保证（禁止指令重排序） |
| 阻塞 | 可能阻塞 | 不会阻塞 |
| 使用范围 | 方法或代码块 | 仅修饰变量 |
| 性能 | 较重（可能涉及锁升级） | 较轻（无锁机制） |
| 编译优化 | 无 | 禁止 JIT 重排序 |

**典型场景**：
- `volatile` 适用于一个线程写、多个线程读的场景（如状态标志位）
- `synchronized` 适用于复合操作需要保证原子性的场景（如 count++）

```java
// volatile 不能保证原子性
volatile int count = 0;
count++; // 非原子操作，线程不安全

// synchronized 保证原子性
synchronized void increment() {
    count++; // 原子操作，线程安全
}
```

## synchronized 与 ReentrantLock 简要对比

| 特性 | synchronized | ReentrantLock |
|---|---|---|
| 实现层面 | JVM 内置 | API 层面（AQS） |
| 锁的获取释放 | 自动 | 手动（需 try-finally） |
| 可中断 | 不可 | 可（lockInterruptibly） |
| 公平性 | 非公平 | 可公平/可非公平 |
| 条件变量 | 单个（wait/notify） | 多个（Condition） |
| 锁绑定 | 不支持 | 支持多个 Condition |

> 详细对比见 [ReentrantLock](./ReentrantLock.md)

## 一句话总结

synchronized 是 Java 内置的互斥锁，通过 Monitor 实现互斥与可重入，JDK 1.6 后引入锁升级（偏向锁 → 轻量级锁 → 重量级锁）大幅优化了性能，适合大多数同步场景；与 volatile 互补——synchronized 保证原子性，volatile 保证可见性。
