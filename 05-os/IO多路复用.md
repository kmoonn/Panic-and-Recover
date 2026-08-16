---
tags:
  - 操作系统
category: 操作系统
---

# IO 多路复用

## 面试Q&A

### Q1: 什么是 IO 多路复用？

**IO 多路复用**：一个线程同时监听多个 IO 事件（文件描述符 fd），哪个 fd 就绪就处理哪个，避免一个线程阻塞在单个 IO 上。

**解决的问题**：
- 传统 BIO：一个连接一个线程，连接数多时线程爆炸
- IO 多路复用：一个线程管理多个连接，大幅减少线程数

```
传统 BIO 模型：
  Thread1 → 阻塞 read(fd1)  ← 如果fd1无数据，线程一直等
  Thread2 → 阻塞 read(fd2)
  Thread3 → 阻塞 read(fd3)
  ...  (10000个连接 = 10000个线程)

IO 多路复用模型：
  单线程 → select/poll/epoll([fd1, fd2, fd3, ...])
            → 只有就绪的fd才返回，逐个处理
  ...  (10000个连接 = 1个线程)
```

---

### Q2: select 机制

```c
// select API
int select(int nfds, fd_set *readfds, fd_set *writefds,
           fd_set *exceptfds, struct timeval *timeout);
```

**工作原理**：
1. 用户构造 `fd_set`（bitmap，1024位），标记关心的 fd
2. 调用 `select()`，**整个 fd_set 从用户态拷贝到内核态**
3. 内核遍历所有 fd，检查是否有事件就绪
4. 将就绪结果写回 fd_set，**再从内核态拷贝回用户态**
5. 用户遍历 fd_set 找到就绪的 fd，逐个处理

**限制**：
- **fd 数量上限 1024**（`FD_SETSIZE` 宏，硬编码在内核）
- **O(n) 遍历**：每次调用都要遍历所有 fd（包括未就绪的）
- **每次调用都要拷贝 fd_set**（用户态 ↔ 内核态，2次拷贝）
- **fd_set 不可重用**：每次调用前要重新构造

```c
// select 使用示例
fd_set readfds;
FD_ZERO(&readfds);
FD_SET(fd1, &readfds);
FD_SET(fd2, &readfds);

while (1) {
    select(maxfd + 1, &readfds, NULL, NULL, NULL);
    for (int i = 0; i < maxfd + 1; i++) {  // O(n) 遍历
        if (FD_ISSET(i, &readfds)) {
            // 处理就绪的 fd
        }
    }
    // 注意：需要重新构造 readfds，因为 select 会修改它
}
```

---

### Q3: poll 机制

```c
// poll API
int poll(struct pollfd *fds, nfds_t nfds, int timeout);

struct pollfd {
    int   fd;        // 文件描述符
    short events;    // 关注的事件
    short revents;   // 返回的就绪事件
};
```

**改进点**：
- 用 `struct pollfd` 数组替代 bitmap，**突破 1024 限制**
- `events` 和 `revents` 分离，**不需要每次重构造**

**仍然存在的问题**：
- **O(n) 遍历**：仍然需要遍历所有 fd 找就绪的
- **每次调用仍需拷贝** struct pollfd 数组（用户态 ↔ 内核态）

```c
// poll 使用示例
struct pollfd fds[2];
fds[0].fd = fd1; fds[0].events = POLLIN;
fds[1].fd = fd2; fds[1].events = POLLIN;

while (1) {
    poll(fds, 2, -1);
    for (int i = 0; i < 2; i++) {  // O(n) 遍历
        if (fds[i].revents & POLLIN) {
            // 处理就绪的 fd
        }
    }
    // 不需要重新构造 fds，revents 每次会被重置
}
```

---

### Q4: epoll 机制

```c
// epoll API（三个系统调用）
int epoll_create(int size);           // 创建 epoll 实例
int epoll_ctl(int epfd, int op,       // 注册/修改/删除 fd
              int fd, struct epoll_event *event);
int epoll_wait(int epfd,              // 等待事件
               struct epoll_event *events, int maxevents, int timeout);
```

**核心改进**：
1. **事件驱动**：内核维护就绪事件队列，`epoll_wait` 只返回就绪的 fd
2. **O(1) 获取就绪事件**：不需要遍历所有 fd，只遍历就绪的（就绪数量通常远小于总数量）
3. **无拷贝开销**：用 mmap 让内核和用户态共享同一块内存，避免每次拷贝
4. **fd 数量无限制**：仅受系统限制（`/proc/sys/fs/file-max`，通常百万级）
5. **注册一次，复用**：`epoll_ctl` 注册后，内核持续跟踪，不需每次重新注册

```c
// epoll 使用示例
int epfd = epoll_create(1);

struct epoll_event ev;
ev.events = EPOLLIN;
ev.data.fd = listen_fd;
epoll_ctl(epfd, EPOLL_CTL_ADD, listen_fd, &ev);  // 注册一次

while (1) {
    struct epoll_event events[MAX_EVENTS];
    int n = epoll_wait(epfd, events, MAX_EVENTS, -1);  // 只返回就绪的
    for (int i = 0; i < n; i++) {  // 只遍历就绪的，O(就绪数)
        int fd = events[i].data.fd;
        // 处理就绪的 fd
    }
}
```

