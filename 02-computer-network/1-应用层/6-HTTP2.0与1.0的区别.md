---
tags:
  - 网络
  - 应用层
category: 计算机网络/应用层
---

# HTTP/2.0 与 HTTP/1.0 的区别

## Q: HTTP/1.0 有什么特点？

| 特性 | HTTP/1.0 |
|------|----------|
| 连接方式 | **短连接**：每次请求/响应后关闭 TCP 连接 |
| 问题 | 每个 TCP 连接只传 1 个请求，大量请求时频繁建连（3次握手）开销大 |
| 优化的笨办法 | 浏览器并发多个 TCP 连接（通常 6~8 个），但仍有连接数上限 |

```
HTTP/1.0 请求流程：
请求1: 建TCP连接 → 发请求 → 收响应 → 断连接
请求2: 建TCP连接 → 发请求 → 收响应 → 断连接
请求3: 建TCP连接 → 发请求 → 收响应 → 断连接
（每次都重新三次握手，慢！）
```

---

## Q: HTTP/1.1 相比 1.0 有什么改进？

| 特性 | HTTP/1.1 |
|------|----------|
| 连接方式 | **Keep-Alive** 持久连接：一个 TCP 连接可发多个请求 |
| Pipelining | 支持管道化：不等第一个响应就发第二个请求 |
| Host 头 | 必须带 Host 头，支持虚拟主机 |
| 分块传输 | Transfer-Encoding: chunked |
| 缓存 | 更丰富的缓存控制（Cache-Control） |

### HTTP/1.1 的核心问题：队头阻塞（Head-of-Line Blocking）

```
Pipelining 请求流程：
请求1 ──────►
请求2 ──────►     （管道化：可连续发）
请求3 ──────►
◄────── 响应1     （但必须按序返回！）
◄────── 响应2     （响应1 慢则阻塞后面所有响应）
◄────── 响应3

问题：虽然请求可以并发发出，但 TCP 层必须按序返回，
      前面一个慢请求会阻塞后面所有响应（HTTP 层队头阻塞）
```

> 现实中 Pipelining 很少被启用（浏览器基本默认关闭），实际还是串行请求。

---

## Q: HTTP/2.0 有哪些核心改进？

| 特性 | HTTP/1.1 | HTTP/2 |
|------|----------|--------|
| 传输格式 | 文本（可读） | **二进制帧**（不可读，解析快） |
| 并发 | 串行 / 管道化（队头阻塞） | **多路复用**（无 HTTP 层队头阻塞） |
| 连接数 | 浏览器并发 6~8 个 TCP 连接 | **1 个 TCP 连接**承载所有请求 |
| 头部 | 每次完整发送（冗余） | **HPACK 头部压缩**（差量编码） |
| 服务端推送 | 无 | **Server Push**（主动推资源） |
| 优先级 | 无 | **流优先级**（重要请求优先） |

### 1. 二进制分帧（Binary Framing）

```
HTTP/1.1 文本格式：
GET /index.html HTTP/1.1\r\n
Host: example.com\r\n
\r\n

HTTP/2 二进制帧格式：
┌──────────┬──────────┬──────────────┬──────────────┐
│ Length   │ Type     │ Flags        │ Stream ID    │
│ (3字节)  │ (1字节)  │ (1字节)      │ (4字节)      │
├──────────┴──────────┴──────────────┴──────────────┤
│ Payload                                            │
└────────────────────────────────────────────────────┘

帧类型：HEADERS / DATA / SETTINGS / PUSH_PROMISE / ...
```

- 二进制解析效率高，不需要逐字符判断换行分隔
- 所有通信在**帧**上进行，帧关联到**流（Stream）**

### 2. 多路复用（Multiplexing）

```
HTTP/2 一个 TCP 连接上多个 Stream 并发：

Stream 1: HEADERS → DATA (请求/响应)
Stream 3: HEADERS → DATA (请求/响应)
Stream 5: HEADERS → DATA (请求/响应)

帧交错发送：
[Stream1 HEADERS][Stream3 HEADERS][Stream1 DATA][Stream5 HEADERS]...

每个 Stream 独立，互不阻塞！
```

- 一个 TCP 连接上可同时承载**无数个 Stream**
- 不同 Stream 的帧可以**交错发送**
- 消除了 **HTTP 层的队头阻塞**

### 3. HPACK 头部压缩

| 机制 | 说明 |
|------|------|
| 静态表 | 61 个常用头部字段预定义编码（如 `:method: GET` → 索引 2） |
| 动态表 | 连接中已发送的头部缓存在两端，后续只发索引号 |
| 哈夫曼编码 | 对字符串值做哈夫曼压缩 |

