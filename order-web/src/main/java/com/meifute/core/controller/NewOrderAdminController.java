package com.meifute.core.controller;

import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallTransferGoods;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.mapper.MallTransferGoodsMapper;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallOrderInfoService;
import com.meifute.core.vo.OrderCloseParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * @program: m-mall-order
 * @description: 订单列表重构
 * @author: Mr.Wang
 * @create: 2019-04-15 16:32
 **/
@RestController
@RequestMapping("v1/app/admin/new/order")
@Api(tags = "NewOrderAdmin", description = "后台订单中心重构版本")
@Slf4j
public class NewOrderAdminController extends BaseController {
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private MallOrderInfoService mallOrderInfoService;
    @Autowired
    private MallTransferGoodsMapper MallTransferGoodsMapper;



    /**
     * 批量关闭订单
     */
    @ApiOperation(value = "批量关闭订单", notes = "批量关闭订单")
    @RequestMapping(value = "/batchClose/order", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> batchCloseOrder(@RequestBody OrderCloseParam param) {
        if (StringUtils.isEmpty(param.getOrderIds())) {
            throw new MallException(OrderRespCode.PARAM_NOT_FOUND);
        }
        String[] split = param.getOrderIds().split(",");
        List<String> list = Arrays.asList(split);
        list.stream().forEach(orderId -> {
            if (!StringUtils.isEmpty(orderId)) {
                MallOrderInfo mallOrderInfo = new MallOrderInfo();
                mallOrderInfo.setOrderId(orderId);
                mallOrderInfoService.closeOrderFromAdmin(mallOrderInfo);
            }

        });

        return ResponseEntity.ok(successResult(Boolean.TRUE));
    }

    /**
     * 后台修改转货列表备注
     */
    @ApiOperation(value = "后台修改转货列表备注", notes = "后台修改转货列表备注")
    @RequestMapping(value = "/update/transfer/order", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> batchCloseOrder(@RequestBody MallTransferGoods mallTransferGoods) {
        Boolean result = false;
        Integer integer = MallTransferGoodsMapper.updateByOrderId(mallTransferGoods);
        result = integer == 1 ? true : false;
        return ResponseEntity.ok(successResult(result));
    }


}
