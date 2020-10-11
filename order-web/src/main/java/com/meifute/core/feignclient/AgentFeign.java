package com.meifute.core.feignclient;

import com.meifute.core.AgentApiService;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * @Auther: wxb
 * @Date: 2018/10/22 20:11
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@FeignClient("mall-agent")
public interface AgentFeign extends AgentApiService {
}
