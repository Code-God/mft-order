package com.meifute.core.service;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallPayInfo;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.vo.RefundOrderInfoParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Auther: wxb
 * @Date: 2018/10/29 20:03
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class RefundOrderInfoService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {

    @Autowired
    private PayFeign payFeign;
    @Autowired
    private MallOrderItemService mallOrderItemService;

    /**
     * 创建退款订单-订单取消或关闭退运费
     * @param refundOrderInfoParam
     * @return
     */
    @TxTransaction
    @Transactional
    public MallOrderInfo createRefundOrderInfo(RefundOrderInfoParam refundOrderInfoParam,MallOrderInfo orderInfo) {
        //生成订单号
        String orderId = IDUtils.genOrderId();
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        //封装参数
        mallOrderInfo.setPaymentAmt(refundOrderInfoParam.getPrice());
        mallOrderInfo.setOriginAmt(refundOrderInfoParam.getPrice());
        mallOrderInfo.setPostFeeAmt(BigDecimal.ZERO);
        mallOrderInfo.setMallUserId(refundOrderInfoParam.getUserId());
        mallOrderInfo.setBelongsCode(MallStatusEnum.BELONGS_CODE_000.getCode());
        mallOrderInfo.setOrderId(orderId);
        mallOrderInfo.setCurrency(MallStatusEnum.CURRENCY_CODE_000.getCode());
        mallOrderInfo.setOrderDescribe("订单取消或关闭退运费");
        mallOrderInfo.setOrderStatus(refundOrderInfoParam.getOrderStatus());
        mallOrderInfo.setOrderType(MallOrderTypeEnum.ORDER_TYPE_007.getCode());
        mallOrderInfo.setIsDel(MallStatusEnum.IS_DEL_CODE_001.getCode());
        mallOrderInfo.setRelationOrderId(orderInfo.getOrderId());//关联原订单号 2019-3-7 19:30
        Date now = new Date();
        Date afterDate = new Date(now.getTime() + 30 * 60 * 1000);
        mallOrderInfo.setCreateDate(now);
        mallOrderInfo.setPayEndDate(afterDate);//支付截止时间30分钟后
        mallOrderInfo.setUpdateDate(new Date());
        mallOrderInfo.setIsCanCancel("1");
        this.insert(mallOrderInfo);
        return mallOrderInfo;
    }

    /**
     * 创建退款支付单号
     * @param userId
     * @param mallOrderInfo
     * @param payStatus
     * @return
     */
    public MallPayInfo createRefundToPayInfo(String userId, MallOrderInfo mallOrderInfo, String payStatus) {
        MallPayInfo mallPayInfo = new MallPayInfo();
        mallPayInfo.setPayId(IDUtils.genId());
        mallPayInfo.setCreateDate(new Date());
        mallPayInfo.setTotalAmt(mallOrderInfo.getPaymentAmt());
        mallPayInfo.setCredit(mallOrderInfo.getCredit());
        mallPayInfo.setMallUserId(userId);
        mallPayInfo.setOrderId(mallOrderInfo.getOrderId());
        mallPayInfo.setCurrency(mallOrderInfo.getCurrency());
        mallPayInfo.setPayStatus(payStatus);
        mallPayInfo.setStatus(MallStatusEnum.STATUS_CODE_000.getCode());
        mallPayInfo.setTitle("平台退款");
        mallPayInfo.setIsDel(MallStatusEnum.IS_DEL_CODE_001.getCode());
        payFeign.insertPayInfo(mallPayInfo);
        return mallPayInfo;
    }

    /**
     * 创建退货订单商品数据
     * @param itemlist
     * @param mallOrderInfo
     */
    public void createRefundOrderItemInfo(List<MallOrderItem> itemlist, MallOrderInfo mallOrderInfo) {
        itemlist.forEach(p -> {
            MallOrderItem mallOrderItem = new MallOrderItem();
            mallOrderItem.setId(IDUtils.genId());
            mallOrderItem.setOrderId(mallOrderInfo.getOrderId());
            mallOrderItem.setCurrency(mallOrderInfo.getCurrency());
            mallOrderItem.setCreateDate(new Date());
            mallOrderItem.setStatus(MallStatusEnum.ORDER_STATUS_000.getCode());
            mallOrderItem.setItemId(p.getItemId());
            mallOrderItem.setSkuCode(p.getSkuCode());
            BigDecimal amount = p.getAmount();
            if (p.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                amount = amount.multiply(BigDecimal.valueOf(-1));
            }
            mallOrderItem.setAmount(amount);
            mallOrderItem.setType("0");
            mallOrderItem.setPrice(p.getPrice());
            mallOrderItem.setUnit(p.getUnit());
            mallOrderItem.setSpec(p.getSpec());
            mallOrderItemService.insertOrderItemInfo(mallOrderItem);
        });
    }

}
