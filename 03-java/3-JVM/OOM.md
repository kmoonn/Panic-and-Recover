---
tags:
  - Java
  - JVM
category: Java/JVM
---

# OOM 触发场景

`OutOfMemoryError` 指 JVM 无法再分配内存且 GC 也回收不出空间。除程序计数器外，[运行时数据区](内存模型.md)的每个区域几乎都会 OOM，报错信息不同、原因不同。

## 各区域 OOM 一览

| OOM 类型 | 区域 | 典型原因 |
|---|---|---|
| `Java heap space` | 堆 | 对象太多/太大且被 GC Root 引用无法回收；内存泄漏；`-Xmx` 设太小 |
| `GC overhead limit exceeded` | 堆 | GC 频繁但回收甚少（默认 98% 时间 GC 却回收 < 2%）——泄漏前兆 |
| `Metaspace` | 元空间 | 动态生成大量类：CGLIB/字节码增强、热部署反复加载、Groovy 脚本 |
| `Direct buffer memory` | 堆外直接内存 | NIO `allocateDirect` 分配过多，未受 `-Xmx` 限制 |
| `unable to create new native thread` | 栈（线程） | 线程数过多，每个线程栈占内存耗尽系统资源 |
| `Requested array size exceeds VM limit` | 堆 | 申请超大数组（接近 Integer.MAX_VALUE） |

> 注意：栈深度递归过深是 `StackOverflowError`（Error 但**不是 OOM**），要区分。

## 最常见的几种

### 1. 堆 OOM（最高频）

- **内存泄漏**：对象已无用但仍被引用，无法回收。典型：静态集合越加越多、[ThreadLocal 不 remove](../2-并发/4-线程池/使用注意事项.md)、监听器未注销、连接未关闭。
- **内存溢出（非泄漏）**：一次性加载超大数据（如 `SELECT *` 全表进内存、大文件一次读完）、缓存无上限。

### 2. 无界队列/无限线程 → OOM

- 用 `Executors.newFixedThreadPool` 的**无界队列**，任务堆积撑爆堆；
- `newCachedThreadPool` 线程数无上限，海量线程 → `unable to create new native thread`。
- 这正是[创建线程池](../2-并发/4-线程池/创建线程池.md)必须手动 `ThreadPoolExecutor` + 有界队列的原因。

### 3. Metaspace OOM

框架大量动态生成代理类（Spring CGLIB、MyBatis），或应用反复热部署未卸载旧类加载器。

## 排查思路

1. **加参数留证据**：`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=...`，OOM 时自动 dump 堆。
2. **分析 dump**：MAT / JProfiler 看**支配树（Dominator Tree）**，找占用最大的对象和 GC Root 引用链。
3. **在线看趋势**：`jstat -gcutil` 看各区使用率与 GC 频率；`jmap -histo` 看对象直方图；`jmap -clstats` 看类加载（查 Metaspace）。
4. **判断泄漏 vs 溢出**：内存持续增长、Full GC 后也降不下来 → 泄漏；某次突发峰值 → 溢出/大对象。

## 解决方向

| 类型 | 解决 |
|---|---|
| 泄漏 | 定位引用链，断开无用引用（集合清理、ThreadLocal.remove、关闭资源） |
| 溢出 | 分批/流式处理、分页查询、限制缓存大小、加 `-Xmx` |
| Metaspace | 限制 `-XX:MaxMetaspaceSize`、减少动态类生成、排查类加载器泄漏 |
| 直接内存 | 限制 `-XX:MaxDirectMemorySize`、及时释放 `DirectByteBuffer` |
| 线程 OOM | 用线程池控制线程数、减小 `-Xss` |

## 一句话总结

> OOM 按区域分：堆（泄漏/大对象/无界队列）最常见、Metaspace（动态类）、直接内存（NIO）、native thread（线程过多）；排查靠 HeapDump + MAT 找支配对象和引用链，核心是区分"泄漏"（持续涨、Full GC 降不下）还是"溢出"（突发峰值）。
