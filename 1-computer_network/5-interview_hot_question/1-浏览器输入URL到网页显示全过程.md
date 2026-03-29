

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
