package com.meifute.core.config;

/**
 * Created by liuliang on 2019/3/19.
 */
public class CustomerContextHolder {
    public static final String DATA_SOURCE_A = "masterDataSource";
    public static final String DATA_SOURCE_B = "slaveDataSource";
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();
    public static void setCustomerType(String customerType) {
        contextHolder.set(customerType);
    }
    public static String getCustomerType() {
        return contextHolder.get();
    }
    public static void clearCustomerType() {
        contextHolder.remove();
    }
}
