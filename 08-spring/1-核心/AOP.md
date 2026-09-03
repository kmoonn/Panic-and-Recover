---
tags:
  - Spring
category: Spring/核心
---

# AOP（面向切面编程）

## Q: 什么是 AOP？

**AOP**（Aspect-Oriented Programming，面向切面编程）：一种编程范式，将**横切关注点**（Cross-Cutting Concern）从业务逻辑中抽取出来，集中管理，实现"在不修改原有代码的前提下，对方法进行增强"。

```
OOP：纵向继承 + 横向组合（对象之间的协作）
AOP：横向切片（统一拦截增强，抽取公共逻辑）

没有 AOP：
class UserService {
    void save() {
        log.info("开始");        // 横切逻辑：日志
        checkAuth();             // 横切逻辑：权限
        beginTx();               // 横切逻辑：事务
        // ---- 核心业务 ----
        userDao.save(user);
        // ---- 核心业务 ----
        commitTx();              // 横切逻辑：事务
        log.info("结束");        // 横切逻辑：日志
    }
}
→ 日志、权限、事务和业务混在一起，每个方法都重复写！

有 AOP：
class UserService {
    void save() {
        userDao.save(user);  // 只写核心业务
    }
}
// 日志、权限、事务通过切面统一织入，业务代码零侵入
```

### 常见使用场景

| 场景 | 说明 |
|------|------|
| 统一日志 | 拦截 Controller/Service，自动记录请求参数、响应、耗时 |
| 统一权限校验 | 拦截需鉴权接口，校验 Token/角色 |
| 事务管理 | `@Transactional` 的底层实现 |
| 全局异常处理 | `@ControllerAdvice` |
| 接口限流/防重复提交 | 环绕通知 + Redis 计数 |
| 性能监控 | 记录慢接口耗时 |
| 数据脱敏 | 返回值字段加密/脱敏 |

---

## Q: AOP 核心术语有哪些？

| 术语 | 英文 | 说明 | 类比 |
|------|------|------|------|
| **切面** | Aspect | 横切逻辑的模块化单元（一个类） | 像"切蛋糕的刀" |
| **连接点** | Join Point | 程序执行中的某个点（方法调用、异常抛出） | 像"可以下刀的位置" |
| **切入点** | Pointcut | 匹配 Join Point 的表达式，决定切面在**哪里**生效 | 像"选定切哪些位置" |
| **通知/增强** | Advice | 在 Join Point 上执行的动作（**做什么**） | 像"切下去后做什么" |
| **目标对象** | Target | 被增强的原始对象 | 被切的"蛋糕" |
| **代理对象** | Proxy | AOP 织入后生成的增强对象 | 切好的"蛋糕" |
| **织入** | Weaving | 将 Aspect 应用到 Target 生成 Proxy 的过程 | "切蛋糕的过程" |

### 五种通知类型

| 通知类型 | 注解 | 执行时机 | 能否改变返回值 |
|---------|------|---------|--------------|
| **前置通知** | `@Before` | 目标方法执行**前** | 不能 |
| **后置通知** | `@After` | 目标方法执行**后**（无论成功/异常） | 不能 |
| **返回通知** | `@AfterReturning` | 目标方法**正常返回后** | 不能 |
| **异常通知** | `@AfterThrowing` | 目标方法**抛异常后** | 不能 |
| **环绕通知** | `@Around` | **包裹目标方法**，可控制是否执行、修改返回值 | **能**（最强大） |

```java
@Aspect
@Component
public class LogAspect {

    // 切入点：匹配 service 包下所有方法
    @Pointcut("execution(* com.example.service..*.*(..))")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    public void before(JoinPoint jp) {
        System.out.println("前置：调用 " + jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        System.out.println("返回后：结果=" + result);
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void afterThrowing(JoinPoint jp, Exception ex) {
        System.out.println("异常后：" + ex.getMessage());
    }

    @Around("serviceLayer()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();  // 执行目标方法
        long cost = System.currentTimeMillis() - start;
        System.out.println("耗时：" + cost + "ms");
        return result;  // 可修改返回值
    }
}
```

### 执行顺序

```
@Around 前半段
  → @Before
    → 目标方法
  → @AfterReturning / @AfterThrowing
  → @After
@Around 后半段
```

---

## Q: JDK 动态代理和 CGLIB 代理的区别？

| 对比项 | JDK 动态代理 | CGLIB 代理 |
|--------|------------|-----------|
| 原理 | 基于**接口**，`java.lang.reflect.Proxy` 生成接口实现类 | 基于**继承**，生成目标类子类（重写方法） |
| 要求 | 目标类必须实现接口 | 目标类不能是 final，方法不能是 final/private |
| 性能 | 生成代理快，调用略慢 | 生成代理慢，调用更快 |
| 依赖 | JDK 自带，无额外依赖 | 需 CGLIB 库（Spring 已内嵌） |
| 底层 | `InvocationHandler.invoke()` | `MethodInterceptor.intercept()` |

### JDK 动态代理示例

```java
// 目标类必须实现接口
public interface UserDao {
    void save();
}

public class UserDaoImpl implements UserDao {
    public void save() { System.out.println("保存用户"); }
}

// JDK 代理
UserDao target = new UserDaoImpl();
UserDao proxy = (UserDao) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    (obj, method, args) -> {
        System.out.println("增强前");
        Object result = method.invoke(target, args);
        System.out.println("增强后");
        return result;
    }
);
proxy.save();
// 输出：增强前 → 保存用户 → 增强后
```

### CGLIB 代理示例

```java
// 不需要接口
public class UserService {
    public void save() { System.out.println("保存用户"); }
}

// CGLIB 代理（生成 UserService 的子类）
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserService.class);
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("增强前");
    Object result = proxy.invokeSuper(obj, args);  // 调用父类方法
    System.out.println("增强后");
    return result;
});
UserService proxy = (UserService) enhancer.create();
proxy.save();
```

