package com.meifute.core.controller.implement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.OrderApiService;
import com.meifute.core.dto.*;
import com.meifute.core.entity.*;
import com.meifute.core.entity.activity.MallAcOrder;
import com.meifute.core.entity.order.AsyncTaskInfo;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallTransferGoodsMapper;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.model.qimen.QiMenItem;
import com.meifute.core.service.*;
import com.meifute.core.test.XmltoJson;
import com.meifute.core.util.DeliveryOrderConfirmRequest;
import com.meifute.core.vo.*;
import com.meifute.core.vo.order.AsyncTaskParam;
import com.meifute.core.vo.order.AsyncTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.simmy.qimen.core.MethodNames;
import tech.simmy.qimen.core.ParamNames;
import tech.simmy.qimen.request.EntryOrderConfirmRequest;
import tech.simmy.qimen.util.ResponseCreators;
import tech.simmy.qimen.util.XmlSerializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.commons.util.InetUtilsProperties.PREFIX;

/**
 * @Auther: wxb
 * @Date: 2018/10/12 15:39
 * @Auto: I AM A CODE MAN -_-!
 * @Description: 内部系统接口
 */
@RestController
@Slf4j
public class ImplementController extends BaseController implements OrderApiService {

    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private MallOrderItemService orderItemService;
    @Autowired
    private MallOrderVerifyService orderVerifyService;
    @Autowired
    private MallRegulateInfoService regulateInfoService;
    @Autowired
    private MallRegulateItemService regulateItemService;
    @Autowired
    private MallTransferItemService transferItemService;
    @Autowired
    private JDExpressPushService jdExpressPushService;
    @Autowired
    private JDCheckPushAddressService jdCheckPushAddressService;
    @Autowired
    private OrderDelayService orderDelayService;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private MallOrderToPushService orderToPushService;
    @Autowired
    MallOrderPriceDetailService mallOrderPriceDetailService;
    @Autowired
    private SMSMarketActivityService smsMarketActivityService;
    @Autowired
    private MallTransferGoodsMapper mallTransferGoodsMapper;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private QimenPushService qimenPushService;
    @Autowired
    WuYouService wuYouService;
    @Autowired
    private ExpressCompanyService expressCompanyService;
    @Autowired
    OrderFeedBackService orderFeedBackService;

    @Autowired
    AsyncTaskInfoService asyncTaskInfoService;

    @Autowired
    MallProdOrderExpService mallProdOrderExpService;

    /**
     * 查询订单信息总数
     *
     * @param param
     * @return
     */
    @Override
    public Integer queryOrderInfoCount(@RequestBody MallOrderInfo param) {
        Integer count = orderInfoService.queryOrderInfoCount(param);
        log.info("success to queryOrderInfoCount, RESULT:{}", count);
        return count;
    }

    /**
     * 内部系统接口：根据orderId查询订单信息
     *
     * @param orderId
     * @return
     */
    @Override
    public MallOrderInfo getMallOrderInfoById(@RequestParam("orderId") String orderId) {
        MallOrderInfo orderInfo = orderInfoService.selectById(orderId);
        log.info("call getMallOrderInfoById success:{}", orderInfo);
        return orderInfo;
    }

    /**
     * 内部系统接口：根据orderId修改订单信息
     *
     * @param mallOrderInfo
     * @return
     */
    @Override
    public Boolean updateMallOrderInfoById(@RequestBody MallOrderInfo mallOrderInfo) {
        Boolean result = orderInfoService.updateOrderById(mallOrderInfo);
        log.info("call updateMallOrderInfoById success:{}", result);
        return result;
    }

    /**
     * 内部系统接口：添加订单审核单
     *
     * @param mallOrderVerify
     * @return
     */
    @Override
    public Boolean insertOrderVerify(@RequestBody MallOrderVerify mallOrderVerify) {
        Boolean result = orderVerifyService.insertOrderVerify(mallOrderVerify);
        log.info("call insertOrderVerify success:{}", result);
        return result;
    }

    /**
     * 内部系统接口：根据orderId查询购买商品信息
     *
     * @param orderId
     * @return
     */
    @Override
    public List<MallOrderItem> getMallOrderItemListByOrderId(@RequestParam("orderId") String orderId) {
        List<MallOrderItem> orderItems = orderItemService.selectByOrderId(orderId);
        log.info("call getMallOrderItemListByOrderId success:{}", orderItems);
        return orderItems;
    }

    /**
     * 内部系统接口：根据orderId修改订单商品信息
     *
     * @param mallOrderItem
     * @return
     */
    @Override
    public Boolean updateMallOrderItemInfoById(@RequestBody MallOrderItem mallOrderItem) {
        Boolean result = orderItemService.updateOrderItemInfo(mallOrderItem);
        log.info("call updateMallOrderItemInfoById success:{}", result);
        return result;
    }

    /**
     * 内部系统接口：查用户的订单个数
     *
     * @param mallOrderInfo
     * @return
     */
    @Override
    public Integer queryOrderInfoCountByUserId(@RequestBody MallOrderInfo mallOrderInfo) {
        Integer result = orderInfoService.queryOrderInfoCountByUserId(mallOrderInfo);
        log.info("call queryOrderInfoCountByUserId success:{}", result);
        return result;
    }