---

### Q5: select / poll / epoll 核心对比

| 对比维度 | select | poll | epoll |
|------------|-------|------|-------|
| **最大 fd 数** | 1024（FD_SETSIZE） | 无限制（受系统限制） | 无限制（受系统限制） |
| **数据结构** | bitmap（fd_set） | 数组（struct pollfd） | 红黑树（存所有fd）+ 就绪链表 |
| **时间复杂度** | O(n) 遍历所有 fd | O(n) 遍历所有 fd | O(1) 返回就绪fd，O(就绪数)遍历 |
| **内核↔用户拷贝** | 每次调用都拷贝 | 每次调用都拷贝 | mmap 共享内存，无拷贝 |
| **fd 注册** | 每次调用需重新构造 | 每次调用需重新构造 | epoll_ctl 注册一次，内核持续跟踪 |
| **触发模式** | 水平触发（LT） | 水平触发（LT） | 支持 LT + ET |
| **跨平台** | 是（POSIX 标准） | 是（POSIX 标准） | 仅 Linux |
| **适用场景** | 少量 fd，跨平台 | 少量 fd，跨平台 | 大量 fd，Linux |

---

### Q6: epoll 的 LT（水平触发）vs ET（边缘触发）

| 对比维度 | LT（Level Triggered） | ET（Edge Triggered） |
|---------|----------------------|---------------------|
| 触发条件 | fd 有数据可读时，**每次 epoll_wait 都通知** | fd 从无数据→有数据时，**只通知一次** |
| 使用方式 | 可部分读取，下次继续 | **必须一次性读完**所有数据（循环read直到EAGAIN） |
| 编程难度 | 简单（不会漏事件） | 较难（漏读会导致该 fd 不再通知） |
| 效率 | 可能多次通知 | 通知次数少，效率更高 |
| 适用场景 | 通用 | 需要极致性能（Nginx） |

```c
// LT 模式 — 简单，读完一部分也没关系
ev.events = EPOLLIN;  // 默认 LT
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &ev);
// epoll_wait 如果fd还有数据没读完，下次还会通知

// ET 模式 — 必须一次性读完
ev.events = EPOLLIN | EPOLLET;  // 边缘触发
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &ev);
// 必须循环读取，直到 EAGAIN
while (1) {
    int n = read(fd, buf, sizeof(buf));
    if (n == -1) {
        if (errno == EAGAIN) break;  // 数据读完
        // 错误处理
    }
    // 处理数据
}
// 注意：ET 模式下 fd 必须设为非阻塞！
```

---

### Q7: 实际应用中谁用了 epoll？

| 应用 | IO 模型 | 说明 |
|------|---------|------|
| **Redis** | 单线程 + epoll | 命令处理单线程，epoll 处理所有连接 |
| **Nginx** | 多进程 + epoll + ET | 每个 worker 一个 epoll，使用 ET 模式 |
| **Java NIO** | epoll（Linux） | 底层封装 epoll，Selector API |
| **Netty** | epoll / kqueue | Java NIO 框架，Linux 上用 epoll |
| **Go net** | epoll（Linux） | runtime 封装 epoll，goroutine per connection |

---

### Q8: 为什么 Redis 单线程还这么快？

1. **纯内存操作**：数据在内存中，读写极快（纳秒级）
2. **epoll IO 多路复用**：单线程处理万级连接，无线程切换开销
3. **高效数据结构**：SDS、ziplist、skiplist 等专为性能优化
4. **避免线程切换和锁**：无锁、无上下文切换、无缓存行伪共享
5. **单线程避免并发问题**：无需加锁，命令顺序执行

> Redis 6.0+ 引入多线程 IO（网络读写），但命令执行仍是单线程。

---

## 八股速记

**问：零拷贝 / IO 多路复用？**

**答（要点式）**：
- **传统 IO**：数据要在内核缓冲区 ↔ 用户缓冲区多次拷贝 + 多次用户/内核态切换。
- **零拷贝**：`sendfile`/`mmap`，减少拷贝和切换次数（如 Kafka、Nginx 高性能靠它）。
- **IO 多路复用**：一个线程管理多个连接（fd）：
  - **select**：fd 有上限（1024），每次遍历所有 fd，O(n)。
  - **poll**：无上限，仍遍历。
  - **epoll**：事件驱动（回调），只返回就绪 fd，O(1)，适合海量连接（Linux 高并发首选）。

**⭐ 加分/易错**：epoll 两种模式——LT（水平触发，默认）、ET（边缘触发，效率高但要一次读完）。Redis、Nginx 单线程能扛高并发就靠 epoll + IO 多路复用。

## 一句话总结

**IO多路复用让一个线程监控多个fd，select有1024限制且O(n)遍历，poll突破数量限制但仍O(n)拷贝，epoll事件驱动O(1)返回就绪事件且mmap共享内存零拷贝，是Linux高并发的基石。**
