package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallUser;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.HistoryOrderService;
import com.meifute.core.service.RetailPurchasesService;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.BaseRequest;
import com.meifute.core.vo.HistoryOrderParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-06-19 16:53
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v2/app/history")
@Api(tags = "history", description = "历史订单")
@Slf4j
public class HistoryOrderController extends BaseController {

    @Autowired
    private HistoryOrderService historyOrderService;
    @Autowired
    private RetailPurchasesService retailPurchasesService;

    @ApiOperation(value = "导入历史订单", notes = "导入历史订单")
    @PostMapping(value = "/upload/order")
    public ResponseEntity<MallResponse<String>> readExcelOrderInfo(@RequestBody MultipartFile file) {
        historyOrderService.readOrderExcel(file);
        return ResponseEntity.ok(successResult("SUCCESS"));
    }

    @ApiOperation(value = "检验是否有未支付的历史订单", notes = "检验是否有未支付的历史订单")
    @PostMapping(value = "/check/order")
    public ResponseEntity<MallResponse<Boolean>> checkHaveHistoryOrder(@RequestBody BaseRequest param) {
        MallUser currentUser = UserUtils.getCurrentUser();
        boolean result = historyOrderService.checkHistoryOrder(currentUser.getId(),"0");
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "获取未支付的历史运费订单详情", notes = "获取未支付的历史运费订单详情")
    @PostMapping(value = "/get/order")
    public ResponseEntity<MallResponse<List<MallOrderInfo>>> getHistoryOrderInfo(@RequestBody BaseRequest param) {
        MallUser currentUser = UserUtils.getCurrentUser();
        List<MallOrderInfo> result = historyOrderService.getHistoryOrderInfo(currentUser);
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "更新历史运费订单", notes = "更新历史运费订单")
    @PostMapping(value = "/update/order")
    public ResponseEntity<MallResponse<Boolean>> adminToUpdateHistoryOrderInfo(@RequestBody HistoryOrderParam historyOrderParam) {
        historyOrderService.adminToUpdateHistoryOrderInfo(historyOrderParam);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "后台获取历史运费订单列表", notes = "后台获取历史运费订单列表")
    @PostMapping(value = "/detail/order")
    public ResponseEntity<MallResponse<Page<HistoryOrderParam>>> getHistoryOrderDetail(@RequestBody HistoryOrderParam historyOrderParam) {
        Page<HistoryOrderParam> orderDetail = historyOrderService.getHistoryOrderDetail(historyOrderParam);
        return ResponseEntity.ok(successResult(orderDetail));
    }


    @ApiOperation(value = "推送8月份进货量（产品尽提瞎需求）", notes = "推送8月份进货量（产品尽提瞎需求）")
    @PostMapping(value = "/send/retail/purchases")
    public ResponseEntity<MallResponse<String>> readExcelRetail(@RequestBody MultipartFile file) {
        retailPurchasesService.readExcel(file);
        return ResponseEntity.ok(successResult("SUCCESS"));
    }
}
