public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    /**
     * 默认初始容量
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * 用户指定容量为 0 的空列表
     * new ArrayList(0) 创建的空列表
     * 第一次 add 时会直接扩容到用户指定的容量
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};

    /**
     * 默认无参构造的空列表，第一次 add 时扩容到默认初始容量
     * new ArrayList() 创建的空列表
     * 在小于默认容量的情况下，不必立即分配数组，支持懒加载
     */
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * 实际存放元素的数组
     */
    transient Object[] elementData; // non-private to simplify nested class access

    /**
     * 元素个数
     */
    private int size;

    // 有参构造
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
    }

    // 无参构造
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
}