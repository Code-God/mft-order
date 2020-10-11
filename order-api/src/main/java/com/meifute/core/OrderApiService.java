package com.meifute.core;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.dto.*;
import com.meifute.core.entity.*;
import com.meifute.core.entity.order.AsyncTaskInfo;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.vo.*;
import com.meifute.core.vo.order.AsyncTaskParam;
import com.meifute.core.vo.order.AsyncTaskVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wxb
 * @Date: 2018/10/20 14:31
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Api(description = "内部系统接口")
@RequestMapping("/api/implement/order")
public interface OrderApiService {

    @ApiOperation(value = "查询订单信息总数", notes = "查询订单信息总数")
    @PostMapping("query/order/count")
    Integer queryOrderInfoCount(@RequestBody MallOrderInfo param);

    @ApiOperation(value = "根据orderId查询订单信息", notes = "根据orderId查询订单信息")
    @GetMapping("/get/order/by/id")
    MallOrderInfo getMallOrderInfoById(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "根据orderId修改订单信息", notes = "根据orderId修改订单信息")
    @PutMapping("/update/order")
    Boolean updateMallOrderInfoById(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "添加订单审核单", notes = "添加订单审核单")
    @PostMapping("/add/order/verify")
    Boolean insertOrderVerify(@RequestBody MallOrderVerify mallOrderVerify);

    @ApiOperation(value = "根据orderId查询购买商品信息", notes = "根据orderId查询购买商品信息")
    @GetMapping("/get/items/by/orderid")
    List<MallOrderItem> getMallOrderItemListByOrderId(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "根据orderId修改订单商品信息", notes = "根据orderId修改订单商品信息")
    @PutMapping("/update/items/by/orderid")
    Boolean updateMallOrderItemInfoById(@RequestBody MallOrderItem mallOrderItem);

    @ApiOperation(value = "查用户的订单个数", notes = "查用户的订单个数")
    @PostMapping("/get/order/count")
    Integer queryOrderInfoCountByUserId(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "添加订单信息", notes = "添加订单信息")
    @PostMapping("/add/order/info")
    MallOrderInfo insertOrderInfo(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "添加调剂单信息", notes = "添加调剂单信息")
    @PostMapping("/add/regulate/info")
    Boolean insertRegulateInfo(@RequestBody MallRegulateInfo mallRegulateInfo);


    @ApiOperation(value = "添加调剂单信息v2", notes = "添加调剂单信息v2")
    @PostMapping("/add/regulate/info/v2")
    Boolean insertRegulateInfoV2(@RequestBody MallRegulateInfo mallRegulateInfo);

    @ApiOperation(value = "添加调剂单商品信息", notes = "添加调剂单商品信息")
    @PostMapping("/add/regulate/item/info")
    Boolean insertRegulateItem(@RequestBody MallRegulateItem mallRegulateItem);

    @ApiOperation(value = "添加转货商品信息", notes = "添加转货商品信息")
    @PostMapping("/add/transfer/item/info")
    Boolean insertTransferItemInfo(@RequestBody MallTransferItem mallTransferItem);

    @ApiOperation(value = "添加订单商品信息", notes = "添加订单商品信息")
    @PostMapping("/add/order/item/info")
    Boolean insertOrderItemInfo(@RequestBody MallOrderItem mallOrderItem);

    @ApiOperation(value = "敏感产品审核", notes = "敏感产品审核")
    @PostMapping("/order/verify/review")
    String sensitiveGoodsVerify(@RequestBody ReviewGoodsVerifyParam reviewGoodsVerifyParam);

    @ApiOperation(value = "敏感产品审核列表", notes = "敏感产品审核列表")
    @PostMapping("/order/verify/list")
    PageDto<OrderVerifyDto> sensitiveOrderInfoList(@RequestBody AdminReviewVerifyParam adminReviewVerifyParam);

    @PostMapping("/order/transferOrderInfos")
    @ApiOperation(value = "云转货单列表", notes = "云转货单列表")
    PageDto<OrderTransferInfoPageDto> queryMallTransferOrders(@RequestBody GetOrderTransferPageListParam pageListParam);

    @PostMapping("/general/infos")
    @ApiOperation(value = "入云单列表", notes = "入云单列表")
    PageDto<OrderInfoPageDto> getGeneralOrderInfoPageList(@RequestBody GetOrderPageListParam pageListParam);

    @PostMapping("/verify/infos")
    @ApiOperation(value = "后台订单审核列表", notes = "后台订单审核列表")
    PageDto<OrderVerifyPageDto> queryOrderVerifyPageList(@RequestBody GetOrderVerifyPageListParam pageListParam);

