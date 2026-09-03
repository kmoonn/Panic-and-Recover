---
tags:
  - Java
  - 基础
category: Java/基础
---

# final、finally、finalize 的区别

## 一句话概览

`final` 是修饰符，`finally` 是异常处理块，`finalize` 是 Object 的方法——三者除了名字相似，没有任何关系。

## final

`final` 是 Java 中的修饰符，表示"不可变"。

| 修饰对象 | 作用 | 示例 |
|---|---|---|
| 类 | 不可被继承 | `final class String` |
| 方法 | 不可被重写 | `private` 方法隐式 `final` |
| 变量 | 不可重新赋值（常量） | `final int x = 10;` |

### final 修饰引用类型

`final` 修饰引用类型时，**引用不可变，但对象内容可变**：

```java
final List<String> list = new ArrayList<>();
list.add("hello");    // OK，修改的是对象内容
list = new ArrayList<>(); // 编译错误，不能改变引用指向
```

### final 与 JVM 优化

- `final` 修饰的变量在编译期可被优化（内联）
- `final` 修饰的方法不会被重写，JIT 可能将其内联
- `final` 修饰的类所有方法隐式 `final`

## finally

`finally` 是 `try-catch-finally` 语句中的代码块，**无论是否发生异常都会执行**，通常用于资源释放。

```java
try {
    // 可能抛出异常的代码
} catch (Exception e) {
    // 异常处理
} finally {
    // 总是执行，通常用于关闭资源
}
```

### finally 什么时候不执行

| 情况 | 说明 |
|---|---|
| `System.exit()` | 终止 JVM，finally 不执行 |
| 线程死亡 | 线程被 `kill` 或 `stop` |
| 死循环/死锁 | try 块中无限循环 |
| 虚拟机崩溃 | JVM 崩溃，所有代码都不执行 |

### finally 与 return 的坑

```java
public static int test() {
    int x = 1;
    try {
        return x;       // 返回值在 return 时已经"确定"为 1
    } finally {
        x = 2;          // 修改了 x，但返回值已经确定为 1
    }
}
// 返回 1，不是 2

public static int test2() {
    try {
        return 1;
    } finally {
        return 2;       // finally 中的 return 会覆盖 try 中的 return
    }
}
// 返回 2
```

> 面试要点：finally 中的修改不会影响 try 中已经确定的返回值（基本类型），但 finally 中的 `return` 会覆盖 try/catch 中的 `return`。实际开发中**禁止在 finally 中使用 return**。

## finalize

`finalize` 是 `Object` 类的方法，在 GC 回收对象之前由垃圾收集器调用。

```java
protected void finalize() throws Throwable {
    // 清理资源（如关闭文件、释放native内存）
    super.finalize();
}
```

### 为什么已废弃

| 问题 | 说明 |
|---|---|
| 不确定 | 无法保证 `finalize` 何时执行，甚至是否执行 |
| 性能差 | `finalize` 机制需要 GC 做额外工作，拖慢垃圾回收 |
| 可能复活 | 在 `finalize` 中将 `this` 赋给某个引用，对象会被"复活" |
| 安全风险 | `finalize` 可能被恶意利用（finalize 攻击） |
| JDK 9 标记 `@Deprecated` | 官方明确不推荐使用 |

### 替代方案

- **`Cleaner`/`PhantomReference`**（JDK 9+）：更安全的资源清理机制
- **`try-with-resources`**：实现 `AutoCloseable` 接口，显式释放资源
- **显式关闭方法**：如 `close()`、`shutdown()`

```java
// 推荐：try-with-resources
try (FileInputStream fis = new FileInputStream("test.txt")) {
    // 使用资源
} // 自动调用 close()
```

## 对比表格

| 维度 | final | finally | finalize |
|---|---|---|---|
| 是什么 | 修饰符 | 异常处理块 | Object 方法 |
| 作用 | 不可变 | 保证代码执行 | GC 回收前回调 |
| 使用位置 | 类、方法、变量 | try-catch 之后 | Object 子类重写 |
| 是否必须 | 按需使用 | 按需使用 | 不推荐使用 |
| 状态 | 正常使用 | 正常使用 | JDK 9 废弃 |
| 关联概念 | 常量、不可变 | 资源释放 | 垃圾回收 |

## 面试高频

**Q：final 修饰引用类型时能否修改内容？**

A：能。`final` 只保证引用不可变（不能指向另一个对象），但对象内部状态可以修改。如 `final List<>` 可以 add/remove，但不能重新赋值。

**Q：finally 什么时候不执行？**

A：`System.exit()`、线程死亡、JVM 崩溃、try 中死循环等情况 finally 不会执行。

**Q：finalize 还能用吗？**

A：JDK 9 已标记 `@Deprecated(forRemoval=true)`，虽然目前仍可使用但强烈不推荐。应使用 `try-with-resources` 或 `Cleaner` 替代。

## 一句话总结

final 是修饰符（不可变），finally 是异常处理的"必执行"块，finalize 是已废弃的 GC 回收回调方法——三者名字相似但功能无关。
