package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.*;
import com.meifute.core.entity.MallOrderFeedBack;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.service.MallOrderInfoService;
import com.meifute.core.service.OrderFeedBackService;
import com.meifute.core.vo.GetOrderInfo;
import com.meifute.core.vo.GetOrderPageListParam;
import com.meifute.core.vo.ImportParam;
import com.meifute.core.vo.UpdateOrderInfoParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

//import sun.misc.BASE64Decoder;

/**
 * @Auther: wll
 * @Date: 2019/1/17 15:35
 * @Auto: I AM A CODE MAN !
 * @Description:
 */
@RestController
@RequestMapping("v1/app/admin/order")
@Api(tags = "Admin", description = "后台订单中心")
@Slf4j
public class OrderAdminController extends BaseController {

    @Autowired
    private MallOrderInfoService mallOrderInfoService;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private PayFeign payFeign;

    @Autowired
    private OrderFeedBackService orderFeedBackService;
    /**
     * 所有订单列表
     * @return
     */
    @ApiOperation(value = "所有订单列表", notes = "所有订单列表")
    @PostMapping(value = "/all/order/info")
    public ResponseEntity<MallResponse<PageDto<AllOrderInfoDto>>> queryMallOrderVerifyByAccepterId(@RequestBody GetOrderPageListParam param) {
        if (!StringUtils.isEmpty(param.getPhone())){
            if (param.getPhone().length() < 4){
                param.setPhone(null);
            }
        }
        if (!StringUtils.isEmpty(param.getAddrPhone())){
            if (param.getAddrPhone().length() < 4){
                param.setAddrPhone(null);
            }
        }
        PageDto<AllOrderInfoDto> page = mallOrderInfoService.queryAllOrderInfoList(param);
        return ResponseEntity.ok(successResult(page));
    }

    /**
     * 根据orderiD获取审核单
     * @return
     */
    @ApiOperation(value = "根据orderiD获取审核单", notes = "根据orderiD获取审核单")
    @PostMapping(value = "/get/order/verify/info")
    public ResponseEntity<MallResponse<VerifyOrderInfoDto>> queryMallOrderVerifyByAccepterId(@RequestBody GetOrderInfo param) {
        VerifyOrderInfoDto page = mallOrderInfoService.queryVerifyOrderByOrderId(param.getOrderId());
        return ResponseEntity.ok(successResult(page));
    }


