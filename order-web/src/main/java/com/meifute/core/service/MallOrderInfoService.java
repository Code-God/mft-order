package com.meifute.core.service;


import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.SqlHelper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.*;
import com.meifute.core.dto.report.InputOutputItemReportDTO;
import com.meifute.core.dto.report.MonthlyOrderReportResponseDTO;
import com.meifute.core.dto.report.OrderReportRequest;
import com.meifute.core.dto.report.OrderReportResponseDTO;
import com.meifute.core.entity.*;
import com.meifute.core.entity.MallRealnameAuth;
import com.meifute.core.entity.activity.MallAcOrder;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.*;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.date.DateUtils;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.*;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.exception.errorcode.RespCode;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.*;
import com.meifute.core.model.JDImportModel;
import com.meifute.core.model.JDPushExcelModel;
import com.meifute.core.test.QimenClient;
import com.meifute.core.util.*;
import com.meifute.core.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.meifute.core.mmall.common.enums.MallOrderTypeEnum.ORDER_TYPE_001;
import static com.meifute.core.mmall.common.enums.MallStatusEnum.*;

/**
 * <p>
 * 订单中心 - 服务类
 * </p>
 *
 * @author wuxb
 * @since 2018-09-25
 */
@Service
@Slf4j
public class MallOrderInfoService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {

    @Value("${spring.profiles}")
    private String profiles;

    @Value("${icbc_flag}")
    private boolean icbcFlag;

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private PlaceCartsOrderService placeCartsOrderService;
    @Autowired
    private PlaceGoodsOrderService placeGoodsOrderService;
    @Autowired
    private CommonOrderService commonOrderService;
    @Autowired
    private MallOrderVerifyService mallOrderVerifyService;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private MallTransferGoodsMapper mallTransferGoodsMapper;
    @Autowired
    private MallOrderItemMapper orderItemMapper;
    @Autowired
    private MallOrderItemService mallOrderItemService;
    @Autowired
    private RefundOrderInfoService refundOrderInfoService;
    @Autowired
    private JDExpressPushService jdExpressPushService;
    @Autowired
    private OrderDelayService orderDelayService;
    @Autowired
    private MallOrderVerifyMapper mallOrderVerifyMapper;
    @Autowired
    private MallCloudStockDetailService mallCloudStockDetailService;
    @Autowired
    private JiaYiOrderService jiaYiOrderService;
    @Autowired
    MallOrderItemMapper mallOrderItemMapper;

    @Autowired
    ExpressCompanyMapper expressCompanyMapper;

    @Autowired
    private MallOrderAfterSalesProblemService problemService;

    @Autowired
    private MallRegulateInfoMapper mallRegulateInfoMapper;
    @Autowired
    private MallRegulateItemMapper regulateItemMapper;
    @Autowired
    MallOrderFeedBackMapper mallOrderFeedBackMapper;
    @Autowired
    private OSSClientUtil ossClientUtil;


    private static String key = "order:excel:";


    public MallOrderInfo selectById(String id) {
        List<MallOrderInfo> list = this.selectList(
                new EntityWrapper<MallOrderInfo>()
                        .eq("order_id", id)
                        .eq("is_del", 0)

        );
        if (CollectionUtils.isEmpty(list)) {
            throw new MallException(OrderRespCode.DONT_HAVE_ORDER);
        }
        return list.get(0);
    }

