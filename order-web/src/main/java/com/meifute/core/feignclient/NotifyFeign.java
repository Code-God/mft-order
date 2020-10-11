package com.meifute.core.feignclient;

import com.meifute.core.NotifyApiService;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 12:03
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient(name = "mall-notify")
public interface NotifyFeign extends NotifyApiService {
}
