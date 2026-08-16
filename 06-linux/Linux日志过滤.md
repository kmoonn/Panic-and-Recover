---
tags:
  - 操作系统
  - Linux
  - grep
category: 操作系统
---

# Linux日志过滤

## Q: Linux 面试里，如何用 `cat`、`grep` 做日志过滤？怎么表示并、或、非？

日志过滤的核心目标不是“背命令”，而是：**能不能快速从大文件里把关键信息筛出来**。

面试里最常见的就是：
- 查错误日志
- 同时满足多个条件（AND）
- 满足其中任意一个条件（OR）
- 排除某些噪音（NOT）

---

## 一、最常用的基础写法

### 1. 直接 grep 文件

```bash
grep "ERROR" app.log
```

含义：
- 在 `app.log` 中查找包含 `ERROR` 的行

这是最常见、最推荐的写法。

### 2. `cat file | grep`

```bash
cat app.log | grep "ERROR"
```

效果也能实现过滤，但通常不如直接 `grep app.log` 简洁。

### 两者区别

| 写法 | 是否可用 | 特点 |
|------|----------|------|
| `grep "ERROR" app.log` | ✅ | 更直接、更常用 |
| `cat app.log \| grep "ERROR"` | ✅ | 可读性差一点，多起一个进程 |

### 面试表达建议

可以这样说：

> 单文件检索时我会优先直接用 `grep 关键词 文件名`，如果前面还有别的命令输出需要继续过滤，再用管道接 `grep`。

例如：

```bash
tail -f app.log | grep "ERROR"
```

---

## 二、常用 grep 选项

| 选项 | 作用 | 示例 |
|------|------|------|
| `-i` | 忽略大小写 | `grep -i "error" app.log` |
| `-n` | 显示行号 | `grep -n "ERROR" app.log` |
| `-c` | 统计匹配行数 | `grep -c "ERROR" app.log` |
| `-v` | 取反，过滤不包含关键词的行 | `grep -v "DEBUG" app.log` |
| `-E` | 使用扩展正则 | `grep -E "ERROR|WARN" app.log` |
| `-A` | 显示匹配行后 N 行 | `grep -A 3 "ERROR" app.log` |
| `-B` | 显示匹配行前 N 行 | `grep -B 3 "ERROR" app.log` |
| `-C` | 显示匹配行前后各 N 行 | `grep -C 3 "ERROR" app.log` |

---

## 三、AND（并且）匹配

AND 的意思是：**一行日志同时满足多个条件**。

### 方法一：多次 grep 串联

```bash
grep "ERROR" app.log | grep "orderId=123"
```

含义：
- 先筛出包含 `ERROR` 的行
- 再从这些结果里筛出包含 `orderId=123` 的行

这就是最典型的 AND。

### 再看一个例子

```bash
grep "timeout" app.log | grep "payment-service"
```

含义：
- 查出同时包含 `timeout` 和 `payment-service` 的日志

### 面试表达

> Linux 里 AND 匹配最常见的做法是多个 `grep` 串联，因为前一个 `grep` 的结果会作为后一个 `grep` 的输入，相当于条件逐层收窄。

---

## 四、OR（或者）匹配

OR 的意思是：**满足多个条件中的任意一个即可**。

### 方法一：`grep -E` + 正则竖线 `|`

```bash
grep -E "ERROR|WARN" app.log
```

含义：
- 匹配包含 `ERROR` 或 `WARN` 的行

### 方法二：`egrep`（老写法）

```bash
egrep "ERROR|WARN" app.log
```

现在更推荐统一写成 `grep -E`。

### 多条件 OR

```bash
grep -E "ERROR|WARN|FATAL|Exception" app.log
```

适合查一组异常关键词。

---

## 五、NOT（非）匹配

NOT 的意思是：**排除包含某个关键词的行**。

### 基础写法

```bash
grep -v "DEBUG" app.log
```

含义：
- 过滤掉包含 `DEBUG` 的行

### 常见组合

#### 查 ERROR，但排除健康检查日志

```bash
grep "ERROR" app.log | grep -v "/health"
```

#### 查异常，但排除已知噪音

```bash
grep -E "ERROR|Exception" app.log | grep -v "KnownBusinessException"
```

---

## 六、AND + OR + NOT 组合写法

### 例 1：查支付服务中的 ERROR 或 WARN，但排除心跳日志

```bash
grep "payment-service" app.log | grep -E "ERROR|WARN" | grep -v "heartbeat"
```

含义：
1. 先定位服务
2. 再筛异常级别
3. 最后排除噪音

### 例 2：查订单相关超时，但排除测试流量

```bash
grep "order" app.log | grep "timeout" | grep -v "test-user"
```

### 例 3：查多个异常关键词，并显示上下文

```bash
grep -nC 2 -E "ERROR|Exception|timeout" app.log
```

适合快速定位上下文。

---

## 七、实时日志过滤

### 1. `tail -f` + `grep`

```bash
tail -f app.log | grep "ERROR"
```

含义：
- 实时追踪日志
- 只显示包含 `ERROR` 的新日志

### 2. 查实时超时异常

```bash
tail -f app.log | grep -E "timeout|TimeoutException"
```

### 3. 排除 debug 噪音

```bash
tail -f app.log | grep "payment" | grep -v "DEBUG"
```

---

## 八、面试高频场景题

### 1. 如何查某个订单的异常日志？

```bash
grep "orderId=123456" app.log | grep -E "ERROR|WARN|Exception"
```

### 2. 如何统计 ERROR 次数？

```bash
grep -c "ERROR" app.log
```

如果忽略大小写：

```bash
grep -ic "error" app.log
```

### 3. 如何看某个异常前后几行上下文？

```bash
grep -nC 3 "NullPointerException" app.log
```

### 4. 如何排除无关日志？

```bash
grep "ERROR" app.log | grep -v "health" | grep -v "metrics"
```

### 5. 如何在多文件中查找？

```bash
grep -rn "ERROR" /var/log/
```

---

## 九、常见误区

### 1. 滥用 `cat | grep`

`cat file | grep pattern` 虽然能用，但如果只是查一个文件，通常直接：

```bash
grep pattern file
```

更简洁。

### 2. 不会区分 AND 和 OR

- 多个 `grep` 串联：**AND**
- `grep -E "A|B"`：**OR**

### 3. 只会查关键词，不会排除噪音

真实排查里，`grep -v` 往往很重要，不然日志太多看不清。

### 4. 不会看上下文

只查到一行异常不一定够，很多时候要用：
- `-A`
- `-B`
- `-C`

才能看到前后文。

---

## 十、面试回答模板

> Linux 日志过滤我一般优先用 `grep`。如果只是查单文件，会直接写 `grep 关键词 文件名`；如果要对前一个命令的输出继续过滤，就用管道接 `grep`。并且匹配通常是多个 `grep` 串联，比如 `grep "ERROR" app.log | grep "orderId=123"`；或者匹配一般用 `grep -E "ERROR|WARN"`；非匹配用 `grep -v` 排除噪音日志。实时看日志我会用 `tail -f app.log | grep ...`，如果需要定位上下文，就加 `-nC` 显示前后几行。

## 一句话总结

> Linux 日志过滤核心是 `grep`：多个 `grep` 串联实现 AND，`grep -E "A|B"` 实现 OR，`grep -v` 实现 NOT，配合 `tail -f`、`-n`、`-C` 可以高效完成线上日志排查。