    public MallOrderInfo selectByIdNew(String id) {
        List<MallOrderInfo> list = this.selectList(
                new EntityWrapper<MallOrderInfo>()
                        .eq("order_id", id)
                        .eq("is_del", 0)

        );
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    public MallOrderInfo selectByIdNoError(String id) {
        List<MallOrderInfo> list = this.selectList(
                new EntityWrapper<MallOrderInfo>()
                        .eq("order_id", id)
                        .eq("is_del", 0)

        );
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @TxTransaction
    @Transactional
    public boolean updateOrderById(MallOrderInfo mallOrderInfo) {
        mallOrderInfo.setUpdateDate(new Date());
        Integer integer = mallOrderInfoMapper.updateById(mallOrderInfo);
        return integer != null && integer != 0;
    }

    @Transactional
    @TxTransaction
    public MallOrderInfo insertOrderInfo(MallOrderInfo mallOrderInfo) {
        return commonOrderService.insertOrderInfo(mallOrderInfo);
    }


    public boolean updateOrderByIdNew(MallOrderInfo mallOrderInfo) {
        mallOrderInfo.setUpdateDate(new Date());
        Integer integer = mallOrderInfoMapper.updateById(mallOrderInfo);
        return integer != null && integer != 0;
    }

    /**
     * 后台关闭订单
     *
     * @param getOrderInfo
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public boolean closeOrderFromAdmin(MallOrderInfo getOrderInfo) {
        String orderId = getOrderInfo.getOrderId();
        //获取订单信息
        MallOrderInfo mallOrderInfo = this.selectById(orderId);

        String orderStatus = mallOrderInfo.getOrderStatus();

        if ("7".equals(mallOrderInfo.getOrderStatus()) || "6".equals(mallOrderInfo.getOrderStatus())) {
            throw new MallException("020017");
        }

        //京东校验
//        if ("0".equals(mallOrderInfo.getLogisticsMode().trim())) {
//            if (ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getEclpSoNo())) {
//                JSONArray parse = JSONArray.parseArray(mallOrderInfo.getEclpSoNo());
//                boolean isCancel = jdExpressPushService.queryJdOrder(parse.get(0).toString());
//                if (!isCancel) {
//                    throw new MallException("020028");
//                }
//            }
//        }
        if (!"0".equals(mallOrderInfo.getLogisticsMode().trim()) && !"1".equals(mallOrderInfo.getLogisticsMode().trim())) {
            if (ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getExpressCode())) {
                throw new MallException("020041");
            }
        }

        //修改订单为关闭
        mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_007.getCode());
        mallOrderInfo.setIsCanCancel("1");
        mallOrderInfo.setSystemMemo("订单已被系统关闭");
        mallOrderInfo.setCloseDate(new Date());
        boolean res = this.updateOrderById(mallOrderInfo);

        MallUser user = userFeign.getUserById(mallOrderInfo.getMallUserId());

        List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());

        List<MallCloudStockLog> stockLogs = agentFeign.queryCloudStockLogByRelation(MallCloudStockLog.builder().relationId(mallOrderInfo.getOrderId()).relationIdType("0").build());

        //1 如果未支付
        if ("0".equals(orderStatus)) {
            // 提货
            if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退库存
                    backCloud(mallOrderInfo, "7");
                }
            }
            // 换货
            if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退库存
                    backCloud(mallOrderInfo, "7");
                }
                // 4 redis增加库存
                itemList.forEach(p -> {
                    if (MallReviewEnum.ITEM_IN_000.getCode().equals(p.getType())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().longValue());
                    }
                });
            }

            // 直发货 或 入云
            if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())
                    || MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType())) {
                int agentLevel = UserUtils.getAgentLevel(user.getId());
                // 4 redis增加库存
                itemList.forEach(p -> {
                    MallSku sku = ItemCacheUtils.getSkuByCode(p.getSkuCode());
                    //总代直发 积分商城 123级包含减库存的商品 需回退
                    if (agentLevel >= 4 || MallStatusEnum.BELONGS_CODE_001.getCode().equals(sku.getBelongsCode())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().longValue());
                    }
                });
            }
        }

        // 已付款
        if (!"0".equals(orderStatus)) {

            if ("1".equals(mallOrderInfo.getLogisticsMode().trim())
                    && Arrays.asList("0", "2").contains(mallOrderInfo.getOrderType())
                    && Arrays.asList("4", "5").contains(mallOrderInfo.getOrderStatus())) {
//                throw new MallException("0019023", "该状态下新亦源物流不能关闭");

//                jiaYiOrderService.cancelOrder(mallOrderInfo.getOrderId());
            }

            // 123级入云 审核中的
            if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType())) {
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
            }

            // 总代直发 退运费->入云
            if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())) {
                // 退运费
                backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, true);
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());

                //生成入云单
                MallOrderInfo orderInfo = createOrderInfoAndOrderItem(itemList, mallOrderInfo);
                // 入云
                itemList.forEach(p -> {
                    commonOrderService.addCloudStock(mallOrderInfo.getMallUserId(), p, orderInfo.getOrderId());
                    //生成入库详情
                    mallCloudStockDetailService.backFillCloudStockDetail(user, p);
                });
                //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//                commonOrderService.set18RecoveryItems(user.getId(),mallOrderInfo.getOrderType(),itemList);
            }

            // 提货 -> 退运费
            if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退
                    backCloud(mallOrderInfo, "7");
                    try {
                        for (MallOrderItem item : itemList) {
                            if ("1".equals(item.getType())) {
                                mallCloudStockDetailService.backFillCloudStockDetail(user, item);
                            }
                        }
                    } catch (Exception e) {
                        log.info("取消提货订单时,保存cloudStockDetail相关异常:{0}", e);
                    }
                }
                // 退运费
                backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, false);
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
                //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//                commonOrderService.set18RecoveryItems(user.getId(),mallOrderInfo.getOrderType(),itemList);
            }

            if (ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getEclpSoNo()) && "0".equals(mallOrderInfo.getLogisticsMode().trim())) {
                JSONArray parse = JSONArray.parseArray(mallOrderInfo.getEclpSoNo());
                boolean b = jdExpressPushService.cancelOrder(parse.get(0).toString());
                if (!b) {
                    throw new MallException("020040", new Object[]{"正在进行库房拦截.... 请稍后再次点击【关闭】按钮进行关闭"});
                }
            }

            if (Arrays.asList("4", "5").contains(mallOrderInfo.getOrderStatus()) && "1".equals(mallOrderInfo.getLogisticsMode().trim())) {
                QimenClient qimenClient = new QimenClient();
                qimenClient.CancelOrder(mallOrderInfo.getOrderId());
            }
        }

        //todo  京东计数
//        if ("0".equals(mallOrderInfo.getLogisticsType()) && "0".equals(mallOrderInfo.getLogisticsMode()) && Arrays.asList("0","2").contains(mallOrderInfo.getOrderType())) {
//            if(!"1".equals(RedisUtil.get("jd_ex:on_off"))) {
//                RedisUtil.decr("jd_ex:order_now_count", 1);
//                String count = RedisUtil.get("jd_ex:order_now_count");
//                LocalDateTime now = LocalDateTime.now();
//                LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
//                long t = Duration.between(now, end).toMillis();
//                int expire = Integer.parseInt(String.valueOf(t/10000));
//                if(Integer.parseInt(count) < 0) {
//                    RedisUtil.set("jd_ex:order_now_count", "0", expire);
//                }
//                if (Integer.parseInt(count) == 0) {
//                    RedisUtil.expire("jd_ex:order_now_count", expire);
//                }
//            }
//        }

        return res;
    }

    public MallOrderInfo createOrderInfoAndOrderItem(List<MallOrderItem> itemList, MallOrderInfo mallOrderInfo) {
        String orderId = IDUtils.genOrderId();
        MallOrderInfo orderInfo = new MallOrderInfo();
        orderInfo.setOrderId(orderId);
        orderInfo.setMallUserId(mallOrderInfo.getMallUserId());
        orderInfo.setPaymentAmt(mallOrderInfo.getOriginAmt());
        orderInfo.setOriginAmt(mallOrderInfo.getOriginAmt());
        orderInfo.setSummaryAmt(mallOrderInfo.getOriginAmt());
        orderInfo.setCurrentLevel(mallOrderInfo.getCurrentLevel());
        orderInfo.setSystemMemo("系统生成");
        orderInfo.setBelongsCode("0");
        orderInfo.setOrderDescribe("直发订单取消或关闭，生成入云单");
        orderInfo.setCompleteDate(new Date());
        orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
        orderInfo.setRelationOrderId(mallOrderInfo.getOrderId());//绑定的原订单号
        orderInfo.setTradeNo(mallOrderInfo.getTradeNo());
        orderInfo.setPayType(mallOrderInfo.getPayType());
        orderInfo.setPayDate(mallOrderInfo.getPayDate());
        orderInfo.setTransactionId(mallOrderInfo.getTransactionId());
        orderInfo.setOrderType(MallOrderTypeEnum.ORDER_TYPE_001.getCode());
        Date afterDate = new Date(new Date().getTime() + 30 * 60 * 1000);
        orderInfo.setCreateDate(new Date());
        orderInfo.setUpdateDate(new Date());
        orderInfo.setIsCanCancel("1");
        orderInfo.setPayEndDate(afterDate);//支付截止时间30分钟后
        MallBranchOffice branchCompany = agentFeign.getBranchCompany(mallOrderInfo.getMallUserId());
        orderInfo.setCompanyId(branchCompany.getId());
        this.insert(orderInfo);

        MallPayInfo mallPayInfo = new MallPayInfo();
        mallPayInfo.setPayId(IDUtils.genId());
        mallPayInfo.setCreateDate(new Date());
        mallPayInfo.setTotalAmt(BigDecimal.ZERO);
        mallPayInfo.setMallUserId(mallOrderInfo.getMallUserId());
        mallPayInfo.setOrderId(orderId);
        mallPayInfo.setPayType("1");
        mallPayInfo.setPayTime(new Date());
        mallPayInfo.setPayStatus(MallPayStatusEnum.PAY_STATUS_001.getCode());
        mallPayInfo.setStatus(MallStatusEnum.STATUS_CODE_000.getCode());
        mallPayInfo.setTitle("直发取消或关闭生成的入云支付单");
        mallPayInfo.setIsDel(MallStatusEnum.IS_DEL_CODE_001.getCode());
        payFeign.insertPayInfo(mallPayInfo);

        itemList.forEach(p -> {
            MallOrderItem mallOrderItem = new MallOrderItem();
            mallOrderItem.setId(IDUtils.genId());
            mallOrderItem.setOrderId(orderId);
            mallOrderItem.setCreateDate(new Date());
            mallOrderItem.setStatus(MallStatusEnum.ORDER_STATUS_005.getCode());
            mallOrderItem.setItemId(p.getItemId());
            mallOrderItem.setSkuCode(p.getSkuCode());
            mallOrderItem.setAmount(p.getAmount());
            mallOrderItem.setPrice(p.getPrice());
            mallOrderItem.setUnit(p.getUnit());
            mallOrderItem.setSpec(p.getSpec());
            mallOrderItem.setType("0");
            mallOrderItemService.insertOrderItemInfo(mallOrderItem);
        });
        return orderInfo;
    }


    @Transactional
    @TxTransaction(isStart = true)
    public void closeCreditOrder(String orderId) {
        MallOrderInfo orderInfo = mallOrderInfoMapper.selectById(orderId);
        if (!"0".equals(orderInfo.getOrderStatus()) && !"3".equals(orderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.IS_NOT_TO_CANCEL);
        }
        if ("0".equals(orderInfo.getOrderStatus())) {
            orderInfo.setUpdateDate(new Date());
            orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_007.getCode());
            //更新状态为关闭
            mallOrderInfoMapper.updateById(orderInfo);
            return;
        }
        BigDecimal amount = orderInfo.getCredit().abs();
        //  返回积分给代理。
        UpdateAccountAmtParam param = new UpdateAccountAmtParam();
        param.setMallUerId(orderInfo.getMallUserId());
        param.setCredit(amount);
        userFeign.updateAccountAmt(param);
        //添加流水
        MallUserAccountDto mallUserAccountDto = userFeign.accountInfo(orderInfo.getMallUserId());
        MallJournalRecord record = new MallJournalRecord();
        record.setId(IDUtils.genId());
        record.setTitle("退款");
        record.setMallUserId(orderInfo.getMallUserId());
        record.setCurrency("1");
        record.setPayBefore(mallUserAccountDto.getCredit());
        record.setPayAmount(amount);
        record.setPayAfter(mallUserAccountDto.getCredit().add(amount));
        record.setIsDel("0");
        record.setBillType("5");
        record.setBillId(orderInfo.getOrderId());
        record.setRelevanceUserId("");
        addCreditRecord(record);
        orderInfo.setUpdateDate(new Date());
        orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_007.getCode());
        //更新状态为关闭
        mallOrderInfoMapper.updateById(orderInfo);
    }

    private void addCreditRecord(MallJournalRecord mallJournalRecord) {
        payFeign.insertJournalRecord(mallJournalRecord);
    }

    /**
     * 从购物车中提交订单嘿嘿
     *
     * @param param
     */
    @TxTransaction(isStart = true)
    @Transactional
    public OrderInfoDto preOrderFromCart(PreOrderFromCartParam param) {
        List<MallCart> mallCarts = new ArrayList<>();

        param.getCartsAndAmounts().forEach(cart -> {
            //过滤重复id
            List<MallCart> haveCarts = mallCarts.stream().filter(p -> p.getId().equals(cart.getCartId())).collect(Collectors.toList());
            if (haveCarts.size() > 0) {
                return;
            }
            MallCart mallCart = MallCart.builder().id(cart.getCartId()).status("0").isDel("0").build();
            MallCart mallCartInfo = commonOrderService.potCartInfoToOrder(mallCart);
            mallCartInfo.setNumber(cart.getAmount());
            mallCarts.add(mallCartInfo);
        });

        //校验
        checkOrder(mallCarts, param);

        param.setMallCarts(mallCarts);
        //调用提交订单接口
        OrderInfoDto orderInfoDto = placeCartsOrderService.reduceStockAndOrder(param, param.getUser());
        //发送到延迟队列，订单24小时过期处理
        orderInfoDto.getOrderInfoList().forEach(p -> {
            commonOrderService.sendOrderDelayMQ(p.getOrderId());
        });
        return orderInfoDto;
    }

    private void checkOrder(List<MallCart> mallCarts, PreOrderFromCartParam param) {
        //todo 18只限制
//        checkItems(user.getId(), mallCarts, param.getOrderType());
//        checkNowDaysC036(user.getId(), mallCarts, param.getOrderType());
        //todo 新品消毒液只能单独购买, 限总代, 限购
        checkNewItem(mallCarts, Integer.parseInt(param.getUser().getRoleId()), param.getMallUserId(), param);
        //云库存上限限制
        checkCloudStock(mallCarts, param.getOrderType(), param.getUser().getId());
    }

    private void checkNewItem(List<MallCart> mallCarts, int agentLevel, String userId, PreOrderFromCartParam param) {
        for (MallCart cart : mallCarts) {
            //限制口腔 C038-Z  、C038-H C039-Z、C039-H
            if ("0".equals(param.getOrderType())) {
                if (("C038-Z".equals(cart.getSkuCode()) || "C039-Z".equals(cart.getSkuCode())) && "1".equals(RedisUtil.get("limitC038"))) {
                    throw new MallException("020057");
                }
                if (Arrays.asList("30030060").contains(cart.getSkuCode())) {
                    throw new MallException("020040", new Object[]{"【米浮泡沫皮肤抗菌液150ml】暂时不支持直发哦"});
                }
                if ("C044-H".equals(cart.getSkuCode())) {
                    throw new MallException("020040", new Object[]{"【标题浮美精粹焕颜修护面膜(盒)】暂时不支持直发哦"});
                }
            }

            //30030050 不允许直发提货
            if ("0".equals(param.getOrderType()) || "1".equals(param.getOrderType())) {
                if ("30030050".equals(cart.getSkuCode())) {
                    throw new MallException("0989001", "老款米浮组合装 暂不支持下单哦");
                }
            }
            //只能总代购买
//            if ("30290020".equals(cart.getSkuCode()) && agentLevel != 4) {
//                throw new MallException("020053");
//            }
        }

        //不能混合下单直发
        if ("0".equals(param.getOrderType())) {
            String items = RedisUtil.get("check_blend_order_item");
            String canItems = RedisUtil.get("check_can_blend_order_item");
            if (items != null && "1".equals(RedisUtil.get("check_blend"))) {
                List<String> blendItems = JsonUtils.jsonToList(items, String.class);
                List<String> canBlendItems = JsonUtils.jsonToList(canItems, String.class);

                if (blendItems != null) {
                    for (String blendItem : blendItems) {
                        List<MallCart> carts1 = mallCarts.stream().filter(p -> p.getSkuCode().equals(blendItem)).collect(Collectors.toList());
                        List<MallCart> carts2 = mallCarts.stream().filter(p -> !p.getSkuCode().equals(blendItem)).collect(Collectors.toList());
                        if (carts1.size() > 0 && carts2.size() > 0) {

                            boolean f = true;
                            if (LocalDateTime.now().isAfter(LocalDateTime.of(2020, 2, 20, 5, 18, 0, 0))) {
                                //允许的指定混合
                                if (canBlendItems != null) {
                                    for (String can : canBlendItems) {
                                        String[] split = can.split(",");
                                        List<String> list = Arrays.asList(split);
                                        List<MallCart> carts3 = carts2.stream().filter(p -> list.contains(p.getSkuCode())).collect(Collectors.toList());
                                        if (carts2.size() == carts3.size()) {
                                            f = false;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (f) {
                                String title = ItemCacheUtils.getSkuByCode(blendItem).getTitle().split(" ")[0];
                                throw new MallException("060092", new Object[]{title});
                            }
                        }
                    }
                }
            }
        }

        if ("0".equals(param.getOrderType())) {
            //1级限制
            if (1 == agentLevel) {
                String items1grades = RedisUtil.get("check_1_grade_take_of");
                if (items1grades != null) {
                    List<CheckOutGoods> checkOutGoods = JsonUtils.jsonToList(items1grades, CheckOutGoods.class);
                    if (checkOutGoods != null) {
                        for (CheckOutGoods goods : checkOutGoods) {
                            if (0 != goods.getOnline()) {
                                continue;
                            }
                            log.info("获取的数据:{}", goods);
                            int amount = mallCarts.stream().filter(p -> p.getSkuCode().equals(goods.getSkuCode())).map(MallCart::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add).intValue();
                            log.info("当前购买的数量:{}", amount);
                            if (amount > 0) {
                                //不再时间内不能直发和提货
                                String title = ItemCacheUtils.getSkuByCode(goods.getSkuCode()).getTitle().split(" ")[0];
                                if (new Date().after(goods.getEntTime()) || new Date().before(goods.getStartTime())) {
                                    throw new MallException("060094", new Object[]{title});
                                }
                                //直接超购
                                if (amount * goods.getSpecNumber() > goods.getCheckAmount() * goods.getSpecNumber()) {
                                    throw new MallException("060091", new Object[]{title, goods.getCheckAmount()});
                                }
                                //汇总超购
                                Integer count = 0;
                                List<String> skuCodes = Arrays.asList(goods.getSkuCode(), goods.getNextSkuCode());
                                goods.setSkuCodes(skuCodes);
                                goods.setUserId(userId);
                                if (0 == goods.getType()) { //每人每天
                                    count = getTakeOfItemCountEveryDay(goods);
                                    if ((amount * goods.getSpecNumber()) + count > goods.getCheckAmount() * goods.getSpecNumber()) {
                                        throw new MallException("060095", new Object[]{title, goods.getCheckAmount()});
                                    }
                                } else { //时间区间
                                    count = getTakeOfItemCount(goods);
                                    if ((amount * goods.getSpecNumber()) + count > goods.getCheckAmount() * goods.getSpecNumber()) {
                                        throw new MallException("060091", new Object[]{title, goods.getCheckAmount()});
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }
        }

        //对提货和直发有限制的，指定时间限购，设置起始时间终止时间
        if ("0".equals(param.getOrderType())) {
            //1。限购
            String checkGoods = RedisUtil.get("check_out_goods_item_list");
            if (checkGoods != null) {
                List<CheckOutGoods> checkOutGoods = JsonUtils.jsonToList(checkGoods, CheckOutGoods.class);
                if (checkOutGoods != null) {
                    for (CheckOutGoods goods : checkOutGoods) {
                        if (0 != goods.getOnline()) {
                            continue;
                        }
                        log.info("获取的数据:{}", goods);
                        int amount = mallCarts.stream().filter(p -> p.getSkuCode().equals(goods.getSkuCode())).map(MallCart::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add).intValue();
                        log.info("当前购买的数量:{}", amount);
                        if (amount > 0) {
                            //不再时间内不能直发和提货
                            String title = ItemCacheUtils.getSkuByCode(goods.getSkuCode()).getTitle().split(" ")[0];
                            if (new Date().after(goods.getEntTime()) || new Date().before(goods.getStartTime())) {
                                throw new MallException("060094", new Object[]{title});
                            }
                            //直接超购
                            if (amount * goods.getSpecNumber() > goods.getCheckAmount() * goods.getSpecNumber()) {
                                throw new MallException("060091", new Object[]{title, goods.getCheckAmount()});
                            }
                            //汇总超购
                            Integer count = 0;
                            List<String> skuCodes = Arrays.asList(goods.getSkuCode(), goods.getNextSkuCode());
                            goods.setSkuCodes(skuCodes);
                            goods.setUserId(userId);
                            if (0 == goods.getType()) { //每人每天
                                count = getTakeOfItemCountEveryDay(goods);
                                if ((amount * goods.getSpecNumber()) + count > goods.getCheckAmount() * goods.getSpecNumber()) {
                                    throw new MallException("060095", new Object[]{title, goods.getCheckAmount()});
                                }
                            } else { //时间区间
                                count = getTakeOfItemCount(goods);
                                if ((amount * goods.getSpecNumber()) + count > goods.getCheckAmount() * goods.getSpecNumber()) {
                                    throw new MallException("060091", new Object[]{title, goods.getCheckAmount()});
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Integer getNewGoodsCount(String userId) {
        String s = RedisUtil.get("outGoodsDate");
        OutGoodsDate outGoodsDate = new OutGoodsDate();
        if (s == null) {
            outGoodsDate.setUserId(userId);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date date1 = sdf.parse("2020-02-06 00:00:00");
                outGoodsDate.setStartTime(date1);
                Date date2 = sdf.parse("2020-02-15 00:00:00");
                outGoodsDate.setEntTime(date2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            outGoodsDate = JsonUtils.jsonToPojo(s, OutGoodsDate.class);
            outGoodsDate.setUserId(userId);
        }
        Integer c1 = mallOrderInfoMapper.getNewGoodsCount(outGoodsDate);
        Integer c2 = mallOrderInfoMapper.getNewGoodsCountExchangeOut(outGoodsDate);
        if (c1 == null || c2 == null) {
            throw new MallException("060088");
        }
        return Math.max((c1 - c2), 0);
    }

    public Integer getNewGoodsCount2(String userId) {
        String s = RedisUtil.get("outGoodsDate");
        OutGoodsDate outGoodsDate = new OutGoodsDate();
        if (s == null) {
            outGoodsDate.setUserId(userId);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date date1 = sdf.parse("2020-02-06 00:00:00");
                outGoodsDate.setStartTime(date1);
                Date date2 = sdf.parse("2020-02-15 00:00:00");
                outGoodsDate.setEntTime(date2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            outGoodsDate = JsonUtils.jsonToPojo(s, OutGoodsDate.class);
            outGoodsDate.setUserId(userId);
        }
        return mallOrderInfoMapper.getNewGoodsCount2(outGoodsDate);
    }

    public Integer getTakeOfItemCount(CheckOutGoods checkOutGoods) {
        Integer c = mallOrderInfoMapper.getTakeOfItemCount(checkOutGoods);
        if (c == null) {
            throw new MallException("060088");
        }
        return c;
    }

    public Integer getTakeOfItemCountEveryDay(CheckOutGoods checkOutGoods) {
        Integer c = mallOrderInfoMapper.getTakeOfItemCountEveryDay(checkOutGoods);
        if (c == null) {
            throw new MallException("060088");
        }
        return c;
    }

    private void checkActualStock(List<MallCart> mallCarts) {
        mallCarts.forEach(p -> {
            MallSku sku = itemFeign.getSkuByCode(p.getSkuCode());
            String key = RedisUtil.getItemStockKey(sku.getItemId(), sku.getSkuCode());
            // b.获取库存
            String value = RedisUtil.get(key);
            int stock = StringUtils.isEmpty(value) ? 0 : Integer.parseInt(value);
            if (stock < p.getNumber().intValue()) {
                throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{sku.getTitle()});
            }
        });
    }

    //云库存上限限制
    private void checkCloudStock(List<MallCart> mallCarts, String orderType, String userId) {
        BigDecimal total = mallCarts.stream().filter(p -> !Arrays.asList("P001", "P002", "F001").contains(p.getSkuCode()))
                .map(MallCart::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total != null && ORDER_TYPE_001.getCode().equals(orderType)) {
            BigDecimal stock = agentFeign.getMyCloudStockByUserId(userId);
            String n = RedisUtil.get("cloudStockUpperLimit:num");
            if (n != null && stock.add(total).compareTo(new BigDecimal(n)) > 0) {
                throw new MallException("060070");
            }
        }
    }

    //任意一款口腔泡沫18支上限下单限制
    private void checkItems(String userId, List<MallCart> mallCarts, String orderType) {
        BigDecimal total = mallCarts.stream().filter(p -> Arrays.asList("C039-Z", "C038-Z").contains(p.getSkuCode()))
                .map(MallCart::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add);
        String s = RedisUtil.get("ItemTo18Limitations:" + userId);
        int i = 0;
        if (s != null) {
            i = Integer.parseInt(s);
        }
        if (total != null) {
            int num = total.multiply(BigDecimal.valueOf(3)).intValue();
            if (ORDER_TYPE_000.getCode().equals(orderType)) {
                if (num + i > 18) {
                    throw new MallException("060075", new Object[]{"18"});
                }
            }
        }
    }


    /**
     * 从商城直接下单
     *
     * @param param
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    public OrderInfoDto preOrderFromGoods(PreOrderFromGoodsParam param, MallUser mallUser) {
        //代理等级
        int agentLevel = UserUtils.getAgentLevel(mallUser.getId());
        //获取商品详细信息
        MallSku sku = itemFeign.getSkuByParam(MallSku.builder()
                .itemId(param.getItemId())
                .skuCode(param.getSkuCode())
                .build());
        //普通用户没有在产品商城下单的权力
        if (BELONGS_CODE_000.getCode().equals(sku.getBelongsCode())) {
            MallPreconditions.checkToError(agentLevel == 0, OrderRespCode.ORDINARY_USER_NOTTOBUY);
        }
        List<ItemVo> itemVos = Arrays.asList(new ItemVo(param.getItemId(), param.getSkuCode(), param.getAmount(), null));
        //1 校验各个商品的库存
//        commonOrderService.checkActualStock(itemVos, agentLevel, param.getOrderType());
        //4 下单操作
        OrderInfoDto orderInfoDto = placeGoodsOrderService.orderOneHandle(param, sku, mallUser, itemVos);
        //2 预减库存
//        commonOrderService.preSubtractStock(itemVos, agentLevel);
        //发送到延迟队列，订单30分钟过期处理
        orderInfoDto.getOrderInfoList().forEach(p -> {
            commonOrderService.sendOrderDelayMQ(p.getOrderId());
        });
        return orderInfoDto;
    }

    @TxTransaction(isStart = true)
    @Transactional
    public OrderInfoDto zeroToPay(OrderInfoDto orderInfoDto, String orderType, BigDecimal totalAmt, MallUser user) {
        //代理等级
        int agentLevel = UserUtils.getAgentLevel(user.getId());
        // 如果是123级。则直接支付。因为支付为0
        if (agentLevel < 4 && ORDER_TYPE_001.getCode().equals(orderType) && totalAmt.compareTo(BigDecimal.ZERO) == 0) {
            orderInfoDto.getOrderInfoList().forEach(p -> {
                OneKeyPayParam oneKeyPayParam = OneKeyPayParam.builder()
                        .payType(MallPayTypeEnum.PAY_TYPE_001.getCode())
                        .orderId(Arrays.asList(p.getOrderId()))
                        .build();
                payFeign.zeroToPay(oneKeyPayParam);
                p.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_001.getCode());
            });
        }
        return orderInfoDto;
    }


    /**
     * 查询订单总数
     *
     * @param mallOrderInfo
     * @return
     */
    public Integer queryOrderInfoCount(MallOrderInfo mallOrderInfo) {
        log.info("call  queryOrderInfoCount-->  params:{}", mallOrderInfo);
        Integer result = mallOrderInfoMapper.selectCount(new EntityWrapper<MallOrderInfo>()
                .eq(ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getMallUserId()), "mall_user_id", mallOrderInfo.getMallUserId())
                .eq(ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getOrderStatus()), "order_status", mallOrderInfo.getOrderStatus())
                .eq(ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getCurrency()), "currency", mallOrderInfo.getCurrency())
                .eq(ObjectUtils.isNotNullAndEmpty(mallOrderInfo.getLeaderId()), "leader_id", mallOrderInfo.getLeaderId())
                .gt("create_date", "2018-12-01")
                .eq("history_freight_status", "0")
                .eq("is_del", '0'));
        return result;
    }

    /**
     * 查询订单总数
     *
     * @param mallOrderInfo
     * @return
     */
    public Integer queryOrderInfoCountByUserId(MallOrderInfo mallOrderInfo) {
        log.info("call  queryOrderInfoCountByUserId-->  params:{}", mallOrderInfo);
        Integer result = mallOrderInfoMapper.selectCount(new EntityWrapper<MallOrderInfo>().eq("mall_user_id", mallOrderInfo.getMallUserId()).eq("is_del", MallStatusEnum.IS_DEL_CODE_001.getCode()));
        return result;
    }


    public List<MallOrderInfo> queryOrderEclpSONo() {
        return mallOrderInfoMapper.queryOrderEclpSONo();
    }

    /**
     * 云转货单列表
     *
     * @param param
     * @return
     */
    public PageDto<OrderTransferInfoPageDto> queryMallTransferOrders(GetOrderTransferPageListParam param) {
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        List<OrderTransferInfoPageDto> result = new ArrayList<>();
        List<MallTransferGoods> list = mallTransferGoodsMapper.getTransferGoodsPageList(param, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, MallUser> userMap = new HashMap<>();
        for (MallTransferGoods record : list) {
            OrderTransferInfoPageDto pageDto = new OrderTransferInfoPageDto();
            MallUser out = new MallUser();
            MallUser re = new MallUser();

            //转出者
            if (ObjectUtils.isNullOrEmpty(userMap.get(record.getMallUserId()))) {
                out = userFeign.getUserById(record.getMallUserId());
            } else {
                out = userMap.get(record.getMallUserId());
            }
            //接收者
            if (ObjectUtils.isNullOrEmpty(userMap.get(record.getNextProxyId()))) {
                re = userFeign.getUserById(record.getNextProxyId());
            } else {
                re = userMap.get(record.getNextProxyId());
            }
            pageDto.setOutUser(out);
            pageDto.setReciveUser(re);
            pageDto.setOrderId(record.getRelationId());
            pageDto.setRelationType(record.getRelationType());
            pageDto.setCreateDate(record.getCreateDate());
            pageDto.setUpdateDate(record.getUpdateDate());
            pageDto.setMemo(record.getMemo());
            pageDto.setCompanyName(record.getCompanyName());
            MallAgentUpgrade mallAgentUpgrade = agentFeign.queryAgentUpgradeLog(record.getRelationId());
            if (null != mallAgentUpgrade) {
                pageDto.setBeforeLevel(mallAgentUpgrade.getUpgradeAgoLev());
                pageDto.setAfterLevel(mallAgentUpgrade.getUpgradeAfterLev());
            }
//            List<OrderItemDetailDto> itemlist = new ArrayList<>();
            List<OrderItemDetailDto> itemlist = mallOrderInfoMapper.getItemsByOrderID(record.getRelationId());
            int goodsamount = 0;
            for (OrderItemDetailDto detailDto : itemlist) {
                BigDecimal p = new BigDecimal(0);
                if ("1".equals(detailDto.getBelongsCode())) {
                    String retailPrice = detailDto.getRetailPrice();
                    p = new BigDecimal(retailPrice);
                } else {
                    p = PriceUtil.getPrice(detailDto.getRetailPrice(), Integer.parseInt(detailDto.getAgentLevel()));
                }
                goodsamount = goodsamount + detailDto.getAmount().intValue();
                detailDto.setPrice(p);
            }
            pageDto.setSkulist(itemlist);
            pageDto.setGoodsAmount(goodsamount);
            result.add(pageDto);
        }
//        for (MallTransferGoods record : list) {
//            OrderTransferInfoPageDto pageDto = new OrderTransferInfoPageDto();
//            MallUser user = UserUtils.getUserInfoByCacheOrId(record.getMallUserId());
//            pageDto.setOutUser(user);
//            MallUser recive = UserUtils.getUserInfoByCacheOrId(record.getNextProxyId());
//            pageDto.setReciveUser(recive);
//            pageDto.setOrderId(record.getRelationId());
//            pageDto.setRelationType(record.getRelationType());
//            pageDto.setCreateDate(record.getCreateDate());
//            pageDto.setUpdateDate(record.getUpdateDate());
//            pageDto.setMemo(record.getMemo());
//            List<OrderItemDetailDto> itemlist = orderItemByOrderId(record.getRelationId(), 0);
//            pageDto.setSkulist(itemlist);
//            int goodsamount = 0;
//            if (itemlist.size() == 0) {
//                pageDto.setGoodsAmount(0);
//            }
//            for (OrderItemDetailDto order : itemlist) {
//                goodsamount = goodsamount + order.getAmount().intValue();
//            }
//            pageDto.setGoodsAmount(goodsamount);
//            //换货等级变更,根据orderId查询代理升级日志
//            MallAgentUpgrade mallAgentUpgrade = agentFeign.queryAgentUpgradeLog(record.getRelationId());
//            if (null != mallAgentUpgrade) {
//                pageDto.setBeforeLevel(mallAgentUpgrade.getUpgradeAgoLev());
//                pageDto.setAfterLevel(mallAgentUpgrade.getUpgradeAfterLev());
//            }
//
////            MallOrderInfo orderInfo = this.selectById(record.getRelationId());
////            pageDto.setCompanyName(getCompanyName(orderInfo.getCompanyId(), orderInfo.getMallUserId()));
//            pageDto.setCompanyName(record.getCompanyName());
//            result.add(pageDto);
//        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;

//        return queryTransferGoodsPageList(mallTransferGoods);
    }

    /**
     * 入云单列表
     *
     * @param param
     * @return
     */
    public PageDto<OrderInfoPageDto> getGeneralOrderInfoPageList(@RequestBody GetOrderPageListParam param) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setRoleIdList(param.getRoleIdList());
        mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setName(param.getName());
        return getOrderInfoListByPageNew(mallOrderInfo);
    }

    /**
     * 获取用户的订单列表
     *
     * @param mallOrderInfo
     * @return
     */
    public Page<MallOrderInfo> getUserOrderInfos(MallOrderInfo mallOrderInfo) {
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        page = this.selectPage(page, new EntityWrapper<MallOrderInfo>()
                .eq("mall_user_id", mallOrderInfo.getMallUserId())
                .eq("is_del", "0")
                .orderBy("create_date", false));
        return page;
    }

    /**
     * 查询入云单列表（新）
     *
     * @param mallOrderInfo
     * @return
     */
    public PageDto<OrderInfoPageDto> getOrderInfoListByPageNew(MallOrderInfo mallOrderInfo) {
        List<OrderInfoPageDto> result = new ArrayList<>();
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        log.info("mallOrderInfo:,{}", mallOrderInfo);
        List<MallOrderInfo> infos = mallOrderInfoMapper.queryOrderInfoListByPages(mallOrderInfo, page);
        if (!CollectionUtils.isEmpty(infos)) {
            for (MallOrderInfo info : infos) {
                OrderInfoPageDto dto = new OrderInfoPageDto();
                BeanMapper.copy(info, dto);
                List<OrderItemDetailDto> itemlist = orderItemByOrderId(info.getOrderId(), 0);
                if (itemlist.size() > 0) {
                    for (OrderItemDetailDto record : itemlist) {
                        log.info("------------------------------record-----:{}", record.toString());
                        record.setSubtotalPrice(record.getAmount().multiply(record.getPrice()));
                    }
                }
                dto.setMallSku(itemlist);
                int goodsamount = 0;
                if (itemlist.size() == 0) {
                    dto.setGoodsAmount(goodsamount);
                }
                for (OrderItemDetailDto order : itemlist) {
                    goodsamount = goodsamount + order.getAmount().intValue();
                }
                dto.setGoodsAmount(goodsamount);
                dto.setMallUserAddress(dto.getAddrId());
                //用户
                MallUser mallUser = UserUtils.getUserInfoByCacheOrId(info.getMallUserId());
                dto.setMallUser(mallUser);
                result.add(dto);
            }
        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }

    /**
     * 查询入云单列表（旧）
     *
     * @param mallOrderInfo
     * @return
     */
    public PageDto<OrderInfoPageDto> getOrderInfoListByPage(MallOrderInfo mallOrderInfo) {
        List<OrderInfoPageDto> dtos = new ArrayList<>();
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        List<MallOrderInfo> infos = mallOrderInfoMapper.queryOrderInfoListByPages(mallOrderInfo, page);
        if (!CollectionUtils.isEmpty(infos)) {
            for (MallOrderInfo info : infos) {
                log.info("getOrderInfoListByPage  userid={}", info.getMallUserId());
                OrderInfoPageDto dto = new OrderInfoPageDto();
                BeanMapper.copy(info, dto);
                List<OrderItemDetailDto> itemlist = orderItemByOrderId(info.getOrderId(), 0);
                if (itemlist.size() > 0) {
                    for (OrderItemDetailDto record : itemlist) {
                        record.setSubtotalPrice(record.getAmount().multiply(record.getPrice()));
                    }
                }
                dto.setMallSku(itemlist);
                int goodsamount = 0;
                if (itemlist.size() == 0) {
                    dto.setGoodsAmount(goodsamount);
                }
                for (OrderItemDetailDto order : itemlist) {
                    goodsamount = goodsamount + order.getAmount().intValue();
                }
                dto.setGoodsAmount(goodsamount);
                dto.setMallUserAddress(info.getAddrId());
                MallPayInfo payInfo = payFeign.getPayInfoByOrderId(info.getOrderId());
                if (ObjectUtils.isNotNullAndEmpty(payInfo)) {
                    MallUser accept = UserUtils.getUserInfoByCacheOrId(payInfo.getMallUserId());
                    payInfo.setMallUser(accept);
                }
                dto.setMallPayInfo(payInfo);

                //用户
                MallUser mallUser = UserUtils.getUserInfoByCacheOrId(dto.getMallUserId());
                dto.setMallUser(mallUser);


//                //上级用户
//                MallAgent agent = agentFeign.getAgentByUserId(dto.getMallUserId());
//                if (ObjectUtils.isNullOrEmpty(agent) && !StringUtils.isEmpty(agent.getParentId())){
//                    String parentId =agent.getParentId();
//                    MallUser parent = new MallUser();
//                    if (!"0".equals(parentId)) {
//                        parent = UserUtils.getUserInfoByCacheOrId(parentId);
//                    }
//                    dto.setSuperiorUser(parent);
//                }else{
//                    dto.setSuperiorUser(null);
//                }
                dtos.add(dto);
            }
        }


        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(dtos);
        return pageResult;
    }


    /**
     * 后台查询云转货单列表
     *
     * @param mallTransferGoods
     * @return
     */
    public PageDto<OrderTransferInfoPageDto> queryTransferGoodsPageList(MallTransferGoods mallTransferGoods) {
        List<OrderTransferInfoPageDto> result = new ArrayList<>();
        Page page = MybatisPageUtil.getPage(mallTransferGoods.getPageCurrent(), mallTransferGoods.getPageSize());
        List<MallTransferGoods> list = mallTransferGoodsMapper.queryTransferGoodsPageList(mallTransferGoods, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        for (MallTransferGoods record : list) {
            OrderTransferInfoPageDto pageDto = new OrderTransferInfoPageDto();
            MallUser user = UserUtils.getUserInfoByCacheOrId(record.getMallUserId());
            pageDto.setOutUser(user);
            MallUser recive = UserUtils.getUserInfoByCacheOrId(record.getNextProxyId());
            pageDto.setReciveUser(recive);
            pageDto.setOrderId(record.getRelationId());
            pageDto.setRelationType(record.getRelationType());
            pageDto.setCreateDate(record.getCreateDate());
            pageDto.setUpdateDate(record.getUpdateDate());
            pageDto.setMemo(record.getMemo());
            List<OrderItemDetailDto> itemlist = orderItemByOrderId(record.getRelationId(), 0);
            pageDto.setSkulist(itemlist);
            int goodsamount = 0;
            if (itemlist.size() == 0) {
                pageDto.setGoodsAmount(0);
            }
            for (OrderItemDetailDto order : itemlist) {
                goodsamount = goodsamount + order.getAmount().intValue();
            }
            pageDto.setGoodsAmount(goodsamount);
            //换货等级变更,根据orderId查询代理升级日志
            MallAgentUpgrade mallAgentUpgrade = agentFeign.queryAgentUpgradeLog(record.getRelationId());
            if (null != mallAgentUpgrade) {
                pageDto.setBeforeLevel(mallAgentUpgrade.getUpgradeAgoLev());
                pageDto.setAfterLevel(mallAgentUpgrade.getUpgradeAfterLev());
            }

            MallOrderInfo orderInfo = this.selectById(record.getRelationId());
            pageDto.setCompanyName(getCompanyName(orderInfo.getCompanyId(), orderInfo.getMallUserId()));
            result.add(pageDto);
        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }


    /**
     * 换货单列表
     *
     * @param param
     * @return
     */
    public PageDto<OrderExchangeInfoDto> queryExchangeInfoList(GetExchangeOrderParam param) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanMapper.copy(param, mallOrderInfo);
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setDateFrom(param.getStartDate());
        mallOrderInfo.setDateTo(param.getEndDate());
        List<MallUser> userByInput = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                return null;
            }
        }
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userByInput)) {
            for (MallUser user : userByInput) {
                userIdList.add(user.getId());
            }
            mallOrderInfo.setMallUserIdList(userIdList);
        }

        PageDto<OrderExchangeInfoDto> pageDto = queryExchangeOrderInfoList(mallOrderInfo);
        //筛选条件有支付状态
        if (!StringUtils.isEmpty(param.getPayStatus())) {
            List<OrderExchangeInfoDto> list = new ArrayList<>();
            //根据支付状态过滤
            if (!CollectionUtils.isEmpty(pageDto.getRecords())) {
                pageDto.getRecords().stream().forEach(orderExchangeInfoDto -> {
                    MallPayInfo mallPayInfo = orderExchangeInfoDto.getMallPayInfo();
                    if (null != mallPayInfo && param.getPayStatus().equals(mallPayInfo.getPayStatus())) {
                        list.add(orderExchangeInfoDto);
                    }
                });
                pageDto.setRecords(list);
                pageDto.setTotal(list.size());
            }
        }

        return pageDto;
    }

    /**
     * 获取换货列表
     *
     * @param mallOrderInfo
     * @return
     */
    public PageDto<OrderExchangeInfoDto> queryExchangeOrderInfoList(MallOrderInfo mallOrderInfo) {
        List<OrderExchangeInfoDto> result = new ArrayList<>();
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        List<MallOrderInfo> list = mallOrderInfoMapper.queryExchangeOrderInfoList(mallOrderInfo, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (MallOrderInfo record : list) {
            OrderExchangeInfoDto dto = new OrderExchangeInfoDto();
            BeanMapper.copy(record, dto);
            //用户
            MallUser mallUser = UserUtils.getUserInfoByCacheOrId(record.getMallUserId());
            dto.setMallUser(mallUser);


            //商品
            List<OrderItemDetailDto> itemlist = orderItemByOrderId(record.getOrderId(), 0);
            dto.setMallSku(itemlist);

            //支付单
            MallPayInfo mallPayInfo = payFeign.getPayInfoByOrderId(record.getOrderId());
            dto.setMallPayInfo(mallPayInfo);

            //金额
            BigDecimal outAmount = new BigDecimal("0");
            BigDecimal inAmount = new BigDecimal("0");
            if (itemlist.size() > 0) {
                for (OrderItemDetailDto detailDto : itemlist) {
                    if ("0".equals(detailDto.getType())) {
                        //云库存转入
                        inAmount = inAmount.add(detailDto.getAmount().abs().multiply(detailDto.getPrice().abs()));
                    } else if ("1".equals(detailDto.getType())) {
                        //云库存转出
                        outAmount = outAmount.add(detailDto.getAmount().abs().multiply(detailDto.getPrice().abs()));
                    }
                }
            }
            dto.setInAmount(inAmount);
            dto.setReturnAmount(outAmount);
            BigDecimal price = new BigDecimal("0");
            //计算用户或商家需要支付金额
            if (outAmount.compareTo(inAmount) == 0) {
                dto.setUserPayAmount(price);
                dto.setMallReturnAmount(price);
            } else if (outAmount.compareTo(inAmount) > 0) {
                dto.setMallReturnAmount(outAmount.subtract(inAmount));
                dto.setUserPayAmount(price);
            } else if (outAmount.compareTo(inAmount) < 0) {
                dto.setMallReturnAmount(price);
                dto.setUserPayAmount(inAmount.subtract(outAmount));
            }

            dto.setCompanyName(getCompanyName(record.getCompanyId(), record.getMallUserId()));
            result.add(dto);
        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }

    /**
     * 所有产品订单列表
     *
     * @param param
     * @return
     */
    public PageDto<AllOrderInfoDto> queryAllOrderInfoList(GetOrderPageListParam param) {

        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());


        //先判断是否需要查询user表
        List<MallUser> userByInput = null;
        //三个条件任意不为空,都需要查询user表
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                return null;
            } else {
                mallOrderInfo.setMallUserIdList(userByInput.stream().map(MallUser::getId).collect(Collectors.toList()));
            }
        }


        PageDto<AllOrderInfoDto> infoList = queryAllOrderInfoListNew(mallOrderInfo);
        if (ObjectUtils.isNotNullAndEmpty(infoList)) {
            if ("1".equals(mallOrderInfo.getExpressTimeout())) {
                infoList.getRecords().forEach(dto -> {
                    dto.setIsExpressTimeout(Boolean.TRUE);
                });
            } else if (ObjectUtils.isNullOrEmpty(param.getOrderTypes()) ||
                    (ObjectUtils.isNotNullAndEmpty(param.getOrderTypes()) && "0".equals(mallOrderInfo.getExpressTimeout()) && (param.getOrderTypes().contains("0") || param.getOrderTypes().contains("2")))) {
                infoList.getRecords().forEach(dto -> {
                    if (ObjectUtils.isNotNullAndEmpty(dto.getOrderType()) && ObjectUtils.isNotNullAndEmpty(dto.getOrderStatus()))
                        if (dto.getOrderType().matches("0|2") && "4".equals(dto.getOrderStatus()) && ObjectUtils.isNullOrEmpty(dto.getExpressCode())) {
                            if (ObjectUtils.isNotNullAndEmpty(dto.getSendDate())) {
                                LocalDateTime sendDate = dto.getSendDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                if (sendDate.plusDays(1).isBefore(LocalDateTime.now())) {
                                    dto.setIsExpressTimeout(Boolean.TRUE);
                                }
                            }
                        }
                });
            }
        }
        infoList.getRecords().stream().forEach(q -> {
            List<MallOrderFeedBack> mallOrderFeedBacks = mallOrderFeedBackMapper.selectList(new EntityWrapper<MallOrderFeedBack>().eq("order_id", q.getOrderId()).orderBy("create_date", false));
            if (!CollectionUtils.isEmpty(mallOrderFeedBacks))
                q.setFeedbackStatus(mallOrderFeedBacks.get(0).getStatus());
           /* List<MallOrderAfterSalesProblem> problems = problemService.query(q.getOrderId());
            if (ObjectUtils.isNotNullAndEmpty(problems)) {
                q.setProblems(problems);
            }*/
        });

        return infoList;
    }

    private List<MallOrderInfo> queryOrderAboutAdminCodePage(MallOrderInfo mallOrderInfo, Page page, List<MallOrderInfo> list, int pageNumber) {
        //1.分页查询
        List<MallOrderInfo> orderListHasAdminCode = mallOrderInfoMapper.queryOrderList(mallOrderInfo, page);
        if (!CollectionUtils.isEmpty(orderListHasAdminCode)) {
            //查询上级对应的商务code
            list.addAll(orderListHasAdminCode.stream().filter(orderInfo -> !StringUtils.isEmpty(orderInfo.getAdminCode())).collect(Collectors.toList()));
            List<MallOrderInfo> collect = orderListHasAdminCode.stream().filter(orderInfo -> StringUtils.isEmpty(orderInfo.getAdminCode())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(collect)) {
                collect.stream().forEach(orderInfo -> {
                    String adminCode = mallOrderVerifyMapper.queryFirstParentAdminCode(orderInfo.getMallUserId());
                    if (StringUtils.isEmpty(adminCode)) {
                        adminCode = mallOrderVerifyMapper.querySecondParentAdminCode(orderInfo.getMallUserId());
                    }
                    orderInfo.setAdminCode(adminCode);
                });
            }
            list.addAll(collect);
        }

        if (!StringUtils.isEmpty(mallOrderInfo.getAdminCode())) {
            if (!CollectionUtils.isEmpty(list)) {
                list = list.stream().filter(orderInfo -> mallOrderInfo.getAdminCode().equals(orderInfo.getAdminCode())).collect(Collectors.toList());
                //比较当前分页数据是否等于每页分页数
                if (pageNumber + 20 == page.getCurrent() && CollectionUtils.isEmpty(list)) {
                    //死循环预警,如果5页还未查到数据,则认为是无数据
                    return null;
                }
                if (pageNumber + 20 == page.getCurrent() && page.getSize() > list.size()) {
                    //死循环预警,如果5页还未查满数据,则返回
                    return list;
                }
                if (!CollectionUtils.isEmpty(list)) {
                    //查询到adminCode对应总数大于pageSize
                    if (page.getSize() > list.size()) {
                        page.setCurrent(page.getCurrent() + 1);
                        List<MallOrderInfo> mallOrderInfos = queryOrderAboutAdminCodePage(mallOrderInfo, page, list, pageNumber);
                        return mallOrderInfos;
                    } else if (page.getSize() == list.size()) {
                        return list;
                    } else {
                        list = list.subList(0, page.getSize() - 1);
                        return list;
                    }

                } else {
                    //当前页无对应数据,接着往后查询
                    page.setCurrent(page.getCurrent() + 1);
                    List<MallOrderInfo> mallOrderInfos = queryOrderAboutAdminCodePage(mallOrderInfo, page, list, pageNumber);
                    return mallOrderInfos;
                }
            }
        }
        return list;

    }

    /**
     * 所有产品订单列表(新)
     *
     * @param mallOrderInfo
     * @return
     */
//    @ReadOnlyConnection
    public PageDto<AllOrderInfoDto> queryAllOrderInfoListNew(MallOrderInfo mallOrderInfo) {
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        List<MallOrderInfo> list = new ArrayList<>();
        PageDto pageDto = new PageDto();
        if (!StringUtils.isEmpty(mallOrderInfo.getSerialNumber()) || !StringUtils.isEmpty(mallOrderInfo.getSecurityNumber())) {
            List<String> orderIds = mallOrderInfoMapper.getOrderIdBySerialNumber(mallOrderInfo.getSerialNumber(), mallOrderInfo.getSecurityNumber());
            if (!CollectionUtils.isEmpty(orderIds)) {
                mallOrderInfo.setOrderId(orderIds.get(0));
                list = mallOrderInfoMapper.queryOrderListBySerialNumber(mallOrderInfo, page);
            }
        } else if (!CollectionUtils.isEmpty(mallOrderInfo.getOrderTypes())) {
            if (mallOrderInfo.getOrderTypes().contains(MallOrderTypeEnum.ORDER_TYPE_002.getCode())) {
                //出货订单
                mallOrderInfo.setStockTypeAboutType(MallOrderTypeEnum.ORDER_TYPE_002.getCode());
                //分页查询
                list = queryOrderAboutAdminCodePage(mallOrderInfo, page, list, page.getCurrent());
                if (!StringUtils.isEmpty(mallOrderInfo.getAdminCode())) {
                    //根据adminCode查询,设置total
                    mallOrderInfoMapper.queryOrderListByAdminCode(mallOrderInfo, page);
                }
            } else if (mallOrderInfo.getOrderTypes().contains(MallOrderTypeEnum.ORDER_TYPE_001.getCode())) {
                //进货订单
                mallOrderInfo.setStockTypeAboutType(MallOrderTypeEnum.ORDER_TYPE_001.getCode());
                list = mallOrderInfoMapper.queryAllOrderInfoListPages(mallOrderInfo, page);
                if (CollectionUtils.isEmpty(list)) {
                    return null;
                }
            } else if (mallOrderInfo.getOrderTypes().size() == 1 && mallOrderInfo.getOrderTypes().get(0).equals("0")) {
                //直接发货
                list = mallOrderInfoMapper.queryAllOrderInfoListPages(mallOrderInfo, page);
                if (CollectionUtils.isEmpty(list)) {
                    return null;
                }
            }
        } else {
            list = mallOrderInfoMapper.queryAllOrderInfoListPages(mallOrderInfo, page);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
        }

        List<AllOrderInfoDto> dtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            for (MallOrderInfo info : list) {
                AllOrderInfoDto dto = new AllOrderInfoDto();
                BeanMapper.copy(info, dto);
                //用户
                dto.setMallUser(userFeign.getUserById(info.getMallUserId()));
                //商品
                List<OrderItemDetailDto> itemlist = orderItemByOrderId(info.getOrderId(), 0);
                dto.setMallSku(itemlist);
                //商品数
                int goodsamount = 0;
                if (itemlist.size() == 0) {
                    dto.setGoodsAmount(0);
                }
                for (OrderItemDetailDto order : itemlist) {
                    goodsamount = goodsamount + order.getAmount().intValue();
                }
                dto.setGoodsAmount(goodsamount);
                if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(info.getOrderType())) {
                    if (info.getPaymentAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                        dto.setStockType("1");
                    } else {
                        dto.setStockType("2");
                    }
                }
                if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(info.getOrderType())) {
                    dto.setStockType("1");
                }
                if ("1".equals(info.getOrderReview()) || !"4".equals(info.getCurrentLevel())) {
                    List<MallOrderVerify> mallOrderVerifyByOrderId = mallOrderVerifyService.getMallOrderVerifyByOrderIdNew(info.getOrderId());
                    if (!CollectionUtils.isEmpty(mallOrderVerifyByOrderId)) {
                        dto.setVerifyDate(mallOrderVerifyByOrderId.get(0).getCreateDate());
                    }
                }

                //所属公司
                dto.setCompanyName(getCompanyName(info.getCompanyId(), info.getMallUserId()));

                //支付单
//                MallPayInfo mallPayInfo = payFeign.getPayInfoByOrderId(info.getOrderId());
//                dto.setMallPayInfo(mallPayInfo);
                dto.setSendDate(info.getDeliverGoodsDate());
                dto.setIsCanJd(info.getIsCanJd());
                if ("2".equals(info.getPayType())) {
                    dto.setTransactionId(info.getTransactionId());
                } else if ("3".equals(info.getPayType())) {
                    dto.setTransactionId(info.getTradeNo());
                } else if ("7".equals(info.getPayType())) {
                    dto.setTransactionId(info.getETradeNo());
                }
                dtos.add(dto);
            }
        }
        // PageDto pageResult = new PageDto();
        pageDto.setTotal(page.getTotal());
        pageDto.setRecords(dtos);
        return pageDto;
    }


    //查询公司信息
    public String getCompanyName(String companyId, String userId) {
        String companyName = "";
        if (ObjectUtils.isNotNullAndEmpty(companyId)) {
            String company = RedisUtil.get("companyInfo:id_" + companyId);
            if (company != null) {
                MallBranchOffice office = JSONUtil.json2pojo(company, MallBranchOffice.class);
                return office.getCompanyName();
            }
        }
        if (ObjectUtils.isNullOrEmpty(companyId) && ObjectUtils.isNotNullAndEmpty(userId)) {
            companyName = agentFeign.getCompanyNameByUserId(userId);
        }
        return companyName;
    }


    public String doExcel(GetOrderPageListParam param, HttpServletResponse response) throws Exception {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());
        List<MallUser> userByInput = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                ExcelUtil excelUtil = new ExcelUtil();
                LinkedHashMap<String, String> map = getMap();
                String s = excelUtil.buildExcel(map, new ArrayList(), ExcelUtil.DEFAULT_ROW_MAX, response);
                return s;
            }
        }
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userByInput)) {
            for (MallUser user : userByInput) {
                userIdList.add(user.getId());
            }
            mallOrderInfo.setMallUserIdList(userIdList);
        }
        List<MallOrderInfo> infos = mallOrderInfoMapper.doExcel(mallOrderInfo);
        if (CollectionUtils.isEmpty(infos)) {
            return null;
        }
        if (infos.size() > ExcelUtil.ROW_MAX) {
            log.info("MallOrderInfoService.doExcel infos size:{}", infos.size());
            throw new MallException(RespCode.DATA_TOO_LARGE);
        }
        fillValues(infos);
        infos = detailInfos(infos);
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = getMap();
        String s = excelUtil.buildExcel(map, infos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;


//        HSSFWorkbook workbook = new HSSFWorkbook();
//        int i = 1;
//        if (!CollectionUtils.isEmpty(infos)) {
//            i = infos.size() / 50000 + 1; //i代表页签
//        }
//
//        for (int j = 0; j < i; j++) {
//            try {
//                //sheet页
//                log.info("第" + j + "页");
//                HSSFSheet sheet = workbook.createSheet("第" + j + "页");
//
//                //创建表头
//                createTitle(workbook, sheet);
//                //设置日期格式
//                HSSFCellStyle style = workbook.createCellStyle();
//                style.setDataFormat(HSSFDataFormat.getBuiltinFormat("yyyy-MM-dd HH:mm:ss"));
//
//                //新增数据行，并且设置单元格数据
//                int rowNum = 1;
//
//
//                for (int m = 50000 * j; m < infos.size(); m++) {
//                    MallOrderInfo info = infos.get(m);
//                    HSSFRow row = sheet.createRow(rowNum);
//                    //下单时间
//                    String createDate = "";
//                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
//
//                    if (info.getCreateDate() != null) {
//                        createDate = simpleDateFormat.format(info.getCreateDate());
//                    }
//
////            List<OrderItemDetailDto> itemlist = orderItemByOrderId(info.getOrderId(),0);
//
//                    List<ItemAndSkuDTO> itemAndSkuDTOS = new ArrayList<>();
//                    if (!StringUtils.isEmpty(info.getOrderId())) {
//                        List<ItemAndSkuDTO> itemAndSku = getItemAndSku(info.getOrderId());
//                        if (ObjectUtils.isNullOrEmpty(itemAndSku)) {
//                            itemAndSkuDTOS = mallOrderInfoMapper.getItemSkuByOrderId(info.getOrderId());
//                            taskRedisUtil.set(TASK_ORDER_ITEM + info.getOrderId(), JSON.toJSONString(itemAndSkuDTOS));
//                        } else {
//                            itemAndSkuDTOS = itemAndSku;
//                        }
//                    }
//
//
//                    Integer goodsamount = 0;
//                    for (ItemAndSkuDTO order : itemAndSkuDTOS) {
//                        goodsamount = Math.abs(goodsamount) + Math.abs(order.getAmount().intValue());
//                    }
//
//                    List<String> items = itemAndSkuDTOS.stream().map(ItemAndSkuDTO::getItemName).collect(Collectors.toList());
//                    String itemNames = org.apache.commons.lang3.StringUtils.strip(JSON.toJSONString(items), "[]");
//
//
//                    List<String> skuCodes = itemAndSkuDTOS.stream().map(ItemAndSkuDTO::getSkuCode).collect(Collectors.toList());
//                    String skus = org.apache.commons.lang3.StringUtils.strip(JSON.toJSONString(skuCodes), "[]");
//
//                    List<Integer> numbers = itemAndSkuDTOS.stream().map(ItemAndSkuDTO::getAmount).collect(Collectors.toList());
//                    String number = org.apache.commons.lang3.StringUtils.strip(JSON.toJSONString(numbers), "[]");
//
//
//                    //0 订单编号
//                    row.createCell(0).setCellValue(info.getOrderId());
////
//                    //1 下单时间
//                    row.createCell(1).setCellValue(createDate);
//
//                    //2 状态
//                    row.createCell(2).setCellValue(MallOrderStatusEnum.explain(info.getOrderStatus()));
//
//                    //3 订单类型
//                    row.createCell(3).setCellValue(MallOrderTypeEnum.explain(info.getOrderType()));
//
//                    //4 代理昵称
//                    row.createCell(4).setCellValue(info.getNickName());
//
//                    //5 代理手机号
//                    row.createCell(5).setCellValue(info.getPhone());
//
//                    //6 代理等级
//                    row.createCell(6).setCellValue(MallTeamEnum.explain(info.getRoleId()));
//
//                    //7 收件人姓名
//                    row.createCell(7).setCellValue(info.getAddrName());
//
//                    //8 收件人手机号
//                    row.createCell(8).setCellValue(info.getAddrPhone());
//
//                    //9 收件人地址
//                    row.createCell(9).setCellValue(info.getAddrId());
//
//                    //10 商品数
//                    row.createCell(10).setCellValue(number);
//
//                    //11 运费
//                    row.createCell(11).setCellValue(info.getPostFeeAmt() + "");
//
//                    //12 物流单号
//                    row.createCell(12).setCellValue(info.getExpressCode());
//
//                    //13 商品名称
//                    row.createCell(13).setCellValue(itemNames);
//
//                    //14 商品SKU
//                    row.createCell(14).setCellValue(skus);
//
//                    //15 下单数量
//                    row.createCell(15).setCellValue(goodsamount);
//
//                    //16 买家备注
//                    row.createCell(16).setCellValue(info.getBuyerMemo());
//
//                    //17后台备注
//                    row.createCell(17).setCellValue(info.getCsMemo());
//
//                    rowNum++;
//
//                    if (rowNum > 50000) {
//                        break;
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.info("导出异常....：{}", e);
//            }
//
//        }
//
//        String fileName = "产品订单.xls";
//
//        //生成excel文件
//        buildExcelFile(fileName, workbook);
//
//
//        //浏览器下载excel
//        buildExcelDocument(fileName, workbook, response);
//        return "download excel";
//        return "";
    }

    private void fillValues(List<MallOrderInfo> infos) {
        List<String> skuCodes = infos.stream().map(vo -> vo.getSkuCode()).distinct().collect(Collectors.toList());
        List<String> userIds = infos.stream().map(vo -> vo.getMallUserId()).distinct().collect(Collectors.toList());
        Map<String, MallSku> skuMap = null;
        Map<String, MallUser> userMap = null;
        if (ObjectUtils.isNotNullAndEmpty(skuCodes)) {
            skuMap = itemFeign.getSkusBySkuCodes(skuCodes);
        }
        if (ObjectUtils.isNotNullAndEmpty(userIds)) {
            userMap = userFeign.queryUsersByUserIds(userIds);
        }
        Map<String, BigDecimal> totalAmountMap = new HashMap<>();
        Map<String, BigDecimal> totalPriceMap = new HashMap<>();
        for (MallOrderInfo orderInfo : infos) {
            if (totalAmountMap.containsKey(orderInfo.getOrderId())) {
                totalAmountMap.put(orderInfo.getOrderId(), totalAmountMap.get(orderInfo.getOrderId()).add(new BigDecimal(orderInfo.getItemNumber())));
            } else {
                totalAmountMap.put(orderInfo.getOrderId(), new BigDecimal(orderInfo.getItemNumber()));
            }
            if (totalPriceMap.containsKey(orderInfo.getOrderId())) {
                totalPriceMap.put(orderInfo.getOrderId(), totalPriceMap.get(orderInfo.getOrderId()).add(new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber()))));
            } else {
                totalPriceMap.put(orderInfo.getOrderId(), new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber())));
            }
        }
        doFillValues(infos, skuMap, userMap, totalAmountMap, totalPriceMap);
    }

    private void doFillValues(List<MallOrderInfo> infos, Map<String, MallSku> skuMap, Map<String, MallUser> userMap, Map<String, BigDecimal> totalAmountMap, Map<String, BigDecimal> totalPriceMap) {
        for (MallOrderInfo orderInfo : infos) {
            if (null != skuMap) {
                if (skuMap.containsKey(orderInfo.getSkuCode())) {
                    orderInfo.setTitle(skuMap.get(orderInfo.getSkuCode()).getTitle());
                }
            }
            if (null != userMap) {
                if (userMap.containsKey(orderInfo.getMallUserId())) {
                    orderInfo.setNickName(userMap.get(orderInfo.getMallUserId()).getNickName());
                    orderInfo.setPhone(userMap.get(orderInfo.getMallUserId()).getPhone());
                    orderInfo.setRoleId(userMap.get(orderInfo.getMallUserId()).getRoleId());
                    orderInfo.setNickName(userMap.get(orderInfo.getMallUserId()).getNickName());
                }
            }
            if (null != totalAmountMap) {
                if (totalAmountMap.containsKey(orderInfo.getOrderId())) {
                    orderInfo.setItemTotalNum(totalAmountMap.get(orderInfo.getOrderId()).toString());
                }
            }
            if (null != totalPriceMap) {
                if (totalPriceMap.containsKey(orderInfo.getOrderId())) {
                    orderInfo.setItemTotalAmount(totalPriceMap.get(orderInfo.getOrderId()).toString());
                }
            }
            orderInfo.setItemAmount(new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber())).toString());
        }
    }


    public List<MallOrderInfo> detailInfos(List<MallOrderInfo> infos) {

        HashMap<String, MallSku> giftRelationSku = new HashMap<>();
        giftRelationSku.put("30290030", MallSku.builder().skuCode("40000061").title("米浮喷雾瓶50ml（空瓶）").build());
        giftRelationSku.put("30290020", MallSku.builder().skuCode("40000062").title("米浮手枪泵").build());
        giftRelationSku.put("30030040", MallSku.builder().skuCode("C036-H").title("米浮泡沫皮肤抗菌液(支)").build());

        List<ExpressCompany> expressCompanyList = expressCompanyMapper.selectList(new EntityWrapper<ExpressCompany>().eq("is_del", "0"));
        Map<String, String> expressCompanyMap = expressCompanyList.stream().collect(Collectors.toMap(ExpressCompany::getCode, ExpressCompany::getCompanyName));

        Date d1 = DateUtils.parseDate("2020-02-20 05:18:00");
//        Map<String, List<MallOrderInfo>> listMap = infos.stream().filter(p -> Arrays.asList("C036-H", "C036-Z").contains(p.getSkuCode())).collect(Collectors.groupingBy(MallOrderInfo::getOrderId));
        List<String> companyIds = infos.stream().map(vo -> vo.getCompanyId()).distinct().collect(Collectors.toList());
        List<BranchOfficeRp> branchOfficeRps = getCompanys(companyIds);
        for (MallOrderInfo info : infos) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
            String createDate = "";
            if (info.getCreateDate() != null) {
                createDate = simpleDateFormat.format(info.getCreateDate());
                info.setCreateDateString(createDate);
            }
            if ("0".equals(info.getOrderType()) || "1".equals(info.getOrderType())) {
                info.setDirectlyBuy("直购");
            } else {
                info.setDirectlyBuy("非直购");
            }

            info.setOrderStatus(MallOrderStatusEnum.explain(info.getOrderStatus()));
            info.setRoleId(MallTeamEnum.explain(info.getRoleId()));
            // 所属分公司
            info.setCompanyName(getCompanyName(branchOfficeRps, info.getCompanyId()));
            if ("0".equals(info.getOrderType()) || "1".equals(info.getOrderType())) {
                info.setDirectlyBuy("直购");
            } else {
                info.setDirectlyBuy("非直购");
            }
            if (ObjectUtils.isNotNullAndEmpty(info.getLogisticsMode())) {
                String logisticsModeName = expressCompanyMap.get(info.getLogisticsMode());
                info.setLogisticsModeName(ObjectUtils.isNotNullAndEmpty(logisticsModeName) ? logisticsModeName : "");
            }
            if (ObjectUtils.isNotNullAndEmpty(info.getLogisticsType())) {
                switch (info.getLogisticsType()) {
                    case "0":
                        info.setLogisticsTypeName("京东");
                        break;
                    case "1":
                        info.setLogisticsTypeName("顺丰");
                        break;
                }
            } else {
                info.setLogisticsTypeName("");
            }
            MallSku giftSku = giftRelationSku.get(info.getSkuCode());
            if (ObjectUtils.isNotNullAndEmpty(giftSku)) {
                if (!"30290020".equals(info.getSkuCode()) || info.getCreateDate().after(d1)) {
                    info.setGiftSku(giftSku.getSkuCode());
                    info.setGiftName(giftSku.getTitle());
                    info.setGiftQuantity(info.getItemNumber());
                }
            }
//            if (Arrays.asList("0", "2").contains(info.getOrderType())) {
//                getGiftSku(info);
//            }
            info.setOrderType(MallOrderTypeEnum.explain(info.getOrderType()));

        }
        return infos;
    }

    private void getGiftSku(MallOrderInfo info) {
        LocalDateTime payDate = info.getPayDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime start = LocalDateTime.of(2020, 3, 20, 0, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2020, 4, 21, 0, 0, 0, 0);

        if (payDate.isAfter(start) && payDate.isBefore(end)) {
            if (Arrays.asList("123456789", "30030070", "30030080").contains(info.getSkuCode())) {
                info.setGiftSku(info.getSkuCode());
                info.setGiftName(info.getTitle());
                info.setGiftQuantity(info.getItemNumber());

            }
//            switch (info.getSkuCode()) {
//                case "C036-H":
//                    int giftNumber = getGiftNumber(info, listMap);
//                    if (giftNumber >= 2) {
//                        info.setGiftSku("C036-H");
//                        info.setGiftName("米浮泡沫皮肤抗菌液(支)");
//                        info.setGiftQuantity(String.valueOf(giftNumber / 2));
//                    }
//                    break;
//                case "C036-Z":
//                    giftNumber = getGiftNumber(info, listMap);
//                    if (giftNumber >= 2) {
//                        info.setGiftSku("C036-H");
//                        info.setGiftName("米浮泡沫皮肤抗菌液(支)");
//                        info.setGiftQuantity(String.valueOf(giftNumber / 2));
//                    }
//                    break;
//                case "30030060":
//                    if (Math.abs(Integer.parseInt(info.getItemNumber())) >= 2) {
//                        info.setGiftSku("30030060");
//                        info.setGiftName("米浮泡沫皮肤抗菌液150ml（大白分享装）");
//                        info.setGiftQuantity(String.valueOf(Math.abs(Integer.parseInt(info.getItemNumber())) / 2));
//                    }
//                    break;
//            }
        }
    }

    private int getGiftNumber(MallOrderInfo info, Map<String, List<MallOrderInfo>> listMap) {
        int total = 0;
        if (ObjectUtils.isNotNullAndEmpty(listMap) && ObjectUtils.isNotNullAndEmpty(listMap.get(info.getOrderId()))) {
            List<MallOrderInfo> infos = listMap.get(info.getOrderId());
            for (MallOrderInfo item : infos) {
                switch (item.getSkuCode()) {
                    case "C036-H":
                        total = total + (Math.abs(Integer.parseInt(item.getItemNumber())));
                        break;
                    case "C036-Z":
                        total = total + (Math.abs(Integer.parseInt(item.getItemNumber())) * 3);
                        break;
                    case "C036-X":
                        total = total + (Math.abs(Integer.parseInt(item.getItemNumber())) * 120);
                        break;
                }
            }
            listMap.remove(info.getOrderId());
        }
        return total;
    }


    //查询公司信息
    private String getCompanyName(String companyId) {
        String companyName = "";
        if (ObjectUtils.isNotNullAndEmpty(companyId)) {
            String company = RedisUtil.get("companyInfo:id_" + companyId);
            if (company != null) {
                MallBranchOffice office = JSONUtil.json2pojo(company, MallBranchOffice.class);
                return office.getCompanyName();
            }
        }
        return companyName;
    }

    public LinkedHashMap<String, String> getMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("订单编号", "orderId");
        map.put("下单时间", "createDateString");
        map.put("状态", "orderStatus");
        map.put("订单类型", "orderType");
        map.put("代理昵称", "nickName");
        map.put("代理手机号", "phone");
        map.put("代理等级", "roleId");
        map.put("收件人姓名", "addrName");
        map.put("收件人手机号", "addrPhone");
        map.put("收件人地址", "addrId");
        map.put("商品数", "itemNumber");
        map.put("运费", "postFeeAmt");
        map.put("物流单号", "expressCode");
        map.put("商品名称", "title");
        map.put("商品SKU", "skuCode");
        map.put("商品价格", "price");
        map.put("数量", "itemNumber");
        map.put("小计", "itemAmount");
        map.put("赠品名称", "giftName");
        map.put("赠品Sku", "giftSku");
        map.put("赠品数量", "giftQuantity");
        map.put("本单数量总计", "itemTotalNum");
        map.put("本单总金额", "itemTotalAmount");
        map.put("买家备注", "buyerMemo");
        map.put("卖家备注", "csMemo");
        map.put("所属公司", "companyName");
        map.put("是否直购", "directlyBuy");
        map.put("物流方式", "logisticsModeName");
        map.put("物流发货方式", "logisticsTypeName");
        map.put("溯源码", "serialNumber");
        return map;
    }

