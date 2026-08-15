---
tags:
  - 网络
  - 应用层
category: 计算机网络/应用层
---

# HTTP 与 RPC 的区别

## 面试Q&A

### Q1: HTTP 和 RPC 分别是什么？

**HTTP（HyperText Transfer Protocol）**：
- 应用层协议，基于请求-响应模型
- 文本协议，人可读（Human-Readable）
- RESTful 风格，资源导向（URI 表示资源，HTTP Method 表示操作）
- 走完整的网络协议栈：应用层 → 传输层(TCP) → 网络层 → 数据链路层
- 标准端口：HTTP 80 / HTTPS 443
- 通用性强，浏览器原生支持

**RPC（Remote Procedure Call）**：
- 远程过程调用，让远程函数调用像本地调用一样简单
- 二进制协议（protobuf / thrift / hessian），机器可读
- 面向服务导向（接口-方法调用模式）
- 可自定义协议栈，可跳过部分层（如基于 TCP 自定义帧协议）
- 性能更高：序列化/反序列化更快，报文体积更小
- 通常用于内部微服务间通信

---

### Q2: HTTP 和 RPC 的核心对比？

| 对比维度 | HTTP | RPC |
|---------|------|-----|
| 协议格式 | 文本协议（HTTP/1.x），可读 | 二进制协议（protobuf/thrift），紧凑 |
| 数据格式 | JSON / XML / Form | Protobuf / Thrift / Hessian |
| 性能 | 较低（文本解析开销大） | 较高（二进制序列化快，体积小） |
| 服务发现 | DNS / Nginx 反向代理 | 注册中心（Nacos / Consul / ZooKeeper） |
| 接口定义 | OpenAPI / Swagger（松散） | IDL（.proto / .thrift，强类型约束） |
| 代码生成 | 无（需手动/框架绑定） | 自动生成 Client/Server 桩代码 |
| 流式通信 | HTTP/1.1 不原生支持 | 双向流式（gRPC stream） |
| 使用场景 | 对外 API、浏览器、简单服务 | 内部微服务、高性能通信 |
| 调试难度 | 低（curl / 浏览器直接调） | 高（需专用工具/客户端） |
| 跨语言 | 天然跨语言 | 依赖 IDL 代码生成，跨语言支持 |

---

### Q3: gRPC 是什么？为什么说它桥接了 HTTP 和 RPC？

gRPC = **HTTP/2 + Protobuf + IDL**

```protobuf
// greeting.proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
  rpc SayHelloStream (HelloRequest) returns (stream HelloReply) {} // 服务端流
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

gRPC 的优势：
- **HTTP/2**：多路复用、头部压缩、双向流 — 解决了 HTTP/1.x 的性能瓶颈
- **Protobuf**：二进制序列化，比 JSON 快 3~10 倍，体积小 3~10 倍
- **IDL**：强类型接口定义，自动生成多语言客户端/服务端代码
- **流式 RPC**：Unary / Server Streaming / Client Streaming / Bidirectional Streaming

```go
// gRPC 客户端调用 — 像调本地函数
client := pb.NewGreeterClient(conn)
resp, err := client.SayHello(ctx, &pb.HelloRequest{Name: "world"})
```

---

### Q4: 什么时候用 HTTP？什么时候用 RPC？

**选 HTTP 的场景**：
- 对外提供 API（开放平台、第三方对接）
- 浏览器前端直接调用
- 服务数量少、架构简单
- 团队跨语言、跨组织，HTTP 通用性更强
- 需要方便调试（curl / Postman）
- CDN / 网关 / 负载均衡天然支持

**选 RPC 的场景**：
- 内部微服务间高频调用
- 对性能敏感（低延迟、高吞吐）
- 需要强类型约束（编译期检查接口变更）
- 需要流式通信（长连接、实时推送）
- 服务数量多，需要注册中心 + 服务治理

> 现实中常见：**对外 HTTP，对内 RPC**（BFF 层做协议转换）

---

### Q5: 常见 RPC 框架对比？

| 框架 | 序列化 | 传输协议 | 跨语言 | 流式 | 服务治理 |
|------|--------|---------|--------|------|---------|
| gRPC | Protobuf | HTTP/2 | 是 | 四种流 | 需配合 xDS |
| Dubbo | Hessian/Protobuf | 自定义 TCP | Java 为主 | 否 | 内置完善 |
| Thrift | Thrift IDL | 自定义 TCP | 是 | 否 | 较弱 |
| Brpc | Protobuf/JSON | 自定义 TCP | C++ 为主 | 是 | 内置 |

---

### Q6: HTTP/2 对比 HTTP/1.1 的改进？

| 特性 | HTTP/1.1 | HTTP/2 |
|------|----------|--------|
| 传输方式 | 文本 | 二进制帧 |
| 多路复用 | 否（队头阻塞） | 是（同一连接多个流） |
| 头部压缩 | 无 | HPACK 压缩 |
| 服务端推送 | 无 | 支持 |
| 流式传输 | 否 | 支持 |

> HTTP/2 使得 HTTP 的性能瓶颈大幅缩小，gRPC 正是利用了这一点。

---

## 一句话总结

**HTTP 通用易调试适合对外，RPC 高效强类型适合对内，gRPC（HTTP/2 + Protobuf）兼具两者优势，是微服务通信的现代首选。**
