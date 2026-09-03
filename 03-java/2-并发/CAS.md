---
tags:
  - Java
  - 并发
category: Java/并发
---

# CAS (Compare And Swap)

## Q1: 什么是 CAS?

**CAS 是一条 CPU 原语级别的原子指令**，其语义为：比较内存中的值是否与预期旧值相等，若相等则将内存值更新为新值，否则什么都不做，整个操作不可分割（原子性）。

| 特性 | 说明 |
|------|------|
| 全称 | Compare And Swap（比较并交换） |
| 本质 | CPU 硬件级原子指令（x86 → `CMPXCHG`） |
| 原子性 | 由 CPU 保证，无需软件锁 |
| 返回值 | 通常返回 `true/false` 表示是否成功 |

```java
// CAS 伪代码（实际由 CPU 原子指令实现，以下仅为语义说明）
boolean CAS(int* addr, int expected, int newValue) {
    if (*addr == expected) {   // 比较
        *addr = newValue;      // 交换
        return true;
    }
    return false;              // 失败，重试或放弃
}
```

---

## Q2: CAS 的三个操作数是什么?

CAS 指令涉及 **3 个操作数**：

| 操作数 | 含义 | 说明 |
|--------|------|------|
| V (Value) | 内存地址 | 需要读写的变量在主内存中的地址 |
| A (Expected) | 期望旧值 | 线程认为该变量当前的值 |
| B (New) | 新值 | 如果 V == A，要把 V 更新为此值 |

**执行流程**：

```
1. 读取内存地址 V 的当前值
2. 比较 V 的值 == A ?
   ├── 相等 → 将 V 设为 B，返回 true
   └── 不等 → 什么都不做，返回 false
3. 若失败，通常进入自旋重试（Spin）
```

---

## Q3: Java 中如何实现 CAS?

### 核心 API：`sun.misc.Unsafe`

Java 无法直接操作内存，通过 `Unsafe` 类调用 native 方法实现 CAS：

```java
// Unsafe 提供的 CAS 方法
public final native boolean compareAndSwapInt(Object o, long offset,
                                               int expected, int x);
public final native boolean compareAndSwapLong(Object o, long offset,
                                                long expected, long x);
public final native boolean compareAndSwapObject(Object o, long offset,
                                                  Object expected, Object x);
```

### AtomicInteger 源码示例

```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset; // value 字段在对象中的偏移量

    static {
        valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
    }

    private volatile int value; // volatile 保证可见性

    // 自增：CAS + 自旋
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }
}

// Unsafe.getAndAddInt 源码（自旋 CAS）
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);    // 读取当前值
    } while (!compareAndSwapInt(o, offset, v, v + delta)); // CAS 自旋
    return v;
}
```

### 常用原子类一览

| 类名 | 底层 CAS 方法 | 说明 |
|------|--------------|------|
| `AtomicInteger` | `compareAndSwapInt` | 整数原子操作 |
| `AtomicLong` | `compareAndSwapLong` | 长整数原子操作 |
| `AtomicReference` | `compareAndSwapObject` | 引用类型原子操作 |
| `AtomicStampedReference` | `compareAndSwapObject` | 带版本号的引用（解决 ABA） |
| `AtomicIntegerFieldUpdater` | `compareAndSwapInt` | 对已有 volatile int 字段做 CAS |

---

## Q4: 什么是 ABA 问题? 如何解决?

### ABA 问题

线程 1 读取值 A，被挂起；线程 2 将 A → B → A；线程 1 恢复后 CAS 成功（因为值仍为 A），但**中间状态已被修改**，逻辑上不该成功。

```
时间线：
  线程1: read(A) ───────────────── CAS(A→C) ✓ （不应该成功！）
  线程2: ──── A→B ──── B→A ──────
```

**危害场景**：栈操作（A→B→A 后栈结构已变）、无锁队列等。

### 解决方案：AtomicStampedReference（带版本号）

每次修改时版本号 +1，CAS 时同时比较 **值 + 版本号**，即使值回到 A，版本号也不同。

```java
AtomicStampedReference<String> ref =
        new AtomicStampedReference<>("A", 1);

int stamp = ref.getStamp();          // 获取当前版本号
String current = ref.getReference(); // 获取当前值

// 其他线程修改：A→B→A，但版本号已经从 1→2→3
// 当前线程 CAS 时仍用旧版本号 1，必然失败
boolean success = ref.compareAndSet(current, "C", stamp, stamp + 1);
// success = false，正确拒绝
```

| 方案 | 原理 | 适用场景 |
|------|------|---------|
| `AtomicStampedReference` | 值 + int 版本号 | 需要精确区分每次修改 |
| `AtomicMarkableReference` | 值 + boolean 标记 | 只关心"是否被修改过" |

---

## Q5: CAS 和 synchronized 有什么区别?

| 对比维度 | CAS | synchronized |
|---------|-----|-------------|
| 实现方式 | CPU 原子指令 + 自旋 | JVM 监视器锁（monitorenter/monitorexit） |
| 锁类型 | **无锁（Lock-Free）** | **阻塞锁（Blocking）** |
| 竞争时行为 | 自旋重试（消耗 CPU） | 线程阻塞挂起（上下文切换开销） |
| 低竞争性能 | 极高（无锁开销） | 较低（获取/释放锁开销） |
| 高竞争性能 | 较低（自旋消耗 CPU） | 较好（线程挂起让出 CPU） |
| 公平性 | 无公平保证 | 可配置公平/非公平 |
| 适用场景 | 简单原子操作、低竞争 | 复杂临界区、高竞争 |

**选择建议**：
- 简单的 `i++`、单字段更新 → CAS（`AtomicInteger`）
- 多步复合操作、临界区较大 → `synchronized` / `ReentrantLock`
- 竞争极高且操作简单 → `LongAdder`（分段 CAS，优于 `AtomicLong`）

---

## Q6: CAS 的应用场景有哪些?

### 1. 原子类（java.util.concurrent.atomic）

```java
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();  // CAS 自旋
```

### 2. ConcurrentHashMap（JDK 1.8）

```java
// 初始化 table 时：CAS 设置 sizeCtl
// put 时：空桶位用 CAS 插入首节点，非空用 synchronized
if (tab[i] == null) {
    if (casTabAt(tab, i, null, new Node<>(hash, key, value, null)))
        break;
}
```

### 3. AQS（AbstractQueuedSynchronizer）

```java
// 设置独占锁持有线程
if (compareAndSetState(0, 1)) {
    setExclusiveOwnerThread(Thread.currentThread());
    return true;
}
// 入队：CAS 设置尾节点
if (compareAndSetTail(pred, node)) {
    pred.next = node;
    return node;
}
```

### 4. 数据库乐观锁

```sql
UPDATE account SET balance = balance - 100, version = version + 1
WHERE id = 1 AND version = 5;  -- CAS 语义
```

---

## 一句话总结

CAS 是 CPU 原子指令实现的"比较并交换"无锁并发原语，Java 通过 Unsafe 调用，广泛用于原子类、ConcurrentHashMap、AQS 等核心并发组件；其 ABA 问题可通过版本号（AtomicStampedReference）解决，低竞争场景性能远超锁，高竞争时应考虑锁或分段策略。