    //生成excel文件
    protected void buildExcelFile(String filename, HSSFWorkbook workbook) throws Exception {
        FileOutputStream fos = new FileOutputStream(filename);
        workbook.write(fos);
        fos.flush();
        fos.close();
    }


    //浏览器下载excel
    protected void buildExcelDocument(String filename, HSSFWorkbook workbook, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "utf-8"));
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }


    /**
     * 创建表头
     *
     * @param workbook
     * @param sheet
     */
    public void createTitle(HSSFWorkbook workbook, HSSFSheet sheet) {
        HSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(3, 17 * 256);
        //设置为居中加粗
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        HSSFCell cell;
        cell = row.createCell(0);
        cell.setCellValue("订单编号");
        cell.setCellStyle(style);


        cell = row.createCell(1);
        cell.setCellValue("下单时间");
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("状态");
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("订单类型");
        cell.setCellStyle(style);


        cell = row.createCell(4);
        cell.setCellValue("代理昵称");
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("代理手机号");
        cell.setCellStyle(style);


        cell = row.createCell(6);
        cell.setCellValue("代理等级");
        cell.setCellStyle(style);


        cell = row.createCell(7);
        cell.setCellValue("收件人姓名");
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("收件人手机号");
        cell.setCellStyle(style);


        cell = row.createCell(9);
        cell.setCellValue("收件人地址");
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("商品数");
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("运费");
        cell.setCellStyle(style);


        cell = row.createCell(12);
        cell.setCellValue("物流单号");
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("商品名称");
        cell.setCellStyle(style);

        cell = row.createCell(14);
        cell.setCellValue("商品SKU");
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("下单数量");
        cell.setCellStyle(style);


        cell = row.createCell(16);
        cell.setCellValue("买家备注");
        cell.setCellStyle(style);


        cell = row.createCell(17);
        cell.setCellValue("后台备注");
        cell.setCellStyle(style);

    }

    /**
     * 根据orderId获取关联审核单
     *
     * @param OrderId
     * @return
     */
    public VerifyOrderInfoDto queryVerifyOrderByOrderId(String OrderId) {
        VerifyOrderInfoDto result = new VerifyOrderInfoDto();
        List<MallOrderVerify> verifylist = mallOrderVerifyService.selectList(new EntityWrapper<MallOrderVerify>()
                .eq("order_id", OrderId));
        if (verifylist.size() > 0) {
            for (MallOrderVerify verify : verifylist) {
                MallUser proposerUser = UserUtils.getUserInfoByCacheOrId(verify.getProposerId());
                verify.setProposerUser(proposerUser);
                if (!StringUtils.isEmpty(verify.getAccepterId())) {
                    MallUser accept = UserUtils.getUserInfoByCacheOrId(verify.getAccepterId());
                    verify.setAccepterUser(accept);
                } else {
                    verify.setAccepterUser(null);
                }
                String orderid = verify.getOrderId();
                MallOrderInfo orderInfo = mallOrderInfoMapper.selectById(orderid);
                verify.setPayImg(orderInfo.getProofPath());
            }
        }
        result.setOrderVerifies(verifylist);
        return result;
    }


    /**
     * 积分商品列表导出
     *
     * @param param
     * @param response
     * @return
     */
    public String doReport(GetOrderPageListParam param, HttpServletResponse response) throws Exception {
        resovleDate(param);
        List<ItemOrderExcelDTO> itemOrderExcel = mallOrderInfoMapper.getItemOrderExcel(param);
        HSSFWorkbook workbook = new HSSFWorkbook();
        //sheet页
        HSSFSheet sheet = workbook.createSheet("商品订单列表");
        //创建表头
        createTitleOrder(workbook, sheet);
        //设置日期格式
        HSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(HSSFDataFormat.getBuiltinFormat("yyyy-MM-dd HH:mm:ss"));
        //新增数据行，并且设置单元格数据
        if (ObjectUtils.isNotNullAndEmpty(itemOrderExcel)) {
            int rowNum = 1;
            List<String> companyIds = itemOrderExcel.stream().map(vo -> vo.getCompanyId()).distinct().collect(Collectors.toList());
            List<BranchOfficeRp> branchOfficeRps = getCompanys(companyIds);
            for (ItemOrderExcelDTO dto : itemOrderExcel) {
                fillCellValue(sheet, rowNum, branchOfficeRps, dto);
                rowNum++;
            }
        }
        String fileName = "列表.xls";
        //生成excel文件
        // buildExcelFile(fileName, workbook);
        //浏览器下载excel
        buildExcelDocument(fileName, workbook, response);
        return "download excel";
    }

    private List<BranchOfficeRp> getCompanys(List<String> companyIds) {
        List<BranchOfficeRp> branchOfficeRps = Lists.newArrayList();
        if (ObjectUtils.isNotNullAndEmpty(companyIds)) {
            branchOfficeRps = agentFeign.getCompanyNamesByIds(companyIds);
        }
        return branchOfficeRps;
    }

    private void resovleDate(GetOrderPageListParam param) {
        if (param.getDateFrom() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(param.getDateFrom());
            c.add(Calendar.HOUR_OF_DAY, -8);
            Date time = c.getTime();
            param.setDateFrom(time);
        }
        if (param.getDateTo() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(param.getDateTo());
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.HOUR_OF_DAY, -8);
            Date time = c.getTime();
            param.setDateTo(time);

        }
    }

    private void fillCellValue(HSSFSheet sheet, int rowNum, List<BranchOfficeRp> branchOfficeRps, ItemOrderExcelDTO dto) {
        List<MallOrderItemDTO> orderItems = dto.getOrderItems();
        List<String> itemNames = new ArrayList<>();
        List<String> itemPrices = new ArrayList<>();
        BigDecimal totalNumber = new BigDecimal(0);
        String totalAmount = "0";
        List<String> skuCodes = new ArrayList<>();
        if (ObjectUtils.isNotNullAndEmpty(orderItems)) {
            for (MallOrderItemDTO orderItemDTO : orderItems) {
                BigDecimal multiply = orderItemDTO.getCredit().multiply(new BigDecimal(orderItemDTO.getAmount().toString()));
                totalAmount = new BigDecimal(totalAmount).add(multiply).toString();
                totalNumber = totalNumber.add(orderItemDTO.getAmount());
                itemPrices.add(orderItemDTO.getCredit().toString());
                skuCodes.add(orderItemDTO.getSkuCode());
                itemNames.add(orderItemDTO.getItemName());
            }
        }
        HSSFRow row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(dto.getOrderId());
        //下单时间
        String createDate = "";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        if (dto.getCreateTime() != null) {
            createDate = simpleDateFormat.format(dto.getCreateTime());
        }
        //发货时间
        String sendDate = "";
        if (dto.getSendDate() != null) {
            sendDate = simpleDateFormat.format(dto.getSendDate());
        }
        row.createCell(1).setCellValue(createDate);
        row.createCell(2).setCellValue(MallOrderStatusEnum.explain(dto.getStatus()));
        row.createCell(3).setCellValue(MallOrderTypeEnum.explain(dto.getOrderType()));
        row.createCell(4).setCellValue(dto.getAgentNickName());
        row.createCell(5).setCellValue(dto.getAgentPhone());
        row.createCell(6).setCellValue(MallTeamEnum.explain(dto.getAgentLevel()));
        row.createCell(7).setCellValue(dto.getAddressName());
        row.createCell(8).setCellValue(dto.getAddressPhone());
        row.createCell(9).setCellValue(dto.getAddress());
        row.createCell(10).setCellValue(org.apache.commons.lang3.StringUtils.strip(itemNames.toString(), "[]"));
        row.createCell(11).setCellValue(org.apache.commons.lang3.StringUtils.strip(skuCodes.toString(), "[]"));
        row.createCell(12).setCellValue(totalNumber.doubleValue());
        row.createCell(13).setCellValue(dto.getPostFee() + "");
        row.createCell(14).setCellValue(dto.getExpressCode());
        row.createCell(15).setCellValue(dto.getBuyMemo());
        row.createCell(16).setCellValue(dto.getCsMemo());
        row.createCell(17).setCellValue(org.apache.commons.lang3.StringUtils.strip(itemPrices.toString(), "[]"));
        row.createCell(18).setCellValue(totalAmount);
        row.createCell(19).setCellValue(getCompanyName(branchOfficeRps, dto.getCompanyId()));
        row.createCell(20).setCellValue(sendDate);
    }

    private String getCompanyName(List<BranchOfficeRp> branchOfficeRps, String companyId) {
        String companyName = "";
        if (CollectionUtils.isEmpty(branchOfficeRps)) {
            return companyName;
        }
        for (BranchOfficeRp branchOfficeRp : branchOfficeRps) {
            if (com.meifute.core.mmall.common.utils.StringUtils.equals(branchOfficeRp.getCompanyId(), companyId)) {
                companyName = branchOfficeRp.getCompanyName();
                break;
            }
        }
        return companyName;
    }


    /**
     * 创建表头
     *
     * @param workbook
     * @param sheet
     */
    public void createTitleOrder(HSSFWorkbook workbook, HSSFSheet sheet) {
        HSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(3, 17 * 256);
        //设置为居中加粗
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        HSSFCell cell;
        cell = row.createCell(0);
        cell.setCellValue("订单号");
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("下单时间");
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("状态");
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("订单类型");
        cell.setCellStyle(style);


        cell = row.createCell(4);
        cell.setCellValue("代理昵称");
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("代理手机号");
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("代理等级");
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("收件人姓名");
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("收件人手机号");
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("收件地址");
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("商品名称");
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("SKU");
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("商品数");
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("运费");
        cell.setCellStyle(style);

        cell = row.createCell(14);
        cell.setCellValue("物流单号");
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("买家备注");
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("后台备注");
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("商品单价");
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("订单总金额");
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("所属公司");
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("发货时间");
        cell.setCellStyle(style);

    }


    public PageDto queryCreditOrderInfoPages(GetOrderPageListParam param) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanMapper.copy(param, mallOrderInfo);
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());


        mallOrderInfo.setDateFrom(param.getDateFrom());
        mallOrderInfo.setDateTo(param.getDateTo());
        List<MallUser> userByInput = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                return null;
            }
        }

        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userByInput)) {
            for (MallUser user : userByInput) {
                userIdList.add(user.getId());
            }
            mallOrderInfo.setMallUserIdList(userIdList);
        }

        return queryCreditOrderInfos(mallOrderInfo);
    }

    /**
     * 获取积分订单列表
     *
     * @param mallOrderInfo
     * @return
     */
    public PageDto queryCreditOrderInfos(MallOrderInfo mallOrderInfo) {
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());

        if (mallOrderInfo.getDateFrom() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(mallOrderInfo.getDateFrom());
            c.add(Calendar.HOUR_OF_DAY, -8);
            Date time = c.getTime();
            mallOrderInfo.setDateFrom(time);
        }

        if (mallOrderInfo.getDateTo() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(mallOrderInfo.getDateTo());
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.HOUR_OF_DAY, -8);
            Date time = c.getTime();
            mallOrderInfo.setDateTo(time);
        }

        List<MallOrderInfo> list = mallOrderInfoMapper.queryCreditOrderInfoPages(mallOrderInfo, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<AllCreditOrderDto> creditList = new ArrayList<>();
        for (MallOrderInfo info : list) {
            AllCreditOrderDto dto = new AllCreditOrderDto();
            BeanMapper.copy(info, dto);
            dto.setSendDate(info.getDeliverGoodsDate());

            //用户
            dto.setMallUser(UserUtils.getUserInfoByCacheOrId(info.getMallUserId()));

            //商品
            List<OrderItemDetailDto> itemlist = orderItemByOrderId(info.getOrderId(), 0);
            dto.setMallSku(itemlist);

            //商品数
            int goodsamount = 0;
            if (itemlist.size() == 0) {
                dto.setGoodsAmount(0);
            }
            for (OrderItemDetailDto order : itemlist) {
                goodsamount = goodsamount + order.getAmount().intValue();
            }
            dto.setGoodsAmount(goodsamount);

            BigDecimal totalAmount = new BigDecimal(0);

            if (!CollectionUtils.isEmpty(itemlist)) {
                for (OrderItemDetailDto detailDto : itemlist) {
//                    log.info("数量："+detailDto.getAmount());
//                    log.info("单价："+detailDto.getPrice());

                    BigDecimal multiply = detailDto.getAmount().multiply(detailDto.getPrice());
                    totalAmount = totalAmount.add(multiply);
                }
            }
            dto.setTotalAmount(totalAmount);

            dto.setCompanyName(getCompanyName(info.getCompanyId(), info.getMallUserId()));
            creditList.add(dto);
        }
        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(creditList);
        return pageResult;
    }

    /**
     * 获取运费
     *
     * @param getPostFeeParam
     * @return
     */
    public BigDecimal getPostFee(GetPostFeeParam getPostFeeParam) {
        MallUserAddress userAddress = userFeign.queryAddressById(getPostFeeParam.getAddrId());
        List<SkuAndAmountParam> skuAndAmountParams = getPostFeeParam.getParams();

        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        String area = userAddress.getArea();
        skuSpecAmount.setProvince(userAddress.getProvice());
        skuSpecAmount.setCity(area.split(" ")[1]);
        skuSpecAmount.setLogisticsType(getPostFeeParam.getLogisticsType());
        List<SkuAndAmountParam> list = getSkuAndAmountParam(skuAndAmountParams);

        skuSpecAmount.setAmountParams(list);
        BigDecimal freight = itemFeign.getPostFee(skuSpecAmount);
        return freight;
    }

    private List<SkuAndAmountParam> getSkuAndAmountParam(List<SkuAndAmountParam> skuAndAmountParams) {

        ArrayList<SkuAndAmountParam> list = new ArrayList<>();
        for (SkuAndAmountParam skuAndAmountParam : skuAndAmountParams) {
            String skuCode = skuAndAmountParam.getSkuCode();
            String amount = skuAndAmountParam.getAmount();
            String relationSkuCode = skuAndAmountParam.getRelationSkuCode();
            String relationAmount = skuAndAmountParam.getRelationAmount();
            if (ObjectUtils.isNotNullAndEmpty(amount) && new BigDecimal(amount).compareTo(BigDecimal.ZERO) > 0) {
                SkuAndAmountParam param = new SkuAndAmountParam();
                param.setAmount(amount);
                param.setSkuCode(skuCode);
                list.add(param);
            }
            if (ObjectUtils.isNotNullAndEmpty(relationAmount) && new BigDecimal(relationAmount).compareTo(BigDecimal.ZERO) > 0) {
                SkuAndAmountParam param = new SkuAndAmountParam();
                param.setAmount(relationAmount);
                param.setSkuCode(relationSkuCode);
                list.add(param);
            }
        }
        return list;
    }

    /**
     * 获取运费列表
     *
     * @param getPostFeeParam
     * @return
     */
    public List<FreightDTO> getPostFeeList(GetPostFeeParam getPostFeeParam) {
        MallUserAddress userAddress = userFeign.queryAddressById(getPostFeeParam.getAddrId());
        List<SkuAndAmountParam> skuAndAmountParams = getPostFeeParam.getParams();

        ArrayList<FreightDTO> freightDTOS = new ArrayList<>();
        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        String area = userAddress.getArea();
        skuSpecAmount.setProvince(userAddress.getProvice());
        skuSpecAmount.setCity(area.split(" ")[1]);
        skuSpecAmount.setAmountParams(getSkuAndAmountParam(skuAndAmountParams));

        skuSpecAmount.setLogisticsType("0");
        BigDecimal jdFreight = itemFeign.getPostFee(skuSpecAmount);

        if ("1".equals(RedisUtil.get("jd_ex:on_off"))) {
            if (jdFreight.compareTo(BigDecimal.ZERO) > 0) {
                freightDTOS.add(FreightDTO.builder().expressName("京东物流").amount(jdFreight).logisticsType("0").isFullFlag(false).build());
            }
        } else {
            skuSpecAmount.setLogisticsType("1");
            BigDecimal sfFreight = itemFeign.getPostFee(skuSpecAmount);

            if (jdFreight.compareTo(BigDecimal.ZERO) > 0) {
                freightDTOS.add(FreightDTO.builder().expressName("京东物流").amount(jdFreight).logisticsType("0").isFullFlag(false).build());
            }
            if (sfFreight.compareTo(BigDecimal.ZERO) > 0) {
                freightDTOS.add(FreightDTO.builder().expressName("顺丰物流").amount(sfFreight).logisticsType("1").isFullFlag(false).build());
            }
        }
        return freightDTOS;
    }


    /**
     * 条件查询订单列表
     *
     * @param param
     * @return
     */
    public Page<OrderInfoDetailDto> queryOrderInfoList(MallOrderInfoParam param, Page<MallOrderInfo> page) {

        String userId = Optional.ofNullable(param.getPc()).orElse(false) ? "" : UserUtils.getCurrentUser().getId();

        Wrapper<MallOrderInfo> eq = new EntityWrapper<MallOrderInfo>()
                .eq(!StringUtils.isEmpty(param.getBelongsCode()), "belongs_code", param.getBelongsCode())
                .eq(!StringUtils.isEmpty(param.getOrderId()), "order_id", param.getOrderId())
                .eq(!StringUtils.isEmpty(userId), "mall_user_id", userId)
                .gt("create_date", "2018-12-01")
                .eq("is_del", "0")
                .eq("history_freight_status", "0");
        if (!"all".equals(param.getOrderStatus()) && ObjectUtils.isNotNullAndEmpty(param.getOrderStatus())) {
            eq = eq.eq("order_status", param.getOrderStatus());
        }
        if (!StringUtils.isEmpty(param.getPhone())) {
            eq.and(" mall_user_id in (select u.id from m_mall_user.mall_user u where u.phone like '%" + param.getPhone() + "%') ");
        }
        eq = eq.orderBy("update_date", false);
        page = this.selectPage(page, eq);

        List<OrderInfoDetailDto> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(page.getRecords())) {
            page.getRecords().forEach(p -> {
                OrderInfoDetailDto orderInfoDetailDto = new OrderInfoDetailDto();
                List<OrderItemDetailDto> dto = orderItemByOrderId(p.getOrderId(), 0);
                BigDecimal goodsAmount = BigDecimal.ZERO;

                BeanMapper.copy(p, orderInfoDetailDto);

                //todo 因白米浮库存不足，白米浮可以转成200ml溶液
                boolean canTransferC036 = false;
                orderInfoDetailDto.setCanTransferC036(false);
                for (OrderItemDetailDto d : dto) {
                    if ("C036-Z".equals(d.getSkuCode().trim())) {
                        if ("3".equals(p.getOrderStatus()) && checkValidTime(orderInfoDetailDto, p)) {
                            canTransferC036 = true;
                        }
                    }
                    goodsAmount = goodsAmount.add(d.getAmount().abs());
                }
                orderInfoDetailDto.setCanTransferC036(canTransferC036);

                if (MallOrderTypeEnum.ORDER_TYPE_004.getCode().equals(p.getOrderType())) {
                    List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                            .eq("relation_id", p.getOrderId())
                            .eq("relation_type", "0"));
                    if (!CollectionUtils.isEmpty(mallTransferGoods)) {
                        String nextProxyId = mallTransferGoods.get(0).getNextProxyId();
                        MallUser userById = userFeign.getUserById(nextProxyId);
                        LoginDto dt = new LoginDto();
                        BeanMapper.copy(userById, dt);
                        dt.setRoleId(userById.getRoleId());
                        orderInfoDetailDto.setTransPortUser(dt);
                    } else {
                        orderInfoDetailDto.setTransPortUser(null);
                    }
                } else if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(p.getOrderType())) {
                    //金额小计
                    BigDecimal inSubtotalPrice = new BigDecimal(0);
                    //金额小计
                    BigDecimal outSubtotalPrice = new BigDecimal(0);
                    if (!CollectionUtils.isEmpty(dto)) {
                        for (OrderItemDetailDto detailDto : dto) {
                            if (MallReviewEnum.ITEM_OUT_001.getCode().equals(detailDto.getType())) {
                                outSubtotalPrice = outSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            } else {
                                inSubtotalPrice = inSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            }
                        }
                    }
                    if (inSubtotalPrice.compareTo(outSubtotalPrice) > 0) {
                        orderInfoDetailDto.setPayExchangeAmt(inSubtotalPrice.subtract(outSubtotalPrice));
                    } else {
                        orderInfoDetailDto.setSendBackExchangeAmt(outSubtotalPrice.subtract(inSubtotalPrice));
                    }
                }

//                BeanMapper.copy(p, orderInfoDetailDto);
                if ("11".equals(orderInfoDetailDto.getOrderStatus())) {
                    orderInfoDetailDto.setOrderStatus("5");
                }
                orderInfoDetailDto.setGoodsAmount(goodsAmount);
                orderInfoDetailDto.setOrderItemDetailDtos(dto);
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getPayExchangeAmt())) {
                    orderInfoDetailDto.setPayExchangeAmt(new BigDecimal(0));
                }
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getSendBackExchangeAmt())) {
                    orderInfoDetailDto.setSendBackExchangeAmt(new BigDecimal(0));
                }
                if (!ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getMallUserId())) {
                    MallUser mallUser = userFeign.getUserById(orderInfoDetailDto.getMallUserId());
                    orderInfoDetailDto.setUserName(mallUser.getName());
                    orderInfoDetailDto.setUserPhone(mallUser.getPhone());
                }
                if (!MallOrderTypeEnum.ORDER_TYPE_007.getCode().equals(p.getOrderType())) {
                    list.add(orderInfoDetailDto);
                }
            });
        }

        //排序
        List<OrderInfoDetailDto> sortList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            sortList = list.stream().sorted(Comparator.comparing(OrderInfoDetailDto::getCreateDate).reversed()).collect(Collectors.toList());
        }
        Page pageResult = new Page();
        BeanUtils.copyProperties(page, pageResult, "records");
        pageResult.setRecords(sortList);
        return pageResult;
    }

    /**
     * 获取订单详情
     *
     * @param param
     * @return
     */
    public OrderInfoDetailDto queryOrderDetailInfo(MallOrderInfoParam param) {
        if (ObjectUtils.isNullOrEmpty(param.getOrderId())) {
            throw new MallException(OrderRespCode.DONT_HAVE_ORDER);
        }

        //判断是否为活动订单
        if ("2".equals(param.getBelongsCode())) {
            OrderInfoDetailDto detailDto = itemFeign.queryAcOrderDetailInfo(param.getOrderId());
            getNowStatus(detailDto.getExpressCode(), detailDto);
            return detailDto;
        }

        if ("9".equals(param.getOrderType()) || "10".equals(param.getOrderType())) {
            String onOff = RedisUtil.get("enableRetailPriceOnOff");
            MallRegulateInfo regulateInfo = mallRegulateInfoMapper.selectById(param.getOrderId());
            if (regulateInfo == null) {
                throw new MallException(OrderRespCode.DONT_HAVE_ORDER);
            }
            List<MallRegulateItem> items = regulateItemMapper.selectList(new EntityWrapper<MallRegulateItem>().eq("regulate_id", param.getOrderId()));
            OrderInfoDetailDto orderInfoDetailDto = new OrderInfoDetailDto();
            BigDecimal goodsAmount = BigDecimal.ZERO;
            BigDecimal originAmt = BigDecimal.ZERO;
            List<OrderItemDetailDto> list = new ArrayList<>();
            for (MallRegulateItem i : items) {

//                MallItem item = itemFeign.getItemById(i.getItemId());
                MallSku sku = itemFeign.getSkuByCode(i.getSkuCode());

                originAmt = originAmt.add(i.getAmount().abs().multiply(PriceUtil.getPrice(sku.getRetailPrice(), 0)));
                goodsAmount = goodsAmount.add(i.getAmount());

                OrderItemDetailDto dto = OrderItemDetailDto.builder()
                        .orderId(regulateInfo.getId())
//                        .itemId(item.getId())
                        .skuCode(sku.getSkuCode())
                        .amount(i.getAmount())
//                        .itemImages(item.getItemImages())
                        .skuImg(sku.getSkuImg())
                        .price(PriceUtil.getPrice(sku.getRetailPrice(), 0))
                        .priceLabel("")
//                        .subtitle(item.getSubtitle())
                        .title(sku.getTitle())
                        .unit(sku.getUnit())
                        .spec(sku.getSpec())
                        .currency(sku.getCurrency())
                        .createDate(sku.getCreateDate())
                        .build();
                if ("0".equals(onOff)) {
                    dto.setPriceLabel("零售价");
                }
                list.add(dto);
            }
            orderInfoDetailDto.setOrderId(regulateInfo.getId());
            orderInfoDetailDto.setOrderItemDetailDtos(list);
            orderInfoDetailDto.setGoodsAmount(goodsAmount);
            orderInfoDetailDto.setOriginAmt(originAmt);
            orderInfoDetailDto.setCreateDate(regulateInfo.getCreateDate());
            orderInfoDetailDto.setUpdateDate(regulateInfo.getCreateDate());
            orderInfoDetailDto.setPayType("1");
            orderInfoDetailDto.setOrderStatus("5");
            orderInfoDetailDto.setOrderType(param.getOrderType());
            return orderInfoDetailDto;
        }


        Wrapper<MallOrderInfo> eq = new EntityWrapper<MallOrderInfo>()
                .eq("order_id", param.getOrderId())
                .eq("is_del", "0");
        List<MallOrderInfo> list = this.selectList(eq);
        if (CollectionUtils.isEmpty(list)) {
            throw new MallException(OrderRespCode.DONT_HAVE_ORDER);
        }

        MallOrderInfo orderInfo = list.get(0);


        MallAgent agent = agentFeign.getAgentByUserId(orderInfo.getMallUserId());

        OrderInfoDetailDto orderInfoDetailDto = new OrderInfoDetailDto();
        List<OrderItemDetailDto> dto = getOrderItems(orderInfo);
        BigDecimal goodsAmount = BigDecimal.ZERO;
        BigDecimal originAmt = BigDecimal.ZERO;

        //todo 因白米浮库存不足，白米浮可以转成200ml溶液
        boolean canTransferC036 = false;
        orderInfoDetailDto.setCanTransferC036(false);
        for (OrderItemDetailDto d : dto) {
            if ("C036-Z".equals(d.getSkuCode().trim())) {
                if ("3".equals(orderInfo.getOrderStatus()) && checkValidTime(orderInfoDetailDto, orderInfo)) {
                    canTransferC036 = true;
                }
            }
            originAmt = (originAmt.add(d.getAmount().abs().multiply(d.getPrice()))).setScale(2, BigDecimal.ROUND_HALF_UP);
            goodsAmount = goodsAmount.add(d.getAmount());
        }
//        if ("3".equals(orderInfo.getOrderStatus()) && !"2".equals(orderInfo.getLogisticsMode())) {
        orderInfoDetailDto.setCanTransferC036(canTransferC036);
//        }

        BeanMapper.copy(orderInfo, orderInfoDetailDto);

        orderInfoDetailDto.setGoodsAmount(goodsAmount);
        orderInfoDetailDto.setOrderItemDetailDtos(dto);

        if (Arrays.asList("0", "2", "4").contains(orderInfo.getOrderType())) {
            orderInfoDetailDto.setOriginAmt(originAmt);
        }

        if (MallOrderTypeEnum.ORDER_TYPE_004.getCode().equals(orderInfo.getOrderType())) {
            List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                    .eq("relation_id", orderInfo.getOrderId())
                    .eq("relation_type", "0"));

            String nextProxyId = mallTransferGoods.get(0).getNextProxyId();
            MallUser userById = userFeign.getUserById(nextProxyId);
            MallAgent agentByUserId = agentFeign.getAgentByUserId(nextProxyId);
            LoginDto dt = new LoginDto();
            BeanMapper.copy(userById, dt);
            dt.setRoleId(agentByUserId.getAgentLevel());
            orderInfoDetailDto.setTransPortUser(dt);
        } else if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(orderInfo.getOrderType())) {
            //金额小计
            BigDecimal inSubtotalPrice = new BigDecimal(0);
            //金额小计
            BigDecimal outSubtotalPrice = new BigDecimal(0);
            if (!CollectionUtils.isEmpty(dto)) {
                for (OrderItemDetailDto detailDto : dto) {
                    //转出
                    if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(detailDto.getType())) {
                        outSubtotalPrice = outSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                    } else {
                        inSubtotalPrice = inSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                    }
                }
            }
            if (inSubtotalPrice.compareTo(outSubtotalPrice) > 0) {
                orderInfoDetailDto.setPayExchangeAmt(inSubtotalPrice.subtract(outSubtotalPrice));
            } else {
                orderInfoDetailDto.setSendBackExchangeAmt(outSubtotalPrice.subtract(inSubtotalPrice));
            }
        }

        //物流信息 > 这一部分先注释掉，等1.3版本发布的时候，再和app同步上去
        getNowStatus(orderInfo.getExpressCode(), orderInfoDetailDto);

        //余额  积分余额 支付方式
        MallPayInfo payInfoByOrderId = payFeign.getPayInfoByOrderId(param.getOrderId());
        if (!ObjectUtils.isNullOrEmpty(payInfoByOrderId)) {
            if (!StringUtils.isEmpty(payInfoByOrderId.getPayType())) {
                orderInfoDetailDto.setPayType(MallPayTypeEnum.explain(payInfoByOrderId.getPayType()));
            }
        }

        MallUserAccountDto userBalanceAccount = userFeign.getUserBalanceAccount(orderInfo.getMallUserId());

        orderInfoDetailDto.setBalance(ObjectUtils.isNullOrEmpty(userBalanceAccount.getAmt()) ? new BigDecimal(0) : userBalanceAccount.getAmt());
        orderInfoDetailDto.setPointBalance(ObjectUtils.isNullOrEmpty(userBalanceAccount.getCredit()) ? new BigDecimal(0) : userBalanceAccount.getCredit());

        List<Integer> payType = getPayType(orderInfoDetailDto.getCurrency(), agent.getCompanyId());
        orderInfoDetailDto.setPayTypeKey(payType);

        if ("1".equals(orderInfo.getRegulateOrder())) {
            if ("3".equals(orderInfo.getOrderType())) {
                orderInfoDetailDto.setOrderType("11");
            }
            if ("4".equals(orderInfo.getOrderType())) {
                orderInfoDetailDto.setOrderType("12");
            }
        }
        orderInfoDetailDto.setCsMemo(null);
        orderInfoDetailDto.setSystemMemo(null);
        return orderInfoDetailDto;
    }

    private boolean checkValidTime(OrderInfoDetailDto orderInfoDetailDto, MallOrderInfo orderInfo) {
        boolean validTime = false;
        String transferSpec = RedisUtil.get("newItemTransferSpec");
        if (transferSpec == null) {
            return false;
        }
        NewItemTransferSpec spec = JsonUtils.jsonToPojo(transferSpec, NewItemTransferSpec.class);
        log.info("--------------序列化后的转规格参数:{}", spec);
        if (spec != null && 0 == spec.getOnline()) {
            LocalDateTime date = LocalDateTime.ofInstant(orderInfo.getCreateDate().toInstant(), ZoneId.systemDefault());
            LocalDateTime startTime = LocalDateTime.ofInstant(spec.getStartTime().toInstant(), ZoneId.systemDefault());
            LocalDateTime endTime = LocalDateTime.ofInstant(spec.getEndTime().toInstant(), ZoneId.systemDefault());
            log.info("--------------->转规格时间:" + date + " " + startTime + " " + endTime);
            if (date.isAfter(startTime) && date.isBefore(endTime)) {
                log.info("--------------->进入了进入了。。。。");
                if ("0".equals(orderInfo.getLogisticsMode())) {
                    orderInfoDetailDto.setC036Msg(spec.getMsg1());
                }
                if ("2".equals(orderInfo.getLogisticsMode())) {
                    orderInfoDetailDto.setC036Msg(spec.getMsg2());
                }
                validTime = true;
            }
        }
        return validTime;
    }

    private void getNowStatus(String expressCode, OrderInfoDetailDto orderInfoDetailDto) {
        if (!StringUtils.isEmpty(expressCode)) {
            AliExpressResult express = AliExpress.getAliExpress(expressCode);
            if (!ObjectUtils.isNullOrEmpty(express) && !CollectionUtils.isEmpty(express.getData())) {
                orderInfoDetailDto.setNowStatus(express.getData().get(0));
            } else {
                AliExpressDetail detail = new AliExpressDetail();
                detail.setContext("暂无物流信息");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                detail.setTime(simpleDateFormat.format(new Date()));
                orderInfoDetailDto.setNowStatus(detail);
            }
        } else {
            AliExpressDetail detail = new AliExpressDetail();
            detail.setContext("没有物流单号，暂无信息");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            detail.setTime(simpleDateFormat.format(new Date()));
            orderInfoDetailDto.setNowStatus(detail);
        }
    }

    public List<OrderItemDetailDto> getOrderItems(MallOrderInfo orderInfo) {

        List<OrderItemDetailDto> list = new ArrayList<>();
        String onOff = RedisUtil.get("enableRetailPriceOnOff");

        // 转货
        if ("4".equals(orderInfo.getOrderType())) {
            List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                    .eq("relation_id", orderInfo.getOrderId())
                    .eq("relation_type", "0"));
            if (CollectionUtils.isEmpty(mallTransferGoods)) {
                return list;
            }

            for (MallTransferGoods goods : mallTransferGoods) {
                if (goods.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                MallItem item = itemFeign.getItemById(goods.getItemId());
                MallSku sku = itemFeign.getSkuByCode(goods.getSkuCode());

                OrderItemDetailDto dto = OrderItemDetailDto.builder()
                        .orderId(orderInfo.getOrderId())
                        .itemId(item.getId())
                        .skuCode(sku.getSkuCode())
                        .amount(goods.getAmount())
                        .itemImages(item.getItemImages())
                        .skuImg(sku.getSkuImg())
                        .priceLabel("")
                        .price(PriceUtil.getPrice(sku.getRetailPrice(), 0))
                        .subtitle(item.getSubtitle())
                        .title(sku.getTitle())
                        .unit(sku.getUnit())
                        .spec(sku.getSpec())
                        .currency(sku.getCurrency())
                        .createDate(sku.getCreateDate())
                        .build();
                if ("0".equals(onOff)) {
                    dto.setPriceLabel("零售价");
                }
                list.add(dto);
            }
        }

        //直发货 || 提货
        if ("0".equals(orderInfo.getOrderType()) || "2".equals(orderInfo.getOrderType())) {
            List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                    new EntityWrapper<MallOrderItem>()
                            .eq("order_id", orderInfo.getOrderId())
                            .eq("is_del", "0")
            );
            if (CollectionUtils.isEmpty(mallOrderItems)) {
                return list;
            }

            for (MallOrderItem orderItem : mallOrderItems) {
                if (orderItem.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                MallItem item = itemFeign.getItemById(orderItem.getItemId());
                MallSku sku = itemFeign.getSkuByCode(orderItem.getSkuCode());

                BigDecimal p = new BigDecimal(0);

                if ("0".equals(sku.getBelongsCode())) {
                    p = PriceUtil.getPrice(sku.getRetailPrice(), 0);

                } else if ("1".equals(sku.getBelongsCode())) {
                    p = orderItem.getCredit();
                }

                OrderItemDetailDto dto = OrderItemDetailDto.builder()
                        .orderId(orderItem.getOrderId())
                        .itemId(orderItem.getItemId())
                        .skuCode(orderItem.getSkuCode())
                        .amount(orderItem.getAmount())
                        .price(p)
                        .priceLabel("")
                        .credit(orderItem.getCredit())
                        .currency(orderItem.getCurrency())
                        .itemImages(item.getItemImages())
                        .skuImg(sku.getSkuImg())
                        .type(orderItem.getType())
                        .subtitle(item.getSubtitle())
                        .title(sku.getTitle())
                        .waringStatus("0")
                        .unit(sku.getUnit())
                        .spec(sku.getSpec())
                        .createDate(sku.getCreateDate())
                        .build();
                if ("0".equals(onOff)) {
                    dto.setPriceLabel("零售价");
                }
                list.add(dto);
            }
        }

        if (!Arrays.asList("0", "2", "4").contains(orderInfo.getOrderType())) {

            List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                    new EntityWrapper<MallOrderItem>()
                            .eq("order_id", orderInfo.getOrderId())
                            .eq("is_del", "0")
            );
            if (CollectionUtils.isEmpty(mallOrderItems)) {
                return list;
            }
            for (MallOrderItem orderItem : mallOrderItems) {
                if (orderItem.getAmount().compareTo(BigDecimal.valueOf(0)) == 0) {
                    continue;
                }
                MallItem item = ItemCacheUtils.getItemById(orderItem.getItemId());
                MallSku sku = ItemCacheUtils.getSkuByCode(orderItem.getSkuCode());

                BigDecimal p = new BigDecimal(0);

                String priceLabel = "";

                if ("0".equals(sku.getBelongsCode())) {
                    p = orderItem.getPrice();
                    if ("3".equals(orderInfo.getOrderType()) && LocalDateTime.now().isAfter(LocalDateTime.of(2020, 4, 25, 23, 59, 59))) {
                        p = PriceUtil.getPrice(sku.getRetailPrice(), 4);
                    }

                } else if ("1".equals(sku.getBelongsCode())) {
                    p = orderItem.getCredit();
                }

                if ("0".equals(onOff) && "0".equals(sku.getBelongsCode()) && "1".equals(orderInfo.getOrderType())) {
                    priceLabel = PriceLabelEnum.explain(orderInfo.getCurrentLevel());
                }

                OrderItemDetailDto dto = OrderItemDetailDto.builder()
                        .orderId(orderItem.getOrderId())
                        .itemId(orderItem.getItemId())
                        .skuCode(orderItem.getSkuCode())
                        .amount(orderItem.getAmount())
                        .price(p)
                        .credit(orderItem.getCredit())
                        .currency(orderItem.getCurrency())
                        .itemImages(item.getItemImages())
                        .skuImg(sku.getSkuImg())
                        .type(orderItem.getType())
                        .subtitle(item.getSubtitle())
                        .title(sku.getTitle())
                        .waringStatus("0")
                        .unit(sku.getUnit())
                        .spec(sku.getSpec())
                        .createDate(sku.getCreateDate())
                        .build();
                dto.setPriceLabel(priceLabel);
                list.add(dto);
            }
        }
        return list;
    }


    /**
     * 获取支付方式
     *
     * @param currency
     * @param companyId
     * @return
     */
    public List<Integer> getPayType(String currency, String companyId) {
        List<Integer> payTypeKey = new ArrayList<>();

        // 1余额, 2微信，3支付宝
        if (MallStatusEnum.CURRENCY_CODE_000.getCode().equals(currency)) {
            payTypeKey.add(1);
            if (icbcFlag) {
                payTypeKey.add(7);
            }
            if (companyId != null) {
                List<MallPayType> payTypeConfig = agentFeign.getPayTypeConfig(companyId);
                if (payTypeConfig != null) {
                    for (MallPayType p : payTypeConfig) {
                        if ("0".equals(p.getType())) {
                            payTypeKey.add(2);
                        } else {
                            payTypeKey.add(3);
                        }
                    }
                }
            }
        }
        //积分方式
        if (MallStatusEnum.CURRENCY_CODE_001.getCode().equals(currency)) {
            payTypeKey = Arrays.asList(6);
        }

        return payTypeKey;
    }


    // type 0不查预警 1查预警
    public List<OrderItemDetailDto> orderItemByOrderId(String orderId, int type) {

        List<OrderItemDetailDto> list = new ArrayList<>();
        //转货另外处理
        List<MallOrderInfo> mallOrderInfos = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("order_id", orderId)
                .eq("is_del", '0'));
        log.info("-------------------------------------,order_id:{}", orderId);
        if (!CollectionUtils.isEmpty(mallOrderInfos)) {
            MallOrderInfo orderInfo = mallOrderInfos.get(0);
            if (MallOrderTypeEnum.ORDER_TYPE_004.getCode().equals(orderInfo.getOrderType())) {
                List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                        .eq("relation_id", orderId)
                        .eq("relation_type", "0"));
                if (CollectionUtils.isEmpty(mallTransferGoods)) {
                    return list;
                }
                int agentLevel = UserUtils.getAgentLevel(orderInfo.getMallUserId());
                for (MallTransferGoods goods : mallTransferGoods) {
                    MallItem item = itemFeign.getItemById(goods.getItemId());
                    MallSku sku = itemFeign.getSkuByCode(goods.getSkuCode());
                    if (goods.getAmount().compareTo(BigDecimal.valueOf(0)) == 0) {
                        continue;
                    }
                    BigDecimal price;
                    if ("1".equals(sku.getBelongsCode())) {
                        String retailPrice = sku.getRetailPrice();
                        price = new BigDecimal(retailPrice);
                    } else {
                        price = PriceUtil.getPrice(sku.getRetailPrice(), agentLevel);
                    }
                    OrderItemDetailDto dto = OrderItemDetailDto.builder()
                            .orderId(orderId)
                            .itemId(item.getId())
                            .skuCode(sku.getSkuCode())
                            .amount(goods.getAmount())
                            .itemImages(item.getItemImages())
                            .skuImg(sku.getSkuImg())
                            .price(price)
                            .subtitle(item.getSubtitle())
                            .title(sku.getTitle())
                            .unit(sku.getUnit())
                            .spec(sku.getSpec())
                            .currency(sku.getCurrency())
                            .createDate(sku.getCreateDate())
                            .build();
                    list.add(dto);
                }
            } else {
                List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                        new EntityWrapper<MallOrderItem>()
                                .eq("order_id", orderId)
                                .eq("is_del", "0")
                );
                if (CollectionUtils.isEmpty(mallOrderItems)) {
                    return list;
                }
                for (MallOrderItem orderItem : mallOrderItems) {
                    if (orderItem.getAmount().compareTo(BigDecimal.valueOf(0)) == 0) {
                        continue;
                    }
                    MallItem item = ItemCacheUtils.getItemById(orderItem.getItemId());
                    MallSku sku = ItemCacheUtils.getSkuByCode(orderItem.getSkuCode());

                    BigDecimal price = new BigDecimal(0);

                    if ("0".equals(sku.getBelongsCode())) {
                        price = orderItem.getPrice();

                    } else if ("1".equals(sku.getBelongsCode())) {
                        price = orderItem.getCredit();
                    }
                    OrderItemDetailDto dto = OrderItemDetailDto.builder()
                            .orderId(orderItem.getOrderId())
                            .itemId(orderItem.getItemId())
                            .skuCode(orderItem.getSkuCode())
                            .amount(orderItem.getAmount())
                            .price(price)
                            .credit(orderItem.getCredit())
                            .currency(orderItem.getCurrency())
                            .itemImages(item.getItemImages())
                            .skuImg(sku.getSkuImg())
                            .type(orderItem.getType())
                            .subtitle(item.getSubtitle())
                            .title(sku.getTitle())
                            .waringStatus("0")
                            .unit(sku.getUnit())
                            .spec(sku.getSpec())
                            .createDate(sku.getCreateDate())
                            .serialNumber(orderItem.getSerialNumber())
                            .securityNumber(orderItem.getSecurityNumber())
                            .build();
                    if (ObjectUtils.isNotNullAndEmpty(sku.getAmtWarning()) && sku.getAmtWarning().intValue() != 0 && type == 1) {
                        WaringSkuParam waringSkuParam = new WaringSkuParam();
                        waringSkuParam.setWaringAmt(sku.getAmtWarning());
                        waringSkuParam.setSkuCode(sku.getSkuCode());
                        waringSkuParam.setMallUserId(orderInfo.getMallUserId());
                        waringSkuParam.setWarningStartDate(sku.getWarningStartDate());
                        waringSkuParam.setWarningEndDate(sku.getWarningEndDate());
                        int waringAmt = getWaringSkuAmount(waringSkuParam);
                        log.info("---------------------预警:{},值:{}", waringAmt + orderItem.getAmount().abs().intValue(), sku.getAmtWarning().intValue());
                        if (waringAmt + orderItem.getAmount().abs().intValue() > sku.getAmtWarning().intValue()) {
                            dto.setWaringStatus("1");
                            dto.setMaxWaringAmt(sku.getAmtWarning().intValue());
                            dto.setWaringAmt(waringAmt);
                            dto.setSumWaringAmt(waringAmt + orderItem.getAmount().abs().intValue());
                            dto.setWarningStartDate(sku.getWarningStartDate());
                            dto.setWarningEndDate(sku.getWarningEndDate());
                        }
                    }
                    list.add(dto);
                }
            }
        }
        return list;
    }


    /**
     * 交易明细
     *
     * @param mallRechargeInfo
     * @return
     */
    public Page<OrderAndPayInfoDto> getUserOrderAndPayInfo(MallRechargeInfo mallRechargeInfo) {
        Page<OrderAndPayInfoDto> result = new Page<>();
        List<OrderAndPayInfoDto> records = new ArrayList<>();
        result.setRecords(records);
        Page page = MybatisPageUtil.getPage(mallRechargeInfo.getPageCurrent(), mallRechargeInfo.getPageSize());
        MallOrderInfo orderInfo = new MallOrderInfo();
        orderInfo.setCurrency(mallRechargeInfo.getCurrency());
        orderInfo.setMallUserId(mallRechargeInfo.getMallUserId());
        orderInfo.setPageSize(mallRechargeInfo.getPageSize());
        orderInfo.setPageCurrent(mallRechargeInfo.getPageCurrent());
        if ("0".equals(mallRechargeInfo.getCurrency())) {
            Page<OrderAndPayInfoDto> userOrderInfos = page;
            //根据userId查询order信息  ??? 暂时没看懂为什么要查订单信息
            userOrderInfos = getUserOrderInfos(orderInfo, page);
            log.info("根据userId:{},查询order信息结果:{}", orderInfo.getMallUserId(), userOrderInfos);
            if (!CollectionUtils.isEmpty(userOrderInfos.getRecords())) {
                for (OrderAndPayInfoDto dto : userOrderInfos.getRecords()) {
                    if (!StringUtils.isEmpty(dto.getId())) {
                        MallJournalRecord record = payFeign.getMallJournalRecordByBillId(dto.getId());
                        if (!ObjectUtils.isNullOrEmpty(record)) {
                            log.info("0   id为：" + record.getId() + "已设置");
                            dto.setAmount(record.getPayAfter());
                            dto.setItemName(record.getTitle());
                        }
                    }

                }
            }
            //根据userId查询充值提现记录
            PageDto<OrderAndPayInfoDto> orderAndPayInfoDtoPageDto = userFeign.queryChargeList(mallRechargeInfo);
            log.info("根据userId:{},查询充值提现记录结果:{}", orderInfo.getMallUserId(), orderAndPayInfoDtoPageDto);
            orderAndPayInfoDtoPageDto.getRecords().addAll(userOrderInfos.getRecords());
            log.info("根据userId:{},查询结果合并:{}", orderInfo.getMallUserId(), orderAndPayInfoDtoPageDto);
            //排序
            List<OrderAndPayInfoDto> list = orderAndPayInfoDtoPageDto.getRecords();
            if (!CollectionUtils.isEmpty(list)) {
                List<OrderAndPayInfoDto> sortList = list.stream().sorted(
                        Comparator.comparing(OrderAndPayInfoDto::getCreateDate).reversed()
                ).collect(Collectors.toList());
                orderAndPayInfoDtoPageDto.setRecords(sortList);
            } else {
                orderAndPayInfoDtoPageDto.setRecords(new ArrayList<>());
            }
            records = orderAndPayInfoDtoPageDto.getRecords();

            result.setRecords(records);
            result.setTotal(orderAndPayInfoDtoPageDto.getTotal() + userOrderInfos.getTotal());
        } else if ("1".equals(mallRechargeInfo.getCurrency())) {
            MallJournalRecord mallJournalRecord = new MallJournalRecord();
            mallJournalRecord.setPageCurrent(mallRechargeInfo.getPageCurrent());
            mallJournalRecord.setPageSize(mallRechargeInfo.getPageSize());
            mallJournalRecord.setMallUserId(mallRechargeInfo.getMallUserId());
            mallJournalRecord.setCurrency(mallRechargeInfo.getCurrency());
            PageDto<MallJournalRecord> userJournalByPage = payFeign.getUserJournalByPage(mallJournalRecord);
            if (CollectionUtils.isEmpty(userJournalByPage.getRecords())) {
                return result;
            }
            for (MallJournalRecord record : userJournalByPage.getRecords()) {
                OrderAndPayInfoDto dto = new OrderAndPayInfoDto();
                if (record.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
                    dto.setType("4");
                } else {
                    dto.setType("2");
                }
                dto.setStatus("4");
                dto.setPayAmount(record.getPayAmount());
                dto.setCurrency(record.getCurrency());
                dto.setCreateDate(record.getCreateDate());
                dto.setId(record.getBillId());
                log.info("id为：" + record.getId() + "已设置");
                dto.setItemName(record.getTitle());
                dto.setAmount(record.getPayAfter());
                records.add(dto);
            }

            result.setTotal(userJournalByPage.getTotal());
        }


        return result;
    }

    /**
     * (仅限交易明细)
     *
     * @param mallOrderInfo
     * @param page
     * @return
     */
    //getOrderStatus: 0 =待付款 1=上级审核中 2= 上级审核未通过 3=待发货 4=待收货 5=已完成 6=交易取消 7=交易关闭 8=商务审核中 9=商务审核未通过 10 =退款中 11=已退款',
    //status :0=待付款 1=处理中 2=交易取消 3=退款中 4=已完成 5=已退款    "0=充值 1=提现 2=支出 3=退款 4=收入 5=消费"
    private Page<OrderAndPayInfoDto> getUserOrderInfos(MallOrderInfo mallOrderInfo, Page page) {
        List<MallOrderInfo> mallOrderInfos = mallOrderInfoMapper.queryUserOrderInfoPages(mallOrderInfo, page);
        Page<OrderAndPayInfoDto> pageResult = new Page<>();
        List<OrderAndPayInfoDto> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mallOrderInfos)) {
            for (MallOrderInfo info : mallOrderInfos) {
                OrderAndPayInfoDto dto = new OrderAndPayInfoDto();
                dto.setId(info.getOrderId());
                dto.setCreateDate(info.getCreateDate());
                dto.setCurrency(info.getCurrency());
                if ("10".equals(info.getOrderStatus()) || "11".equals(info.getOrderStatus())) {
                    if ("10".equals(info.getOrderStatus())) {
                        setOrderInfoDto(dto, "3", info.getPaymentAmt(), "3", null);
                    }
                    if ("11".equals(info.getOrderStatus())) {
                        setOrderInfoDto(dto, "5", info.getPaymentAmt(), "3", null);
                    }
                } else if ("6".equals(info.getOrderStatus()) || "7".equals(info.getOrderStatus())) {
                    setOrderInfoDto(dto, "2", info.getPaymentAmt().multiply(new BigDecimal("-1")), "2", info);
                } else if ("5".equals(info.getOrderStatus())) {
                    MallPayInfo payInfoByOrderId = payFeign.getPayInfoByOrderId(info.getOrderId());
                    if (payInfoByOrderId != null && "3".equals(payInfoByOrderId.getPayStatus())) {
                        setOrderInfoDto(dto, "4", info.getPaymentAmt(), "3", info);
                    } else {
                        setOrderInfoDto(dto, "4", info.getPaymentAmt().multiply(new BigDecimal("-1")), "2", info);
                    }
                } else if ("0".equals(info.getOrderStatus())) {
                    setOrderInfoDto(dto, "0", info.getPaymentAmt().multiply(new BigDecimal("-1")), "2", info);
                } else if ("2".equals(info.getOrderStatus()) || "9".equals(info.getOrderStatus())) {
                    setOrderInfoDto(dto, "7", info.getPaymentAmt().multiply(new BigDecimal("-1")), "2", info);
                } else {
                    setOrderInfoDto(dto, "1", info.getPaymentAmt().multiply(new BigDecimal("-1")), "2", info);
                }
                list.add(dto);
            }
        }
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(list);
        return pageResult;
    }

    private void setOrderInfoDto(OrderAndPayInfoDto dto, String status, BigDecimal payAmount, String type, MallOrderInfo info) {
        dto.setPayAmount(payAmount);
        dto.setType(type);
        dto.setStatus(status);
        if (ObjectUtils.isNotNullAndEmpty(info)) {
            if ("1".equals(info.getCurrency())) {
                dto.setType("5");
            }
        }
    }

    /**
     * 取消订单信息
     *
     * @param getOrderInfo
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "orderId")
    public boolean cancelOrderInfo(GetOrderInfo getOrderInfo) {
        //todo 禁用
        if ("1".equals(RedisUtil.get("closeFlag"))) {
            throw new MallException("060090");
        }
        String s = RedisUtil.get("cancelOrderProhibitRepeat:orderId_" + getOrderInfo.getOrderId());
        if (s != null) {
            throw new MallException("060089");
        }
        RedisUtil.set("cancelOrderProhibitRepeat:orderId_" + getOrderInfo.getOrderId(), getOrderInfo.getOrderId(), 10);

        String orderId = getOrderInfo.getOrderId();
        log.info("=========================取消订单，order_id:{}", orderId);
        //获取订单信息
        MallOrderInfo mallOrderInfo = this.selectById(orderId);
        String orderStatus = mallOrderInfo.getOrderStatus();
        log.info("=========================取消订单，状态：{}", orderStatus);

        //1为不可取消
        if ("1".equals(mallOrderInfo.getIsCanCancel())) {
            throw new MallException(OrderRespCode.IS_NOT_TO_CANCEL);
        }
        if (Arrays.asList("6", "7", "10", "11", "12").contains(orderStatus)) {
            throw new MallException(OrderRespCode.IS_NOT_TO_CANCEL);
        }

        //修改订单为取消
        mallOrderInfo.setOrderStatus(MallStatusEnum.ORDER_STATUS_006.getCode());
        mallOrderInfo.setIsCanCancel("1");
        mallOrderInfo.setSystemMemo("订单已被用户取消");
        mallOrderInfoMapper.updateById(mallOrderInfo);

        //查询用户信息
        MallUser user = UserUtils.getCurrentUser();

        List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());

        List<MallCloudStockLog> stockLogs = agentFeign.queryCloudStockLogByRelation(MallCloudStockLog.builder().relationId(mallOrderInfo.getOrderId()).relationIdType("0").build());

        //1 如果未支付
        if (MallPayStatusEnum.PAY_STATUS_000.getCode().equals(orderStatus)) {
            //入云库存，总代回退库存到公司缓存
            if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType()) && "4".equals(user.getRoleId())) {
                // 4 redis增加库存
                itemList.forEach(p -> {
                    if ("30190010".equals(p.getSkuCode())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().abs().longValue());
                    }

                });
            }
            // 提货
            if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退库存
                    backCloud(mallOrderInfo, "6");
                }
            }
            // 换货
            if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退库存
                    backCloud(mallOrderInfo, "6");
                    // 4 redis增加库存
                    itemList.forEach(p -> {
                        if ("30190010".equals(p.getSkuCode())) {
                            if (MallReviewEnum.ITEM_IN_000.getCode().equals(p.getType())) {
                                String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                                RedisUtil.incr(key, p.getAmount().abs().longValue());
                            }
                        }
                    });
                }
            }
            //直接发货
            if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())) {
                // 4 redis增加库存
                itemList.forEach(p -> {
                    if ("30190010".equals(p.getSkuCode())) {
                        String key = RedisUtil.getItemStockKey(p.getItemId(), p.getSkuCode());
                        RedisUtil.incr(key, p.getAmount().abs().longValue());
                    }

                });
            }
            log.info("已取消该订单！");
        }

        // 已付款
        if (!MallPayStatusEnum.PAY_STATUS_000.getCode().equals(orderStatus)) {
            // 123级入云 审核中的可以取消
            if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(mallOrderInfo.getOrderType())) {
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
            }
            // 商务审核拒绝的不允许取消
            if (MallOrderStatusEnum.ORDER_STATUS_009.getCode().equals(mallOrderInfo.getOrderStatus())) {
                throw new MallException(OrderRespCode.IS_NOT_TO_CANCEL);
            }
            // 总代直发 退运费->入云
            if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())) {

                // 退运费
                backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, true);
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
                //5 生成入云单
                MallOrderInfo orderInfo = createOrderInfoAndOrderItem(itemList, mallOrderInfo);
                // 入云
                itemList.forEach(p -> {
                    commonOrderService.addCloudStock(mallOrderInfo.getMallUserId(), p, orderInfo.getOrderId());
                    //生成入库详情
                    mallCloudStockDetailService.backFillCloudStockDetail(user, p);
                });
                //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//                commonOrderService.set18RecoveryItems(user.getId(),mallOrderInfo.getOrderType(),itemList);
            }

            // 提货 -> 退运费
            if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
                if (stockLogs != null) {
                    // 回退
                    backCloud(mallOrderInfo, "6");
                    try {
                        for (MallOrderItem item : itemList) {
                            if ("1".equals(item.getType())) {
                                //生成入库详情
                                mallCloudStockDetailService.backFillCloudStockDetail(user, item);
                            }
                        }
                    } catch (Exception e) {
                        log.info("取消提货订单时,保存cloudStockDetail相关异常:{}", e);
                    }
                    //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//                    commonOrderService.set18RecoveryItems(user.getId(),mallOrderInfo.getOrderType(),itemList);
                }
                // 退运费
                backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, false);
                //4 更新审核单
                cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
                //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//                commonOrderService.set18RecoveryItems(user.getId(), mallOrderInfo.getOrderType(), itemList);
            }
        }

        //todo  京东计数
//        if ("0".equals(mallOrderInfo.getLogisticsType()) && "0".equals(mallOrderInfo.getLogisticsMode()) && Arrays.asList("0","2").contains(mallOrderInfo.getOrderType())) {
//            if(!"1".equals(RedisUtil.get("jd_ex:on_off"))) {
//                RedisUtil.decr("jd_ex:order_now_count", 1);
//                String count = RedisUtil.get("jd_ex:order_now_count");
//                LocalDateTime now = LocalDateTime.now();
//                LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
//                long t = Duration.between(now, end).toMillis();
//                int expire = Integer.parseInt(String.valueOf(t/10000));
//                if(Integer.parseInt(count) < 0) {
//                    RedisUtil.set("jd_ex:order_now_count", "0", expire);
//                }
//                if (Integer.parseInt(count) == 0) {
//                    RedisUtil.expire("jd_ex:order_now_count", expire);
//                }
//            }
//        }

        return true;
    }

    //取消审核单
    public void cancelVerifyOrderInfo(String orderId) {
        List<MallOrderVerify> list = mallOrderVerifyService.getMallOrderVerifyByOrderId(orderId);
        if (ObjectUtils.isNotNullAndEmpty(list)) {
            list.forEach(p -> {
                if (MallOrderVerifyEnum.VERIFY_STATUS_000.getCode().equals(p.getVerifyStatus())) {
                    MallOrderVerify verify = new MallOrderVerify();
                    verify.setId(p.getId());
                    verify.setUpdateDate(new Date());
                    verify.setVerifyStatus(MallOrderVerifyStatusEnum.VERIFY_STATUS_004.getCode());
                    mallOrderVerifyService.updateVerifyOrder(verify);
                }
            });
        }
    }


    /**
     * 退款
     *
     * @param price
     * @param mallOrderInfo
     * @param itemList
     */
    public void backToPay(BigDecimal price, MallOrderInfo mallOrderInfo, List<MallOrderItem> itemList, boolean isCallBack) {
        //1 生成退款订单
        RefundOrderInfoParam refundOrderInfoParam = new RefundOrderInfoParam();
        refundOrderInfoParam.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_011.getCode());
        refundOrderInfoParam.setPrice(price);
        refundOrderInfoParam.setUserId(mallOrderInfo.getMallUserId());
        MallOrderInfo orderInfo = refundOrderInfoService.createRefundOrderInfo(refundOrderInfoParam, mallOrderInfo);

        //2 生成退款支付信息
        MallPayInfo refundPayInfo = refundOrderInfoService.createRefundToPayInfo(orderInfo.getMallUserId(), orderInfo, MallPayStatusEnum.PAY_STATUS_003.getCode());

        //3 生成退货商品记录信息
        refundOrderInfoService.createRefundOrderItemInfo(itemList, orderInfo);

        //用户进账
        UpdateAccountAmtParam accountAmt = new UpdateAccountAmtParam();
        accountAmt.setMallUerId(orderInfo.getMallUserId());
        accountAmt.setAmount(price);
        MallAccount account = userFeign.updateAccountAmt(accountAmt);

        //记录退款详细title
        MallSku sku = itemFeign.getSkuByParam(MallSku.builder().itemId(itemList.get(0).getItemId())
                .skuCode(itemList.get(0).getSkuCode()).build());
        String title = "交易关闭退运费:" + sku.getTitle();

        refundPayInfo.setTitle("交易关闭退运费:" + sku.getTitle());

        //end 支付完成记录流水表
        recordJournal(title, mallOrderInfo, account, price, BigDecimal.valueOf(1), "2");
    }


    /**
     * 记录资金变动交易流水
     *
     * @param orderInfo
     * @param account
     * @param amt
     * @param type      -1或1
     *                  billType 1充值，2.退款，3支付，4提现，5退代提现，6新品试用支付，7活动支付
     */
    public void recordJournal(String title, MallOrderInfo orderInfo, MallAccount account, BigDecimal amt, BigDecimal type, String billType) {
        MallJournalRecord mallJournalRecord = new MallJournalRecord();
        mallJournalRecord.setBillId(orderInfo.getOrderId());
        mallJournalRecord.setBillType(billType);
        mallJournalRecord.setCurrency(orderInfo.getCurrency());
        mallJournalRecord.setMallUserId(account.getMallUserId());
        mallJournalRecord.setPayBefore(account.getAmt().subtract(amt.multiply(type)));
        mallJournalRecord.setPayAmount(amt.multiply(type));
        mallJournalRecord.setPayAfter(account.getAmt());
        mallJournalRecord.setRelevanceUserId(account.getMallUserId());
        mallJournalRecord.setTitle(title);
        payFeign.insertJournalRecord(mallJournalRecord);
    }


    public void backCloud(MallOrderInfo mallOrderInfo, String orderStatus) {
        // 回退库存
        List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());

