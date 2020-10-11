package com.meifute.core.config.rocketmq;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.meifute.core.entity.SMSAcOrderVerify;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.util.MqUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @Auther: wuxb
 * @Date: 2019-11-20 15:41
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Service
public class MarketAcConsumerListener implements MessageListener {

    @Override
    public Action consume(Message message, ConsumeContext context) {
        log.info("开始执行短信营销提交审核单处理。。。。。。。。");
        try {
            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("订阅消息：{}", msg);
            SMSAcOrderVerify verify = JSONUtil.json2pojo(msg, SMSAcOrderVerify.class);
            MqUtils.getInstance().getMQConsumerService().verifyTimeoutListen(verify);
            return Action.CommitMessage;
        } catch (Exception e) {
            //消费失败
            log.info("消费失败:{0}", e);
            //重试
            return Action.ReconsumeLater;
        }
    }


}
