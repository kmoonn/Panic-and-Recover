---
tags:
  - Spring
  - Bean
category: Spring/核心
---

# Bean生命周期与作用域

Bean 生命周期从实例化到销毁经历完整流程，作用域决定 Bean 实例的共享范围；单例 Bean 默认线程不安全。

## 生命周期主线

实例化 → 属性填充（依赖注入）→ `Aware` 回调 → `BeanPostProcessor` 前置 → `@PostConstruct`/`init` → **使用** → `@PreDestroy`/`destroy`。

## 作用域

| 作用域 | 说明 |
|--------|------|
| `singleton` | 默认，容器内单例 |
| `prototype` | 每次取都新建 |
| `request` | Web 下，每次 HTTP 请求一个实例 |
| `session` | Web 下，每次 HTTP Session 一个实例 |

## ⭐ 加分/易错

单例 Bean **默认线程不安全**——若有可变成员变量会被多线程共享出问题；无状态才安全。这是并发 bug 常见根因。

## 一句话总结

> Bean 生命周期 = 实例化→属性注入→Aware回调→PostProcessor前后置→初始化→使用→销毁；singleton 默认线程不安全，无状态才安全。