    /**
     * 添加订单信息
     *
     * @param mallOrderInfo
     * @return
     */
    @Override
    public MallOrderInfo insertOrderInfo(@RequestBody MallOrderInfo mallOrderInfo) {
        MallOrderInfo result = orderInfoService.insertOrderInfo(mallOrderInfo);
        log.info("call insertOrderInfo success:{}", result);
        return result;
    }

    /**
     * 添加调剂单信息
     *
     * @param mallRegulateInfo
     * @return
     */
    @Override
    public Boolean insertRegulateInfo(@RequestBody MallRegulateInfo mallRegulateInfo) {
        Boolean result = regulateInfoService.insertRegulateInfo(mallRegulateInfo);
        log.info("call insertRegulateInfo success:{}", result);
        return result;
    }

    @Override
    public Boolean insertRegulateInfoV2(@RequestBody MallRegulateInfo mallRegulateInfo) {
        Boolean result = regulateInfoService.insertRegulateInfo(mallRegulateInfo);
        log.info("call insertRegulateInfo success:{}", result);
        return result;
    }

    /**
     * 添加调剂单信息
     *
     * @param mallRegulateItem
     * @return
     */
    @Override
    public Boolean insertRegulateItem(@RequestBody MallRegulateItem mallRegulateItem) {
        Boolean result = regulateItemService.insertRegulateItem(mallRegulateItem);
        log.info("call insertRegulateItem success:{}", result);
        return result;
    }

    /**
     * 添加转货商品信息
     *
     * @param mallTransferItem
     * @return
     */
    @Override
    public Boolean insertTransferItemInfo(@RequestBody MallTransferItem mallTransferItem) {
        Boolean result = transferItemService.insertTransferItem(mallTransferItem);
        log.info("call insertTransferItemInfo success:{}", result);
        return result;
    }

    /**
     * 添加订单商品信息
     *
     * @param mallOrderItem
     * @return
     */
    @Override
    public Boolean insertOrderItemInfo(@RequestBody MallOrderItem mallOrderItem) {
        return orderItemService.insertOrderItemInfo(mallOrderItem);
    }

    /**
     * 敏感产品审核
     *
     * @param reviewGoodsVerifyParam
     * @return
     */
    @Override
    public String sensitiveGoodsVerify(@RequestBody ReviewGoodsVerifyParam reviewGoodsVerifyParam) {
        return orderVerifyService.sensitiveGoodsVerify(reviewGoodsVerifyParam);
    }

    /**
     * 敏感产品审核列表
     *
     * @param adminReviewVerifyParam
     * @return
     */
    @Override
    public PageDto<OrderVerifyDto> sensitiveOrderInfoList(@RequestBody AdminReviewVerifyParam adminReviewVerifyParam) {
        return orderVerifyService.sensitiveOrderInfoList(adminReviewVerifyParam);
    }

    //云转货单列表
    @Override
    public PageDto<OrderTransferInfoPageDto> queryMallTransferOrders(@RequestBody GetOrderTransferPageListParam pageListParam) {
        return orderInfoService.queryMallTransferOrders(pageListParam);
    }

    @Override
    public PageDto<OrderInfoPageDto> getGeneralOrderInfoPageList(@RequestBody GetOrderPageListParam pageListParam) {
        return orderInfoService.getGeneralOrderInfoPageList(pageListParam);
    }

    @Override
    public PageDto<OrderVerifyPageDto> queryOrderVerifyPageList(@RequestBody GetOrderVerifyPageListParam pageListParam) {
        return orderVerifyService.queryOrderVerifyPageList(pageListParam);
    }

    @Override
    public void updateOrderInfo(@RequestBody MallOrderInfo mallOrderInfo) {
        orderInfoService.updateOrderById(mallOrderInfo);
    }

    @Override
    public PageDto<RegulateDto> queryMallRegulatePageList(@RequestBody GetRegulateParam param) {
        return regulateInfoService.queryMallRegulatePageList(param);
    }

    @Override
    public MallRegulateInfo regulationInfoGet(@RequestParam("id") String orderId) {
        log.info("===============================>>>regulationInfoGet:{}", orderId);
        List<MallRegulateInfo> mallRegulateInfos = regulateInfoService.selectList(
                new EntityWrapper<MallRegulateInfo>()
                        .eq("order_id", orderId)
                        .eq("regulate_type", "1")
        );
        log.info("===============================>>>mallRegulateInfos:{}", mallRegulateInfos);
        return ObjectUtils.isNotNullAndEmpty(mallRegulateInfos) ? mallRegulateInfos.get(0) : null;
    }

    @Override
    public PageDto<OrderExchangeInfoDto> queryExchangeInfoList(@RequestBody GetExchangeOrderParam param) {
        return orderInfoService.queryExchangeInfoList(param);
    }

    @Override
    public List<MallRegulateInfo> checkRegulationInfoExist(@RequestParam("id") String orderId) {
        List<MallRegulateInfo> mallRegulateInfos = regulateInfoService.selectList(
                new EntityWrapper<MallRegulateInfo>().eq("order_id",orderId)
        );
        return mallRegulateInfos;
    }


    @Override
    public PageDto queryAllOrderInfoList(@RequestBody GetOrderPageListParam param) {
        PageDto<AllOrderInfoDto> dtoPage = orderInfoService.queryAllOrderInfoList(param);
        return dtoPage;
    }

