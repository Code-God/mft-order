package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.order.AsyncTaskInfo;
import com.meifute.core.mapper.AsyncTaskInfoMapper;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.vo.order.AsyncTaskReq;
import com.meifute.core.vo.order.AsyncTaskResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * <p>
 * 异步任务信息表 服务类
 * </p>
 *
 * @author zhangli
 * @since 2020-08-04
 */
@Service
@Slf4j
public class AsyncTaskInfoService extends ServiceImpl<AsyncTaskInfoMapper, AsyncTaskInfo> {

    @Autowired
    AsyncTaskInfoMapper asyncTaskInfoMapper;

    private static final long DAY_TO_MILLIS = 1000 * 3600 * 24;

    private static final Integer MAX_DEAL_TIME = 60;//以分为单位

    public AsyncTaskResp queryTaskResult(String userId) {
        List<AsyncTaskInfo> todayTaskInfos = queryTodayTask(userId);
        AsyncTaskResp resp = new AsyncTaskResp();
        resp.setAsyncTaskInfos(todayTaskInfos);
        return resp;
    }

    public AsyncTaskResp submitAsyncTask(AsyncTaskReq request) {
        AsyncTaskResp resp = new AsyncTaskResp();
        resp.setQueueNum(selectQueueNum());
        AsyncTaskInfo taskInfo = createNewTask(request, selectLatestTask(request));
        resp.setId(taskInfo.getId());
        return resp;
    }

    public AsyncTaskInfo queryWaitTask() {
        List<AsyncTaskInfo> taskInfos = asyncTaskInfoMapper.selectList(new EntityWrapper<AsyncTaskInfo>()
                .eq("status", MallStatusEnum.TASK_STATUS_001.getCode())
                .orderBy("create_time", false));
        if (CollectionUtils.isEmpty(taskInfos)) {
            return null;
        }
        return taskInfos.get(0);
    }

    public List<AsyncTaskInfo> queryLongTimeDoingTask() {
        return asyncTaskInfoMapper.selectList(new EntityWrapper<AsyncTaskInfo>()
                .eq("status", MallStatusEnum.TASK_STATUS_002.getCode()));
    }

    private AsyncTaskInfo createNewTask(AsyncTaskReq request, AsyncTaskInfo latestTask) {

        if (Objects.isNull(latestTask)) {
            return doCreateNewTask(request);
        }
        if (request.getForceQuery()) {
            if (MallStatusEnum.TASK_STATUS_003.getCode().equals(latestTask.getStatus())
                    || MallStatusEnum.TASK_STATUS_004.getCode().equals(latestTask.getStatus())) {
                return doCreateNewTask(request);
            }
        }
        return latestTask;
    }

    private AsyncTaskInfo doCreateNewTask(AsyncTaskReq request) {
        AsyncTaskInfo taskInfo = AsyncTaskInfo.builder()
                .processName(request.getProcessName())
                .param(request.getParam())
                .userId(request.getUserId())
                .createTime(new Date())
                .status(MallStatusEnum.TASK_STATUS_001.getCode()).build();
        asyncTaskInfoMapper.insert(taskInfo);
        return taskInfo;
    }

    private List<AsyncTaskInfo> queryTodayTask(String userId) {
        long startTime = calTodayZeroTime();
        long endTime = calToday24Time(startTime);
        List<AsyncTaskInfo> taskInfos =  asyncTaskInfoMapper.selectList(new EntityWrapper<AsyncTaskInfo>()
                .eq(!StringUtils.isEmpty(userId), "user_id", userId)
                .between("create_time", new Timestamp(startTime).toString(), new Timestamp(endTime).toString())
                .orderBy("create_time",false));
        return taskInfos;
    }

    private long calToday24Time(long startTime) {
        return startTime + DAY_TO_MILLIS - 1;
    }

    private long calTodayZeroTime() {
        long current = System.currentTimeMillis();//当前时间毫秒数
        return current / (DAY_TO_MILLIS) * (DAY_TO_MILLIS) - TimeZone.getDefault().getRawOffset();
    }

    private int selectQueueNum() {
        return asyncTaskInfoMapper.selectCount(new EntityWrapper<AsyncTaskInfo>().eq("status", MallStatusEnum.TASK_STATUS_001.getCode())) + 1;
    }


    private AsyncTaskInfo selectLatestTask(AsyncTaskReq request) {
        long startTime = calTodayZeroTime();
        long endTime = calToday24Time(startTime);
        List<AsyncTaskInfo> taskInfos = asyncTaskInfoMapper.selectList(new EntityWrapper<AsyncTaskInfo>()
                .eq(!StringUtils.isEmpty(request.getParam()), "param", request.getParam())
                .eq(!StringUtils.isEmpty(request.getUserId()), "user_id", request.getUserId())
                .eq(!StringUtils.isEmpty(request.getProcessName()), "process_name", request.getProcessName())
                .between("create_time", new Timestamp(startTime).toString(), new Timestamp(endTime).toString())
                .orderBy("create_time",false));
        if (CollectionUtils.isEmpty(taskInfos)) {
            return null;
        }
        return taskInfos.get(0);
    }
}