    @PostMapping("/update/orderinfo")
    @ApiOperation(value = "更新訂單信息", notes = "更新訂單信息")
    void updateOrderInfo(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "调剂单列表", notes = "调剂单列表")
    @PostMapping("/regulate/order/infos")
    PageDto<RegulateDto> queryMallRegulatePageList(@RequestBody GetRegulateParam param);

    @ApiOperation(value = "获取调剂单", notes = "获取调剂单")
    @GetMapping("/regulate/info/id")
    MallRegulateInfo regulationInfoGet(@RequestParam("id") String id);

    @ApiOperation(value = "换货单列表", notes = "换货单列表")
    @PostMapping("/exchange/order/infos")
    PageDto<OrderExchangeInfoDto> queryExchangeInfoList(@RequestBody GetExchangeOrderParam param);

    @ApiOperation(value = "校验调剂单是否存在", notes = "校验调剂单是否存在")
    @GetMapping("/regulate/id")
    List<MallRegulateInfo> checkRegulationInfoExist(@RequestParam("id") String id);

    @ApiOperation(value = "所有产品订单列表", notes = "所有产品订单列表")
    @PostMapping("/all/order/infos")
    PageDto<AllOrderInfoDto> queryAllOrderInfoList(@RequestBody GetOrderPageListParam param);

    @ApiOperation(value = "所有积分订单列表", notes = "所有积分订单列表")
    @PostMapping("/all/credit/order/infos")
    PageDto<AllCreditOrderDto> queryCreditOrderInfoPages(@RequestBody GetOrderPageListParam param);

//    @ApiOperation(value = "创建退款订单", notes = "创建退款订单")
//    @PostMapping("/create/refund/order/info")
//    MallOrderInfo createRefundOrderInfo(@RequestBody RefundOrderInfoParam refundOrderInfoParam);

    @ApiOperation(value = "根据订单号查询审核单列表", notes = "根据订单号查询审核单列表")
    @GetMapping("/get/verify/order/by/orderid")
    List<MallOrderVerify> getMallOrderVerifyByOrderId(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "更新审核单", notes = "更新审核单")
    @PutMapping("/update/verify/order/info")
    Boolean updateVerifyOrder(@RequestBody MallOrderVerify mallOrderVerify);

    @ApiOperation(value = "查用户订单列表", notes = "查用户订单列表")
    @PostMapping("/get/user/order/infos")
    Page<MallOrderInfo> getUserOrderInfos(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "查询订单的商品", notes = "查询订单的商品")
    @GetMapping("/get/order/items/by/orderid")
    List<OrderItemDetailDto> orderItemByOrderId(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "查询审核单", notes = "查询审核单")
    @GetMapping("/get/order/verify/by/id")
    MallOrderVerify getOrderVerifyById(@RequestParam("id") String id);

    @ApiOperation(value = "添加主商品", notes = "添加主商品")
    @PostMapping("/add/transport/goods/info")
    void addTransportGoodsInfo(@RequestBody AddTransportGoodsParams params);

    @ApiOperation(value = "超时未支付取消 (现产品商场的订单改为24小时取消，活动订单为30分钟)", notes = "超时未支付取消 (现产品商场的订单改为24小时取消，活动订单为30分钟)")
    @PostMapping("/to/order/delay/by/id")
    void orderDelayListener(@RequestBody OrderInfoMessage orderInfoMessage);

    @ApiOperation(value = "订单延迟处理-三天审核过期", notes = "订单延迟处理-三天审核过期")
    @PostMapping("/to/order/delay/verify/cancel")
    void delayOrderVerifyListener(@RequestBody OrderInfoMessage orderInfoMessage);

    @ApiOperation(value = "通过id查询一条订单", notes = "通过id查询一条订单")
    @PostMapping("/query/order/by/id")
    MallOrderInfo queryOrderByOrderId(@RequestBody GetOrderInfo getOrderInfo);

    @ApiOperation(value = "延迟京东推单", notes = "延迟京东推单")
    @PostMapping("/jd/delay/push/express")
    Boolean JdExpressDelayListener(@RequestBody OrderInfoMessage orderInfoMessage);

    @ApiOperation(value = "后台推单", notes = "后台推单")
    @PostMapping("/jd/push/express/from/admin")
    JdPushFromAdmin pushToJdFromAdmin(@RequestBody OrderInfoMessage orderInfoMessage);

    @ApiOperation(value = "获取某个代理订单总数和总额", notes = "获取某个代理订单总数和总额")
    @GetMapping("/get/order/count/total/amt")
    OrderCountAndAmt queryCountAmtByUserId(@RequestParam("userId") String userId);

    @ApiOperation(value = "获取团队的订单总数和总额", notes = "获取团队的订单总数和总额")
    @PostMapping("/get/order/team/total/amt")
    OrderCountAndAmt queryTeamTotalAmt(@RequestBody List<String> userIds);