    @ApiOperation(value = "总代入云单", notes = "总代入云单")
    @RequestMapping(value = "/general/infos", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Page<OrderInfoPageDto>>> getGeneralOrderInfoPageList(@RequestBody GetOrderPageListParam pageListParam) {
        if (!StringUtils.isEmpty(pageListParam.getPhone())){
            if (pageListParam.getPhone().length() < 4){
                pageListParam.setPhone(null);
            }
        }
        if (!StringUtils.isEmpty(pageListParam.getAddrPhone())){
            if (pageListParam.getAddrPhone().length() < 4){
                pageListParam.setAddrPhone(null);
            }
        }
        List<String> idList=new ArrayList<>(1);
        // `role_id` char(1) DEFAULT '0' COMMENT '等级 0普通用户，1一级代理，2二级代理，3三级代理，4总代理',
        idList.add("4");
        pageListParam.setRoleIdList(idList);
        PageDto<OrderInfoPageDto> list = mallOrderInfoService.getGeneralOrderInfoPageList(pageListParam);
        if(ObjectUtils.isNullOrEmpty(list)){
            list = new PageDto();
            list.setRecords(new ArrayList());
            list.setTotal(0);
        }
        return ResponseEntity.ok(successResult(list));
    }


    @ApiOperation(value = "非总代入云单", notes = "非总代入云单")
    @RequestMapping(value = "/infos", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Page<OrderInfoPageDto>>> getOrderInfoPageList(@RequestBody GetOrderPageListParam pageListParam) {
        // `role_id` char(1) DEFAULT '0' COMMENT '等级 0普通用户，1一级代理，2二级代理，3三级代理，4总代理',
        if (!StringUtils.isEmpty(pageListParam.getPhone())){
            if (pageListParam.getPhone().length() < 4){
                pageListParam.setPhone(null);
            }
        }
        if (!StringUtils.isEmpty(pageListParam.getAddrPhone())){
            if (pageListParam.getAddrPhone().length() < 4){
                pageListParam.setAddrPhone(null);
            }
        }
        List<String> idList=Arrays.asList("0","1","2","3");
        pageListParam.setRoleIdList(idList);
        PageDto<OrderInfoPageDto> list = mallOrderInfoService.getGeneralOrderInfoPageList(pageListParam);
        if(ObjectUtils.isNullOrEmpty(list)){
            list = new PageDto();
            list.setRecords(new ArrayList());
            list.setTotal(0);
        }
        return ResponseEntity.ok(successResult(list));
    }

    @ApiOperation(value = "关闭订单", notes = "关闭订单")
    @RequestMapping(value = "/close/order", method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_/commodity/order/closeOrder')")
    public ResponseEntity<MallResponse> getOrderInfoPageList(@RequestBody MallOrderInfo mallOrderInfo) {
        if (StringUtils.isEmpty(mallOrderInfo.getOrderId())){
            throw  new MallException(OrderRespCode.PARAM_NOT_FOUND);
        }
        mallOrderInfoService.closeOrderFromAdmin(mallOrderInfo);
        return ResponseEntity.ok(successResult(null));
    }


    @ApiOperation(value = "关闭积分订单", notes = "关闭积分订单")
    @RequestMapping(value = "/close/credit/order", method = RequestMethod.POST)
    public ResponseEntity<MallResponse> closeCreditOrder(@RequestBody GetOrderInfo orderInfo) {
        if (StringUtils.isEmpty(orderInfo.getOrderId())){
            throw  new MallException(OrderRespCode.PARAM_NOT_FOUND);
        }
        mallOrderInfoService.closeCreditOrder(orderInfo.getOrderId());
        return ResponseEntity.ok(successResult(null));
    }

    @ApiOperation(value = "导出订单列表", notes = "导出订单列表")
    @PostMapping("/do/excel")
    @PreAuthorize("hasRole('ROLE_/productBill/order/export')")
    public ResponseEntity<MallResponse> doExcel(@RequestBody GetOrderPageListParam param , HttpServletResponse response) throws Exception {
        String s = mallOrderInfoService.doExcel(param, response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "下载商品订单物流信息导入模板", notes = "下载商品订单物流信息导入模板")
    @PostMapping("/down/excel")
    @PreAuthorize("hasRole('ROLE_/commodity/order/downloadTemplate')")
    public ResponseEntity<MallResponse> downLoad(HttpServletResponse response) throws Exception {
        String s = mallOrderInfoService.downLoadExcel( response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "物流信息导入（第一步）", notes = "物流信息导入（第一步）")
    @PostMapping("/import/excel")
    @PreAuthorize("hasRole('ROLE_/commodity/order/import')")
    public ResponseEntity<MallResponse> importExcel(@RequestBody ImportParam param) throws Exception {
//        InputStream in = file.getInputStream();
//        BASE64Decoder decoder = new BASE64Decoder();
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] b = decoder.decode(param.getFile());
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        String key = mallOrderInfoService.importExcel(stream);
        return ResponseEntity.ok(successResult(key));
    }


    @ApiOperation(value = "获取导入结果（第二步）",notes = "获取导入结果（第二步）")
    @GetMapping("/get/import/result")
    public ResponseEntity<MallResponse> getImportResult(@RequestParam String key,HttpServletResponse response) throws Exception {
        String anImport = mallOrderInfoService.getImport(key, response);
        return ResponseEntity.ok(successResult(anImport));
    }


    @ApiOperation(value = "后台编辑备注及快递号", notes = "后台编辑备注及快递号")
    @RequestMapping(value = "/updateinfo", method = RequestMethod.POST)
    public ResponseEntity<MallResponse> queryOrderVerifyPageList(@RequestBody UpdateOrderInfoParam param) {
        MallOrderInfo mallOrderInfo =new MallOrderInfo();

        boolean change=false;
        if (!StringUtils.isEmpty(param.getExpressCode())){
            mallOrderInfo = mallOrderInfoMapper.selectOrderById(param.getOrderId());
            if (!MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType()) && !MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())){
                throw  new MallException(OrderRespCode.ADD_ORDER_FAIL);
            }
            if (!MallOrderStatusEnum.ORDER_STATUS_003.getCode().equals(mallOrderInfo.getOrderStatus())){
                throw  new MallException(OrderRespCode.ADD_ORDER_FAIL);
            }
           change=true;
        }
        if (!ObjectUtils.isNullOrEmpty(param.getPostFeeAmt())){
            mallOrderInfo = mallOrderInfoMapper.selectOrderById(param.getOrderId());
            mallOrderInfoService.setPostFee(mallOrderInfo,param.getPostFeeAmt());
        }
        BeanMapper.copy(param,mallOrderInfo);
        if (change){
            mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
        }
        mallOrderInfoMapper.updateById(mallOrderInfo);
        return ResponseEntity.ok(successResult(null));
    }

    /**
     * 所有仓库订单列表
     * @return
     */
    @ApiOperation(value = "所有仓库订单列表", notes = "所有仓库订单列表")
    @PostMapping(value = "/warehouse/order/info")
    public ResponseEntity<MallResponse<PageDto<AllOrderInfoDto>>> queryWarehouseOrderInfo(@RequestBody GetOrderPageListParam param) {
        if (!StringUtils.isEmpty(param.getPhone())){
            if (param.getPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        if (!StringUtils.isEmpty(param.getAddrPhone())){
            if (param.getAddrPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        PageDto<AllOrderInfoDto> page = mallOrderInfoService.queryWarehouseOrderInfo(param);
        return ResponseEntity.ok(successResult(page));
    }


    @ApiOperation(value = "仓库订单导出", notes = "仓库订单导出")
    @PostMapping(value = "/warehouse/order/info/download")
    public ResponseEntity<MallResponse> downloadWarehouseOrderInfo(@RequestBody GetOrderPageListParam param,HttpServletResponse response)throws Exception {
        if (!StringUtils.isEmpty(param.getPhone())){
            if (param.getPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        if (!StringUtils.isEmpty(param.getAddrPhone())){
            if (param.getAddrPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        String ret = mallOrderInfoService.downloadWarehouseOrderInfo(param, response);
        return ResponseEntity.ok(successResult(ret));
    }

    @ApiOperation(value = "仓库订单京东模版导出", notes = "仓库订单京东模版导出")
    @PostMapping(value = "/warehouse/order/info/download/to/jd")
    public ResponseEntity<MallResponse<String>> downloadWarehouseOrderInfoToJD(@RequestBody GetOrderPageListParam param,HttpServletResponse response)throws Exception {
        if (!StringUtils.isEmpty(param.getPhone())){
            if (param.getPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        if (!StringUtils.isEmpty(param.getAddrPhone())){
            if (param.getAddrPhone().length() < 4){
                throw new MallException("020032");
            }
        }
        mallOrderInfoService.downloadWarehouseOrderInfoToJD(param, response);
        return ResponseEntity.ok(successResult("SUCCESS"));
    }

    @ApiOperation(value = "订单问题反馈导出",notes = "订单问题反馈导出")
    @PostMapping("do/export/feedback")
    public ResponseEntity<MallResponse> exportFeedBack(@RequestBody MallOrderFeedBack mallOrderFeedBack,HttpServletResponse response) throws Exception {
        String s = orderFeedBackService.doExportFeedBackInfo(mallOrderFeedBack, response);
        return ResponseEntity.ok(successResult(s));
    }


    @ApiOperation(value = "出货订单查询（用于批量推单）", notes = "出货订单查询（用于批量推单）")
    @PostMapping(value = "/query/order/to/push")
    public ResponseEntity<MallResponse<PageDto<QueryPushOrderInfoDto>>> queryOrderToPush(@RequestBody GetOrderPageListParam param) {
        PageDto<QueryPushOrderInfoDto> page = mallOrderInfoService.queryOrderToPush(param);
        return ResponseEntity.ok(successResult(page));
    }

    @ApiOperation(value = "后台根据订单查询物流信息", notes = "后台根据订单查询物流信息")
    @GetMapping(value = "/query/itemPackage/by/orderId")
    public ResponseEntity<MallResponse<List<MallLogisticsInfoDTO>>> queryItemPackageByOrderId(@RequestParam("orderId") String orderId) {
        List<MallLogisticsInfoDTO> result = mallOrderInfoService.queryItemPackageByOrderId(orderId);
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "导入京东明细更新运单号", notes = "导入京东明细更新运单号")
    @PostMapping("/imort/jd/billway/code")
    public ResponseEntity<MallResponse<String>> readSuperExcel(@RequestBody MultipartFile file) {
        String s = mallOrderInfoService.readJDBillWay(file);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "开启零售价，0开启，1关闭，2查询", notes = "开启零售价，0开启，1关闭，2查询")
    @GetMapping("/enable/retail/price")
    public ResponseEntity<MallResponse<String>> enableRetailPriceOnOff(@RequestParam("status") String status) {
        if ("2".equals(status)) {
            String onOff = RedisUtil.get("enableRetailPriceOnOff");
            if (onOff == null) {
                RedisUtil.set("enableRetailPriceOnOff","1");
                onOff = "1";
            }
            return ResponseEntity.ok(successResult(onOff));
        }
        if ("0".equals(status)) {
            RedisUtil.set("enableRetailPriceOnOff","0");
        } else {
            RedisUtil.set("enableRetailPriceOnOff", "1");
        }
        return ResponseEntity.ok(successResult(status));
    }
}
