---
tags:
  - Java
  - 并发
  - 线程池
category: Java/并发/线程池
---

# 为什么默认线程数是 CPU 核心数 × 2?

## Q1: 线程数的理论公式是什么?

**Little's Law 推导出的线程数计算公式**：

$$
threads = N_{CPU} \times (1 + \frac{W}{C})
$$

| 参数 | 含义 | 说明 |
|------|------|------|
| $N_{CPU}$ | CPU 核心数 | `Runtime.getRuntime().availableProcessors()` |
| $W$ | 等待时间（Wait） | 线程等待 IO、锁、睡眠等的时间 |
| $C$ | 计算时间（Compute） | 线程实际使用 CPU 的时间 |
| $W/C$ | 等待/计算比 | 任务中阻塞时间与计算时间的比值 |

---

## Q2: CPU 密集型任务：为什么线程数 ≈ CPU 核心数?

当任务几乎没有等待（纯计算），$W \approx 0$：

$$
threads = N_{CPU} \times (1 + \frac{0}{C}) = N_{CPU}
$$

**多出来的线程只会增加上下文切换开销，不会提升吞吐量。**

```java
// CPU 密集型示例：加密、压缩、数学计算
ExecutorService pool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()  // 如 8 核 → 8 线程
);

for (int i = 0; i < 10000; i++) {
    pool.submit(() -> {
        // 纯计算，无 IO 等待
        double result = Math.log(Math.sqrt(i) * Math.PI);
    });
}
```

| 场景 | W/C | 推荐线程数 |
|------|-----|-----------|
| 纯计算（加密、排序） | ≈ 0 | $N_{CPU}$ |
| 计算为主，少量等待 | ≈ 0.5 | $N_{CPU} \times 1.5$ |

---

## Q3: IO 密集型任务：为什么线程数 ≈ CPU 核心数 × 2?

当任务的等待时间约等于计算时间（$W \approx C$）时：

$$
threads = N_{CPU} \times (1 + \frac{C}{C}) = N_{CPU} \times 2
$$

**IO 等待期间 CPU 空闲，多出的线程可填补等待间隙，提高 CPU 利用率。**

```java
// IO 密集型示例：HTTP 请求、数据库查询、文件读写
ExecutorService pool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2  // 如 8 核 → 16 线程
);

for (int i = 0; i < 1000; i++) {
    pool.submit(() -> {
        // 网络请求：大部分时间在等待响应（W >> C）
        String response = httpClient.get("https://api.example.com/data");
        process(response);  // 短暂计算
    });
}
```

| 场景 | W/C | 推荐线程数 |
|------|-----|-----------|
| 常规 IO（Web 请求、DB 查询） | ≈ 1 | $N_{CPU} \times 2$ |
| 重度 IO（慢网络、大文件传输） | ≈ 10+ | $N_{CPU} \times (10 \sim 20)$ |

---

## Q4: 公式推导与直觉理解

### 推导思路

1. 单核上 1 个纯计算线程 → CPU 利用率 100%
2. 如果该线程 50% 时间在等待 IO → CPU 利用率 50%
3. 要让 CPU 不空闲 → 需要 2 个这样的线程（1 个计算时，另 1 个在等待的线程刚好被唤醒）
4. 推广到 N 核 → $N \times (1 + W/C)$

### 直觉类比

```
8 核 CPU，IO 密集型（W/C = 1）：

时间 →  T1  T2  T3  T4  T5  ... T16
CPU1   [计算][等待][计算][等待]...
CPU2   [计算][等待][计算][等待]...
...
CPU8   [计算][等待][计算][等待]...

→ 8 核 × 2 = 16 线程，确保任意时刻每核都有线程可执行
```

---

## Q5: 主流框架的默认线程数配置

| 框架 | 默认线程数 | 计算方式 | 场景 |
|------|-----------|---------|------|
| **Netty** (worker) | `CPU * 2` | `Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2))` | IO 密集（网络 IO） |
| **Tomcat** (maxThreads) | 200 | 固定值 200（非公式，因 Servlet 阻塞模型 W/C 很高） | IO 密集（HTTP 请求） |
| **Dubbo** (provider) | `CPU + 1`（默认 200 实际） | 早期版本 `CPU + 1`，后改为 200 | IO 密集（RPC 调用） |
| **Dubbo** (consumer) | `CPU + 1` | `Runtime.getRuntime().availableProcessors() + 1` | IO 密集 |
| **ForkJoinPool** (parallelism) | `CPU - 1` | `Runtime.getRuntime().availableProcessors() - 1` | CPU 密集（并行流） |
| **WebFlux** (Netty) | `CPU * 2` | 同 Netty | IO 密集（响应式） |
| **RabbitMQ Consumer** | `CPU * 2` ~ `CPU * 5` | 视消息处理 IO 比例 | IO 密集（消息消费） |

### Netty 源码

```java
// io.netty.channel.MultithreadEventLoopGroup
protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
}

static {
    DEFAULT_EVENT_LOOP_THREADS = Math.max(1,
        SystemPropertyUtil.getInt("io.netty.eventLoopThreads",
            NettyRuntime.availableProcessors() * 2));
}
```

### Tomcat 配置

```xml
<!-- server.xml -->
<Connector port="8080"
           maxThreads="200"        <!-- IO 密集，W/C 高，需要更多线程 -->
           minSpareThreads="10"
           acceptCount="100" />
```

---

## Q6: 实际项目中如何调优?

### 三步法

1. **分类**：先判断任务是 CPU 密集还是 IO 密集
2. **估算 W/C**：通过压测或 profiling 得到等待/计算比
3. **微调**：以公式为起点，压测观察 CPU 利用率和吞吐量，微调 ± 20%

### 监控指标

| 指标 | 理想值 | 说明 |
|------|--------|------|
| CPU 利用率 | 70%~90% | 过低 → 线程不够；过高 → 线程太多或计算过重 |
| 线程池活跃线程数 | 接近核心线程数 | 经常打满 → 需扩容 |
| 任务队列长度 | 不持续增长 | 持续增长 → 处理速度跟不上提交速度 |
| 上下文切换次数 | < 5000次/秒（Linux） | 过高 → 线程数过多 |

```java
// 实际调优示例：8 核机器，HTTP 调用占比 80%
// W/C ≈ 4 → threads = 8 × (1 + 4) = 40
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    40,    // corePoolSize
    40,    // maxPoolSize
    60,    // keepAliveSeconds
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## 一句话总结

线程数公式 $threads = N_{CPU} \times (1 + W/C)$ 揭示了 CPU 密集型任务线程数应约等于核心数、IO 密集型任务线程数应约等于核心数乘 2 的本质原因——IO 等待期间 CPU 空闲需要更多线程填补，这也是 Netty 等框架默认 `CPU * 2` 的理论依据。
