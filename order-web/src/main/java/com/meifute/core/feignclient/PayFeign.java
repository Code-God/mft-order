package com.meifute.core.feignclient;

import com.meifute.core.PayApiService;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 12:03
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient(name = "mall-pay")
public interface PayFeign extends PayApiService {
}