    @Override
    public PageDto queryCreditOrderInfoPages(@RequestBody GetOrderPageListParam param) {
        return orderInfoService.queryCreditOrderInfoPages(param);
    }

//    /**
//     * 创建退款订单
//     * @param refundOrderInfoParam
//     * @return
//     */
//    @Override
//    public MallOrderInfo createRefundOrderInfo(@RequestBody RefundOrderInfoParam refundOrderInfoParam) {
//        MallOrderInfo result = refundOrderInfoService.createRefundOrderInfo(refundOrderInfoParam);
//        log.info("call createRefundOrderInfo success:{}", result);
//        return result;
//    }

    @Override
    public List<MallOrderVerify> getMallOrderVerifyByOrderId(@RequestParam("orderId") String orderId) {
        List<MallOrderVerify> result = orderVerifyService.getMallOrderVerifyByOrderId(orderId);
        log.info("call getMallOrderVerifyByOrderId success:{}", result);
        return result;
    }

    @Override
    public Boolean updateVerifyOrder(@RequestBody MallOrderVerify mallOrderVerify) {
        return orderVerifyService.updateVerifyOrder(mallOrderVerify);
    }

    @Override
    public Page<MallOrderInfo> getUserOrderInfos(@RequestBody MallOrderInfo mallOrderInfo) {
        return orderInfoService.getUserOrderInfos(mallOrderInfo);
    }

    @Override
    public List<OrderItemDetailDto> orderItemByOrderId(@RequestParam("orderId") String orderId) {
        return orderInfoService.orderItemByOrderId(orderId, 0);
    }

    @Override
    public MallOrderVerify getOrderVerifyById(@RequestParam("id") String id) {
        return orderVerifyService.selectById(id);
    }

    @Override
    public void addTransportGoodsInfo(@RequestBody AddTransportGoodsParams params) {
        jdExpressPushService.transportGoodsInfo(params.getId(), params.getThirdCateGoryNo(), params.getExpressType());
    }

    @Override
    public void orderDelayListener(@RequestBody OrderInfoMessage orderInfoMessage) {
        orderDelayService.delayOrderListener(orderInfoMessage);
    }

    @Override
    public void delayOrderVerifyListener(@RequestBody OrderInfoMessage orderInfoMessage) {
        orderDelayService.delayOrderVerifyListener(orderInfoMessage);
    }

//    @Override
//    public Boolean updateTransPortCode(@RequestBody JdGetExpressCodeVo getOrderInfo) {
//        jdExpressPushService.queryJDOrder(getOrderInfo.getEclpSoNo());
//        return true;
//    }

//    @Override
//    public List<GoodsInfo> getTransportGoodsInfo(@RequestBody BaseParam baseRequest){
//        return  jdExpressPushService.getTransportGoodsInfo(baseRequest);
//    }

    @Override
    public MallOrderInfo queryOrderByOrderId(@RequestBody GetOrderInfo getOrderInfo) {
        List<MallOrderInfo> list = orderInfoService.selectList(new EntityWrapper<MallOrderInfo>().eq("order_id", getOrderInfo.getOrderId()));
        return list.get(0);
    }

    @Override
    public Boolean JdExpressDelayListener(@RequestBody OrderInfoMessage orderInfoMessage) {
        return orderDelayService.delayExpressListener(orderInfoMessage);
    }

    @Override
    public JdPushFromAdmin pushToJdFromAdmin(@RequestBody OrderInfoMessage orderInfoMessage) {
        return orderDelayService.pushToJdFromAdmin(orderInfoMessage);
    }

    @Override
    public OrderCountAndAmt queryCountAmtByUserId(@RequestParam("userId") String userId) {
        return orderInfoService.queryCountAmtByUserId(userId);
    }

//    @Override
//    public Boolean cancleJdOrder(@RequestBody String orderid){
//       return  jdExpressPushService.cancelOrder(orderid);
//    }

    @Override
    public OrderCountAndAmt queryTeamTotalAmt(@RequestBody List<String> userIds) {
        return orderInfoService.queryTeamTotalAmt(userIds);
    }

    @Transactional
    @Override
    public String queryAllOrder(@RequestBody GetOrderInfo getOrderInfo) {
        List<MallOrderInfo> mallOrderInfos = orderInfoService.queryOrderEclpSONo();
        if (!CollectionUtils.isEmpty(mallOrderInfos)) {
            mallOrderInfos.forEach(p -> {
                try {
                    jdExpressPushService.queryJDOrderWayBill(JSONArray.parseArray(p.getEclpSoNo()).get(0).toString(), 0, p.getOrderId());
                } catch (Exception e) {
                    log.info("" + e);
                }
            });
        }
        List<MallAcOrder> acOrders = orderInfoService.getAcOrderEclpSoNo();
        if (acOrders != null && acOrders.size() != 0) {
            acOrders.forEach(p -> {
                try {
                    jdExpressPushService.queryJDOrderWayBill(p.getSingleNum(), 2, p.getId());
                } catch (Exception e) {
                    log.info("" + e);
                }
            });
        }
        return "success";
    }

    /**
     * 查询所有订单问题反馈补发的运单号
     *
     * @return
     */
    @Override
    public void getExpressCodeFeedback(@RequestBody GetOrderInfo getOrderInfo) {
        orderFeedBackService.getExpressCodeFeedback();
    }

