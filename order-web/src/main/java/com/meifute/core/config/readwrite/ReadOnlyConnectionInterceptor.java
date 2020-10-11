package com.meifute.core.config.readwrite;

import com.meifute.core.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @Auther: wuxb
 * @Date: 2019-03-19 19:02
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Aspect
@Component
public class ReadOnlyConnectionInterceptor implements Ordered {


    @Around("@annotation(readOnlyConnection)")
    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, ReadOnlyConnection readOnlyConnection) throws Throwable {
        try {
            log.info("===================切换只读从数据库中。。。。");
            DbContextHolder.setDbType(DbContextHolder.DbType.SLAVE);
            Object dataSource = SpringContextUtil.getBean("multipleDataSource");
            log.info("dataSource:{}",dataSource);

//            if(dataSource instanceof DynamicDataSource){
//                DynamicDataSource source = (DynamicDataSource) dataSource;
//
//            }
            log.info("===================已切换只读从数据库。。。。");
            Object result = proceedingJoinPoint.proceed();
            return result;
        } finally {
            DbContextHolder.clearDbType();
            log.info("restore database connection");
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
