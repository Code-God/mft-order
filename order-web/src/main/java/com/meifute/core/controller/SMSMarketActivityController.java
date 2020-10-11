package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.dto.OrderInfoDto;
import com.meifute.core.dto.OrderVerifyDto;
import com.meifute.core.dto.SMSMarketVerifyDto;
import com.meifute.core.dto.UserAddressDto;
import com.meifute.core.entity.SMSAcEvaluate;
import com.meifute.core.entity.SMSRecordHits;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.SMSMarketActivityService;
import com.meifute.core.vo.GetOrderVeifyParam;
import com.meifute.core.vo.MultiOrderInfo;
import com.meifute.core.vo.QuerySMSVerify;
import com.meifute.core.vo.SMSAcVerify;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Classname SMSMarketActivityController
 * @Description 短信营销活动
 * @Date 2019-11-19 14:26
 * @Created by MR. Xb.Wu
 */
@RestController
@RequestMapping("v2/sms/market")
@Api(tags = "market", description = "短信营销活动")
@Slf4j
public class SMSMarketActivityController extends BaseController {

    @Autowired
    private SMSMarketActivityService smsMarketActivityService;

    @ApiOperation(value = "记录点击量", notes = "记录点击量")
    @PostMapping("/record/hits")
    public ResponseEntity<MallResponse<Boolean>> recordHits(@RequestBody SMSRecordHits smsRecordHits) {
        smsMarketActivityService.recordHits(smsRecordHits);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "初始化地址信息", notes = "初始化地址信息")
    @GetMapping("/init/address")
    public ResponseEntity<MallResponse<UserAddressDto>> initAddress(@RequestParam("phone") String phone) {
        UserAddressDto address = smsMarketActivityService.initAddress(phone);
        return ResponseEntity.ok(successResult(address));
    }

    @ApiOperation(value = "提交审核订单", notes = "提交审核订单")
    @PostMapping("/submit/verify/order")
    public ResponseEntity<MallResponse<SMSMarketVerifyDto>> submitVerifyOrder(@RequestBody MultiOrderInfo multiOrderInfo) {
        if (StringUtils.isEmpty(multiOrderInfo.getCity())) {
            multiOrderInfo.setCity(multiOrderInfo.getProvince());
        }
        if (StringUtils.isEmpty(multiOrderInfo.getArea())) {
            multiOrderInfo.setArea(multiOrderInfo.getProvince());
        }
        SMSMarketVerifyDto dto = smsMarketActivityService.submitVerifyOrder(multiOrderInfo);
        return ResponseEntity.ok(successResult(dto));
    }

    @ApiOperation(value = "h5页面短信营销审核单列表", notes = "h5页面短信营销审核单列表")
    @PostMapping(value = "/verify/list/info/h5")
    public ResponseEntity<MallResponse<Page<SMSMarketVerifyDto>>> getSMSVerifyInH5(@RequestBody QuerySMSVerify querySMSVerify) {
        Page<SMSMarketVerifyDto> page = smsMarketActivityService.getSMSVerifyInH5(querySMSVerify);
        return ResponseEntity.ok(successResult(page));
    }



    @ApiOperation(value = "查询短信营销审核单列表(父级查询-app)", notes = "查询短信营销审核单列表(父级查询-app)")
    @PostMapping(value = "/verify/list/info")
    public ResponseEntity<MallResponse<Page<OrderVerifyDto>>> getSMSVerifyByAccepterId(@RequestBody GetOrderVeifyParam getOrderVerifyParam) {
        Page<OrderVerifyDto> page = smsMarketActivityService.getSMSVerifyByAccepterId(getOrderVerifyParam);
        return ResponseEntity.ok(successResult(page));
    }

    @ApiOperation(value = "审核", notes = "审核")
    @PostMapping("/verify")
    public ResponseEntity<MallResponse<OrderInfoDto>> verify(@RequestBody SMSAcVerify smsAcVerify) {
        OrderInfoDto dto = smsMarketActivityService.verify(smsAcVerify);
        return ResponseEntity.ok(successResult(dto));
    }

    @ApiOperation(value = "重新下单", notes = "重新下单")
    @GetMapping("/resubmit/verify/order")
    public ResponseEntity<MallResponse<Boolean>> submitVerifyOrder(@RequestParam("verifyId") String verifyId) {
        smsMarketActivityService.reSubmitVerifyOrder(verifyId);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "取消提审单", notes = "取消提审单")
    @GetMapping("/cancel/verify/order")
    public ResponseEntity<MallResponse<Boolean>> cancelVerifyOrder(@RequestParam("verifyId") String verifyId) {
        smsMarketActivityService.cancelVerifyOrder(verifyId);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "新增评价信息", notes = "新增评价信息")
    @PostMapping("/add/sms/evaluate")
    public ResponseEntity<MallResponse<Boolean>> addSmsEvaluate(@RequestBody SMSAcEvaluate smsAcEvaluate) {
        smsMarketActivityService.addSmsEvaluate(smsAcEvaluate);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "查询是否已评价，true已评价false未评价", notes = "查询是否已评价，true已评价false未评价")
    @GetMapping("/is/evaluated")
    public ResponseEntity<MallResponse<Boolean>> getEvaluated(@RequestParam("orderId") String orderId) {
        boolean evaluated = smsMarketActivityService.getEvaluated(orderId);
        return ResponseEntity.ok(successResult(evaluated));
    }

    @ApiOperation(value = "发送短信营销短信", notes = "发送短信营销短信")
    @GetMapping("/send/sms/market/msg")
    public ResponseEntity<MallResponse<Boolean>> sendNoticeMarketMsg(@RequestParam("pid") String pid, @RequestParam("phone") String phone) {
        smsMarketActivityService.sendNoticeMarketMsg(phone, pid);
        return ResponseEntity.ok(successResult(true));
    }

}