    //自动收货job
    @Override
    public String receivingGoodsFromJd(@RequestBody GetOrderInfo getOrderInfo) {
        List<MallOrderInfo> list = mallOrderInfoMapper.queryJDExpressCode();
        if (!CollectionUtils.isEmpty(list) && list.size() > 0) {
            for (MallOrderInfo info : list) {
                jdExpressPushService.receivingGoodsFromJd(info.getEclpSoNo(), info.getOrderId());
            }
        }
        return "success";
    }

    @Override
    public String forceReceivingGoodsFromJd(@RequestBody GetOrderInfo getOrderInfo) {
        Integer affectRows = mallOrderInfoMapper.updateForceReceivingGoods();
        log.info("forceReceivingGoodsFromJd===================>>>>affectRows:{}", affectRows);
        return "success";
    }


    @Override
    public void updateMallOrderInfo(@RequestBody MallOrderInfo mallOrderInfo) {
        orderInfoService.updateById(mallOrderInfo);
    }

    @Override
    public Boolean receivingItem(@RequestBody GetOrderInfo info) {
        return orderInfoService.receivingItem(info.getOrderId());
    }

    @Override
    public void pushAllOrder() {
        orderInfoService.pushAllOrder();
    }

    /**
     * 内部系统接口：根据orderId查询订单信息
     *
     * @param orderId
     * @return
     */
    @Override
    public MallOrderInfo getMallOrderByIdNoError(@RequestParam("orderId") String orderId) {
        MallOrderInfo orderInfo = orderInfoService.selectByIdNoError(orderId);
        log.info("call getMallOrderInfoById success:{}", orderInfo);
        return orderInfo;
    }

    /**
     * 获取个人中心主页订单信息2.0
     *
     * @return
     */
    @Override
    public MallPersonalCenterDto queryPersonalOrderInfo(@RequestParam("userId") String userId) {
        MallPersonalCenterDto result = orderInfoService.queryPersonalOrderInfo(userId);
        log.info("success to queryPersonalOrderInfo, RESULT:{}", result);
        return result;
    }

    @Override
    public List<String> getOrderIdsByType(@RequestParam("orderType") String orderType) {
        List<String> orderIdsByOrderType = orderInfoService.getOrderIdsByOrderType(orderType);
        return orderIdsByOrderType;
    }

    @Override
    public MallOrderInfo getFirstOutItemOrderInfo(@RequestParam("mallUserId") String mallUserId) {
        return orderInfoService.getFirstOutItemOrderInfo(mallUserId);
    }

    @Override
    public List<MallOrderToPush> getOrderToPush() {
        return orderToPushService.getOrderToPush();
    }

    public void toPushOrderToPush(@RequestBody List<MallOrderToPush> list) {
        orderToPushService.toPushOrderToPush(list);
    }

    public void insertPush(@RequestParam("orderId") String orderId) {
        orderToPushService.insertPush(orderId, 0);
    }

    public Boolean jdCheckPushAddressService(@RequestBody MallOrderInfo info) {
        return jdCheckPushAddressService.checkPushAddress(info.getOrderId(), info.getAddrId());
    }

    //查询代理升级的订单的商品总数
    @Override
    public BigDecimal selectSumItemByAgentUpGrade(@RequestParam("relationId") String relationId) {

        return mallTransferGoodsMapper.sumItem(relationId);
    }


