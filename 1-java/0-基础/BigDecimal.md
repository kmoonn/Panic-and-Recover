# BigDecimal 类

BigDecimal 是 Java java.math 包下用于高精度十进制运算的核心类，专为解决 float/double 二进制浮点运算的精度丢失问题而生，是金融、电商、计费等精度敏感场景的标准方案。

禁止用 double 构造：始终用 new BigDecimal(String) 或 BigDecimal.valueOf(double)

除法必须指定舍入：divide 三参数（除数、精度、舍入模式），必传

比较用 compareTo：equals 会校验标度（小数位数）

不可变性：运算不修改原对象，必须接收返回值