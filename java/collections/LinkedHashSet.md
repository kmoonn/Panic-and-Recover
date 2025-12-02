# LinkedHashSet

## 数据结构

- 继承自`HashSet`，底层是哈希表 + 双向链表，本质是包装了一个`LinkedHashMap`
- 有序存储
- 不允许重复元素
- 只能存储一个`null`值
- 元素存储逻辑与`HashSet`一致，key 存储元素，value 是固定空对象

## 特点

- **保证元素插入顺序**

元素遍历属性与插入属性完全一致，底层**链表**负责维护顺序。

- **非线程安全**

多线程环境下需手动保证同步（如使用`Collections.synchronizedSet()`包装）。