//        if (!"7".equals(orderStatus)) {
//            String s = RedisUtil.get("backCloudRecord:orderId_" + mallOrderInfo.getOrderId());
//            if (s != null) {
//                return;
//            }
//            RedisUtil.set("backCloudRecord:orderId_" + mallOrderInfo.getOrderId(), mallOrderInfo.getOrderId(), 60 * 60);
//        }

        if ("3".equals(mallOrderInfo.getOrderType())) {

            for (MallOrderItem item : itemList) {
                if ("1".equals(item.getType())) {
                    MallCloudStock mallCloudStock = agentFeign.getCloudStock(MallCloudStock.builder().mallUserId(mallOrderInfo.getMallUserId()).skuCode(item.getSkuCode()).build());

                    BigDecimal amount = item.getAmount().multiply(BigDecimal.valueOf(-1));

                    agentFeign.updateCloudStockByLock(MallCloudStock.builder().id(mallCloudStock.getId()).stock(amount).build());

                    // 添加云库存记录 云库存单号 用户信息 商品 交易前数量	交易数量	交易后数量 日志时间
                    MallCloudStockLog mallCloudStockLog1 = MallCloudStockLog.builder()
                            .cloudStockId(mallCloudStock.getId())
                            .skuCode(mallCloudStock.getSkuCode())
                            .itemId(mallCloudStock.getItemId())
                            .payStock(amount)
                            .relationId(mallOrderInfo.getOrderId())
                            .mallUserId(mallOrderInfo.getMallUserId())
                            .payAgoStock(mallCloudStock.getStock())
                            .payAfterStock(mallCloudStock.getStock().add(amount))
                            .build();
                    agentFeign.addCloudStockLog(mallCloudStockLog1);
                }
            }
        } else {
            for (MallOrderItem item : itemList) {

                MallCloudStock mallCloudStock = agentFeign.getCloudStock(MallCloudStock.builder().mallUserId(mallOrderInfo.getMallUserId()).skuCode(item.getSkuCode()).build());

                BigDecimal amount = item.getAmount().multiply(BigDecimal.valueOf(-1));

                agentFeign.updateCloudStockByLock(MallCloudStock.builder().id(mallCloudStock.getId()).stock(amount).build());

                // 添加云库存记录 云库存单号 用户信息 商品 交易前数量	交易数量	交易后数量 日志时间
                MallCloudStockLog mallCloudStockLog1 = MallCloudStockLog.builder()
                        .cloudStockId(mallCloudStock.getId())
                        .skuCode(mallCloudStock.getSkuCode())
                        .itemId(mallCloudStock.getItemId())
                        .payStock(amount)
                        .relationId(mallOrderInfo.getOrderId())
                        .mallUserId(mallOrderInfo.getMallUserId())
                        .payAgoStock(mallCloudStock.getStock())
                        .payAfterStock(mallCloudStock.getStock().add(amount))
                        .build();
                agentFeign.addCloudStockLog(mallCloudStockLog1);
            }
        }
    }


    /**
     * 真正增加库存 （支付完成后在增加库存）
     *
     * @param itemId
     * @param skuCode
     * @param amount
     */
    public void reduceAddStock(String itemId, String skuCode, BigDecimal amount, BigDecimal allAmount, boolean isCallBack) {
        //查询sku数据库存
        MallSku result = ItemCacheUtils.getSkuByCode(skuCode);
        allAmount = allAmount.add(amount.abs());
        if (isCallBack) {
            //更新
            UpdateSkuParam sku = UpdateSkuParam.builder()
                    .stock(amount.abs())
                    .id(result.getId())
                    .skuCode(skuCode)
                    .build();
            itemFeign.updateSkuStock(sku);
//            String key = RedisUtil.getItemStockKey(itemId, skuCode);
//            RedisUtil.incr(key, amount.longValue());
        }
    }

    /**
     * 确认收货
     *
     * @param orderId
     */
    @TxTransaction(isStart = true)
    @Transactional
    public boolean receivingItem(String orderId) {
        //获取订单信息
        MallOrderInfo mallOrderInfo = this.selectById(orderId);
        //如何不是待收货状态就不允许确认收货
        if (!MallOrderStatusEnum.ORDER_STATUS_004.getCode().equals(mallOrderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.IS_NOT_TO_RECEVIED);
        }
        //1 更新订单状态
        mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
        mallOrderInfo.setUpdateDate(new Date());
        mallOrderInfo.setIsCanCancel("1");
        mallOrderInfo.setConfirmGoodsDate(new Date());
        mallOrderInfo.setCompleteDate(new Date());
        this.updateOrderById(mallOrderInfo);
        return true;
    }


    /**
     * 查询某个代理的订单总条数和订单总额
     *
     * @param userId
     */
    public OrderCountAndAmt queryCountAmtByUserId(String userId) {
        Wrapper<MallOrderInfo> eq = new EntityWrapper<MallOrderInfo>()
                .eq("mall_user_id", userId)
                .eq("is_del", "0")
                .eq("currency", "0")
                // 排除交易取消，交易关闭，退款中，已退款
                .notIn("order_status", new Object[]{"6", "7", "10", "11"})
                .in("order_type", new Object[]{"0", "1"});
        List<MallOrderInfo> list = this.selectList(eq);
        BigDecimal totalAmt = BigDecimal.ZERO;
        List<MallOrderInfo> listResult = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            totalAmt = list.stream().map(MallOrderInfo::getSummaryAmt).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        OrderCountAndAmt orderCountAndAmt = new OrderCountAndAmt();
        orderCountAndAmt.setMallUserId(userId);
        orderCountAndAmt.setCount(listResult.size());
        orderCountAndAmt.setTotalAmt(totalAmt);
        return orderCountAndAmt;
    }

    public OrderCountAndAmt queryTeamTotalAmt(List<String> userIds) {
        Wrapper<MallOrderInfo> eq = new EntityWrapper<MallOrderInfo>()
                .eq("is_del", "0")
                .eq("currency", "0")
                // 排除交易取消，交易关闭，退款中，已退款
                .notIn("order_status", new Object[]{"6", "7", "10", "11"})
                .in("order_type", new Object[]{"0", "1"})
                .in("mall_user_id", userIds);
        List<MallOrderInfo> list = this.selectList(eq);
        List<MallOrderInfo> listResult = new ArrayList<>();
        BigDecimal totalAmt = BigDecimal.ZERO;
        log.info("获取用户:{}的直发和入云的订单:{}", userIds, list);
       /*
        if (!CollectionUtils.isEmpty(list)) {
            list.stream().forEach(mallOrderInfo -> {
                //团队信息-过滤入云非直购订单
                if ("0".equals(mallOrderInfo.getOrderType())) {
                    //直接发货,不过滤
                    listResult.add(mallOrderInfo);
                } else {
                    //入云库存,直购 不过滤
                    if (mallOrderInfo.getPaymentAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                        listResult.add(mallOrderInfo);
                    }
                }
            });
            totalAmt = listResult.stream().map(MallOrderInfo::getSummaryAmt).reduce(BigDecimal.ZERO, BigDecimal::add);

        }*/
        if (!CollectionUtils.isEmpty(list)) {
            totalAmt = list.stream().map(MallOrderInfo::getSummaryAmt).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        OrderCountAndAmt orderCountAndAmt = new OrderCountAndAmt();
        orderCountAndAmt.setCount(listResult.size());
        orderCountAndAmt.setTotalAmt(totalAmt);
        return orderCountAndAmt;
    }

    /**
     * 特殊情况下调的推单
     */
    public void pushAllOrder() {
        List<String> orderIds = mallOrderInfoMapper.getOrderPush();
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        if (ObjectUtils.isNotNullAndEmpty(orderIds)) {
            log.info("---------------------------:{}", orderIds.size());
            for (String orderId : orderIds) {
                orderInfoMessage.setOrderId(orderId);
                orderInfoMessage.setOrderOrigin(0);
                orderDelayService.delayExpressListener(orderInfoMessage);
            }
        }
    }

    /**
     * 查询订单预警数量 传sku和用户id
     *
     * @param waringSkuParam
     * @return
     */
    public Integer getWaringSkuAmount(WaringSkuParam waringSkuParam) {
        int amt = 0;
        if (ObjectUtils.isNotNullAndEmpty(waringSkuParam.getWaringAmt())
                && waringSkuParam.getWaringAmt().compareTo(BigDecimal.ZERO) > 0) {
            amt = mallOrderInfoMapper.getWaringSku(waringSkuParam);
            if (ObjectUtils.isNullOrEmpty(amt)) {
                amt = 0;
            }
        }
        log.info("-------------------------预警:{}", amt);
        return amt;
    }


    /**
     * 2.0 查询订单总数
     *
     * @param userId
     * @return
     */
    public MallPersonalCenterDto queryPersonalOrderInfo(String userId) {
        MallAgent mallAgent = agentFeign.getAgentByUserId(userId);

        MallPersonalCenterDto personalCenterDto = new MallPersonalCenterDto();

        //1 进货
        //待支付
        Integer toInPay = getOrderCount(userId, "0", "0", Arrays.asList("1", "3"));
        personalCenterDto.setToInPay(toInPay);
        //待审核
        Integer toInVerify = getOrderCount(userId, "1", "0", Arrays.asList("1", "3"));
        personalCenterDto.setToInVerify(toInVerify);
        //代发货
        Integer toInDelivery = getOrderCount(userId, "3", "0", Arrays.asList("1", "3"));
        personalCenterDto.setToInDelivery(toInDelivery);
        //待收货
        Integer toInTakeOverGoods = getOrderCount(userId, "4", "0", Arrays.asList("1", "3"));
        personalCenterDto.setToInTakeOverGoods(toInTakeOverGoods);

        //2 出货
        //待支付
        Integer toOutPay = getOrderCount(userId, "0", "0", Arrays.asList("0", "2", "4"));
        personalCenterDto.setToOutPay(toOutPay);
        //待审核
        Integer toOutVerify = getOrderCount(userId, "8", "0", Arrays.asList("0", "2", "4"));
        personalCenterDto.setToOutVerify(toOutVerify);
        //待发货
        Integer toOutDelivery = getOrderCount(userId, "3", "0", Arrays.asList("0", "2", "4"));
        personalCenterDto.setToOutDelivery(toOutDelivery);
        //待收货
        Integer toOutTakeOverGoods = getOrderCount(userId, "4", "0", Arrays.asList("0", "2", "4"));
        personalCenterDto.setToOutTakeOverGoods(toOutTakeOverGoods);

        //3 积分
        //待支付
        Integer toCreditPay = getOrderCount(userId, "0", "1", Arrays.asList("0"));
        personalCenterDto.setToCreditPay(toCreditPay);
        //代发货
        Integer toCreditDelivery = getOrderCount(userId, "3", "1", Arrays.asList("0"));
        personalCenterDto.setToCreditDelivery(toCreditDelivery);
        //待收货
        Integer toCreditTakeOverGoods = getOrderCount(userId, "4", "1", Arrays.asList("0"));
        personalCenterDto.setToCreditTakeOverGoods(toCreditTakeOverGoods);

        //4 待审核
        Integer toHandleVerify = mallOrderInfoMapper.selectCount(new EntityWrapper<MallOrderInfo>()
                .eq("order_status", "1")
                .eq("currency", "0")
                .eq("leader_id", userId)
                .gt("create_date", "2018-12-01")
                .eq("is_del", '0'));
        personalCenterDto.setToHandleVerify(toHandleVerify);

        if (ObjectUtils.isNotNullAndEmpty(mallAgent)) {
            personalCenterDto.setIntoCompanyTime(mallAgent.getCreateDate());
        }
        return personalCenterDto;
    }


    private Integer getOrderCount(String userId, String orderStatus, String currency, List<String> orderType) {
        Integer result = mallOrderInfoMapper.selectCount(new EntityWrapper<MallOrderInfo>()
                .eq("mall_user_id", userId)
                .eq("order_status", orderStatus)
                .eq("currency", currency)
                .gt("create_date", "2018-12-01")
                .eq("is_del", '0')
                .eq("history_freight_status", "0")
                .in("order_type", orderType));
        return result;
    }


    public List<String> getOrderIdsByOrderType(String orderType) {
        List<MallOrderInfo> infos = this.selectList(
                new EntityWrapper<MallOrderInfo>()
                        .eq("order_type", orderType)
        );
        return infos.stream().map(MallOrderInfo::getOrderId).collect(Collectors.toList());
    }

    /**
     * 条件查询订单列表2.0
     *
     * @param param
     * @return
     */
    public Page<OrderInfoDetailDto> queryNewOrderInfoList(MallNewOrderParam param, Page<MallOrderInfo> page) {
        MallUser user = UserUtils.getCurrentUser();
        List<OrderInfoDetailDto> list = null;
        switch (param.getType()) {
            case 1: //进货
                list = getNewOrderInfos(param, user.getId(), page, Arrays.asList("1", "3"));
                break;
            case 2: //出货
                list = getNewOrderInfos(param, user.getId(), page, Arrays.asList("0", "2", "4"));
                break;
            case 3: //积分
                list = getNewOrderInfos(param, user.getId(), page, Arrays.asList("0"));
                break;
        }
        //排序
        List<OrderInfoDetailDto> sortList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            sortList = list.stream().sorted(Comparator.comparing(OrderInfoDetailDto::getCreateDate).reversed()).collect(Collectors.toList());
        }
        Page pageResult = new Page();
        BeanUtils.copyProperties(page, pageResult, "records");
        pageResult.setRecords(sortList);
        return pageResult;
    }

    private List<OrderInfoDetailDto> getNewOrderInfos(MallNewOrderParam param, String userId, Page<MallOrderInfo> page, List<String> orderTypes) {
        Wrapper<MallOrderInfo> eq = new EntityWrapper<MallOrderInfo>()
                .eq(!StringUtils.isEmpty(param.getBelongsCode()), "belongs_code", param.getBelongsCode())
                .eq(!StringUtils.isEmpty(param.getOrderId()), "order_id", param.getOrderId())
                .eq("mall_user_id", userId)
                .gt("create_date", "2019-01-01") //todo 19年之前的数据隐藏
                .eq("is_del", "0")
                .in("order_type", orderTypes)
                .eq("history_freight_status", "0")
                .orderBy("create_date", false);
        if (!"all".equals(param.getOrderStatus())) {
            if ("1".equals(param.getOrderStatus()) || "8".equals(param.getOrderStatus())) {
                eq = eq.in("order_status", Arrays.asList("1", "8"));
            } else {
                eq = eq.eq("order_status", param.getOrderStatus());
            }
        }
        page = this.selectPage(page, eq);
        List<OrderInfoDetailDto> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(page.getRecords())) {
            page.getRecords().forEach(p -> {
                OrderInfoDetailDto orderInfoDetailDto = new OrderInfoDetailDto();
                List<OrderItemDetailDto> dto = orderItemByOrderId(p.getOrderId(), 0);
                BigDecimal goodsAmount = BigDecimal.ZERO;

                //todo 因白米浮库存不足，白米浮可以转成200ml溶液
                boolean canTransferC036 = false;
                orderInfoDetailDto.setCanTransferC036(false);
                for (OrderItemDetailDto d : dto) {
                    if ("C036-Z".equals(d.getSkuCode().trim()) && checkValidTime(orderInfoDetailDto, p)) {
                        if ("3".equals(p.getOrderStatus())) {
                            canTransferC036 = true;
                        }
                    }
                    goodsAmount = goodsAmount.add(d.getAmount().abs());
                }
//                if ("3".equals(p.getOrderStatus()) && !"2".equals(p.getLogisticsMode())) {
                orderInfoDetailDto.setCanTransferC036(canTransferC036);
//                }

                String addrName = "";
                String addrPhone = "";
                String addrId = "";
                if (MallOrderTypeEnum.ORDER_TYPE_004.getCode().equals(p.getOrderType())) {
                    List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                            .eq("relation_id", p.getOrderId())
                            .eq("relation_type", "0"));
                    if (!CollectionUtils.isEmpty(mallTransferGoods)) {
                        String nextProxyId = mallTransferGoods.get(0).getNextProxyId();
                        MallUser userById = userFeign.getUserById(nextProxyId);
                        LoginDto dt = new LoginDto();
                        BeanMapper.copy(userById, dt);
                        dt.setRoleId(userById.getRoleId());
                        orderInfoDetailDto.setTransPortUser(dt);
                        addrName = userById.getName();
                        addrPhone = userById.getPhone();
//                        addrId = p.getOrderDescribe();
                    } else {
                        orderInfoDetailDto.setTransPortUser(null);
                    }
                } else if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(p.getOrderType())) {
                    //金额小计
                    BigDecimal inSubtotalPrice = new BigDecimal(0);
                    //金额小计
                    BigDecimal outSubtotalPrice = new BigDecimal(0);
                    if (!CollectionUtils.isEmpty(dto)) {
                        for (OrderItemDetailDto detailDto : dto) {
                            if (MallReviewEnum.ITEM_OUT_001.getCode().equals(detailDto.getType())) {
                                outSubtotalPrice = outSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            } else {
                                inSubtotalPrice = inSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            }
                        }
                    }
                    if (inSubtotalPrice.compareTo(outSubtotalPrice) > 0) {
                        orderInfoDetailDto.setPayExchangeAmt(inSubtotalPrice.subtract(outSubtotalPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
                    } else {
                        orderInfoDetailDto.setSendBackExchangeAmt(outSubtotalPrice.subtract(inSubtotalPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
                    }
                }
                BeanMapper.copy(p, orderInfoDetailDto);
                orderInfoDetailDto.setOrderStatus("11".equals(p.getOrderStatus()) ? "5" : p.getOrderStatus());
                if (ObjectUtils.isNullOrEmpty(p.getAddrId())) {
                    orderInfoDetailDto.setAddrName(addrName);
                    orderInfoDetailDto.setAddrPhone(addrPhone);
                    orderInfoDetailDto.setAddrId(addrId);
                }
                orderInfoDetailDto.setGoodsAmount(goodsAmount);
                //商品信息
                orderInfoDetailDto.setOrderItemDetailDtos(dto);
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getPayExchangeAmt())) {
                    orderInfoDetailDto.setPayExchangeAmt(new BigDecimal(0));
                }
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getSendBackExchangeAmt())) {
                    orderInfoDetailDto.setSendBackExchangeAmt(new BigDecimal(0));
                }
                //todo 商务要求不在APP展示他们后台的备注
                orderInfoDetailDto.setCsMemo("");
                if (!MallOrderTypeEnum.ORDER_TYPE_007.getCode().equals(p.getOrderType())) {
                    list.add(orderInfoDetailDto);
                }
            });
        }
        return list;
    }

    /**
     * 个人当月业绩
     *
     * @param userId
     * @return
     */
    public PersonalDTO queryUserMonthSales(String userId) {
        PersonalDTO result = new PersonalDTO();
        Date fristOfMonth = DateUtil.getFristOfMonth(0);
        Date lastOfMonth = DateUtil.getLastOfMonth(1);

        MallAgent agent = new MallAgent();
        agent.setUserId(userId);
        agent.setBeginTime(fristOfMonth);
        agent.setEndTime(lastOfMonth);
        Integer integer = agentFeign.queryUserUpgradeCount(agent);
        result.setPromotionGeneralAgent(ObjectUtils.isNotNullAndEmpty(integer) ? integer : 0);


        MallAgent info = new MallAgent();
        info.setBeginTime(fristOfMonth);
        info.setEndTime(lastOfMonth);
        info.setUserId(userId);

        //当月进货总金额
        BigDecimal sumOrderAmountByAgent = mallOrderInfoMapper.getSumOrderAmountByAgent(info);
        result.setUserMonthStockPrice(ObjectUtils.isNotNullAndEmpty(sumOrderAmountByAgent) ? sumOrderAmountByAgent : BigDecimal.valueOf(0.00));

        BigDecimal outOrderSumPrice = mallOrderInfoMapper.getOutOrderSumPrice(agent);
        result.setUserMonthClearPrice(ObjectUtils.isNotNullAndEmpty(outOrderSumPrice) ? outOrderSumPrice : BigDecimal.valueOf(0.00));

        BigDecimal group = new BigDecimal(0);
        BigDecimal branch = new BigDecimal(0);

        //进货
        List<MallOrderItem> personItems = mallOrderInfoMapper.getOrderByAgentList(agent);
        if (!CollectionUtils.isEmpty(personItems)) {
            for (MallOrderItem orderId : personItems) {
                if ("支".equals(orderId.getUnit())) {
                    branch = branch.add(orderId.getAmount().abs());
                } else {
                    group = group.add(orderId.getAmount().abs());
                }
            }
        }

        //支换组  3=1组
        if (ObjectUtils.isNotNullAndEmpty(branch)) {
            BigDecimal[] remainder = branch.divideAndRemainder(BigDecimal.valueOf(3));
            group = group.add(remainder[0]);
            branch = remainder[1];
        }
        result.setUserMonthStock(group);
        result.setUserMonthStockBranch(branch);
        result.setUserMonthSale(group);
        log.info("个人进货总数:{}", result);


        BigDecimal teamgroup = new BigDecimal(0);
        BigDecimal teambranch = new BigDecimal(0);
        //出货
        List<MallOrderItem> outPerson = mallOrderInfoMapper.getOutOrderItems(agent);
        if (!CollectionUtils.isEmpty(outPerson)) {
            for (MallOrderItem orderId : outPerson) {
                if ("盒".equals(orderId.getUnit()) || "组".equals(orderId.getUnit())) {
                    teamgroup = teamgroup.add(orderId.getAmount().abs());
                } else {
                    teambranch = teambranch.add(orderId.getAmount().abs());
                }
            }
        }
        if (ObjectUtils.isNotNullAndEmpty(teambranch)) {
            BigDecimal[] remainder = teambranch.divideAndRemainder(BigDecimal.valueOf(3));
            teamgroup = teamgroup.add(remainder[0]);
            teambranch = remainder[1];
        }
        result.setUserMonthClear(teamgroup);
        result.setUserMonthClearBranch(teambranch);


//        //进货统计
//        BigDecimal userMonthStock = BigDecimal.valueOf(0);
//        BigDecimal userMonthStockPrice = BigDecimal.valueOf(0.00);
//        List<MallOrderItem> mallOrderItems = mallOrderInfoMapper.queryUserMonthStock(info);
//        BigDecimal branch = BigDecimal.valueOf(0);
//        if (!CollectionUtils.isEmpty(mallOrderItems)) {
//            for (MallOrderItem item : mallOrderItems) {
//                //判断单位  3支 = 1组 1盒=1组
//                if ("支".equals(item.getUnit())) {
//                    branch = branch.add(item.getAmount().abs());
//                } else if ("箱".equals(item.getUnit())) {
//                    userMonthStock = userMonthStock.add(item.getAmount().abs().multiply(BigDecimal.valueOf(Long.valueOf(item.getSpec()))));
//                } else if ("盒".equals(item.getUnit()) || "组".equals(item.getUnit())) {
//                    userMonthStock = userMonthStock.add(item.getAmount().abs());
//                }
//
//                userMonthStockPrice = userMonthStockPrice.add(item.getAmount().abs().multiply(item.getPrice()));
//            }
//        }
//        result.setUserMonthStockPrice(userMonthStockPrice);
//
//        BigDecimal decimal = branch.divideToIntegralValue(BigDecimal.valueOf(3));
//        result.setUserMonthStock(userMonthStock.add(decimal));
//
//
//       // result.setUserMonthSale(userMonthStock.add(decimal));
//
//        //出货统计
//        BigDecimal saleBranch = BigDecimal.valueOf(0);
//        BigDecimal userMonthClear = BigDecimal.valueOf(0);
//        BigDecimal userMonthClearPrice = BigDecimal.valueOf(0);
//        List<MallOrderItem> saleItems = mallOrderInfoMapper.queryUserMonthSales(info);
//        if (!CollectionUtils.isEmpty(saleItems)) {
//            for (MallOrderItem item : saleItems) {
//                if ("支".equals(item.getUnit())) {
//                    saleBranch = saleBranch.add(item.getAmount().abs());
//                } else if ("箱".equals(item.getUnit())) {
//                    userMonthClear = userMonthClear.add(item.getAmount().abs().multiply(BigDecimal.valueOf(Long.valueOf(item.getSpec()))));
//                } else if ("盒".equals(item.getUnit()) || "组".equals(item.getUnit())) {
//                    userMonthClear = userMonthClear.add(item.getAmount().abs());
//                }
//                userMonthClearPrice = userMonthClearPrice.add(item.getAmount().abs().multiply(item.getPrice()));
//            }
//        }
//        BigDecimal clear = saleBranch.divideToIntegralValue(BigDecimal.valueOf(3));
//        result.setUserMonthClear(userMonthClear.add(clear));


        //  result.setUserMonthClearPrice(userMonthClearPrice);
        return result;
    }

    /**
     * 团队管理
     *
     * @param userId
     * @return
     */
    public TeamDTO queryTeamMonthSales(String userId) {
        TeamDTO result = new TeamDTO();
        Date fristOfMonth = DateUtil.getFristOfMonth(0);
        Date lastOfMonth = DateUtil.getLastOfMonth(1);

        MallAgent agentByUserId = agentFeign.getAgentByUserId(userId);
        if (ObjectUtils.isNullOrEmpty(agentByUserId)) {
            throw new MallException(OrderRespCode.NOT_AGENT);
        }

        //获取所有3级总代useriID
        List<String> stringList = agentFeign.getAllLowerUserIdByAgentId(agentByUserId.getId());
        log.info("团队信息中所有的userId:{}", stringList);
        if (CollectionUtils.isEmpty(stringList)) {
            stringList = new ArrayList<>(1);
            stringList.add(userId);
        }

        MallOrderInfo info = new MallOrderInfo();
        info.setDateFrom(fristOfMonth);
        info.setDateTo(lastOfMonth);
        info.setMallUserIdList(stringList);


        //进货统计
        BigDecimal teamMonthStock = BigDecimal.valueOf(0);
        BigDecimal teamMonthStockPrice = BigDecimal.valueOf(0.00);
        List<MallOrderItem> mallOrderItems = mallOrderInfoMapper.queryTeamMonthStock(info);
        log.info("查询到团队进货数据:{}", mallOrderItems);
        /* log.info("获取用户:{}的直发和入云的订单:{}", userIds, list);
        if (!CollectionUtils.isEmpty(list)) {
            list.stream().forEach(mallOrderInfo -> {
                //团队信息-过滤入云非直购订单
                if ("0".equals(mallOrderInfo.getOrderType())) {
                    //直接发货,不过滤
                    listResult.add(mallOrderInfo);
                } else {
                    //入云库存,直购 不过滤
                    if (mallOrderInfo.getPaymentAmt().compareTo(BigDecimal.valueOf(0)) > 0) {
                        listResult.add(mallOrderInfo);
                    }
                }
            });
            totalAmt = listResult.stream().map(MallOrderInfo::getSummaryAmt).reduce(BigDecimal.ZERO, BigDecimal::add);

        }*/


        BigDecimal branch = BigDecimal.valueOf(0);
        if (!CollectionUtils.isEmpty(mallOrderItems)) {
            for (MallOrderItem item : mallOrderItems) {
                //判断单位  3支 = 1组 1盒=1组
                if ("支".equals(item.getUnit())) {
                    branch = branch.add(item.getAmount().abs());
                } else if ("箱".equals(item.getUnit())) {
                    teamMonthStock = teamMonthStock.add(item.getAmount().abs().multiply(BigDecimal.valueOf(Long.valueOf(item.getSpec()))));
                } else if ("盒".equals(item.getUnit()) || "组".equals(item.getUnit())) {
                    teamMonthStock = teamMonthStock.add(item.getAmount().abs());
                }
                teamMonthStockPrice = teamMonthStockPrice.add(item.getAmount().abs().multiply(item.getPrice()));
            }
        }
        result.setTeamMonthStockPrice(teamMonthStockPrice);
        BigDecimal decimal = branch.divideToIntegralValue(BigDecimal.valueOf(3));
        result.setTeamMonthStock(teamMonthStock.add(decimal));
        log.info("团队进货总数:{}", result);

        //获取所有下级useriID
        List<String> allLowerUserIdByAgentId = agentFeign.getThreeLowerUserIdByAgentId(agentByUserId.getId());
        if (CollectionUtils.isEmpty(allLowerUserIdByAgentId)) {
            allLowerUserIdByAgentId = new ArrayList<>(1);
            allLowerUserIdByAgentId.add(userId);
        }
        info.setMallUserIdList(allLowerUserIdByAgentId);
        //出货统计
        BigDecimal saleBranch = BigDecimal.valueOf(0);
        BigDecimal teamMonthClear = BigDecimal.valueOf(0);
        BigDecimal teamMonthClearPrice = BigDecimal.valueOf(0);
        List<MallOrderItem> saleItems = mallOrderInfoMapper.queryTemMonthSales(info);
        if (!CollectionUtils.isEmpty(saleItems)) {
            for (MallOrderItem item : saleItems) {
                if ("支".equals(item.getUnit())) {
                    saleBranch = saleBranch.add(item.getAmount().abs());
                } else if ("箱".equals(item.getUnit())) {
                    teamMonthClear = teamMonthClear.add(item.getAmount().abs().multiply(BigDecimal.valueOf(Long.valueOf(item.getSpec()))));
                } else if ("盒".equals(item.getUnit()) || "组".equals(item.getUnit())) {
                    teamMonthClear = teamMonthClear.add(item.getAmount().abs());
                }
                teamMonthClearPrice = teamMonthClearPrice.add(item.getAmount().abs().multiply(item.getPrice()));
            }
        }
        BigDecimal clear = saleBranch.divideToIntegralValue(BigDecimal.valueOf(3));
        result.setTeamMonthClear(teamMonthClear.add(clear));
        result.setTeamMonthClearPrice(teamMonthClearPrice);

        Map<String, Integer> map = Maps.newHashMap();


        //地域分布
        MallRealnameAuth mallRealnameAuth = new MallRealnameAuth();
        mallRealnameAuth.setUserIds(allLowerUserIdByAgentId);
        List<String> areaByUserIds = userFeign.getAreaByUserIds(mallRealnameAuth);
        if (CollectionUtils.isEmpty(areaByUserIds)) {
            result.setAreas(map);
            return result;
        }
        for (String provice : Const.CHINAPROVICE) {
            map.put(provice, 0);
        }

        for (String provice : Const.CHINAPROVICE) {
            for (String areas : areaByUserIds) {
                if (areas.contains(provice)) {
                    Integer integer = map.get(provice);
                    map.put(provice, integer + 1);
                }
            }
        }


        result.setAreas(map);
        return result;
    }

    private static final String TASK_ORDER_ITEM = "task:order:item:";

    public List<ItemAndSkuDTO> getItemAndSku(String orderId) {
        String s = RedisUtil.get(TASK_ORDER_ITEM + orderId);
        if (!StringUtils.isEmpty(s)) {
            List<ItemAndSkuDTO> itemAndSkuDTOS = JSONUtil.json2list(s, ItemAndSkuDTO.class);
            return itemAndSkuDTOS;
        }
        return null;
    }


    public String downLoadExcel(HttpServletResponse response) throws Exception {

        HSSFWorkbook workbook = new HSSFWorkbook();

        //sheet页
        HSSFSheet sheet = workbook.createSheet("商品订单物流导入");

        //创建表头
        createDownTitle(workbook, sheet);
        String fileName = "商品订单物流导入.xls";

        //生成excel文件
        buildExcelFile(fileName, workbook);


        //浏览器下载excel
        buildExcelDocument(fileName, workbook, response);

        return "success";
    }

    /**
     * 创建表头
     *
     * @param workbook
     * @param sheet
     */
    public void createDownTitle(HSSFWorkbook workbook, HSSFSheet sheet) {
        HSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(3, 17 * 256);
        //设置为居中加粗
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        HSSFCell cell;
        cell = row.createCell(0);
        cell.setCellValue("订单编号");
        cell.setCellStyle(style);


        cell = row.createCell(1);
        cell.setCellValue("订单物流号");
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("物流公司");
        cell.setCellStyle(style);

    }


    @Transactional
    public String importExcel(InputStream inputStream) throws Exception {

        Sheet sheet = WorkbookFactory.create(inputStream).getSheetAt(0);

        List<HashMap<String, String>> list = new ArrayList<>();
        List<ItemOrderResultDTO> dtos = new ArrayList<>();
        if (sheet == null) {
            HashMap<String, String> map = new HashMap();
            map.put("000000", "糟了,文件传输失败 -_- ");
            list.add(map);
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("000000");
            dto.setReason("糟了,文件传输失败 -_-");
            dtos.add(dto);
        } else {
            log.info(sheet.getLastRowNum() + "");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    //如果是空行（即没有任何数据、格式），直接把它以下的数据往上移动
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    continue;
                }
                String orderId = ExcelUtil.getCellStringValue(row.getCell(0));
                try {
                    String expressCode = ExcelUtil.getCellStringValue(row.getCell(1));
                    String expressCompany = ExcelUtil.getCellStringValue(row.getCell(2));
                    MallOrderInfo info = mallOrderInfoMapper.selectById(orderId);
                    if (ObjectUtils.isNullOrEmpty(info)) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "没找到该订单");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("没找到该订单");
                        dtos.add(dto);
                        continue;
                    }

                    //校验状态
                    if (!MallOrderStatusEnum.ORDER_STATUS_003.getCode().equals(info.getOrderStatus())) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "状态不是待发货");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("状态不是待发货");
                        dtos.add(dto);
                        continue;
                    }
                    info.setExpressCompany(expressCompany);
                    info.setExpressCode(expressCode);
                    info.setDeliverGoodsDate(new Date());
                    info.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
                    mallOrderInfoMapper.updateById(info);
                } catch (Exception e) {
                    log.info("=========orderId有未知异常！！！:{}", orderId);
                    log.info("Exception:{}", e);
                    HashMap<String, String> map = new HashMap<>();
                    map.put(orderId, "未知异常");
                    list.add(map);
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(orderId);
                    dto.setReason("未知异常");
                    dtos.add(dto);
                }
            }
        }


        String key = UUID.randomUUID().toString();
        RedisUtil.set(MallOrderInfoService.key + key, JSONUtil.obj2json(dtos), 60 * 60);
        log.info("dtos:{}", dtos);
