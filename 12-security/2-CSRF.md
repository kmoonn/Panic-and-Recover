---
tags:
  - 中间件
  - 安全
category: 安全
---

# CSRF 跨站请求伪造

## Q: 什么是 CSRF 攻击？原理是什么？

**CSRF**（Cross-Site Request Forgery，跨站请求伪造）：攻击者诱导已登录用户访问恶意页面，该页面**利用用户浏览器自动携带的 Cookie**，向目标网站发送伪造请求，以用户身份执行操作。

### 攻击流程

```
1. 用户登录 bank.com，浏览器保存 Session Cookie
2. 用户访问恶意网站 evil.com
3. evil.com 页面中隐藏：
   <img src="https://bank.com/transfer?to=hacker&amount=10000">
4. 浏览器请求 bank.com 时自动带上 Cookie
5. bank.com 验证 Cookie 有效 → 转账成功！
```

```html
<!-- 常见 CSRF 攻击载体 -->
<!-- 1. img 标签（GET 请求） -->
<img src="https://bank.com/transfer?to=hacker&amount=10000" style="display:none">

<!-- 2. 自动提交的表单（POST 请求） -->
<form id="f" action="https://bank.com/transfer" method="POST">
    <input type="hidden" name="to" value="hacker">
    <input type="hidden" name="amount" value="10000">
</form>
<script>document.getElementById('f').submit();</script>
```

> **核心**：CSRF 利用的是浏览器自动携带 Cookie 的机制，攻击者**不需要知道 Cookie 内容**，只需让浏览器自动带上即可。

---

## Q: CSRF 攻击如何防御？

| 防御方式 | 原理 | 优点 | 缺点 |
|---------|------|------|------|
| **CSRF Token** | 服务器生成随机 Token 嵌入表单，提交时校验 | 最经典、安全 | 需后端配合存储/校验 |
| **SameSite Cookie** | Cookie 设 SameSite=Strict/Lax，跨站不携带 | 简单、浏览器原生 | 旧浏览器不支持、Lax 下 GET 仍携带 |
| **Referer/Origin 检查** | 请求头 Referer/Origin 必须是本站域名 | 无需 Token | Referer 可被用户隐私设置去掉 |
| **Double Submit Cookie** | Cookie 中放 Token，请求参数也放 Token，两者比对 | 无需服务端存储 | Cookie 泄露则失效 |

### CSRF Token 实现

```java
// Spring Security 默认启用 CSRF Token
// 表单中自动输出隐藏字段：
// <input type="hidden" name="_csrf" value="xxxx-xxxx-xxxx">

// 前后端分离：Token 放 Cookie + Header 双重提交
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        // Cookie 存 Token → 前端 JS 读 Cookie → 请求 Header 带 Token → 服务端比对
    }
}
```

### SameSite Cookie

```
Set-Cookie: sessionId=abc123; SameSite=Strict
```

| SameSite 值 | 行为 |
|-------------|------|
| Strict | 完全禁止跨站携带 Cookie（最安全，但从外链点进也不带，体验差） |
| Lax | GET 导航请求携带，POST/iframe/img 不携带（推荐默认值） |
| None | 允许跨站携带（必须同时设 Secure，仅 HTTPS） |

---

## Q: CSRF 和 XSS 的区别？

| 对比项 | CSRF | XSS |
|--------|------|-----|
| 攻击目标 | 伪造请求以**用户身份**执行操作 | 窃取用户 Cookie/Token 等信息 |
| 是否需要注入脚本 | 不需要（利用 `<img>`/`<form>` 即可） | 需要（注入 `<script>`） |
| 是否能看到响应 | 不能（盲攻击） | 能（脚本可读响应） |
| 前提 | 用户已登录目标站 | 目标站有 XSS 漏洞 |
| 防御 | CSRF Token、SameSite Cookie | 输入过滤/转义、CSP |

---

## 一句话总结

CSRF 利用浏览器自动带 Cookie 伪造跨站请求，防御靠 CSRF Token + SameSite Cookie。