```
HTTP/1.1 每次请求重复发送完整头部（几百字节~KB级）：
GET /api/user  Host: example.com  Cookie: xxx  User-Agent: xxx  Accept: xxx ...

HTTP/2 首次发送完整头部，后续只发差量（几字节）：
首次: :method GET  :path /api/user  host example.com ...
后续: :path /api/order  （其他头部用动态表索引，仅发变化的）
```

### 4. 服务端推送（Server Push）

```
客户端请求 index.html
服务端不仅返回 index.html，还主动推送 style.css、app.js

Client ── GET /index.html ──► Server
Client ◄── index.html ──────── Server
Client ◄── PUSH style.css ──── Server
Client ◄── PUSH app.js ─────── Server
```

> 注意：HTTP/2 Server Push 在实践中争议较大，Chrome 已移除支持，HTTP/3 中可能弃用。替代方案是 `rel="preload"` 链接头。

---

## Q: HTTP/2 还有队头阻塞吗？

**有！在 TCP 层。**

```
HTTP/2 解决了 HTTP 层队头阻塞，但 TCP 层仍有：

Stream 1: ──────[DATA]──────[DATA]──────
Stream 2: ──────[DATA]──────[DATA]──────
Stream 3: ──────[DATA]──────[DATA]──────

        ↓ 所有 Stream 共享 1 个 TCP 连接 ↓

如果 TCP 层某个包丢失：
→ TCP 重传等待（拥塞控制可能降窗）
→ 所有 Stream 的数据都被阻塞（TCP 必须按序交付）
→ 这就是 TCP 层队头阻塞
```

| 层级 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|----------|--------|--------|
| HTTP 层队头阻塞 | 有 | 无（多路复用） | 无 |
| 传输层队头阻塞 | 有（TCP） | 有（TCP） | **无（QUIC/UDP）** |

### HTTP/3 = QUIC + HTTP/2 语义

| 特性 | HTTP/2 over TCP | HTTP/3 over QUIC |
|------|----------------|------------------|
| 传输层 | TCP | **UDP**（QUIC 封装） |
| 队头阻塞 | TCP 层阻塞所有流 | **无**（每个 Stream 独立，丢包只影响该流） |
| 连接建立 | TCP 握手(1RTT) + TLS 握手(1~2RTT) | **0-RTT / 1-RTT**（握手和加密合并） |
| 连接迁移 | 四元组（IP+Port）变则断连 | **Connection ID**（网络切换不断连） |

---

## Q: HTTP 状态码有哪些？常见的分别什么含义？

### 状态码分类

| 范围 | 类别 | 说明 |
|------|------|------|
| 1xx | 信息 | 请求已接收，继续处理 |
| 2xx | 成功 | 请求已成功处理 |
| 3xx | 重定向 | 需进一步操作完成请求 |
| 4xx | 客户端错误 | 请求有误（客户端问题） |
| 5xx | 服务端错误 | 服务器处理失败 |

### 常见状态码

| 状态码 | 含义 | 说明 |
|--------|------|------|
| **200** | OK | 请求成功 |
| **201** | Created | 资源创建成功（POST） |
| **204** | No Content | 成功但无返回体（DELETE） |
| **301** | Moved Permanently | 永久重定向，浏览器缓存新地址 |
| **302** | Found | 临时重定向，不缓存 |
| **304** | Not Modified | 缓存有效，返回缓存内容 |
| **400** | Bad Request | 请求格式错误 |
| **401** | Unauthorized | 未认证（没登录/Token 无效） |
| **403** | Forbidden | 已认证但无权限 |
| **404** | Not Found | 资源不存在 |
| **405** | Method Not Allowed | 请求方法不允许 |
| **429** | Too Many Requests | 限流 |
| **500** | Internal Server Error | 服务器内部错误 |
| **502** | Bad Gateway | 网关/代理收到上游无效响应 |
| **503** | Service Unavailable | 服务不可用（过载/维护） |
| **504** | Gateway Timeout | 网关等待上游超时 |

### 301 vs 302

| 对比项 | 301 | 302 |
|--------|-----|-----|
| 类型 | 永久重定向 | 临时重定向 |
| 浏览器缓存 | 缓存新地址，下次直接访问 | 不缓存，每次仍请求原地址 |
| SEO | 搜索引擎将权重转移到新地址 | 权重不转移 |
| POST 处理 | 变成 GET | 可能变成 GET（历史原因） |

---

## 一句话总结

HTTP/1.0 短连接每次建连开销大，1.1 用 Keep-Alive 持久连接但管道化有 HTTP 层队头阻塞，HTTP/2 通过二进制分帧+多路复用消除 HTTP 层队头阻塞且头部压缩大幅减少带宽，但仍存在 TCP 层队头阻塞，HTTP/3 用 QUIC(UDP) 彻底解决传输层队头阻塞并支持连接迁移和 0-RTT 握手。
