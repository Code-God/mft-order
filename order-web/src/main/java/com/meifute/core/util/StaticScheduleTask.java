package com.meifute.core.util;


import com.meifute.core.entity.MallUser;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.service.MallPushItemRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Configuration
@EnableScheduling
@Slf4j
public class StaticScheduleTask {


    @Autowired
    private MallPushItemRuleService pushItemRuleService;

    @Scheduled(fixedRate = 5*1000)
    public void schedule() {
        MallUser mallUser = new MallUser();
        mallUser.setId("updateSecurityNumber");
        this.schedule(mallUser);
    }

    @RedisLock(key = "id", sync = true)
    public void schedule(MallUser mallUser) {
        pushItemRuleService.checkValid();
    }


}
