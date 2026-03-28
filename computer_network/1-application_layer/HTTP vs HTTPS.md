# HTTP vs HTTPS

- 端口号
  - HTTP：默认端口 80
  - HTTPS：默认端口 443
- URL前缀
- 安全性
  - HTTP基于TCP、明文传输、无法校验双方身份、数据易被窃听、篡改、冒充
  - HTTPS = HTTP + TLS/SSL，在应用层和传输层之间加了加密层，加密传输、安全可靠
- 资源消耗
  - HTTP需要身份校验、加解密
- 连接过程
  - HTTP：TCP 三次握手后直接通信
  - HTTPS：TCP 三次握手 → TLS 握手 → 再开始 HTTP 通信
- 证书
  - HTTP：不需要证书
  - HTTPS：需要 CA 颁发的数字证书，用于身份认证
- SEO
  - HTTP：浏览器标记为不安全
  - HTTPS：显示小锁图标，有利于 SEO 排名
