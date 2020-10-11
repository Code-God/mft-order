package com.meifute.core.service;

import com.codingapi.tx.annotation.TxTransaction;
import com.google.common.base.Joiner;
import com.jd.open.api.sdk.response.ECLP.EclpOrderAddOrderResponse;
import com.meifute.core.dto.AutoPushItem;
import com.meifute.core.dto.AutoSku;
import com.meifute.core.dto.JdPushFromAdmin;
import com.meifute.core.entity.*;
import com.meifute.core.entity.activity.MallAcOrder;
import com.meifute.core.entity.activity.MallAcOrderItem;
import com.meifute.core.entity.activity.OrderParam;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.NotifyFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallOrderVerifyEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import com.meifute.core.util.ItemCacheUtils;
import com.meifute.core.vo.OrderInfoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: wuxb
 * @Date: 2018-12-14 11:37
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class OrderDelayService {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private NotifyFeign notifyFeign;
    @Autowired
    private QimenPushService qimenPushService;
    @Autowired
    private MallOrderInfoService mallOrderInfoService;
    @Autowired
    private MallOrderVerifyService mallOrderVerifyService;
    @Autowired
    private MallOrderItemService mallOrderItemService;
    @Autowired
    private JDExpressPushService jdExpressPushService;
    @Autowired
    private MallOrderToPushService mallOrderToPushService;
    @Autowired
    private AutoPushService autoPushService;
    @Autowired
    private JiaYiOrderService jiaYiOrderService;
    @Autowired
    private WuYouService wuYouService;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private MallPushItemRuleService pushItemRuleService;

    public static final String C035_H = "C035-H";
    public static final String C035_X = "C035-X";
    public static final String C034_H = "C034-H";
    public static final String C034_X = "C034-X";
    public static final String C033_H = "C033-H";
    public static final String C033_X = "C033-X";


    /**
     * 超时未支付取消 (现产品商场的订单改为24小时取消，活动订单为30分钟)
     *
     * @param orderInfoMessage
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public void delayOrderListener(OrderInfoMessage orderInfoMessage) {
        log.info("========>订单超时支付处理:{}", orderInfoMessage);
        String orderId = orderInfoMessage.getOrderId();
        if (orderInfoMessage.getOrderOrigin() == 2) {
            // 活动订单
            OrderParam orderParam = new OrderParam();
            orderParam.setIsSystem(true);
            orderParam.setOrderId(orderId);
            itemFeign.cancelOrder(orderParam);
            return;
        }
        MallOrderInfo mallOrderInfo = mallOrderInfoService.selectById(orderId);
        // 支付超时 30分钟
        payOverTime(mallOrderInfo);

        log.info("订单超时支付处理成功，mallOrderInfo:{}", mallOrderInfo);
    }

    /**
     * 三天审核拒绝或审核超时取消
     *
     * @param orderInfoMessage
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public void delayOrderVerifyListener(OrderInfoMessage orderInfoMessage) {
        String orderId = orderInfoMessage.getOrderId();
        log.info("========>订单审核拒绝或超时处理，订单号:{}", orderId);

        MallOrderInfo mallOrderInfo = mallOrderInfoService.selectById(orderId);
        // 审核过期 三天
        verifyOverTime(mallOrderInfo);
        log.info("订单审核拒绝或超时处理成功，mallOrderInfo:{}", mallOrderInfo);

    }

    /**
     * 支付超时
     *
     * @param mallOrderInfo
     */
    private void payOverTime(MallOrderInfo mallOrderInfo) {
        //类型判断
        if ("0".equals(mallOrderInfo.getOrderStatus())) {
            // 1 更新订单表 订单取消
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = format.format(new Date());
            MallOrderInfo orderInfo = new MallOrderInfo();
            orderInfo.setUpdateDate(new Date());
            orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_006.getCode());
            orderInfo.setOrderId(mallOrderInfo.getOrderId());
            orderInfo.setIsCanCancel("1");
            orderInfo.setSystemMemo("订单于" + date + " 由于支付超时被取消");
            mallOrderInfoService.updateOrderById(orderInfo);
            // 3 更新订单商品表 交易取消
            MallOrderItem mallOrderItem = new MallOrderItem();
            mallOrderItem.setOrderId(mallOrderInfo.getOrderId());
            mallOrderItem.setStatus(MallOrderStatusEnum.ORDER_STATUS_006.getCode());
            mallOrderItemService.updateOrderItemInfoByOrderId(mallOrderItem);

            List<MallCloudStockLog> stockLogs = agentFeign.queryCloudStockLogByRelation(MallCloudStockLog.builder().relationId(mallOrderInfo.getOrderId()).relationIdType("0").build());

            //云库存提货回退
            if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    mallOrderInfoService.backCloud(mallOrderInfo, "6");
                }
            }
            //云库存换货回退
            if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    mallOrderInfoService.backCloud(mallOrderInfo, "6");
                }

                // 4 redis增加库存
                List<MallOrderItem> resultItems = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
                resultItems.forEach(p -> {
                    if ("0".equals(p.getType())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().abs().longValue());
                    }
                });
            }
            //直发或入云回退
            if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())
                    || MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType())) {
                // 4 redis增加库存
                List<MallOrderItem> resultItems = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
                resultItems.forEach(p -> {
                    MallSku sku = ItemCacheUtils.getSkuByCode(p.getSkuCode());
                    MallUser user = userFeign.getUserByIdFromFeign(mallOrderInfo.getMallUserId());
                    if (Integer.parseInt(user.getRoleId()) >= 4 || MallStatusEnum.BELONGS_CODE_001.getCode().equals(sku.getBelongsCode())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().longValue());
                    }
                });
            }
            MallPropelNews mallPropelNews = new MallPropelNews();
            mallPropelNews.setMallUserId(mallOrderInfo.getMallUserId());
            mallPropelNews.setTitle("支付超时");
            mallPropelNews.setSubTitle("您当前有订单超时未支付，已为您自动取消！");
            mallPropelNews.setAmt(mallOrderInfo.getPaymentAmt());
            mallPropelNews.setRelationId(mallOrderInfo.getOrderId());
            mallPropelNews.setType("4");//付款通知
            mallPropelNews.setRelationType("0");
            mallPropelNews.setStatus("0");
            mallPropelNews.setNewsType("1");
            notifyFeign.createNewPropelNews(mallPropelNews);
            log.info("订单30分钟过期未支付,已取消该订单！");
        }
    }

    /**
     * 审核超时
     *
     * @param mallOrderInfo
     */
    private void verifyOverTime(MallOrderInfo mallOrderInfo) {
        // 类型判断
        if (MallOrderStatusEnum.ORDER_STATUS_001.getCode().equals(mallOrderInfo.getOrderStatus())
                || MallOrderStatusEnum.ORDER_STATUS_002.getCode().equals(mallOrderInfo.getOrderStatus())) {
            // 1 更新订单表 订单取消
            MallOrderInfo orderInfoUpdate = new MallOrderInfo();
            orderInfoUpdate.setIsCanCancel("1");

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = format.format(new Date());
            // 上级审核中
            if (MallOrderStatusEnum.ORDER_STATUS_001.getCode().equals(mallOrderInfo.getOrderStatus())) {
                orderInfoUpdate.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_006.getCode());
                orderInfoUpdate.setSystemMemo("订单于" + date + " 由于上级审核超时被取消");
            }
            // 上级审核未通过
            if (MallOrderStatusEnum.ORDER_STATUS_002.getCode().equals(mallOrderInfo.getOrderStatus())) {
                orderInfoUpdate.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_006.getCode());
                orderInfoUpdate.setSystemMemo("订单于" + date + " 由于上级审核未通过被取消");
            }
            orderInfoUpdate.setUpdateDate(new Date());
            orderInfoUpdate.setOrderId(mallOrderInfo.getOrderId());
            mallOrderInfoService.updateOrderById(orderInfoUpdate);

            //4 更新审核单
            List<MallOrderVerify> list = mallOrderVerifyService.getMallOrderVerifyByOrderId(mallOrderInfo.getOrderId());
            if (ObjectUtils.isNotNullAndEmpty(list)) {
                list.forEach(p -> {
                    if (MallOrderVerifyEnum.VERIFY_STATUS_000.getCode().equals(p.getVerifyStatus())) {
                        MallOrderVerify verify = new MallOrderVerify();
                        verify.setId(p.getId());
                        verify.setUpdateDate(new Date());
                        verify.setVerifyStatus(MallOrderVerifyEnum.VERIFY_STATUS_003.getCode());
                        mallOrderVerifyService.updateVerifyOrder(verify);
                    }
                });
            }
            MallPropelNews mallPropelNews = new MallPropelNews();
            mallPropelNews.setMallUserId(mallOrderInfo.getMallUserId());
            mallPropelNews.setTitle("审核结果");
            mallPropelNews.setSubTitle("您的订单审核未通过或审核超时，已为您自动取消！");
            mallPropelNews.setAmt(mallOrderInfo.getPaymentAmt());
            mallPropelNews.setRelationId(list.get(0).getId());
            mallPropelNews.setType("2");//审核通知
            mallPropelNews.setRelationType("0");
            mallPropelNews.setStatus("0");
            mallPropelNews.setNewsType("1");
            notifyFeign.createNewPropelNews(mallPropelNews);
            log.info("订单三天审核过期,已取回退该订单！");
        }
    }

    /**
     * 开关触发推单
     *
     * @param orderInfoMessage
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public boolean openToPush(OrderInfoMessage orderInfoMessage) {
        String orderId = orderInfoMessage.getOrderId();
        orderInfoMessage.setIsBitch("0");
        boolean result = false;
        try {
            log.info("开始进入推单主程序，订单ID:{}", orderId);
            MallOrderInfo mallOrderInfo = mallOrderInfoService.selectByIdNew(orderId);
            List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
            if (0 == orderInfoMessage.getOrderOrigin()) {
                String msg = expressPush(mallOrderInfo, orderInfoMessage, 0, itemList);
                if (msg == null) {
                    result = true;
                }
            }
        } catch (Exception e) {
            log.info("sorry 推单失败:" + e);
            result = false;
        }
        return result;
    }

    /**
     * 后台推单
     *
     * @param orderInfoMessage
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public JdPushFromAdmin pushToJdFromAdmin(OrderInfoMessage orderInfoMessage) {
        String orderId = orderInfoMessage.getOrderId();
        boolean result = false;
        String msg = null;
        try {
            log.info("开始进入推单主程序，订单ID:{}", orderId);
            switch (orderInfoMessage.getOrderOrigin()) {
                case 0: // 产品订单
                    MallOrderInfo mallOrderInfo = mallOrderInfoService.selectByIdNew(orderId);
                    List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
                    msg = expressPush(mallOrderInfo, orderInfoMessage, 0, itemList);
                    break;
                case 2: // 活动订单
                    msg= acExpressPush(orderInfoMessage);
                    break;
            }
        } catch (Exception e) {
            log.info("sorry 推单失败:" + e);
            result = false;
        }
        if (msg == null) {
            result = true;
        }
        return JdPushFromAdmin.builder().success(result).msg(msg).build();
    }


    /**
     * 自动推单
     * 该推单在用户支付完成一个小时后执行
     *
     * @param orderInfoMessage
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public boolean delayExpressListener(OrderInfoMessage orderInfoMessage) {
        String orderId = orderInfoMessage.getOrderId();
        orderInfoMessage.setIsBitch("0");
        boolean result = false;
        try {
            log.info("开始进入推单主程序，订单ID:{}", orderId);
            switch (orderInfoMessage.getOrderOrigin()) {
                case 0: // 产品订单
                    MallOrderInfo mallOrderInfo = mallOrderInfoService.selectByIdNew(orderId);
                    List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
                    String msg = expressPush(mallOrderInfo, orderInfoMessage, 0, itemList);
                    if ("1".equals(mallOrderInfo.getLogisticsMode()) && msg == null) { //新亦源
                        result = true;
                    }
                    break;
                case 2: // 活动订单
                    acExpressPush(orderInfoMessage);
                    break;
            }
            result = true;
        } catch (Exception e) {
            log.info("sorry 推单失败:" + e);
            if (e instanceof MallException) {
                MallException exception = (MallException) e;
                throw new MallException("020040", new Object[]{exception.getMsg()});
            }
        }
        return result;
    }


    private String acExpressPush(OrderInfoMessage orderInfoMessage) {
        MallAcOrder acOrder = itemFeign.getAcOrderById(orderInfoMessage.getOrderId());
        if (acOrder == null) {
            return "该订单号位空";
        }
        if (!"3".equals(acOrder.getOrderStatus())) {
            return "订单不是待发货订单";
        }
        List<MallAcOrderItem> acItems = itemFeign.getAcOrderItemsByOrderId(orderInfoMessage.getOrderId());
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(acOrder.getId());
        mallOrderInfo.setLogisticsMode(acOrder.getDeliveryMode());
        mallOrderInfo.setAddrName(acOrder.getRecipientName());
        mallOrderInfo.setAddrPhone(acOrder.getRecipientPhone());
        mallOrderInfo.setAddrId(acOrder.getRecipientArea() + acOrder.getRecipientAddress());
        mallOrderInfo.setOrderStatus("3");
        mallOrderInfo.setBelongsCode("0");

        List<MallOrderItem> itemList = new ArrayList<>();
        acItems.forEach(p -> {
            MallOrderItem item = new MallOrderItem();
            item.setAmount(BigDecimal.valueOf(p.getNum()));
            item.setSkuCode(p.getSkuCode());
            itemList.add(item);
        });
        return expressPush(mallOrderInfo, orderInfoMessage, 2, itemList);
    }

    private String expressPush(MallOrderInfo mallOrderInfo, OrderInfoMessage orderInfoMessage, int orderOrigin, List<MallOrderItem> itemList) {
        // 1 非后台主动推单需要检验是否支持自动推单
        if (!"0".equals(orderInfoMessage.getIsAdmin()) && checkPush(itemList, orderInfoMessage.getOrderId(), orderOrigin)) {
            return "有不支持推单的商品";
        }
        String msg = null;
        // 2 推单发货 - 待发货状态下
        if ("3".equals(mallOrderInfo.getOrderStatus()) && "0".equals(mallOrderInfo.getBelongsCode())) {
            switch (mallOrderInfo.getLogisticsMode()) {
                case "0": //京东发货
                    msg = pushJD(mallOrderInfo, itemList, orderInfoMessage, orderOrigin);
                    break;
                case "1": //新亦源
                    Boolean entryOrder = qimenPushService.DeliveryOrder(mallOrderInfo, itemList);
                    if (!entryOrder) {
                        mallOrderToPushService.insertPushNew(mallOrderInfo.getOrderId(), 0);
                        log.info("-------新亦源推单失败,订单号:{}", mallOrderInfo.getOrderId());
                        msg = "新亦源推单失败";
                    }
                    break;
                case "2": //仓库发货
                    break;
                case "3": //无忧
                    Boolean aBoolean = wuYouService.orderReceive(mallOrderInfo, itemList);
                    if (!aBoolean) {
                        mallOrderToPushService.insertPushNew(mallOrderInfo.getOrderId(), 0);
                        log.info("-------无忧推单失败,订单号:{}", mallOrderInfo.getOrderId());
                        msg = "无忧推单失败";
                    }
                    break;
            }
        }
        return msg;
    }


    private String pushJD(MallOrderInfo mallOrderInfo, List<MallOrderItem> itemList, OrderInfoMessage orderInfoMessage, int orderOrigin) {
        String orderId = orderInfoMessage.getOrderId();
        String msg = null;
        if (ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getEclpSoNo())) {
            msg = "该订单单已发货";
            return msg;
        }
        if (!"2".equals(orderInfoMessage.getStatus()) && "1".equals(mallOrderInfo.getIsCanJd())) {
            msg = "该订单京东不可达";
            return msg;
        }
        List<String> eclpSoNos = new ArrayList<>();
        List<MallOrderItem> list = new ArrayList<>();

        //商品换算
        itemList.forEach(p -> {
            p.setAmount(p.getAmount().abs());
            if (p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                List<MallOrderItem> i = getUnitChange(getSpec(p.getSkuCode()), p.getAmount());
                list.addAll(i);
            }
        });
        // 3 赠品
        List<MallOrderItem> gifts = giveThisItem(list);
        if (gifts.size() > 0) {
            list.addAll(gifts);
        }
        // 4 京东推单
        List<String> goodsNo = new ArrayList<>(); // 商品编码
        List<String> amount = new ArrayList<>(); // 数量

        // 5 智能组合推单
        List<MallPushItemRule> rules = pushItemRuleService.getPushItemRules();

        list.forEach(p -> {
            if (rules != null) {
                rules.forEach(r -> {
                    if (p.getSkuCode().trim().equals(r.getSkuCode())) {
                        p.setSkuCode(r.getReplaceSkuCode());
                        p.setAmount(p.getAmount().multiply(new BigDecimal(r.getProportion().split(":")[1])));
                        return;
                    }
                });
            }
            MallSkuSpec skuSpec = getSpec(p.getSkuCode());
            goodsNo.add(skuSpec.getTransportGoodsNo());
            amount.add(String.valueOf(p.getAmount().intValue()));
        });

        String goodsNoStr = Joiner.on(",").join(goodsNo);
        String amountStr = Joiner.on(",").join(amount);
        String recipientName = mallOrderInfo.getAddrName();
        String recipientPhone = mallOrderInfo.getAddrPhone();
        String address = mallOrderInfo.getAddrId();
        EclpOrderAddOrderResponse response = jdExpressPushService.jdExpressPush(orderId, goodsNoStr, amountStr, recipientName, recipientPhone, address);
        String eclpSoNo = null;
        if (response != null) {
            eclpSoNo = response.getEclpSoNo();
            msg = response.getZhDesc();
        }
        if (eclpSoNo == null) {
            mallOrderToPushService.insertPushNew(orderId, orderOrigin);
            return msg;
        }
        eclpSoNos.add(eclpSoNo);

        if (orderOrigin == 0) {
            finishOrder(orderId, JSONUtil.obj2json(eclpSoNos));
        } else if (orderOrigin == 2) {
            itemFeign.updateAcOrderById(MallAcOrder.builder()
                    .id(mallOrderInfo.getOrderId())
                    .orderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode())
                    .isCanCancel("1")
                    .logisticsCompany("京东物流")
                    .singleNum(eclpSoNo)
                    .updateTime(new Date())
                    .build());
        }
        return msg;
    }

    private void finishOrder(String orderId, String eclpSoNos) {
        // a 更新订单表,已推单的不可取消
        MallOrderInfo orderInfoParam = new MallOrderInfo();
        orderInfoParam.setUpdateDate(new Date());
        orderInfoParam.setOrderId(orderId);
        orderInfoParam.setIsCanCancel("1");
        orderInfoParam.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
        // b 京东的订单的出库单号
        orderInfoParam.setEclpSoNo(eclpSoNos);
        orderInfoParam.setExpressCompany("京东物流");
        orderInfoParam.setIsCanJd("0");
        updateOrderById(orderInfoParam);
        log.info("Congratulations 订单推单成功:{}", orderInfoParam);
    }


    private boolean checkPush(List<MallOrderItem> itemList, String orderId, int orderOrigin) {
        boolean r = false;
        if (orderOrigin == 0 || orderOrigin == 5) {
            // 1 检验是否支持自动推单
            AutoPushItem autoItems = autoPushService.getAutoItems();
            if ("0".equals(autoItems.getOnline())) { //开启
                if (ObjectUtils.isNotNullAndEmpty(autoItems.getAutoSkus())) {
                    //不支持自动推单的商品
                    List<AutoSku> collect = autoItems.getAutoSkus().stream().filter(p -> "1".equals(p.getAutoed())).collect(Collectors.toList());
                    log.info("this is 不支持自动推单的商品:{}", collect);
                    List<String> offSkus = collect.stream().map(AutoSku::getSkuCode).collect(Collectors.toList());
                    for (MallOrderItem p : itemList) {
                        if (ObjectUtils.isNotNullAndEmpty(offSkus) && offSkus.contains(p.getSkuCode())) {
                            mallOrderToPushService.insertPushNew(orderId, orderOrigin);
                            r = true;
                        }
                    }
                }
            } else {
                log.info("sorry 自动推单已关闭");
                mallOrderToPushService.insertPushNew(orderId, orderOrigin);
                r = true;
            }
        } else if (orderOrigin == 2) {
            String on = RedisUtil.get("autoPush:acOnline");
            if ("1".equals(on)) {
                mallOrderToPushService.insertPushNew(orderId, orderOrigin);
                r = true;
            }
        }
        return r;
    }


    private void updateOrderById(MallOrderInfo mallOrderInfo) {
        mallOrderInfo.setUpdateDate(new Date());
        mallOrderInfoService.updateById(mallOrderInfo);
    }


    public List<MallOrderItem> getUnitChange(MallSkuSpec spec, BigDecimal goodsAmount) {
        List<MallOrderItem> items = new ArrayList<>();
        convertItem(items, spec, goodsAmount.intValue());
        log.info("---转换后的商品结构---:{}", items);
        return items;
    }

    private void convertItem(List<MallOrderItem> items, MallSkuSpec spec, int originAmount) {
        //判断是否最小单位 是则向上换算
        if ("1".equals(spec.getIsSmallUnit())) {
            //是否支持单位换算
            if ("1".equals(spec.getIsCanConversion())) {
                //获取比例
                int amount = convert(spec, originAmount, items);
                if (amount == 0) {
                    return;
                }
                MallSkuSpec specx = getSpec(spec.getStructureSku());
                convertItem(items, specx, amount);
            } else { //不支持单位换算
                takeItems(items, spec.getSkuCode(), originAmount);
            }
        } else {
            //非最小单位 - 支持单位换算
            if ("1".equals(spec.getIsCanConversion())) {
                //获取比例
                int amount = convert(spec, originAmount, items);
                if (amount == 0) {
                    return;
                }
                takeItems(items, spec.getStructureSku(), amount);
            } else { //不支持单位换算
                takeItems(items, spec.getSkuCode(), originAmount);
            }
        }

    }

    private void takeItems(List<MallOrderItem> items, String skuCode, int amount) {
        MallOrderItem item = new MallOrderItem();
        item.setSkuCode(skuCode);
        item.setAmount(BigDecimal.valueOf(amount));
        items.add(item);
    }

    private int convert(MallSkuSpec spec, int originAmount, List<MallOrderItem> items) {
        String ratio = spec.getConversionRatio();
        if (ObjectUtils.isNullOrEmpty(ratio)) {
            takeItems(items, spec.getSkuCode(), originAmount);
            return 0;
        }
        String[] ratioArr = ratio.split(":");
        int first = Integer.parseInt(ratioArr[0]);
        int second = Integer.parseInt(ratioArr[1]);
        int amount = originAmount / first * second;
        if (originAmount == amount) {
            takeItems(items, spec.getSkuCode(), originAmount);
            return 0;
        }
        if (originAmount - (amount * first) != 0) {
            MallOrderItem item = new MallOrderItem();
            item.setSkuCode(spec.getSkuCode());
            item.setAmount(BigDecimal.valueOf(originAmount - (amount * first)));
            items.add(item);
        }
        return amount;
    }

    private MallSkuSpec getSpec(String skuCode) {
        return mallOrderInfoMapper.getMallSkuSpec(skuCode, "1").get(0);
    }

    public List<MallOrderItem> giveThisItem(List<MallOrderItem> list) {
        List<MallOrderItem> gifts = new ArrayList<>();
        Map<String, List<MallSku>> giftsGoods = getGiftsGoods();
        list.forEach(p -> {
            giftsGoods.forEach((k, v) -> {
                if (p.getSkuCode().trim().equals(k)) {
                    v.forEach(i -> {
                        MallOrderItem item = new MallOrderItem();
                        item.setSkuCode(i.getSkuCode());
                        item.setAmount(p.getAmount().multiply(i.getStock()));
                        gifts.add(item);
                    });
                    return;
                }
            });
        });
        return gifts;
    }

    public static Map<String, List<MallSku>> getGiftsGoods() {
        Map<String, List<MallSku>> map = new HashMap<>();
        map.put(C035_H, Arrays.asList(MallSku.builder().skuCode("P002").stock(BigDecimal.valueOf(1)).build()));
        map.put(C035_X, Arrays.asList(MallSku.builder().skuCode("P002").stock(BigDecimal.valueOf(40)).build()));
        map.put(C034_H, Arrays.asList(MallSku.builder().skuCode("P001").stock(BigDecimal.valueOf(1)).build()));
        map.put(C034_X, Arrays.asList(MallSku.builder().skuCode("P001-X").stock(BigDecimal.valueOf(1)).build()));
        map.put(C033_H, Arrays.asList(MallSku.builder().skuCode("P001").stock(BigDecimal.valueOf(1)).build()));
        map.put(C033_X, Arrays.asList(MallSku.builder().skuCode("P001-X").stock(BigDecimal.valueOf(1)).build()));
        map.put("30290030", Arrays.asList(MallSku.builder().skuCode("40000061").stock(BigDecimal.valueOf(2)).build()));
        map.put("30290020", Arrays.asList(MallSku.builder().skuCode("40000062").stock(BigDecimal.valueOf(1)).build(),
                MallSku.builder().skuCode("40000061").stock(BigDecimal.valueOf(1)).build()));
        map.put("30030040", Arrays.asList(MallSku.builder().skuCode("C036-H").stock(BigDecimal.valueOf(1)).build()));
        map.put("30030050", Arrays.asList(MallSku.builder().skuCode("C036-H").stock(BigDecimal.valueOf(1)).build()));
        return map;
    }

    private boolean checkC036Z(List<MallOrderItem> list) {
        for (MallOrderItem item : list) {
            if ("C036-Z".equals(item.getSkuCode().trim())) {
                if (item.getAmount().abs().compareTo(BigDecimal.valueOf(10)) >= 0) {
                    return true;
                }
            }
            if ("C036-X".equals(item.getSkuCode().trim())) {
                if (item.getAmount().abs().compareTo(BigDecimal.valueOf(1)) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void buyOneGetOneFree(Date orderPayDate, List<MallOrderItem> itemList) {
        LocalDateTime payDate = orderPayDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime start = LocalDateTime.of(2020, 3, 20, 0, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2020, 4, 21, 0, 0, 0, 0);
        if (payDate.isAfter(start) && payDate.isBefore(end)) {
            for (MallOrderItem item : itemList) {
                if (Arrays.asList("123456789", "30030070", "30030080").contains(item.getSkuCode())) {
                    item.setAmount(item.getAmount().multiply(BigDecimal.valueOf(2)));
                }
            }
        }

    }
}
