package com.meifute.core.config.readwrite;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @Auther: wuxb
 * @Date: 2019-03-19 19:01
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
public class ReadWriteSplitRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.getDbType();
    }

}
