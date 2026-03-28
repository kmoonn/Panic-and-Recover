# JSON Web Token

令牌技术

一种轻量级的、自包含的跨域认证协议，常用于无状态认证。

## 三部分组成

- header 头部
  - JSON 结构，包含签名算法、令牌类型
  - Base64Url 编码
- payload 负载
  - json 结构，存放用户信息、过期时间 exp、签发时间 iat 等
  - Base64Url 编码
- signature 签名
  - 对`编码后的header.编码后的payload`用密钥和指定算法进行签名
  - 防止内容被篡改

## 如何验证合法性

- 拆分
- 使用相同密钥 + 算法，对 header 和 payload 重新计算签名
- 比对签名是否一致
- 检查exp过期时间