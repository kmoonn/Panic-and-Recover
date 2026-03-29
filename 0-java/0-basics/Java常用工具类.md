# Java 常用工具类

Java 常用工具类分 JDK 原生和第三方。

原生工具类是基础，优先使用（无依赖），主要包括 Arrays、Objects、java.time 日期 API、Collections；

第三方工具类中，Apache Commons Lang3 解决字符串/对象通用痛点，Guava 集合/缓存增强，Hutool 一站式开发。

## 字符串操作

- java.lang.String
  - 核心方法
    - equals()
    - split()
    - substring()
    - trim()
      - strip() (JDK 11+ 推荐使用)
    - contains()
    - replace() / replaceAll()
- java.util.StringJoiner
  - JDK 8+
  - 优雅拼接字符串，替代手动拼接“+”或 StringBuffer
  - 支持分隔符、前缀、后缀
- java.text.MessageFormat
  - 格式化字符串
  - 适合动态参数拼接\
- java.lang.StringBuffer/StringBuilder
  - 字符串拼接
  - StringBuilder非线程安全，性能高，单线程推荐
  - StringBuffer线程安全，多线程使用

## 集合操作

java.util.Collections

专门操作集合（List、Set、Map）的工具类，全是静态方法。

- 核心方法
  - 排序
    - sort(list) 自然排序
    - sort(list, 比较器) 自定义排序
  - 判空
    - isEmpty(collection)
  - 空集合
    - emptyList() / emptyMap()
  - 不可变集合
    - unmodifiableList(list)
  - 同步集合
    - synchronizedList(list)
    - 将非线程安全集合转为线程安全

## 日期时间操作

java.time

- LocalDateTime/LocalDate/LocalTime
  - 日期时间、日期、时间，无时区
- DateTimeFormatter
- Duration/Period
  - 计算时间间隔（Duration算时分秒，Period算年月日）
- Instant
  - 时间戳，UTC 时区，适合跨时区场景

## 数字操作

- java.lang.Math
  - 数学计算
  - 核心方法
    - abs()
    - round()
    - ceil()
    - floor()
    - max()、min()
    - pow()
- java.util.Random
  - nextInt(int bound)
  - nextDouble()
  - ThreadLocalRandom 线程安全的随机数生成器
- NumberFormat
  - 数字格式化，线程安全
    - 保留小数
    - 百分比
    - 货币

## 对象操作

java.util.Arrays

- 核心方法
  - sort()
  - toString()
  - deepToString() 多维数组转字符串
  - binarySearch() 二分查找
  - asList() 转集合
  - copyof() 数组复制


## 数组操作

java.util.Objects

- 核心方法
  - isNull()
  - nonNull()
  - equals(a, b)
  - requireNonNull() 检验对象非空，适合方法入参校验
  - hash()

## 其他

- java.util.UUID：生成唯一标识（32 位字符串），核心方法UUID.randomUUID().toString()（常用作主键、唯一编号）。
- java.util.Optional：JDK8+，解决空指针问题（NPE），封装可能为 null 的对象，核心方法ofNullable()/orElse()/ifPresent()；


## 第三方工具类

- Apache Commons Lang3
  - **StringUtils** 字符串操作
  - ObjectUtils 对象操作
  - DateUtils/DateFormatUtils 日期操作
  - NumberUtils 数字操作
  - ArrayUtils 数组操作
- Apache Commons Collections
  - CollectionUtils 集合通用操作
  - MapUtils Map 操作
- Jackson/Gson/FastJSON
  - JSON 序列化、反序列化