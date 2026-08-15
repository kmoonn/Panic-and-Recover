---
tags:
  - Spring
category: Spring/核心
---

# IoC（控制反转）

## Q: 什么是 IoC？

**IoC**（Inversion of Control，控制反转）：一种设计思想，将对象的创建、依赖绑定、生命周期管理等原本由程序员在代码中主动控制的职责，**交由 Spring 容器统一接管和自动化处理**。

```
传统开发：
class UserService {
    UserDao userDao = new UserDaoImpl();  // 主动 new，硬编码依赖具体实现
}

IoC 开发：
class UserService {
    @Autowired
    UserDao userDao;  // 不 new，容器自动注入
}
```

| 对比项 | 传统开发 | IoC 开发 |
|--------|---------|---------|
| 谁创建对象 | 程序员 `new` | 容器创建 |
| 谁管理依赖 | 程序员手动绑定 | 容器自动注入 |
| 控制权 | 应用代码主动控制 | **反转给容器** |
| 耦合度 | 高（依赖具体实现） | 低（依赖接口/抽象） |

> **核心**：IoC 是**思想**，DI（Dependency Injection）是实现 IoC 的**核心手段**。

---

## Q: 为什么需要 IoC？解决了什么问题？

### 1. 解耦

```java
// 没有 IoC：UserService 硬依赖 UserDaoImpl
public class UserService {
    private UserDao userDao = new UserDaoImpl();  // 换实现要改代码！
}

// 有 IoC：依赖接口，具体实现由容器决定
public class UserService {
    @Autowired
    private UserDao userDao;  // 注入什么实现由配置决定，改实现不改代码
}
```

### 2. 易于测试

```java
// 没有 IoC：无法 Mock 依赖
public class UserService {
    private UserDao userDao = new UserDaoImpl();  // 测试时必须连真实数据库
}

// 有 IoC：轻松注入 Mock
@Test
void testUserService() {
    UserService service = new UserService();
    // 反射或 setter 注入 Mock 对象
    service.setUserDao(mockUserDao);  // 不需要真实数据库
}
```

### 3. 集中生命周期管理

- 容器统一管理 Bean 的创建、初始化、销毁
- 避免各处散落的 `new` 和资源释放代码

### 4. AOP 基础

- Spring AOP 依赖 IoC 容器创建代理对象
- 没有 IoC 管理 Bean，`@Transactional` 等注解无法生效

---

## Q: 让 IoC 容器管理 Bean 生命周期有什么好处？

| 好处 | 说明 |
|------|------|
| **延迟初始化** | `@Lazy` 按需创建，减少启动时间 |
| **作用域管理** | singleton（单例）、prototype（每次新建）、request、session |
| **自动装配** | `@Autowired` 按类型自动注入，无需手动 |
| **销毁回调** | `@PreDestroy`、`DisposableBean`，容器关闭时自动释放资源 |
| **统一配置** | 集中在配置类/XML/YAML，一处修改全局生效 |
| **代理生成** | 容器创建 Bean 时自动织入 AOP 代理（事务、日志等） |
| **事件机制** | Bean 可监听/发布 ApplicationContext 事件 |

```java
@Component
@Scope("singleton")   // 默认单例
@Lazy                 // 延迟初始化
public class MyService {
    @PostConstruct
    public void init() {
        System.out.println("初始化");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("释放资源");
    }
}
```

---

## Q: DI 的三种注入方式？

| 注入方式 | 示例 | 优点 | 缺点 | 推荐 |
|---------|------|------|------|------|
| **构造器注入** | 构造函数参数 | 依赖不可变（final）、强制依赖、易测试 | 参数多时构造器冗长 | **Spring 推荐首选** |
| **Setter 注入** | setter 方法 | 可选依赖、灵活重配置 | 依赖可被中途修改 | 可选依赖用 |
| **字段注入** | `@Autowired` 放字段 | 简洁 | 无法设 final、难测试、隐藏依赖 | 不推荐 |

```java
@Service
public class UserService {

    // 1. 构造器注入（推荐！）
    private final UserDao userDao;
    private final EmailService emailService;

    public UserService(UserDao userDao, EmailService emailService) {
        this.userDao = userDao;
        this.emailService = emailService;
    }

    // 2. Setter 注入（可选依赖）
    private CacheService cacheService;

    @Autowired(required = false)
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    // 3. 字段注入（不推荐，但最常见）
    // @Autowired
    // private UserDao userDao;
}
```

