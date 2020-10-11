package com.meifute.core.service;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.orderfeedback.MallFeedbackGoods;
import com.meifute.core.mapper.MallFeedBackGoodsMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @program: m-mall-order
 * @description:
 * @author: Mr.Wang
 * @create: 2020-05-25 13:58
 **/
@Service
@Slf4j
public class MallFeedbackGoodsService extends ServiceImpl<MallFeedBackGoodsMapper, MallFeedbackGoods> {

    @Autowired
    MallFeedBackGoodsMapper mallFeedBackGoodsMapper;

    public Boolean saveGoods(List<MallFeedbackGoods> goodsList, String id) {
        List<MallFeedbackGoods> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(goodsList)) {
            goodsList.forEach(good -> {
                good.setId(IDUtils.genId());
                good.setFeedbackId(id);
                good.setCreateDate(new Date());
                if (good.getAfterSaleAmount().compareTo(new BigDecimal(BigInteger.ZERO)) > 0)
                    list.add(good);
            });
            if (!CollectionUtils.isEmpty(list))
                return insertBatch(list);
            return true;
        }

        return false;
    }
}
