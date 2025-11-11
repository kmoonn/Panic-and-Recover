# synchronized 和 ReentrantLock 的区别

二者都是 Java 中的常用**并发同步**手段。

`synchronized`简单、安全、适合大多数情况；

`ReentrantLock`灵活、可控、适合复杂并发场景。

## 语义
- `synchronized`是 Java 语言级别的关键字，由 JVM 直接支持，隐式锁，JVM 管理。
- `ReentrantLock`是 Java 并发包（`java.util.concurrent.locks`）中的类，是 API 层面的实现，显式锁，手动控制。

## 可重入性
两者都支持重入，同一线程可以多多次进入。

## 锁的公平性
- `synchronized`不支持公平锁，线程获取锁顺序不可控。
- `ReentrantLock`支持公平/不公平锁，可以通过`new ReentrantLock(true)`创建公平锁。

## 可中断性
- `synchronized`不可中断，线程一旦阻塞，无法被 interrupt 打断。
- `ReentrantLock`可中断，可以使用`lockInterruptibly()`响应中断。

## 锁获取尝试（tryLock）
- `synchronized`无法尝试获取锁，只能阻塞。
- `ReentrantLock`可以尝试获取锁
  - `tryLock()`：立即返回成功或失败
  - `tryLock(timeout)`：超时仍未获取则返回失败

## 释放方式
- `synchronized`自动释放，方法或者代码块执行完毕或异常时自动释放。
- `ReentrantLock`需要手动释放，必须在`finally`里`unlock()`，否则可能死锁。

## 条件队列
- `synchronized`只有一个条件队列，不支持多个等待队列。
- `ReentrantLock`支持多个条件队列，可以实现更细粒度的等待/唤醒控制。

