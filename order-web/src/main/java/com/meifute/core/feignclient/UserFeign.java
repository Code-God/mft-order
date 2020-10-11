package com.meifute.core.feignclient;

import com.meifute.core.UserApiService;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 20:04
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient("mall-user")
public interface UserFeign extends UserApiService {
}