    @ApiOperation(value = "查询订单出库单号和运单号", notes = "获取所有订单")
    @PostMapping("/get/order/all/infos")
    String queryAllOrder(@RequestBody GetOrderInfo getOrderInfo);

    @ApiOperation(value = "查询所有订单问题反馈补发的运单号", notes = "查询所有订单问题反馈补发的运单号")
    @PostMapping("/get/express/code/feedback")
    void getExpressCodeFeedback(@RequestBody GetOrderInfo getOrderInfo);

    @ApiOperation(value = "更新订单", notes = "更新订单")
    @PostMapping("/upadte/order/infos")
    void updateMallOrderInfo(@RequestBody MallOrderInfo mallOrderInfo);

    @ApiOperation(value = "确认收货", notes = "更新订单")
    @PostMapping("/received/item/order")
    Boolean receivingItem(@RequestBody GetOrderInfo info);

    @ApiOperation(value = "小罗后台推单", notes = "小罗后台推单")
    @GetMapping("/some/times/to/push/")
    void pushAllOrder();

    @ApiOperation(value = "根据orderId查询订单信息", notes = "根据orderId查询订单信息")
    @GetMapping("/get/orderInfo/by/id")
    MallOrderInfo getMallOrderByIdNoError(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "获取个人中心主页订单信息2.0", notes = "获取个人中心主页订单信息2.0")
    @GetMapping("/v2/get/personal/order/info")
    MallPersonalCenterDto queryPersonalOrderInfo(@RequestParam("userId") String userId);

    @ApiOperation(value = "根据type获取ids", notes = "根据type获取ids")
    @GetMapping("/v2/get/order/ids/by/type")
    List<String> getOrderIdsByType(@RequestParam("orderType") String orderType);

    @ApiOperation(value = "获取首次出货订单", notes = "获取首次出货订单")
    @GetMapping("/v2/get/first/out/order")
    MallOrderInfo getFirstOutItemOrderInfo(@RequestParam("mallUserId") String mallUserId);

    @ApiOperation(value = "自动收货job", notes = "自动收货job")
    @PostMapping("/v2/task/receiving/goods")
    String receivingGoodsFromJd(@RequestBody GetOrderInfo getOrderInfo);

    @ApiOperation(value = "超过15天强制自动收货", notes = "超过15天强制自动收货")
    @PostMapping("/v2/task/force/receiving/goods")
    String forceReceivingGoodsFromJd(@RequestBody GetOrderInfo getOrderInfo);

    @ApiOperation(value = "获取那些没有推的单push", notes = "获取push")
    @GetMapping("/v2/get/push/order")
    List<MallOrderToPush> getOrderToPush();

    @ApiOperation(value = "推那些没有推的单push", notes = "推那些没有推的单push")
    @PostMapping("/v2/to/push/order")
    void toPushOrderToPush(@RequestBody List<MallOrderToPush> list);

    @ApiOperation(value = "插入那些没有推的单push", notes = "插入那些没有推的单push")
    @GetMapping("/v2/insert/push/order")
    void insertPush(@RequestParam("orderId") String orderId);

    @ApiOperation(value = "查看地址能发推送jd", notes = "查看地址能发推送jd")
    @PostMapping("/v2/check/iscan/jd")
    Boolean jdCheckPushAddressService(@RequestBody MallOrderInfo info);

    @ApiOperation(value = "查询代理升级的订单的商品总数", notes = "查询代理升级的订单的商品总数")
    @GetMapping("/v2/get/transfer/itemSum")
    BigDecimal selectSumItemByAgentUpGrade(@RequestParam("relationId") String relationId);

    @ApiOperation(value = "根据orderId查询转货信息")
    @GetMapping("/get/transfer/by/order/id")
    List<MallTransferGoods> getTransferGoodById(@RequestParam("orderId") String orderId);


    @GetMapping("/get/regulate/by/id")
    MallRegulateInfo getRegulateInfoById(@RequestParam("id") String id);

    @GetMapping("/get/transfer/by/id")
    MallTransferGoods getTransFerById(@RequestParam("id") String id);


    @GetMapping("/get/regulate/item/by/regulate/id")
    MallRegulateItem getRegulateItemByRegulateId(@RequestParam("id") String id);

    @ApiOperation(value = "根据条件查询订单")
    @PostMapping("/query/order/list/by/param")
    List<MallOrderInfo> queryOrderListByParam(@RequestBody MallOrderInfo param);

    @ApiOperation(value = "根据条件查询云调剂单")
    @PostMapping("/query/regulation/info/list/by/param")
    List<MallRegulateInfo> queryRegulationInfoListByParam(@RequestBody MallRegulateInfo param);

