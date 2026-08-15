---
tags:
  - Python
category: Python
---

# Python 线程与 GIL

## GIL 是什么

**GIL（Global Interpreter Lock）** 是 CPython 的一把全局互斥锁，任何线程要执行 Python 字节码都必须先获取 GIL。

这意味着：**同一时刻只有一个线程在执行 Python 代码**，多线程无法利用多核。

## 为什么要有 GIL

CPython 的内存管理（引用计数）不是线程安全的：

- 对象的引用计数增减是频繁操作
- 如果不加锁，多线程同时修改引用计数 → 对象被提前回收 → 崩溃
- GIL 是最简单的方案：一把大锁保护所有对象

## GIL 的影响

| 场景 | 多线程表现 | 原因 |
|---|---|---|
| **CPU 密集型** | ❌ 比单线程还慢 | 线程频繁争 GIL，无并行收益 + 切换开销 |
| **IO 密集型** | ✅ 有明显加速 | IO 等待时释放 GIL，其他线程可执行 |

```python
# CPU 密集型：多线程反而更慢
def cpu_task():
    sum(i * i for i in range(10_000_000))

# 单线程: 0.8s
# 多线程(4个): 1.2s  ← 更慢！GIL 争抢开销

# IO 密集型：多线程明显加速
def io_task():
    requests.get("https://httpbin.org/delay/1")

# 单线程(10次): 10s
# 多线程(10个): 1s  ← 10倍加速
```

## 多线程 vs 多进程 vs 协程

| | 多线程 | 多进程 | 协程（asyncio） |
|---|---|---|---|
| 并行 | ❌ 受 GIL 限制 | ✅ 真正并行 | ❌ 单线程并发 |
| 适合场景 | IO 密集 | CPU 密集 | IO 密集（高并发） |
| 切换开销 | 中 | 大 | 极小（用户态切换） |
| 通信 | 共享变量（需加锁） | IPC | 无需通信 |
| 稳定性 | 线程崩溃拖垮进程 | 进程隔离 | 单线程，异常可控 |

## 选择策略

```
CPU 密集型？
  ├── 是 → 多进程（multiprocessing / ProcessPoolExecutor）
  └── 否 → IO 密集型？
              ├── 简单场景 → 多线程（threading / ThreadPoolExecutor）
              └── 高并发（>1000连接）→ 协程（asyncio + aiohttp）
```

## 绕开 GIL 的方式

| 方式 | 原理 |
|---|---|
| 多进程 | 每个进程有独立 GIL |
| C 扩展 | NumPy/Pandas 底层 C 代码执行时释放 GIL |
| `multiprocessing` | 用进程池替代线程池 |
| 子进程 | `subprocess` 调外部程序 |
| 其他解释器 | Jython、IronPython 无 GIL（但生态差） |

```python
# CPU 密集用进程池
from concurrent.futures import ProcessPoolExecutor

with ProcessPoolExecutor() as executor:
    results = list(executor.map(cpu_task, range(8)))
```

## 线程安全与锁

GIL 只保证**字节码执行**的原子性，不保证业务逻辑的原子性：

```python
# ❌ 虽然 GIL 存在，这仍然不安全
count = 0
def increment():
    global count
    for _ in range(1_000_000):
        count += 1  # 不是原子操作（读 → 加 → 写）

# 两个线程各 increment 一次
# 期望 count = 2_000_000，实际 < 2_000_000
```

`count += 1` 被编译为多条字节码，GIL 会在字节码之间切换线程。

```python
# ✅ 用 Lock 保证
import threading

count = 0
lock = threading.Lock()

def increment():
    global count
    for _ in range(1_000_000):
        with lock:
            count += 1
```

## 总结一句话

> GIL 让 Python 多线程无法 CPU 并行，但 IO 场景仍有效；CPU 密集用多进程，IO 密集高并发用协程，线程安全仍需加锁。
