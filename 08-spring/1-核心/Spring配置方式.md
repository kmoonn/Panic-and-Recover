---
tags:
  - Spring
category: Spring/核心
---

# Spring 3 种配置方式

## XML

配置与代码完全分离

## 注解

需要配合 XML 或 Java 配置启用扫描

- `@Component`
- `@Service`
- `@Repository`
- `@Controller`
- `@Autowried`
- `@Qualifier`

## Java Config

基于注解的配置类，全注解方式，完全不使用 XML

- `@Configuration`

## 一句话总结

> Spring三种配置方式：XML分离、注解便捷、JavaConfig全注解，现代开发推荐JavaConfig。