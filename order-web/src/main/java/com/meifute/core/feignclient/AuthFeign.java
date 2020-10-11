package com.meifute.core.feignclient;

import com.meifute.core.entity.MallUser;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Auther: wxb
 * @Date: 2018/9/24 15:28
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient(name = "mall-auth")
public interface AuthFeign {


    //获取当前用户对象
    @GetMapping("/current/user")
    Object getCurrentUsers();

}
