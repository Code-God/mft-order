package com.meifute.core.service;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.exception.ONSClientException;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.config.rocketmq.RocketMQConfig;
import com.meifute.core.dto.*;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.*;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.MallOrderVerifyStatusEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.enums.MallTeamEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.jpush.JpushUtil;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.AliExpress;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.PriceUtil;
import com.meifute.core.util.DESUtil;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.*;
import com.meifute.core.vo.notify.QueueParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Classname SMSMarketActivityService
 * @Description 短信营销活动
 * @Date 2019-11-19 14:29
 * @Created by MR. Xb.Wu
 */
@Slf4j
@Service
public class SMSMarketActivityService extends ServiceImpl<SMSAcOrderVerifyMapper, SMSAcOrderVerify> {

    @Autowired
    private PayFeign payFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private MallOrderInfoMapper orderInfoMapper;
    @Autowired
    private MallOrderItemMapper orderItemMapper;
    @Autowired
    private SMSRecordHitsMapper smsRecordHitsMapper;
    @Autowired
    private SMSAcOrderItemMapper smsAcOrderItemMapper;
    @Autowired
    private SMSAcOrderVerifyMapper smsAcOrderVerifyMapper;
    @Autowired
    private RocketMQConfig rocketMQConfig;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private MallSMSAcEvaluateMapper smsAcEvaluateMapper;
    @Value("${sms_market_url}")
    private String sms_market_url;


    /**
     * 页面初始化时调用，第一次点击链接时记录点击量
     *
     * @param smsRecordHits
     * @return
     */
    @RedisLock(key = "phone")
    public void recordHits(SMSRecordHits smsRecordHits) {
        // 解密
        String pid = DESUtil.decrypt(smsRecordHits.getPid());
        String phone = DESUtil.decrypt(smsRecordHits.getPhone());
        // 校验并记录点击量
        if (!checkRecord(phone, pid)) {
            smsRecordHitsMapper.insert(SMSRecordHits.builder()
                    .id(IDUtils.genId())
                    .pid(pid)
                    .phone(phone)
                    .state(smsRecordHits.getState())
                    .createTime(new Date())
                    .build());
        }
    }

    /**
     * 初始化地址信息
     *
     * @param phone
     */
    public UserAddressDto initAddress(String phone) {
        String validPhone = DESUtil.decrypt(phone);
        checkValidParam("check", validPhone);
        //获取最近的地址
        return userFeign.getLatelyAddress(validPhone);
    }

    /**
     * 提交订单
     *
     * @param multiOrderInfo
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "phone")
    public SMSMarketVerifyDto submitVerifyOrder(MultiOrderInfo multiOrderInfo) {
        //解密
        String pid = DESUtil.decrypt(multiOrderInfo.getPid());
        String phone = DESUtil.decrypt(multiOrderInfo.getPhone());
        //校验参数合法性
        checkValidParam(phone, pid);
        //一个人一天只能下5单
        List<Object> list = RedisUtil.lRange("smsOrderCount:phone_" + phone, 0, -1);
        if (list.size() >= 5) {
            throw new MallException("020045");
        }
        //获取用户信息，没有则自动注册
        MallUser user = userFeign.getUserByPhoneFromFeign(phone);
        if (user == null) {
            user = userFeign.register(phone);
        }
        multiOrderInfo.setPid(pid);
        multiOrderInfo.setPhone(phone);
        multiOrderInfo.setUserId(user.getId());
        SMSMarketVerifyDto dto = null;
        //这里加入场景的方式（主要防止狗日的产品变需求）
        switch (multiOrderInfo.getState()) {
            case 1: //场景1 短信营销
                //初始化数据
                Map<String, Object> map = initAcVerifyValidMap(multiOrderInfo, user);
                if (map != null) {
                    //创建审核单
                    SMSAcOrderVerify verify = doAcOrderVerifyBean(multiOrderInfo, map);
                    //创建审核商品
                    List<SMSAcOrderItem> items = (List<SMSAcOrderItem>) map.get("acItems");
                    items = doAcOrderItemBean(verify, items);
                    //发送审核过期死信mq
                    sendOrderDelayMQ(QueueParam.builder().message(JSONUtil.obj2json(verify)).times(24 * 60 * 60 * 1000).build());
                    //返回
                    dto = getSMSMarketVerifyDto(verify, items);
                    //发送短信
                    MallUser parentUser = userFeign.getUserByIdFromFeign(pid);
                    UserAddressDto address = userFeign.getLatelyAddress(phone);
                    JpushUtil.sendNoticeMarketMsg2(parentUser.getPhone(), "(" + phone + "，" + (address == null ? user.getName() : address.getName()) + ")");
                }
                break;
            case 2: //场景2 来来来，等着你出场景2的需求
                break;
        }
        //记录下单
        boolean b = RedisUtil.lSet("smsOrderCount:phone_" + phone, "1", 24 * 60 * 60);
        log.info("---------打印日志，判断是否插入redis成功:" + phone + "---->" + b);
        return dto;
    }


    /**
     * 审核单超时处理 -> mq消费
     *
     * @param verify
     */
    @RedisLock(key = "id")
    @Transactional
    public void verifyTimeoutListen(SMSAcOrderVerify verify) {
        SMSAcOrderVerify orderVerify = this.selectById(verify.getId());
        if (orderVerify.getVerifyStatus() != 0) {
            return;
        }
        //更新审核单状态为超时
        updateVerify(null, SMSAcVerify.builder().verifyId(verify.getId()).verifyStatus(3).build());
    }


