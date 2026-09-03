---
tags:
  - Spring
  - SpringBoot
category: Spring/SpringBoot/注解
---

# @Autowired 和 @Resource 区别

| 维度 | @Autowired | @Resource |
|---|---|---|
| **来源** | Spring 注解 | JSR-250 标准注解（Java 原生） |
| **装配规则** | 默认按类型 byType，配合 @Qualifier 按名称 | 默认按名称 byName，找不到再按类型 |
| **必填性** | 默认必须注入，否则报错（可加 `required=false`） | 非必须 |
| **适用范围** | 项目中使用更多 | 兼容性更强，脱离 Spring 也能用 |

## 一句话总结

> @Autowired按类型装配（Spring），@Resource按名称装配（Java标准），前者默认必填，后者兼容性更强