package com.meifute.core.service;

import com.meifute.core.dto.AutoPushItem;
import com.meifute.core.dto.AutoSku;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-04-04 11:40
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class AutoPushService {

    public AutoPushItem getAutoItems() {
        AutoPushItem autoPushItem = new AutoPushItem();
        String on = RedisUtil.get("autoPush:online");
        if (ObjectUtils.isNullOrEmpty(on)) {
            on = "1";
        }
        String items = RedisUtil.get("autoPush:items");
        if (!ObjectUtils.isNullOrEmpty(items)) {
            List<AutoSku> autoSkus = JSONUtil.json2list(items, AutoSku.class);
            autoPushItem.setAutoSkus(autoSkus);
        }
        autoPushItem.setOnline(on);
        return autoPushItem;
    }

}
