package com.meifute.core.service;

import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.NotifyFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallOrderItemMapper;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.*;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.jpush.JpushUtil;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.PriceUtil;
import com.meifute.core.util.DESUtil;
import com.meifute.core.util.ItemCacheUtils;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.ItemVo;
import com.meifute.core.vo.OrderInfoMessage;
import com.meifute.core.vo.notify.QueueParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.meifute.core.mmall.common.enums.MallOrderTypeEnum.ORDER_TYPE_000;
import static com.meifute.core.mmall.common.enums.MallStatusEnum.*;

/**
 * @Auther: wxb
 * @Date: 2018/10/16 14:26
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class CommonOrderService {

    @Autowired
    private MallOrderItemMapper orderItemMapper;
    @Autowired
    private MallOrderInfoMapper orderInfoMapper;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private NotifyFeign notifyFeign;
    @Value("${sms_market_url}")
    private String sms_market_url;


    public MallOrderInfo insertOrderInfo(MallOrderInfo mallOrderInfo) {
        if (ObjectUtils.isNullOrEmpty(mallOrderInfo.getOrderId())) {
            mallOrderInfo.setOrderId(IDUtils.genOrderId());
        }
        mallOrderInfo.setCreateDate(new Date());
        mallOrderInfo.setUpdateDate(new Date());
        MallBranchOffice branchCompany = agentFeign.getBranchCompany(mallOrderInfo.getMallUserId());
        if (branchCompany != null) {
            mallOrderInfo.setCompanyId(branchCompany.getId());
        }
        Integer result = orderInfoMapper.insert(mallOrderInfo);
        if (result == 0) {
            throw new MallException(OrderRespCode.ADD_ORDER_FAIL);
        }
        return orderInfoMapper.selectById(mallOrderInfo.getOrderId());
    }

    public MallOrderInfo insertOrderInfoToThis(MallOrderInfo mallOrderInfo) {
        if (ObjectUtils.isNullOrEmpty(mallOrderInfo.getOrderId())) {
            mallOrderInfo.setOrderId(IDUtils.genOrderId());
        }
        mallOrderInfo.setCreateDate(new Date());
        mallOrderInfo.setUpdateDate(new Date());
        MallBranchOffice branchCompany = agentFeign.getBranchCompany(mallOrderInfo.getMallUserId());
        if (branchCompany != null) {
            mallOrderInfo.setCompanyId(branchCompany.getId());
        }
        Integer result = orderInfoMapper.insert(mallOrderInfo);
        if (result == 0) {
            throw new MallException(OrderRespCode.ADD_ORDER_FAIL);
        }
        return mallOrderInfo;
    }

    /**
     * 查询购物车信息，若没有则抛重复订单提交，仅限提交订单使用
     *
     * @param mallCart
     * @return
     */
    public MallCart potCartInfoToOrder(MallCart mallCart) {
        MallCart cart = agentFeign.getMallCartByParam(mallCart);
        MallPreconditions.checkNotNull(cart, OrderRespCode.REPEAT_ORDER);
        return cart;
    }


    /**
     * 创建下单商品数据
     *
     * @param itemVos
     */
    public void createOrderItemInfo(List<ItemVo> itemVos, List<MallOrderInfo> mallOrderInfo, int agentLevel) {
        List<MallOrderItem> itemList = new ArrayList<>();
        //多条商品
        mallOrderInfo.forEach(o -> {
            itemVos.forEach(p -> {
                MallSku sku = ItemCacheUtils.getSkuByCode(p.getSkuCode());
                MallOrderItem mallOrderItem = new MallOrderItem();
                BeanMapper.copy(p, mallOrderItem);
                mallOrderItem.setId(IDUtils.genId());
                mallOrderItem.setCartId(p.getId());
                mallOrderItem.setAmount(p.getNumber());
                mallOrderItem.setOrderId(o.getOrderId());
                mallOrderItem.setCurrency(o.getCurrency());
                // 1 产品商城
                if (MallStatusEnum.BELONGS_CODE_000.getCode().equals(sku.getBelongsCode())) {
                    BigDecimal priceAmt = PriceUtil.getPrice(sku.getRetailPrice(), agentLevel);
                    mallOrderItem.setPrice(priceAmt);
                }
                //2 积分商城
                if (!MallStatusEnum.BELONGS_CODE_000.getCode().equals(sku.getBelongsCode())) {
                    //a 积分币种
                    if (MallStatusEnum.CURRENCY_CODE_001.getCode().equals(sku.getCurrency())) {
                        BigDecimal priceAmt = new BigDecimal(sku.getRetailPrice());
                        mallOrderItem.setCredit(priceAmt);
                    } else {//b 非积分币种
                        BigDecimal priceAmt = new BigDecimal(sku.getRetailPrice());
                        mallOrderItem.setPrice(priceAmt);
                    }
                }
                mallOrderItem.setUnit(sku.getUnit());
                mallOrderItem.setSpec(sku.getSpec());
                mallOrderItem.setStatus(MallOrderStatusEnum.ORDER_STATUS_000.getCode());
                mallOrderItem.setCreateDate(new Date());
                orderItemMapper.insert(mallOrderItem);
                itemList.add(mallOrderItem);
            });
        });
        //1级必须买大余10
        if ("0".equals(mallOrderInfo.get(0).getBelongsCode()) && agentLevel == 1) {
            List<MallOrderItem> list = itemList.stream().filter(p ->
                    !p.getSkuCode().equals("P001") && !p.getSkuCode().equals("P002") && !"支".equals(p.getUnit()))
                    .collect(Collectors.toList());
            BigDecimal total = list.stream().map(MallOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.abs().compareTo(BigDecimal.valueOf(10)) < 0) {
                throw new MallException("020030");
            }
        }
    }

    /**
     * 添加支付单信息
     *
     * @param itemVos
     * @param p
     */
    public void createPayInfo(List<ItemVo> itemVos, MallOrderInfo p, String userId) {
        MallPayInfo mallPayInfo = new MallPayInfo();
        mallPayInfo.setTotalAmt(p.getPaymentAmt());
        mallPayInfo.setCredit(p.getCredit());
        mallPayInfo.setMallUserId(userId);
        mallPayInfo.setOrderId(p.getOrderId());
        mallPayInfo.setCurrency(p.getCurrency());
        mallPayInfo.setPayStatus(MallPayStatusEnum.PAY_STATUS_000.getCode());
        mallPayInfo.setStatus(MallStatusEnum.STATUS_CODE_000.getCode());
        BigDecimal total = BigDecimal.ZERO;
        MallSku sku = itemFeign.getSkuByParam(MallSku.builder().skuCode(itemVos.get(0).getSkuCode()).itemId(itemVos.get(0).getItemId()).build());
        for (ItemVo itemVo : itemVos) {
            total = total.add(itemVo.getNumber());
        }
        mallPayInfo.setTitle(sku.getTitle() + "等" + total + "件商品");
        mallPayInfo.setIsDel(MallStatusEnum.IS_DEL_CODE_001.getCode());
        payFeign.insertPayInfo(mallPayInfo);
    }


    /**
     * 创建订单信息
     *
     * @return
     */
    public MallOrderInfo createOrderInfo(MallOrderInfo mallOrderInfo, BigDecimal amt, String belongsCode, String currency, MallUserAddress addr, BigDecimal freight) {
        MallUser mallUser = UserUtils.getCurrentUser();
        //查询上级信息
        MallAgent leaderAgent = agentFeign.queryLeaderAgentInfo(mallUser.getId());
        if (leaderAgent != null) {
            mallOrderInfo.setLeaderId(leaderAgent.getUserId());
        }
        // 代理等级
        int agentLevel = UserUtils.getAgentLevel(mallUser.getId());
        //地址
        String address = null;
        //描叙
        String orderDesc = null;
        if (ObjectUtils.isNotNullAndEmpty(addr)) {
            address = addr.getArea() + addr.getFullAddress();
        }

        //1 订单类型->存入云库存 只有产品商城的东西可以存入云库存
        if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType())) {
            // 123级代理
            if (agentLevel < 4) {
//                mallOrderInfo.setPaymentAmt(isSubStockAmt);
                mallOrderInfo.setPaymentAmt(BigDecimal.ZERO);
                mallOrderInfo.setOriginAmt(amt);
                mallOrderInfo.setSummaryAmt(amt);
            } else { //总代
                mallOrderInfo.setPaymentAmt(amt);
                mallOrderInfo.setOriginAmt(amt);
                mallOrderInfo.setSummaryAmt(amt);
            }
            orderDesc = MallOrderTypeEnum.ORDER_TYPE_001.getDesc();
        }

        //2 订单类型->直接发货
        if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())) {

            //1 产品商城 只有总代才可以在产品商城发货
            if (MallStatusEnum.BELONGS_CODE_000.getCode().equals(belongsCode)) {
                //订单封参
                mallOrderInfo.setPaymentAmt(amt.add(freight));// 邮费加商品费
                mallOrderInfo.setOriginAmt(amt);// 商品费
                mallOrderInfo.setPostFeeAmt(freight);
                mallOrderInfo.setSummaryAmt(amt);
            }

            //省市区-街道
            mallOrderInfo.setProvincialUrbanArea(addr.getArea());
            mallOrderInfo.setStreet(addr.getFullAddress());

            //2 积分商城 包邮
            if (!MallStatusEnum.BELONGS_CODE_000.getCode().equals(belongsCode)) {
                //a 积分币种
                if (MallStatusEnum.CURRENCY_CODE_001.getCode().equals(currency)) {
                    mallOrderInfo.setPaymentAmt(BigDecimal.ZERO);
                    mallOrderInfo.setOriginAmt(BigDecimal.ZERO);
                    mallOrderInfo.setCredit(amt);
                } else { //b 金额币种
                    mallOrderInfo.setPaymentAmt(amt); //金额
                    mallOrderInfo.setOriginAmt(amt);
                    mallOrderInfo.setSummaryAmt(amt);
                }
            }
        }
        mallOrderInfo.setMallUserId(mallUser.getId());
        mallOrderInfo.setCurrency(currency);
        mallOrderInfo.setBelongsCode(belongsCode);
        mallOrderInfo.setOrderDescribe(orderDesc);
        mallOrderInfo.setAddrId(address);
        if (ObjectUtils.isNotNullAndEmpty(addr)) {
            mallOrderInfo.setAddrName(addr.getName());
            mallOrderInfo.setAddrPhone(addr.getPhone());
        }
        mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_000.getCode());
        mallOrderInfo.setOrderType(mallOrderInfo.getOrderType());
        mallOrderInfo.setIsDel(MallStatusEnum.IS_DEL_CODE_001.getCode());
        Date now = new Date();
