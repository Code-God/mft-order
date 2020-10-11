package com.meifute.core.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meifute.core.entity.MallItem;
import com.meifute.core.entity.MallSku;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.mmall.common.enums.Const;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Auther: wll
 * @Date: 2018/11/27 13:56
 * @Auto: I AM A CODE MAN !
 * @Description:
 */
@Component
@Slf4j
public class ItemCacheUtils {

    private static ItemFeign itemFeign;
    @Autowired
    public ItemCacheUtils(ItemFeign itemFeign){
        ItemCacheUtils.itemFeign=itemFeign;
    }

    public  static MallItem getItemById(String id){
        log.info("======================item_id:{}",id);
        MallItem mallItem = null;
        String userString = RedisUtil.get(Const.ITEM_INFO_ID + id);
        log.info("=======================redis-item:{}", userString);
        if (ObjectUtils.isNotNullAndEmpty(userString)) {
            mallItem = JSONObject.parseObject(userString, MallItem.class);
            log.info("=======================mallItem:{}", mallItem);
            return mallItem;
        }
        mallItem = itemFeign.getItemById(id);
        if (ObjectUtils.isNotNullAndEmpty(mallItem)) {
            RedisUtil.set(Const.ITEM_INFO_ID + id, JSON.toJSONString(mallItem),   60 * 10);
        }
        return mallItem;
    }

    public static MallSku getSkuByCode(String code){
        MallSku sku = null;
        String userString = RedisUtil.get(Const.SKU_INFO_ID + code);
        if (ObjectUtils.isNotNullAndEmpty(userString)) {
            sku = JSONObject.parseObject(userString,MallSku.class);
            return sku;
        }
        sku = itemFeign.getSkuByCode(code);
        if (ObjectUtils.isNotNullAndEmpty(sku)) {
            RedisUtil.set(Const.SKU_INFO_ID + code, JSON.toJSONString(sku),  60 * 10);
        }
        return sku;
    }
}
