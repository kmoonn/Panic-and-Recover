---
tags:
  - 网络
  - 面试
category: 计算机网络/面试热点
---



# 输入 URL 到网页显示全过程

- DNS域名解析
- 建立TCP连接
  - 浏览器根据IP地址和端口，与服务器进行TCP三次握手
- 发送HTPP请求
  - 在TCP连接上，浏览器构造HTTP请求报文，请求页面资源
- 服务器处理请求并返回响应
  - 服务器处理请求，返回HTTP响应报文
- 浏览器解析渲染页面
  - 解析HTML生成DOM树
  - 解析CSS生成DOM树
  - 合成渲染树，布局绘制页面
  - 遇到图片、JS、CSS等资源时，再次发起HTTP请求加载
- 关闭TCP连接
  - 数据传输完毕，通过TCP四次挥手断开连接
 

DNS 域名解析浏览器缓存 → 系统缓存 → 路由器缓存 → LDNS → 根 → 顶级域 → 权威 → 返回 IP
封装 HTTP/HTTPS 请求
HTTPS 先进行 TLS 握手
构建请求行、请求头、请求体
建立 TCP 连接（三次握手）
发送 HTTP 请求报文
经过网络传输：交换机 → 路由器 → 运营商网络 → 目标服务器
到达服务器 Nginx负载均衡 → 转发到后端应用（Tomcat）
Tomcat 处理：Acceptor → Poller → Worker 线程 → Filter → Servlet
进入 Spring MVC：DispatcherServlet → HandlerMapping → Interceptor → Controller → Service → Dao → DB
响应返回：封装 HTTP 响应 → Nginx → 浏览器
浏览器渲染：DOM → CSSOM → 渲染树 → 布局 → 绘制
TCP 断开（四次挥手）或 Keep-Alive 复用


## 八股速记

**Q：输入 URL 到页面展示**

1. **DNS 解析**：域名 → IP（浏览器缓存→系统 hosts→本地 DNS→递归查询）。
2. **建立 TCP 连接**：三次握手（HTTPS 再加 TLS 握手）。
3. **发送 HTTP 请求**：方法、路径、Header、body。
4. **服务端处理并响应**：可能经过 CDN、负载均衡、反向代理（Nginx）、后端服务。
5. **浏览器渲染**：解析 HTML 构建 DOM、CSS 构建 CSSOM → 渲染树 → 布局 → 绘制；遇到 JS/CSS/图片再发请求。
6. **关闭或复用连接**：HTTP/1.1 长连接复用，最终四次挥手。

⭐ **加分**：这题是"钩子题"，面试官会挑任意一步深挖（DNS 怎么查、TLS 怎么握手、缓存怎么命中）——每步都能接到上面的卡片。

## 一句话总结

> DNS解析→TCP三次握手→TLS握手→HTTP请求→服务端处理→响应返回→浏览器渲染→TCP断连或Keep-Alive复用。
