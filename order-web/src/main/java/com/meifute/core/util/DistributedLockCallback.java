package com.meifute.core.util;

/**
 * @Auther: wxb
 * @Date: 2018/10/12 10:08
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
public interface DistributedLockCallback<T> {

    /**
     * 调用者必须在此方法中实现需要加分布式锁的业务逻辑
     *
     * @return
     */
    public T process();

    /**
     * 得到分布式锁名称
     *
     * @return
     */
    public String getLockName();
}
