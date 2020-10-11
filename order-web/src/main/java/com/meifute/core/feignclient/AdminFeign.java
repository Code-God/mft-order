package com.meifute.core.feignclient;

import com.meifute.core.AdminApiService;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 20:11
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient("mall-admin")
public interface AdminFeign extends AdminApiService {
}