    public List<MallTransferGoods> getTransferGoodById(@RequestParam("orderId") String orderId) {
        return mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>().eq(!StringUtils.isEmpty(orderId), "relation_id", orderId));
    }

    public MallRegulateInfo getRegulateInfoById(@RequestParam("id") String id) {
        return regulateInfoService.selectById(id);
    }

    public MallTransferGoods getTransFerById(@RequestParam("id") String id) {
        return mallTransferGoodsMapper.selectById(id);
    }


    public MallRegulateItem getRegulateItemByRegulateId(@RequestParam("id") String id) {
        List<MallRegulateItem> items = regulateItemService.selectList(new EntityWrapper<MallRegulateItem>().eq("regulate_id", id));
        if (!CollectionUtils.isEmpty(items)) {
            return items.get(0);
        }
        return null;
    }

    @Override
    public List<MallOrderInfo> queryOrderListByParam(@RequestBody MallOrderInfo param) {
        return orderInfoService.queryOrderListByParam(param);
    }

    @Override
    public List<MallRegulateInfo> queryRegulationInfoListByParam(@RequestBody MallRegulateInfo param) {
        return regulateInfoService.queryRegulationInfoListByParam(param);
    }

    /**
     * 记录出库价格详情
     *
     * @param param
     * @return
     */
    @Override
    public Boolean saveOrderPriceDetail(@RequestBody MallOrderPriceDetail param) {
        return mallOrderPriceDetailService.saveOrderPriceDetail(param);
    }

    public String queryJDOrderWayBill(@RequestParam("eclpSoNo") String eclpSoNo, @RequestParam("originOrder") Integer originOrder, @RequestParam("orderId") String orderId) {
        return jdExpressPushService.queryJDOrderWayBill(eclpSoNo, originOrder, orderId);
    }

    @Override
    public void updateSMSAcVerify(@RequestBody SMSAcOrderVerify smsAcOrderVerify) {
        smsMarketActivityService.updateSMSAcVerify(smsAcOrderVerify);
    }

    @Override
    public Integer getNowDaysC036(@RequestParam("userId") String userId) {
        return orderInfoService.getNowDaysC036(userId);
    }

    @Override
    public Integer getNowDaysC036New(@RequestParam("userId") String userId) {
        return orderInfoService.getNowDaysC036New(userId);
    }

    @Override
    public void temporaryPushToJd(@RequestBody TemPushToJd temPushToJd) {
        orderInfoService.temporaryPushToJd(temPushToJd);
    }

    @Override
    public void setTemporaryNotPushPhone(@RequestBody TemPushToJd temPushToJd) {
        if (temPushToJd.getPhones() != null && temPushToJd.getPhones().size() > 0) {
            List<String> phones = temPushToJd.getPhones();
            List<String> list = new ArrayList<>();
            phones.forEach(p -> {
                MallUser user = userFeign.getUserByPhoneFromFeign(p);
                list.add(user.getId());
            });
            RedisUtil.set("temporaryNotPushUserId", JSONUtil.obj2json(list));
        }
    }

    public Boolean syncItem(@RequestParam("skuCode") String skuCode) {
        qimenPushService.syncQimenItem(skuCode);
        return true;
    }

    @Transactional
    @Override
    public String qiMenAPIStorage(@RequestBody String requestBody, @RequestParam Map<String, String> requestParams) throws Exception {

        log.debug("接收奇门消息:{} \r\n{}", requestParams, requestBody);

        final String method = requestParams.get(ParamNames.METHOD).replace(PREFIX, "");
        try {
            if (MethodNames.STOCKOUT_CONFIRM.equalsIgnoreCase(method)) {

                //出库确认 不需要 直接返回
                return getFormat("出库确认");

            } else if (MethodNames.ENTRYORDER_CONFIRM.equalsIgnoreCase(method) || ("taobao.qimen." + MethodNames.ENTRYORDER_CONFIRM).equals(method)) {

                //入库确认
                EntryOrderConfirmRequest request = XmlSerializer.readXml(requestBody, EntryOrderConfirmRequest.class);
                EntryOrderConfirmRequest.EntryOrder entryOrder = request.getEntryOrder();
                List<EntryOrderConfirmRequest.OrderLine> orderLines = request.getOrderLines();

                if (entryOrder != null && orderLines != null && orderLines.size() > 0) {

//                    List<String> items = orderLines.stream().map(EntryOrderConfirmRequest.OrderLine::getItemCode).collect(Collectors.toList());

//                    qimenPushService.DeliveryOrder(entryOrder.getEntryOrderCode());

//                    MallOrderInfo orderInfo = new MallOrderInfo();
//                    orderInfo.setOrderId(entryOrder.getEntryOrderCode());
//                    orderInfo.setQmType("2");
//                    boolean b = orderInfoService.updateOrderByIdNew(orderInfo);
//                    if (!b) {
//                        return getFormatFailure("接收失败");
//                    }
                    return getFormat("接收成功，已处理入库确认");
                }

            } else if (MethodNames.DELIVERYORDER_CONFIRM.equals(method) || ("taobao.qimen." + MethodNames.DELIVERYORDER_CONFIRM).equals(method)) {
                //发货回调没有封装
                log.info("奇门发货回调:{}", requestBody);


                DeliveryOrderConfirmRequest deliveryOrderConfirmRequest = XmlSerializer.readXml(requestBody, DeliveryOrderConfirmRequest.class);

                log.info("==========解析后数据:{}", deliveryOrderConfirmRequest);


                //<request>
                //    <deliveryOrder>
                //        <deliveryOrderCode>XZXS2001230288</deliveryOrderCode>
                //        <ownerCode>XYY201</ownerCode>
                //        <deliveryOrderId>XZXS2001230288</deliveryOrderId>
                //        <orderType>JYCK</orderType>
                //        <warehouseCode>WX</warehouseCode>
                //        <outBizCode>XZXS2001230288</outBizCode>
                //        <confirmType>0</confirmType>
                //        <orderConfirmTime>2020-01-23 15:24:04</orderConfirmTime>
                //        <operatorName>戴艳</operatorName>
                //        <operateTime>2020-01-23 15:24:04</operateTime>
                //    </deliveryOrder>
                //    <packages>
                //        <package>
                //            <logisticsCode>STO2</logisticsCode>
                //            <expressCode>773024447089377</expressCode>
                //            <weight>0.0</weight>
                //            <items>
                //                <item>
                //                    <itemCode>6001280065103</itemCode>
                //                    <itemId>6001280065103</itemId>
                //                    <quantity>1</quantity>
                //                </item>
                //            </items>
                //        </package>
                //    </packages>
                //    <orderLines>
                //        <orderLine>
                //            <orderLineNo></orderLineNo>
                //            <ownerCode>XYY201</ownerCode>
                //            <itemCode>6001280065103</itemCode>
                //            <planQty>0</planQty>
                //            <actualQty>1</actualQty>
                //        </orderLine>
                //    </orderLines>
                //</request>

                JSONObject request = XmltoJson.xmltoJson(requestBody);
                JSONObject jsonObject = JSONObject.parseObject(request.get("request") + "");


                //订单信息
                Object object = jsonObject.get("deliveryOrder");
                JSONObject deliveryOrder = JSONObject.parseObject(object + "");
                String deliveryOrderCode = deliveryOrder.get("deliveryOrderCode") + "";//我方业务单号id
                String deliveryOrderId = deliveryOrder.get("deliveryOrderId") + "";
                log.info("订单信息:{},{}", deliveryOrderCode, deliveryOrderId);

                //实际数量和入库数量
//                JSONArray orderLines = (JSONArray) jsonObject.get("orderLines");
//                log.info("实际数量和入库数量:{}", orderLines);

//                JSONObject orderLinesJson = JSONObject.parseObject(orderLines + "");
//                Object orderLineObj = orderLinesJson.get("orderLine");
//                JSONObject orderLineJson = JSONObject.parseObject(orderLineObj + "");
//                String planQty = (String) orderLineJson.get("planQty");
//                String actualQty = (String) orderLineJson.get("actualQty");

                //我们库里实际数量
//                Integer amount = qimenPushService.getActualAmount(deliveryOrderCode);
//                if(amount != Integer.parseInt(actualQty)){
//                    //当计划数量与接受数量不一致，需要抛错，或者添加中间状态：类似（推单失败之类）
//                    log.info("当计划数量与接受数量不一致,订单:{}", deliveryOrderCode);
//                    MallOrderInfo orderInfo = new MallOrderInfo();
//                    orderInfo.setOrderId(deliveryOrderCode);
//                    orderInfo.setQmType("5");
//                    orderInfoService.updateOrderByIdNew(orderInfo);
//                    return getFormatFailure("500","当计划数量与接受数量不一致,订单:"+deliveryOrderCode);
//                }


                log.info("jsonObject------:{}", jsonObject);
                JSONObject packages = JSONObject.parseObject(jsonObject.get("packages").toString());
                log.info("packages:{}", packages);

                log.info("package:{}", packages.get("package"));

                List<DeliveryOrderConfirmRequest.QiMenPackage> packageList = deliveryOrderConfirmRequest.getPackages();
//                Integer amount = qimenPushService.getActualAmount(deliveryOrderCode);
//                if(amount != Integer.parseInt(actualQty)){
//                    //当计划数量与接受数量不一致，需要抛错，或者添加中间状态：类似（推单失败之类）
//                    log.info("当计划数量与接受数量不一致,订单:{}", deliveryOrderCode);
//                    MallOrderInfo orderInfo = new MallOrderInfo();
//                    orderInfo.setOrderId(deliveryOrderCode);
//                    orderInfo.setQmType("5");
//                    orderInfoService.updateOrderByIdNew(orderInfo);
//                    return getFormatFailure("500","当计划数量与接受数量不一致,订单:"+deliveryOrderCode);
//                }


                //发货包裹信息
//                List<QiMenPackage> packageList = JSON.parseArray(JSON.parse(packages.get("package").toString()).toString(), QiMenPackage.class);
//                List<QiMenPackage> packageList = JSONArray.parseArray(JSONObject.toJSONString(packages.get("package")), QiMenPackage.class);

                log.info("发货包裹信息:{}", packageList);

                StringBuilder sb = new StringBuilder();
                StringBuilder itemSb = new StringBuilder();
                for (int i = 0; i < packageList.size(); i++) {
                    List<QiMenItem> items = packageList.get(i).getItems();
                    String expressCode = packageList.get(i).getExpressCode();
                    if (i == 0) {
                        sb.append(expressCode);
                        itemSb.append(items);
                    } else {
                        sb.append(",").append(expressCode);
                        itemSb.append(";").append(itemSb);
                    }
                }
                String expressCode = sb.toString();
                String items = itemSb.toString();


//                JSONObject packagesJson = JSONObject.parseObject(packages + "");
//                Object packageObj = packagesJson.get("package");
//                JSONObject packageJson = JSONObject.parseObject(packageObj + "");
//                String expressCode = (String) packageJson.get("expressCode");
//                String logisticsCode = (String) packageJson.get("logisticsCode");

                //商品明细信息
//                Object orderLines = jsonObject.get("orderLines");
//                log.info("商品明细信息:{}",orderLines);

//                HashMap hashMap = orderLineHashMaps.get(0);
//                String itemCode = hashMap.get("itemCode")+"";
//                String planQty = hashMap.get("planQty") + "";
//                String actualQty = hashMap.get("actualQty") + "";
//                if(!planQty.equals(actualQty)){
//                    //todo 当计划数量与接受数量不一致，需要抛错，或者添加中间状态：类似（推单失败之类）
//
//                }

                //todo 发货成功，则修改单据状态等操作
                MallOrderInfo orderInfo = new MallOrderInfo();
                orderInfo.setOrderId(deliveryOrderCode);
                orderInfo.setQmType("3");
                orderInfo.setExpressCompany(packageList.get(0).getLogisticsCode());
                orderInfo.setExpressCode(expressCode);
                orderInfo.setItemsPackage(items);
                boolean b = orderInfoService.updateOrderByIdNew(orderInfo);
                if (!b) {
                    return getFormatFailure("接收失败");
                }
            } else {
                return getFormatFailure("接收失败");
            }
            return getFormat("接收成功");
        } catch (Exception ex) {
            log.info("新亦源回调失败:{0}", ex);
            return ResponseCreators.fail(ex);
        }
    }


    public String getFormat(String message) {
        return String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<response><flag>success</flag><code>%s</code><message>%s</message></response>", null, message);
    }

    public String getFormatFailure(String message) {
        return String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<response><flag>failure</flag><code>%s</code><message>%s</message></response>", null, message);
    }

    public String getFormatFailure(String code, String message) {
        return String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<response><flag>failure</flag><code>%s</code><message>%s</message></response>", code, message);
    }

    public Boolean qiMenAPIStorage(@RequestParam("method") String method, @RequestBody String o) throws Exception {
//
//
//        if("taobao.qimen.entryorder.confirm".trim().equals(method.trim()) || "entryorder.confirm".trim().equals(method.trim())){
//            log.info("奇门入库回调:{}",o);
//
//            //回调例子
//
//            //<request>
//            //    <entryOrder>
//            //        <ownerCode>xyybs</ownerCode>
//            //        <entryOrderCode>00000002_ZDTHD000050</entryOrderCode>
//            //        <entryOrderId>00000002_ZDTHD000050</entryOrderId>
//            //        <outBizCode>00000002_ZDTHD000050</outBizCode>
//            //        <warehouseCode>B1</warehouseCode>
//            //        <confirmType>0</confirmType>
//            //        <entryOrderType>QTRK</entryOrderType>
//            //        <status>FULFILLED</status>
//            //        <operateTime>2020-01-20 10:28:31</operateTime>
//            //        <remark>麦金森</remark>
//            //    </entryOrder>
//            //    <orderLines>
//            //        <orderLine>
//            //            <orderLineNo>1</orderLineNo>
//            //            <itemCode>9341082001570</itemCode>
//            //            <ownerCode>xyybs</ownerCode>
//            //            <actualQty>5</actualQty>
//            //            <inventoryType>ZP</inventoryType>
//            //        </orderLine>
//            //        <orderLine>
//            //            <orderLineNo>2</orderLineNo>
//            //            <itemCode>9341082001686</itemCode>
//            //            <ownerCode>xyybs</ownerCode>
//            //            <actualQty>7</actualQty>
//            //            <inventoryType>ZP</inventoryType>
//            //        </orderLine>
//            //        <orderLine>
//            //            <orderLineNo>3</orderLineNo>
//            //            <itemCode>9341082002041</itemCode>
//            //            <ownerCode>xyybs</ownerCode>
//            //            <actualQty>1</actualQty>
//            //            <inventoryType>ZP</inventoryType>
//            //        </orderLine>
//            //        <orderLine>
//            //            <orderLineNo>4</orderLineNo>
//            //            <itemCode>9341082002065</itemCode>
//            //            <ownerCode>xyybs</ownerCode>
//            //            <actualQty>1</actualQty>
//            //            <inventoryType>ZP</inventoryType>
//            //        </orderLine>
//            //    </orderLines>
//            //</request>
//
//
//
//
////
////            JSONObject request = XmltoJson.xmltoJson(o);
////            Object jsonObject = request.get("request");
////            Object object = jsonObject.get("deliveryOrder");
////            JSONObject deliveryOrder = JSONObject.parseObject(object + "");
////            String deliveryOrderCode =  deliveryOrder.get("deliveryOrderCode")+"";//我方业务单号id
////
////            Object orderLines = jsonObject.get("orderLines");
////            List<HashMap> hashMaps = com.meifute.core.mmall.common.utils.ObjectUtils.castList(orderLines, HashMap.class);
////            HashMap hashMap = hashMaps.get(0);
////            String itemCode = hashMap.get("itemCode")+"";
////
////            String planQty = hashMap.get("planQty") + "";
////            String actualQty = hashMap.get("actualQty") + "";
////            if(!planQty.equals(actualQty)){
////                //todo 当计划数量与接受数量不一致，需要抛错，或者添加中间状态：类似（推单失败之类）
////
////            }
////            qimenPushService.DeliveryOrder(deliveryOrderCode,itemCode);
//
//
//
//        }else if("taobao.qimen.deliveryorder.confirm".trim().equals(method.trim()) || "deliveryorder.confirm".trim().equals(method.trim())){
//            log.info("奇门发货回调:{}",o);
//            //<request>
//            //    <deliveryOrder>
//            //        <deliveryOrderCode>XZXS2001230288</deliveryOrderCode>
//            //        <ownerCode>XYY201</ownerCode>
//            //        <deliveryOrderId>XZXS2001230288</deliveryOrderId>
//            //        <orderType>JYCK</orderType>
//            //        <warehouseCode>WX</warehouseCode>
//            //        <outBizCode>XZXS2001230288</outBizCode>
//            //        <confirmType>0</confirmType>
//            //        <orderConfirmTime>2020-01-23 15:24:04</orderConfirmTime>
//            //        <operatorName>戴艳</operatorName>
//            //        <operateTime>2020-01-23 15:24:04</operateTime>
//            //    </deliveryOrder>
//            //    <packages>
//            //        <package>
//            //            <logisticsCode>STO2</logisticsCode>
//            //            <expressCode>773024447089377</expressCode>
//            //            <weight>0.0</weight>
//            //            <items>
//            //                <item>
//            //                    <itemCode>6001280065103</itemCode>
//            //                    <itemId>6001280065103</itemId>
//            //                    <quantity>1</quantity>
//            //                </item>
//            //            </items>
//            //        </package>
//            //    </packages>
//            //    <orderLines>
//            //        <orderLine>
//            //            <orderLineNo></orderLineNo>
//            //            <ownerCode>XYY201</ownerCode>
//            //            <itemCode>6001280065103</itemCode>
//            //            <planQty>0</planQty>
//            //            <actualQty>1</actualQty>
//            //        </orderLine>
//            //    </orderLines>
//            //</request>
//
//            JSONObject jsonObject = XmltoJson.xmltoJson(o);
//            Object object = jsonObject.get("deliveryOrder");
//            JSONObject deliveryOrder = JSONObject.parseObject(object + "");
//            String deliveryOrderCode =  deliveryOrder.get("deliveryOrderCode")+"";//我方业务单号id
//
//            Object orderLines = jsonObject.get("orderLines");
//            List<HashMap> hashMaps = com.meifute.core.mmall.common.utils.ObjectUtils.castList(orderLines, HashMap.class);
//            HashMap hashMap = hashMaps.get(0);
//            String itemCode = hashMap.get("itemCode")+"";
//
//            String planQty = hashMap.get("planQty") + "";
//            String actualQty = hashMap.get("actualQty") + "";
//            if(!planQty.equals(actualQty)){
//                //todo 当计划数量与接受数量不一致，需要抛错，或者添加中间状态：类似（推单失败之类）
//
//            }
//            qimenPushService.DeliveryOrder(deliveryOrderCode,itemCode);
//
//        }else {
//            log.info("奇门其他回调..:{}",method);
//        }

        return true;
    }

    @Override
    public Integer getNewGoodsCount(@RequestParam("userId") String userId) {
        return orderInfoService.getNewGoodsCount(userId);
    }

    @Override
    public Integer getNewGoodsCount2(@RequestParam("userId") String userId) {
        return orderInfoService.getNewGoodsCount2(userId);
    }

    @Override
    public Integer getTakeOfItemCount(@RequestBody CheckOutGoods checkOutGoods) {
        return orderInfoService.getTakeOfItemCount(checkOutGoods);
    }

    @Override
    public Integer getTakeOfItemCountEveryDay(@RequestBody CheckOutGoods checkOutGoods) {
        return orderInfoService.getTakeOfItemCountEveryDay(checkOutGoods);
    }

    @Override
    public JSONObject wuYouUpdateOrderExpressCode(@RequestBody String json) {
        JSONObject result = wuYouService.updateOrderExpressInfo(json);
        return result;
    }

    @Override
    public String getHighestExpressCode(@RequestParam("codes") List<String> codeList) {
        log.info("--------物流code:{}", codeList);
        return expressCompanyService.getHighestExpressCode(codeList);
    }

    @Override
    public List<String> getOrderIdsByPayInfoRefundParam(@RequestBody PayInfoRefundVO param) {
        return orderInfoService.getOrderIdsByPayInfoRefundParam(param);
    }

    @Override
    public List<SkuSpecStockVO> queryStock(@RequestParam("goodsNo") String goodsNo, @RequestParam("currentPage") Integer currentPage, @RequestParam("pageSize") Integer pageSize) {
        List<SkuSpecStockVO> itemStocks = jdExpressPushService.queryStock(goodsNo, currentPage, pageSize);
        return itemStocks;
    }


    public List<OrderInfoByInputDTO> getThreeOrder(@RequestParam("userId") String userId) {
        return mallOrderInfoMapper.getThreeOrder(userId);
    }

    @Override
    public MallTransferGoods getTransferByUserId(@RequestParam("userId") String userId) {
        List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>().eq("is_del", "0")
                .eq("next_proxy_id", userId)
                .eq("relation_type", "0")
                .orderBy("create_date", false));
        if (CollectionUtils.isEmpty(mallTransferGoods))
            return null;
        return mallTransferGoods.get(0);
    }

    @Override
    public int getNotFinishOrder(@RequestParam("userId") String userId) {
        List<MallOrderInfo> result = mallOrderInfoMapper.getNotFinishOrder(userId);
        if (CollectionUtils.isEmpty(result))
            return 0;
        return result.size();
    }

    @Override
    public List<UserOrderAmountDTO> queryOrderAmountByDate(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate, @RequestParam("yestodayDate") String yestodayDate) {

        return orderInfoService.queryOrderAmountByDate(startDate, endDate, yestodayDate);
    }

    @Override
    public List<UserOrderAmountDTO> queryOrderAmountByUserIds(@RequestBody UserIdsDateParam param) {
        return orderInfoService.queryOrderAmountByUserIds(param);
    }

    @Override
    public List<String> getOrderIdsByStockInfoParam(@RequestParam("userId") String userId, @RequestParam("param") String param) {
        return orderInfoService.getOrderIdsByStockInfoParam(userId, param);
    }

    @Override
    public AsyncTaskInfo queryWaitTask() {
        return asyncTaskInfoService.queryWaitTask();
    }

    @Override
    public void exportProductOrder(@RequestBody AsyncTaskParam param) throws Exception {
        mallProdOrderExpService.exportExecl(param);
    }

    @Override
    public void updateAsyncTask(@RequestBody AsyncTaskInfo taskInfo) {
        asyncTaskInfoService.updateById(taskInfo);
    }

    @Override
    public List<AsyncTaskInfo> queryLongTimeDoingTask() {
        return asyncTaskInfoService.queryLongTimeDoingTask();
    }

    @Override
    public void updateAsyncTasks(@RequestBody AsyncTaskVo taskVo) {
        asyncTaskInfoService.updateBatchById(taskVo.getAsyncTaskInfos());
    }

    @Override
    public Boolean sendEMail(@RequestParam(value = "flag", required = false) String flag) {
        try {
            orderInfoService.sendEMail(flag);
        } catch (Exception e) {
            log.info("发送邮件异常:{}", e);
            return false;
        }
        return true;
    }


}