> **Spring 官方推荐**：强制依赖用构造器注入，可选依赖用 setter 注入，避免字段注入。

---

## Q: BeanFactory 和 ApplicationContext 的区别？

| 对比项 | BeanFactory | ApplicationContext |
|--------|------------|-------------------|
| Bean 初始化时机 | **懒加载**（首次 `getBean` 时创建） | **预加载**（容器启动时创建所有 singleton） |
| 国际化 | 不支持 | 支持 MessageSource |
| 事件发布 | 不支持 | 支持 ApplicationEvent |
| AOP 集成 | 需手动配置 | 自动集成 |
| 资源访问 | 不支持 | 支持 ResourceLoader（classpath:/ file:/ 等） |
| BeanPostProcessor | 需手动注册 | 自动注册 |
| 关系 | **顶层接口** | BeanFactory 的子接口，功能超集 |

```java
// BeanFactory：懒加载
BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));
// 此时 Bean 还没创建
UserService service = factory.getBean(UserService.class);  // 这时才创建

// ApplicationContext：预加载
ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
// 启动时所有 singleton Bean 已创建完毕
```

> 实际开发中几乎都用 **ApplicationContext**（如 `AnnotationConfigApplicationContext`）。

---

## Q: Spring Bean 的完整生命周期？

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Bean 生命周期                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 实例化（Instantiation）                                      │
│     → 构造函数创建对象                                            │
│                                                                 │
│  2. 属性赋值（Populate Properties）                               │
│     → @Autowired / setter 注入依赖                               │
│                                                                 │
│  3. Aware 接口回调                                               │
│     → BeanNameAware.setBeanName()                               │
│     → BeanFactoryAware.setBeanFactory()                         │
│     → ApplicationContextAware.setApplicationContext()            │
│                                                                 │
│  4. BeanPostProcessor.postProcessBeforeInitialization()          │
│     → 前置处理（如解析 @PostConstruct 注解）                      │
│                                                                 │
│  5. 初始化方法                                                   │
│     → @PostConstruct                                            │
│     → InitializingBean.afterPropertiesSet()                     │
│     → init-method（XML 配置的自定义方法）                         │
│                                                                 │
│  6. BeanPostProcessor.postProcessAfterInitialization()           │
│     → 后置处理（如生成 AOP 代理对象）                             │
│                                                                 │
│  7. ★ Bean 就绪，可以使用 ★                                      │
│                                                                 │
│  8. 销毁方法（容器关闭时）                                        │
│     → @PreDestroy                                               │
│     → DisposableBean.destroy()                                  │
│     → destroy-method（XML 配置的自定义方法）                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```java
@Component
public class MyBean implements BeanNameAware, InitializingBean, DisposableBean {

    public MyBean() {
        System.out.println("1. 构造函数：实例化");
    }

    @Autowired
    public void setDependency(SomeDependency dep) {
        System.out.println("2. 属性赋值：注入依赖");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("3. BeanNameAware：获知Bean名称=" + name);
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("5. @PostConstruct：初始化");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("5. InitializingBean：afterPropertiesSet");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("8. @PreDestroy：销毁前");
    }

    @Override
    public void destroy() {
        System.out.println("8. DisposableBean：destroy");
    }
}
```

### 生命周期的核心要点

| 阶段 | 关键点 |
|------|--------|
| 实例化 | 构造函数调用，此时依赖**还未注入** |
| 属性赋值 | 依赖注入完成 |
| 初始化前 | BeanPostProcessor 前置处理 |
| 初始化 | `@PostConstruct` → `afterPropertiesSet` → `init-method` |
| 初始化后 | **AOP 代理在此生成**（BeanPostProcessor 后置处理） |
| 就绪 | 返回的可能是代理对象而非原始对象 |
| 销毁 | `@PreDestroy` → `destroy` → `destroy-method` |

> **注意**：构造函数中**不要**访问 `@Autowired` 注入的字段，因为此时依赖还未注入！初始化逻辑应放在 `@PostConstruct` 中。

---

## 一句话总结

IoC 是将对象创建和依赖管理的控制权从应用代码反转给 Spring 容器的设计思想，DI 是其核心实现手段（构造器注入首选）；容器管理 Bean 生命周期带来解耦、易测试、作用域管理、AOP 代理等好处，BeanFactory 是懒加载顶层接口，ApplicationContext 是预加载增强版，Bean 从实例化到销毁经历属性注入、Aware 回调、PostProcessor 前后置处理、初始化/销毁方法等完整流程。
