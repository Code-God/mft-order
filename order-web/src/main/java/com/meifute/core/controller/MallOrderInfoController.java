package com.meifute.core.controller;


import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.*;
import com.meifute.core.dto.report.InputOutputItemReportDTO;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.mmall.common.utils.AliExpress;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.StringUtils;
import com.meifute.core.service.CheckOrderService;
import com.meifute.core.service.ExpressCompanyService;
import com.meifute.core.service.JDCheckPushAddressService;
import com.meifute.core.service.MallOrderInfoService;
import com.meifute.core.util.JsonUtils;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author wuxb
 * @since 2018-09-25
 */
@RestController
@RequestMapping("v1/app/ordercenter")
@Api(tags = "orderCenter", description = "订单中心")
@Slf4j
public class MallOrderInfoController extends BaseController {

    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private CheckOrderService checkOrderService;
    @Autowired
    private JDCheckPushAddressService jdCheckPushAddressService;
    @Autowired
    UserFeign userFeign;
    @Autowired
    private ExpressCompanyService expressCompanyService;

    /**
     * 从购物车中提交订单
     *
     * @return
     */
    @ApiOperation(value = "从购物车中提交订单", notes = "从购物车中提交订单")
    @RequestMapping(value = "/order/from/cart", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<OrderInfoDto>> preOrderFromCart(@RequestBody PreOrderFromCartParam preOrderFromCartParam) {
        //todo 禁用
        if ("1".equals(RedisUtil.get("closeFlag"))) {
            throw new MallException("060090");
        }

        //收货地区拦截校验
        String message = userFeign.checkoutExcludeAddress(preOrderFromCartParam.getAddrId());
        if (!StringUtils.isEmpty(message)) {
            MallResponse r = new MallResponse();
            return ResponseEntity.ok(failedResult(r, "500", message, null));
        }

        MallUser user = UserUtils.getCurrentUser();
        MallAgent agent = agentFeign.getAgentByUserId(user.getId());
        user.setRoleId(agent == null ? "0" : agent.getAgentLevel());

        //普通用户没有下单的权力
        boolean noVip = agent == null || Integer.parseInt(agent.getAgentLevel()) == 0;
        MallPreconditions.checkToError(noVip, OrderRespCode.ORDINARY_USER_NOTTOBUY);

        preOrderFromCartParam.setUser(user);
        preOrderFromCartParam.setMallUserId(user.getId());
        //下单
        OrderInfoDto orderInfoDto = orderInfoService.preOrderFromCart(preOrderFromCartParam);

        checkZeroToPay(orderInfoDto, preOrderFromCartParam.getOrderType(), user, agent.getCompanyId());
        return ResponseEntity.ok(successResult(orderInfoDto));
    }

    /**
     * 从商城直接下单
     *
     * @return
     */
    @ApiOperation(value = "从商城直接下单", notes = "从商城直接下单")
    @RequestMapping(value = "/order/from/item", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<OrderInfoDto>> preOrderFromGoods(@RequestBody PreOrderFromGoodsParam preOrderFromGoodsParam) {
        //todo 禁用
        if ("1".equals(RedisUtil.get("closeFlag"))) {
            throw new MallException("060090");
        }
        //收货地区拦截校验
        String message = userFeign.checkoutExcludeAddress(preOrderFromGoodsParam.getAddrId());
        if (!StringUtils.isEmpty(message)) {
            MallResponse r = new MallResponse();
            return ResponseEntity.ok(failedResult(r, "500", message, null));
        }

        //查询用户信息
        MallUser mallUser = UserUtils.getCurrentUser();
        MallAgent agent = agentFeign.getAgentByUserId(mallUser.getId());
        OrderInfoDto orderInfoDto = orderInfoService.preOrderFromGoods(preOrderFromGoodsParam, mallUser);
        log.info("###################### MallOrderInfoDto:{}", orderInfoDto);

        orderInfoDto.setPayTypeKey(orderInfoService.getPayType("1", agent.getCompanyId()));
        return ResponseEntity.ok(successResult(orderInfoDto));
    }


    private void checkZeroToPay(OrderInfoDto orderInfoDto, String orderType, MallUser user, String companyId) {
        BigDecimal totalAmt = orderInfoDto.getOrderInfoList().stream().map(MallOrderInfo::getPaymentAmt).reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("###################### totalAmt:{}", totalAmt);
        orderInfoService.zeroToPay(orderInfoDto, orderType, totalAmt, user);
        orderInfoDto.setPayTypeKey(orderInfoService.getPayType(orderInfoDto.getOrderInfoList().get(0).getCurrency(), companyId));
        orderInfoDto.setCredit(ObjectUtils.isNullOrEmpty(orderInfoDto.getCredit()) ? new BigDecimal(0) : orderInfoDto.getCredit());
    }

    /**
     * 获取运费
     *
     * @return
     */
    @ApiOperation(value = "获取运费", notes = "获取运费")
    @RequestMapping(value = "/order/freight", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<BigDecimal>> getPostFee(@RequestBody GetPostFeeParam getPostFeeParam) {
        //收货地区拦截校验
        String message = userFeign.checkoutExcludeAddress(getPostFeeParam.getAddrId());
        if (!StringUtils.isEmpty(message)) {
            MallResponse r = new MallResponse();
            return ResponseEntity.ok(failedResult(r, "500", message, BigDecimal.ZERO));
        }
        //todo 算运费
//        if ("1".equals(getPostFeeParam.getLogisticsType())) {
//            String count = RedisUtil.get("sf:order_count");
//            if (count != null && Integer.parseInt(count) > Integer.parseInt(RedisUtil.get("sf:order_re_count"))) {
//                throw new MallException("00998", new Object[]{"今日顺丰快递单量已满，请更换物流方式"});
//            }
//        }

        BigDecimal postFee = orderInfoService.getPostFee(getPostFeeParam);
        return ResponseEntity.ok(successResult(postFee));
    }

    /**
     * 获取运费列表
     *
     * @return
     */
    @ApiOperation(value = "获取运费列表", notes = "获取运费列表")
    @RequestMapping(value = "/order/freight/list", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<List<FreightDTO>>> getPostFeeList(@RequestBody GetPostFeeParam getPostFeeParam) {
        //收货地区拦截校验
        String message = userFeign.checkoutExcludeAddress(getPostFeeParam.getAddrId());
        if (!StringUtils.isEmpty(message)) {
            MallResponse r = new MallResponse();
            return ResponseEntity.ok(failedResult(r, "500", message, new ArrayList<>()));
        }
        List<FreightDTO> postFeeList = orderInfoService.getPostFeeList(getPostFeeParam);
        return ResponseEntity.ok(successResult(postFeeList));
    }

    /**
     * 检查是否可以生成订单
     *
     * @return
     */
    @ApiOperation(value = "检查是否可以生成订单", notes = "检查是否可以生成订单")
    @RequestMapping(value = "/check/create/order", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> checkIsCanCreateOrder(@RequestBody CheckOrderGoodsParam checkOrderGoodsParam) {
        Boolean result = checkOrderService.checkOrderGoods(checkOrderGoodsParam);
        return ResponseEntity.ok(successResult(result));
    }

    /**
     * 京东回调
     *
     * @return
     */
    @ApiOperation(value = "京东回调", hidden = true)
    @RequestMapping(value = "/jd/notify", method = RequestMethod.GET)
    public ResponseEntity<MallResponse> jdExpressNotify() {
        String code = request.getParameter("code");
        return ResponseEntity.ok(successResult(code));
    }


    @ApiOperation(value = "获取订单列表")
    @PostMapping("/get/order/infos")
    public ResponseEntity<MallResponse<Page<OrderInfoDetailDto>>> getInfo(@RequestBody MallOrderInfoParam param) {
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        Page<OrderInfoDetailDto> page1 = orderInfoService.queryOrderInfoList(param, page);
        return ResponseEntity.ok(successResult(page1));
    }

    @ApiOperation(value = "获取订单详情")
    @PostMapping("/get/order/detail")
    public ResponseEntity<MallResponse<OrderInfoDetailDto>> getOrderDetail(@RequestBody MallOrderInfoParam param) {
        OrderInfoDetailDto result = orderInfoService.queryOrderDetailInfo(param);
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "根据订单id查订单")
    @PostMapping("/get/order/by/order/id")
    public ResponseEntity<MallResponse<List<OrderItemDetailDto>>> getById(@RequestBody GetOrderInfo info) {
        List<OrderItemDetailDto> orderItemDetailByOrderId = orderInfoService.orderItemByOrderId(info.getOrderId(), 0);
        return ResponseEntity.ok(successResult(orderItemDetailByOrderId));
    }

    /**
     * 交易信息列表  pagesize分为两部分 一部分查user充值提现 一部分查订单 按createdate排序
     *
     * @return
     */
    @ApiOperation(value = "交易信息列表", notes = "交易信息列表")
    @RequestMapping(value = "/change/infos", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Page<OrderAndPayInfoDto>>> selectUserChangeInfosByPage(@RequestBody QueryUserJournalInfosParams params) {
        MallRechargeInfo info = new MallRechargeInfo();
        info.setMallUserId(UserUtils.getCurrentUser().getId());
        info.setPageCurrent(params.getPageCurrent());
        info.setPageSize(params.getPageSize());
        info.setCurrency(params.getCurrency());
        Page<OrderAndPayInfoDto> userOrderAndPayInfo = orderInfoService.getUserOrderAndPayInfo(info);
        return ResponseEntity.ok(successResult(userOrderAndPayInfo));
    }

    @ApiOperation(value = "取消订单")
    @PostMapping("/cancel/order")
    public ResponseEntity<MallResponse<Boolean>> cancelOrderInfo(@RequestBody GetOrderInfo info) {
        boolean result = orderInfoService.cancelOrderInfo(info);
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "确认收货")
    @PostMapping("/received/item/order")
    public ResponseEntity<MallResponse<Boolean>> receivingItem(@RequestBody GetOrderInfo info) {
        boolean result = orderInfoService.receivingItem(info.getOrderId());
        return ResponseEntity.ok(successResult(result));
    }


    @ApiOperation(value = "导出商品列表", notes = "导出商品列表")
    @PostMapping("/do/item/excel")
    @PreAuthorize("hasRole('ROLE_/commodity/order/export')")
    public ResponseEntity<MallResponse> doExcel(@RequestBody GetOrderPageListParam param, HttpServletResponse response) throws Exception {
        String s = orderInfoService.doReport(param, response);
        return ResponseEntity.ok(successResult(s));
    }


    @ApiOperation(value = "条件获取订单列表2.0")
    @PostMapping("/v2/get/order/infos")
    public ResponseEntity<MallResponse<Page<OrderInfoDetailDto>>> queryNewOrderInfoList(@RequestBody MallNewOrderParam param) {
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        Page<OrderInfoDetailDto> pageResult = orderInfoService.queryNewOrderInfoList(param, page);
        return ResponseEntity.ok(successResult(pageResult));
    }


    @ApiOperation(value = "获取用户当月业绩")
    @PostMapping("/v2/get/personal/sales")
    public ResponseEntity<MallResponse<PersonalDTO>> queryUserMonthSales(@RequestBody GetUserParam param) {
        String userId = StringUtils.isEmpty(param.getUserId()) ? UserUtils.getCurrentUser().getId() : param.getUserId();
        PersonalDTO personalDTO = orderInfoService.queryUserMonthSales(userId);
        if ("36754442602676224".equals(userId) || "195377541286989824".equals(userId)) {
            return ResponseEntity.ok(successResult(new PersonalDTO()));
        }
        return ResponseEntity.ok(successResult(personalDTO));
    }

    @ApiOperation(value = "团队管理团队信息")
    @PostMapping("/v2/get/team/sales")
    public ResponseEntity<MallResponse<TeamDTO>> queryTeamMonthSales(@RequestBody GetUserParam param) {
        String userId = StringUtils.isEmpty(param.getUserId()) ? UserUtils.getCurrentUser().getId() : param.getUserId();

        TeamDTO teamDTO = orderInfoService.queryTeamMonthSales(userId);
        if ("36754442602676224".equals(userId) || "195377541286989824".equals(userId)) {
            return ResponseEntity.ok(successResult(new TeamDTO()));
        }
        return ResponseEntity.ok(successResult(teamDTO));
    }

    @ApiOperation(value = "验证京东是否可配送")
    @GetMapping("/v1/check/push")
    public ResponseEntity<MallResponse<Boolean>> queryTeamMonthSales(@RequestParam("address") String param) {
        return ResponseEntity.ok(successResult(jdCheckPushAddressService.checkPushAddress("001", param)));
    }

    @ApiOperation(value = "验证京东是否可配送2")
    @GetMapping("/v1/check/push2")
    public ResponseEntity<MallResponse<Boolean>> queryTeamMonthSales2(@RequestParam("orderId") String orderId, @RequestParam("address") String param) {
        return ResponseEntity.ok(successResult(jdCheckPushAddressService.checkPushAddress(orderId, param)));
    }


    @ApiOperation(value = "根据物流单号查询物流详细信息")
    @PostMapping("/get/express/by/code")
    public ResponseEntity<MallResponse<AliExpressResult>> getAliExpress(@RequestBody AliExpressParam param) {
        AliExpressResult express = AliExpress.getAliExpress(param.getExpressCode());
        return ResponseEntity.ok(successResult(express));
    }

    @ApiOperation(value = "根据多个物流单号查询物流详细信息")
    @PostMapping("/get/express/by/more/code")
    public ResponseEntity<MallResponse<List<AliExpressResult>>> getAliExpressByMore(@RequestBody AliExpressParam param) {
        List<AliExpressResult> result = new ArrayList<>();
        String code = param.getExpressCode();
        String[] split = code.split(",");
        for (String str : split) {
            AliExpressResult express = AliExpress.getAliExpress(str);
            result.add(express);
        }
        return ResponseEntity.ok(successResult(result));
    }


    @ApiOperation(value = "强制变更京东可达（推单的时候使用）")
    @PostMapping("/force/jd/to/arrive")
    public ResponseEntity<MallResponse> forceJdArrive(@RequestBody GetOrderInfo info) {
        orderInfoService.forceJdArrive(info.getOrderId());
        return ResponseEntity.ok(successResult("success"));
    }


    /**
     * @Description 按下单时间统计订单中产品进出货的数据
     * @Author ChenXiang
     * @Date 2019-04-25 16:20:28
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "按下单时间统计订单中产品进出货的数据", notes = "按下单时间统计订单中产品进出货的数据")
    @PostMapping("/query/item/list/report")
    public ResponseEntity<MallResponse<List<OrderItemReportDTO>>> queryOrderItemListReport(@RequestBody BaseParam param) {
        return ResponseEntity.ok(successResult(orderInfoService.queryOrderItemReport(param)));
    }

    /**
     * @Description 按下单时间统计订单中产品进出货的数据
     * @Author ChenXiang
     * @Date 2019-04-25 16:20:28
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "按下单时间统计订单中产品进出货的数据", notes = "统计总数量")
    @PostMapping("/query/item/detail/report")
    public ResponseEntity<MallResponse<HashMap<String, Integer>>> queryOrderItemDetailReport(@RequestBody BaseParam param) {
        return ResponseEntity.ok(successResult(orderInfoService.queryOrderItemDetailReport(param)));
    }

    /**
     * @Description 按下单时间统计订单中产品进出货的数据 导出列表
     * @Author ChenXiang
     * @Date 2019-04-26 11:05:52
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "导出产品进出货统计报表", notes = "导出数据")
    @PostMapping("/output/item/list/report")
    public ResponseEntity<MallResponse> outputOrderItemDetailReport(@RequestBody BaseParam param, HttpServletResponse response) throws Exception {
        String s = orderInfoService.doOutputOrderItemReport(param, response);
        return ResponseEntity.ok(successResult(s));
    }

    /**
     * @Description 产品进出货统计报表，统计总的进货/出货数量,及转化率
     * @Author ChenXiang
     * @Date 2019-05-06 15:27:15
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "产品进出货统计报表", notes = "统计总的进货/出货数量,及转化率")
    @PostMapping("/input/output/item/statistics/report")
    public ResponseEntity<MallResponse<Map<String, Integer>>> inputOutputItemStatisticsReport(@RequestBody BaseParam param) {
        Map<String, Integer> retMap = orderInfoService.inputOutputItemStatisticsReport(param);
        return ResponseEntity.ok(successResult(retMap));
    }

    /**
     * @Description 产品进出货统计报表, 产品进出货统计报表, 详细内容
     * @Author ChenXiang
     * @Date 2019-05-06 15:27:15
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "产品进出货统计报表", notes = "产品进出货统计报表,详细内容")
    @PostMapping("/input/output/item/statistics/report/detail")
    public ResponseEntity<MallResponse<List<InputOutputItemReportDTO>>> inputOutputItemStatisticsReportDetail(@RequestBody BaseParam param) {
        List<InputOutputItemReportDTO> ret = orderInfoService.inputOutputItemStatisticsReportDetail(param);
        return ResponseEntity.ok(successResult(ret));
    }

    /**
     * @Description 产品进出货统计报表，excel下载 产品进出货统计报表的详细内容
     * @Author ChenXiang
     * @Date 2019-05-06 15:27:15
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "产品进出货统计报表", notes = "excel下载 产品进出货统计报表的详细内容")
    @PostMapping("/input/output/item/statistics/report/detail/download")
    public ResponseEntity<MallResponse> inputOutputItemStatisticsReportDetailDownload(@RequestBody BaseParam param, HttpServletResponse response) throws Exception {
        String ret = orderInfoService.inputOutputItemStatisticsReportDetailDownload(param, response);
        return ResponseEntity.ok(successResult(ret));
    }

    /**
     * @Description
     * @Author ChenXiang
     * @Date 2019-05-29 10:42:15
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "下载京东不可达订单信息", notes = "下载京东不可达订单信息")
    @PostMapping("/down/jd/excel")
    public ResponseEntity<MallResponse> downLoadJDExcel(HttpServletResponse response) throws Exception {
        String s = orderInfoService.downLoadJDExcel(response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "导入京东不可达订单信息", notes = "导入京东不可达订单信息")
    @PostMapping("/import/jd/excel")
    public ResponseEntity<MallResponse<List<ItemOrderResultDTO>>> importJDExcel(@RequestBody ImportParam param) throws Exception {
//        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] b = decoder.decodeBuffer(param.getFile());
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] b = decoder.decode(param.getFile());
        log.info(param.getFile());
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        List<ItemOrderResultDTO> ret = orderInfoService.importJDExcel(stream);
        return ResponseEntity.ok(successResult(ret));
    }

    @ApiOperation(value = "仓库发货", notes = "仓库发货")
    @GetMapping("/send/by/store")
    public ResponseEntity sendByStore(@RequestParam String id, @RequestParam String company, @RequestParam String code) {
        return ResponseEntity.ok(successResult(orderInfoService.sendByStore(id, company, code)));
    }

    @ApiOperation(value = "后台查询入云单", notes = "后台查询入云单")
    @GetMapping("/background/query/order/by/type")
    public ResponseEntity<MallResponse<List<MallOrderItem>>> queryOrderBackground(@RequestParam("mallUserId") String mallUserId) {
        List<MallOrderItem> ret = orderInfoService.queryOrderBackground(mallUserId);
        return ResponseEntity.ok(successResult(ret));
    }

    @ApiOperation(value = "白米浮转200ml溶液", notes = "白米浮转200ml溶液")
    @PostMapping("/transfer/c036/to/new")
    public ResponseEntity<MallResponse<Boolean>> transferC036(@RequestBody GetOrderInfo getOrderInfo) {
        orderInfoService.transferC036(getOrderInfo);
        return ResponseEntity.ok(successResult(true));
    }


    @ApiOperation(value = "设置白米浮转200ml溶液参数配置", notes = "白米浮转200ml溶液参数配置")
    @PostMapping("/set/item/spec/transfer")
    public ResponseEntity<MallResponse<Boolean>> setNewItemTransferSpec(@RequestBody NewItemTransferSpec newItemTransferSpec) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startTime = format.format(newItemTransferSpec.getStartTime());
        String endTime = format.format(newItemTransferSpec.getEndTime());
        try {
            newItemTransferSpec.setStartTime(format.parse(startTime));
            newItemTransferSpec.setEndTime(format.parse(endTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        RedisUtil.set("newItemTransferSpec", JsonUtils.objectToJson(newItemTransferSpec));
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "查询白米浮转200ml溶液参数配置", notes = "白米浮转200ml溶液参数配置")
    @GetMapping("/get/item/spec/transfer")
    public ResponseEntity<MallResponse<NewItemTransferSpec>> getNewItemTransferSpec() {
        String transferSpec = RedisUtil.get("newItemTransferSpec");
        if (transferSpec == null) {
            return ResponseEntity.ok(successResult(null));
        }
        NewItemTransferSpec spec = JsonUtils.jsonToPojo(transferSpec, NewItemTransferSpec.class);
        return ResponseEntity.ok(successResult(spec));
    }

    /**
     * 修改订单检验运费是否需要修改
     *
     * @return
     */
    @ApiOperation(value = "修改订单时，检验运费是否需要修改", notes = "修改订单时，检验运费是否需要修改")
    @RequestMapping(value = "/app/check/order/freight", method = RequestMethod.POST)
    public ResponseEntity<MallResponse> checkOrderPostFee(@RequestBody CheckPostFeeParam checkPostFeeParam) {

        if ("1".equals(RedisUtil.get("check_freight_button"))) {
            throw new MallException("1234567", "暂不支持修改收件信息");
        }

        //收货地区拦截校验
        String message = userFeign.checkoutExcludeAddress(checkPostFeeParam.getAddrId());
        if (!StringUtils.isEmpty(message)) {
            MallResponse r = new MallResponse();
            return ResponseEntity.ok(failedResult(r, "500", message, BigDecimal.ZERO));
        }
        OrderCheckPostFeeDto ret = orderInfoService.checkOrderPostFee(checkPostFeeParam);
        return ResponseEntity.ok(successResult(ret));
    }

    @ApiOperation(value = "修改订单的收件人，收货地址信息", notes = "修改订单的收件人，收货地址信息")
    @PostMapping("/app/update/address")
    public ResponseEntity<MallResponse<Boolean>> updateOrderAddress(@RequestBody CheckPostFeeParam checkPostFeeParam) {
        Boolean ret = orderInfoService.updateOrderAddress(checkPostFeeParam);
        return ResponseEntity.ok(successResult(ret));
    }

//    拆单查询
//    拆单查询
//    批量拆单

    /**
     * @Description 拆单查询
     * @Author ChenXiang
     * @Date 2020-02-12 11:28:46
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "订单拆单查询")
    @PostMapping("/split/order/query")
    public ResponseEntity<MallResponse<Page<OrderInfoDetailDto>>> SplitOrderQuery(@RequestBody SplitOrderQueryParam param) {
        if (!org.springframework.util.StringUtils.isEmpty(param.getPhone())) {
            if (param.getPhone().length() < 4) {
                throw new MallException("020032");
            }
        }
        if (!org.springframework.util.StringUtils.isEmpty(param.getAddrPhone())) {
            if (param.getAddrPhone().length() < 4) {
                throw new MallException("020032");
            }
        }
        PageDto<AllOrderInfoDto> page = orderInfoService.splitOrderQuery(param);
        return ResponseEntity.ok(successResult(page));

    }

    /**
     * @Description 订单拆单
     * @Author ChenXiang
     * @Date 2020-02-12 14:31:39
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "订单拆单")
    @PostMapping("/split/order")
    public ResponseEntity<MallResponse<Boolean>> doSplitOrder(@RequestBody OrderSplitParam param) {
        Boolean ret = orderInfoService.doSplitOrder(param);
        return ResponseEntity.ok(successResult(true));
    }


    @ApiOperation(value = "获取物流公司列表")
    @GetMapping("/get/express/company/list")
    public ResponseEntity<MallResponse<List<ExpressCompany>>> getExpressCompanyList() {
        List<ExpressCompany> companyList = expressCompanyService.getExpressCompanyList();
        return ResponseEntity.ok(successResult(companyList));
    }

    @ApiOperation(value = "发送当日提货大于10盒的邮件")
    @GetMapping("/send/email/about/stock")
    public ResponseEntity<MallResponse<Boolean>> sendEMail(@RequestParam(value = "flag", required = false) String flag) throws Exception {
        Boolean result = orderInfoService.sendEMail(flag);
        return ResponseEntity.ok(successResult(result));
    }

}
