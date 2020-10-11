package com.meifute.core.controller;

import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.model.WaybillNosRequest;
import com.meifute.core.service.JiaYiOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v2/logistics")
@Api(tags = "JiaYi", description = "佳一物流对接")
@Slf4j
public class JiaYiOrderController extends BaseController {

    @Autowired
    private JiaYiOrderService jiaYiOrderService;

    @ApiOperation(value = "佳一回调物流单号", notes = "佳一回调物流单号")
    @PostMapping("/receive/waybillno/jiayi")
    public ResponseEntity<MallResponse<Boolean>> receiveWaybillNos(@RequestBody WaybillNosRequest waybillNosRequest) {
        checkClient(waybillNosRequest);
        jiaYiOrderService.receiveWaybillNos(waybillNosRequest.getWaybillNos());
        return ResponseEntity.ok(successResult(true));
    }

    private void checkClient(WaybillNosRequest waybillNosRequest) {
        MallPreconditions.checkToError(ObjectUtils.isNullOrEmpty(waybillNosRequest.getClientId()),"020033");
        MallPreconditions.checkToError(ObjectUtils.isNullOrEmpty(waybillNosRequest.getClientSecret()),"020034");
        if (!waybillNosRequest.getClientId().trim().equals(hashClientId())) {
            MallPreconditions.checkToError(true,"020033");
        }
        if (!waybillNosRequest.getClientSecret().trim().equals(hashSecret())) {
            MallPreconditions.checkToError(true,"020034");
        }
        if (ObjectUtils.isNullOrEmpty(waybillNosRequest.getWaybillNos())) {
            MallPreconditions.checkToError(true,"020036");
        }
    }

    private String hashClientId() {
        //70580f69c9d3a59756
        String HashSalt = "Meifute@QAZ";
        String h =  jiaYiOrderService.SHA1("20190929" + HashSalt + 2);
        return h.substring(0,18);
    }

    private String hashSecret() {
        //687c418b0cbd521d17533c44366841f2aec61e55
        String HashSalt = "Meifute@QAZ";
        return jiaYiOrderService.SHA1("20190929" + HashSalt + 1);
    }


}
