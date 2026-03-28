# HTTP 超文本传输协议

## 状态码

- 1XX
  - 信息
- 2XX
  - 成功
- 3XX
  - 重定向
- 4XX
  - 客户端错误
- 5XX
  - 服务端错误

## Header

- 通用头
  - cache-control 缓存控制
    - max-age
    - no-cache
    - no-store
  - connection 连接管理
    - close
    - keep-alive
  - date 报文时间
- 请求头
  - host 目标主机名和端口
    - http1.1 必带
  - user-agent 客户端浏览器/设备信息
  - accept 可接受的响应内容类型
  - accept-encoding 支持的压缩方式
  - accept-language 期望语言
  - refer 当前页面来源页面的url
  - cookie 携带服务器下发的cookie
- 响应头
  - server 服务器信息
  - set-cookie 服务器向客户端设置cookie
  - location 重定向地址
    - 3xx 状态码用
  - content-type 响应体内容类型
  - content-length 响应体长度
  - content-encoding 响应压缩编码
  - expires 缓存过期时间
  - last-modified 资源最后修改时间
  - etag 资源唯一标识，用于缓存校验

## HTTP/1.0 vs HTTP/1.1

- **连接方式**
  - 1.0默认短连接，一次请求-响应就关闭TCP
  - **1.1默认长连接（keep-alive）**，复用TCP连接
  - 本质都是TCP的长短连接
- 状态码
  - 1.1新增大量状态码
    - 100 continue 大资源前的预检
    - 206 范围请求、断点续传
    - 409 资源冲突
    - 410 资源永久消失
- 缓存机制
  - 1.0 expires、if-modified-since
  - 1.1 新增E-Tag、if-match、if-none-match等更精细策略
- 带宽优化
  - 1.0不支持部分请求，只能完整传输
  - 1.1支持range头，只请求部分内容，然后206
- Host 头（虚拟主机）
  - 1.0无host头，一个ip只能对应一个域名
  - 1.1新增host头，支持虚拟主机，一个ip可以对应多个域名

## HTTP/1.1 vs HTPP/2.0

- **二进制分帧**
  - 1.1报文基于文本格式报文，解析慢
  - 2.0报文基于二进制帧，更紧凑、高效、易解析
- **🌟 多路复用**
  - 多路复用，多个请求共用一条 TCP 连接，并行传输，无队头阻塞
  - 解决了1.1队头阻塞（应用层）（串行请求，一个请求完才能下一个）
- **头部压缩**
  - 使用HPACK算法
  - 对header压缩，大量减少开销
- **服务器推送**
  - 服务器可主动推送客户端所需资源（静态资源、一次性）
  - 减少请求次数、降低延迟
- 队头阻塞问题
  - 2.0解决了应用层队头阻塞
  - 但仍受tcp层队头阻塞影响

## HTTP/2.0 vs HTTP/3.0

- 传输协议
  - 2.0基于tcp
  - 3.0基于quic（udp之上）
    - quic是udp的可靠、加密升级版
- 连接建立
  - 2 tcp三次握手+tls，约3RTT
  - 3 QUIC+tls1.3 支持0-RTT/1-RTT，连接更快
- 头部压缩
  - 2 HPACK
  - 3 QPACK
- 队头阻塞
  - 2 解决应用层阻塞，但仍受tcp层对头阻塞
  - 3 QUIC数据流独立，彻底解决传输层队头阻塞
- 连接迁移
  - 2 TCP 依赖四元组（源IP、端口、目标IP、端口），切换网络断连
  - 3 QUIC 用64连接id，WiFi/移动流量切换不重连
- 错误恢复
  - 2 依赖tcp重传机制
  - 3 QUIC 内置更快的重传与错误恢复
- 安全性
  - 2 TLS加密应用数据，tcp头不加密
  - 3 QUIC 全报文加密（头+体），安全性更高