---

## Q: Spring 如何选择代理方式？

| 情况 | 代理方式 |
|------|---------|
| 目标类实现了接口 | 默认用 **JDK 动态代理** |
| 目标类未实现接口 | 用 **CGLIB** |
| 强制使用 CGLIB | `@EnableAspectJAutoProxy(proxyTargetClass = true)` |
| **Spring Boot 2.x** | 默认 **CGLIB**（`spring.aop.proxy-target-class=true`） |

```yaml
# Spring Boot 2.x 默认配置
spring:
  aop:
    proxy-target-class: true  # 默认 true，即 CGLIB
```

> **Spring Boot 2.x 默认 CGLIB 的原因**：避免 JDK 代理只能代理接口的问题，减少"接口+实现类"的强制约束。

---

## Q: @Transactional 是如何通过 AOP 实现的？

```java
@Service
public class UserService {
    @Transactional
    public void createUser(User user) {
        userDao.insert(user);        // 1. 代理在方法前：开启事务
        roleDao.insert(user.getRole()); // 2. 执行业务
        // 3. 代理在方法后：提交事务
        // 4. 如果抛异常：回滚事务
    }
}
```

### 底层流程

```
调用 userService.createUser(user)
       │
       ▼
  代理对象（Proxy）拦截
       │
       ▼
  1. TransactionInterceptor.invoke()
  2. 获取事务管理器 → 开启事务（connection.setAutoCommit(false)）
       │
       ▼
  3. 执行目标方法 createUser()
       │
       ├── 正常返回 → 提交事务（connection.commit()）
       │
       └── 抛出异常 → 判断是否回滚 → 回滚事务（connection.rollback()）
```

### @Transactional 的关键属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| propagation | REQUIRED | 事务传播行为 |
| isolation | DEFAULT | 隔离级别 |
| rollbackFor | {} | 指定回滚的异常类（**默认只回滚 RuntimeException 和 Error**） |
| noRollbackFor | {} | 指定不回滚的异常类 |
| readOnly | false | 只读事务（可优化） |
| timeout | -1 | 超时秒数 |

> **常见坑**：`@Transactional` 默认不回滚受检异常（checked exception），如 `IOException`。需要显式指定 `@Transactional(rollbackFor = Exception.class)`。

---

## Q: AOP 失效的常见场景？

### 1. 自调用（Self-Invocation）—— 最常见！

```java
@Service
public class UserService {
    public void methodA() {
        this.methodB();  // ← AOP 失效！methodB 的 @Transactional 不生效
    }

    @Transactional
    public void methodB() {
        userDao.save(user);
    }
}
```

**原因**：`this.methodB()` 调用的是**原始对象**而非代理对象，AOP 增强在代理上，绕过代理自然失效。

**解决方案**：

```java
// 方案1：注入自身（推荐）
@Service
public class UserService {
    @Autowired
    private UserService self;  // 注入代理对象

    public void methodA() {
        self.methodB();  // 走代理，AOP 生效
    }
}

// 方案2：从 ApplicationContext 获取代理对象
@Autowired
private ApplicationContext ctx;

public void methodA() {
    ctx.getBean(UserService.class).methodB();
}

// 方案3：拆到不同 Service（最推荐，职责更清晰）
```

### 2. private / final / static 方法

| 方法类型 | AOP 是否生效 | 原因 |
|---------|-------------|------|
| private | 不生效 | CGLIB 无法重写 private 方法，JDK 代理也看不到 |
| final | 不生效 | CGLIB 无法重写 final 方法 |
| static | 不生效 | AOP 基于实例方法代理，static 不参与 |

### 3. Bean 未被 Spring 容器管理

```java
// 没有 @Service/@Component，不是 Spring Bean
public class UserService {
    @Transactional  // 失效！容器不知道这个类的存在
    public void save() { ... }
}
```

### 4. 异常被 catch 吞掉

```java
@Service
public class UserService {
    @Transactional
    public void save() {
        try {
            userDao.insert(user);
            int i = 1 / 0;  // 异常
        } catch (Exception e) {
            log.error("出错了", e);  // catch 后异常没往外抛，事务不回滚！
        }
    }
}

// 修复：catch 后再抛出
catch (Exception e) {
    log.error("出错了", e);
    throw new RuntimeException(e);  // 重新抛出，触发回滚
}
```

---

## 八股速记

**Q: Spring AOP / 动态代理**

- **AOP（面向切面）**：把**横切关注点**（日志、事务、权限、监控）从业务代码里抽出来，统一织入。
- **核心概念**：切面（Aspect）、切点（Pointcut，切哪里）、通知（Advice，`@Before`/`@After`/`@Around`）。
- **实现**：底层是**动态代理**——接口用 **JDK 动态代理**，无接口用 **CGLIB**（生成子类）。

**⭐ 加分/易错**：
- 正因为 AOP 基于代理，**类内部方法自调用**（`this.method()`）不走代理 → 这也是 `@Transactional`/缓存注解"失效"的经典原因（见 @Transactional原理）。
- `@Around` 最强，能控制目标方法是否执行、改入参返回值。

## 一句话总结

AOP 通过切面抽取横切关注点（日志、事务、权限等），核心概念是切面=切入点+通知，Spring 用 JDK 动态代理（接口）或 CGLIB（子类继承）生成代理对象织入增强，Spring Boot 2.x 默认 CGLIB；@Transactional 底层就是 AOP 代理包裹方法实现事务管理，自调用（this.methodB()）绕过代理是 AOP 失效的最常见原因，private/final 方法和未被容器管理的 Bean 也会导致 AOP 失效。
