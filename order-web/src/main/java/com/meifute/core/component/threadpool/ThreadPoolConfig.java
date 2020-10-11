package com.meifute.core.component.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: wuxiaobiao
 * @Description: 线程池配置类
 * @Date: Created in 2018/8/8
 * @Time: 10:23
 * I am a Code Man -_-!
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {

    /**
     * 1.当一个任务被提交到线程池时，首先查看线程池的核心线程是否都在执行任务，否就选择一条线程执行任务，是就执行第二步。（执行时需要获取全局锁）。
     * 2.查看核心线程池是否已满，不满就创建一条线程执行任务，否则执行第三步。
     * 3.查看任务队列是否已满，不满就将任务存储在任务队列中，否则执行第四步。
     * 4.查看线程池是否已满，不满就创建一条线程执行任务，否则就按照策略处理无法执行的任务。
     * @return
     */
    @Bean
    public Executor asyncServiceExecutor() {
        log.info("start asyncServiceExecutor");
        ThreadPoolTaskExecutor executor = new VisiableThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(5);
        //配置最大线程数
        executor.setMaxPoolSize(10);
        //配置线程最大空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        //配置缓存队列大小
        executor.setQueueCapacity(20);
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix("async-notify-");
        /**
         * rejectedExecutionHandler字段用于配置拒绝策略，常用的拒绝策略如下：
         * AbortPolicy，用于被拒绝任务的处理程序，它将抛出RejectedExecutionException。
         * CallerRunsPolicy，用于被拒绝任务的处理程序，它直接在execute方法的调用线程中运行被拒绝的任务。
         * DiscardOldestPolicy，用于被拒绝任务的处理程序，它放弃最旧的未处理请求，然后重试execute。
         * DiscardPolicy，用于被拒绝任务的处理程序，默认情况下它将丢弃被拒绝的任务。
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    /**
     * 对异常线程池的控制
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new MyAsyncExceptionHandler();
    }


    /**
     * 自定义异常处理类
     * @author hry
     */
    class MyAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
            log.info("Exception message - " + throwable.getMessage());
            log.info("Method name - " + method.getName());
            for (Object param : obj) {
                log.info("Parameter value - " + param);
            }
        }

    }
}
