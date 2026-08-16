---
tags:
  - 网络
  - 面试
category: 计算机网络/面试热点
---

# Cookie、Token、Session 的区别

HTTP 是**无状态协议**，客户端每次发送请求时，下一次请求无法得知上一次请求所包含的状态数据。

## Cookie

Cookie 机制是浏览器与服务器在 HTTP/HTTPS 通信中用于在无状态协议之上保存和传递状态信息的一种手段。核心作用是在多次请求之间“识别用户、保持会话和存储少量数据”。

- 工作原理
  1. 服务器在响应中设置 Cookie
     - 返回头中包含`set-Cookie`
  2. 浏览器保存 Cookie
     - 存入浏览器的 Cookie 存储中（按域名隔离）
  3. 浏览器后续请求自动携带 Cookie
     - 访问同一域时，在请求头中带上
     - Cookie 由浏览器自动发送
     - 服务器端不需要主动请求客户端返回
  4. 服务器读取 Cookie
     - 用来识别用户、恢复会话状态或个性化处理
- 组成要素
  - name
  - value
  - 作用域
    - Domain
    - Path
  - 过期时间 / 生命周期
    - Expires
      - 绝对时间
    - Max-Age
      - 相对秒数
  - 安全属性
    - Secure
      - 仅 HTTPS 发送
      - 防止明文泄露
    - HttpOnly
      - 浏览器脚本无法访问
    - SameSite

- 分类
  - 持久 Cookie
    - 含 Expires / Max-Age，按时间过期
  - 会话 Cookie（Session Cookie）
    - 关闭浏览器即删除


## Session 机制

会话

服务器存储用户状态

数据存储在服务端，客户端只存一个sessionID（放在cookie里）
每次请求带上sessionID，服务器根据ID找数据
有状态

## Token

令牌

客户端自己携带用户状态

数据全部存在token里面，服务器不存任何用户状态
每次请求带上token，服务器直接验证签名即可
无状态服务

Cookie 存在客户端，不安全、容量小、自动携带；

Session 存在服务端，安全、容量大、依赖 Cookie 传递 SessionID。


## 八股速记

**Q：Cookie / Session / Token（JWT）**

- **Cookie**：存在浏览器、每次请求自动带上，服务端用来识别客户端。
- **Session**：状态存**服务端**，浏览器只存 sessionId（通常放 Cookie）。有状态 → 多机需共享（如存 Redis）。
- **Token / JWT**：状态存**客户端**（token 自带信息+签名），服务端**无状态**，天然适合分布式/多端。JWT = Header.Payload.Signature，签名防篡改但**payload 是 base64 非加密**，别放敏感信息。

⭐ **加分/易错**：JWT 的痛点是**难主动失效**（服务端不存状态），常配短过期 + refresh token，或用黑名单（又退回有状态）。这点和项目里 `01-contest-sso.md` 的 Sa-Token 会话存 Redis 可互相印证。

## 一句话总结

> Cookie存客户端自动携带但不安全，Session存服务端安全但依赖Cookie传ID，Token自包含无状态适合分布式。
