package com.meifute.core.config.readwrite;

/**
 * @Auther: wuxb
 * @Date: 2019-03-19 19:01
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
public class DbContextHolder {

    public enum DbType {
        MASTER,
        SLAVE
    }

    private static final ThreadLocal<DbType> contextHolder = new ThreadLocal<>();

    public static void setDbType(DbType dbType) {
        if(dbType == null){
            throw new NullPointerException();
        }
        contextHolder.set(dbType);
    }

    public static DbType getDbType() {
        return contextHolder.get() == null ? DbType.MASTER : contextHolder.get();
    }

    public static void clearDbType() {
        contextHolder.remove();
    }
}