//        //处理结果集
//        ExcelUtil excelUtil = new ExcelUtil();
//        LinkedHashMap<String,String> map = new LinkedHashMap();
//        map.put("订单编号","orderId");
//        map.put("错误原因","reason");
//        String s = excelUtil.buildExcel(map, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return key;
    }


    public String getImport(String key, HttpServletResponse response) throws Exception {
        String s1 = RedisUtil.get(MallOrderInfoService.key + key);
        List<ItemOrderResultDTO> dtos = JSONUtil.json2list(s1, ItemOrderResultDTO.class);
        //处理结果集
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = new LinkedHashMap();
        map.put("订单编号", "orderId");
        map.put("错误原因", "reason");
        log.info("dtos:{}", dtos);
        String s = excelUtil.buildExcel(map, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;
    }


    public MallOrderInfo getFirstOutItemOrderInfo(String mallUserId) {
        return mallOrderInfoMapper.getFirstOutItemOrderInfo(mallUserId);
    }


    //填写的运费 > 用户订单已付运费：需要从代理的余额账户中扣除【 商务填写运费 -  订单中的运费】，且该运费产生的资金流水类型为【支付】，标题和备注为【补运费】。若代理月账户中费用不足，则确认时提示【代理资金账户余额不足，无法修改物流】，无法将数据插入进去。
    //填写的运费 = 用户订单已付运费：不做处理
    //填写的运费 < 用户订单已付运费：将补充代理余额账户金额【订单总的运费 - 商务填写的运费】，且该运费产生的资金流水类型为【退款】，标题和备注为【补运费】

    @Transactional
    @TxTransaction(isStart = true)
    public void setPostFee(MallOrderInfo mallOrderInfo, BigDecimal amt) {
        MallPayInfo payInfoByOrderId = payFeign.getPayInfoByOrderId(mallOrderInfo.getOrderId());
        if ("1".equals(payInfoByOrderId.getPayStatus())) {
            if (amt.compareTo(mallOrderInfo.getPostFeeAmt()) != 0) {
                BigDecimal subtract = mallOrderInfo.getPostFeeAmt().subtract(amt);
                MallUserAccountDto mallUserAccountDto = userFeign.accountInfo(mallOrderInfo.getMallUserId());
                if (subtract.compareTo(BigDecimal.valueOf(0)) < 0 && mallUserAccountDto.getAmt().compareTo(subtract.abs()) < 0) {
                    throw new MallException(OrderRespCode.ACCOUNT_NOT_ENOUGH);
                }
                UpdateAccountAmtParam param = new UpdateAccountAmtParam();
                param.setMallUerId(mallOrderInfo.getMallUserId());
                param.setAmount(subtract);
                userFeign.updateAccountAmt(param);
                MallJournalRecord record = new MallJournalRecord();
                record.setMallUserId(mallOrderInfo.getMallUserId());
                record.setRelevanceUserId("");
                record.setBillId(mallOrderInfo.getOrderId());
                record.setPayBefore(mallUserAccountDto.getAmt());
                record.setPayAmount(subtract);
                record.setPayAfter(mallUserAccountDto.getAmt().add(subtract));
                record.setCurrency("0");
                record.setTitle("补运费 ");
                record.setRemark("补运费 ");
                if (amt.compareTo(mallOrderInfo.getPostFeeAmt()) > 0) {
                    record.setBillType("3");
                } else {
                    record.setBillType("5");
                }
                payFeign.insertJournalRecord(record);
            }
        }
    }

    @Transactional
    public void forceJdArrive(String orderId) {
        if (ObjectUtils.isNotNullAndEmpty(orderId)) {
            MallOrderInfo mallOrderInfo = new MallOrderInfo();
            mallOrderInfo.setOrderId(orderId);
            mallOrderInfo.setIsCanJd("0");
            mallOrderInfoMapper.updateById(mallOrderInfo);
        }
    }

    /**
     * 订单报表统计
     *
     * @param year
     * @return
     */
    public OrderReportResponseDTO orderReport(String year) {
        OrderReportResponseDTO orderReportResponseDTO = new OrderReportResponseDTO();
        //年度进货总金额
        BigDecimal yearlyStockTotalAmount = new BigDecimal(0);
        //年度出货总金额
        BigDecimal yearlyShipmentTotalAmount = new BigDecimal(0);

        //"年度进货产品总数"
        BigDecimal yearlyStockOrderAmount = new BigDecimal(0);
        //"年度出货产品总数"
        BigDecimal yearlyShipmentOrderAmount = new BigDecimal(0);
        //1.统计进货
        // 【入云（直购）】且状态为【已完成】的订单金额和订单数量
        List<MonthlyOrderReportResponseDTO> list1 = setOrderRequestParam(year, MallOrderTypeEnum.ORDER_TYPE_001.getCode(), true);
        // 【直发】且状态为【待商务审核】【待发货】【待收货】【已完成】的订单金额（去除运费）和订单数量
        List<MonthlyOrderReportResponseDTO> list2 = setOrderRequestParam(year, MallOrderTypeEnum.ORDER_TYPE_000.getCode(), true);
        if (!CollectionUtils.isEmpty(list1) && !CollectionUtils.isEmpty(list2)) {
            for (MonthlyOrderReportResponseDTO monthOrderReport : list1) {
                for (MonthlyOrderReportResponseDTO report : list2) {
                    if (monthOrderReport.getMonth().equals(report.getMonth())) {
                        //月份相同,合并list1,list2的金额,数量
                        monthOrderReport.setMonthlyStockTotalAmount(monthOrderReport.getMonthlyStockTotalAmount().add(report.getMonthlyStockTotalAmount()));
                        monthOrderReport.setMonthlyStockTotalNumber(monthOrderReport.getMonthlyStockTotalNumber().add(report.getMonthlyStockTotalNumber()));
                        monthOrderReport.setMonthlyStockOrderNumber(monthOrderReport.getMonthlyStockOrderNumber().add(report.getMonthlyStockOrderNumber()));
                    }
                }
            }
        }
        //2.统计出货
        //【云库存提货】【直发】且订单状态为【已完成】【待收货】的订单金额（去除运费）和订单数量
        List<MonthlyOrderReportResponseDTO> list3 = setOrderRequestParam(year, null, false);
        if (!CollectionUtils.isEmpty(list1) && !CollectionUtils.isEmpty(list3)) {
            for (MonthlyOrderReportResponseDTO monthOrderReport : list1) {
                for (MonthlyOrderReportResponseDTO report : list3) {
                    if (monthOrderReport.getMonth().equals(report.getMonth())) {
                        //月份相同,合并出货进货
                        monthOrderReport.setMonthlyShipmentTotalAmount(report.getMonthlyStockTotalAmount());
                        monthOrderReport.setMonthlyShipmentTotalNumber(report.getMonthlyStockTotalNumber());
                        monthOrderReport.setMonthlyShipmentOrderNumber(report.getMonthlyStockOrderNumber());
                        yearlyStockTotalAmount = yearlyStockTotalAmount.add(monthOrderReport.getMonthlyStockTotalAmount());
                        yearlyShipmentTotalAmount = yearlyShipmentTotalAmount.add(monthOrderReport.getMonthlyShipmentTotalAmount());
                        yearlyStockOrderAmount = yearlyStockOrderAmount.add(monthOrderReport.getMonthlyStockTotalNumber());
                        yearlyShipmentOrderAmount = yearlyShipmentOrderAmount.add(monthOrderReport.getMonthlyShipmentTotalNumber());
                    }
                }
            }
        }

        //单独统计出货订单数量  因为有支的存在
        List<MonthlyOrderReportResponseDTO> listShipment = totalShipmentOrderAmount(year, null, false);
        if (!CollectionUtils.isEmpty(list1) && !CollectionUtils.isEmpty(listShipment)) {
            for (MonthlyOrderReportResponseDTO monthOrderReport : list1) {
                for (MonthlyOrderReportResponseDTO report : listShipment) {
                    if (monthOrderReport.getMonth().equals(report.getMonth())) {
                        //月份相同,合并出货进货
                        monthOrderReport.setMonthlyShipmentOrderNumber(report.getMonthlyShipmentOrderNumber());
                    }
                }
            }
        }

        orderReportResponseDTO.setYearlyStockTotalAmount(yearlyStockTotalAmount);
        orderReportResponseDTO.setYearlyShipmentTotalAmount(yearlyShipmentTotalAmount);
        orderReportResponseDTO.setYearlyShipmentOrderAmount(yearlyShipmentOrderAmount);
        orderReportResponseDTO.setYearlyStockOrderAmount(yearlyStockOrderAmount);
        orderReportResponseDTO.setMonthList(list1);
        //3.合并
        return orderReportResponseDTO;
    }

    /**
     * 根据不同的订单状态和类型,统计报表
     *
     * @param year
     * @param orderType
     * @param isStock
     * @return
     */
    private List<MonthlyOrderReportResponseDTO> setOrderRequestParam(String year, String orderType, Boolean isStock) {
        List<String> orderTypeList = new ArrayList<>();
        List<String> orderStatusList = new ArrayList<>();
        if (isStock) {
            //进货
            if (MallOrderTypeEnum.ORDER_TYPE_001.getCode().equals(orderType)) {
                // 【入云（直购）】且状态为【已完成】的订单金额和订单数量
                orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_001.getCode());
                orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
            } else if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(orderType)) {
                // 【直发】且状态为【待商务审核】【待发货】【待收货】【已完成】的订单金额（去除运费）和订单数量
                orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_000.getCode());
                orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_008.getCode());
                orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_003.getCode());
                orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
                orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
            }
        } else {
            //出货
            //【云库存提货】【直发】且订单状态为【已完成】【待收货】的订单金额（去除运费）和订单数量
            orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
            orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
            orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_000.getCode());
            orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_002.getCode());
        }

        OrderReportRequest report = new OrderReportRequest();
        report.setYear(year);
        report.setOrderTypeList(orderTypeList);
        report.setOrderStatusList(orderStatusList);
        log.info("订单报表统计入参:{}", report);
        List<MonthlyOrderReportResponseDTO> result = mallOrderInfoMapper.orderReportByParams(report);
        log.info("订单报表统计返回结果:{}", result);
        return result;
    }

    private List<MonthlyOrderReportResponseDTO> totalShipmentOrderAmount(String year, String orderType, Boolean isStock) {
        List<String> orderTypeList = new ArrayList<>();
        List<String> orderStatusList = new ArrayList<>();
        orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_000.getCode());
        orderTypeList.add(MallOrderTypeEnum.ORDER_TYPE_002.getCode());
        orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
        orderStatusList.add(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
        OrderReportRequest report = new OrderReportRequest();
        report.setYear(year);
        report.setOrderTypeList(orderTypeList);
        report.setOrderStatusList(orderStatusList);
        List<MonthlyOrderReportResponseDTO> result = mallOrderInfoMapper.orderReportByTotalAmount(report);
        return result;
    }


    /**
     * @Description 按下单时间统计订单中产品进出货的数据 统计各个商品的数量
     * @Author ChenXiang
     * @Date 2019-04-25 16:46:19
     * @ModifyBy
     * @ModifyDate
     **/
    public List<OrderItemReportDTO> queryOrderItemReport(BaseParam param) {
//        校验时间
        if (ObjectUtils.isNotNullAndEmpty(param.getBeginTime())) {
            param.setBeginTime(DateUtils.getBeforeHour(param.getBeginTime(), 8));
        }
        if (ObjectUtils.isNotNullAndEmpty(param.getEndTime())) {
            param.setEndTime(DateUtils.getAfterHour(param.getEndTime(), 16));
        }

//        查询进货产品详情 出货产品详情 1是进货 2是出货
        param.setOrderRule("1");//查询 1是进货
        List<OrderItemReportDTO> inDtos = mallOrderInfoMapper.selectOrderItemReport(param);

        param.setOrderRule("2");//查询  2是出货
        List<OrderItemReportDTO> outDtos = mallOrderInfoMapper.selectOrderItemReport(param);

        for (OrderItemReportDTO inDto : inDtos) {
            OrderItemReportDTO tempOutDto = outDtos.stream().filter(q -> q.getSkuCode().equals(inDto.getSkuCode())).findFirst().orElse(null);
            if (ObjectUtils.isNotNullAndEmpty(tempOutDto)) {
                inDto.setOutputCount(tempOutDto.getOutputCount());
                inDto.setOutputAmt(tempOutDto.getOutputAmt());
            } else {
                inDto.setOutputCount(0);
                inDto.setOutputAmt(BigDecimal.ZERO);
            }
            MallSku sku = itemFeign.getSkuByCode(inDto.getSkuCode());
            inDto.setTitle(sku.getTitle());
            inDto.setUnit(sku.getUnit());
        }

        List<String> skuCodeList = inDtos.stream().map(OrderItemReportDTO::getSkuCode).collect(Collectors.toList());
        for (OrderItemReportDTO outDto : outDtos) {
            if (!skuCodeList.contains(outDto.getSkuCode())) {
                outDto.setInputCount(0);
                outDto.setInputAmt(BigDecimal.ZERO);

                MallSku sku = itemFeign.getSkuByCode(outDto.getSkuCode());
                outDto.setTitle(sku.getTitle());
                outDto.setUnit(sku.getUnit());

                inDtos.add(outDto);
            }
        }

        return inDtos;
    }

    /**
     * @Description 按下单时间统计订单中产品进出货的数据   统计总数量
     * @Author ChenXiang
     * @Date 2019-04-25 16:46:19
     * @ModifyBy
     * @ModifyDate
     **/
    public HashMap queryOrderItemDetailReport(BaseParam param) {
        //        校验时间
        if (ObjectUtils.isNotNullAndEmpty(param.getBeginTime())) {
            param.setBeginTime(DateUtils.getBeforeHour(param.getBeginTime(), 8));
        }
        if (ObjectUtils.isNotNullAndEmpty(param.getEndTime())) {
            param.setEndTime(DateUtils.getAfterHour(param.getEndTime(), 16));
        }
        Integer inputCount = mallOrderInfoMapper.selectTotalInputOrderItemCount(param);
        Integer outputCount = mallOrderInfoMapper.selectTotalOutputOrderItemCount(param);
        HashMap<String, Integer> retMap = new HashMap<>();
        retMap.put("inputCount", inputCount);
        retMap.put("outputCount", outputCount);
        return retMap;
    }

    /**
     * @Description 导出产品进出货统计报表的数据
     * @Author ChenXiang
     * @Date 2019-04-26 16:46:19
     * @ModifyBy
     * @ModifyDate
     **/
    public String doOutputOrderItemReport(BaseParam param, HttpServletResponse response) throws Exception {
        List<OrderItemReportDTO> orderItemReportDTOs = this.queryOrderItemReport(param);
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("产品名称", "title");
        titleMap.put("进货数量", "inputCount");
        titleMap.put("进货总金额", "inputAmt");
        titleMap.put("出货数量", "outputCount");
        titleMap.put("出货总金额", "outputAmt");
        excelUtil.buildExcel(titleMap, orderItemReportDTOs, ExcelUtil.DEFAULT_ROW_MAX, response);
        return "download excel";
    }

    /**
     * 产品订单统计报表导出
     *
     * @param response
     * @return
     * @throws Exception
     */
    public String exportOrderReport(String year, HttpServletResponse response) throws Exception {
        OrderReportResponseDTO orderReportResponseDTO = this.orderReport(year);
        if (null != orderReportResponseDTO) {
            ExcelUtil excelUtil = new ExcelUtil();
            LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
            titleMap.put("月份", "month");
            titleMap.put("进货金额", "monthlyStockTotalAmount");
            titleMap.put("进货订单数量", "monthlyStockOrderNumber");
            titleMap.put("进货产品数量", "monthlyStockTotalNumber");
            titleMap.put("出货金额", "monthlyShipmentTotalAmount");
            titleMap.put("出货订单数量", "monthlyShipmentOrderNumber");
            titleMap.put("出货产品数量", "monthlyShipmentTotalNumber");
            excelUtil.buildExcel(titleMap, orderReportResponseDTO.getMonthList(), ExcelUtil.DEFAULT_ROW_MAX, response);
            return "success";
        } else {
            return "false";
        }

    }

    /**
     * @Description 产品进出货统计报表，统计总的进货/出货数量,及转化率
     * @Author ChenXiang
     * @Date 2019-05-06 15:41:15
     * @ModifyBy
     * @ModifyDate
     **/
    public Map<String, Integer> inputOutputItemStatisticsReport(BaseParam param) {
        HashMap<String, Integer> retMap = new HashMap<>();
        if (ObjectUtils.isNullOrEmpty(param.getBeginTime())) {
            param.setBeginTime(DateUtils.getBeforeHour(param.getBeginTime(), 8));
        }
        if (ObjectUtils.isNullOrEmpty(param.getEndTime())) {
            param.setEndTime(DateUtils.getAfterHour(param.getEndTime(), 16));
        }
        Integer inputItemCount = mallOrderInfoMapper.selectInputItem(param);
        Integer outputItemCount = mallOrderInfoMapper.selectOutputItem(param);
        retMap.put("inputItemCount", inputItemCount);
        retMap.put("outputItemCount", outputItemCount);
        return retMap;
    }

    /**
     * @Description 产品进出货统计报表, 产品进出货统计报表, 详细内容
     * @Author ChenXiang
     * @Date 2019-05-06 15:54:46
     * @ModifyBy
     * @ModifyDate
     **/
    public List<InputOutputItemReportDTO> inputOutputItemStatisticsReportDetail(BaseParam param) {
        if (ObjectUtils.isNullOrEmpty(param.getBeginTime())) {
            param.setBeginTime(DateUtils.getBeforeHour(param.getBeginTime(), 8));
        }
        if (ObjectUtils.isNullOrEmpty(param.getEndTime())) {
            param.setEndTime(DateUtils.getAfterHour(param.getEndTime(), 16));
        }
        List<InputOutputItemReportDTO> dtos = mallOrderInfoMapper.selectOutputItemDetail(param);
        for (InputOutputItemReportDTO dto : dtos) {
            MallSku mallSku = itemFeign.getSkuByCode(dto.getSkuCode());
            if (ObjectUtils.isNotNullAndEmpty(mallSku)) {
                dto.setSkuTitle(mallSku.getTitle());
                dto.setSkuUnit(mallSku.getUnit());
            }
        }
        return dtos;
    }

    /**
     * @Description 产品进出货统计报表, 下载产品进出货统计报表,
     * @Author ChenXiang
     * @Date 2019-05-06 16:43:04
     * @ModifyBy
     * @ModifyDate
     **/
    public String inputOutputItemStatisticsReportDetailDownload(BaseParam param, HttpServletResponse response) throws Exception {
        List<InputOutputItemReportDTO> dtos = this.inputOutputItemStatisticsReportDetail(param);

        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("产品名称", "skuTitle");
        titleMap.put("进货总量", "inputCount");
        titleMap.put("实际进货数量", "inputRealCount");
        titleMap.put("换货差数量", "exchangeCount");
        titleMap.put("进货总金额", "inputAmt");
        titleMap.put("出货数量", "outputCount");
        titleMap.put("出货总金额", "outputAmt");
        excelUtil.buildExcel(titleMap, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return "download excel";
    }

    public String downLoadJDExcel(HttpServletResponse response) throws Exception {

        String currenty = DateUtils.getDate("yyyy-MM-dd 00:00:00");
        Date beginDate = DateUtils.addDays(DateUtils.parseDate(currenty), -7);
        Date endDate = DateUtils.addDays(DateUtils.parseDate(currenty), 1);

        List<MallOrderInfo> mallOrderInfos = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("order_status", "3")
                .eq("is_can_jd", "1")
                .eq("is_del", "0")
                .gt("create_date", beginDate)
                .lt("create_date", endDate)
        );
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("订单号", "orderId");
        titleMap.put("收货地址", "addrId");
        titleMap.put("京东(0=可达，1=不可达)", "isCanJd");
        excelUtil.buildExcel(titleMap, mallOrderInfos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return "download excel success";
    }

    public List<ItemOrderResultDTO> importJDExcel(ByteArrayInputStream inputStream) throws Exception {
        Sheet sheet = WorkbookFactory.create(inputStream).getSheetAt(0);

        List<ItemOrderResultDTO> dtos = new ArrayList<>();
        if (ObjectUtils.isNullOrEmpty(sheet)) {
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("000000");
            dto.setReason("文件传输失败");
            dtos.add(dto);
        } else {
            int lastRowNum = sheet.getLastRowNum();
            log.info(lastRowNum + "");
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    //如果是空行（即没有任何数据、格式），直接把它以下的数据往上移动
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    continue;
                }
                String orderId = ExcelUtil.getCellStringValue(row.getCell(0));
                log.info("========================================》orderId:" + orderId);
                try {
                    String isCanJd = ExcelUtil.getCellStringValue(row.getCell(2));
                    log.info("========================================》isCanJd:" + isCanJd);

                    MallOrderInfo info = this.selectById(orderId);
                    if (ObjectUtils.isNullOrEmpty(info)) {
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("没找到该订单");
                        dtos.add(dto);
                        continue;
                    }
                    //校验状态
                    if (!MallOrderStatusEnum.ORDER_STATUS_003.getCode().equals(info.getOrderStatus())) {
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("状态不是待发货");
                        dtos.add(dto);
                        continue;
                    }
                    if ("0".equals(isCanJd)) {
                        info.setIsCanJd("0");
                        boolean ret = updateOrderById(info);
                        if (ret) {
                            OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
                            orderInfoMessage.setOrderId(orderId);
                            orderInfoMessage.setOrderOrigin(0);
                            orderDelayService.delayExpressListener(orderInfoMessage);
                        }
                    }
                } catch (Exception e) {
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(orderId);
                    dto.setReason("未知异常");
                    dtos.add(dto);
                }
            }
        }
        return dtos;
    }


    /**
     * 根据订单号 仓库发货
     *
     * @param orderId
     * @param company
     * @param code
     * @return
     */
    @Transactional
    public Boolean sendByStore(String orderId, String company, String code) {
        MallOrderInfo orderInfo = mallOrderInfoMapper.selectOrderById(orderId);
        orderInfo.setOrderStatus("4");
        orderInfo.setExpressCode(code);
        orderInfo.setIsCanCancel("1");
        orderInfo.setExpressCompany(company);
        Integer affectedRows = mallOrderInfoMapper.updateById(orderInfo);
        //取消审核单
        cancelVerifyOrderInfo(orderId);
        return SqlHelper.retBool(affectedRows);

    }

    public List<MallOrderInfo> queryOrderListByParam(MallOrderInfo param) {
        List<MallOrderInfo> orderList = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("is_del", "0")
                .eq(!StringUtils.isEmpty(param.getMallUserId()), "mall_user_id", param.getMallUserId())
                .eq(!StringUtils.isEmpty(param.getRelationOrderId()), "relation_order_id", param.getRelationOrderId())
                .eq("belongs_code", "0")
                .orderBy("create_date", true)
        );
        if (!CollectionUtils.isEmpty(orderList)) {
            orderList.forEach(orderInfo -> {
                List<MallOrderItem> itemList = orderItemMapper.selectList(new EntityWrapper<MallOrderItem>()
                        .eq("is_del", "0")
                        .eq("order_id", orderInfo.getOrderId())
                );
                List<OrderItemDetailDto> itemDetailDtoList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(itemList)) {
                    itemList.forEach(orderItem -> {
                        OrderItemDetailDto dto = OrderItemDetailDto.builder()
                                .orderId(orderItem.getOrderId())
                                .itemId(orderItem.getItemId())
                                .skuCode(orderItem.getSkuCode())
                                .amount(orderItem.getAmount())
                                .price(orderItem.getPrice())
                                .type(orderItem.getType())
                                .unit(orderItem.getUnit())
                                .createDate(orderItem.getCreateDate())
                                .build();
                        itemDetailDtoList.add(dto);
                    });
                }
                orderInfo.setOrderItemDetailDtos(itemDetailDtoList);
            });
        }
        return orderList;
    }

    /**
     * 后台查询入云单
     *
     * @param mallUserId
     * @return
     */
    public List<MallOrderItem> queryOrderBackground(String mallUserId) {
        List<MallOrderInfo> mallOrderInfos = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("is_del", "0")
                .eq("mall_user_id", mallUserId)
                .eq("order_type", "1")
                .eq("payment_amt", 0)
        );
        List<MallOrderItem> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mallOrderInfos)) {
            mallOrderInfos.forEach(order -> {
                List<MallOrderItem> itemList = orderItemMapper.selectList(new EntityWrapper<MallOrderItem>()
                        .eq("is_del", "0")
                        .eq("order_id", order.getOrderId())
                );
                MallOrderItem item = new MallOrderItem();
                if (!CollectionUtils.isEmpty(itemList)) {
                    BigDecimal reduce = itemList.stream().map(MallOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    item.setAmount(reduce);
                    item.setOrderId(order.getOrderId());
                    item.setCreateDate(order.getCreateDate());
                    list.add(item);
                }
            });
        }
        return list;
    }

    /**
     * 所有仓库订单列表
     *
     * @param param
     * @return
     */
    public PageDto<AllOrderInfoDto> queryWarehouseOrderInfo(GetOrderPageListParam param) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);

        if (!CollectionUtils.isEmpty(param.getOrderStatusList())) {
            mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        } else {
            mallOrderInfo.setOrderStatusList(Arrays.asList("3", "4", "5"));
        }

        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());

        //先判断是否需要查询user表
        mallOrderInfo.setMallUserIdList(null);
        //三个条件任意不为空,都需要查询user表
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            List<MallUser> userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                return null;
            } else {
                mallOrderInfo.setMallUserIdList(userByInput.stream().map(MallUser::getId).collect(Collectors.toList()));
            }
        }
        return queryWarehouseOrderInfo(mallOrderInfo);
    }

    /**
     * 所有仓库订单列表
     *
     * @param mallOrderInfo
     * @return
     */
    public PageDto<AllOrderInfoDto> queryWarehouseOrderInfo(MallOrderInfo mallOrderInfo) {
        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());

        List<MallOrderInfo> list = mallOrderInfoMapper.queryWarehouseOrderInfo(mallOrderInfo, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<AllOrderInfoDto> dtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            for (MallOrderInfo info : list) {
                AllOrderInfoDto dto = new AllOrderInfoDto();
                BeanMapper.copy(info, dto);
                //用户信息
                dto.setMallUser(MallUser.builder().name(info.getName()).nickName(info.getNickName()).phone(info.getPhone()).roleId(info.getRoleId()).build());
                //商品
                List<OrderItemDetailDto> itemList = queryOrderItem4Warehouse(info.getOrderId());
                dto.setMallSku(itemList);
                //商品数量
                BigDecimal goodsAmount = itemList.stream().map(OrderItemDetailDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setGoodsAmount(goodsAmount.intValue());
                //所属公司
                dto.setCompanyName(getCompanyName(info.getCompanyId(), info.getMallUserId()));
                dtos.add(dto);
            }
        }
        PageDto pageDto = new PageDto();
        pageDto.setTotal(page.getTotal());
        pageDto.setRecords(dtos);
        return pageDto;
    }

    private List<OrderItemDetailDto> queryOrderItem4Warehouse(String orderId) {
        List<OrderItemDetailDto> list = new ArrayList<>();
        List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                new EntityWrapper<MallOrderItem>()
                        .eq("order_id", orderId)
                        .eq("is_del", "0")
        );
        if (CollectionUtils.isEmpty(mallOrderItems)) {
            return list;
        }
        for (MallOrderItem orderItem : mallOrderItems) {
            if (orderItem.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            MallSku sku = ItemCacheUtils.getSkuByCode(orderItem.getSkuCode());
            OrderItemDetailDto dto = OrderItemDetailDto.builder()
                    .orderId(orderItem.getOrderId())
                    .itemId(orderItem.getItemId())
                    .skuCode(orderItem.getSkuCode())
                    .amount(orderItem.getAmount().abs())
                    .price(orderItem.getPrice())
                    .credit(orderItem.getCredit())
                    .currency(orderItem.getCurrency())
                    .skuImg(sku.getSkuImg())
                    .type(orderItem.getType())
                    .title(sku.getTitle())
                    .waringStatus("0")
                    .unit(sku.getUnit())
                    .spec(sku.getSpec())
                    .build();
            list.add(dto);
        }
        return list;
    }

    public List<MallAcOrder> getAcOrderEclpSoNo() {
        return mallOrderInfoMapper.getAcOrderEclpSoNo();
    }


    public Integer getNowDaysC036New(String userId) {
        Integer count = mallOrderInfoMapper.getNowDaysC036New(userId);
        return count == null ? 0 : count;
    }

    public Integer getNowDaysC036(String userId) {
        Integer count = mallOrderInfoMapper.getNowDaysC036(userId);
        return count == null ? 0 : count;
    }

    private void checkNowDaysC036(String userId, List<MallCart> mallCarts, String orderType) {
        List<String> list = Arrays.asList("1221668351816163328", "1221350986145828864");
        if (list.contains(userId)) {
            return;
        }
        MallUser user = userFeign.getUserByIdFromFeign(userId);
        if (!Arrays.asList("0", "2").contains(orderType) || !"4".equals(user.getRoleId())) {
            return;
        }
        Date dateThree = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateThree = sdf.parse("2020-01-20 00:00:00");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (user.getUpgradeDate().after(dateThree)) {
            BigDecimal total = mallCarts.stream().filter(p -> "C036-Z".equals(p.getSkuCode()))
                    .map(MallCart::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal t = total == null ? BigDecimal.ZERO : total.multiply(BigDecimal.valueOf(3));
            log.info("白米浮限制数量------------------:" + t);
            if (t.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
            Integer count = getNowDaysC036New(userId);
            log.info("已出货的白米浮限制数量------------------:" + count);
            if ((t.intValue() + count) > 45) {
                throw new MallException("020049");
            }
        }
    }

    /**
     * 2020-01-27号临时推单
     */
    public void temporaryPushToJd(TemPushToJd temPushToJd) {
        String userIds = RedisUtil.get("temporaryNotPushUserId");
        temPushToJd.setPhones(null);
        if (!StringUtils.isEmpty(userIds)) {
            List<String> list = JsonUtils.jsonToList(userIds, String.class);
            temPushToJd.setPhones(list);
        }
        List<String> orderIds = mallOrderInfoMapper.temporaryPushToJd(temPushToJd);
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        if (ObjectUtils.isNotNullAndEmpty(orderIds)) {
            log.info("---------------------------2020-01-27号临时推单:{}", orderIds.size());
            for (String orderId : orderIds) {
                orderInfoMessage.setOrderId(orderId);
                orderInfoMessage.setOrderOrigin(0);
                orderInfoMessage.setIsAdmin("0");
                orderDelayService.delayExpressListener(orderInfoMessage);
            }
        }
    }

    /**
     * 出货订单查询（用于批量推单）
     *
     * @param param
     * @return
     */
    public PageDto<QueryPushOrderInfoDto> queryOrderToPush(GetOrderPageListParam param) {
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        List<QueryPushOrderInfoDto> result = mallOrderInfoMapper.queryOrderToPush(param, page);
        if (!CollectionUtils.isEmpty(result)) {
            log.info("========result.size:{}", result.size());
            List<String> collect = result.stream().map(QueryPushOrderInfoDto::getOrderId).collect(Collectors.toList());
            MallOrderInfo mallOrderInfo = new MallOrderInfo();
            mallOrderInfo.setMallUserIdList(collect);
            List<OrderItemDetailDto> itemDetailDtoList = mallOrderInfoMapper.queryOrderItemDetail(mallOrderInfo);


            if (!CollectionUtils.isEmpty(itemDetailDtoList)) {
                Map<String, List<OrderItemDetailDto>> map = itemDetailDtoList.stream().collect(Collectors.groupingBy(OrderItemDetailDto::getOrderId));
                for (QueryPushOrderInfoDto order : result) {
                    List<OrderItemDetailDto> items = map.get(order.getOrderId());
                    if (!CollectionUtils.isEmpty(items)) {
                        BigDecimal reduce = items.stream().map(OrderItemDetailDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                        order.setGoodsAmount(reduce.intValue());
                        order.setMallSku(items);
                    }
                }
            }
        }

        PageDto pageResult = new PageDto();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }

    @Transactional
    @RedisLock(key = "orderId")
    public void transferC036(GetOrderInfo getOrderInfo) {
        MallOrderInfo orderInfo = this.selectById(getOrderInfo.getOrderId());

        if (!"3".equals(orderInfo.getOrderStatus())) {
            throw new MallException("020059");
        }
        String s = RedisUtil.get("isCanTransferC036:orderId_" + getOrderInfo.getOrderId());
        if (s == null) {
            RedisUtil.set("isCanTransferC036:orderId_" + getOrderInfo.getOrderId(), "0");
        } else {
            if ("5".equals(s.trim())) {
                throw new MallException("070095");
            }
        }
        if (getOrderInfo.getCanTransferC036() == null) {
            getOrderInfo.setCanTransferC036(1);
        }
        if (getOrderInfo.getCanTransferC036() == 1 && "2".equals(orderInfo.getLogisticsMode())) {
            return;
        }
        if (getOrderInfo.getCanTransferC036() == 2 && "0".equals(orderInfo.getLogisticsMode())) {
            return;
        }
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(getOrderInfo.getOrderId());
        mallOrderInfo.setLogisticsMode("2");
        if (getOrderInfo.getCanTransferC036() == 1) {
            mallOrderInfo.setLogisticsMode("2");
        }
        if (getOrderInfo.getCanTransferC036() == 2) {
            mallOrderInfo.setLogisticsMode("0");
        }
//        mallOrderInfo.setSystemMemo("白米浮已转转规格发货");
        this.updateById(mallOrderInfo);
        RedisUtil.incr("isCanTransferC036:orderId_" + getOrderInfo.getOrderId(), 1);

    }

    @Transactional
    public void revokeTransferC036(String orderId) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(orderId);
        mallOrderInfo.setLogisticsMode("0");
        mallOrderInfo.setSystemMemo("白米浮已转转规格发货(已撤销)");
        this.updateById(mallOrderInfo);
    }

    public String downloadWarehouseOrderInfo(GetOrderPageListParam param, HttpServletResponse response) throws Exception {

        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());
        if (!CollectionUtils.isEmpty(param.getOrderStatusList())) {
            mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        } else {
            mallOrderInfo.setOrderStatusList(Arrays.asList("3", "4", "5"));
        }
        List<MallUser> userByInput = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                ExcelUtil excelUtil = new ExcelUtil();
                LinkedHashMap<String, String> map = getMap();
                String s = excelUtil.buildExcel(map, new ArrayList(), ExcelUtil.DEFAULT_ROW_MAX, response);
                return s;
            }
        }
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userByInput)) {
            for (MallUser user : userByInput) {
                userIdList.add(user.getId());
            }
            mallOrderInfo.setMallUserIdList(userIdList);
        }
        List<MallOrderInfo> infos = mallOrderInfoMapper.doWarehouseExcel(mallOrderInfo);
        infos = detailInfos(infos);
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = getMap();
        String s = excelUtil.buildExcel(map, infos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;

    }

    @Transactional
    public void downloadWarehouseOrderInfoToJD(GetOrderPageListParam param, HttpServletResponse response) throws Exception {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());
