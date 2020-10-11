package com.meifute.core.feignclient;

import com.meifute.core.ItemApiService;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 20:17
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient(name = "mall-item")
public interface ItemFeign extends ItemApiService {
}
