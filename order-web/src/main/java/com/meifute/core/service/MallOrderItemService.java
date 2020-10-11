package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.mapper.MallOrderItemMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by liuliang on 2018/10/9.
 */
@Service
@Slf4j
public class MallOrderItemService extends ServiceImpl<MallOrderItemMapper, MallOrderItem> {

    @Autowired
    private MallOrderItemMapper orderItemMapper;


    public List<MallOrderItem> selectByOrderId(String orderId) {
        List<MallOrderItem> list = this.selectList(
                new EntityWrapper<MallOrderItem>()
                        .eq("order_id", orderId)
                        .eq("is_del", 0));
        return list;
    }

    @Transactional
    @TxTransaction
    public Boolean insertOrderItemInfo(MallOrderItem mallOrderItem) {
        if(ObjectUtils.isNullOrEmpty(mallOrderItem.getId())) {
            mallOrderItem.setId(IDUtils.genId());
        }
        mallOrderItem.setCreateDate(new Date());
        return this.insert(mallOrderItem);
    }

    @Transactional
    @TxTransaction
    public Boolean updateOrderItemInfo(MallOrderItem mallOrderItem) {
        mallOrderItem.setUpdateDate(new Date());
        return this.updateById(mallOrderItem);
    }

    public Boolean updateOrderItemInfoByOrderId(MallOrderItem mallOrderItem) {
        mallOrderItem.setUpdateDate(new Date());
        return this.update(mallOrderItem,
                new EntityWrapper<MallOrderItem>()
                .eq("order_id", mallOrderItem.getOrderId()));
    }






}