    /**
     * 审核
     *
     * @param smsAcVerify
     * @return
     */
    @TxTransaction(isStart = true)
    @Transactional
    @RedisLock(key = "verifyId")
    public OrderInfoDto verify(SMSAcVerify smsAcVerify) {
        SMSAcOrderVerify verifyOrder = getVerifyOrderById(smsAcVerify.getVerifyId());
        if (verifyOrder.getVerifyStatus() != 0) {
            throw new MallException("020014");
        }
        MallUser user = UserUtils.getCurrentUser();
        List<SMSAcOrderItem> items = getAcOrderVerifyItemsByVerifyId(smsAcVerify.getVerifyId());
        OrderInfoDto dto = null;
        switch (smsAcVerify.getVerifyStatus()) {
            case 1: //审核通过 (支付完成后)更新审核单状态
                String orderId = IDUtils.genOrderId();
                //判断库存
                checkCloudStock(items, user.getId(), orderId);
                //判断是否已经生成了未支付的提货单
                MallOrderInfo orderInfo = getOrderInfoByAcVerifyId(smsAcVerify.getVerifyId());
                if (orderInfo == null) {
                    //创建提货单
                    orderInfo = handleTakeItemOrderInfo(orderId, verifyOrder);
                    //创建提货支付单
                    handleTakeItemPayInfo(orderInfo);
                    //创建提货商品
                    handleTakeItemInfo(user, orderInfo, items);
                }
                //返回dto
                dto = getOrderInfoDto(orderInfo);
                break;
            case 2: //审核不通过
                //更新审核单状态
                updateVerify(null, smsAcVerify);
                dto = OrderInfoDto.builder().result(false).build();
                break;
        }
        return dto;
    }

    /**
     * 重新下单 (审核驳回或超时可以重新赋值下单，订单继续推给该用户的有效上上级代理，直到审核通过为止)
     *
     * @param verifyId
     */
    public void reSubmitVerifyOrder(String verifyId) {
        SMSAcOrderVerify verifyOrder = getVerifyOrderById(verifyId);
        List<SMSAcOrderItem> items = getAcOrderVerifyItemsByVerifyId(verifyId);
        //转交 查询有效上上级
        MallAgent agent = agentFeign.getAgentByUserId(verifyOrder.getAccepterId());
        MallAgent agentSuper = agentFeign.getAgentById(agent.getParentId());
        MallAgent superior = agentFeign.getValidSuperior(agentSuper.getUserId());
        if (superior == null || "0".equals(superior.getId())) {
            throw new MallException("020044");
        }
        //重新创建审核单
        verifyOrder.setId(IDUtils.genId());
        verifyOrder.setAccepterId(superior.getUserId());
        verifyOrder.setVerifyStatus(0);
        verifyOrder.setCreateDate(new Date());
        smsAcOrderVerifyMapper.insert(verifyOrder);
        //创建审核商品
        doAcOrderItemBean(verifyOrder, items);
    }

    /**
     * 取消审核单
     *
     * @param verifyId
     */
    public void cancelVerifyOrder(String verifyId) {
        SMSAcOrderVerify verify = getVerifyOrderById(verifyId);
        if (verify.getVerifyStatus() != 0) {
            throw new MallException("020017");
        }
        //判断是否已经生成了未支付的提货单
        MallOrderInfo orderInfo = getOrderInfoByAcVerifyId(verifyId);
        if (orderInfo != null) {
            throw new MallException("020048");
        }
        updateVerify(null, SMSAcVerify.builder().verifyId(verifyId).verifyStatus(4).build());
    }

