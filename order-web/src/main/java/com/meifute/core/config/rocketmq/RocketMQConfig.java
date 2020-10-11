package com.meifute.core.config.rocketmq;

import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import com.aliyun.openservices.ons.api.bean.ProducerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Auther: wuxb
 * @Date: 2018-12-27 15:31
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Configuration
@Slf4j
@RefreshScope
@Data
public class RocketMQConfig {

    @Value("${mq_access_key_id}")
    public String accessKeyId;
    @Value("${mq_access_key_secret}")
    public String accessKeySecret;
    //超时时间
    @Value("${ali-sendMsgTimeoutMillis}")
    public String sendMsgTimeoutMillis;
    //实例地址
    @Value("${mall-ali-ons-addr}")
    private String mallAliOnsAddr;
    //短信营销提交审核单topic
    @Value("${market-verify-topic}")
    private String marketVerifyTopic;
    //短信营销提交审核单groupId
    @Value("${market-verify-gid}")
    private String marketVerifyGid;

    public static final String MARKET_VERIFY = "market_verify";


    //短信营销提交审核单
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public ProducerBean marketVerifyProducer() {
        return producer(marketVerifyGid, mallAliOnsAddr);
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public ConsumerBean marketVerifyConsumer() {
        return consumer(marketVerifyTopic, marketVerifyGid, MARKET_VERIFY, mallAliOnsAddr, new MarketAcConsumerListener());
    }


    private ProducerBean producer(String GID, String onsAddr) {
        ProducerBean producerBean = new ProducerBean();
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, GID);
        properties.put(PropertyKeyConst.AccessKey, accessKeyId);
        properties.put(PropertyKeyConst.SecretKey, accessKeySecret);
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, sendMsgTimeoutMillis);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, onsAddr);
        producerBean.setProperties(properties);
        return producerBean;
    }

    private ConsumerBean consumer(String TOPIC, String GID, String tag, String onsAddr, MessageListener messageListener) {
        ConsumerBean consumerBean = new ConsumerBean();
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, GID);
        properties.put(PropertyKeyConst.AccessKey, accessKeyId);
        properties.put(PropertyKeyConst.SecretKey, accessKeySecret);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, onsAddr);
        consumerBean.setProperties(properties);
        Subscription subscription = new Subscription();
        subscription.setTopic(TOPIC);
        subscription.setExpression(tag);
        Map<Subscription, MessageListener> map = new HashMap<>();
        map.put(subscription, messageListener);
        consumerBean.setSubscriptionTable(map);
        return consumerBean;
    }
}
