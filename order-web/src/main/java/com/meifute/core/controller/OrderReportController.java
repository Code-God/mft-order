package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.dto.PageDto;
import com.meifute.core.dto.report.AgentTotalAmountSortRequest;
import com.meifute.core.dto.report.AgentTotalAmountSortResponseDTO;
import com.meifute.core.dto.report.BaseSortRequest;
import com.meifute.core.dto.report.NewAgent.NewGeneralAgentSortResponseDTO;
import com.meifute.core.dto.report.OrderReportResponseDTO;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallOrderInfoService;
import com.meifute.core.service.MallOrderItemService;
import com.meifute.core.service.MallOrderReportService;
import com.meifute.core.service.WuYouService;
import com.meifute.core.util.MybatisPageUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @program: m-mall-order
 * @description: 后台订单报表统计
 * @author: Mr.Wang
 * @create: 2019-04-24 13:52
 **/
@RestController
@RequestMapping("v1/admin/report")
@Api(tags = "report", description = "后台订单报表统计")
@Slf4j
public class OrderReportController extends BaseController {
    @Autowired
    private MallOrderInfoService mallOrderInfoService;
    @Autowired
    private MallOrderReportService mallOrderReportService;
    @Autowired
    WuYouService wuYouService;
    @Autowired
    MallOrderItemService mallOrderItemService;


    /**
     * 根据条件统计订单报表
     */
    @ApiOperation(value = "根据条件统计订单报表", notes = "根据条件统计订单报表")
    @GetMapping(value = "/order/report")
    public ResponseEntity<MallResponse<OrderReportResponseDTO>> orderReport(@RequestParam("year") String year) {
        OrderReportResponseDTO orderReportResponseDTO = mallOrderInfoService.orderReport(year);

        return ResponseEntity.ok(successResult(orderReportResponseDTO));
    }

    @ApiOperation(value = "订单统计报表导出", notes = "订单统计报表导出")
    @GetMapping("/export/order/report")
    public ResponseEntity<MallResponse> exportOrderReport(@RequestParam("year") String year, HttpServletResponse response) throws Exception {
        String s = mallOrderInfoService.exportOrderReport(year, response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "代理进出货排名报表", notes = "代理进出货排名报表")
    @PostMapping("/sort/agent/totalAmount/report")
    public ResponseEntity<MallResponse<PageDto<AgentTotalAmountSortResponseDTO>>> sortAgentTotalAmount(@RequestBody AgentTotalAmountSortRequest request) {
        Page page = MybatisPageUtil.getPage(request.getPageCurrent(), request.getPageSize());
        PageDto<AgentTotalAmountSortResponseDTO> responseDTO = mallOrderReportService.sortAgentTotalAmount(request,page);
        return ResponseEntity.ok(successResult(responseDTO));
    }

    @ApiOperation(value = "代理进出货排名导出", notes = "代理进出货排名导出")
    @PostMapping("/export/agent/totalAmount")
    public ResponseEntity<MallResponse> exportAgentTotalAmount(@RequestBody AgentTotalAmountSortRequest request, HttpServletResponse response) throws Exception {
        String s = mallOrderReportService.exportAgentTotalAmount(request, response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "代理进出货前三名", notes = "代理进出货前三名")
    @PostMapping("/agent/totalAmount/firstThree")
    public ResponseEntity<MallResponse<Map<String, List<AgentTotalAmountSortResponseDTO>>>> sortAgentTotalAmountFirstThree(@RequestBody AgentTotalAmountSortRequest request) {
        Map<String, List<AgentTotalAmountSortResponseDTO>> stringListMap = mallOrderReportService.sortAgentTotalAmountFirstThree(request);
        return ResponseEntity.ok(successResult(stringListMap));
    }

    @ApiOperation(value = "新增总代排名报表", notes = "新增总代排名报表")
    @PostMapping("/sort/newGeneralAgent/report")
    public ResponseEntity<MallResponse<PageDto<NewGeneralAgentSortResponseDTO>>> sortNewGeneralAgent(@RequestBody BaseSortRequest request) {
        Page page = MybatisPageUtil.getPage(request.getPageCurrent(), request.getPageSize());
        PageDto<NewGeneralAgentSortResponseDTO> responseDTO = mallOrderReportService.sortNewGeneralAgent(request,page);
        return ResponseEntity.ok(successResult(responseDTO));
    }

    @ApiOperation(value = "新增总代排名报表导出", notes = "新增总代排名报表导出")
    @PostMapping("/export/newGeneralAgent")
    public ResponseEntity<MallResponse> exportNewGeneralAgent(@RequestBody BaseSortRequest request, HttpServletResponse response) throws Exception {
        String s = mallOrderReportService.exportNewGeneralAgent(request, response);
        return ResponseEntity.ok(successResult(s));
    }

    @ApiOperation(value = "新增总代前三名", notes = "新增总代前三名")
    @PostMapping("/sort/newGeneralAgent/firstThree")
    public ResponseEntity<MallResponse<List<NewGeneralAgentSortResponseDTO>>> sortNewGeneralAgentFirstThree(@RequestBody BaseSortRequest request) {
        List<NewGeneralAgentSortResponseDTO> responseDTO = mallOrderReportService.sortNewGeneralAgentFirstThree(request);
        return ResponseEntity.ok(successResult(responseDTO));
    }

    @GetMapping("/wuyou/order")
    public Boolean testWuYou(@RequestParam("orderId") String orderId){
        MallOrderInfo mallOrderInfo = mallOrderInfoService.selectByIdNew(orderId);
        List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(orderId);
        Boolean aBoolean = wuYouService.orderReceive(mallOrderInfo, itemList);
        return aBoolean;
    }

}
