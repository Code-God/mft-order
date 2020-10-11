package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.dto.OrderInfoDetailDto;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallNewOrderInfoService;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.vo.ImportParam;
import com.meifute.core.vo.SearchOrderParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * @Auther: wuxb
 * @Date: 2019-02-25 11:32
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v2/app/ordercenter")
@Api(tags = "orderCenter2.0", description = "订单中心2.0")
@Slf4j
public class MallNewOrderInfoController extends BaseController {

    @Autowired
    private MallNewOrderInfoService newOrderInfoService;

    @ApiOperation(value = "搜索订单2.0")
    @PostMapping("/search/order/info")
    public ResponseEntity<MallResponse<Page<OrderInfoDetailDto>>> searchOrderInfo(@RequestBody SearchOrderParam param) {
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        Page<OrderInfoDetailDto> pageResult = newOrderInfoService.searchOrderInfo(param, page);
        return ResponseEntity.ok(successResult(pageResult));
    }

    @ApiOperation(value = "查询所有订单")
    @PostMapping("/search/all/order")
    public ResponseEntity<MallResponse<Page<OrderInfoDetailDto>>> getAllOrders(@RequestBody SearchOrderParam param){
        return ResponseEntity.ok(successResult(newOrderInfoService.getAllOrder(param)));
    }

    /**
     * @Description 物流信息导入
     * @Author ChenXiang
     * @Date 2019-08-14 10:36:32
     * @ModifyBy
     * @ModifyDate
     **/
    @ApiOperation(value = "仓库发货，订单物流信息导入（第一步）", notes = "物流信息导入（第一步）")
    @PostMapping("/warehouse/import/excel")
    public ResponseEntity<MallResponse> importExcel(@RequestBody ImportParam param, HttpServletResponse response) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] b = decoder.decode(param.getFile());
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        String key = newOrderInfoService.importExcel(stream,response);
        return ResponseEntity.ok(successResult(key));
    }

    @ApiOperation(value = "仓库发货，订单获取导入结果（第二步）",notes = "获取导入结果（第二步）")
    @GetMapping("/warehouse/get/import/result")
    public ResponseEntity<MallResponse> getImportResult(@RequestParam String key,HttpServletResponse response) throws Exception {
        String anImport = newOrderInfoService.getImport(key, response);
        return ResponseEntity.ok(successResult(anImport));
    }




}
