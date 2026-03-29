# HTTPS

HTTPS = HTTP + TLS/SSL

## HTTPS 涉及的三个角色

Client：浏览器 / 客户端
Server：网站服务器（需要配置证书）
CA（Certificate Authority）：证书颁发机构，负责签发、认证证书

CA 负责给 Server 发证书，用自己私钥签名
Server 持有证书和私钥，向 Client 出示证书
Client 用内置 CA 公钥验证证书，确认 Server 身份
验证通过后，双方协商对称会话密钥
后续通信使用对称加密

CA 做公证，Server 带身份证，Client 验身份证，验完才加密通信。

## 完整流程

阶段一：证书颁发阶段（Server ↔ CA，提前做）

网站上线前就完成的，不是用户访问时才做

Server 生成密钥对
生成非对称密钥对：公钥 Public Key + 私钥 Private Key
Server 向 CA 提交证书申请（CSR）
提交：公钥、域名、公司信息等
生成 CSR（Certificate Signing Request）
CA 验证 Server 身份
验证域名所有权、企业资质
CA 用自己的私钥对信息签名，生成数字证书证书包含：
服务器公钥
域名、有效期
签名算法
CA 签名（用 CA 私钥加密的摘要）
CA 将证书颁发给 Server
Server 部署证书 + 自己的私钥

阶段二：TLS 握手阶段（Client ↔ Server，访问时做）