//        Date afterDate = new Date(now.getTime() + 30 * 60 * 1000);
        Date afterDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        mallOrderInfo.setPayEndDate(afterDate);//支付截止时间30分钟后
        return this.insertOrderInfoToThis(mallOrderInfo);
    }


    /**
     * 检查库存
     *
     * @param itemVos
     * @param agentLevel
     * @param orderType
     */
    public void checkActualStock(List<ItemVo> itemVos, int agentLevel, String orderType, String belongsCode) {
        itemVos.forEach(item -> {
            if ("30190010".equals(item.getSkuCode())) {
                // 校验123级不能在产品商城直接发货
                if (agentLevel < 4) {
                    if (ORDER_TYPE_000.getCode().equals(orderType) && BELONGS_CODE_000.getCode().equals(belongsCode)) {
                        throw new MallException(OrderRespCode.DONT_DELIVER_GOODS);
                    }
                }
                String key = RedisUtil.getItemStockKey(item.getItemId(), item.getSkuCode());
                String stock = RedisUtil.get(key);
                // 获取库存
                stock = ObjectUtils.isNullOrEmpty(stock) ? "0" : stock;
                BigDecimal stockNum = new BigDecimal(stock);

                // 总代以上级别，积分商城，包含需要减库存的sku,都是需要减库存的
                if (agentLevel >= 4 || BELONGS_CODE_001.getCode().equals(belongsCode)) {
                    // c.校验库存
                    if (stockNum.compareTo(item.getNumber().abs()) < 0) {
                        MallSku sku = itemFeign.getSkuByCode(item.getSkuCode());
                        throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{sku.getTitle()});
                    }
                }
            }

        });
    }

    /**
     * 预减库存
     *
     * @param itemVos
     * @param agentLevel
     */
    public void preSubtractStock(List<ItemVo> itemVos, int agentLevel, String belongsCode) {
        List<BigDecimal> lackSock = new ArrayList<>();
        itemVos.forEach(f -> {
            if ("30190010".equals(f.getSkuCode())) {
                String key = RedisUtil.getItemStockKey(f.getItemId(), f.getSkuCode());
                // b.需要减少的数量
                BigDecimal amount = f.getNumber().abs();
                // 总代以上或积分商城减库存
                if (agentLevel >= 4 || BELONGS_CODE_001.getCode().equals(belongsCode)) {
                    // c.减少库存
                    long value = RedisUtil.decr(key, amount.longValue());
                    lackSock.add(amount);
                    // d.库存不足
                    if (value < 0) {
                        //库存不足，需要增加刚刚减去的库存
                        lackSock.forEach(p -> {
                            RedisUtil.incr(key, p.longValue());
                        });
                        // 获取商品详细信息
                        MallSku sku = itemFeign.getSkuByCode(f.getSkuCode());
                        throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{sku.getTitle()});
                    }
                }
            }
        });
    }

    public void sendOrderDelayMQ(String orderId) {
        QueueParam queueParam = new QueueParam();
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        orderInfoMessage.setOrderOrigin(0);
        orderInfoMessage.setOrderId(orderId);
        queueParam.setMessage(JSONUtil.obj2json(orderInfoMessage));
        queueParam.setTimes(24 * 60 * 60 * 1000);
        notifyFeign.sendOrderDelayMQ(queueParam);
    }


    public void sendExpressMQ(String orderId) {
        QueueParam queueParam = new QueueParam();
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        orderInfoMessage.setOrderOrigin(0);
        orderInfoMessage.setOrderId(orderId);
        queueParam.setMessage(JSONUtil.obj2json(orderInfoMessage));
        queueParam.setTimes(60 * 60 * 1000);
        notifyFeign.sendExpressMQ(queueParam);
    }

    /**
     * 添加云库存并添加日志
     *
     * @param mallUserId
     * @param mallOrderItem
     */
    public void addCloudStock(String mallUserId, MallOrderItem mallOrderItem, String orderId) {
        MallCloudStock cloudStock = agentFeign.getCloudStock(MallCloudStock.builder().mallUserId(mallUserId).itemId(mallOrderItem.getItemId()).skuCode(mallOrderItem.getSkuCode()).build());
        MallSku sku = itemFeign.getSkuByParam(MallSku.builder().itemId(mallOrderItem.getItemId()).skuCode(mallOrderItem.getSkuCode()).build());
        if (ObjectUtils.isNullOrEmpty(cloudStock)) {
            log.info("=========>>>>>>进来add<<<<================");
            // 创建新库存
            MallCloudStock cloudParam = MallCloudStock.builder()
                    .mallUserId(mallUserId)
                    .itemId(mallOrderItem.getItemId())
                    .skuCode(mallOrderItem.getSkuCode())
                    .stock(mallOrderItem.getAmount())
                    .unit(mallOrderItem.getUnit())
                    .spec(mallOrderItem.getSpec())
                    .isChange(sku.getIsChange())
                    .build();
            MallCloudStock result = agentFeign.addCloudStock(cloudParam);
            // 添加云库存记录 云库存单号 用户信息 商品 交易前数量	交易数量	交易后数量 日志时间
            MallCloudStockLog mallCloudStockLog = MallCloudStockLog.builder()
                    .cloudStockId(result.getId())
                    .skuCode(mallOrderItem.getSkuCode())
                    .itemId(mallOrderItem.getItemId())
                    .payStock(mallOrderItem.getAmount())
                    .relationId(orderId)
                    .mallUserId(mallUserId)
                    .payAgoStock(BigDecimal.ZERO)
                    .payAfterStock(mallOrderItem.getAmount())
                    .build();
            agentFeign.addCloudStockLog(mallCloudStockLog);
        } else {
            log.info("=========>>>>>>>没进来<<<<update================");
            // 更新库存
            agentFeign.updateCloudStockByLock(MallCloudStock.builder().id(cloudStock.getId()).stock(mallOrderItem.getAmount()).build());
            // 添加云库存记录 库存单号 用户信息 商品 交易前数量	交易数量	交易后数量 日志时间
            MallCloudStockLog mallCloudStockLog = MallCloudStockLog.builder()
                    .cloudStockId(cloudStock.getId())
                    .skuCode(mallOrderItem.getSkuCode())
                    .itemId(mallOrderItem.getItemId())
                    .payStock(mallOrderItem.getAmount())
                    .relationId(orderId)
                    .mallUserId(mallUserId)
                    .payAgoStock(cloudStock.getStock())
                    .payAfterStock(cloudStock.getStock().add(mallOrderItem.getAmount()).setScale(2, BigDecimal.ROUND_HALF_UP))
                    .build();
            agentFeign.addCloudStockLog(mallCloudStockLog);
        }
    }

    //任意一款口腔泡沫18支上限下单限制
    public void set18RecoveryItems(String userId, String orderType, List<MallOrderItem> itemList) {
        BigDecimal total = itemList.stream().filter(p -> Arrays.asList("C039-Z", "C038-Z").contains(p.getSkuCode()))
                .map(MallOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total2 = itemList.stream().filter(p -> Arrays.asList("C039-H", "C038-H").contains(p.getSkuCode()))
                .map(MallOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String s = RedisUtil.get("ItemTo18Limitations:" + userId);
        int i = 0;
        if (s != null) {
            i = Integer.parseInt(s);
        }
        int t = total2 == null ? 0 : total2.abs().intValue();
        if (total != null) {
            int num = total.abs().multiply(BigDecimal.valueOf(3)).intValue() + t;
            if (MallStatusEnum.ORDER_TYPE_000.getCode().equals(orderType) || ORDER_TYPE_002.getCode().equals(orderType)) {
                if (i - num < 0) {
                    set18Item(userId, 0);
                }
                if (i - num >= 0) {
                    set18Item(userId, i - num);
                }
            }
        }
    }

    private void set18Item(String userId, int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date zero = calendar.getTime();
        long v = zero.getTime() - new Date().getTime();
        int t = Integer.parseInt(String.valueOf(v));
        RedisUtil.set("ItemTo18Limitations:" + userId, JSONUtil.obj2json(num), t / 1000);
    }

    @Async
    public void sendDeliverySmsMsg(String pid, String phone, String usrName, String expressCode) {
        JpushUtil.sendNoticeMarketMsg3(phone, sms_market_url, DESUtil.encrypt(pid), DESUtil.encrypt(phone), usrName, expressCode);
    }

    @Async
    public void sendDeliverySmsMsgEvaluate(String phone, String orderId) {
        JpushUtil.sendNoticeMarketMsg4(phone, sms_market_url, orderId);
    }


    public String geiAgentPriceLabel(String[] retailPrice, String price) {
        String priceLabel = "";
        if (retailPrice[0].equals(price)) {
            priceLabel = "零售价";
        } else if (retailPrice[1].equals(price)) {
            priceLabel = "一级价";
        } else if (retailPrice[2].equals(price)) {
            priceLabel = "二级价";
        } else if (retailPrice[3].equals(price)) {
            priceLabel = "vip价";
        } else if (retailPrice[4].equals(price)) {
            priceLabel = "总代价";
        } else if (retailPrice[1].equals(price)) {
            priceLabel = "总代价";
        }
        return priceLabel;
    }
}
