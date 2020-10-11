package com.meifute.core.service;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.MallOrderPriceDetail;
import com.meifute.core.mapper.MallOrderPriceDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @program: m-mall-order
 * @description: 出库价格详情
 * @author: Mr.Wang
 * @create: 2019-09-25 14:20
 **/
@Slf4j
@Service
public class MallOrderPriceDetailService extends ServiceImpl<MallOrderPriceDetailMapper, MallOrderPriceDetail> {
    @Autowired
    MallOrderPriceDetailMapper mallOrderPriceDetailMapper;

    public Boolean saveOrderPriceDetail(MallOrderPriceDetail mallOrderPriceDetail) {
        Integer insert = mallOrderPriceDetailMapper.insert(mallOrderPriceDetail);
        return insert > 0 ? true : false;
    }
}
