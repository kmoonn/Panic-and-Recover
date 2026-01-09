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

session 数据存储在服务器

