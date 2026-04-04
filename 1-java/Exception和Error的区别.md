# Exception 和 Error 的区别

Java 中，`Exception` 和 `Error` 都是 `Throwable` 的子类，用于描述程序执行中发生的问题，但它们的性质、使用场景和处理方式有明显区别。

| 特性     | Exception                                   | Error                                |
|--------|---------------------------------------------|--------------------------------------|
| 层次     | Throwable → Exception                       | Throwable → Error                    |
| 可处理性   | 可通过 try-catch 捕获并恢复                         | 通常无法恢复，属于系统级错误                       |
| 是否强制捕获 | **受检异常**必须捕获或抛出                             | 不要求捕获                                |
| 常见子类   | IOException, SQLException, RuntimeException | OutOfMemoryError, StackOverflowError |
| 适用场景   | 程序逻辑异常或外部环境异常                               | JVM、硬件或系统资源异常                        |

## Exception
异常
- 程序中可处理的问题，可能捕获并恢复。
- 分为受检异常（编译器强制要求捕获或声明）和非受检异常（编译器不强制捕获）。
- 使用`try-catch`捕获或通过`throws`抛出。

## Error
错误
- 严重性高，表示 JVM 层面或系统层面的问题，通常无法恢复。
- 不要求捕获，编译器不强制处理。
- 常见类型有内存不足（OOM）、栈溢出、虚拟机错误。


