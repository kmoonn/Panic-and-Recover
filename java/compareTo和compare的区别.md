# compareTo 和 compare 的区别

## compareTo 方法

- 属于 Comparable 接口
- 由被比较的对象自身调用，参数是另一个对象
- 定义对象的自然“自然排序”规则
- int compareTo(T o)

## compare 方法

- 属于 Comparator 接口
- 由**外部比较器**对象调用，参数是两个待比较对象
- 定义对象的“自定义排序”规则
- int compare(T o1, T o2)