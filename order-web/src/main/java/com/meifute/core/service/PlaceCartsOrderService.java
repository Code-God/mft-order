package com.meifute.core.service;

import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.MallUserAccountDto;
import com.meifute.core.dto.OrderInfoDto;
import com.meifute.core.dto.SkuSpecAmount;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.PriceUtil;
import com.meifute.core.util.JsonUtils;
import com.meifute.core.vo.ItemVo;
import com.meifute.core.vo.PreOrderFromCartParam;
import com.meifute.core.vo.SkuAndAmountParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.meifute.core.mmall.common.enums.MallOrderTypeEnum.ORDER_TYPE_001;
import static com.meifute.core.mmall.common.enums.MallReviewEnum.IS_SUB_STOCK_001;
import static com.meifute.core.mmall.common.enums.MallStatusEnum.BELONGS_CODE_000;
import static com.meifute.core.mmall.common.enums.MallStatusEnum.BELONGS_CODE_001;

/**
 * @Auther: wxb
 * @Date: 2018/10/16 14:22
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class PlaceCartsOrderService {

    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private CommonOrderService commonOrderService;
    @Autowired
    private ExpressCompanyService expressCompanyService;

    /**
     * 购物车提交订单接口
     *
     * @param param
     * @return
     */
    public OrderInfoDto reduceStockAndOrder(PreOrderFromCartParam param, MallUser user) {
        //代理等级
        int agentLevel = Integer.parseInt(user.getRoleId());
        //普通用户没有下单的权力
        MallPreconditions.checkToError(agentLevel == 0, OrderRespCode.ORDINARY_USER_NOTTOBUY);
        // 123级代理校验支付凭证
        if (agentLevel < 4) {
            // 校验订单类型存入云库存，123级支付凭证不能为空
            if (ORDER_TYPE_001.getCode().equals(param.getOrderType())) {
                //1 支付凭证图片不能为空
                MallPreconditions.checkNotNull(param.getProofPath(), OrderRespCode.PAY_VOUCHER_ISNOT);
            }
        }
        //1 获取购物车信息
        List<ItemVo> itemVos = BeanMapper.mapList(param.getMallCarts(), ItemVo.class);
        log.info("call itemVos result:{}", itemVos);
        //2 校验各个商品的库存
        commonOrderService.checkActualStock(itemVos, agentLevel, param.getOrderType(), MallStatusEnum.BELONGS_CODE_000.getCode());
        //4 下单操作
        return orderHandle(param, user, agentLevel, itemVos);
    }

    /**
     * 下单步骤
     *
     * @param param
     * @param user
     * @return
     */
    private OrderInfoDto orderHandle(PreOrderFromCartParam param, MallUser user, int agentLevel, List<ItemVo> itemVos) {
        // 1.生成订单信息
        OrderInfoDto orderInfo = createOrderInfo(itemVos, param, user, agentLevel);
        // 2.todo  生成支付信息 不要了
        commonOrderService.createPayInfo(itemVos, orderInfo.getOrderInfoList().get(0), user.getId());
        // 3.生成商品记录信息
        commonOrderService.createOrderItemInfo(itemVos, orderInfo.getOrderInfoList(), agentLevel);
        // 4.更新购物车为已结算
        List<MallCart> list = param.getMallCarts();
        list.forEach(p -> {
            MallCart mallCart = MallCart.builder().status(MallStatusEnum.IS_SETTLEMENT_001.getCode()).id(p.getId()).build();
            agentFeign.updateCartById(mallCart);
        });
        //5 预减库存
        commonOrderService.preSubtractStock(itemVos, agentLevel, MallStatusEnum.BELONGS_CODE_000.getCode());
        return orderInfo;
    }

    /**
     * 生成订单信息 购物车里的无积分支付商品
     * 1.如果多商户，积分和产品，则生成多条订单
     *
     * @param param
     * @return
     */
    private OrderInfoDto createOrderInfo(List<ItemVo> itemVos, PreOrderFromCartParam param, MallUser mallUser, int agentLevel) {
        //购物车信息
        List<MallCart> result = param.getMallCarts();
        BigDecimal proAmt = BigDecimal.ZERO; //产品商城
        BigDecimal redAmt = BigDecimal.ZERO; //积分商城
        BigDecimal isSubStockAmt = BigDecimal.ZERO; // 123级可减产品商城的库存金额

        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        List<SkuAndAmountParam> listSku = new ArrayList<>();
        // 计算总额
        for (MallCart mallCart : result) {
            MallSku sku = itemFeign.getSkuByCode(mallCart.getSkuCode());
            // 产品商城
            if (BELONGS_CODE_000.getCode().equals(sku.getBelongsCode())) {
                // 直接发货收取运费
                if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(param.getOrderType())) {
                    SkuAndAmountParam skuParam = new SkuAndAmountParam();
                    skuParam.setSkuCode(sku.getSkuCode());
                    skuParam.setAmount(String.valueOf(mallCart.getNumber()));
                    listSku.add(skuParam);
                }
                BigDecimal priceAmt = PriceUtil.getPrice(sku.getRetailPrice(), agentLevel);
                priceAmt = priceAmt.multiply(mallCart.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP);
                proAmt = proAmt.add(priceAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
                // 123级需要支付的产品金额
                if (IS_SUB_STOCK_001.getCode().equals(sku.getIsSubStock())) {
                    isSubStockAmt.add(priceAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
            // 积分商城
            if (BELONGS_CODE_001.getCode().equals(sku.getBelongsCode())) {
                BigDecimal priceAmt = new BigDecimal(sku.getRetailPrice());
                priceAmt = priceAmt.multiply(mallCart.getNumber()).setScale(2, BigDecimal.ROUND_HALF_UP);
                redAmt = redAmt.add(priceAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }
        skuSpecAmount.setAmountParams(listSku);

        //运费
        BigDecimal freight = BigDecimal.ZERO;
        //地址
        MallUserAddress address = null;
        //直接发货需要收取运费 （只有总代才可以直发）
        if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(param.getOrderType())) {
            address = userFeign.queryAddressById(param.getAddrId());
            // 检验地址是否有效
            MallPreconditions.checkNotNull(address, OrderRespCode.INVALID_ADDRESS);
            skuSpecAmount.setProvince(address.getProvice());
            skuSpecAmount.setCity(address.getArea().split(" ")[1]);
            skuSpecAmount.setLogisticsType(param.getLogisticsType());
            freight = itemFeign.getPostFee(skuSpecAmount);
        }
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);

        //记录当前等级
        mallOrderInfo.setCurrentLevel(mallUser.getRoleId());
        //物流方式
        String expressType = screenExpress(itemVos);
//        String expressType = getExpressType(result, param.getLogisticsType());
//        String expressType = param.getLogisticsType();
        mallOrderInfo.setLogisticsMode(expressType);

        //todo 京东计数
//        setJdNowCount(mallOrderInfo, param.getLogisticsType(), expressType, param.getOrderType());
        mallOrderInfo.setLogisticsType(param.getLogisticsType());
//        if ("1".equals(param.getLogisticsType())) {
//            String count = RedisUtil.get("sf:order_count");
//            RedisUtil.incr("sf:order_count", 1);
//            LocalDateTime now = LocalDateTime.now();
//            LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
//            long t = Duration.between(now, end).toMillis();
//            if ("1".equals(count)) {
//                RedisUtil.expire("sf:order_count", t/10000);
//            }else if (StringUtils.isEmpty(count)) {
//                RedisUtil.incr("sf:order_count", 1);
//                RedisUtil.expire("sf:order_count", t/10000);
//                count = RedisUtil.get("sf:order_count");
//            }
//            if (Integer.parseInt(count) > Integer.parseInt(RedisUtil.get("sf:order_re_count"))) {
//                throw new MallException("00998", new Object[]{"今日顺丰快递单量已满，请更换物流方式"});
//            }
//            mallOrderInfo.setLogisticsMode("1");
//        }

        //todo 王正鹏加的临时的代码
//        if (address != null) {
//            if (address.getProvice().trim().startsWith("湖北")) {
//                mallOrderInfo.setLogisticsMode("2");
//            }
//        }

        //生成多条订单
        List<MallOrderInfo> list = new ArrayList<>();
        if (proAmt.compareTo(BigDecimal.ZERO) > 0) { //产品
            MallOrderInfo order = commonOrderService.createOrderInfo(mallOrderInfo, proAmt, BELONGS_CODE_000.getCode(), "0", address, freight);
            list.add(order);
        }
        if (redAmt.compareTo(BigDecimal.ZERO) > 0) { //积分 金额产品
            MallOrderInfo order = commonOrderService.createOrderInfo(mallOrderInfo, redAmt, BELONGS_CODE_001.getCode(), "0", address, freight);
            list.add(order);
        }

        //封装返回值
        OrderInfoDto orderInfoDto = new OrderInfoDto();
        orderInfoDto.setOrderInfoList(list);
        MallUserAccountDto account = userFeign.accountInfo(mallUser.getId());
        orderInfoDto.setBalance(account.getAmt()); //账户余额
        orderInfoDto.setCredit(account.getCredit()); //账户积分
        orderInfoDto.setPayCredit(BigDecimal.ZERO);
        orderInfoDto.setPostFee(freight);
        BigDecimal paymentAmt = BigDecimal.ZERO; //实际支付总金额
        BigDecimal originAmt = BigDecimal.ZERO; //原始支付总金额
        for (MallOrderInfo p : list) {
            paymentAmt = paymentAmt.add(p.getPaymentAmt());
            originAmt = originAmt.add(p.getOriginAmt());
        }
        orderInfoDto.setPaymentAmt(new BigDecimal(new DecimalFormat("#.00").format(paymentAmt)).setScale(2, BigDecimal.ROUND_HALF_UP));
        orderInfoDto.setOriginAmt(originAmt);
        return orderInfoDto;
    }

    public String screenExpress(List<ItemVo> itemVos) {
        List<String> express = new ArrayList<>();
        for (ItemVo item : itemVos) {
            MallSku sku = itemFeign.getSkuByCode(item.getSkuCode());
            express.add(sku.getLogisticsMode());
        }
//        List<String> collect = express.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        return expressCompanyService.getHighestExpressCode(express);
//        return collect.get(0);
    }

    public String getExpressType(List<MallCart> skuList, String logisticsType) {
        if("1".equals(RedisUtil.get("jd_ex:on_off"))) {
            return "0";
        }
        String items = RedisUtil.get("check_blend_order_item"); //指定商品
        List<String> blendItems = null;
        if (items != null) {
            blendItems = JsonUtils.jsonToList(items, String.class);
        }
        if ("1".equals(logisticsType)) {
            if (blendItems != null) {
                List<String> finalBlendItems = blendItems;
                List<MallCart> sku1 = skuList.stream().filter(p -> finalBlendItems.contains(p.getSkuCode())).collect(Collectors.toList());
                List<MallCart> sku2 = skuList.stream().filter(p -> !finalBlendItems.contains(p.getSkuCode())).collect(Collectors.toList());
                if (sku1.size() > 0 && sku2.size() == 0) { //新亦源顺丰
                    return "1";
                }
                if (sku2.size() > 0) { //仓库发顺丰
                    return "2";
                }
            }
            return "1";
        } else {
            if (blendItems != null && "1".equals(RedisUtil.get("jd_ex:only_push_ck"))) {
                List<String> finalBlendItems = blendItems;
                List<MallCart> sku1 = skuList.stream().filter(p -> finalBlendItems.contains(p.getSkuCode())).collect(Collectors.toList());
                if (sku1.size() > 0) {
                    return "2";
                }
            }
            String nowCount = RedisUtil.get("jd_ex:order_now_count"); //当前数量
            String maxCount = RedisUtil.get("jd_ex:order_count"); //上限数量
            int now = nowCount == null ? 0 : Integer.parseInt(nowCount);
            int max = maxCount == null ? 0 : Integer.parseInt(maxCount);
            if (now >= max) { //仓库发京东
                return "2";
            } else { //直接京东
                return "0";
            }
        }
    }

    public void setJdNowCount(MallOrderInfo mallOrderInfo, String logisticsType, String expressType, String orderType) {
        if("1".equals(RedisUtil.get("jd_ex:on_off"))) {
            return;
        }
        if ("0".equals(logisticsType) && "0".equals(expressType) && Arrays.asList("0", "2").contains(orderType)) {
            String count = RedisUtil.get("jd_ex:order_now_count");
            RedisUtil.incr("jd_ex:order_now_count", 1);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
            long t = Duration.between(now, end).toMillis();
            if ("1".equals(count)) {
                RedisUtil.expire("jd_ex:order_now_count", t / 10000);
            } else if (StringUtils.isEmpty(count)) {
                RedisUtil.expire("jd_ex:order_now_count", t / 10000);
            }
        }
        mallOrderInfo.setLogisticsType(logisticsType);
    }

}
