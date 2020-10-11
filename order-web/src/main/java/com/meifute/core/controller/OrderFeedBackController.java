package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.entity.MallOrderFeedBack;
import com.meifute.core.entity.orderfeedback.MallChangeFeedback;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.OrderFeedBackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @Auther: wuxb
 * @Date: 2019-05-24 13:19
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v2/app/feedback")
@Api(tags = "feedback", description = "订单反馈")
@Slf4j
public class OrderFeedBackController extends BaseController {

    @Autowired
    private OrderFeedBackService orderFeedBackService;

    @ApiOperation(value = "添加订单反馈信息", notes = "添加订单反馈信息")
    @PostMapping(value = "/add/info")
    public ResponseEntity<MallResponse<Boolean>> insertFeedBack(@RequestBody MallOrderFeedBack mallOrderFeedBack) {
        orderFeedBackService.insertFeedBack(mallOrderFeedBack);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "后台新增订单反馈信息", notes = "后台新增订单反馈信息")
    @PostMapping(value = "/admin/add/info")
    public ResponseEntity<MallResponse<Boolean>> addFeedBack(@RequestBody MallOrderFeedBack mallOrderFeedBack) {
        orderFeedBackService.addFeedBack(mallOrderFeedBack);
        return ResponseEntity.ok(successResult(true));
    }


    @ApiOperation(value = "查询订单反馈信息", notes = "查询订单反馈信息")
    @PostMapping(value = "/get/info")
    public ResponseEntity<MallResponse<Page<MallOrderFeedBack>>> getFeedBackInfo(@RequestBody MallOrderFeedBack mallOrderFeedBack) {
        Page<MallOrderFeedBack> backInfo = orderFeedBackService.getFeedBackInfo(mallOrderFeedBack);
        return ResponseEntity.ok(successResult(backInfo));
    }

    @ApiOperation(value = "更新订单反馈信息", notes = "更新订单反馈信息")
    @PostMapping(value = "/update/info")
    public ResponseEntity<MallResponse<Boolean>> updateFeedBackInfo(@RequestBody MallOrderFeedBack mallOrderFeedBack) {
        orderFeedBackService.updateFeedBackInfo(mallOrderFeedBack);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "查询反馈详情", notes = "查询反馈详情")
    @GetMapping(value = "/query/detail")
    public ResponseEntity<MallResponse<MallOrderFeedBack>> queryDetail(@RequestParam("id") String id) {
        MallOrderFeedBack result = orderFeedBackService.queryDetail(id);
        return ResponseEntity.ok(successResult(result));
    }

    @ApiOperation(value = "分页查询订单问题反馈", notes = "分页查询订单问题反馈")
    @PostMapping(value = "/query/page")
    public ResponseEntity<MallResponse<Page<MallOrderFeedBack>>> queryPage(@RequestBody MallOrderFeedBack mallOrderFeedBack) {
        Page<MallOrderFeedBack> backInfo = orderFeedBackService.queryPage(mallOrderFeedBack);
        return ResponseEntity.ok(successResult(backInfo));
    }

    @ApiOperation(value = "变更状态", notes = "变更状态")
    @PostMapping(value = "/change/back/status")
    public ResponseEntity<MallResponse<Boolean>> changeFeedBackInfo(@RequestBody MallChangeFeedback mallChangeFeedback) {
        Boolean result = orderFeedBackService.changeFeedBackInfo(mallChangeFeedback);
        return ResponseEntity.ok(successResult(result));
    }

}
