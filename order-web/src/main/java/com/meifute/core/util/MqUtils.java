package com.meifute.core.util;

import com.meifute.core.service.SMSMarketActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MqUtils {

    @Autowired
    private SMSMarketActivityService smsMarketActivityService;

    @PostConstruct
    public void init() {
        MqUtils.getInstance().smsMarketActivityService = this.smsMarketActivityService;
    }

    /**
     * 实现单例 start
     */
    private static class SingletonHolder {
        private static final MqUtils INSTANCE = new MqUtils();
    }

    private MqUtils() {
    }

    public static final MqUtils getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 实现单例 end
     */
    public SMSMarketActivityService getMQConsumerService() {
        return MqUtils.getInstance().smsMarketActivityService;
    }
}
