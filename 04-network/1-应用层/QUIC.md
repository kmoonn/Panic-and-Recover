---
tags:
  - 网络
  - 应用层
category: 计算机网络/应用层
---

# QUIC 协议

## 什么是 QUIC？

QUIC（Quick UDP Internet Connections）是 Google 设计的**传输层协议**，基于 UDP 实现，是 HTTP/3 的底层传输协议。它将 TLS 加密、流量控制、多路复用等功能集成在传输层，旨在替代 TCP+TLS 的组合。

## 为什么不用 TCP 而用 UDP？

TCP 存在三个核心问题，使得它难以满足现代 Web 的性能需求：

| TCP 问题 | 说明 |
|---------|------|
| **队头阻塞（HOL）** | TCP 是字节流协议，一个包丢失会阻塞所有后续数据的交付，即使其他流的数据已经到达 |
| **握手延迟高** | TCP 三次握手 + TLS 握手 = 首次连接需要 2~3 个 RTT 才能传输应用数据 |
| **升级困难** | TCP 协议栈在操作系统内核中实现，任何修改都需要升级操作系统，中间件（NAT、防火墙）也可能拦截未知 TCP 选项 |

而 UDP 是轻量级的，QUIC 在用户态实现了可靠传输，绕过了内核协议栈的限制。

## QUIC 的核心改进

### 1. 0-RTT / 1-RTT 建连

QUIC 将传输握手和 TLS 握手**合并**为一次交互：

```
首次连接（1-RTT）：
  Client ─── CH(QUIC) + ClientHello(TLS) ──→ Server
  Client ←── SH(QUIC) + ServerHello(TLS) ── Server
  Client ─── 应用数据 ──────────────────────→ Server  ← 1-RTT 后即可发数据

重连（0-RTT）：
  Client ─── CH + ClientHello + 0-RTT 数据 ─→ Server  ← 立即发送数据！
```

- **首次连接**：1-RTT 即可发送数据（TCP+TLS 1.3 需要 2-RTT）
- **重连**：0-RTT，利用之前缓存的密钥信息直接发送数据

### 2. 解决队头阻塞

这是 QUIC 最核心的改进：

- **TCP 的队头阻塞**：TCP 层只有一个字节流，一个包丢失会阻塞所有 Stream
- **QUIC 的解决**：多路复用在 QUIC 层实现，每个 Stream 独立排序、独立确认

```
TCP：  Stream1 █�█▁███  ← 包3丢失，Stream2 的包4、5也只能等待
       Stream2 ██████▁█

QUIC： Stream1 █�█▁███  ← 包3丢失，只影响 Stream1
       Stream2 ████████  ← Stream2 不受影响，继续交付
```

### 3. 连接迁移（Connection Migration）

TCP 用四元组（源IP、源端口、目的IP、目的端口）标识连接，网络切换（如 WiFi→4G）会导致连接断开。

QUIC 使用 **Connection ID（CID）** 标识连接：

| 特性 | TCP | QUIC |
|------|-----|------|
| 连接标识 | 四元组（IP+端口） | Connection ID |
| 切换网络 | 连接断开，需重新建连 | CID 不变，连接继续 |
| 典型场景 | 手机切 WiFi→5G 断线 | 无缝切换 |

### 4. 内置 TLS 1.3 加密

QUIC 强制使用 TLS 1.3，几乎所有头部信息都被加密（包括帧类型），比 TCP+TLS 更安全：

- 协议头部加密，中间设备无法窥探/篡改
- 防止协议僵化（ossification），中间设备无法"优化"不理解的字段

## QUIC vs TCP+TLS 对比

| 对比维度 | TCP + TLS 1.3 | QUIC |
|---------|---------------|------|
| 传输层 | 内核态 TCP | 用户态 UDP |
| 首次建连 | 2-RTT（TCP 1 + TLS 1） | 1-RTT |
| 重连 | 1-RTT（TLS 恢复） | 0-RTT |
| 队头阻塞 | 有（TCP 层） | 无（Stream 独立） |
| 连接迁移 | 不支持（四元组变化断连） | 支持（CID 标识） |
| 加密 | TLS 可选 | TLS 1.3 强制 |
| 拥塞控制 | 内核实现，升级困难 | 用户态实现，可快速迭代 |
| 流量控制 | 基于字节流 | 基于 Stream + Connection 双层 |
| 中间件兼容性 | 极好 | 部分网络可能拦截 UDP |

## 面试高频：QUIC 为什么能解决 TCP 队头阻塞？

**关键点**：TCP 的队头阻塞发生在**传输层**，因为 TCP 只维护一个有序字节流，一个序号的丢失会阻塞所有后续数据的交付。而 HTTP/2 的多路复用仍然跑在 TCP 之上，所以一个 TCP 包丢失会阻塞所有 HTTP Stream。

QUIC 将多路复用下放到**传输层自身**，每个 QUIC Stream 有独立的包序号空间和确认机制。一个 Stream 的包丢失只会影响该 Stream，其他 Stream 的数据可以正常交付，从而彻底解决了队头阻塞问题。

## 一句话总结

QUIC 基于 UDP 在用户态重新实现了可靠传输，通过合并握手（0/1-RTT）、Stream 级独立确认（消除队头阻塞）、CID 标识连接（支持迁移）和强制 TLS 1.3，系统性解决了 TCP 的固有缺陷，是 HTTP/3 的核心支撑。
