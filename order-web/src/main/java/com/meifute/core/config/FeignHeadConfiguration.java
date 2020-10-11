package com.meifute.core.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @Auther: wxb
 * @Date: 2018/9/24 16:08
 * @Auto: I AM A CODE MAN -_-!
 * @Description: 自定义的请求头处理类，处理服务发送时的请求头
 */
@Slf4j
@Configuration
public class FeignHeadConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        String value = request.getHeader(name);
                        /**
                         * 遍历请求头里面的属性字段，将token添加到新的请求头中转发到下游服务
                         * */
                        if ("Authorization".equalsIgnoreCase(name)) {
                            log.debug("添加自定义请求头key:" + name + ",value:" + value);
                            requestTemplate.header(name, value);
                        } else {
                            log.debug("FeignHeadConfiguration", "非自定义请求头key:" + name + ",value:" + value + "不需要添加!");
                        }
                    }
                } else {
                    log.warn("FeignHeadConfiguration", "获取请求头失败！");
                }
            }
        };
    }

}
