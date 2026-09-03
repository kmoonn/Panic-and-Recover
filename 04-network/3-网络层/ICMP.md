---
tags:
  - 网络
  - 网络层
category: 计算机网络/网络层
---

# ICMP 互联网控制报文协议

## 什么是 ICMP？

ICMP（Internet Control Message Protocol，互联网控制报文协议）是**网络层**的控制协议，用于在 IP 网络中传递**控制消息**和**差错报告**。它不传输应用数据，而是帮助网络设备报告问题和诊断故障。

关键认知：
- ICMP 是 IP 协议的**辅助协议**，不是上层协议
- ICMP 报文封装在 **IP 数据报**中传输
- ICMP 不能纠正错误，只能**报告**错误

## ICMP 报文的两种类型

| 类型 | 说明 | 典型应用 |
|------|------|---------|
| **ICMP 查询报文** | 主动请求-应答模式，用于网络诊断 | Ping、时间戳查询、地址掩码查询 |
| **ICMP 差错报文** | 路由器或主机报告 IP 数据报传输中的问题 | 目标不可达、超时、重定向、参数问题 |

> 重要：差错报文本身出错时，不会再产生差错报文（防止无限循环）。同时，对于分片的后续片、组播地址、特殊地址，不产生差错报文。

## 常见 ICMP 类型表格

| Type | Code | 名称 | 类别 | 说明 |
|------|------|------|------|------|
| 0 | 0 | Echo Reply | 查询 | Echo 应答（Ping 回复） |
| 3 | 0 | Network Unreachable | 差错 | 网络不可达 |
| 3 | 1 | Host Unreachable | 差错 | 主机不可达 |
| 3 | 3 | Port Unreachable | 差错 | 端口不可达（常用） |
| 3 | 6 | Network Unknown | 差错 | 不知道目标网络 |
| 5 | — | Redirect | 差错 | 路由重定向 |
| 8 | 0 | Echo Request | 查询 | Echo 请求（Ping 发送） |
| 11 | 0 | TTL Exceeded | 差错 | TTL 超时（Traceroute 依赖） |
| 11 | 1 | Fragment Reassembly Timeout | 差错 | 分片重组超时 |

## ICMP 的应用

### Ping（查询报文的应用）

```
$ ping 8.8.8.8

发送：ICMP Echo Request（Type=8, Code=0）
接收：ICMP Echo Reply（Type=0, Code=0）
计算：RTT = 接收时间 - 发送时间
```

### Traceroute（差错报文的应用）

Traceroute 利用 **TTL 超时**差错报文逐跳探测路径：

```
Traceroute 原理：

1. 发送 TTL=1 的 IP 数据报 → 第1个路由器 TTL 减为 0，丢弃
   → 路由器返回 ICMP Time Exceeded（Type=11），记录第1跳地址

2. 发送 TTL=2 的 IP 数据报 → 通过第1跳，第2个路由器丢弃
   → 返回 ICMP Time Exceeded，记录第2跳地址

3. TTL 逐步递增...直到到达目标主机
   → 目标返回 ICMP Echo Reply 或 Port Unreachable，探测结束
```

```
$ traceroute 8.8.8.8
1  192.168.1.254   1.2ms   ← TTL=1 超时
2  10.0.0.1        5.3ms   ← TTL=2 超时
3  203.100.1.1     10.1ms  ← TTL=3 超时
...
10 8.8.8.8          25.6ms  ← 到达目标
```

## ICMP 与 IP 的关系

ICMP 报文封装在 IP 数据报中，但 ICMP **不是**传输层协议：

```
┌────────────────────────────────────┐
│           IP 头部                   │  ← 协议字段 = 1（表示 ICMP）
├────────────────────────────────────┤
│           ICMP 报文                 │  ← ICMP 是 IP 的辅助协议
│  ┌──────────┬──────────┬────────┐ │
│  │ Type(8b) │ Code(8b) │ 校验和 │ │
│  ├──────────┴──────────┴────────┤ │
│  │           数据部分             │ │
│  └───────────────────────────────┘ │
└────────────────────────────────────┘
```

- IP 头部中协议字段值为 **1**，表示数据部分是 ICMP
- ICMP 报文也会产生 IP 头部，但这不影响其作为网络层协议的定位
- ICMP 报文的目的地址取自**触发该 ICMP 报文的原始 IP 数据报的源地址**

## 面试高频

### Ping 用了什么协议？

Ping 使用 **ICMP 查询报文**：
- 发送 ICMP Echo Request（Type=8）
- 接收 ICMP Echo Reply（Type=0）
- 不使用 TCP 也不使用 UDP

### Traceroute 原理？

Traceroute 利用 **ICMP 差错报文**：
- 发送 TTL 递增的 IP 数据报
- 路由器 TTL 减为 0 时返回 **ICMP Time Exceeded**（Type=11）
- 到达目标时返回 ICMP Echo Reply 或 Port Unreachable
- 通过逐步增加 TTL，记录路径上每一跳的地址和延迟

| 对比 | Ping | Traceroute |
|------|------|------------|
| 使用的 ICMP 类型 | 查询报文（Type 8/0） | 差错报文（Type 11）+ 查询报文 |
| 目的 | 测试连通性和延迟 | 探测路径上每一跳 |
| 原理 | Echo Request/Reply | TTL 递增触发超时 |

## 一句话总结

ICMP 是网络层的控制协议，分为查询报文（Ping）和差错报文（目标不可达、超时等），封装在 IP 数据报中传输，是网络诊断和故障报告的核心机制。
