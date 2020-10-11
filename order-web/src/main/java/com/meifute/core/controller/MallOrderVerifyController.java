package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.dto.OrderVerifyDto;
import com.meifute.core.entity.MallOrderVerify;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallOrderInfoService;
import com.meifute.core.service.MallOrderVerifyService;
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
import java.util.Base64;

/**
 * @Auther: wxb
 * @Date: 2018/11/12 16:13
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v1/app/orderverify")
@Api(tags = "orderverify", description = "订单审核中心")
@Slf4j
public class MallOrderVerifyController extends BaseController {

    @Autowired
    private MallOrderVerifyService orderVerifyService;

    @Autowired
    private MallOrderInfoService mallOrderInfoService;

    /**
     * 查询下级购入产品审核单列表
     * @return
     */
    @ApiOperation(value = "查询下级购入产品审核单列表", notes = "查询下级购入产品审核单列表")
    @PostMapping(value = "/verify/list/info")
    public ResponseEntity<MallResponse<Page<OrderVerifyDto>>> queryMallOrderVerifyByAccepterId(@RequestBody GetOrderVeifyParam getOrderVerifyParam) {
        Page<OrderVerifyDto> page = orderVerifyService.queryMallOrderVerifyByAccepterId(getOrderVerifyParam);
        return ResponseEntity.ok(successResult(page));
    }

    /**
     * 根据下级手机号搜索审核单
     * @return
     */
    @ApiOperation(value = "根据下级手机号搜索审核单", notes = "根据下级手机号搜索审核单")
    @PostMapping(value = "/verify/phone/list/info")
    public ResponseEntity<MallResponse<Page<OrderVerifyDto>>> queryMallOrderVerifyByAccepterId(@RequestBody GetOrderVeifyByPhoneParam getOrderVerifyParam) {
        Page<OrderVerifyDto> page = orderVerifyService.queryMallOrderVerifyLikeProposerId(getOrderVerifyParam);
        return ResponseEntity.ok(successResult(page));
    }

    /**
     * 下级购入产品订单审核
     * @return
     */
    @ApiOperation(value = "下级购入产品订单审核", notes = "下级购入产品订单审核")
    @PostMapping(value = "/verify/to/pass")
    public ResponseEntity<MallResponse<Boolean>> orderVerify(@RequestBody OrderVerifyParam orderVerifyParam) {
        boolean result = orderVerifyService.orderVerify(orderVerifyParam);
        return ResponseEntity.ok(successResult(result));
    }

    /**
     * 下级购入产品订单审核
     * @return
     */
    @ApiOperation(value = "重新发起审核", notes = "下级购入产品订单审核")
    @PostMapping(value = "/verify/to/reverify")
    public ResponseEntity<MallResponse<Boolean>> reToVerify(@RequestBody GetOrderInfo verify) {
        boolean result = orderVerifyService.reToVerify(verify.getOrderId(),verify.getProofPath());
        return ResponseEntity.ok(successResult(result));
    }


    @ApiOperation(value = "商务添加（修改）备注", notes = "商务添加（修改）备注")
    @GetMapping("/update/remark/by/id")
    public ResponseEntity<MallResponse> updateRemark(@RequestParam String id,@RequestParam String adminRemark){
        MallOrderVerify verify = orderVerifyService.selectById(id);
        verify.setAdminRemark(adminRemark);
        orderVerifyService.updateById(verify);
        return ResponseEntity.ok(successResult(true));
    }

    /**
     * @Description 敏感订单下载物流模板
     * @Author ChenXiang
     * @Date 2019-08-14 10:36:32
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "敏感订单下载物流模板", notes = "敏感订单下载物流模板")
    @PostMapping("/down/excel")
    public ResponseEntity<MallResponse> downLoad(HttpServletResponse response) throws Exception {
        String s = mallOrderInfoService.downLoadExcel( response);
        return ResponseEntity.ok(successResult(s));
    }

    /**
     * @Description 物流信息导入
     * @Author ChenXiang
     * @Date 2019-08-14 10:36:32
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "物流信息导入（第一步）", notes = "物流信息导入（第一步）")
    @PostMapping("/import/excel")
    public ResponseEntity<MallResponse> importExcel(@RequestBody ImportParam param,HttpServletResponse response) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] b = decoder.decode(param.getFile());
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        String key = orderVerifyService.importExcel(stream,response);
        return ResponseEntity.ok(successResult(key));
    }

    @ApiOperation(value = "获取导入结果（第二步）",notes = "获取导入结果（第二步）")
    @GetMapping("/get/import/result")
    public ResponseEntity<MallResponse> getImportResult(@RequestParam String key,HttpServletResponse response) throws Exception {
        String anImport = orderVerifyService.getImport(key, response);
        return ResponseEntity.ok(successResult(anImport));
    }
}
