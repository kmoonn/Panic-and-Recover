# 计算机网络

## 分层模型

- OSI 七层
- TCP/IP 四层
- TCP/IP 五层
- [网络协议](0-basics/2-网络协议.md)

## 应用层

- [HTTP](1-application_layer/1-HTTP.md)
  - Header
  - 报文
  - 请求方法
    - [GET 请求与 POST 请求的区别]()
  - 响应码 / 状态码
  - HTTP/1.1
    - 长连接（Keep-Alive）
    - 虚拟主机（Host）
  - HTTP/2
    - 多路复用
    - 二进制分帧
    - 头部压缩（HPACK）
    - 服务器推送
  - HTTP/3
    - QUIC（UDP）
- [HTTPS](1-application_layer/2-HTTPS.md)
  - TLS
- [HTTP 与 HTTPS 的区别]()
- [WebSocket](1-application_layer/WebSocket.md)
- [SSE](1-application_layer/SSE.md)
- [DNS](1-application_layer/3-DNS.md)
- 递归查询、迭代查询

## 传输层

- [TCP](2-transport_layer/1-TCP.md)
  - 三次握手、四次挥手
  - 流量控制
  - 拥塞控制
  - 超时重传
- [UDP](2-transport_layer/2-UDP.md)
- [TCP 与 UDP 的区别](2-transport_layer/TCP、UDP.md)

## 网络层

- [IP](3-network_layer/1-IP.md)
  - IPv4、IPv6
- [NAT](3-network_layer/NAT.md)

## 网络接口层

- [MAC](4-network_interface_layer/MAC.md)

## 面试常考

- 浏览器输入 URL 到网页显示全过程
- Cookie、Session、Token 的区别
- JWT
- CDN