    public MallOrderInfo getOrderInfoByAcVerifyId(String verifyId) {
        List<MallOrderInfo> infos = orderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("ac_verify_id", verifyId)
                .eq("order_status", "0")
                .eq("is_del", 0));
        if (CollectionUtils.isEmpty(infos)) {
            return null;
        }
        return infos.get(0);
    }

    /**
     * 查询短信营销审核单列表
     *
     * @param getOrderVerifyParam
     * @return
     */
    public Page<OrderVerifyDto> getSMSVerifyByAccepterId(GetOrderVeifyParam getOrderVerifyParam) {
        MallUser user = UserUtils.getCurrentUser();
        Page page = MybatisPageUtil.getPage(getOrderVerifyParam.getPageCurrent(), getOrderVerifyParam.getPageSize());
        Page<SMSAcOrderVerify> result = geSMSOrderVerifyByAccepterId(page, user.getId(), getOrderVerifyParam.getVerifyStatus());
        return getOrderVerifyDto(result);
    }

    /**
     * h5页面短信营销审核单列表
     *
     * @param querySMSVerify
     * @return
     */
    public Page<SMSMarketVerifyDto> getSMSVerifyInH5(QuerySMSVerify querySMSVerify) {
        String phone = DESUtil.decrypt(querySMSVerify.getPhone());
        MallUser user = userFeign.getUserByPhoneFromFeign(phone);
        if (user == null) {
            return null;
        }
        Page<SMSAcOrderVerify> result = getAcOrderVerifyByUserId(querySMSVerify, user.getId());
        Page<SMSMarketVerifyDto> dto = null;
        if (result != null) {
            List<SMSMarketVerifyDto> list = new ArrayList<>();
            result.getRecords().forEach(r -> {
                List<SMSAcOrderItem> items = getAcOrderVerifyItemsByVerifyId(r.getId());
                list.add(getSMSMarketVerifyDto(r, items));
            });
            dto = new Page<>();
            BeanUtils.copyProperties(result, dto);
            dto.setRecords(list);
        }
        return dto;
    }


    private Page<SMSAcOrderVerify> geSMSOrderVerifyByAccepterId(Page<SMSAcOrderVerify> page, String accepterId, String verifyStatus) {
        Wrapper<SMSAcOrderVerify> eq = new EntityWrapper<SMSAcOrderVerify>()
                .eq("accepter_id", accepterId)
                .eq("is_del", 0);

        if (StringUtils.isEmpty(verifyStatus) || "4".equals(verifyStatus)) {
            eq = eq.andNew()
                    .in("verify_status", Arrays.asList("0", "1", "2", "3"))
                    .orderBy("create_date", false);
        } else {
            eq = eq.andNew()
                    .eq("verify_status", verifyStatus)
                    .orderBy("create_date", false);
        }

        return this.selectPage(page, eq);
    }

    private Page<OrderVerifyDto> getOrderVerifyDto(Page<SMSAcOrderVerify> result) {
        Page<OrderVerifyDto> dtoPage = new Page<>();
        List<OrderVerifyDto> list = new ArrayList<>();
        if (ObjectUtils.isNotNullAndEmpty(result.getRecords())) {
            result.getRecords().forEach(p -> {
                OrderVerifyDto orderVerifyDto = new OrderVerifyDto();
                MallUser nextUser = UserUtils.getUserInfoByCacheOrId(p.getProposerId());
                List<SMSAcOrderItem> items = getAcOrderVerifyItemsByVerifyId(p.getId());
                List<OrderItemDetailDto> dto = queryAcVerifyItemDetail(items);
                BeanMapper.copy(p, orderVerifyDto);
                orderVerifyDto.setVerifyStatusName(MallOrderVerifyStatusEnum.explain(p.getVerifyStatus().toString()));
                orderVerifyDto.setVerifyMemo(p.getVerifyRemark());
                orderVerifyDto.setLastVerifyTime(p.getVerifyEndDate());
                orderVerifyDto.setTotalAmt(p.getTotalAmt().add(p.getFreightAmt()));
                orderVerifyDto.setOrderAmt(p.getTotalAmt());
                orderVerifyDto.setNextProxyName(nextUser.getName());
                orderVerifyDto.setNextProxyPhone(nextUser.getPhone());
                orderVerifyDto.setNextProxyRoleName(MallTeamEnum.explain(nextUser.getRoleId()));
                orderVerifyDto.setNextProxyIcon(nextUser.getIcon());
                orderVerifyDto.setOrderItemDetailDtos(dto);
                list.add(orderVerifyDto);
            });
        }
        dtoPage.setRecords(list);
        return dtoPage;
    }

    private List<OrderItemDetailDto> queryAcVerifyItemDetail(List<SMSAcOrderItem> items) {
        List<OrderItemDetailDto> dto = new ArrayList<>();
        items.forEach(i -> {
            MallSku sku = itemFeign.getSkuByCodeFromFeign(i.getSkuCode());
            dto.add(OrderItemDetailDto.builder()
                    .itemId(sku.getItemId())
                    .skuCode(sku.getSkuCode())
                    .amount(BigDecimal.valueOf(i.getNumber()))
                    .itemImages(sku.getSkuImg())
                    .skuImg(sku.getSkuImg())
                    .price(i.getPrice())
                    .title(sku.getTitle())
                    .unit(sku.getUnit())
                    .spec(sku.getSpec())
                    .currency(sku.getCurrency())
                    .createDate(i.getCreateDate())
                    .build());
        });
        return dto;
    }

    private void checkCloudStock(List<SMSAcOrderItem> items, String userId, String orderId) {
        items.forEach(i -> {
            MallCloudStock cloudStock = agentFeign.getCloudStock(MallCloudStock.builder().mallUserId(userId).skuCode(i.getSkuCode()).build());
            MallSku sku = itemFeign.getSkuByCode(i.getSkuCode());
            if (cloudStock.getStock().compareTo(BigDecimal.valueOf(i.getNumber())) < 0) {
                throw new MallException("060007", new Object[]{sku.getTitle()});
            }
            BigDecimal amount = BigDecimal.valueOf(i.getNumber()).negate();
            //预扣云库存
            agentFeign.updateCloudStockByLock(MallCloudStock.builder().id(cloudStock.getId()).stock(amount).build());
            //添加云库存变更记录
            log(cloudStock.getId(), i.getSkuCode(), sku.getItemId(), userId, amount, cloudStock.getStock(), cloudStock.getStock().add(amount), orderId);
        });
    }

    private MallOrderInfo handleTakeItemOrderInfo(String orderId, SMSAcOrderVerify verifyOrder) {
        MallOrderInfo orderInfo = MallOrderInfo.builder()
                .orderId(orderId)
                .paymentAmt(verifyOrder.getFreightAmt())
                .originAmt(BigDecimal.ZERO)
                .mallUserId(verifyOrder.getAccepterId())
                .summaryAmt(BigDecimal.ZERO)
                .postFeeAmt(verifyOrder.getFreightAmt())
                .belongsCode(MallStatusEnum.BELONGS_CODE_000.getCode())
                .addrId(verifyOrder.getProvince() + " " + verifyOrder.getCity() + " " + verifyOrder.getArea() + " " + verifyOrder.getStreet())
                .provincialUrbanArea(verifyOrder.getProvince() + " " + verifyOrder.getCity() + " " + verifyOrder.getArea())
                .street(verifyOrder.getStreet())
                .addrName(verifyOrder.getRecipientName())
                .addrPhone(verifyOrder.getRecipientPhone())
                .currency(MallStatusEnum.CURRENCY_CODE_000.getCode())
                .orderStatus(MallStatusEnum.ORDER_STATUS_000.getCode())
                .orderType(MallStatusEnum.ORDER_TYPE_002.getCode())
                .isDel(MallStatusEnum.IS_DEL_CODE_001.getCode())
                .logisticsMode(verifyOrder.getLogisticsMode())
                .createDate(new Date())
                .payEndDate(new Date(new Date().getTime() + 24 * 60 * 60 * 1000))
                .channelState(1)
                .subordinateId(verifyOrder.getProposerId())
                .acVerifyId(verifyOrder.getId())
                .build();
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }

    private void handleTakeItemPayInfo(MallOrderInfo mallOrderInfo) {
        payFeign.insertPayInfo(MallPayInfo.builder()
                .payId(IDUtils.genId())
                .createDate(new Date())
                .totalAmt(mallOrderInfo.getPaymentAmt())
                .mallUserId(mallOrderInfo.getMallUserId())
                .orderId(mallOrderInfo.getOrderId())
                .currency(mallOrderInfo.getCurrency())
                .payStatus(MallStatusEnum.PAY_STATUS_000.getCode())
                .status(MallStatusEnum.STATUS_CODE_000.getCode())
                .title("短信活动支付单")
                .isDel(MallStatusEnum.IS_DEL_CODE_001.getCode())
                .build());
    }

    private void handleTakeItemInfo(MallUser user, MallOrderInfo orderInfo, List<SMSAcOrderItem> items) {
        items.forEach(i -> {
            MallSku sku = itemFeign.getSkuByCode(i.getSkuCode());
            BigDecimal price = PriceUtil.getPrice(sku.getRetailPrice(), Integer.parseInt(user.getRoleId()));
            MallOrderItem item = MallOrderItem.builder()
                    .id(IDUtils.genId())
                    .orderId(orderInfo.getOrderId())
                    .currency(orderInfo.getCurrency())
                    .status(MallStatusEnum.ORDER_STATUS_000.getCode())
                    .itemId(sku.getItemId())
                    .skuCode(i.getSkuCode())
                    .amount(BigDecimal.valueOf(i.getNumber()).negate())
                    .price(price)
                    .unit(sku.getUnit())
                    .spec(sku.getSpec())
                    .createDate(new Date())
                    .build();
            orderItemMapper.insert(item);
        });
    }

    public void updateSMSAcVerify(SMSAcOrderVerify smsAcOrderVerify) {
        //如果超过6盒则自动生成vip用户
        SMSAcOrderVerify orderVerify = getVerifyOrderById(smsAcOrderVerify.getId());
        List<SMSAcOrderItem> items = getAcOrderVerifyItemsByVerifyId(smsAcOrderVerify.getId());
        Integer total = items.stream().map(e -> e.getNumber()).reduce(Integer::sum).get();
        if (total >= 6) {
            MallUser user = userFeign.getUserById(orderVerify.getProposerId());
            if ("0".equals(user.getRoleId())) {
                //升级成vip
                agentFeign.changeAgentToUpgrade(orderVerify.getProposerId(), "3", orderVerify.getAccepterId(), 0,
                        smsAcOrderVerify.getOrderId(), "短信营销活动升级为vip", "0");
            }
        }
        updateVerify(smsAcOrderVerify.getOrderId(), SMSAcVerify.builder().verifyId(smsAcOrderVerify.getId()).verifyStatus(smsAcOrderVerify.getVerifyStatus()).build());
    }


    private void updateVerify(String orderId, SMSAcVerify smsAcVerify) {
        smsAcOrderVerifyMapper.updateById(SMSAcOrderVerify.builder()
                .id(smsAcVerify.getVerifyId())
                .verifyStatus(smsAcVerify.getVerifyStatus())
                .verifyRemark(smsAcVerify.getRemark())
                .orderId(orderId)
                .verifyDate(new Date())
                .build());
    }

    private OrderInfoDto getOrderInfoDto(MallOrderInfo orderInfo) {
        MallUserAccountDto account = userFeign.accountInfo(orderInfo.getMallUserId());
        MallAgent agent = agentFeign.getAgentByUserId(orderInfo.getMallUserId());
        return OrderInfoDto.builder()
                .result(true)
                .orderInfoList(Collections.singletonList(orderInfo))
                .balance(account.getAmt() == null ? BigDecimal.ZERO : account.getAmt())
                .credit(account.getCredit() == null ? BigDecimal.ZERO : account.getCredit())
                .payCredit(BigDecimal.ZERO)
                .postFee(orderInfo.getPostFeeAmt())
                .paymentAmt(orderInfo.getPaymentAmt())
                .originAmt(BigDecimal.ZERO)
                .payTypeKey(orderInfoService.getPayType(orderInfo.getCurrency(), agent.getCompanyId()))
                .build();
    }

    private SMSMarketVerifyDto getSMSMarketVerifyDto(SMSAcOrderVerify verify, List<SMSAcOrderItem> items) {
        SMSMarketVerifyDto smsMarketVerifyDto = new SMSMarketVerifyDto();
        BeanUtils.copyProperties(verify, smsMarketVerifyDto);
        smsMarketVerifyDto.setOrderId(verify.getId());

        if (!StringUtils.isEmpty(verify.getOrderId())) {
            smsMarketVerifyDto.setOrderId(verify.getOrderId());
            MallOrderInfo orderInfo = orderInfoService.selectByIdNew(verify.getOrderId());
            if (!StringUtils.isEmpty(orderInfo.getEclpSoNo())) {
                getNowStatus(orderInfo.getExpressCode(), smsMarketVerifyDto);
            }
            //这些是verifyStatus为1的
            switch (Integer.parseInt(orderInfo.getOrderStatus())) {
                case 3:
                    smsMarketVerifyDto.setOrderStatus(3);
                    break;
                case 4:
                    smsMarketVerifyDto.setOrderStatus(4);
                    break;
                case 5:
                    smsMarketVerifyDto.setOrderStatus(5);
                    break;
            }
        } else {
            switch (verify.getVerifyStatus()) {
                case 0: //待审核
                    smsMarketVerifyDto.setOrderStatus(0);
                    break;
                case 2: //未过审
                    smsMarketVerifyDto.setOrderStatus(1);
                    break;
                case 3: //已超时
                    smsMarketVerifyDto.setOrderStatus(6);
                    break;
                case 4: //已取消
                    smsMarketVerifyDto.setOrderStatus(2);
                    break;
            }
        }
        MallUser superUser = userFeign.getUserByIdFromFeign(verify.getAccepterId());
        smsMarketVerifyDto.setSuperiorName(superUser.getName());
        smsMarketVerifyDto.setSuperiorPhone(superUser.getPhone());
        smsMarketVerifyDto.setAmount(items.stream().map(SMSAcOrderItem::getNumber).reduce(Integer::sum).get());
        smsMarketVerifyDto.setOrderItemDetailDtos(queryAcVerifyItemDetail(items));
        return smsMarketVerifyDto;
    }

    private void getNowStatus(String expressCode, SMSMarketVerifyDto smsMarketVerifyDto) {
        if (!StringUtils.isEmpty(expressCode)) {
            AliExpressResult express = AliExpress.getAliExpress(expressCode);
            if (!ObjectUtils.isNullOrEmpty(express) && !CollectionUtils.isEmpty(express.getData())) {
                smsMarketVerifyDto.setNowStatus(express.getData().get(0));
            } else {
                AliExpressDetail detail = new AliExpressDetail();
                detail.setContext("暂无物流信息");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                detail.setTime(simpleDateFormat.format(new Date()));
                smsMarketVerifyDto.setNowStatus(detail);
            }
        } else {
            AliExpressDetail detail = new AliExpressDetail();
            detail.setContext("没有物流单号，暂无信息");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            detail.setTime(simpleDateFormat.format(new Date()));
            smsMarketVerifyDto.setNowStatus(detail);
        }
    }

    private void sendOrderDelayMQ(QueueParam queueParam) {
        log.info("开始发送短信营销审核单数据:{}", queueParam.getMessage());
        Producer producer = rocketMQConfig.marketVerifyProducer();
        Message msg = new Message(rocketMQConfig.getMarketVerifyTopic(), RocketMQConfig.MARKET_VERIFY, queueParam.getMessage().getBytes());
        try {
            long delayTime = System.currentTimeMillis() + queueParam.getTimes();
            msg.setStartDeliverTime(delayTime);
            SendResult sendResult = producer.send(msg);
            if (sendResult != null) {
                log.info("消息发送成功：" + sendResult.toString());
            }
        } catch (ONSClientException e) {
            log.info("消息发送失败：", e);
        }
    }

    private Page<SMSAcOrderVerify> getAcOrderVerifyByUserId(QuerySMSVerify querySMSVerify, String userId) {
        Page<SMSAcOrderVerify> page = MybatisPageUtil.getPage(querySMSVerify.getPageCurrent(), querySMSVerify.getPageSize());
        Page<SMSAcOrderVerify> result = new Page<>();
        ;
        if (Arrays.asList(3, 4, 5).contains(querySMSVerify.getOrderStatus())) {
            querySMSVerify.setUserId(userId);
            List<SMSAcOrderVerify> r = smsAcOrderVerifyMapper.queryByOrderStatus(querySMSVerify, page);
            if (r != null) {
                result.setRecords(r);
            }
        } else {
            Wrapper<SMSAcOrderVerify> eq = new EntityWrapper<SMSAcOrderVerify>().eq("proposer_id", userId).eq("is_del", 0).orderBy("create_date", false);
            switch (querySMSVerify.getOrderStatus()) {
                case 0:
                    eq.andNew().eq("verify_status", 0); //待审核
                    break;
                case 1:
                    eq.andNew().eq("verify_status", 2);//未过审
                    break;
                case 2:
                    eq.andNew().eq("verify_status", 4);//已取消
                    break;
                case 6:
                    eq.andNew().eq("verify_status", 3);//已取消
                    break;
            }
            result = this.selectPage(page, eq);
        }
        if (CollectionUtils.isEmpty(result.getRecords())) {
            return null;
        }
        return result;
    }

    private boolean checkIsSubmit(String userId) {
        List<MallOrderInfo> orderInfos = orderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("channel_state", 1)
                .eq("is_del", 0)
                .eq("subordinate_id", userId));
        return !CollectionUtils.isEmpty(orderInfos);
    }

    private SMSAcOrderVerify getVerifyOrderById(String id) {
        return smsAcOrderVerifyMapper.selectById(id);
    }

    private List<SMSAcOrderItem> getAcOrderVerifyItemsByVerifyId(String verifyId) {
        List<SMSAcOrderItem> items = smsAcOrderItemMapper.selectList(new EntityWrapper<SMSAcOrderItem>().eq("verify_id", verifyId).eq("is_del", 0));
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        return items;
    }

    private boolean checkRecord(String phone, String pid) {
        if (pid == null || phone == null) {
            return true;
        }
        SMSRecordHits hits = smsRecordHitsMapper.selectOne(SMSRecordHits.builder().phone(phone).isDel("0").build());
        return hits != null;
    }

    private void checkValidParam(String phone, String pid) {
        if (pid == null || phone == null) {
            throw new MallException("020042");
        }
    }

    private SMSAcOrderVerify doAcOrderVerifyBean(MultiOrderInfo multiOrderInfo, Map<String, Object> map) {
        //查询有效上级
        MallAgent superior = agentFeign.getValidSuperior(multiOrderInfo.getPid());

        SMSAcOrderVerify verify = SMSAcOrderVerify.builder()
                .id(IDUtils.genId())
                .proposerId(multiOrderInfo.getUserId())
                .accepterId(superior.getUserId())
                .totalAmt(((BigDecimal) map.get("totalAmt")).add((BigDecimal) map.get("postFee")))
                .freightAmt((BigDecimal) map.get("postFee"))
                .logisticsMode((String) map.get("logisticsMode"))
                .createDate(new Date())
                .verifyStatus(0)
                .remark(multiOrderInfo.getRemark())
                .verifyEndDate(new Date(new Date().getTime() + 24 * 60 * 60 * 1000)) //24小时审核期
                .build();
        BeanUtils.copyProperties(multiOrderInfo, verify);
        smsAcOrderVerifyMapper.insert(verify);
        return verify;
    }

    private List<SMSAcOrderItem> doAcOrderItemBean(SMSAcOrderVerify verify, List<SMSAcOrderItem> acItems) {
        acItems.forEach(i -> {
            i.setId(IDUtils.genId());
            i.setVerifyId(verify.getId());
            i.setCreateDate(new Date());
            smsAcOrderItemMapper.insert(i);
        });
        return acItems;
    }

    private Map<String, Object> initAcVerifyValidMap(MultiOrderInfo multiOrderInfo, MallUser user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();

        List<SMSAcOrderItem> acItems = new ArrayList<>();
        List<SMSAcOrderItem> acItemsVip = new ArrayList<>();

        List<MallSku> skuList = new ArrayList<>();

        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalAmtVip = BigDecimal.ZERO;
        SkuSpecAmount specAmount = new SkuSpecAmount();
        List<SkuAndAmountParam> skuAndAmountParams = new ArrayList<>();
        int sum = 0;

        for (ItemParam item : multiOrderInfo.getItems()) {
            if (item.getNumber() == 0 || StringUtils.isEmpty(item.getNumber())) {
                continue;
            }
            MallSku mallSku = itemFeign.getSkuByCodeFromFeign(item.getSkuCode());
            BigDecimal priceVip = PriceUtil.getPrice(mallSku.getRetailPrice(), 3);
            BigDecimal price = PriceUtil.getPrice(mallSku.getRetailPrice(), 0);

            totalAmtVip = totalAmtVip.add(priceVip.multiply(BigDecimal.valueOf(item.getNumber())));
            totalAmt = totalAmt.add(price.multiply(BigDecimal.valueOf(item.getNumber())));

            sum += item.getNumber();

            skuAndAmountParams.add(SkuAndAmountParam.builder()
                    .skuCode(item.getSkuCode())
                    .amount(item.getNumber().toString())
                    .build());

            acItemsVip.add(SMSAcOrderItem.builder().skuCode(item.getSkuCode()).number(item.getNumber()).price(priceVip).build());
            acItems.add(SMSAcOrderItem.builder().skuCode(item.getSkuCode()).number(item.getNumber()).price(price).build());

            skuList.add(mallSku);
        }
        if (sum == 0) {
            throw new MallException("020046");
        }
        specAmount.setAmountParams(skuAndAmountParams);
        specAmount.setProvince(multiOrderInfo.getProvince());
        specAmount.setCity(multiOrderInfo.getCity());
        specAmount.setLogisticsType(multiOrderInfo.getLogisticsType());

        map.put("totalAmt", totalAmt);
        map.put("acItems", acItems);
        if (sum >= 6) {
            map.put("totalAmt", totalAmtVip);
            map.put("acItems", acItemsVip);
        }
        map.put("postFee", itemFeign.getPostFee(specAmount));
        map.put("logisticsMode", screenExpress(skuList));
        map.put("sum", sum);
        return map;
    }

    private String screenExpress(List<MallSku> skuList) {
        List<String> express = skuList.stream().map(MallSku::getLogisticsMode).collect(Collectors.toList());
        List<String> reverseOrder = express.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        return reverseOrder.get(0);
    }

    private void log(String cloudId, String skuCode, String itemId, String userId, BigDecimal payStock, BigDecimal payAgoStock, BigDecimal payAfterStock, String relationId) {
        //添加云库存变更记录
        MallCloudStockLog mallCloudStockLog = new MallCloudStockLog();
        mallCloudStockLog.setId(IDUtils.genId());
        mallCloudStockLog.setCloudStockId(cloudId);
        mallCloudStockLog.setSkuCode(skuCode);
        mallCloudStockLog.setItemId(itemId);
        mallCloudStockLog.setPayStock(payStock);
        mallCloudStockLog.setMallUserId(userId);
        mallCloudStockLog.setPayAgoStock(payAgoStock);
        mallCloudStockLog.setPayAfterStock(payAfterStock);
        mallCloudStockLog.setCreateDate(new Date());
        mallCloudStockLog.setRelationId(relationId);
        mallCloudStockLog.setRelationIdType("0");
        mallCloudStockLog.setStatus("0");
        MallBranchOffice branchCompany = agentFeign.getBranchCompany(userId);
        if (branchCompany != null) {
            mallCloudStockLog.setCompanyId(branchCompany.getId());
        }
        agentFeign.addCloudStockLog(mallCloudStockLog);
    }

    public void addSmsEvaluate(SMSAcEvaluate smsAcEvaluate) {
        boolean evaluated = getEvaluated(smsAcEvaluate.getOrderId());
        if (evaluated) {
            throw new MallException("020047");
        }
        smsAcEvaluate.setId(IDUtils.genId());
        smsAcEvaluate.setCreateDate(new Date());
        String userId;
        MallOrderInfo orderInfo = orderInfoService.selectByIdNew(smsAcEvaluate.getOrderId());
        if (orderInfo == null) {
            SMSAcOrderVerify verify = this.selectById(smsAcEvaluate.getOrderId());
            userId = verify.getProposerId();
        } else {
            userId = orderInfo.getSubordinateId();
        }
        smsAcEvaluate.setUserId(userId);
        smsAcEvaluateMapper.insert(smsAcEvaluate);
    }

    public boolean getEvaluated(String orderId) {
        String userId;
        MallOrderInfo orderInfo = orderInfoService.selectByIdNew(orderId);
        if (orderInfo == null) {
            SMSAcOrderVerify verify = this.selectById(orderId);
            userId = verify.getProposerId();
        } else {
            userId = orderInfo.getSubordinateId();
        }
        List<SMSAcEvaluate> evaluates = smsAcEvaluateMapper.selectList(new EntityWrapper<SMSAcEvaluate>().eq("user_id", userId).eq("is_del", 0));
        return !CollectionUtils.isEmpty(evaluates);
    }

    public void sendNoticeMarketMsg(String phone, String pid) {
        JpushUtil.sendNoticeMarketMsg1(phone, sms_market_url, DESUtil.encrypt(pid), DESUtil.encrypt(phone));
    }
}
