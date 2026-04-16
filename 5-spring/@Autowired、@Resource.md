# @Autowired 和 @Resource 区别

来源不同
@Autowired：Spring 注解
@Resource：JSR-250 标准注解，Java 原生
装配规则
@Autowired：默认按类型 byType，配合 @Qualifier 按名称
@Resource：默认按名称 byName，找不到再按类型
必填性
@Autowired：默认必须注入，否则报错；可加required=false
@Resource：非必须
适用范围
Resource 兼容性更强，脱离 Spring 也能用；
项目中Autowired 使用更多。