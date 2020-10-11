package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.entity.MallOrderToPush;
import com.meifute.core.feignclient.NotifyFeign;
import com.meifute.core.mapper.MallOrderToPushMapper;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.vo.OrderInfoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-03-06 18:36
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class MallOrderToPushService extends ServiceImpl<MallOrderToPushMapper, MallOrderToPush> {

    @Autowired
    private NotifyFeign notifyFeign;
    @Autowired
    private OrderDelayService orderDelayService;

    public List<MallOrderToPush> getOrderToPush() {
        List<MallOrderToPush> list = this.selectList(new EntityWrapper<MallOrderToPush>()
                .eq("status", 0)
                .eq("order_origin", 0)
                .eq("is_del", 0)
                .orderBy("create_date", true));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list;
    }

    public List<MallOrderToPush> getAcOrderToPush() {
        List<MallOrderToPush> list = this.selectList(new EntityWrapper<MallOrderToPush>()
                .eq("status", 0)
                .eq("order_origin", 2)
                .eq("is_del", 0));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list;
    }

    public MallOrderToPush getOrderPushInfo(String orderId) {
        List<MallOrderToPush> list = this.selectList(new EntityWrapper<MallOrderToPush>()
                .eq("status", 0)
                .eq("order_id", orderId)
                .eq("is_del", 0));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @TxTransaction
    @Transactional
    public void insertPush(String orderId, int orderOrigin) {
        List<MallOrderToPush> list = this.selectList(new EntityWrapper<MallOrderToPush>()
                .eq("order_id", orderId)
                .eq("status", "0")
                .eq("is_del", "0"));
        if (CollectionUtils.isEmpty(list)) {
            MallOrderToPush mallOrderToPush = new MallOrderToPush();
            mallOrderToPush.setId(IDUtils.genId());
            mallOrderToPush.setCreateDate(new Date());
            mallOrderToPush.setOrderId(orderId);
            mallOrderToPush.setStatus("0");
            mallOrderToPush.setOrderOrigin(orderOrigin);
            this.insert(mallOrderToPush);
        }
    }

    public void insertPushNew(String orderId, int orderOrigin) {
        List<MallOrderToPush> list = this.selectList(new EntityWrapper<MallOrderToPush>()
                .eq("order_id", orderId)
                .eq("status", "0")
                .eq("is_del", "0"));
        if (CollectionUtils.isEmpty(list)) {
            MallOrderToPush mallOrderToPush = new MallOrderToPush();
            mallOrderToPush.setId(IDUtils.genId());
            mallOrderToPush.setCreateDate(new Date());
            mallOrderToPush.setOrderId(orderId);
            mallOrderToPush.setStatus("0");
            mallOrderToPush.setOrderOrigin(orderOrigin);
            this.insert(mallOrderToPush);
        }
    }

    @TxTransaction
    @Transactional
    public void toPushOrderToPush(List<MallOrderToPush> list) {
        list.forEach(p -> {
            sendExpressMQ(p.getOrderId());
            p.setStatus("1");
            p.setUpdateDate(new Date());
            this.updateById(p);
        });
    }

    private void sendExpressMQ(String orderId) {
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        orderInfoMessage.setOrderOrigin(0);
        orderInfoMessage.setOrderId(orderId);
        orderInfoMessage.setIsAdmin("3");
        orderDelayService.openToPush(orderInfoMessage);
    }

    public void toPush() {
        List<MallOrderToPush> orderToPushList = getOrderToPush();
        if (ObjectUtils.isNotNullAndEmpty(orderToPushList)) {
            log.info("打开产品订单开关触发推单数量" + orderToPushList.size());

            for (MallOrderToPush p : orderToPushList) {
                String on = RedisUtil.get("autoPush:online");
                if ("0".equals(on)) {
                    try {
                        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
                        orderInfoMessage.setOrderOrigin(0);
                        orderInfoMessage.setOrderId(p.getOrderId());
                        orderInfoMessage.setIsAdmin("3");
                        boolean b = orderDelayService.openToPush(orderInfoMessage);
                        if (b) {
                            p.setStatus("1");
                            p.setUpdateDate(new Date());
                            this.updateById(p);
                        }
                    } catch (Exception e) {
                        log.info("--------->开关触发推单:{0}", e);
                    }
                }
            }
        }
    }

    private boolean checkPush() {
        String orderNumber = RedisUtil.get("oneDayCanPushOrderNumber");
        if (orderNumber == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date zero = calendar.getTime();
            long v = zero.getTime() - new Date().getTime();
            RedisUtil.set("oneDayCanPushOrderNumber", "0", Integer.parseInt(String.valueOf(v / 1000)));
        } else {
            String isCanPushNumber = RedisUtil.get("isCanPushNumber");
            if (Integer.parseInt(orderNumber) >= (isCanPushNumber == null ? 5000 : Integer.parseInt(isCanPushNumber))) {
                return false;
            }
        }
        return true;
    }

    public void toAcPush() {
        List<MallOrderToPush> orderToPushs = getAcOrderToPush();
        if (ObjectUtils.isNotNullAndEmpty(orderToPushs)) {
            log.info("定时活动推单数量" + orderToPushs.size());
            orderToPushs.forEach(p -> {
                String on = RedisUtil.get("autoPush:acOnline");
                if ("0".equals(on)) {
                    OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
                    orderInfoMessage.setOrderId(p.getOrderId());
                    orderInfoMessage.setOrderOrigin(2);
                    orderDelayService.delayExpressListener(orderInfoMessage);
                    p.setStatus("1");
                    p.setUpdateDate(new Date());
                    this.updateById(p);
                }
            });
        }
    }

}
