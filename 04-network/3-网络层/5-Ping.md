---
tags:
  - 网络
  - 网络层
category: 计算机网络/网络层
---

# Ping 命令

## 什么是 Ping？

Ping 是最常用的**网络诊断工具**，用于测试主机之间的**连通性**和**网络延迟**。它基于网络层的 **ICMP 协议**，不使用 TCP 也不使用 UDP。

名字来源于声纳定位系统（ping），意为"发送信号，等待回声"。

## Ping 的工作原理

```
发送端                                接收端
  │                                    │
  │ ── ICMP Echo Request (Type=8) ──→ │
  │                                    │
  │ ←── ICMP Echo Reply  (Type=0) ─── │
  │                                    │
  │  计算 RTT = 回复时间 - 发送时间     │
```

1. 发送端构造 **ICMP Echo Request** 报文（Type=8, Code=0），包含时间戳和序号
2. 目标主机收到后返回 **ICMP Echo Reply** 报文（Type=0, Code=0），原样携带时间戳
3. 发送端根据时间戳计算 **RTT（Round-Trip Time，往返时延）**
4. 默认发送多个请求（Linux 下持续发送，Windows 默认发 4 个）

## Ping 的输出解读

```bash
$ ping -c 4 www.baidu.com
PING www.a.shifen.com (14.215.177.39): 56 data bytes
64 bytes from 14.215.177.39: icmp_seq=0 ttl=54 time=5.321 ms
64 bytes from 14.215.177.39: icmp_seq=1 ttl=54 time=5.456 ms
64 bytes from 14.215.177.39: icmp_seq=2 ttl=54 time=5.289 ms
64 bytes from 14.215.177.39: icmp_seq=3 ttl=54 time=5.612 ms

--- www.a.shifen.com ping statistics ---
4 packets transmitted, 4 packets received, 0.0% packet loss
round-trip min/avg/max/stddev = 5.289/5.420/5.612/0.127 ms
```

| 字段 | 含义 |
|------|------|
| `64 bytes` | ICMP 报文数据部分长度（不含 IP 头和 ICMP 头） |
| `icmp_seq` | ICMP 序号，用于匹配请求和应答 |
| `ttl=54` | 生存时间，每经过一个路由器减 1，可用于估算跳数 |
| `time=5.321 ms` | RTT（往返时延），越小网络越快 |
| `packet loss` | 丢包率，0% 表示全部收到 |
| `min/avg/max` | RTT 的最小值、平均值、最大值 |

### TTL 的含义

- Linux 默认 TTL=64，Windows 默认 TTL=128，网络设备通常 TTL=255
- 输出中的 TTL 可推算跳数：`跳数 ≈ 初始TTL - 输出TTL`
- 上例中 TTL=54，若目标为 Linux（初始 64），则经过约 10 跳

## Ping 不通的可能原因

Ping 不通不代表网络完全不可用，需要具体分析：

| 原因 | 说明 | 排查方式 |
|------|------|---------|
| 网络不通 | 路由问题、链路断开 | traceroute 查看在哪一跳断开 |
| 防火墙禁 ICMP | 服务器出于安全考虑禁 Ping | 尝试 telnet/nc 测试端口 |
| 主机不在线 | 目标主机关机或未联网 | 检查目标主机状态 |
| ARP 解析失败 | 同链路内无法解析 MAC | `arp -a` 查看缓存 |
| 对端路由不可达 | 中间路由器返回 ICMP 不可达 | 查看 Ping 返回的错误信息 |

常见的 Ping 错误信息：

```
Request timeout        → 请求超时（对端未回应）
Destination Host Unreachable → 主机不可达（路由问题）
Destination Net Unreachable  → 网络不可达
TTL Expired           → TTL 超时（路由环路）
```

## Ping 与 Telnet 的区别

| 对比维度 | Ping | Telnet |
|---------|------|--------|
| 协议 | ICMP（网络层） | TCP（传输层） |
| 目的 | 测试网络连通性 | 测试端口是否可达 |
| 端口 | 无端口概念 | 指定端口号 |
| 能否检测服务 | 不能（只测网络层） | 能（测传输层连接） |
| 防火墙绕过 | 容易被禁 | 通常不会被禁 |
| 使用场景 | 网络是否通畅 | 服务是否可用 |

> **经典面试陷阱**：Ping 不通但能访问网站 → 很可能是防火墙禁了 ICMP，但 TCP 80/443 端口仍然开放。

## 常用 Ping 参数

```bash
# Linux
ping -c 4 host         # 发送 4 个包后停止
ping -i 0.5 host       # 每 0.5 秒发一次（默认 1 秒）
ping -s 1400 host      # 指定数据部分大小（默认 56 字节）
ping -W 2 host         # 设置超时时间为 2 秒

# Windows
ping -n 4 host         # 发送 4 个包
ping -l 1400 host      # 指定数据部分大小
ping -w 2000 host      # 设置超时为 2000 毫秒
```

## 面试高频

### Ping 用了什么协议？

Ping 使用 **ICMP 协议**（Internet Control Message Protocol），具体使用 ICMP Echo Request（Type=8）和 ICMP Echo Reply（Type=0），不使用 TCP 也不使用 UDP。

### Ping 不通但能访问的原因？

1. **防火墙禁 ICMP**：服务器出于安全考虑禁止 ICMP 回应，但 TCP 端口仍然开放
2. **ICMP 被过滤**：中间路由器/防火墙丢弃 ICMP 包但不影响 TCP 流量
3. **Ping 被系统禁用**：Linux 可通过 `sysctl net.ipv4.icmp_echo_ignore_all=1` 禁 Ping
4. **MTU 问题**：ICMP 包大小导致分片失败，但 TCP 有 PMTUD 机制

## 一句话总结

Ping 是基于 ICMP Echo Request/Reply 的网络连通性测试工具，通过计算 RTT 评估网络延迟；Ping 不通不等于网络不可用，防火墙禁 ICMP 是最常见的"能访问但 Ping 不通"的原因。