    //----------------------------出库价格详情----------------------------
    @ApiOperation(value = "记录出库价格详情", notes = "记录出库价格详情")
    @PostMapping("/save/order/price/detail")
    Boolean saveOrderPriceDetail(@RequestBody MallOrderPriceDetail param);
    //----------------------------出库价格详情----------------------------

    @GetMapping("/get/waybill/order/by/origin")
    String queryJDOrderWayBill(@RequestParam("eclpSoNo") String eclpSoNo, @RequestParam("originOrder") Integer originOrder, @RequestParam("orderId") String orderId);

    @PostMapping("/update/sms/ac/verify")
    void updateSMSAcVerify(@RequestBody SMSAcOrderVerify smsAcOrderVerify);

    @GetMapping("/get/now/days/c036/count")
    Integer getNowDaysC036(@RequestParam("userId") String userId);

    @GetMapping("/get/now/days/c036/count/new")
    Integer getNowDaysC036New(@RequestParam("userId") String userId);

    @PostMapping("/push/jd/temporary")
    void temporaryPushToJd(@RequestBody TemPushToJd temPushToJd);

    @PostMapping("temporary/not/push/jd/phones")
    void setTemporaryNotPushPhone(@RequestBody TemPushToJd temPushToJd);


    //==============奇门回调接口==============
    @PostMapping(value = "/qimen", produces = "text/xml")
    String qiMenAPIStorage(@RequestBody String requestBody, @RequestParam Map<String, String> requestParams) throws Exception;


    @GetMapping(value = "/qimen/sync/item")
    Boolean syncItem(@RequestParam("skuCode") String skuCode);


    @GetMapping("/get/new/goods/count/209")
    Integer getNewGoodsCount(@RequestParam("userId") String userId);

    @GetMapping("/get/new/goods/count/209/2")
    Integer getNewGoodsCount2(@RequestParam("userId") String userId);

    @PostMapping("/get/takeof/item/count/new")
    Integer getTakeOfItemCount(@RequestBody CheckOutGoods checkOutGoods);

    @PostMapping("/get/every/takeof/item/count/new")
    Integer getTakeOfItemCountEveryDay(@RequestBody CheckOutGoods checkOutGoods);

    //===================无忧接口回调===================
    @PostMapping(value = "/wuyou/thirdParty", produces = {"application/json;charset=utf-8"})
    JSONObject wuYouUpdateOrderExpressCode(@RequestBody String json);


    @GetMapping("/get/highest/express/company/by/code")
    String getHighestExpressCode(@RequestParam("codes") List<String> codeList);

    @PostMapping("/get/order/ids/by/refund/param")
    List<String> getOrderIdsByPayInfoRefundParam(PayInfoRefundVO param);

    @GetMapping("/queryStock")
    public List<SkuSpecStockVO> queryStock(@RequestParam("goodsNo") String goodsNo, @RequestParam("currentPage") Integer currentPage, @RequestParam("pageSize") Integer pageSize);

    @GetMapping("/get/three/order")
    List<OrderInfoByInputDTO> getThreeOrder(@RequestParam("userId") String userId);

    @GetMapping("/get/transfer/by/userId")
    MallTransferGoods getTransferByUserId(@RequestParam("userId") String userId);

    @GetMapping("/get/not/finish/order")
    int getNotFinishOrder(@RequestParam("userId") String userId);

    @GetMapping("/query/user/order/amount")
    List<UserOrderAmountDTO> queryOrderAmountByDate(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate, @RequestParam("yestodayDate") String yestodayDate);

    @PostMapping("/query/user/order/amount/by/userids")
    List<UserOrderAmountDTO> queryOrderAmountByUserIds(@RequestBody UserIdsDateParam param);

    @GetMapping("/query/cloud/stock/order/ids")
    List<String> getOrderIdsByStockInfoParam(@RequestParam("userId") String userId, @RequestParam("param") String param);

    @GetMapping("/query/wait/task")
    AsyncTaskInfo queryWaitTask();

    @PostMapping("/export/product/order")
    void exportProductOrder(@RequestBody AsyncTaskParam exportParam) throws Exception;

    @PostMapping("/update/task")
    void updateAsyncTask(@RequestBody AsyncTaskInfo taskInfo);


    @ApiOperation(value = "发送当日提货大于10盒的邮件")
    @GetMapping("/send/email/about/stock")
    Boolean sendEMail(@RequestParam(value = "flag", required = false) String flag);

    @GetMapping("/query/long/time/doing/task")
    List<AsyncTaskInfo> queryLongTimeDoingTask();

    @PostMapping("/update/tasks")
    void updateAsyncTasks(@RequestBody AsyncTaskVo taskVo);
}

