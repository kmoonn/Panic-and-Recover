---
tags:
  - Java
  - 基础
category: Java/基础
---

# String、StringBuilder 与 StringBuffer 的区别

## 核心对比

| 维度 | String | StringBuilder | StringBuffer |
|---|---|---|---|
| 可变性 | 不可变（`final`） | 可变 | 可变 |
| 线程安全 | 安全（不可变即线程安全） | 不安全 | 安全（`synchronized`） |
| 性能 | 拼接时最差 | 最好 | 略差（锁开销） |
| 出现版本 | JDK 1.0 | JDK 1.5 | JDK 1.0 |
| 底层存储 | JDK 8: `final char[]`；JDK 9+: `final byte[]` + coder | `byte[]`（JDK 9+） | `byte[]`（JDK 9+） |
| 使用场景 | 少量字符串操作 | 单线程字符串拼接 | 多线程字符串拼接 |

## String

`String` 是不可变类，一旦创建其内容不可修改。每次对 String 的修改操作都会产生新的 String 对象。

### 为什么 String 不可变

```java
// JDK 8
public final class String implements Serializable, Comparable<String> {
    private final char[] value;  // final + private，不可修改
    // ...
}

// JDK 9+（Compact Strings）
public final class String implements Serializable, Comparable<String> {
    private final byte[] value;  // final + private
    private final byte coder;    // 编码方式：LATIN1 或 UTF16
    // ...
}
```

不可变的原因：
1. **`final` 类**：不能被继承，防止子类破坏不可变性
2. **`final` 数组**：引用不可变
3. **`private` + 无 setter**：外部无法修改数组内容
4. **所有修改方法返回新对象**：如 `substring()`、`concat()` 等

### String 不可变的好处

| 好处 | 说明 |
|---|---|
| 字符串常量池 | 相同字面量指向同一对象，节省内存 |
| 线程安全 | 不可变对象天然线程安全 |
| HashMap key | 不可变保证 hashCode 不变，缓存 hashCode 提升性能 |
| 安全性 | 防止敏感数据（如数据库连接串）被意外修改 |

## StringBuilder

`StringBuilder` 是 JDK 1.5 引入的可变字符序列，**非线程安全**，性能最好。

```java
StringBuilder sb = new StringBuilder();
sb.append("hello");
sb.append(" ");
sb.append("world");
String result = sb.toString(); // "hello world"
```

## StringBuffer

`StringBuffer` 是 JDK 1.0 引入的可变字符序列，**线程安全**（方法用 `synchronized` 修饰），性能略差。

```java
// StringBuffer 的方法都加了 synchronized
@Override
public synchronized StringBuffer append(String str) {
    // ...
}
```

## 字符串拼接与 JVM 优化

### javac 的优化

Java 编译器（javac）会自动将 `+` 拼接优化为 `StringBuilder`：

```java
// 源码
String s = "a" + "b" + "c";

// 编译后等价于（非字面量拼接时）
String s = new StringBuilder().append("a").append("b").append("c").toString();
```

### 什么时候编译器优化不了

```java
// 循环中的拼接 —— 每次循环都会创建新的 StringBuilder
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i; // 等价于 result = new StringBuilder().append(result).append(i).toString();
}
// 应改为：
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

### JDK 9 的 InvokeDynamic 优化

JDK 9 开始，`javac` 不再简单地将 `+` 转换为 `StringBuilder`，而是使用 `invokedynamic` 指令调用 `StringConcatFactory`，由 JVM 在运行时决定最优拼接策略，可能比 `StringBuilder` 更高效。

## 面试高频

**Q：为什么 String 设计为不可变？**

A：四个原因——(1) 实现字符串常量池节省内存；(2) 天然线程安全；(3) hashCode 可缓存，适合做 HashMap 的 key；(4) 安全性，防止敏感数据被修改。底层通过 `final` 类 + `final` 数组 + `private` 访问控制实现。

**Q：StringBuilder 和 StringBuffer 怎么选？**

A：99% 的场景用 `StringBuilder`，因为字符串拼接通常在方法内部（局部变量，不存在线程安全问题）。只有在多线程共享同一个拼接对象时才用 `StringBuffer`，但这种情况极少。

**Q：`String s = new String("abc")` 创建了几个对象？**

A：最多 2 个——如果常量池中没有 `"abc"`，则先在常量池创建一个，再在堆上创建一个 String 对象；如果常量池中已有 `"abc"`，则只在堆上创建 1 个。

## 一句话总结

String 不可变（final 底层数组），线程安全；StringBuilder 可变、非线程安全、性能最好；StringBuffer 可变、线程安全（synchronized）；单线程拼接用 StringBuilder，循环拼接不要用 +。