//        if (!CollectionUtils.isEmpty(param.getOrderStatusList())) {
//            mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
//        } else {
//            mallOrderInfo.setOrderStatusList(Collections.singletonList("3"));
//        }
        List<MallUser> userByInput = null;
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
            userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                xiong.utils.ExcelUtil.writeExcel(response, new ArrayList<>(), "京东面单推单", "京东面单推单", ExcelTypeEnum.XLS, JDPushExcelModel.class);
                return;
            }
        }
        List<String> userIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userByInput)) {
            for (MallUser user : userByInput) {
                userIdList.add(user.getId());
            }
            mallOrderInfo.setMallUserIdList(userIdList);
        }
        List<MallOrderInfo> infos = mallOrderInfoMapper.doWarehouseExcelToJD(mallOrderInfo);

        Map<String, List<MallOrderInfo>> listMap = infos.stream().collect(Collectors.groupingBy(MallOrderInfo::getOrderId));
        List<JDPushExcelModel> list = new ArrayList<>();
        listMap.forEach((k, v) -> {
            StringBuilder remark = new StringBuilder();
            for (MallOrderInfo p : v) {
                remark.append(new BigDecimal(p.getItemNumber()).abs()).append(p.getTitle()).append("\r\n");
            }
            list.add(JDPushExcelModel.builder()
                    .p1(k)
                    .p3(v.get(0).getAddrName())
                    .p4(v.get(0).getAddrPhone())
                    .p5(v.get(0).getAddrPhone())
                    .p6(v.get(0).getAddrId())
                    .p8("普通")
                    .p9("其他")
                    .p11("1")
                    .p15("特惠送")
                    .p19("否")
                    .p26(remark.toString())
                    .build());
            MallOrderInfo orderInfo = new MallOrderInfo();
            orderInfo.setOrderId(k);
            orderInfo.setIsCanCancel("1");
            orderInfo.setProhibitEdit("1");
            orderInfo.setUpdateDate(new Date());
            mallOrderInfoMapper.updateById(orderInfo);
        });
        xiong.utils.ExcelUtil.writeExcel(response, list, "京东面单推单", "京东面单推单", ExcelTypeEnum.XLS, JDPushExcelModel.class);
    }

    public OrderCheckPostFeeDto checkOrderPostFee(CheckPostFeeParam checkPostFeeParam) {

        MallUserAddress userAddress = userFeign.queryAddressById(checkPostFeeParam.getAddrId());

        List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                new EntityWrapper<MallOrderItem>().eq("is_del", "0").eq("order_id", checkPostFeeParam.getOrderId())
        );

        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        skuSpecAmount.setProvince(userAddress.getProvice());
        List<SkuAndAmountParam> list = new ArrayList<>();

        for (MallOrderItem item : mallOrderItems) {
            String skuCode = item.getSkuCode();
            String amount = item.getAmount().abs().toString();
            if (!"0".equals(amount)) {
                list.add(SkuAndAmountParam.builder().skuCode(skuCode).amount(amount).build());
            }
        }
        skuSpecAmount.setAmountParams(list);
        BigDecimal newPostFeeAmt = itemFeign.getPostFee(skuSpecAmount);

        MallOrderInfo info = selectById(checkPostFeeParam.getOrderId());
        BigDecimal orderPostFeeAmt = info.getPostFeeAmt();

        OrderCheckPostFeeDto orderCheckPostFeeDto = new OrderCheckPostFeeDto();
        orderCheckPostFeeDto.setChangeFlag(false);
        if (orderPostFeeAmt.compareTo(newPostFeeAmt) > 0) {
            BigDecimal amt = orderPostFeeAmt.subtract(newPostFeeAmt);
            orderCheckPostFeeDto.setChangeFlag(true);
            orderCheckPostFeeDto.setMsg("将退运费" + amt + "元到您的余额。");
        } else if (orderPostFeeAmt.compareTo(newPostFeeAmt) < 0) {
            BigDecimal amt = newPostFeeAmt.subtract(orderPostFeeAmt);
            orderCheckPostFeeDto.setChangeFlag(true);
            orderCheckPostFeeDto.setMsg("将从您的余额中额外扣除运费" + amt + "元，请保证余额充足。");
        }
        return orderCheckPostFeeDto;
    }

    @Transactional
    @TxTransaction
    @RedisLock(key = "orderId")
    public Boolean updateOrderAddress(CheckPostFeeParam checkPostFeeParam) {
        String id = checkPostFeeParam.getOrderId();
        MallOrderInfo info = mallOrderInfoMapper.selectById(id);
        if (!ORDER_STATUS_003.getCode().equals(info.getOrderStatus())) {
            throw new MallException("020061", new Object[]{id});
        }
        if ("2".equals(info.getLogisticsMode())) {
            throw new MallException("020040", new Object[]{"当前物流方式不支持修改。"});
        }

        MallUserAddress userAddress = userFeign.queryAddressById(checkPostFeeParam.getAddrId());
        List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(
                new EntityWrapper<MallOrderItem>().eq("is_del", "0").eq("order_id", checkPostFeeParam.getOrderId())
        );
        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        skuSpecAmount.setProvince(userAddress.getProvice());
        List<SkuAndAmountParam> list = new ArrayList<>();
        for (MallOrderItem item : mallOrderItems) {
            String skuCode = item.getSkuCode();
            String amount = item.getAmount().abs().toString();
            if (!"0".equals(amount)) {
                list.add(SkuAndAmountParam.builder().skuCode(skuCode).amount(amount).build());
            }
        }
        skuSpecAmount.setAmountParams(list);
        BigDecimal newPostFeeAmt = itemFeign.getPostFee(skuSpecAmount);

        BigDecimal orderPostFeeAmt = info.getPostFeeAmt();
        if (orderPostFeeAmt.compareTo(newPostFeeAmt) != 0) {
            BigDecimal subtract = orderPostFeeAmt.subtract(newPostFeeAmt);
            MallUserAccountDto mallUserAccountDto = userFeign.accountInfo(info.getMallUserId());
            if (subtract.compareTo(BigDecimal.valueOf(0)) < 0 && mallUserAccountDto.getAmt().compareTo(subtract.abs()) < 0) {
                throw new MallException(OrderRespCode.ACCOUNT_NOT_ENOUGH);
            }
            UpdateAccountAmtParam param = new UpdateAccountAmtParam();
            param.setMallUerId(info.getMallUserId());
            param.setAmount(subtract);
            userFeign.updateAccountAmt(param);
            MallJournalRecord record = new MallJournalRecord();
            record.setMallUserId(info.getMallUserId());
            record.setRelevanceUserId("");
            record.setBillId(info.getOrderId());
            record.setPayBefore(mallUserAccountDto.getAmt());
            record.setPayAmount(subtract);
            record.setPayAfter(mallUserAccountDto.getAmt().add(subtract));
            record.setCurrency("0");
            record.setTitle("订单修改收货地址，调整运费差额");
            record.setRemark("订单修改收货地址，调整运费差额 ");
            if (subtract.compareTo(BigDecimal.valueOf(0)) > 0) {
                record.setBillType("2");
            } else {
                record.setBillType("3");
            }
            payFeign.insertJournalRecord(record);
        }

        info.setAddrId(userAddress.getArea() + " " + userAddress.getFullAddress());
        info.setProvincialUrbanArea(userAddress.getArea());
        info.setStreet(userAddress.getFullAddress());
        info.setAddrName(userAddress.getName());
        info.setAddrPhone(userAddress.getPhone());
        info.setUpdateDate(new Date());
        return retBool(mallOrderInfoMapper.updateById(info));
    }

    public PageDto<AllOrderInfoDto> splitOrderQuery(SplitOrderQueryParam param) {

        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setPageCurrent(param.getPageCurrent());
        mallOrderInfo.setPageSize(param.getPageSize());
        mallOrderInfo.setBeginTime(param.getBeginTime());
        mallOrderInfo.setEndTime(param.getEndTime());
        //先判断是否需要查询user表
        mallOrderInfo.setMallUserIdList(null);
        //三个条件任意不为空,都需要查询user表
        if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getName(), param.getPhone()))) {
            List<MallUser> userByInput = userFeign.getUserByInput(null, param.getName(), param.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userByInput)) {
                return null;
            } else {
                mallOrderInfo.setMallUserIdList(userByInput.stream().map(MallUser::getId).collect(Collectors.toList()));
            }
        }
        return querySplitOrderInfo(mallOrderInfo);
    }

    public PageDto<AllOrderInfoDto> querySplitOrderInfo(MallOrderInfo mallOrderInfo) {

        List<String> orderIds;
        if ("0".equals(mallOrderInfo.getSplitFlag())) {
            orderIds = mallOrderInfoMapper.querySplitOrderIds(mallOrderInfo);
        } else {
            orderIds = mallOrderInfoMapper.querySplitOrderIds2(mallOrderInfo);
        }
        if (ObjectUtils.isNullOrEmpty(orderIds)) return null;

        Page page = MybatisPageUtil.getPage(mallOrderInfo.getPageCurrent(), mallOrderInfo.getPageSize());
        List<MallOrderInfo> list = mallOrderInfoMapper.querySplitOrderInfo(orderIds, page);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<AllOrderInfoDto> dtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            for (MallOrderInfo info : list) {
                AllOrderInfoDto dto = new AllOrderInfoDto();
                BeanMapper.copy(info, dto);
                //用户信息
                dto.setMallUser(MallUser.builder().name(info.getName()).nickName(info.getNickName()).phone(info.getPhone()).roleId(info.getRoleId()).build());
                //商品
                List<OrderItemDetailDto> itemList = queryOrderItem4Warehouse(info.getOrderId());
                dto.setMallSku(itemList);
                //商品数量
                BigDecimal goodsAmount = itemList.stream().map(OrderItemDetailDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setGoodsAmount(goodsAmount.intValue());
                //所属公司
                dto.setCompanyName(getCompanyName(info.getCompanyId(), info.getMallUserId()));
                dtos.add(dto);
            }
        }
        PageDto pageDto = new PageDto();
        pageDto.setTotal(page.getTotal());
        pageDto.setRecords(dtos);
        return pageDto;
    }

    @Transactional
    @TxTransaction
    public Boolean doSplitOrder(OrderSplitParam param) {
        List<String> orderIds = param.getOrderIds();
        List<String> itemIds = getRelationItem(param.getItemId());
        for (String orderId : orderIds) {
            doSplitOrderDetail(orderId, itemIds);
        }
        return true;
    }

    private List<String> getRelationItem(String itemId) {
        List<MallSku> allSku = itemFeign.getAllSku();
        MallSku mallSku = allSku.stream().filter(q -> q.getItemId().equals(itemId)).findFirst().get();
        List<String> itemIds = new ArrayList<>();
        itemIds.add(itemId);
//                allSku.stream().filter(q -> q.getRelationSku().equals(mallSku.getRelationSku())).map(MallSku::getItemId).collect(Collectors.toList());
        if ("C034-H".equals(mallSku.getRelationSku())) {//美浮特皮肤抗菌液C034_H 配 鞋套 P001
            MallSku p001 = allSku.stream().filter(q -> q.getSkuCode().equals("P001")).findFirst().get();
            itemIds.add(p001.getItemId());
        } else if ("C035-H".equals(mallSku.getRelationSku())) {//菲嘉皮肤抗菌液C035_H 配 指套 P002
            MallSku p002 = allSku.stream().filter(q -> q.getSkuCode().equals("P002")).findFirst().get();
            itemIds.add(p002.getItemId());
        }
        return itemIds;
    }

    private void doSplitOrderDetail(String orderId, List<String> splitItemIds) {
        //拆单 拆item 拆pay 拆运费
        MallOrderInfo info = selectById(orderId);
        if (!ORDER_STATUS_003.getCode().equals(info.getOrderStatus())) {
            throw new MallException("020040", new Object[]{"订单号【" + orderId + "】的订单不是【待发货】状态。"});
        }
        //订单详情
        List<MallOrderItem> mallOrderItems = orderItemMapper.selectList(new EntityWrapper<MallOrderItem>()
                .eq("is_del", "0")
                .eq("order_id", orderId)
        );
        List<MallOrderItem> mallOrderItem2 = mallOrderItems.stream().filter(q -> splitItemIds.contains(q.getItemId())).collect(Collectors.toList());
        List<MallOrderItem> mallOrderItem3 = mallOrderItems.stream().filter(q -> !splitItemIds.contains(q.getItemId())).collect(Collectors.toList());
        if (ObjectUtils.isNotNullAndEmpty(mallOrderItem2) && ObjectUtils.isNotNullAndEmpty(mallOrderItem3)) {

            MallOrderInfo info2 = new MallOrderInfo(); //拆 商品
            MallOrderInfo info3 = new MallOrderInfo(); //其他商品
            BeanUtils.copyProperties(info, info2);
            BeanUtils.copyProperties(info, info3);
            info2.setOrderId(IDUtils.genOrderId());
            info3.setOrderId(IDUtils.genOrderId());

            BigDecimal item2Amt = BigDecimal.ZERO;
            BigDecimal item3Amt = BigDecimal.ZERO;
            for (MallOrderItem mallOrderItem : mallOrderItem2) {
                item2Amt = item2Amt.add(mallOrderItem.getAmount().multiply(mallOrderItem.getPrice()).abs());
                mallOrderItem.setOrderId(info2.getOrderId());
                orderItemMapper.updateSplitCloudStockLog(info2.getOrderId(), mallOrderItem.getId(), info.getOrderId());
            }
            for (MallOrderItem mallOrderItem : mallOrderItem3) {
                item3Amt = item3Amt.add(mallOrderItem.getAmount().multiply(mallOrderItem.getPrice()).abs());
                mallOrderItem.setOrderId(info3.getOrderId());
                orderItemMapper.updateSplitCloudStockLog(info3.getOrderId(), mallOrderItem.getId(), info.getOrderId());
            }
            BigDecimal originAmt2 = BigDecimal.ZERO;
            BigDecimal originAmt3 = BigDecimal.ZERO;
            BigDecimal postFeeAmt2 = BigDecimal.ZERO;
            BigDecimal postFeeAmt3 = BigDecimal.ZERO;

            if (info.getOriginAmt().compareTo(BigDecimal.ZERO) > 0) {
                originAmt2 = item2Amt;
                originAmt3 = item3Amt;
            }
            if (info.getPostFeeAmt().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalItemAmt = item2Amt.add(item3Amt);
                postFeeAmt2 = info.getPostFeeAmt().multiply(item2Amt).divide(totalItemAmt, 2, BigDecimal.ROUND_HALF_UP);
                postFeeAmt3 = info.getPostFeeAmt().multiply(item3Amt).divide(totalItemAmt, 2, BigDecimal.ROUND_HALF_UP);
            }

            info2.setPaymentAmt(originAmt2.add(postFeeAmt2));
            info2.setOriginAmt(originAmt2);
            info2.setPostFeeAmt(postFeeAmt2);
            info2.setSplitFlag("1");
            info2.setSummaryAmt(info2.getPaymentAmt());

            info3.setPaymentAmt(originAmt3.add(postFeeAmt3));
            info3.setOriginAmt(originAmt3);
            info3.setPostFeeAmt(postFeeAmt3);
            info3.setSplitFlag("1");
            info3.setSummaryAmt(info3.getPaymentAmt());
            info3.setLogisticsMode("0".equals(info.getLogisticsMode()) ? "2" : "0");

            info.setIsDel("1");
            info.setSplitFlag("1");
            info.setRemark1("该订单已被拆单，子订单有" + info2.getOrderId() + "," + info3.getOrderId());
            info.setUpdateDate(new Date());
            insertOrUpdateBatch(Arrays.asList(info, info2, info3));

            mallOrderItem2.addAll(mallOrderItem3);
            mallOrderItemService.updateBatchById(mallOrderItem2);

            MallPayInfo payInfo = payFeign.getPayInfoByOrderId(orderId);

            MallPayInfo payInfo2 = new MallPayInfo();
            MallPayInfo payInfo3 = new MallPayInfo();
            BeanUtils.copyProperties(payInfo, payInfo2);
            BeanUtils.copyProperties(payInfo, payInfo3);

            payInfo2.setPayId(IDUtils.genId());
            payInfo2.setOrderId(info2.getOrderId());
            payInfo2.setTotalAmt(info2.getPaymentAmt());
            payInfo2.setBalancePaymentAmt(info2.getPaymentAmt());
            payInfo2.setUpdateDate(new Date());
            payInfo3.setPayId(IDUtils.genId());
            payInfo3.setOrderId(info3.getOrderId());
            payInfo3.setTotalAmt(info3.getPaymentAmt());
            payInfo3.setBalancePaymentAmt(info3.getPaymentAmt());
            payInfo3.setUpdateDate(new Date());

            payInfo.setIsDel("1");
            payInfo.setMemo("改支付单已经被拆单，子单为:" + payInfo2.getPayId() + "," + payInfo3.getPayId());
            payFeign.updatePayInfo(payInfo);
            payFeign.insertPayInfo(payInfo2);
            payFeign.insertPayInfo(payInfo3);


        }
    }


    /**
     * 后台订单详情查看物流信息
     *
     * @param orderId
     * @return
     */
    public List<MallLogisticsInfoDTO> queryItemPackageByOrderId(String orderId) {
        List<MallLogisticsInfoDTO> result = new ArrayList<>();

        MallOrderInfo mallOrderInfo = selectById(orderId);
        mallOrderInfo.getExpressCode();
        String itemsPackage = mallOrderInfo.getItemsPackage();
        if (!StringUtils.isEmpty(itemsPackage)) {
            String[] split = itemsPackage.split(";");

        }

        return null;
    }


    // 通过模版导入更新京东物流
    public String readJDBillWay(MultipartFile file) {
        List<ItemOrderResultDTO> dtos = new ArrayList<>();
        List<JDImportModel> excel = null;
        try {
            excel = xiong.utils.ExcelUtil.readExcel(file, JDImportModel.class);
        } catch (Exception e) {
            log.info("导入导出excel异常" + e);
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("00001");
            dto.setReason("糟了,文件传输失败（文件格式错误或为空信息） -_-");
            dtos.add(dto);
        }

        if (excel == null || excel.size() == 0) {
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("0000001");
            dto.setReason("糟了,文件传输失败（文件格式错误或为空信息） -_-");
            dtos.add(dto);
        } else {
            excel.forEach(p -> {
                if (StringUtils.isEmpty(p.getP3())) {
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(p.getP2());
                    dto.setReason("运单号为空");
                    dtos.add(dto);
                    return;
                }

                log.info("===================12:{}", p.getP2());
                MallOrderInfo info = mallOrderInfoMapper.selectById(p.getP2().trim());
                log.info("===================13:{}", info);
                if (info == null) {
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(p.getP2());
                    dto.setReason("没找到该订单");
                    dtos.add(dto);
                    return;
                }

                //校验状态
                if (!MallOrderStatusEnum.ORDER_STATUS_003.getCode().equals(info.getOrderStatus())) {
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(p.getP2());
                    dto.setReason("订单状态不是待发货（或该笔订单已发货）");
                    dtos.add(dto);
                    return;
                }

                try {
                    MallOrderInfo orderInfo = new MallOrderInfo();
                    orderInfo.setOrderId(p.getP2().trim());
                    orderInfo.setExpressCode(p.getP3().trim());
                    orderInfo.setExpressCompany("京东物流");
                    orderInfo.setOrderStatus("4");
                    orderInfo.setDeliverGoodsDate(new Date());
                    orderInfo.setRemark1("仓库发货");
                    this.updateOrderByIdNew(orderInfo);
                } catch (Exception e) {
                    log.info("通过模版导入更新京东物流失败", e);
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(p.getP2());
                    dto.setReason("更新运单号失败");
                    dtos.add(dto);
                }
            });
        }
        String key = UUID.randomUUID().toString();
        if (dtos.size() == 0) {
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("00000");
            dto.setReason("更新成功，无异常信息～");
            dtos.add(dto);
        }
        RedisUtil.set(MallOrderInfoService.key + key, JSONUtil.obj2json(dtos), 60 * 60);
        return key;
    }

    public Boolean sendEMail(String flag) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minus = now.minusDays(1L);
        String dateTo = now.getYear() + "-" + now.getMonthValue() + "-" + now.getDayOfMonth() + " 17:00:00";
        String dateTo12 = now.getYear() + "-" + now.getMonthValue() + "-" + now.getDayOfMonth() + " 12:00:00";
        String dateFrom = minus.getYear() + "-" + minus.getMonthValue() + "-" + minus.getDayOfMonth() + " 17:00:00";

        LocalDateTime threeTime = LocalDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 15, 00, 00);
        if (now.compareTo(threeTime) > 0) {
            //12-17邮件
            dateFrom = dateTo12;
        } else {
            //前一天5点 到 今天12点
            dateTo = dateTo12;
        }

        InputStream inputStream = null;
        InputStream inputStream2 = null;
        ExcelUtil excelUtil = new ExcelUtil();

        log.info("时间范围:{}--:{}", dateFrom, dateTo);
        List<QueryShipmentMoreThan20Dto> result = mallOrderInfoMapper.queryShipmentMoreThan10(dateFrom, dateTo);
        if (!CollectionUtils.isEmpty(result)) {
            Map<String, List<QueryShipmentMoreThan20Dto>> collect = result.stream().collect(Collectors.groupingBy(QueryShipmentMoreThan20Dto::getPhone));
            result.forEach(o -> {
                BigDecimal reduce = collect.get(o.getPhone()).stream().map(QueryShipmentMoreThan20Dto::getStock).reduce(BigDecimal.ZERO, BigDecimal::add);
                o.setTotalStock(reduce);
            });

            result = result.stream().sorted(Comparator.comparing(QueryShipmentMoreThan20Dto::getStock).reversed()).sorted(Comparator.comparing(QueryShipmentMoreThan20Dto::getTotalStock).reversed()).collect(Collectors.toList());
            LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
            titleMap.put("订单时间", "orderDateStr");
            titleMap.put("订单号", "orderId");
            titleMap.put("姓名", "name");
            titleMap.put("手机号", "phone");
            titleMap.put("出货量", "stock");
            titleMap.put("产品名称", "title");
            inputStream = excelUtil.buildExcelGetInputStream(titleMap, result, 5000, "order");
        }

        //附件2
        try {
            List<DisorderlyPriceOrderInfo> disorderlyPriceOrderInfos = setMailAcct2(dateFrom, dateTo);
            log.info("附件二内容:{}", disorderlyPriceOrderInfos);
            if (!CollectionUtils.isEmpty(disorderlyPriceOrderInfos))
                inputStream2 = excelUtil.buildExcelGetInputStream(setTitle(), disorderlyPriceOrderInfos, 5000, "order");

        } catch (Exception e) {
            log.info("构建附件二内容时异常++++++++++++++:{0}", e);
        }
        InternetAddress address1 = new InternetAddress();
        address1.setAddress("wangzhenpeng@meifute.com");
        InternetAddress address2 = new InternetAddress();
        address2.setAddress("weixiuxiang@meifute.com");
        InternetAddress address3 = new InternetAddress();
        address3.setAddress("luoye@meifute.com");
        InternetAddress address4 = new InternetAddress();
        address4.setAddress("yufen@meifute.com");
        InternetAddress address5 = new InternetAddress();
        address5.setAddress("zhaoxin@meifute.com");
        InternetAddress address6 = new InternetAddress();
        address6.setAddress("dengmingjie@meifute.com");
        InternetAddress address7 = new InternetAddress();
        address7.setAddress("zhengzijuan@meifute.com");
        InternetAddress address8 = new InternetAddress();
        address8.setAddress("gaoji@meifute.com");
        InternetAddress address9 = new InternetAddress();
        address9.setAddress("suxin@meifute.com");
        InternetAddress[] address = null;
        String ip = InetAddress.getLocalHost().getHostAddress();
        log.info("=============服务器IP:{}", ip);
        if (!StringUtils.isEmpty(flag)) {
            log.info("邮件只发给我");
            address = new InternetAddress[]{address1};
        } else if ("client".equals(profiles) || "local".equals(profiles)) {
            address = new InternetAddress[]{address1, address8};
        } else if ("prod-a".equals(profiles) || "prod-b".equals(profiles)) {
            address = new InternetAddress[]{address1, address2, address3, address4, address5, address6, address7, address9};
        }

        if (null != inputStream || null != inputStream2) {
            boolean sendMail = SendMail.sendMail(address, "当日提货>=10明细表",
                    "领导们好，附件是【当日提货>=10 明细表】，请注意查收，谢谢！", inputStream, inputStream2);
            log.info("发送邮件结果:{}", sendMail);
        }

        return true;
    }

    private LinkedHashMap<String, String> setTitle() {
        LinkedHashMap<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("乱价人姓名", "name");
        titleMap.put("乱价人手机号", "phone");
        titleMap.put("重复地址", "repeatAddr");
        titleMap.put("重复收货手机号", "repeatPhone");
        titleMap.put("当天重复下单人姓名", "repeatName");
        titleMap.put("当天重复的订单号", "repeatOrderId");
        titleMap.put("乱价人的订单号", "historyOrderId");
        return titleMap;
    }

    //获取附件2信息
    private List<DisorderlyPriceOrderInfo> setMailAcct2(String dateFrom, String dateTo) {
        List<DisorderlyPriceOrderInfo> result = new ArrayList<>();
        //违规人员userId
        String userIds = "1249212429063663616,1107619719555293184,1222356463507927040,1145667044974260224,1222347836819431424," +
                "1251847553492168704,1221336849981804544,1222024227885608960,1222056381114671104,1231067535177895936,1262561641422880768," +
                "1237969512046481408,1226165158289309696,1252462103199461376,211848301522194432,1256599523578757120,1241389499062939648," +
                "1223538589422669824,1262016047437271040,1224154147646595072,1222441450151890944,1220184310200680448,1247089921580855296," +
                "1262024184282173440,1148923857702162432,1232872745613348864,1248562237363933184,184641362979717120,1239473885989785600," +
                "1256572628253790208,1263398690338078720,1173765996893458432,1235202890449784832,1222009899358810112,1276708894924308480," +
                "1245309156333449216,1226433258750443520,219746740088078336,1221786624578740224,1221756180151853056,1267305250151956480," +
                "1221724766398980096,1251713049820872704,1260236683623460864,1260167478421966848,1251827854858731520,1239812349507620864," +
                "1109425320048439296,1272548472345051136,1227573257361379328,1253534147759718400,1174339623804227584,1222426456966176768," +
                "1222450699443511296,1246794303692075008,1248192533151608832,1260138076765351936,1270554860966330368,1233739973917364224," +
                "1221261772284137472,1221713914673373184,1238026002245570560,97246011398815744,1134055395301261312,1232590932891136000," +
                "1238437573418225664,202681004173824000,198803465412030464,1256470124614635520,1239118719469965312,1222082479746904064," +
                "1222017261855502336";
        String[] split = userIds.split(",");
        List<String> userIdList = new ArrayList<>();
        userIdList.addAll(Arrays.asList(split));
        userIdList.add("1106385899210731520");//添加测试环境的账号
//        userIdList.add("225873190830747648");//添加测试环境的账号 这个上生产要去掉

        MallUser user = new MallUser();
        user.setUserIds(userIdList);
        List<MallUser> mallUsers = userFeign.queryUserByParams(user);

        List<String> orderType = new ArrayList<>();
        orderType.add("0");
        orderType.add("2");
        //1.查询昨天17点到今天17点 的所有出货类型订单  直发+提货 ,状态不需要筛选
        List<MallOrderInfo> ownOrderList = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>().eq("is_del", "0")
                .in("order_type", orderType)
                .ge("create_date", dateFrom)
                .le("create_date", dateTo));
        log.info("所有出货订单:{}", ownOrderList);

        List<String> collect = ownOrderList.stream().map(MallOrderInfo::getMallUserId).collect(Collectors.toList());
        MallUser user1 = new MallUser();
        user1.setUserIds(collect);
        List<MallUser> mallUsers1 = userFeign.queryUserByParams(user1);

        //2.查询违规的这部分总代的所有出货订单
        List<MallOrderInfo> orderInfoList = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>().in("mall_user_id", userIdList)
                .in("order_type", orderType));
        log.info("乱加人的出货订单:{}", orderInfoList);

        orderInfoList = orderInfoList.stream().filter(o -> !StringUtils.isEmpty(o.getAddrId()) && !StringUtils.isEmpty(o.getAddrPhone())).collect(Collectors.toList());
        orderInfoList.forEach(o -> o.setAddrId(o.getAddrId().trim()));

        Map<String, List<MallOrderInfo>> addIds = orderInfoList.stream().collect(Collectors.groupingBy(MallOrderInfo::getAddrId));
        Map<String, List<MallOrderInfo>> addPhone = orderInfoList.stream().collect(Collectors.groupingBy(MallOrderInfo::getAddrPhone));

        //3.比较收货地址,手机号
        for (int i = 0; i < ownOrderList.size(); i++) {
            MallOrderInfo own = ownOrderList.get(i);
            if (addPhone.keySet().contains(own.getAddrPhone().trim())) {
                log.info("收货人电话相同");
                setTempOrderInfo(result, addPhone.get(own.getAddrPhone()), own, "phone", mallUsers, mallUsers1);
                continue;

            }
            if (addIds.keySet().contains(own.getAddrId().trim())) {
                log.info("收货地址相同");
                setTempOrderInfo(result, addIds.get(own.getAddrId()), own, "address", mallUsers, mallUsers1);
                continue;
            }
        }

        return result;
    }


    private void setTempOrderInfo(List<DisorderlyPriceOrderInfo> tempRepOrderList, List<MallOrderInfo> specList, MallOrderInfo allOrderInfo, String info, List<MallUser> mallUsers, List<MallUser> mallUsers1) {
        if (!CollectionUtils.isEmpty(specList)) {
            MallOrderInfo o = specList.get(0);
            MallUser user = mallUsers.stream().filter(u -> u.getId().equals(o.getMallUserId())).collect(Collectors.toList()).get(0);
            DisorderlyPriceOrderInfo orderInfo = new DisorderlyPriceOrderInfo();
            orderInfo.setName(user.getName());
            orderInfo.setPhone(user.getPhone());
            if ("phone".equals(info))
                orderInfo.setRepeatPhone(allOrderInfo.getAddrPhone());
            if ("address".equals(info))
                orderInfo.setRepeatAddr(allOrderInfo.getAddrId());
            MallUser user1 = mallUsers1.stream().filter(u -> u.getId().equals(allOrderInfo.getMallUserId())).collect(Collectors.toList()).get(0);
            orderInfo.setRepeatName(user1.getName());
            orderInfo.setRepeatOrderId(allOrderInfo.getOrderId());
            orderInfo.setHistoryOrderId(o.getOrderId());
            orderInfo.setRepeatUserPhone(user1.getPhone());//重复下单人手机号
            tempRepOrderList.add(orderInfo);
        }
    }

    public List<String> getOrderIdsByPayInfoRefundParam(PayInfoRefundVO param) {
        return mallOrderInfoMapper.getOrderIdsByPayInfoRefundParam(param);
    }

    public List<UserOrderAmountDTO> queryOrderAmountByDate(String startDate, String endDate, String yestodayDate) {
        List<UserOrderAmountDTO> nowList = mallOrderInfoMapper.queryOrderAmountByDate(startDate, endDate);
        if (CollectionUtils.isEmpty(nowList)) {
            return Lists.newArrayList();
        }
        List<UserOrderAmountDTO> yestodayList = mallOrderInfoMapper.queryOrderAmountByDate(startDate, yestodayDate);
        if (CollectionUtils.isEmpty(yestodayList)) {
            return nowList;
        }
        fillRankValue(nowList, yestodayList);
        return nowList;
    }

    private void fillRankValue(List<UserOrderAmountDTO> nowList, List<UserOrderAmountDTO> yestodayList) {
        nowList.sort((v1, v2) -> v2.getAmount().compareTo(v1.getAmount()));
        yestodayList.sort((v1, v2) -> v2.getAmount().compareTo(v1.getAmount()));
        int rank = 1;
        for (UserOrderAmountDTO nowDto : nowList) {
            nowDto.setRank(rank++);
            int yestodayRank = 0;
            for (UserOrderAmountDTO yestodayDto : yestodayList) {
                yestodayRank++;
                if (StringUtils.equals(nowDto.getUserId(), yestodayDto.getUserId())) {
                    nowDto.setYestodayAmount(yestodayDto.getAmount());
                    nowDto.setYestodayRank(yestodayRank);
                    break;
                }
            }
        }
    }

    public List<UserOrderAmountDTO> queryOrderAmountByUserIds(UserIdsDateParam param) {
        return mallOrderInfoMapper.queryOrderAmountByUserIds(param);
    }

    public List<String> getOrderIdsByStockInfoParam(String userId, String param) {
        return mallOrderInfoMapper.getOrderIdsByStockInfoParam(userId, param);
    }
}
