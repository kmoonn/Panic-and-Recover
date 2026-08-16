---
category: 设计模式/面试
tags:
  - 设计模式
  - Spring
  - 面试
---

# Spring 中的设计模式

> 面试高频题："Spring 框架中用到了哪些设计模式？"——按功能模块分组答，比逐个列举更有逻辑。

---

## IoC 容器相关

| 设计模式 | Spring 应用 | 说明 |
|---------|------------|------|
| **工厂方法** | `BeanFactory` / `ApplicationContext` | IoC 容器本身就是工厂，创建和管理所有 Bean |
| **工厂方法** | `FactoryBean` | 定制单个 Bean 的创建逻辑（如 `SqlSessionFactoryBean`） |
| **单例** | Bean 默认 scope | 每个容器中一个实例，本质是单例注册表（`SingletonBeanRegistry`） |
| **建造者** | `BeanDefinitionBuilder` | 逐步构建复杂的 BeanDefinition |

## AOP 相关

| 设计模式 | Spring 应用 | 说明 |
|---------|------------|------|
| **代理** | `AOP` / `ProxyFactory` | JDK 动态代理或 CGLIB，实现事务、日志、权限等切面 |
| **代理** | `@Transactional` | 代理拦截方法调用，自动开启/提交/回滚事务 |
| **模板方法** | `JdbcTemplate` / `RestTemplate` | 执行流程固定（获取连接→执行→处理结果→释放），回调由用户提供 |

## 事件相关

| 设计模式 | Spring 应用 | 说明 |
|---------|------------|------|
| **观察者** | `ApplicationEvent` / `@EventListener` | 事件发布订阅，`ApplicationEventPublisher` 发布，`@EventListener` 监听 |

## MVC 相关

| 设计模式 | Spring 应用 | 说明 |
|---------|------------|------|
| **策略** | `Resource` 接口 | ClassPathResource / FileSystemResource / UrlResource 不同资源加载策略 |
| **策略** | `HandlerMapping` | 不同请求映射策略（RequestMapping、SimpleUrlMapping） |
| **适配器** | `HandlerAdapter` | 统一不同 Handler 的调用方式（Controller、HttpRequestHandler 等） |
| **责任链** | `HandlerInterceptor` | preHandle → Controller → postHandle → afterCompletion |
| **装饰器** | `HttpServletRequestWrapper` | Filter 中动态扩展请求对象 |

## 容器生命周期

| 设计模式 | Spring 应用 | 说明 |
|---------|------------|------|
| **模板方法** | `AbstractApplicationContext.refresh()` | 容器刷新流程固定，子类可重写钩子方法 |
| **装饰器** | `BeanWrapperImpl` | 对 Bean 属性访问能力的包装增强 |

---

## 面试答题模板

> "Spring 用了哪些设计模式？"

**按模块答，5/1 分钟版本**：

1. **IoC**：工厂（BeanFactory/FactoryBean）、单例（Bean 默认 scope）
2. **AOP**：代理（JDK/CGLIB 动态代理）、模板方法（JdbcTemplate）
3. **事件**：观察者（ApplicationEvent / @EventListener）
4. **MVC**：策略（HandlerMapping）、适配器（HandlerAdapter）、责任链（Interceptor）
5. **容器**：模板方法（refresh()）、装饰器（BeanWrapperImpl）

**追问准备**：
- "BeanFactory 和 FactoryBean 区别？" → 见[工厂模式](2-工厂模式.md)
- "Spring 的单例是严格单例吗？" → 见[单例模式](1-单例模式.md)
- "SpringBoot 2.x 默认用哪种代理？" → 见[代理模式](3-代理模式.md)

---

## 一句话总结

Spring 的设计模式贯穿 IoC（工厂+单例）、AOP（代理+模板）、MVC（策略+适配器+责任链）、事件（观察者）四大核心模块，面试按模块分组答。
