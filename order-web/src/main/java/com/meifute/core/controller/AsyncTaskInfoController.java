package com.meifute.core.controller;


import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.AsyncTaskInfoService;
import com.meifute.core.vo.order.AsyncTaskReq;
import com.meifute.core.vo.order.AsyncTaskResp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 异步任务信息表 前端控制器
 * </p>
 *
 * @author zhangli
 * @since 2020-08-04
 */
@RestController
@RequestMapping("/v1/order/asyncTaskInfo")
@Api(tags = "asyncTaskInfo", description = "异步任务")
@Slf4j
public class AsyncTaskInfoController extends BaseController {

    @Autowired
    AsyncTaskInfoService asyncTaskInfoService;

    /**
     * 查询某人当天所有的任务
     *
     * @return
     */
    @ApiOperation(value = "查询某人当天所有的任务", notes = "查询某人当天所有的任务")
    @RequestMapping(value = "/queryTodayAllTask", method = RequestMethod.GET)
    public ResponseEntity<MallResponse<AsyncTaskResp>> queryTaskResult(@RequestParam("userId") String userId) {
        AsyncTaskResp response = asyncTaskInfoService.queryTaskResult(userId);
        return ResponseEntity.ok(successResult(response));
    }

    /**
     * 提交异步处理请求
     *
     * @return
     */
    @ApiOperation(value = "提交异步处理请求", notes = "提交异步处理请求")
    @RequestMapping(value = "/submitAsyncTask", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<AsyncTaskResp>> submitAsyncTask(@Valid @RequestBody AsyncTaskReq request) {
        AsyncTaskResp response = asyncTaskInfoService.submitAsyncTask(request);
        return ResponseEntity.ok(successResult(response));
    }
}
