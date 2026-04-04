# TreeSet

## 数据结构

- 红黑树，添加、查询、删除的时间复杂度为 **O(log n)**
- 不允许`null`值，因为排序时无法比较`null`值
- 内部通过`TreeMap`实现，元素作为 key 存储，value 是固定空对象

## 特点

- **自定义排序规则**

1. 自然排序（默认）

    元素类需实现 `Comparable` 接口，并重写 `compareTo(T o)` 方法。

2. 自定义排序
    
    创建`TreeSet`时传入`Comparator`接口的实现类（或 lambda 表达式）指定排序规则。
    优先级高于自然排序。
- **通过排序规则去重**

若两个元素的比较结果为 0，则视为重复元素。

- **非线程安全**

多线程环境需手动同步（如 `Collections.synchronizedSortedSet()` 包装）。

