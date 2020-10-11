package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.model.HistoryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-06-20 11:59
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Service
public class HistoryThreadService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private MallOrderItemService orderItemService;

    //    @Async
    @TxTransaction(isStart = true)
    @Transactional
    public void makeHistoryOrderInfo(List<HistoryOrder> listStr) {
        MallSku sku = itemFeign.getSkuByCode("F001");
        for (HistoryOrder l : listStr) {
            MallUser user = userFeign.getUserByPhone(l.getPhone());
            if (user == null) {
                continue;
            }
            if (checkHistoryOrder(user.getId())) {
                continue;
            }
            String orderId = IDUtils.genOrderId();
            BigDecimal amt = l.getAmt();
            //创建订单号
            insertOrderInfo(user.getId(), orderId, amt, l.getCreateDate(), l.getSubordinate());
            //创建支付单号
            insertPayInfo(user.getId(), orderId, amt);
            //创建商品信息
            insertItemInfo(orderId, amt, sku);
        }
    }

    @TxTransaction(isStart = true)
    @Transactional
    public void makeHistoryOrderInfo(HistoryOrder l) {
        MallSku sku = itemFeign.getSkuByCode("F001");
        MallUser user = userFeign.getUserByPhone(l.getPhone());
        if (user == null) {
            return;
        }
        if (checkHistoryOrder(user.getId())) {
            return;
        }
        String orderId = IDUtils.genOrderId();
        BigDecimal amt = l.getAmt();
        //创建订单号
        insertOrderInfo(user.getId(), orderId, amt, l.getCreateDate(), l.getSubordinate());
        //创建支付单号
        insertPayInfo(user.getId(), orderId, amt);
        //创建商品信息
        insertItemInfo(orderId, amt, sku);
    }


    private void insertOrderInfo(String userId, String orderId, BigDecimal amt, Date createDate, String subordinate) {
        MallOrderInfo orderInfo = new MallOrderInfo();
        orderInfo.setOrderId(orderId);
        orderInfo.setMallUserId(userId);
        orderInfo.setPaymentAmt(amt);
        orderInfo.setOriginAmt(amt);
        orderInfo.setPostFeeAmt(amt);
        orderInfo.setCurrency("0");
        orderInfo.setOrderType("2");
        orderInfo.setBelongsCode("0");
        orderInfo.setCreateDate(createDate);
        orderInfo.setUpdateDate(createDate);
        orderInfo.setPayEndDate(new Date());
        orderInfo.setOrderStatus("0");
        orderInfo.setHistoryFreightStatus("1");
        orderInfo.setIsCanCancel("1");
        orderInfo.setCsMemo(subordinate);
        this.insert(orderInfo);
    }

    private void insertPayInfo(String userId, String orderId, BigDecimal amt) {
        MallPayInfo mallPayInfo = new MallPayInfo();
        mallPayInfo.setTotalAmt(amt);
        mallPayInfo.setMallUserId(userId);
        mallPayInfo.setOrderId(orderId);
        mallPayInfo.setCurrency("0");
        mallPayInfo.setPayStatus("0");
        mallPayInfo.setStatus("0");
        mallPayInfo.setTitle("补历史运费");
        mallPayInfo.setIsDel("0");
        mallPayInfo.setCreateDate(new Date());
        payFeign.insertPayInfo(mallPayInfo);
    }

    private void insertItemInfo(String orderId, BigDecimal amt, MallSku sku) {
        MallOrderItem mallOrderItem = new MallOrderItem();
        mallOrderItem.setId(IDUtils.genId());
        mallOrderItem.setAmount(amt);
        mallOrderItem.setOrderId(orderId);
        mallOrderItem.setCurrency("0");
        mallOrderItem.setPrice(BigDecimal.ONE);
        mallOrderItem.setUnit("件");
        mallOrderItem.setItemId(sku.getItemId());
        mallOrderItem.setSkuCode("F001");
        mallOrderItem.setStatus("0");
        mallOrderItem.setSpec("1");
        mallOrderItem.setCreateDate(new Date());
        orderItemService.insert(mallOrderItem);
    }

    public boolean checkHistoryOrder(String userId) {
        List<MallOrderInfo> list = this.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("history_freight_status", "1")
                .eq("mall_user_id", userId)
                .eq("is_del", "0"));
        return !CollectionUtils.isEmpty(list);
    }
}
