---
tags:
  - 操作系统
category: 操作系统
---

# Linux 内存分布

## 面试Q&A

### Q1: Linux 进程的虚拟地址空间布局是怎样的？

从低地址到高地址依次为：

```
高地址 0xFFFFFFFFFFFFFFFF (64位)
┌─────────────────────────────────┐
│         Kernel Space            │  ← 内核空间，用户进程不可访问
│         (高地址区间)              │     32位: 1GB  64位: 128TB
├─────────────────────────────────┤
│         Stack                   │  ← 栈：局部变量、函数调用帧
│         ↓ 向低地址增长            │
│                                 │
│         (空闲区域)                │
│                                 │
│         Shared Libraries        │  ← 共享库 (mmap区域)
│         (libpthread, libc...)   │
│                                 │
│         ↑ 向高地址增长            │
│         Heap                    │  ← 堆：malloc / new 分配
│                                 │
├─────────────────────────────────┤
│         BSS Segment             │  ← 未初始化的全局/静态变量，零初始化
│         (Block Started by Sym.) │
├─────────────────────────────────┤
│         Data Segment            │  ← 已初始化的全局/静态变量
├─────────────────────────────────┤
│         Text Segment            │  ← 代码段：可执行指令，只读
├─────────────────────────────────┤
低地址 0x0000000000000000 (64位)
```

---

### Q2: 各段详细说明？

| 段 | 内容 | 读写 | 生命周期 | 示例 |
|----|------|------|---------|------|
| **Text（代码段）** | 可执行机器指令 | 只读+执行 | 程序运行期间 | `int main() { ... }` |
| **Data（数据段）** | 已初始化的全局/静态变量 | 读写 | 程序运行期间 | `int g = 10;` |
| **BSS** | 未初始化的全局/静态变量 | 读写 | 程序运行期间 | `int g;` (自动置0) |
| **Heap（堆）** | 动态分配的内存 | 读写 | malloc到free之间 | `p = malloc(100);` |
| **Stack（栈）** | 局部变量、函数调用帧 | 读写 | 函数调用期间 | `int x = 5;` |
| **mmap** | 共享库、mmap映射区 | 读写 | 映射期间 | `dlopen("lib.so")` |
| **Kernel** | 内核代码和数据 | — | 系统运行期间 | 用户不可直接访问 |

```c
// 代码示例：各段变量的归属
int g_init = 42;        // Data 段（已初始化全局变量）
int g_uninit;            // BSS 段（未初始化全局变量，值为0）
static int s_init = 7;  // Data 段（已初始化静态变量）
static int s_uninit;     // BSS 段（未初始化静态变量，值为0）

void func() {
    int local = 3;       // Stack（局部变量）
    int *p = malloc(100); // Heap（动态分配，p本身在Stack，指向Heap）
}
```

---

### Q3: 32位 vs 64位地址空间差异？

| 维度 | 32位 | 64位 |
|------|------|------|
| 虚拟地址空间大小 | 4GB (2^32) | 16EB (2^64)，实际使用 48位 = 256TB |
| 用户空间 |?| 3GB | 128TB |
| 内核空间 | 1GB | 128TB |
| 用户/内核分界 | 0xC0000000 | 0x8000000000000000 |

---

### Q4: 栈（Stack）vs 堆（Heap）对比？

| 对比维度 | 栈（Stack） | 堆（Heap） |
|---------|------------|-----------|
| 分配方式 | 编译器自动分配/释放 | 程序员手动 malloc/free |
| 增长方向 | 向低地址增长（↓） | 向高地址增长（↑） |
| 大小限制 | 较小（通常 1~8MB，ulimit -s 查看） | 较大（受虚拟地址空间限制） |
| 分配速度 | 极快（移动栈指针） | 较慢（需查找空闲块） |
| 碎片问题 | 无碎片 | 易产生内部/外部碎片 |
| 生命周期 | 函数返回自动释放 | 需手动释放，否则内存泄漏 |
| 访问效率 | 高（缓存友好，连续内存） | 较低（可能不连续） |
| 典型溢出 | StackOverflow（递归太深/局部太大） | OOM（申请过多未释放） |

```bash
# 查看栈大小限制
ulimit -s        # 单位KB，默认8192 (8MB)
ulimit -s 10240  # 设置为10MB

# 触发栈溢出的典型场景
# 1. 无限递归
# 2. 函数内大数组: int arr[10000000]; // 在栈上分配约40MB
```

---

### Q5: BSS 段为什么不存在于可执行文件中？

BSS 段的变量未初始化，运行时统一置 0，因此：
- **可执行文件中只记录 BSS 段的长度**，不存储实际数据
- 运行时由加载器（loader）分配内存并清零
- 好处：**减小可执行文件体积**

```bash
# 示例：对比初始化 vs 未初始化对文件大小的影响
# file1.c
int arr[10000];          // BSS段 — 文件很小

# file2.c
int arr[10000] = {1};    // Data段 — 文件增大约40KB
```

---

### Q6: 如何查看进程的内存分布？

```bash
# 方法1: /proc/pid/maps — 查看虚拟内存区域映射
cat /proc/$(pidof nginx)/maps

# 输出示例:
# 55a1c0000-55a1c1000  r-xp  ...  /usr/sbin/nginx  (Text)
# 55a1c1000-55a1c2000  r--p  ...  /usr/sbin/nginx  (Data readonly)
# 55a1c2000-55a1c3000  rw-p  ...  /usr/sbin/nginx  (Data)
# 7f00000000-7f00100000 rw-p  ...                    (Heap/mmap)
# 7fff000000-7fff100000 rw-p  ...                    (Stack)

# 方法2: pmap — 查看进程内存映射（更友好）
pmap -x $(pidof nginx)

# 方法3: /proc/pid/smaps — 更详细的内存统计
cat /proc/$(pidof nginx)/smaps | grep -E "Size|RSS|Private"

# 方法4: size — 查看可执行文件各段大小
size /usr/sbin/nginx
#    text    data     bss     dec     hex filename
#  123456    8900    3200  135556   21164 /usr/sbin/nginx
```

---

### Q7: 堆内存分配器（Allocator）有哪些？

| 分配器 | 特点 | 适用场景 |
|--------|------|---------|
| **ptmalloc (glibc)** | 主流Linux默认，多线程支持，有arena | 通用 |
| **tcmalloc (Google)** | 线程缓存，减少锁竞争，小对象高效 | 高并发 |
| **jemalloc (FreeBSD)** | 碎片率低，多arena | 高并发、长运行服务 |
| **mimalloc (Microsoft)** | 安全性高，碎片率低 | 通用 |

```bash
# Linux下替换内存分配器（以tcmalloc为例）
LD_PRELOAD=/usr/lib/libtcmalloc.so ./your_program
```

---

## 一句话总结

**Linux进程内存从低到高依次为：代码段(Text) → 数据段(Data) → BSS段 → 堆(Heap,↑) → 共享库(mmap) → 栈(Stack,↓) → 内核空间，栈小而快由编译器管理，堆大而慢由程序员管理。**
