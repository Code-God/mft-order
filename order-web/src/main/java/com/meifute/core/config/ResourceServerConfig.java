package com.meifute.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

/**
 * @Auther: wxb
 * @Date: 2018/9/22 15:21
 * @Auto: I AM A CODE MAN -_-!
 * @Description: 资源服务配置
 */
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    private static final String[] AUTH_WHITELIST = {
            "/**/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "swagger-resources/configuration/ui",
            "/doc.html",
            "/webjars/**",
            "/v1/app/ordercenter/jd/notify",
            "/api/implement/order/query/order/list/by/param",
            "/api/implement/order/query/regulation/info/list/by/param",
            "/api/implement/order/get/regulate/item/by/regulate/id",
            "/api/implement/order/regulate/id",
            "/query/order/list/by/param",
            "/query/regulation/info/list/by/param",
            "/get/regulate/item/by/regulate/id",
            "/api/implement/order/save/order/price/detail",
            "/v2/logistics/receive/waybillno/jiayi",
            "/v2/sms/market/record/hits",
            "/v2/sms/market/init/address",
            "/v2/sms/market/submit/verify/order",
            "/v2/sms/market/verify/list/info",
            "/v2/sms/market/verify/list/info/h5",
            "/v2/sms/market/cancel/verify/order",
            "/v1/app/ordercenter/get/express/by/code",
            "/v2/sms/market/add/sms/evaluate",
            "/v2/sms/market/is/evaluated",
            "/v2/sms/market/resubmit/verify/order",
            "/api/implement/order/temporary/not/push/jd/phones",
            "/api/implement/order/qimen",
            "/api/implement/order/qimen/**",
            "/api/implement/order/wuyou/thirdParty"
    };

    @Override
    public void configure(HttpSecurity http) throws Exception {
        for (String au : AUTH_WHITELIST) {
            http.authorizeRequests().antMatchers(au).permitAll();
        }
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

}
